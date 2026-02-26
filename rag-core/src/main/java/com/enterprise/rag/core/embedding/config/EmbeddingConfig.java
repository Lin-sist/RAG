package com.enterprise.rag.core.embedding.config;

import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.util.RedisUtil;
import com.enterprise.rag.core.embedding.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 嵌入服务配置类
 */
@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingConfig.class);

    @Bean
    @ConditionalOnProperty(name = "rag.embedding.openai.enabled", havingValue = "true", matchIfMissing = true)
    public OpenAIEmbeddingProvider openAIEmbeddingProvider(EmbeddingProperties properties) {
        log.info("Configuring OpenAI Embedding Provider");
        return new OpenAIEmbeddingProvider(properties.getOpenai());
    }

    @Bean
    @ConditionalOnProperty(name = "rag.embedding.qwen.enabled", havingValue = "true", matchIfMissing = true)
    public QwenEmbeddingProvider qwenEmbeddingProvider(EmbeddingProperties properties) {
        log.info("Configuring Qwen Embedding Provider");
        return new QwenEmbeddingProvider(properties.getQwen());
    }

    @Bean
    @ConditionalOnProperty(name = "rag.embedding.bge.enabled", havingValue = "true", matchIfMissing = true)
    public BGEEmbeddingProvider bgeEmbeddingProvider(EmbeddingProperties properties) {
        log.info("Configuring BGE Embedding Provider");
        return new BGEEmbeddingProvider(properties.getBge());
    }

    @Bean
    public EmbeddingService embeddingService(
            List<EmbeddingProvider> providers,
            RedisUtil redisUtil,
            ObjectMapper objectMapper,
            EmbeddingProperties properties) {
        
        log.info("Configuring EmbeddingService with {} providers", providers.size());
        providers.forEach(p -> log.info("  - {} (priority: {}, available: {})", 
                p.getModelName(), p.getPriority(), p.isAvailable()));
        
        return new EmbeddingServiceImpl(
                providers,
                redisUtil,
                objectMapper,
                properties.isEnableFallback(),
                RedisKeyConstants.EMBEDDING_CACHE_TTL
        );
    }
}
