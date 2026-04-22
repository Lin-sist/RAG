package com.enterprise.rag.core.rag.service;

import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.util.RedisUtil;
import com.enterprise.rag.core.rag.generator.AnswerGenerator;
import com.enterprise.rag.core.rag.model.*;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
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

    @Override
    public QAResponse ask(QARequest request) {
        String question = request.question();
        String collectionName = request.collectionName();

        if (question == null || question.isBlank()) {
            return QAResponse.error(question, "问题不能为空");
        }

        if (collectionName == null || collectionName.isBlank()) {
            return QAResponse.error(question, "知识库名称不能为空");
        }

        log.info("Processing QA request for collection: {}, question: {}",
                collectionName, truncateForLog(question));

        try {
            // 1. 检查缓存
            String modelName = answerGenerator.getModelName();
            if (request.enableCache()) {
                QAResponse cachedResponse = getFromCache(question, collectionName, request.topK(), request.filter(),
                        modelName);
                if (cachedResponse != null) {
                    log.debug("Cache hit for question: {}", truncateForLog(question));
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
            List<RetrievedContext> contexts = queryEngine.retrieve(question, retrieveOptions);
            log.debug("Retrieved {} contexts for question", contexts.size());
            log.debug("Top retrieval scores: {}", topScoresForLog(contexts, 5));

            // 3. 处理无结果情况
            if (contexts.isEmpty()) {
                contexts = retryExplanatoryRetrieval(question, request, collectionName);
            }

            if (contexts.isEmpty()) {
                log.info("No relevant contexts found for question: {}", truncateForLog(question));
                return QAResponse.noResult(question);
            }

            // 4. 生成答案
            GeneratedAnswer generatedAnswer = answerGenerator.generate(question, contexts);

            // 5. 构建响应
            Map<String, Object> metadata = new HashMap<>(generatedAnswer.metadata());
            metadata.put("cached", false);
            metadata.putIfAbsent("contextCount", contexts.size());
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
            if (request.enableCache()) {
                saveToCache(question, collectionName, request.topK(), request.filter(), modelName, response);
            }

            log.info("Successfully generated answer for question: {}", truncateForLog(question));
            return response;

        } catch (Exception e) {
            log.error("Failed to process QA request", e);
            return QAResponse.error(question, toClientErrorMessage(e));
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
            List<RetrievedContext> contexts = queryEngine.retrieve(question, retrieveOptions);
            long retrievalLatencyMs = System.currentTimeMillis() - retrievalStartTime;

            if (contexts.isEmpty()) {
                contexts = retryExplanatoryRetrieval(question, request, collectionName);
            }

            log.info("stream_retrieval_done collection={}, question={}, retrievalLatencyMs={}, contextCount={}, topScores={}",
                    collectionName,
                    truncateForLog(question),
                    retrievalLatencyMs,
                    contexts.size(),
                    topScoresForLog(contexts, 5));

            if (contexts.isEmpty()) {
                log.info("stream_retrieval_no_context collection={}, question={}, retrievalLatencyMs={}",
                        collectionName, truncateForLog(question), retrievalLatencyMs);
                return Flux.just("抱歉，未能找到与您问题相关的信息。请尝试换一种方式提问或提供更多细节。");
            }

            // 流式生成答案
            return answerGenerator.generateStream(question, contexts);

        } catch (Exception e) {
            log.error("Failed to process streaming QA request", e);
            return Flux.error(new IllegalStateException(toClientErrorMessage(e), e));
        }
    }

    private String toClientErrorMessage(Exception e) {
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

    private List<RetrievedContext> retryExplanatoryRetrieval(String question, QARequest request, String collectionName) {
        if (!isExplanatoryQuestion(question)) {
            return List.of();
        }

        List<String> fallbackQueries = buildExplanatoryFallbackQueries(question);
        if (fallbackQueries.isEmpty()) {
            return List.of();
        }

        float fallbackMinScore = Math.min(request.minScore(), EXPLANATORY_FALLBACK_MIN_SCORE);
        if (fallbackMinScore <= 0f) {
            fallbackMinScore = request.minScore();
        }

        List<RetrievedContext> merged = new ArrayList<>();
        for (String fallbackQuery : fallbackQueries) {
            RetrieveOptions fallbackOptions = new RetrieveOptions(
                    collectionName,
                    request.topK(),
                    fallbackMinScore,
                    request.filter(),
                    true);
            List<RetrievedContext> fallbackContexts = queryEngine.retrieve(fallbackQuery, fallbackOptions);
            if (!fallbackContexts.isEmpty()) {
                log.info("explanatory_retrieval_fallback_hit originalQuestion={}, fallbackQuery={}, fallbackMinScore={}, contextCount={}",
                        truncateForLog(question),
                        truncateForLog(fallbackQuery),
                        fallbackMinScore,
                        fallbackContexts.size());
            }
            mergeDistinctContexts(merged, fallbackContexts);
        }

        return merged.stream()
                .sorted(Comparator.comparingDouble(RetrievedContext::relevanceScore).reversed())
                .limit(request.topK())
                .toList();
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

    private void mergeDistinctContexts(List<RetrievedContext> merged, List<RetrievedContext> additions) {
        if (additions == null || additions.isEmpty()) {
            return;
        }
        for (RetrievedContext addition : additions) {
            boolean exists = merged.stream().anyMatch(existing -> sameContext(existing, addition));
            if (!exists) {
                merged.add(addition);
            }
        }
    }

    private boolean sameContext(RetrievedContext left, RetrievedContext right) {
        if (left == null || right == null) {
            return false;
        }

        String leftSource = left.source() == null ? "" : left.source().trim();
        String rightSource = right.source() == null ? "" : right.source().trim();
        String leftContent = left.content() == null ? "" : left.content().trim();
        String rightContent = right.content() == null ? "" : right.content().trim();
        return leftSource.equalsIgnoreCase(rightSource) && leftContent.equalsIgnoreCase(rightContent);
    }

    @Override
    public void evictCache(String question, String collectionName) {
        String queryHash = hashString(question.toLowerCase().trim());
        String pattern = RedisKeyConstants.QA_CACHE_PREFIX + queryHash + ":" + collectionName + ":*";
        redisUtil.deleteByPattern(pattern);
        log.debug("Evicted cache for question: {}", truncateForLog(question));
    }

    @Override
    public void clearAllCache() {
        redisUtil.deleteByPattern(RedisKeyConstants.QA_CACHE_PREFIX + "*");
        log.info("Cleared all QA cache");
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
        String cachedJson = redisUtil.getString(cacheKey);

        if (cachedJson != null) {
            try {
                return objectMapper.readValue(cachedJson, QAResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize cached response", e);
            }
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
            log.debug("Cached response for question: {}", truncateForLog(question));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize response for caching", e);
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
            log.debug("Failed to canonicalize filter, using fallback toString", e);
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
    private String truncateForLog(String text) {
        if (text == null)
            return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

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
