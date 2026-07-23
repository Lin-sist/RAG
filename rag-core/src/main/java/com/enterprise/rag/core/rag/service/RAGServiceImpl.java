package com.enterprise.rag.core.rag.service;

import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.util.RedisUtil;
import com.enterprise.rag.common.trace.GenAiTelemetry;
import com.enterprise.rag.core.rag.generator.AnswerGenerator;
import com.enterprise.rag.core.rag.generator.LLMException;
import com.enterprise.rag.core.rag.model.*;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.query.RetrievalResult;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 服务实现
 * 协调检索和生成流程，提供完整的问答能力
 */
@Slf4j
@Service
public class RAGServiceImpl implements RAGService {
    private static final Pattern LEADING_CONVERSATIONAL_PATTERN = Pattern.compile(
            "^(请问|请教一下|请教|想问一下|想问|麻烦问下|麻烦问一下|帮我|请你|请|你认为|你觉得|你看|可以说说|说说|聊聊|分析一下|分析下|帮忙分析一下|帮忙分析下)\\s*");
    private static final Pattern HOW_PATTERN = Pattern.compile("^(.+?)\\s*(?:是)?(?:如何|怎么)(?:运作|工作|运行|实现|发挥作用)的?$");
    private static final Pattern WHY_PATTERN = Pattern.compile("^为什么(?:需要|要|会)?\\s*(.+)$");
    private static final Pattern WHY_SUBJECT_PATTERN = Pattern.compile("^(.+?)\\s*为什么(?:重要|需要|有用)$");
    private static final Pattern PRINCIPLE_PATTERN = Pattern.compile(
            "^(.+?)\\s*(?:的)?(?:工作原理|运行原理|原理|机制|流程|作用|实现方式)(?:是什么)?$");
    private static final float EXPLANATORY_FALLBACK_MIN_SCORE = 0.15f;

    private final QueryEngine queryEngine;
    private final AnswerGenerator answerGenerator;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final GenAiTelemetry telemetry;

    @Autowired
    public RAGServiceImpl(QueryEngine queryEngine,
            AnswerGenerator answerGenerator,
            RedisUtil redisUtil,
            ObjectMapper objectMapper,
            GenAiTelemetry telemetry) {
        this.queryEngine = queryEngine;
        this.answerGenerator = answerGenerator;
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
        this.telemetry = telemetry == null ? GenAiTelemetry.noop() : telemetry;
    }

    public RAGServiceImpl(QueryEngine queryEngine,
            AnswerGenerator answerGenerator,
            RedisUtil redisUtil,
            ObjectMapper objectMapper) {
        this(queryEngine, answerGenerator, redisUtil, objectMapper, GenAiTelemetry.noop());
    }

    @Override
    public QAResponse ask(QARequest request) {
        try (GenAiTelemetry.SpanScope askSpan = telemetry.startRoot(
                GenAiTelemetry.SpanNames.ASK,
                Map.of(GenAiTelemetry.Attributes.OPERATION, "ask"),
                null)) {
            return askWithinTrace(request, askSpan);
        }
    }

