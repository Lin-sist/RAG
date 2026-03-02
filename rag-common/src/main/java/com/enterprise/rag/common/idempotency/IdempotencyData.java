package com.enterprise.rag.common.idempotency;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 幂等性数据存储结构
 * <p>
 * 存储在 Redis 中的幂等性处理结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyData {

    /**
     * 处理状态
     */
    private IdempotencyStatus status;

    /**
     * 处理结果（JSON 序列化）
     */
    private String resultJson;

    /**
     * 结果类型全限定名
     */
    private String resultType;

    /**
     * 处理时间
     */
    private Instant processedAt;

    /**
     * 幂等性处理状态
     */
    public enum IdempotencyStatus {
        /**
         * 处理中
         */
        PROCESSING,
        /**
         * 已完成
         */
        COMPLETED,
        /**
         * 处理失败
         */
        FAILED
    }

    /**
     * 创建处理中状态的数据
     */
    public static IdempotencyData processing() {
        IdempotencyData data = new IdempotencyData();
        data.setStatus(IdempotencyStatus.PROCESSING);
        data.setProcessedAt(Instant.now());
        return data;
    }

    /**
     * 创建已完成状态的数据
     */
    public static IdempotencyData completed(String resultJson, String resultType) {
        IdempotencyData data = new IdempotencyData();
        data.setStatus(IdempotencyStatus.COMPLETED);
        data.setResultJson(resultJson);
        data.setResultType(resultType);
        data.setProcessedAt(Instant.now());
        return data;
    }

    /**
     * 创建失败状态的数据
     */
    public static IdempotencyData failed(String errorMessage) {
        IdempotencyData data = new IdempotencyData();
        data.setStatus(IdempotencyStatus.FAILED);
        data.setResultJson(errorMessage);
        data.setProcessedAt(Instant.now());
        return data;
    }
}
