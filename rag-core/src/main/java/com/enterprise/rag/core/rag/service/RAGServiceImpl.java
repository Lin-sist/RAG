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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RAG 服务实现
 * 协调检索和生成流程，提供完整的问答能力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGServiceImpl implements RAGService {

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
            if (request.enableCache()) {
                QAResponse cachedResponse = getFromCache(question, collectionName);
                if (cachedResponse != null) {
                    log.debug("Cache hit for question: {}", truncateForLog(question));
                    return addCacheMetadata(cachedResponse, true);
                }
            }

            // 2. 检索相关文档
            RetrieveOptions retrieveOptions = new RetrieveOptions(
                    collectionName,
                    request.topK(),
                    RetrieveOptions.DEFAULT_MIN_SCORE,
                    request.filter(),
                    true
            );
            List<RetrievedContext> contexts = queryEngine.retrieve(question, retrieveOptions);
            log.debug("Retrieved {} contexts for question", contexts.size());

            // 3. 处理无结果情况
            if (contexts.isEmpty()) {
                log.info("No relevant contexts found for question: {}", truncateForLog(question));
                return QAResponse.noResult(question);
            }


            // 4. 生成答案
            GeneratedAnswer generatedAnswer = answerGenerator.generate(question, contexts);

            // 5. 构建响应
            Map<String, Object> metadata = new HashMap<>(generatedAnswer.metadata());
            metadata.put("cached", false);
            metadata.put("contextCount", contexts.size());

            QAResponse response = QAResponse.success(
                    question,
                    generatedAnswer.answer(),
                    generatedAnswer.citations(),
                    contexts,
                    metadata
            );

            // 6. 缓存结果
            if (request.enableCache()) {
                saveToCache(question, collectionName, response);
            }

            log.info("Successfully generated answer for question: {}", truncateForLog(question));
            return response;

        } catch (Exception e) {
            log.error("Failed to process QA request", e);
            return QAResponse.error(question, e.getMessage());
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
            // 检索相关文档
            RetrieveOptions retrieveOptions = new RetrieveOptions(
                    collectionName,
                    request.topK(),
                    RetrieveOptions.DEFAULT_MIN_SCORE,
                    request.filter(),
                    true
            );
            List<RetrievedContext> contexts = queryEngine.retrieve(question, retrieveOptions);

            if (contexts.isEmpty()) {
                return Flux.just("抱歉，未能找到与您问题相关的信息。请尝试换一种方式提问或提供更多细节。");
            }

            // 流式生成答案
            return answerGenerator.generateStream(question, contexts);

        } catch (Exception e) {
            log.error("Failed to process streaming QA request", e);
            return Flux.error(e);
        }
    }

    @Override
    public void evictCache(String question, String collectionName) {
        String cacheKey = buildCacheKey(question, collectionName);
        redisUtil.delete(cacheKey);
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
    private QAResponse getFromCache(String question, String collectionName) {
        String cacheKey = buildCacheKey(question, collectionName);
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
    private void saveToCache(String question, String collectionName, QAResponse response) {
        String cacheKey = buildCacheKey(question, collectionName);
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
    private String buildCacheKey(String question, String collectionName) {
        String queryHash = hashString(question.toLowerCase().trim());
        return RedisKeyConstants.QA_CACHE_PREFIX + queryHash + ":" + collectionName;
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
                newMetadata
        );
    }

    /**
     * 截断日志输出
     */
    private String truncateForLog(String text) {
        if (text == null) return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