    private QAResponse askWithinTrace(QARequest request, GenAiTelemetry.SpanScope askSpan) {
        String question = request.question();
        String collectionName = request.collectionName();

        if (question == null || question.isBlank()) {
            askSpan.outcome("INVALID_REQUEST");
            return QAResponse.error(question, "问题不能为空");
        }

        if (collectionName == null || collectionName.isBlank()) {
            askSpan.outcome("INVALID_REQUEST");
            return QAResponse.error(question, "知识库名称不能为空");
        }

        log.info("Processing QA request for collection: {}", collectionName);

        try {
            // 1. 检查缓存
            String modelName = answerGenerator.getModelName();
            if (request.enableCache()) {
                QAResponse cachedResponse = traceStage(GenAiTelemetry.SpanNames.CACHE_LOOKUP,
                        () -> getFromCache(question, collectionName, request.topK(), request.filter(), modelName));
                if (cachedResponse != null) {
                    log.debug("QA cache hit for collection: {}", collectionName);
                    askSpan.outcome("CACHE_HIT");
                    return addCacheMetadata(cachedResponse, true);
                }
            }

            // 2. 检索相关文档
            RetrieveOptions retrieveOptions = new RetrieveOptions(
                    collectionName,
                    request.topK(),
                    request.minScore(),
                    request.filter(),
                    true);
            RetrievalResult retrievalResult = traceRetrieval(request.topK(), () -> {
                RetrievalResult initial = retrieveWithDiagnostics(question, retrieveOptions);
                return initial.contexts().isEmpty()
                        ? retryExplanatoryRetrieval(question, request, collectionName, initial)
                        : initial;
            });
            List<RetrievedContext> contexts = retrievalResult.contexts();
            log.debug("Retrieved {} contexts for question", contexts.size());
            log.debug("Top retrieval scores: {}", topScoresForLog(contexts, 5));

            if (contexts.isEmpty()) {
                log.info("No relevant contexts found for collection: {}", collectionName);
                askSpan.outcome("NO_RESULT");
                return QAResponse.noResult(question);
            }

            // 4. 生成答案
            recordLineage(askSpan, contexts, request.topK());
            List<RetrievedContext> selectedContexts = contexts;
            GeneratedAnswer generatedAnswer = traceStage(GenAiTelemetry.SpanNames.GENERATION,
                    () -> answerGenerator.generate(question, selectedContexts));

            // 5. 构建响应
            Map<String, Object> metadata = new HashMap<>(generatedAnswer.metadata());
            metadata.putAll(retrievalResult.diagnostics());
            metadata.put("cached", false);
            metadata.putIfAbsent("contextCount", contexts.size());
            copyCitationValidationSummary(metadata);
            metadata.put("retrievedContextCount", contexts.size());
            metadata.put("retrievedTopScore", contexts.stream()
                    .mapToDouble(RetrievedContext::relevanceScore)
                    .max()
                    .orElse(0.0d));
            metadata.put("retrievedAvgScore", contexts.stream()
                    .mapToDouble(RetrievedContext::relevanceScore)
                    .average()
                    .orElse(0.0d));

            QAResponse response = QAResponse.success(
                    question,
                    generatedAnswer.answer(),
                    generatedAnswer.citations(),
                    contexts,
                    metadata);

            // 6. 缓存结果
            if (request.enableCache() && !retrievalResult.degraded()) {
                saveToCache(question, collectionName, request.topK(), request.filter(), modelName, response);
            }

            log.info("Successfully generated answer for collection: {}", collectionName);
            askSpan.outcome("SUCCESS");
            return response;

        } catch (Exception e) {
            log.error("Failed to process QA request: errorType={}", e.getClass().getSimpleName());
            askSpan.safeError(e, classifyClientError(e), "ASK_FAILED").outcome("ERROR");
            return QAResponse.error(question, toClientErrorMessage(e), errorMetadata(e));
        }
    }

    private <T> T traceStage(String spanName, java.util.function.Supplier<T> action) {
        try (GenAiTelemetry.SpanScope stage = telemetry.startSpan(spanName, Map.of())) {
            try {
                T result = action.get();
                stage.outcome("SUCCESS");
                return result;
            } catch (RuntimeException failure) {
                stage.safeError(failure, "ask", "STAGE_FAILED").outcome("ERROR");
                throw failure;
            }
        }
    }

    private RetrievalResult traceRetrieval(int topK,
            java.util.function.Supplier<RetrievalResult> action) {
        try (GenAiTelemetry.SpanScope stage = telemetry.startSpan(
                GenAiTelemetry.SpanNames.RETRIEVAL, Map.of())) {
            try {
                RetrievalResult result = action.get();
                stage.diagnostics(result.diagnostics())
                        .longFact(GenAiTelemetry.Attributes.TOP_K, topK)
                        .longFact(GenAiTelemetry.Attributes.SELECTED_COUNT, result.contexts().size())
                        .stringFact(GenAiTelemetry.Attributes.RETRIEVAL_ROUTE,
                                stringMetadata(result.diagnostics().get("retrievalMode")))
                        .outcome("SUCCESS");
                return result;
            } catch (RuntimeException failure) {
                stage.safeError(failure, "retrieval", "RETRIEVAL_FAILED").outcome("ERROR");
                throw failure;
            }
        }
    }

