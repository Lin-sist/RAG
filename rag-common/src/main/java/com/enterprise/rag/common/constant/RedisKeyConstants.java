package com.enterprise.rag.common.constant;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Redis Key 常量定义
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
        // Utility class
    }

    // ==================== Token Related ====================
    
    /**
     * Token 黑名单前缀
     * 格式: token:blacklist:{tokenHash}
     */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * 用户会话前缀
     * 格式: session:{userId}
     */
    public static final String USER_SESSION_PREFIX = "session:";

    /**
     * Tenant-scoped user session prefix.
     * Format: session:v2:{tenantId}:{userId}
     */
    public static final String USER_SESSION_V2_PREFIX = "session:v2:";

    // ==================== Rate Limiting ====================
    
    /**
     * 限流计数器前缀
     * 格式: ratelimit:{dimension}:{key}
     */
    public static final String RATE_LIMIT_PREFIX = "ratelimit:";

    // ==================== Idempotency ====================
    
    /**
     * 幂等性 Key 前缀
     * 格式: idempotency:{key}
     */
    public static final String IDEMPOTENCY_PREFIX = "idempotency:";

    /**
     * Tenant-scoped idempotency prefix.
     * Format: idempotency:v2:{tenantId}:{userId}:{endpoint}:{key}
     */
    public static final String IDEMPOTENCY_V2_PREFIX = "idempotency:v2:";

    // ==================== Embedding Cache ====================
    
    /**
     * 嵌入向量缓存前缀
     * 格式: embedding:{contentHash}
     */
    public static final String EMBEDDING_CACHE_PREFIX = "embedding:";

    /**
     * Tenant-scoped embedding cache prefix.
     * Format: embedding:v2:{tenantId}:{provider}/{model}:{contentHash}
     */
    public static final String EMBEDDING_CACHE_V2_PREFIX = "embedding:v2:";

    // ==================== QA Cache ====================
    
    /**
     * 问答结果缓存前缀
     * 格式: qa:cache:{queryHash}:{kbId}
     */
    public static final String QA_CACHE_PREFIX = "qa:cache:";

    /**
     * Tenant-scoped QA cache prefix.
     * Format: qa:cache:v2:{tenantId}:{knowledgeBaseId}:{queryHash}
     */
    public static final String QA_CACHE_V2_PREFIX = "qa:cache:v2:";

    // ==================== Task Related ====================
    
    /**
     * 异步任务状态前缀
     * 格式: task:status:{taskId}
     */
    public static final String TASK_STATUS_PREFIX = "task:status:";

    /**
     * Tenant-scoped task projection prefix.
     * Format: task:status:v2:{tenantId}:{taskId}
     */
    public static final String TASK_STATUS_V2_PREFIX = "task:status:v2:";

    // ==================== Lock ====================
    
    /**
     * 分布式锁前缀
     * 格式: lock:{resource}
     */
    public static final String LOCK_PREFIX = "lock:";

    /**
     * Tenant-scoped business lock prefix.
     * Format: lock:v2:{tenantId}:{resource}
     */
    public static final String TENANT_BUSINESS_LOCK_V2_PREFIX = "lock:v2:";

    // ==================== Default TTL (seconds) ====================
    
    public static final long TOKEN_BLACKLIST_TTL = 86400L; // 24 hours
    public static final long USER_SESSION_TTL = 7200L; // 2 hours
    public static final long IDEMPOTENCY_TTL = 86400L; // 24 hours
    public static final long EMBEDDING_CACHE_TTL = 3600L; // 1 hour
    public static final long QA_CACHE_TTL = 1800L; // 30 minutes
    public static final long TASK_STATUS_TTL = 86400L; // 24 hours
    public static final long LOCK_TTL = 30L; // 30 seconds

    // ==================== Helper Methods ====================

    public static String tokenBlacklistKey(String tokenHash) {
        return TOKEN_BLACKLIST_PREFIX + tokenHash;
    }

    /**
     * @deprecated Tenant business sessions must use
     *             {@link #userSessionV2Key(long, long)}.
     */
    @Deprecated(since = "C13b", forRemoval = false)
    public static String userSessionKey(Long userId) {
        return USER_SESSION_PREFIX + userId;
    }

    public static String rateLimitKey(String dimension, String key) {
        return RATE_LIMIT_PREFIX + dimension + ":" + key;
    }

    /**
     * @deprecated Tenant business idempotency must use
     *             {@link #idempotencyV2Key(long, long, String, String)}.
     */
    @Deprecated(since = "C13b", forRemoval = false)
    public static String idempotencyKey(String key) {
        return IDEMPOTENCY_PREFIX + key;
    }

    /**
     * @deprecated Tenant embedding caches must use
     *             {@link #embeddingCacheV2Key(long, String, String, String)}.
     */
    @Deprecated(since = "C13b", forRemoval = false)
    public static String embeddingCacheKey(String contentHash) {
        return EMBEDDING_CACHE_PREFIX + contentHash;
    }

    /**
     * @deprecated Tenant QA caches must use
     *             {@link #qaCacheV2Key(long, long, String)}.
     */
    @Deprecated(since = "C13b", forRemoval = false)
    public static String qaCacheKey(String queryHash, Long kbId) {
        return QA_CACHE_PREFIX + queryHash + ":" + kbId;
    }

    /**
     * @deprecated Tenant task projections must use
     *             {@link #taskStatusV2Key(long, String)}.
     */
    @Deprecated(since = "C13b", forRemoval = false)
    public static String taskStatusKey(String taskId) {
        return TASK_STATUS_PREFIX + taskId;
    }

    /**
     * @deprecated Tenant business locks must use
     *             {@link #tenantBusinessLockV2Key(long, String)}.
     */
    @Deprecated(since = "C13b", forRemoval = false)
    public static String lockKey(String resource) {
        return LOCK_PREFIX + resource;
    }

    public static String userSessionV2Key(long tenantId, long userId) {
        return USER_SESSION_V2_PREFIX
                + requirePositive(tenantId, "tenantId") + ":"
                + requirePositive(userId, "userId");
    }

    public static String idempotencyV2Key(
            long tenantId,
            long userId,
            String endpoint,
            String idempotencyKey) {
        return IDEMPOTENCY_V2_PREFIX
                + requirePositive(tenantId, "tenantId") + ":"
                + requirePositive(userId, "userId") + ":"
                + encodeComponent(endpoint, "endpoint") + ":"
                + encodeComponent(idempotencyKey, "idempotencyKey");
    }

    public static String embeddingCacheV2Key(
            long tenantId,
            String effectiveProvider,
            String effectiveModel,
            String contentHash) {
        return EMBEDDING_CACHE_V2_PREFIX
                + requirePositive(tenantId, "tenantId") + ":"
                + encodeComponent(effectiveProvider, "effectiveProvider") + "/"
                + encodeComponent(effectiveModel, "effectiveModel") + ":"
                + encodeComponent(contentHash, "contentHash");
    }

    public static String qaCacheV2Key(long tenantId, long knowledgeBaseId, String queryHash) {
        return QA_CACHE_V2_PREFIX
                + requirePositive(tenantId, "tenantId") + ":"
                + requirePositive(knowledgeBaseId, "knowledgeBaseId") + ":"
                + encodeComponent(queryHash, "queryHash");
    }

    public static String taskStatusV2Key(long tenantId, String taskId) {
        return TASK_STATUS_V2_PREFIX
                + requirePositive(tenantId, "tenantId") + ":"
                + encodeComponent(taskId, "taskId");
    }

    public static String tenantBusinessLockV2Key(long tenantId, String resource) {
        return TENANT_BUSINESS_LOCK_V2_PREFIX
                + requirePositive(tenantId, "tenantId") + ":"
                + encodeComponent(resource, "resource");
    }

    private static long requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static String encodeComponent(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
