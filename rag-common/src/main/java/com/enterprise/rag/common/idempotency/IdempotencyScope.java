package com.enterprise.rag.common.idempotency;

/**
 * 已认证幂等请求的业务身份范围。
 *
 * @param tenantId 租户 ID
 * @param userId   用户 ID
 */
public record IdempotencyScope(long tenantId, long userId) {

    public IdempotencyScope {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
