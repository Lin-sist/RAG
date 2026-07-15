package com.enterprise.rag.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Redis 关键状态不可用时的稳定业务异常。
 *
 * <p>只暴露固定分类，不把 Redis key/value 或底层异常消息带到客户端。</p>
 */
@Getter
public class RedisDependencyException extends BusinessException {

    public static final String ERROR_CODE_UNAVAILABLE = "REDIS_DEPENDENCY_UNAVAILABLE";
    public static final String ERROR_CODE_OUTCOME_UNKNOWN = "IDEMPOTENCY_OUTCOME_UNKNOWN";

    private final String dependency = "redis";
    private final String subsystem;
    private final String operation;
    private final String errorCategory;
    private final String failMode;

    private RedisDependencyException(
            String errorCode,
            String message,
            String subsystem,
            String operation,
            String failMode,
            Throwable cause) {
        super(errorCode, message, HttpStatus.SERVICE_UNAVAILABLE);
        this.subsystem = subsystem;
        this.operation = operation;
        this.errorCategory = classify(cause);
        this.failMode = failMode;
        if (cause != null) {
            initCause(cause);
        }
    }

    public static RedisDependencyException unavailable(
            String subsystem, String operation, Throwable cause) {
        return new RedisDependencyException(
                ERROR_CODE_UNAVAILABLE,
                "Redis 依赖暂时不可用，请稍后重试",
                subsystem,
                operation,
                "closed",
                cause);
    }

    public static RedisDependencyException outcomeUnknown(
            String subsystem, String operation, Throwable cause) {
        return new RedisDependencyException(
                ERROR_CODE_OUTCOME_UNKNOWN,
                "请求结果状态未知，请先查询当前资源状态，勿直接自动重试",
                subsystem,
                operation,
                "outcome_unknown",
                cause);
    }

    private static String classify(Throwable cause) {
        if (cause == null) {
            return "unknown";
        }
        String type = cause.getClass().getSimpleName().toLowerCase();
        if (type.contains("timeout")) {
            return "timeout";
        }
        if (type.contains("connection") || type.contains("connect")) {
            return "connection";
        }
        if (type.contains("command")) {
            return "command";
        }
        if (type.contains("json") || type.contains("serial")) {
            return "serialization";
        }
        return "unknown";
    }
}
