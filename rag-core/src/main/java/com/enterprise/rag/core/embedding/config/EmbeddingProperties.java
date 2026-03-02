package com.enterprise.rag.core.embedding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 嵌入服务配置属性
 */
@ConfigurationProperties(prefix = "rag.embedding")
public class EmbeddingProperties {

    private OpenAI openai = new OpenAI();
    private Qwen qwen = new Qwen();
    private BGE bge = new BGE();
    private String defaultProvider = "openai";
    private boolean enableFallback = true;

    public static class OpenAI {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "text-embedding-ada-002";
        private int dimension = 1536;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;
        private long timeoutMs = 30000;
        private boolean enabled = true;
        private int priority = 1;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public long getRetryDelayMs() { return retryDelayMs; }
        public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
    }

    public static class Qwen {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String model = "text-embedding-v1";
        private int dimension = 1536;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;
        private long timeoutMs = 30000;
        private boolean enabled = true;
        private int priority = 2;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public long getRetryDelayMs() { return retryDelayMs; }
        public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
    }

    public static class BGE {
        private String baseUrl = "http://localhost:8080";
        private String model = "bge-large-zh";
        private int dimension = 1024;
        private int maxRetries = 3;
        private long retryDelayMs = 500;
        private long timeoutMs = 10000;
        private boolean enabled = true;
        private int priority = 3;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public long getRetryDelayMs() { return retryDelayMs; }
        public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
    }

    public OpenAI getOpenai() { return openai; }
    public void setOpenai(OpenAI openai) { this.openai = openai; }
    public Qwen getQwen() { return qwen; }
    public void setQwen(Qwen qwen) { this.qwen = qwen; }
    public BGE getBge() { return bge; }
    public void setBge(BGE bge) { this.bge = bge; }
    public String getDefaultProvider() { return defaultProvider; }
    public void setDefaultProvider(String defaultProvider) { this.defaultProvider = defaultProvider; }
    public boolean isEnableFallback() { return enableFallback; }
    public void setEnableFallback(boolean enableFallback) { this.enableFallback = enableFallback; }
}
