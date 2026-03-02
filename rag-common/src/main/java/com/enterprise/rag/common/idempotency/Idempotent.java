package com.enterprise.rag.common.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等性注解
 * <p>
 * 标记在方法上，表示该方法需要幂等性控制。
 * 幂等性 Key 从请求头 X-Idempotency-Key 中提取。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等性 Key 的请求头名称
     * 默认为 X-Idempotency-Key
     */
    String headerName() default "X-Idempotency-Key";

    /**
     * 幂等性 Key 的过期时间（秒）
     * 默认为 24 小时
     */
    long ttlSeconds() default 86400L;

    /**
     * 是否必须提供幂等性 Key
     * 如果为 true，未提供 Key 时将抛出异常
     * 如果为 false，未提供 Key 时将跳过幂等性检查
     */
    boolean required() default true;

    /**
     * Key 前缀，用于区分不同业务场景
     * 默认为空，使用方法签名作为前缀
     */
    String keyPrefix() default "";

    /**
     * 缺少幂等性 Key 时的错误消息
     */
    String message() default "缺少幂等性Key，请在请求头中提供 X-Idempotency-Key";
}
