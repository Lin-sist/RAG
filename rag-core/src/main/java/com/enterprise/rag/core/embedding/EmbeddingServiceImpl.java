package com.enterprise.rag.core.embedding;

import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.util.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 嵌入服务实现
 * 实现 Provider 选择、降级逻辑和 Redis 缓存
 */
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);

    private final List<EmbeddingProvider> providers;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final boolean enableFallback;
    private final long cacheTtlSeconds;

    private volatile EmbeddingProvider activeProvider;

    public EmbeddingServiceImpl(List<EmbeddingProvider> providers, 
                                 RedisUtil redisUtil,
                                 ObjectMapper objectMapper,
                                 boolean enableFallback,
                                 long cacheTtlSeconds) {
        this.providers = sortByPriority(providers);
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
        this.enableFallback = enableFallback;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.activeProvider = selectActiveProvider();
        
        if (this.activeProvider == null) {
            log.warn("No embedding provider is available");
        } else {
            log.info("Active embedding provider: {}", this.activeProvider.getModelName());
        }
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Input text cannot be null or empty");
        }

        // Try to get from cache first
        String cacheKey = getCacheKey(text);
        float[] cached = getFromCache(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for embedding: {}", cacheKey);
            return cached;
        }

        // Get embedding from provider with fallback
        float[] embedding = getEmbeddingWithFallback(text);
        
        // Cache the result
        saveToCache(cacheKey, embedding);
        
        return embedding;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new EmbeddingException("Input texts cannot be null or empty");
        }

        List<float[]> results = new ArrayList<>(texts.size());
        List<String> uncachedTexts = new ArrayList<>();
        List<Integer> uncachedIndices = new ArrayList<>();
        Map<Integer, float[]> cachedResults = new HashMap<>();

        // Check cache for each text
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            String cacheKey = getCacheKey(text);
            float[] cached = getFromCache(cacheKey);
            if (cached != null) {
                cachedResults.put(i, cached);
            } else {
                uncachedTexts.add(text);
                uncachedIndices.add(i);
            }
        }

        // Get embeddings for uncached texts
        if (!uncachedTexts.isEmpty()) {
            List<float[]> newEmbeddings = getEmbeddingsBatchWithFallback(uncachedTexts);
            
            // Cache new embeddings
            for (int i = 0; i < uncachedTexts.size(); i++) {
                String cacheKey = getCacheKey(uncachedTexts.get(i));
                saveToCache(cacheKey, newEmbeddings.get(i));
                cachedResults.put(uncachedIndices.get(i), newEmbeddings.get(i));
            }
        }

        // Reconstruct results in original order
        for (int i = 0; i < texts.size(); i++) {
            results.add(cachedResults.get(i));
        }

        return results;
    }

    @Override
    public int getDimension() {
        EmbeddingProvider provider = getActiveProvider();
        return provider.getDimension();
    }

    @Override
    public String getActiveProviderName() {
        EmbeddingProvider provider = activeProvider;
        return provider != null ? provider.getModelName() : "none";
    }

    @Override
    public void evictCache(String text) {
        String cacheKey = getCacheKey(text);
        redisUtil.delete(cacheKey);
        log.debug("Evicted cache for: {}", cacheKey);
    }

    @Override
    public void clearAllCache() {
        redisUtil.deleteByPattern(RedisKeyConstants.EMBEDDING_CACHE_PREFIX + "*");
        log.info("Cleared all embedding cache");
    }

    private float[] getEmbeddingWithFallback(String text) {
        EmbeddingProvider provider = getActiveProvider();
        
        try {
            return provider.getEmbedding(text);
        } catch (EmbeddingException e) {
            if (enableFallback && e.isRetryable()) {
                return tryFallbackProviders(text, provider);
            }
            throw e;
        }
    }

    private List<float[]> getEmbeddingsBatchWithFallback(List<String> texts) {
        EmbeddingProvider provider = getActiveProvider();
        
        try {
            return provider.getEmbeddings(texts);
        } catch (EmbeddingException e) {
            if (enableFallback && e.isRetryable()) {
                return tryFallbackProvidersBatch(texts, provider);
            }
            throw e;
        }
    }

    private float[] tryFallbackProviders(String text, EmbeddingProvider failedProvider) {
        log.warn("Primary provider {} failed, trying fallback providers", failedProvider.getModelName());
        
        for (EmbeddingProvider provider : providers) {
            if (provider == failedProvider || !provider.isAvailable()) {
                continue;
            }
            
            try {
                log.info("Trying fallback provider: {}", provider.getModelName());
                float[] result = provider.getEmbedding(text);
                // Update active provider on successful fallback
                this.activeProvider = provider;
                log.info("Switched to fallback provider: {}", provider.getModelName());
                return result;
            } catch (EmbeddingException e) {
                log.warn("Fallback provider {} also failed: {}", provider.getModelName(), e.getMessage());
            }
        }
        
        throw new EmbeddingException("All embedding providers failed");
    }

    private List<float[]> tryFallbackProvidersBatch(List<String> texts, EmbeddingProvider failedProvider) {
        log.warn("Primary provider {} failed for batch, trying fallback providers", failedProvider.getModelName());
        
        for (EmbeddingProvider provider : providers) {
            if (provider == failedProvider || !provider.isAvailable()) {
                continue;
            }
            
            try {
                log.info("Trying fallback provider for batch: {}", provider.getModelName());
                List<float[]> result = provider.getEmbeddings(texts);
                this.activeProvider = provider;
                log.info("Switched to fallback provider: {}", provider.getModelName());
                return result;
            } catch (EmbeddingException e) {
                log.warn("Fallback provider {} also failed for batch: {}", provider.getModelName(), e.getMessage());
            }
        }
        
        throw new EmbeddingException("All embedding providers failed for batch operation");
    }

    private EmbeddingProvider getActiveProvider() {
        if (activeProvider == null || !activeProvider.isAvailable()) {
            activeProvider = selectActiveProvider();
        }
        
        if (activeProvider == null) {
            throw new EmbeddingException("No embedding provider is available");
        }
        
        return activeProvider;
    }

    private EmbeddingProvider selectActiveProvider() {
        return providers.stream()
                .filter(EmbeddingProvider::isAvailable)
                .findFirst()
                .orElse(null);
    }

    private List<EmbeddingProvider> sortByPriority(List<EmbeddingProvider> providers) {
        List<EmbeddingProvider> sorted = new ArrayList<>(providers);
        sorted.sort(Comparator.comparingInt(EmbeddingProvider::getPriority));
        return sorted;
    }

    private String getCacheKey(String text) {
        String hash = computeHash(text);
        return RedisKeyConstants.embeddingCacheKey(hash);
    }

    private String computeHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 32); // Use first 32 chars
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private float[] getFromCache(String cacheKey) {
        try {
            String json = redisUtil.getString(cacheKey);
            if (json != null) {
                return objectMapper.readValue(json, float[].class);
            }
        } catch (Exception e) {
            log.warn("Failed to read embedding from cache: {}", e.getMessage());
        }
        return null;
    }

    private void saveToCache(String cacheKey, float[] embedding) {
        try {
            String json = objectMapper.writeValueAsString(embedding);
            redisUtil.setString(cacheKey, json, cacheTtlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache embedding: {}", e.getMessage());
        }
    }
}
