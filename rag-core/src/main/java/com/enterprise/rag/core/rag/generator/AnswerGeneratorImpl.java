package com.enterprise.rag.core.rag.generator;

import com.enterprise.rag.common.trace.GenAiTelemetry;
import com.enterprise.rag.core.rag.citation.CitationValidationResult;
import com.enterprise.rag.core.rag.citation.CitationValidator;
import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.prompt.PromptBuilder;
import com.enterprise.rag.core.rag.prompt.PromptStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeoutException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 答案生成器实现
 * 支持 OpenAI 和通义千问 API
 */
@Slf4j
@Service
public class AnswerGeneratorImpl implements AnswerGenerator {
    static final String SOURCE_MARKER_PREFIX = "[Source";
    private static final int MAX_FALLBACK_CITATIONS = 3;
    private static final int FALLBACK_SNIPPET_MAX_LENGTH = 180;
    private static final Pattern RAW_SOURCE_MARKER_PATTERN = Pattern.compile("\\[Source\\s+\\d+:[^\\]]*]");
    private static final Pattern INLINE_SOURCE_MARKER_PATTERN = Pattern.compile("\\[Source\\s+\\d+]");
    private static final Pattern SENTENCE_BOUNDARY_PATTERN = Pattern.compile("(?<=[.。!！?？;；])|\\R+");
    private static final Pattern LATIN_TOKEN_PATTERN = Pattern.compile("[\\p{Alnum}]+");
    private static final Pattern CJK_SEGMENT_PATTERN = Pattern.compile("[\\p{IsHan}]+");

    private final LLMProperties properties;
    private final PromptBuilder promptBuilder;
    private final CitationValidator citationValidator;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final GenAiTelemetry telemetry;

    @Autowired
    public AnswerGeneratorImpl(LLMProperties properties, PromptBuilder promptBuilder,
            CitationValidator citationValidator, WebClient.Builder proxyWebClientBuilder,
            GenAiTelemetry telemetry) {
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.citationValidator = citationValidator;
        this.telemetry = telemetry == null ? GenAiTelemetry.noop() : telemetry;
        this.objectMapper = new ObjectMapper();
        this.webClient = proxyWebClientBuilder.clone()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public AnswerGeneratorImpl(LLMProperties properties, PromptBuilder promptBuilder,
            CitationValidator citationValidator, WebClient.Builder proxyWebClientBuilder) {
        this(properties, promptBuilder, citationValidator, proxyWebClientBuilder, GenAiTelemetry.noop());
    }

    @Override
    public GeneratedAnswer generate(String query, List<RetrievedContext> contexts) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or blank");
        }

        log.debug("Generating answer");

        // 构建 Prompt（RAG-02: 上下文去重 + token budget + 来源增强）
        PromptBuilder.PromptBuildResult buildResult = buildPrompt(query, contexts);
        String prompt = buildResult.prompt();
        List<RetrievedContext> effectiveContexts = buildResult.contexts();

        // 调用 LLM API
        String answer = sanitizeAnswerText(traceStage(
                GenAiTelemetry.SpanNames.LLM_REQUEST,
                () -> callLLM(prompt)));

        // 提取引用来源
        CitationResolution citationResolution = traceStage(
                GenAiTelemetry.SpanNames.CITATION_VALIDATE,
                () -> resolveCitations(query, answer, effectiveContexts));
        CitationValidationResult citationValidation = citationResolution.validation();

        // 构建元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model", getModelName());
        metadata.put("contextCount", effectiveContexts.size());
        metadata.put("contextTokenBudget", buildResult.tokenBudget());
        metadata.put("estimatedContextTokens", buildResult.estimatedContextTokens());
        metadata.put("removedByDedup", buildResult.removedByDedup());
        metadata.put("removedByBudget", buildResult.removedByBudget());
        Map<String, Object> citationMetadata = citationValidation.metadata();
        metadata.put("citationValidation", citationMetadata);
        metadata.put("citationFallbackUsed", citationResolution.fallbackUsed());
        metadata.put("citationFallbackCount", citationResolution.fallbackCount());
        metadata.putAll(citationMetadata);
        if (isNoAnswerText(answer)) {
            metadata.put("status", "no_result");
        }

