package com.enterprise.rag.core.rag.prompt;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void shouldDeduplicateAndApplyTokenBudget() {
        RetrievedContext duplicateA = new RetrievedContext(
                "Java 并发编程基础与线程池使用说明",
                "doc-a",
                0.92f,
                Map.of("title", "并发入门", "chunkIndex", 1));
        RetrievedContext duplicateB = new RetrievedContext(
                "Java 并发编程基础与线程池使用说明",
                "doc-a",
                0.85f,
                Map.of("title", "并发入门", "chunkIndex", 2));
        RetrievedContext longContext = new RetrievedContext(
                "这是一个很长的上下文内容，用于模拟预算限制场景。".repeat(40),
                "doc-b",
                0.80f,
                Map.of("title", "预算测试", "chunkIndex", 3));

        PromptBuilder.PromptBuildResult result = promptBuilder.buildOptimized(
                "请解释线程池的核心参数",
                List.of(duplicateA, duplicateB, longContext),
                PromptStrategy.STRUCTURED,
                120);

        assertEquals(1, result.removedByDedup());
        assertTrue(result.removedByBudget() >= 1);
        assertEquals(1, result.contexts().size());
        assertTrue(result.prompt().contains("并发入门 / doc-a #chunk=1"));
    }

    @Test
    void shouldEnhanceSourceWithMetadataHints() {
        RetrievedContext context = new RetrievedContext(
                "Spring Boot 配置加载顺序说明",
                "doc-config",
                0.77f,
                Map.of("title", "配置指南", "chunkIndex", 5));

        PromptBuilder.PromptBuildResult result = promptBuilder.buildOptimized(
                "Spring Boot 如何加载配置",
                List.of(context),
                PromptStrategy.STRUCTURED,
                800);

        assertEquals(1, result.contexts().size());
        assertTrue(result.prompt().contains("配置指南 / doc-config #chunk=5"));
        assertTrue(result.estimatedContextTokens() > 0);
    }
}
