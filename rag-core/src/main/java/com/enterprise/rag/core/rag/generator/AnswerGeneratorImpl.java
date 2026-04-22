package com.enterprise.rag.core.rag.generator;

import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.prompt.PromptBuilder;
import com.enterprise.rag.core.rag.prompt.PromptStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 答案生成器实现
 * 支持 OpenAI 和通义千问 API
 */
@Slf4j
@Service
public class AnswerGeneratorImpl implements AnswerGenerator {
    static final String SOURCE_MARKER_PREFIX = "[Source";
    private static final Pattern RAW_SOURCE_MARKER_PATTERN = Pattern.compile("\\[Source\\s+\\d+:[^\\]]*]");
    private static final Pattern INLINE_SOURCE_MARKER_PATTERN = Pattern.compile("\\[Source\\s+\\d+]");

    private final LLMProperties properties;
    private final PromptBuilder promptBuilder;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AnswerGeneratorImpl(LLMProperties properties, PromptBuilder promptBuilder,
            WebClient.Builder proxyWebClientBuilder) {
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.objectMapper = new ObjectMapper();
        this.webClient = proxyWebClientBuilder.clone()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Override
    public GeneratedAnswer generate(String query, List<RetrievedContext> contexts) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or blank");
        }

        log.debug("Generating answer for query: {}", truncateForLog(query));

        // 构建 Prompt（RAG-02: 上下文去重 + token budget + 来源增强）
        PromptBuilder.PromptBuildResult buildResult = promptBuilder.buildOptimized(
                query,
                contexts,
                PromptStrategy.STRUCTURED,
                PromptBuilder.DEFAULT_CONTEXT_TOKEN_BUDGET);
        String prompt = buildResult.prompt();
        List<RetrievedContext> effectiveContexts = buildResult.contexts();

        // 调用 LLM API
        String answer = sanitizeAnswerText(callLLM(prompt));

        // 提取引用来源
        List<Citation> citations = extractCitations(answer, effectiveContexts);

        // 构建元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model", getModelName());
        metadata.put("contextCount", effectiveContexts.size());
        metadata.put("contextTokenBudget", buildResult.tokenBudget());
        metadata.put("estimatedContextTokens", buildResult.estimatedContextTokens());
        metadata.put("removedByDedup", buildResult.removedByDedup());
        metadata.put("removedByBudget", buildResult.removedByBudget());