        return GeneratedAnswer.of(answer, citationValidation.citations(), metadata);
    }

    @Override
    public Flux<String> generateStream(String query, List<RetrievedContext> contexts) {
        if (query == null || query.isBlank()) {
            return Flux.error(new IllegalArgumentException("Query cannot be null or blank"));
        }

        log.debug("Generating streaming answer");

        PromptBuilder.PromptBuildResult buildResult = buildPrompt(query, contexts);
        String prompt = buildResult.prompt();
        log.info("stream_prompt_ready model={}, contextCount={}, estimatedContextTokens={}, removedByDedup={}, removedByBudget={}",
                getModelName(),
                buildResult.contexts().size(),
                buildResult.estimatedContextTokens(),
                buildResult.removedByDedup(),
                buildResult.removedByBudget());

        GenAiTelemetry.SpanScope llmSpan = telemetry.startSpan(
                GenAiTelemetry.SpanNames.LLM_REQUEST, Map.of());
        Flux<String> stream;
        try {
            stream = callLLMStream(prompt, llmSpan);
        } catch (RuntimeException failure) {
            llmSpan.safeError(failure, "llm", "LLM_FAILED").finish("ERROR");
            throw failure;
        }
        llmSpan.detach();
        return stream
                .doOnError(failure -> llmSpan.safeError(failure, "llm", "LLM_FAILED"))
                .doFinally(signal -> llmSpan.finish(switch (signal) {
                    case CANCEL -> "CANCELLED";
                    case ON_ERROR -> "ERROR";
                    default -> "SUCCESS";
                }));
    }

    private <T> T traceStage(String spanName, java.util.function.Supplier<T> action) {
        try (GenAiTelemetry.SpanScope stage = telemetry.startSpan(spanName, Map.of())) {
            try {
                T result = action.get();
                stage.outcome("SUCCESS");
                return result;
            } catch (RuntimeException failure) {
                if (failure instanceof LLMException llmFailure) {
                    stage.diagnostics(llmFailure.diagnostics());
                }
                stage.safeError(failure, "generation", "STAGE_FAILED").outcome("ERROR");
                throw failure;
            }
        }
    }

    private PromptBuilder.PromptBuildResult buildPrompt(String query, List<RetrievedContext> contexts) {
        try (GenAiTelemetry.SpanScope prompt = telemetry.startSpan(
                GenAiTelemetry.SpanNames.PROMPT_BUILD, Map.of())) {
            try {
                PromptBuilder.PromptBuildResult result = promptBuilder.buildOptimized(
                        query,
                        contexts,
                        PromptStrategy.STRUCTURED,
                        PromptBuilder.DEFAULT_CONTEXT_TOKEN_BUDGET);
                prompt.longFact(GenAiTelemetry.Attributes.PROMPT_ESTIMATED_TOKENS,
                                result.estimatedContextTokens())
                        .outcome("SUCCESS");
                return result;
            } catch (RuntimeException failure) {
                prompt.safeError(failure, "prompt", "PROMPT_BUILD_FAILED").outcome("ERROR");
                throw failure;
            }
        }
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
        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call LLM API: errorType={}", e.getClass().getSimpleName());
            throw new LLMException("LLM provider request failed", e);
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
        AtomicInteger attemptCount = new AtomicInteger();
        AtomicInteger retryCount = new AtomicInteger();

        try {
            String response = applyRetryPolicy(
                    Mono.defer(() -> {
                        attemptCount.incrementAndGet();
                        return responseMono;
                    }),
                    "openai",
                    "/chat/completions",
                    retryCount)
                    .doOnError(error -> logLlmError("openai", "/chat/completions", error))
                    .block();

            String parsed = parseOpenAIResponse(response);
            telemetry.currentProviderCall(
                    "openai", "openai", config.getModel(), "http_json",
                    attemptCount.get(), retryCount.get());
            return parsed;
        } catch (Exception e) {
            throw buildLlmException(
                    "openai",
                    "/chat/completions",
                    config.getModel(),
                    e,
                    attemptCount.get(),
                    retryCount.get());
        }
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
        AtomicInteger attemptCount = new AtomicInteger();
        AtomicInteger retryCount = new AtomicInteger();

        try {
            String response = applyRetryPolicy(
                    Mono.defer(() -> {
                        attemptCount.incrementAndGet();
                        return responseMono;
                    }),
                    "qwen",
                    "/services/aigc/text-generation/generation",
                    retryCount)
                    .doOnError(error -> logLlmError("qwen", "/services/aigc/text-generation/generation", error))
                    .block();

            String parsed = parseQwenResponse(response);
            telemetry.currentProviderCall(
                    "qwen", "qwen", config.getModel(), "http_json",
                    attemptCount.get(), retryCount.get());
            return parsed;
        } catch (Exception e) {
            throw buildLlmException(
                    "qwen",
                    "/services/aigc/text-generation/generation",
                    config.getModel(),
                    e,
                    attemptCount.get(),
                    retryCount.get());
        }
    }

    /**
     * 调用 LLM API（流式）
     */
    private Flux<String> callLLMStream(String prompt, GenAiTelemetry.SpanScope llmSpan) {
        if ("openai".equalsIgnoreCase(properties.getProvider())) {
            return callOpenAIStream(prompt, llmSpan);
        } else {
            return callQwenStream(prompt, llmSpan);
        }
    }

    /**
     * 调用 OpenAI API（流式）
     */
    private Flux<String> callOpenAIStream(String prompt, GenAiTelemetry.SpanScope llmSpan) {
        LLMProperties.OpenAIConfig config = properties.getOpenai();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)));
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());
        requestBody.put("stream", true);

        AtomicInteger attemptCount = new AtomicInteger();
        AtomicInteger retryCount = new AtomicInteger();
        AtomicBoolean visibleContentEmitted = new AtomicBoolean();
        String endpoint = "/chat/completions(stream)";

        Flux<String> visibleResponseFlux = Flux.defer(() -> sanitizeStream(Flux.defer(() -> {
            attemptCount.incrementAndGet();
            return webClient.post()
                    .uri(config.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(properties.getTimeout()));
        })
                .filter(line -> !line.equals("[DONE]"))
                .map(this::parseOpenAIStreamChunk)
                .filter(content -> content != null && !content.isEmpty())))
                .switchIfEmpty(Flux.error(invalidResponse()));

        return applyStreamRetryPolicy(
                visibleResponseFlux.doOnNext(ignored -> visibleContentEmitted.set(true)),
                "openai",
                endpoint,
                retryCount,
                visibleContentEmitted)
                .doOnError(error -> logLlmError("openai", endpoint, error))
                .onErrorMap(error -> buildLlmException(
                                "openai",
                                endpoint,
                                config.getModel(),
                                error,
                                attemptCount.get(),
                                retryCount.get()))
                .doOnComplete(() -> llmSpan.providerCall(
                        "openai", "openai", config.getModel(), "sse",
                        attemptCount.get(), retryCount.get()));
    }

    /**
     * 调用通义千问 API（流式）
     */
    private Flux<String> callQwenStream(String prompt, GenAiTelemetry.SpanScope llmSpan) {
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

        AtomicInteger attemptCount = new AtomicInteger();
        AtomicInteger retryCount = new AtomicInteger();
        AtomicBoolean visibleContentEmitted = new AtomicBoolean();
        String endpoint = "/services/aigc/text-generation/generation(stream)";

        Flux<String> visibleResponseFlux = Flux.defer(() -> sanitizeStream(Flux.defer(() -> {
            attemptCount.incrementAndGet();
            return webClient.post()
                    .uri(config.getBaseUrl() + "/services/aigc/text-generation/generation")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("X-DashScope-SSE", "enable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(properties.getTimeout()));
        })
                .map(this::parseQwenStreamChunk)
                .filter(content -> content != null && !content.isEmpty())))
                .switchIfEmpty(Flux.error(invalidResponse()));

        return applyStreamRetryPolicy(
                visibleResponseFlux.doOnNext(ignored -> visibleContentEmitted.set(true)),
                "qwen",
                endpoint,
                retryCount,
                visibleContentEmitted)
                .doOnError(error -> logLlmError("qwen", endpoint, error))
                .onErrorMap(error -> buildLlmException(
                                "qwen",
                                endpoint,
                                config.getModel(),
                                error,
                                attemptCount.get(),
                                retryCount.get()))
                .doOnComplete(() -> llmSpan.providerCall(
                        "qwen", "qwen", config.getModel(), "sse",
                        attemptCount.get(), retryCount.get()));
    }

    private <T> Mono<T> applyRetryPolicy(
            Mono<T> source,
            String provider,
            String endpoint,
            AtomicInteger retryCount) {
        int maxRetries = Math.max(0, properties.getMaxRetries());
        if (maxRetries == 0) {
            return source;
        }
        return source.retryWhen(buildRetrySpec(provider, endpoint, maxRetries, retryCount));
    }

    private <T> Flux<T> applyStreamRetryPolicy(
            Flux<T> source,
            String provider,
            String endpoint,
            AtomicInteger retryCount,
            AtomicBoolean visibleContentEmitted) {
        int maxRetries = Math.max(0, properties.getMaxRetries());
        if (maxRetries == 0) {
            return source;
        }
        return source.retryWhen(buildRetrySpec(
                provider,
                endpoint,
                maxRetries,
                retryCount,
                error -> !visibleContentEmitted.get() && isRetryableError(error)));
    }

    private Retry buildRetrySpec(
            String provider,
            String endpoint,
            int maxRetries,
            AtomicInteger retryCount) {
        return buildRetrySpec(provider, endpoint, maxRetries, retryCount, this::isRetryableError);
    }

    private Retry buildRetrySpec(
            String provider,
            String endpoint,
            int maxRetries,
            AtomicInteger retryCount,
            Predicate<Throwable> retryFilter) {
        return Retry.backoff(maxRetries, Duration.ofMillis(800))
                .filter(retryFilter)
                .doBeforeRetry(signal -> {
                    retryCount.set(Math.toIntExact(signal.totalRetries() + 1));
                    Throwable cause = unwrap(signal.failure());
                    log.warn("LLM调用重试: provider={}, endpoint={}, attempt={}/{}, errorType={}",
                            provider,
                            endpoint,
                            signal.totalRetries() + 1,
                            maxRetries,
                            cause.getClass().getSimpleName());
                })
                .onRetryExhaustedThrow((spec, signal) -> {
                    return signal.failure();
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
            log.error("LLM调用失败: provider={}, endpoint={}, status={}, errorType={}",
                    provider, endpoint, ex.getStatusCode().value(), ex.getClass().getSimpleName());
            return;
        }
        log.error("LLM调用失败: provider={}, endpoint={}, errorType={}",
                provider,
                endpoint,
                cause.getClass().getSimpleName());
    }

    private Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private LLMException buildLlmException(
            String provider,
            String endpoint,
            String model,
            Throwable error,
            int attemptCount,
            int retryCount) {
        Throwable cause = unwrap(error);
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("provider", provider);
        diagnostics.put("endpoint", endpoint);
        diagnostics.put("model", model);
        diagnostics.put("timeoutSeconds", properties.getTimeout());
        diagnostics.put("maxRetries", properties.getMaxRetries());
        diagnostics.put("attemptCount", attemptCount);
        diagnostics.put("retryCount", retryCount);
        diagnostics.put("retryExhausted",
                properties.getMaxRetries() > 0
                        && retryCount >= properties.getMaxRetries()
                        && isRetryableError(cause));
        diagnostics.put("errorType", cause.getClass().getSimpleName());
        diagnostics.put("errorCategory", diagnosticCategory(error, cause));
        if (cause instanceof WebClientResponseException ex) {
            diagnostics.put("httpStatus", ex.getStatusCode().value());
        }
        return new LLMException("LLM provider request failed", cause, diagnostics);
    }

    private String diagnosticCategory(Throwable error, Throwable cause) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof LLMException llmException) {
                Object category = llmException.diagnostics().get("errorCategory");
                if (category instanceof String value && !value.isBlank()) {
                    return value;
                }
            }
            if (current.getCause() == null || current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return classifyLlmError(cause);
    }

    private String classifyLlmError(Throwable cause) {
        if (cause instanceof WebClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            if (statusCode == 429) {
                return "rate_limit";
            }
            if (statusCode >= 500) {
                return "provider_5xx";
            }
            return "provider_http_error";
        }
        if (cause instanceof TimeoutException || String.valueOf(cause.getMessage()).toLowerCase(Locale.ROOT).contains("timeout")) {
            return "timeout";
        }
        if (cause instanceof IOException || cause instanceof ConnectException) {
            return "network";
        }
        return "unknown";
    }

    /**
     * 解析 OpenAI 响应
     */
    private String parseOpenAIResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw invalidResponse();
            }
            String content = choices.get(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw invalidResponse();
            }
            JsonNode usage = root.path("usage");
            telemetry.currentTokenUsage(
                    integralLong(usage.path("prompt_tokens")),
                    integralLong(usage.path("completion_tokens")));
            return content;
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: errorType={}", e.getClass().getSimpleName());
            if (e instanceof LLMException llmException) {
                throw llmException;
            }
            throw invalidResponse(e);
        }
    }

    /**
     * 解析通义千问响应
     */
    private String parseQwenResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("output").path("text").asText("");
            if (content.isBlank()) {
                throw invalidResponse();
            }
            JsonNode usage = root.path("usage");
            telemetry.currentTokenUsage(
                    integralLong(usage.path("input_tokens")),
                    integralLong(usage.path("output_tokens")));
            return content;
        } catch (Exception e) {
            log.error("Failed to parse Qwen response: errorType={}", e.getClass().getSimpleName());
            if (e instanceof LLMException llmException) {
                throw llmException;
            }
            throw invalidResponse(e);
        }
    }

    private Long integralLong(JsonNode value) {
        return value != null && value.isIntegralNumber() ? value.longValue() : null;
    }

    private LLMException invalidResponse() {
        return invalidResponse(null);
    }

    private LLMException invalidResponse(Throwable cause) {
        return new LLMException(
                "Invalid LLM provider response",
                cause,
                Map.of("errorCategory", "invalid_response"));
    }

    static String sanitizeAnswerText(String answer) {
        if (answer == null || answer.isBlank()) {
            return answer;
        }

        String sanitized = RAW_SOURCE_MARKER_PATTERN.matcher(answer).replaceAll("");
        sanitized = INLINE_SOURCE_MARKER_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = sanitized.replaceAll("([。！？.!?])\\s*中指出", "$1");
        sanitized = sanitized.replaceAll("([。！？.!?])[，,]", "$1");
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
            log.debug("Failed to parse OpenAI stream chunk: errorType={}",
                    e.getClass().getSimpleName());
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
            log.debug("Failed to parse Qwen stream chunk: errorType={}",
                    e.getClass().getSimpleName());
            return "";
        }
    }

    /**
     * 从答案中提取引用来源
     */
    CitationResolution resolveCitations(String query, String answer, List<RetrievedContext> contexts) {
        boolean noAnswer = isNoAnswerText(answer);
        List<Citation> extractedCitations = extractCitations(answer, contexts);
        CitationValidationResult originalValidation = citationValidator.validate(
                extractedCitations,
                contexts,
                noAnswer);

        if (!originalValidation.citations().isEmpty() || !shouldUseCitationFallback(answer, contexts, noAnswer)) {
            return new CitationResolution(originalValidation, false, 0);
        }

        List<Citation> fallbackCitations = buildFallbackCitations(query, answer, contexts);
        CitationValidationResult fallbackValidation = citationValidator.validate(
                fallbackCitations,
                contexts,
                false);
        return new CitationResolution(
                fallbackValidation,
                !fallbackValidation.citations().isEmpty(),
                fallbackValidation.citations().size());
    }

    private List<Citation> extractCitations(String answer, List<RetrievedContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return List.of();
        }

        List<Citation> citations = new ArrayList<>();
        String answerLower = answer == null ? "" : answer.toLowerCase(Locale.ROOT);

        for (RetrievedContext context : contexts) {
            if (context == null || context.content() == null || context.content().isBlank()) {
                continue;
            }
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
                        citations.add(Citation.grounded(
                                context.source(),
                                extractStringMetadata(context.metadata(), "sourceFileName", "originalFilename",
                                        "fileName", "filename"),
                                extractStringMetadata(context.metadata(), "documentTitle", "title"),
                                extractLongMetadata(context.metadata(), "documentId"),
                                context.source(),
                                (double) context.relevanceScore(),
                                sentenceTrimmed,
                                -1,
                                -1));
                        break;
                    }
                }
            }
        }

        return citations;
    }

    private boolean shouldUseCitationFallback(String answer, List<RetrievedContext> contexts, boolean noAnswer) {
        return answer != null
                && !answer.isBlank()
                && !noAnswer
                && contexts != null
                && !contexts.isEmpty();
    }

    private List<Citation> buildFallbackCitations(String query, String answer, List<RetrievedContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return List.of();
        }

        Set<String> seenContexts = new LinkedHashSet<>();
        List<Citation> citations = new ArrayList<>();
        List<RetrievedContext> topContexts = contexts.stream()
                .filter(context -> context != null && context.content() != null && !context.content().isBlank())
                .sorted(Comparator.comparingDouble(RetrievedContext::relevanceScore).reversed())
                .toList();

        for (RetrievedContext context : topContexts) {
            String dedupKey = normalizeForCitationDedup(context.source()) + "|"
                    + normalizeTextForCitationDedup(context.content());
            if (!seenContexts.add(dedupKey)) {
                continue;
            }

            String snippet = selectFallbackSnippet(query, answer, context.content());
            if (snippet.isBlank()) {
                continue;
            }

            citations.add(Citation.grounded(
                    context.source(),
                    extractStringMetadata(context.metadata(), "sourceFileName", "originalFilename", "fileName",
                            "filename"),
                    extractStringMetadata(context.metadata(), "documentTitle", "title"),
                    extractLongMetadata(context.metadata(), "documentId"),
                    context.source(),
                    (double) context.relevanceScore(),
                    snippet,
                    -1,
                    -1));

            if (citations.size() >= MAX_FALLBACK_CITATIONS) {
                break;
            }
        }

        return citations;
    }

    private String selectFallbackSnippet(String query, String answer, String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isBlank()) {
            return "";
        }

        Set<String> queryAnswerTokens = citationTokens((query == null ? "" : query) + " " + (answer == null ? "" : answer));
        String bestSentence = "";
        int bestOverlap = 0;

        for (String sentence : SENTENCE_BOUNDARY_PATTERN.split(normalizedContent)) {
            String candidate = sentence == null ? "" : sentence.trim();
            if (candidate.isBlank()) {
                continue;
            }

            Set<String> sentenceTokens = citationTokens(candidate);
            int overlap = countOverlap(queryAnswerTokens, sentenceTokens);
            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                bestSentence = candidate;
            }
        }

        if (bestOverlap > 0 && !bestSentence.isBlank()) {
            return truncateSnippet(bestSentence);
        }
        return truncateSnippet(normalizedContent);
    }

    private int countOverlap(Set<String> left, Set<String> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        int overlap = 0;
        for (String token : left) {
            if (right.contains(token)) {
                overlap++;
            }
        }
        return overlap;
    }

    private Set<String> citationTokens(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        Matcher latinMatcher = LATIN_TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (latinMatcher.find()) {
            String token = latinMatcher.group();
            if (token.length() >= 2) {
                result.add(token);
            }
        }

        Matcher cjkMatcher = CJK_SEGMENT_PATTERN.matcher(text);
        while (cjkMatcher.find()) {
            String segment = cjkMatcher.group();
            if (segment.length() == 1) {
                result.add(segment);
                continue;
            }
            for (int i = 0; i < segment.length() - 1; i++) {
                result.add(segment.substring(i, i + 2));
            }
        }

        return result;
    }

    private String truncateSnippet(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= FALLBACK_SNIPPET_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, FALLBACK_SNIPPET_MAX_LENGTH).trim();
    }

    private Long extractLongMetadata(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractStringMetadata(Map<String, Object> metadata, String... keys) {
        if (metadata == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private boolean isNoAnswerText(String answer) {
        if (answer == null || answer.isBlank()) {
            return true;
        }
        String normalized = answer.toLowerCase(Locale.ROOT);
        return normalized.contains("未能找到")
                || normalized.contains("上下文未包含")
                || normalized.contains("没有足够")
                || normalized.contains("无法回答")
                || normalized.contains("无法根据现有内容回答")
                || normalized.contains("知识库中没有")
                || normalized.contains("context doesn't contain")
                || normalized.contains("not enough information")
                || normalized.contains("cannot answer");
    }

    private String normalizeForCitationDedup(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeTextForCitationDedup(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 截断日志输出
     */
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

                int lastBracket = pending.lastIndexOf("[");
                if (lastBracket < 0) {
                    emitted.add(pending.toString());
                    pending.setLength(0);
                    break;
                }

                String suffix = pending.substring(lastBracket);
                if (SOURCE_MARKER_PREFIX.startsWith(suffix)) {
                    if (lastBracket == 0) {
                        break;
                    }
                    emitted.add(pending.substring(0, lastBracket));
                    pending.delete(0, lastBracket);
                    break;
                }

                emitted.add(pending.toString());
                pending.setLength(0);
                break;
            }

            return emitted.stream()
                    .map(AnswerGeneratorImpl::sanitizeAnswerText)
                    .filter(text -> text != null && !text.isEmpty())
                    .toList();
        }
    }

    record CitationResolution(
            CitationValidationResult validation,
            boolean fallbackUsed,
            int fallbackCount) {
    }
}
