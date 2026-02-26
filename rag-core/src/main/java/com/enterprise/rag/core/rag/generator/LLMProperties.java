package com.enterprise.rag.core.rag.generator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.llm")
public class LLMProperties {

    /**
     * 默认提供者：openai, qwen
     */
    private String provider = "openai";

    /**
     * OpenAI 配置
     */
    private OpenAIConfig openai = new OpenAIConfig();

    /**
     * 通义千问配置
     */
    private QwenConfig qwen = new QwenConfig();

    /**
     * 请求超时时间（秒）
     */
    private int timeout = 60;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    @Data
    public static class OpenAIConfig {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-3.5-turbo";
        private double temperature = 0.7;
        private int maxTokens = 2048;
    }

    @Data
    public static class QwenConfig {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String model = "qwen-turbo";
        private double temperature = 0.7;
        private int maxTokens = 2048;
    }
}
