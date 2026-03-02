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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;

/**
 * 答案生成器实现
 * 支持 OpenAI 和通义千问 API
 */
@Slf4j
@Service
public class AnswerGeneratorImpl implements AnswerGenerator {

    private final LLMProperties properties;
    private final PromptBuilder promptBuilder;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AnswerGeneratorImpl(LLMProperties properties, PromptBuilder promptBuilder) {
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Override
    public GeneratedAnswer generate(String query, List<RetrievedContext> contexts) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or blank");
        }

        log.debug("Generating answer for query: {}", truncateForLog(query));

        // 构建 Prompt
        String prompt = promptBuilder.build(query, contexts, PromptStrategy.STRUCTURED);

        // 调用 LLM API
        String answer = callLLM(prompt);

        // 提取引用来源
        List<Citation> citations = extractCitations(answer, contexts);

        // 构建元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model", getModelName());
        metadata.put("contextCount", contexts != null ? contexts.size() : 0);

        return GeneratedAnswer.of(answer, citations, metadata);
    }


    @Override
    public Flux<String> generateStream(String query, List<RetrievedContext> contexts) {
        if (query == null || query.isBlank()) {
            return Flux.error(new IllegalArgumentException("Query cannot be null or blank"));
        }

        log.debug("Generating streaming answer for query: {}", truncateForLog(query));

        String prompt = promptBuilder.build(query, contexts, PromptStrategy.STRUCTURED);

        return callLLMStream(prompt);
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
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());

        String response = webClient.post()
                .uri(config.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(properties.getTimeout()))
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
                        Map.of("role", "user", "content", prompt)
                )
        ));
        requestBody.put("parameters", Map.of(
                "temperature", config.getTemperature(),
                "max_tokens", config.getMaxTokens()
        ));

        String response = webClient.post()
                .uri(config.getBaseUrl() + "/services/aigc/text-generation/generation")
                .header("Authorization", "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(properties.getTimeout()))
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
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());
        requestBody.put("stream", true);

        return webClient.post()
                .uri(config.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(properties.getTimeout()))
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
                        Map.of("role", "user", "content", prompt)
                )
        ));
        requestBody.put("parameters", Map.of(
                "temperature", config.getTemperature(),
                "max_tokens", config.getMaxTokens(),
                "incremental_output", true
        ));

        return webClient.post()
                .uri(config.getBaseUrl() + "/services/aigc/text-generation/generation")
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("X-DashScope-SSE", "enable")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(properties.getTimeout()))
                .map(this::parseQwenStreamChunk)
                .filter(content -> content != null && !content.isEmpty());
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
        if (text == null) return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