        return GeneratedAnswer.of(answer, citations, metadata);
    }

    @Override
    public Flux<String> generateStream(String query, List<RetrievedContext> contexts) {
        if (query == null || query.isBlank()) {
            return Flux.error(new IllegalArgumentException("Query cannot be null or blank"));
        }

        log.debug("Generating streaming answer for query: {}", truncateForLog(query));

        PromptBuilder.PromptBuildResult buildResult = promptBuilder.buildOptimized(
                query,
                contexts,
                PromptStrategy.STRUCTURED,
                PromptBuilder.DEFAULT_CONTEXT_TOKEN_BUDGET);
        String prompt = buildResult.prompt();
        log.info("stream_prompt_ready model={}, query={}, contextCount={}, estimatedContextTokens={}, removedByDedup={}, removedByBudget={}",
                getModelName(),
                truncateForLog(query),
                buildResult.contexts().size(),
                buildResult.estimatedContextTokens(),
                buildResult.removedByDedup(),
                buildResult.removedByBudget());

        return sanitizeStream(callLLMStream(prompt));
    }

    @Override
    public String getModelName() {
        return "openai".equalsIgnoreCase(properties.getProvider())
                ? properties.getOpenai().getModel()
                : properties.getQwen().getModel();
    }

    /**
     * 调用 LLM API（同步）
     */
    private String callLLM(String prompt) {
        try {
            if ("openai".equalsIgnoreCase(properties.getProvider())) {
                return callOpenAI(prompt);
            } else {
                return callQwen(prompt);
            }
        } catch (Exception e) {
            log.error("Failed to call LLM API", e);
            throw new LLMException("Failed to generate answer: " + e.getMessage(), e);
        }
    }

    /**
     * 调用 OpenAI API
     */
    private String callOpenAI(String prompt) {
        LLMProperties.OpenAIConfig config = properties.getOpenai();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)));
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());

        Mono<String> responseMono = webClient.post()
                .uri(config.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(properties.getTimeout()));

        String response = applyRetryPolicy(responseMono, "openai", "/chat/completions")
                .doOnError(error -> logLlmError("openai", "/chat/completions", error))
                .block();

        return parseOpenAIResponse(response);
    }

    /**
     * 调用通义千问 API
     */
    private String callQwen(String prompt) {
        LLMProperties.QwenConfig config = properties.getQwen();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("input", Map.of(
                "messages", List.of(
                        Map.of("role", "user", "content", prompt))));
        requestBody.put("parameters", Map.of(
                "temperature", config.getTemperature(),
                "max_tokens", config.getMaxTokens()));

        Mono<String> responseMono = webClient.post()
                .uri(config.getBaseUrl() + "/services/aigc/text-generation/generation")
                .header("Authorization", "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(properties.getTimeout()));

        String response = applyRetryPolicy(responseMono, "qwen", "/services/aigc/text-generation/generation")
                .doOnError(error -> logLlmError("qwen", "/services/aigc/text-generation/generation", error))
                .block();

        return parseQwenResponse(response);
    }

    /**
     * 调用 LLM API（流式）
     */
    private Flux<String> callLLMStream(String prompt) {
        if ("openai".equalsIgnoreCase(properties.getProvider())) {
            return callOpenAIStream(prompt);
        } else {
            return callQwenStream(prompt);
        }
    }

    /**
     * 调用 OpenAI API（流式）
     */
    private Flux<String> callOpenAIStream(String prompt) {
        LLMProperties.OpenAIConfig config = properties.getOpenai();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)));
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());
        requestBody.put("stream", true);

        Flux<String> responseFlux = webClient.post()
                .uri(config.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(properties.getTimeout()));

        return applyRetryPolicy(responseFlux, "openai", "/chat/completions(stream)")
                .doOnError(error -> logLlmError("openai", "/chat/completions(stream)", error))
                .filter(line -> !line.equals("[DONE]"))
                .map(this::parseOpenAIStreamChunk)
                .filter(content -> content != null && !content.isEmpty());
    }

    /**
     * 调用通义千问 API（流式）
     */
    private Flux<String> callQwenStream(String prompt) {
        LLMProperties.QwenConfig config = properties.getQwen();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("input", Map.of(
                "messages", List.of(
                        Map.of("role", "user", "content", prompt))));
        requestBody.put("parameters", Map.of(
                "temperature", config.getTemperature(),
                "max_tokens", config.getMaxTokens(),
                "incremental_output", true));

        Flux<String> responseFlux = webClient.post()
                .uri(config.getBaseUrl() + "/services/aigc/text-generation/generation")
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("X-DashScope-SSE", "enable")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(properties.getTimeout()));

        return applyRetryPolicy(responseFlux, "qwen", "/services/aigc/text-generation/generation(stream)")
                .doOnError(error -> logLlmError("qwen", "/services/aigc/text-generation/generation(stream)", error))
                .map(this::parseQwenStreamChunk)
                .filter(content -> content != null && !content.isEmpty());
    }

    private <T> Mono<T> applyRetryPolicy(Mono<T> source, String provider, String endpoint) {
        int maxRetries = Math.max(0, properties.getMaxRetries());
        if (maxRetries == 0) {
            return source;
        }
        return source.retryWhen(buildRetrySpec(provider, endpoint, maxRetries));
    }

    private <T> Flux<T> applyRetryPolicy(Flux<T> source, String provider, String endpoint) {
        int maxRetries = Math.max(0, properties.getMaxRetries());
        if (maxRetries == 0) {
            return source;
        }
        return source.retryWhen(buildRetrySpec(provider, endpoint, maxRetries));
    }

    private Retry buildRetrySpec(String provider, String endpoint, int maxRetries) {
        return Retry.backoff(maxRetries, Duration.ofMillis(800))
                .filter(this::isRetryableError)
                .doBeforeRetry(signal -> {
                    Throwable cause = unwrap(signal.failure());
                    log.warn("LLM调用重试: provider={}, endpoint={}, attempt={}/{}, reason={}",
                            provider,
                            endpoint,
                            signal.totalRetries() + 1,
                            maxRetries,
                            cause.getClass().getSimpleName() + ": " + cause.getMessage());
                })
                .onRetryExhaustedThrow((spec, signal) -> {
                    Throwable cause = unwrap(signal.failure());
                    return new LLMException("Max retries exceeded for LLM API: " + cause.getMessage(), cause);
                });
    }

    private boolean isRetryableError(Throwable error) {
        Throwable cause = unwrap(error);
        if (cause instanceof WebClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            return statusCode == 429 || statusCode >= 500;
        }
        return cause instanceof TimeoutException
                || cause instanceof IOException
                || cause instanceof ConnectException;
    }

    private void logLlmError(String provider, String endpoint, Throwable error) {
        Throwable cause = unwrap(error);
        if (cause instanceof WebClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            String bodyPreview = body == null ? "" : (body.length() > 500 ? body.substring(0, 500) + "..." : body);
            log.error("LLM调用失败: provider={}, endpoint={}, status={}, body={}",
                    provider, endpoint, ex.getStatusCode().value(), bodyPreview, ex);
            return;
        }
        log.error("LLM调用失败: provider={}, endpoint={}, causeType={}, message={}",
                provider,
                endpoint,
                cause.getClass().getSimpleName(),
                cause.getMessage(),
                cause);
    }

    private Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * 解析 OpenAI 响应
     */
    private String parseOpenAIResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", response, e);
            throw new LLMException("Failed to parse OpenAI response", e);
        }
    }

    /**
     * 解析通义千问响应
     */
    private String parseQwenResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("output").path("text").asText();
        } catch (Exception e) {
            log.error("Failed to parse Qwen response: {}", response, e);
            throw new LLMException("Failed to parse Qwen response", e);
        }
    }

    static String sanitizeAnswerText(String answer) {
        if (answer == null || answer.isBlank()) {
            return answer;
        }

        String sanitized = RAW_SOURCE_MARKER_PATTERN.matcher(answer).replaceAll("");
        sanitized = INLINE_SOURCE_MARKER_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = sanitized.replaceAll("([。！？.!?])\\s*中指出", "$1");
        sanitized = sanitized.replaceAll("\\s{2,}", " ")
                .replaceAll("\\s+([，。！？；：,!.?;:])", "$1")
                .trim();
        return sanitized;
    }

    Flux<String> sanitizeStream(Flux<String> upstream) {
        StreamSanitizer sanitizer = new StreamSanitizer();
        return upstream.concatMap(chunk -> Flux.fromIterable(sanitizer.accept(chunk)))
                .concatWith(Flux.defer(() -> Flux.fromIterable(sanitizer.finish())))
                .filter(content -> content != null && !content.isEmpty());
    }

    /**
     * 解析 OpenAI 流式响应块
     */
    private String parseOpenAIStreamChunk(String chunk) {
        try {
            if (chunk.startsWith("data: ")) {
                chunk = chunk.substring(6);
            }
            if (chunk.equals("[DONE]")) {
                return "";
            }
            JsonNode root = objectMapper.readTree(chunk);
            return root.path("choices").get(0).path("delta").path("content").asText("");
        } catch (Exception e) {
            log.debug("Failed to parse stream chunk: {}", chunk);
            return "";
        }
    }

    /**
     * 解析通义千问流式响应块
     */
    private String parseQwenStreamChunk(String chunk) {
        try {
            if (chunk.startsWith("data:")) {
                chunk = chunk.substring(5);
            }
            JsonNode root = objectMapper.readTree(chunk);
            return root.path("output").path("text").asText("");
        } catch (Exception e) {
            log.debug("Failed to parse stream chunk: {}", chunk);
            return "";
        }
    }

    /**
     * 从答案中提取引用来源
     */
    private List<Citation> extractCitations(String answer, List<RetrievedContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return List.of();
        }

        List<Citation> citations = new ArrayList<>();
        String answerLower = answer.toLowerCase();

        for (RetrievedContext context : contexts) {
            // 检查答案是否引用了该上下文的内容
            String[] sentences = context.content().split("[.。!！?？]");
            for (String sentence : sentences) {
                String sentenceTrimmed = sentence.trim();
                if (sentenceTrimmed.length() > 20) {
                    // 检查是否有相似内容
                    String[] words = sentenceTrimmed.toLowerCase().split("\\s+");
                    int matchCount = 0;
                    for (String word : words) {
                        if (word.length() > 3 && answerLower.contains(word)) {
                            matchCount++;
                        }
                    }
                    // 如果超过50%的词匹配，认为是引用
                    if (words.length > 0 && (float) matchCount / words.length > 0.5) {
                        citations.add(Citation.of(context.source(), sentenceTrimmed));
                        break;
                    }
                }
            }
        }

        return citations;
    }

    /**
     * 截断日志输出
     */
    private String truncateForLog(String text) {
        if (text == null)
            return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    static final class StreamSanitizer {
        private final StringBuilder pending = new StringBuilder();

        List<String> accept(String chunk) {
            if (chunk == null || chunk.isEmpty()) {
                return List.of();
            }
            pending.append(chunk);
            return drain(false);
        }

        List<String> finish() {
            return drain(true);
        }

        private List<String> drain(boolean finalFlush) {
            List<String> emitted = new ArrayList<>();

            while (pending.length() > 0) {
                int markerIndex = pending.indexOf(SOURCE_MARKER_PREFIX);
                if (markerIndex >= 0) {
                    if (markerIndex > 0) {
                        emitted.add(pending.substring(0, markerIndex));
                        pending.delete(0, markerIndex);
                    }

                    int markerEnd = pending.indexOf("]");
                    if (markerEnd >= 0) {
                        pending.delete(0, markerEnd + 1);
                        continue;
                    }
                    break;
                }

                if (finalFlush) {
                    emitted.add(pending.toString());
                    pending.setLength(0);
                    break;
                }

                int safeLength = Math.max(0, pending.length() - (SOURCE_MARKER_PREFIX.length() - 1));
                if (safeLength == 0) {
                    break;
                }
                emitted.add(pending.substring(0, safeLength));
                pending.delete(0, safeLength);
            }

            return emitted.stream()
                    .map(AnswerGeneratorImpl::sanitizeAnswerText)
                    .filter(text -> text != null && !text.isEmpty())
                    .toList();
        }
    }
}
