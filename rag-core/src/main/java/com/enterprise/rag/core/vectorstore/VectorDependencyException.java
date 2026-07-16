package com.enterprise.rag.core.vectorstore;

import com.enterprise.rag.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 向量存储依赖故障的稳定、安全边界。
 *
 * <p>客户端和普通日志只能消费固定字段，不得传播底层 SDK message。</p>
 */
public final class VectorDependencyException extends BusinessException {

    public static final String ERROR_CODE_UNAVAILABLE = "VECTOR_STORE_UNAVAILABLE";
    public static final String ERROR_CODE_INDEX_UNAVAILABLE = "VECTOR_INDEX_UNAVAILABLE";
    public static final String ERROR_CODE_OUTCOME_UNKNOWN = "VECTOR_OPERATION_OUTCOME_UNKNOWN";

    private final String dependency;
    private final String subsystem;
    private final String operation;
    private final String errorCategory;
    private final String failMode;

    private VectorDependencyException(
            String errorCode,
            String message,
            String subsystem,
            String operation,
            String errorCategory,
            String failMode,
            Throwable cause) {
        super(errorCode, message, HttpStatus.SERVICE_UNAVAILABLE);
        this.dependency = "milvus";
        this.subsystem = subsystem;
        this.operation = operation;
        this.errorCategory = errorCategory == null ? classify(cause) : errorCategory;
        this.failMode = failMode;
        if (cause != null) {
            initCause(cause);
        }
    }

    public static VectorDependencyException unavailable(String operation, Throwable cause) {
        return new VectorDependencyException(
                ERROR_CODE_UNAVAILABLE,
                "向量存储暂时不可用，请稍后重试",
                "vector_store",
                operation,
                null,
                "closed",
                cause);
    }

    public static VectorDependencyException rpcFailure(String operation, Throwable cause) {
        return new VectorDependencyException(
                ERROR_CODE_UNAVAILABLE,
                "向量存储暂时不可用，请稍后重试",
                "vector_store",
                operation,
                "rpc",
                "closed",
                cause);
    }

    public static VectorDependencyException malformedResponse(String operation, Throwable cause) {
        return new VectorDependencyException(
                ERROR_CODE_UNAVAILABLE,
                "向量存储暂时不可用，请稍后重试",
                "vector_store",
                operation,
                "serialization",
                "closed",
                cause);
    }

    public static VectorDependencyException indexUnavailable(String operation, Throwable cause) {
        return new VectorDependencyException(
                ERROR_CODE_INDEX_UNAVAILABLE,
                "向量索引暂时不可用，请联系管理员检查索引状态",
                "vector_store",
                operation,
                "index_missing",
                "closed",
                cause);
    }

    public static VectorDependencyException outcomeUnknown(String operation, Throwable cause) {
        return new VectorDependencyException(
                ERROR_CODE_OUTCOME_UNKNOWN,
                "向量操作结果状态未知，请先查询资源状态，勿直接自动重试",
                "vector_store",
                operation,
                null,
                "outcome_unknown",
                cause);
    }

    public String getDependency() {
        return dependency;
    }

    public String getSubsystem() {
        return subsystem;
    }

    public String getOperation() {
        return operation;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public String getFailMode() {
        return failMode;
    }

    public Map<String, Object> diagnostics() {
        return Map.of(
                "dependency", dependency,
                "subsystem", subsystem,
                "operation", operation,
                "errorCategory", errorCategory,
                "failMode", failMode);
    }

    private static String classify(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            String type = current.getClass().getSimpleName().toLowerCase();
            if (type.contains("timeout") || type.contains("deadline")) {
                return "timeout";
            }
            if (type.contains("connection") || type.contains("connect") || type.contains("channel")) {
                return "connection";
            }
            if (type.contains("json") || type.contains("serial") || type.contains("parse")) {
                return "serialization";
            }
            current = current.getCause();
        }
        return "unknown";
    }
}
