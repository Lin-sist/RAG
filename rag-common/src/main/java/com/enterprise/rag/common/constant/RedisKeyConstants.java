package com.enterprise.rag.common.constant;

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

    // ==================== Embedding Cache ====================
    
    /**
     * 嵌入向量缓存前缀
     * 格式: embedding:{contentHash}
     */
    public static final String EMBEDDING_CACHE_PREFIX = "embedding:";

    // ==================== QA Cache ====================
    
    /**
     * 问答结果缓存前缀
     * 格式: qa:cache:{queryHash}:{kbId}
     */
    public static final String QA_CACHE_PREFIX = "qa:cache:";

    // ==================== Task Related ====================
    
    /**
     * 异步任务状态前缀
     * 格式: task:status:{taskId}
     */
    public static final String TASK_STATUS_PREFIX = "task:status:";

    // ==================== Lock ====================
    
    /**
     * 分布式锁前缀
     * 格式: lock:{resource}
     */
    public static final String LOCK_PREFIX = "lock:";

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

    public static String userSessionKey(Long userId) {
        return USER_SESSION_PREFIX + userId;
    }

    public static String rateLimitKey(String dimension, String key) {
        return RATE_LIMIT_PREFIX + dimension + ":" + key;
    }

    public static String idempotencyKey(String key) {
        return IDEMPOTENCY_PREFIX + key;
    }

    public static String embeddingCacheKey(String contentHash) {
        return EMBEDDING_CACHE_PREFIX + contentHash;
    }

    public static String qaCacheKey(String queryHash, Long kbId) {
        return QA_CACHE_PREFIX + queryHash + ":" + kbId;
    }

    public static String taskStatusKey(String taskId) {
        return TASK_STATUS_PREFIX + taskId;
    }

    public static String lockKey(String resource) {
        return LOCK_PREFIX + resource;
    }
}
