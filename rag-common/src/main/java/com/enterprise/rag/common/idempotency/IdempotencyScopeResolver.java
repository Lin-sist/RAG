package com.enterprise.rag.common.idempotency;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * 从当前 HTTP 请求安全解析幂等身份范围的最小 SPI。
 * <p>
 * 具体认证模型由上层认证模块负责，common 模块不依赖认证实现。
 */
@FunctionalInterface
public interface IdempotencyScopeResolver {

    Optional<IdempotencyScope> resolve(HttpServletRequest request);
}
