package com.enterprise.rag.core.embedding;

import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.util.RedisUtil;
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
    public float[] embed(long tenantId, String text) {
        requireTenantId(tenantId);
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Input text cannot be null or empty");
        }

        // Try to get from cache first
        EmbeddingProvider requestedProvider = getActiveProvider();
        String cacheKey = getCacheKey(tenantId, text, requestedProvider);
        float[] cached = getFromCache(cacheKey, tenantId, requestedProvider);
        if (cached != null) {
            log.debug("Cache hit for embedding: {}", cacheKey);
            return cached;
        }

        // Get embedding from provider with fallback
        float[] embedding = getEmbeddingWithFallback(text);
        EmbeddingProvider effectiveProvider = getActiveProvider();
        if (!isValidVector(embedding, effectiveProvider.getDimension())) {
            throw new EmbeddingException("Embedding vector contract mismatch",
                    effectiveProvider.getModelName(), false);
        }
        
        // Cache the result
        saveToCache(getCacheKey(tenantId, text, effectiveProvider),
                tenantId, effectiveProvider, embedding);
        
        return embedding;
    }

    @Override
    public List<float[]> embedBatch(long tenantId, List<String> texts) {
        requireTenantId(tenantId);
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
            EmbeddingProvider requestedProvider = getActiveProvider();
            String cacheKey = getCacheKey(tenantId, text, requestedProvider);
            float[] cached = getFromCache(cacheKey, tenantId, requestedProvider);
            if (cached != null) {
                cachedResults.put(i, cached);
            } else {
                uncachedTexts.add(text);
                uncachedIndices.add(i);
            }
        }

        // Get embeddings for uncached texts
        if (!uncachedTexts.isEmpty()) {
            List<float[]> newEmbeddings = validateBatch(
                    getEmbeddingsBatchWithFallback(uncachedTexts), uncachedTexts.size(), getActiveProvider());
            
            // Cache new embeddings
            for (int i = 0; i < uncachedTexts.size(); i++) {
                EmbeddingProvider effectiveProvider = getActiveProvider();
                String cacheKey = getCacheKey(tenantId, uncachedTexts.get(i), effectiveProvider);
                saveToCache(cacheKey, tenantId, effectiveProvider, newEmbeddings.get(i));
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
    public List<float[]> embedBatchUncached(long tenantId, List<String> texts) {
        requireTenantId(tenantId);
        if (texts == null || texts.isEmpty() || texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new EmbeddingException("Input texts cannot be null, empty, or blank");
        }
        EmbeddingProvider provider = getActiveProvider();
        return validateBatch(provider.getEmbeddings(List.copyOf(texts)), texts.size(), provider);
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
    public EmbeddingModelIdentity getActiveModelIdentity() {
        return resolveModelIdentity(getActiveProvider());
    }

    @Override
    public int getMaxBatchSize() {
        return getActiveProvider().getMaxBatchSize();
    }

    @Override
    public void evictCache(long tenantId, String text) {
        String cacheKey = getCacheKey(tenantId, text, getActiveProvider());
        try {
            redisUtil.delete(cacheKey);
            log.debug("Evicted embedding cache");
        } catch (Exception e) {
            log.warn("Embedding cache eviction degraded: dependency=redis, subsystem=embedding_cache, "
                            + "operation=delete, failMode=open, errorType={}",
                    e.getClass().getSimpleName());
        }
    }

    @Override
    public void clearCache(long tenantId) {
        requireTenantId(tenantId);
        try {
            redisUtil.deleteByPattern(RedisKeyConstants.EMBEDDING_CACHE_V2_PREFIX + tenantId + ":*");
            log.info("Cleared tenant-scoped embedding cache");
        } catch (Exception e) {
            log.warn("Embedding cache clear degraded: dependency=redis, subsystem=embedding_cache, "
                            + "operation=clear, failMode=open, errorType={}",
                    e.getClass().getSimpleName());
        }
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
                log.warn("Fallback provider {} also failed: errorType={}",
                        provider.getModelName(), e.getClass().getSimpleName());
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
                log.warn("Fallback provider {} also failed for batch: errorType={}",
                        provider.getModelName(), e.getClass().getSimpleName());
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

    private String getCacheKey(long tenantId, String text, EmbeddingProvider provider) {
        String hash = computeHash(text);
        EmbeddingModelIdentity identity = resolveModelIdentity(provider);
        return RedisKeyConstants.embeddingCacheV2Key(
                requireTenantId(tenantId), identity.providerFamily(), identity.fingerprint(), hash);
    }

    private long requireTenantId(long tenantId) {
        if (tenantId <= 0) {
            throw new EmbeddingException("Tenant scope is required");
        }
        return tenantId;
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

    private float[] getFromCache(String cacheKey, long tenantId, EmbeddingProvider provider) {
        try {
            String json = redisUtil.getString(cacheKey);
            if (json != null) {
                TenantCachedEmbedding cached = objectMapper.readValue(json, TenantCachedEmbedding.class);
                if (cached.tenantId() == tenantId
                        && resolveModelIdentity(provider).fingerprint().equals(cached.identityFingerprint())
                        && isValidVector(cached.embedding(), provider.getDimension())) {
                    return cached.embedding();
                }
            }
        } catch (Exception e) {
            log.warn("Embedding cache read degraded: dependency=redis, subsystem=embedding_cache, "
                            + "operation=read, failMode=open, errorType={}",
                    e.getClass().getSimpleName());
        }
        return null;
    }

    private void saveToCache(String cacheKey,
            long tenantId,
            EmbeddingProvider provider,
            float[] embedding) {
        try {
            String json = objectMapper.writeValueAsString(new TenantCachedEmbedding(
                    tenantId, resolveModelIdentity(provider).fingerprint(), embedding));
            redisUtil.setString(cacheKey, json, cacheTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Embedding cache write degraded: dependency=redis, subsystem=embedding_cache, "
                            + "operation=write, failMode=open, errorType={}",
                    e.getClass().getSimpleName());
        }
    }

    private List<float[]> validateBatch(List<float[]> embeddings, int expectedCount, EmbeddingProvider provider) {
        if (embeddings == null || embeddings.size() != expectedCount) {
            throw new EmbeddingException("Embedding batch count mismatch", provider.getModelName(), false);
        }
        for (float[] embedding : embeddings) {
            if (!isValidVector(embedding, provider.getDimension())) {
                throw new EmbeddingException("Embedding vector contract mismatch", provider.getModelName(), false);
            }
        }
        return List.copyOf(embeddings);
    }

    private boolean isValidVector(float[] embedding, int dimension) {
        if (embedding == null || embedding.length != dimension) {
            return false;
        }
        for (float value : embedding) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private EmbeddingModelIdentity resolveModelIdentity(EmbeddingProvider provider) {
        EmbeddingModelIdentity identity = provider.getModelIdentity();
        if (identity != null) {
            return identity;
        }
        return new EmbeddingModelIdentity(
                defaultIfBlank(provider.getProviderFamily(), provider.getClass().getSimpleName()),
                defaultIfBlank(provider.getModelName(), "unknown-model"),
                defaultIfBlank(provider.getEndpointIdentity(), "local"),
                defaultIfBlank(provider.getRequestContractVersion(), "legacy-v1"),
                provider.getDimension());
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record TenantCachedEmbedding(
            long tenantId,
            String identityFingerprint,
            float[] embedding) {
    }
}