    private void recordLineage(GenAiTelemetry.SpanScope askSpan,
            List<RetrievedContext> contexts,
            int topK) {
        int limit = Math.min(Math.max(0, topK), contexts.size());
        for (int index = 0; index < limit; index++) {
            RetrievedContext context = contexts.get(index);
            Map<String, Object> metadata = context.metadata() == null ? Map.of() : context.metadata();
            String taskId = stringMetadata(metadata.get("ingestTaskId"));
            Long documentId = longMetadata(metadata.get("documentId"));
            String chunkId = stringMetadata(metadata.get("chunkId"));
            String status = taskId != null && documentId != null && chunkId != null
                    ? "COMPLETE"
                    : (documentId != null || chunkId != null ? "PARTIAL" : "MISSING");
            askSpan.lineageContext(taskId, documentId, chunkId, index + 1L,
                    context.relevanceScore(), status);
        }
    }

    private String stringMetadata(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longMetadata(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public Flux<String> askStream(QARequest request) {
        String question = request.question();
        String collectionName = request.collectionName();

        if (question == null || question.isBlank()) {
            return Flux.error(new IllegalArgumentException("问题不能为空"));
        }

        if (collectionName == null || collectionName.isBlank()) {
            return Flux.error(new IllegalArgumentException("知识库名称不能为空"));
        }

        return Flux.deferContextual(contextView -> askStreamSubscription(
                request,
                contextView.getOrDefault(RAGService.STREAM_TERMINAL_SIGNAL_CONTEXT_KEY, null)));
    }

    private Flux<String> askStreamSubscription(QARequest request,
            RAGService.StreamTerminalSignal terminalSignal) {
        String question = request.question();
        String collectionName = request.collectionName();
        GenAiTelemetry.SpanScope askSpan = telemetry.startRoot(
                GenAiTelemetry.SpanNames.ASK,
                Map.of(GenAiTelemetry.Attributes.OPERATION, "ask_stream"),
                null);

        log.info("Processing streaming QA request for collection: {}", collectionName);

        try {
            long retrievalStartTime = System.currentTimeMillis();

            // 检索相关文档
            RetrieveOptions retrieveOptions = new RetrieveOptions(
                    collectionName,
                    request.topK(),
                    request.minScore(),
                    request.filter(),
                    true);
            RetrievalResult retrievalResult = traceRetrieval(request.topK(), () -> {
                RetrievalResult initial = retrieveWithDiagnostics(question, retrieveOptions);
                return initial.contexts().isEmpty()
                        ? retryExplanatoryRetrieval(question, request, collectionName, initial)
                        : initial;
            });
            List<RetrievedContext> contexts = retrievalResult.contexts();
            long retrievalLatencyMs = System.currentTimeMillis() - retrievalStartTime;

            log.info("stream_retrieval_done collection={}, retrievalLatencyMs={}, contextCount={}, topScores={}",
                    collectionName,
                    retrievalLatencyMs,
                    contexts.size(),
                    topScoresForLog(contexts, 5));
            if (retrievalResult.degraded()) {
                log.warn("stream_retrieval_degraded dependency=milvus, retrievalMode=keyword_only, failMode=open");
            }

            if (contexts.isEmpty()) {
                log.info("stream_retrieval_no_context collection={}, retrievalLatencyMs={}",
                        collectionName, retrievalLatencyMs);
                askSpan.detach();
                return finishStream(
                        Flux.just("抱歉，未能找到与您问题相关的信息。请尝试换一种方式提问或提供更多细节。"),
                        askSpan, null, "NO_RESULT", terminalSignal);
            }

            // 流式生成答案
            recordLineage(askSpan, contexts, request.topK());
            GenAiTelemetry.SpanScope generation = telemetry.startSpan(
                    GenAiTelemetry.SpanNames.GENERATION, Map.of());
            Flux<String> stream;
            try {
                stream = answerGenerator.generateStream(question, contexts);
            } catch (RuntimeException failure) {
                generation.safeError(failure, "ask", "GENERATION_FAILED").finish("ERROR");
                throw failure;
            }
            generation.detach();
            askSpan.detach();
            return finishStream(stream, askSpan, generation, "SUCCESS", terminalSignal);

        } catch (Exception e) {
            log.error("Failed to process streaming QA request: errorType={}",
                    e.getClass().getSimpleName());
            askSpan.safeError(e, classifyClientError(e), "ASK_STREAM_FAILED").finish("ERROR");
            return Flux.error(new IllegalStateException(toClientErrorMessage(e), e));
        }
    }

    private Flux<String> finishStream(Flux<String> source,
            GenAiTelemetry.SpanScope askSpan,
            GenAiTelemetry.SpanScope generationSpan,
            String completedOutcome,
            RAGService.StreamTerminalSignal terminalSignal) {
        java.util.concurrent.atomic.AtomicReference<String> errorOutcome =
                new java.util.concurrent.atomic.AtomicReference<>("ERROR");
        return source
                .doOnError(failure -> {
                    if (isTimeoutFailure(failure)) {
                        errorOutcome.set("TIMEOUT");
                    }
                    askSpan.safeError(failure, "ask", "ASK_STREAM_FAILED");
                    if (generationSpan != null) {
                        generationSpan.safeError(failure, "ask", "GENERATION_FAILED");
                    }
                })
                .doFinally(signal -> {
                    String outcome = switch (signal) {
                        case CANCEL -> terminalSignal != null && terminalSignal.isTimeout()
                                ? "TIMEOUT" : "CANCELLED";
                        case ON_ERROR -> errorOutcome.get();
                        default -> completedOutcome;
                    };
                    if (generationSpan != null) {
                        generationSpan.finish(outcome);
                    }
                    askSpan.finish(outcome);
                });
    }

    private boolean isTimeoutFailure(Throwable failure) {
        LLMException llmException = findLlmException(failure);
        if (llmException != null
                && "timeout".equals(String.valueOf(llmException.diagnostics().get("errorCategory")))) {
            return true;
        }
        Throwable current = failure;
        while (current != null) {
            if (current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String toClientErrorMessage(Exception e) {
        VectorDependencyException vectorException = findVectorException(e);
        if (vectorException != null) {
            return vectorException.getMessage();
        }
        LLMException llmException = findLlmException(e);
        if (llmException != null) {
            return toLlmClientErrorMessage(llmException.diagnostics());
        }
        String message = e != null ? e.getMessage() : null;
        if (message == null || message.isBlank()) {
            return "问答服务暂时不可用，请稍后重试";
        }
        String lower = message.toLowerCase();
        if (lower.contains("collection not found")) {
            return "知识库向量索引不存在，请重新上传文档以重建索引";
        }
        if (lower.contains("max retries exceeded")) {
            return "模型服务当前不稳定（重试耗尽），请稍后重试";
        }
        if (lower.contains("too many requests") || lower.contains(" 429 ") || lower.startsWith("429")) {
            return "模型服务触发限流，请稍后重试";
        }
        if (lower.contains("timeout")) {
            return "模型服务响应超时，请稍后重试";
        }
        return message;
    }

    private RetrievalResult retrieveWithDiagnostics(String question, RetrieveOptions options) {
        RetrievalResult result = queryEngine.retrieveWithDiagnostics(question, options);
        return result != null
                ? result
                : RetrievalResult.complete(queryEngine.retrieve(question, options));
    }

    private String toLlmClientErrorMessage(Map<String, Object> diagnostics) {
        String category = String.valueOf(diagnostics.getOrDefault("errorCategory", "unknown"));
        return switch (category) {
            case "rate_limit" -> "模型服务触发限流，请稍后重试";
            case "timeout" -> "模型服务响应超时，请稍后重试";
            case "provider_5xx" -> Boolean.TRUE.equals(diagnostics.get("retryExhausted"))
                    ? "模型服务当前不稳定（重试耗尽），请稍后重试"
                    : "模型服务当前不可用，请稍后重试";
            case "network" -> "模型服务网络连接失败，请稍后重试";
            case "provider_http_error" -> "模型服务请求未被接受，请检查配置后重试";
            case "invalid_response" -> "模型服务返回了无效响应，请稍后重试";
            default -> "问答服务暂时不可用，请稍后重试";
        };
    }

    private Map<String, Object> errorMetadata(Exception e) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("errorType", e.getClass().getSimpleName());
        metadata.put("errorCategory", classifyClientError(e));

        VectorDependencyException vectorException = findVectorException(e);
        if (vectorException != null) {
            metadata.put("errorCode", vectorException.getErrorCode());
            metadata.put("vectorDiagnostics", vectorException.diagnostics());
        }

        LLMException llmException = findLlmException(e);
        if (llmException != null && !llmException.diagnostics().isEmpty()) {
            Map<String, Object> diagnostics = llmException.diagnostics();
            metadata.put("llmDiagnostics", diagnostics);
            copyDiagnostic(metadata, diagnostics, "provider", "llmProvider");
            copyDiagnostic(metadata, diagnostics, "endpoint", "llmEndpoint");
            copyDiagnostic(metadata, diagnostics, "model", "llmModel");
            copyDiagnostic(metadata, diagnostics, "timeoutSeconds", "llmTimeoutSeconds");
            copyDiagnostic(metadata, diagnostics, "maxRetries", "llmMaxRetries");
            copyDiagnostic(metadata, diagnostics, "attemptCount", "llmAttemptCount");
            copyDiagnostic(metadata, diagnostics, "retryCount", "llmRetryCount");
            copyDiagnostic(metadata, diagnostics, "retryExhausted", "llmRetryExhausted");
            copyDiagnostic(metadata, diagnostics, "errorType", "llmErrorType");
            copyDiagnostic(metadata, diagnostics, "errorCategory", "llmErrorCategory");
            copyDiagnostic(metadata, diagnostics, "httpStatus", "llmHttpStatus");
        }

        return metadata;
    }

    private void copyDiagnostic(
            Map<String, Object> target,
            Map<String, Object> source,
            String sourceKey,
            String targetKey) {
        Object value = source.get(sourceKey);
        if (value != null) {
            target.put(targetKey, value);
        }
    }

    private LLMException findLlmException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof LLMException llmException) {
                return llmException;
            }
            current = current.getCause();
        }
        return null;
    }

    private VectorDependencyException findVectorException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof VectorDependencyException vectorException) {
                return vectorException;
            }
            current = current.getCause();
        }
        return null;
    }

    private String classifyClientError(Exception e) {
        String message = e != null ? String.valueOf(e.getMessage()).toLowerCase() : "";
        if (findLlmException(e) != null) {
            return "llm";
        }
        VectorDependencyException vectorException = findVectorException(e);
        if (vectorException != null) {
            return vectorException.getErrorCategory();
        }
        if (message.contains("collection not found")) {
            return "vector_index";
        }
        if (message.contains("too many requests") || message.contains(" 429 ") || message.startsWith("429")) {
            return "rate_limit";
        }
        if (message.contains("timeout")) {
            return "timeout";
        }
        return "unknown";
    }

    @SuppressWarnings("unchecked")
    private void copyCitationValidationSummary(Map<String, Object> metadata) {
        Object value = metadata.get("citationValidation");
        if (!(value instanceof Map<?, ?> validation)) {
            return;
        }

        Object validCitations = validation.get("validCitations");
        Object droppedCitations = validation.get("droppedCitations");
        Object citationCoverage = validation.get("citationCoverage");
        if (validCitations != null) {
            metadata.put("validCitations", validCitations);
        }
        if (droppedCitations != null) {
            metadata.put("droppedCitations", droppedCitations);
        }
        if (citationCoverage != null) {
            metadata.put("citationCoverage", citationCoverage);
        }
    }

    private RetrievalResult retryExplanatoryRetrieval(
            String question,
            QARequest request,
            String collectionName,
            RetrievalResult initialResult) {
        if (!isExplanatoryQuestion(question)) {
            return initialResult;
        }

        List<String> fallbackQueries = buildExplanatoryFallbackQueries(question);
        if (fallbackQueries.isEmpty()) {
            return initialResult;
        }

        float fallbackMinScore = Math.min(request.minScore(), EXPLANATORY_FALLBACK_MIN_SCORE);
        if (fallbackMinScore <= 0f) {
            fallbackMinScore = request.minScore();
        }

        List<RetrievalResult> attempts = new ArrayList<>();
        attempts.add(initialResult);
        for (String fallbackQuery : fallbackQueries) {
            RetrieveOptions fallbackOptions = new RetrieveOptions(
                    collectionName,
                    request.topK(),
                    fallbackMinScore,
                    request.filter(),
                    true);
            RetrievalResult fallbackResult = retrieveWithDiagnostics(fallbackQuery, fallbackOptions);
            attempts.add(fallbackResult);
            List<RetrievedContext> fallbackContexts = fallbackResult.contexts();
            if (!fallbackContexts.isEmpty()) {
                log.info("explanatory_retrieval_fallback_hit fallbackMinScore={}, contextCount={}",
                        fallbackMinScore, fallbackContexts.size());
                List<RetrievedContext> contexts = fallbackContexts.stream()
                        .sorted(Comparator.comparingDouble(RetrievedContext::relevanceScore).reversed())
                        .limit(request.topK())
                        .toList();
                return withAccumulatedRerankFacts(new RetrievalResult(contexts, fallbackResult.diagnostics()), attempts);
            }
        }

        RetrievalResult lastAttempt = attempts.get(attempts.size() - 1);
        return withAccumulatedRerankFacts(lastAttempt, attempts);
    }

    private RetrievalResult withAccumulatedRerankFacts(
            RetrievalResult effectiveResult,
            List<RetrievalResult> attempts) {
        Map<String, Object> diagnostics = new LinkedHashMap<>(effectiveResult.diagnostics());
        diagnostics.put("rerankModelCallCount", attempts.stream()
                .map(RetrievalResult::diagnostics)
                .mapToInt(facts -> intDiagnostic(facts, "rerankModelCallCount"))
                .sum());
        diagnostics.put("rerankFallbackCount", attempts.stream()
                .map(RetrievalResult::diagnostics)
                .mapToInt(facts -> intDiagnostic(facts, "rerankFallbackCount"))
                .sum());
        diagnostics.put("rerankLatencyMillis", attempts.stream()
                .map(RetrievalResult::diagnostics)
                .mapToLong(facts -> longDiagnostic(facts, "rerankLatencyMillis"))
                .sum());
        return new RetrievalResult(effectiveResult.contexts(), diagnostics);
    }

    private int intDiagnostic(Map<String, Object> diagnostics, String key) {
        Object value = diagnostics.get(key);
        return value instanceof Number number ? Math.max(0, number.intValue()) : 0;
    }

    private long longDiagnostic(Map<String, Object> diagnostics, String key) {
        Object value = diagnostics.get(key);
        return value instanceof Number number ? Math.max(0L, number.longValue()) : 0L;
    }

    private boolean isExplanatoryQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }

        return question.contains("如何")
                || question.contains("怎么")
                || question.contains("为什么")
                || question.contains("原理")
                || question.contains("机制")
                || question.contains("流程")
                || question.contains("运作")
                || question.contains("工作")
                || question.contains("运行");
    }

    private List<String> buildExplanatoryFallbackQueries(String question) {
        String normalized = normalizeQuestion(question);
        String stripped = LEADING_CONVERSATIONAL_PATTERN.matcher(normalized).replaceFirst("");
        LinkedHashMap<String, Boolean> queries = new LinkedHashMap<>();

        addFallbackQuery(queries, stripped);

        String subject = matchFallbackSubject(HOW_PATTERN, stripped);
        if (subject != null) {
            addFallbackQuery(queries, subject);
            addFallbackQuery(queries, subject + " 工作原理");
            addFallbackQuery(queries, subject + " 运行流程");
            return List.copyOf(queries.keySet());
        }

        subject = matchFallbackSubject(WHY_PATTERN, stripped);
        if (subject != null) {
            addFallbackQuery(queries, subject);
            addFallbackQuery(queries, subject + " 作用");
            addFallbackQuery(queries, subject + " 目的");
            return List.copyOf(queries.keySet());
        }

        subject = matchFallbackSubject(WHY_SUBJECT_PATTERN, stripped);
        if (subject != null) {
            addFallbackQuery(queries, subject);
            addFallbackQuery(queries, subject + " 作用");
            addFallbackQuery(queries, subject + " 目的");
            return List.copyOf(queries.keySet());
        }

        subject = matchFallbackSubject(PRINCIPLE_PATTERN, stripped);
        if (subject != null) {
            addFallbackQuery(queries, subject);
            addFallbackQuery(queries, subject + " 原理");
            addFallbackQuery(queries, subject + " 机制");
            return List.copyOf(queries.keySet());
        }

        return List.copyOf(queries.keySet());
    }

    private void addFallbackQuery(Map<String, Boolean> queries, String candidate) {
        String normalized = normalizeQuestion(candidate);
        if (!normalized.isBlank()) {
            queries.put(normalized, Boolean.TRUE);
        }
    }

    private String matchFallbackSubject(Pattern pattern, String question) {
        Matcher matcher = pattern.matcher(question);
        if (!matcher.matches()) {
            return null;
        }
        return normalizeQuestion(matcher.group(1));
    }

    private String normalizeQuestion(String question) {
        if (question == null) {
            return "";
        }
        return question.trim()
                .replaceAll("[\\s？?！!。,.，；;：:]+$", "")
                .replaceAll("\\s+", " ");
    }

    @Override
    public void evictCache(String question, String collectionName) {
        String queryHash = hashString(question.toLowerCase().trim());
        String pattern = RedisKeyConstants.QA_CACHE_PREFIX + queryHash + ":" + collectionName + ":*";
        try {
            redisUtil.deleteByPattern(pattern);
            log.debug("Evicted QA cache for collection: {}", collectionName);
        } catch (Exception e) {
            log.warn("QA cache eviction degraded: dependency=redis, subsystem=qa_cache, "
                            + "operation=delete, failMode=open, errorType={}",
                    e.getClass().getSimpleName());
        }
    }

    @Override
    public void clearAllCache() {
        try {
            redisUtil.deleteByPattern(RedisKeyConstants.QA_CACHE_PREFIX + "*");
            log.info("Cleared all QA cache");
        } catch (Exception e) {
            log.warn("QA cache clear degraded: dependency=redis, subsystem=qa_cache, "
                            + "operation=clear, failMode=open, errorType={}",
                    e.getClass().getSimpleName());
        }
    }

    /**
     * 从缓存获取响应
     */
    private QAResponse getFromCache(String question,
            String collectionName,
            int topK,
            Map<String, Object> filter,
        String modelName) {
        String cacheKey = buildCacheKey(question, collectionName, topK, filter, modelName);
        try {
            String cachedJson = redisUtil.getString(cacheKey);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, QAResponse.class);
            }
        } catch (Exception e) {
            log.warn("QA cache read degraded: dependency=redis, subsystem=qa_cache, "
                            + "operation=read, failMode=open, errorType={}",
                    e.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * 保存响应到缓存
     */
    private void saveToCache(String question,
            String collectionName,
            int topK,
            Map<String, Object> filter,
            String modelName,
            QAResponse response) {
        String cacheKey = buildCacheKey(question, collectionName, topK, filter, modelName);
        try {
            String json = objectMapper.writeValueAsString(response);
            redisUtil.setString(cacheKey, json, RedisKeyConstants.QA_CACHE_TTL, TimeUnit.SECONDS);
            log.debug("Cached QA response for collection: {}", collectionName);
        } catch (Exception e) {
            log.warn("QA cache write degraded: dependency=redis, subsystem=qa_cache, "
                            + "operation=write, failMode=open, errorType={}",
                    e.getClass().getSimpleName());
        }
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(String question,
            String collectionName,
            int topK,
            Map<String, Object> filter,
            String modelName) {
        String queryHash = hashString(question.toLowerCase().trim());
        String filterHash = hashString(canonicalizeFilter(filter));
        String optionHash = hashString(topK + "|" + modelName + "|" + filterHash);
        return RedisKeyConstants.QA_CACHE_PREFIX + queryHash + ":" + collectionName + ":" + optionHash;
    }

    private String canonicalizeFilter(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return "{}";
        }

        try {
            Object normalized = normalizeFilterValue(filter);
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            log.debug("Failed to canonicalize filter, using fallback toString: errorType={}",
                    e.getClass().getSimpleName());
            return new TreeMap<>(filter).toString();
        }
    }

    @SuppressWarnings("unchecked")
    private Object normalizeFilterValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>(Comparator.naturalOrder());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sorted.put(String.valueOf(entry.getKey()), normalizeFilterValue(entry.getValue()));
            }
            return sorted;
        }

        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            for (Object item : list) {
                normalized.add(normalizeFilterValue(item));
            }
            return normalized;
        }

        return value;
    }

    /**
     * 计算字符串的 SHA-256 哈希
     */
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16); // 使用前16位
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 添加缓存元数据
     */
    private QAResponse addCacheMetadata(QAResponse response, boolean cached) {
        Map<String, Object> newMetadata = new HashMap<>(response.metadata());
        newMetadata.put("cached", cached);
        return new QAResponse(
                response.question(),
                response.answer(),
                response.citations(),
                response.contexts(),
                newMetadata);
    }

    /**
     * 截断日志输出
     */
    private String topScoresForLog(List<RetrievedContext> contexts, int limit) {
        if (contexts == null || contexts.isEmpty()) {
            return "[]";
        }

        return contexts.stream()
                .map(RetrievedContext::relevanceScore)
                .sorted(Comparator.reverseOrder())
                .limit(Math.max(1, limit))
                .map(score -> String.format("%.4f", score))
                .toList()
                .toString();
    }
}
