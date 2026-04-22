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
        assertTrue(result.prompt().contains("Source: 并发入门 / doc-a"));
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
        assertTrue(result.prompt().contains("Source: 配置指南 / doc-config"));
        assertTrue(result.estimatedContextTokens() > 0);
    }

    @Test
    void shouldAddExplanationGuidanceForHowQueries() {
        RetrievedContext context = new RetrievedContext(
                "RAG 的工作流程包括检索知识库、构建上下文并生成回答。",
                "doc-rag",
                0.91f,
                Map.of("title", "RAG 原理", "chunkIndex", 2));

        PromptBuilder.PromptBuildResult result = promptBuilder.buildOptimized(
                "你认为RAG是如何运作的？",
                List.of(context),
                PromptStrategy.STRUCTURED,
                800);

        assertTrue(result.prompt().contains("synthesize the relevant context into a coherent explanation"));
        assertTrue(result.prompt().contains("You MAY combine multiple context snippets into one grounded explanation"));
    }

    @Test
    void shouldAvoidLeakingRawSourceMarkersIntoPromptInstructions() {
        RetrievedContext context = new RetrievedContext(
                "RAG 是一种检索增强生成架构。",
                "0",
                0.34f,
                Map.of("chunkIndex", 0.0));

        PromptBuilder.PromptBuildResult result = promptBuilder.buildOptimized(
                "什么是RAG？",
                List.of(context),
                PromptStrategy.STRUCTURED,
                800);

        assertTrue(result.prompt().contains("Do not include raw source headers"));
        assertTrue(result.prompt().contains("Source: Document 1"));
        assertTrue(!result.prompt().contains("[Source 1:"));
    }
}
