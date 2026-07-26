package com.enterprise.rag.admin.security;

/**
 * 服务端认证得到的不可变请求身份；不得由客户端 tenant selector 构造。
 */
public record RequestIdentity(long userId, long tenantId) {

    public RequestIdentity {
        if (userId <= 0 || tenantId <= 0) {
            throw new IllegalArgumentException("Authenticated request identity must be positive");
        }
    }
}
