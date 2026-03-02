package com.enterprise.rag.core.rag;

import com.enterprise.rag.common.util.RedisUtil;
import com.enterprise.rag.core.rag.generator.AnswerGenerator;
import com.enterprise.rag.core.rag.model.*;
import com.enterprise.rag.core.rag.prompt.PromptBuilder;
import com.enterprise.rag.core.rag.prompt.PromptStrategy;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.service.RAGService;
import com.enterprise.rag.core.rag.service.RAGServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RAG 核心模块属性测试
 * 
 * Feature: enterprise-rag-qa-system
 * Validates: Requirements 5.2, 5.4, 10.1
 */
class RAGServicePropertyTest {

    /**
     * Property 13: Prompt 上下文包含性
     * 
     * *For any* 检索到的上下文文档，构建的 Prompt 应包含这些文档的内容。
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 100)
    void promptShouldContainAllRetrievedContexts(
            @ForAll("nonBlankText") String query,
            @ForAll("contextList") List<RetrievedContext> contexts) {
        
        // Skip if no contexts
        if (contexts.isEmpty()) {
            return;
        }
        
        PromptBuilder promptBuilder = new PromptBuilder();
        
        // Build prompt with different strategies
        for (PromptStrategy strategy : PromptStrategy.values()) {
            String prompt = promptBuilder.build(query, contexts, strategy);
            
            // Verify prompt is not null or empty
            assertThat(prompt != null && !prompt.isBlank())
                    .as("Prompt should not be null or blank for strategy %s", strategy)
                    .isTrue();
            
            // Verify all context contents are included in the prompt
            for (RetrievedContext context : contexts) {
                assertThat(prompt.contains(context.content()))
                        .as("Prompt should contain context content: %s", 
                            truncate(context.content(), 50))
                        .isTrue();
            }
        }
        
        // Verify using the helper method
        String defaultPrompt = promptBuilder.build(query, contexts);
        assertThat(promptBuilder.containsAllContexts(defaultPrompt, contexts))
                .as("containsAllContexts should return true for valid prompt")
                .isTrue();
    }


    /**
     * Property 14: 问答响应完整性
     * 
     * *For any* 成功的问答请求，响应应包含非空答案和引用来源列表。
     * 
     * **Validates: Requirements 5.4**
     */
    @Property(tries = 100)
    void qaResponseShouldContainAnswerAndCitations(
            @ForAll("nonBlankText") String question,
            @ForAll("collectionName") String collectionName,
            @ForAll("contextList") List<RetrievedContext> contexts) {
        
        // Skip if no contexts (would result in no_result response)
        if (contexts.isEmpty()) {
            return;
        }
        
        // Setup mocks
        QueryEngine mockQueryEngine = createMockQueryEngine(contexts);
        AnswerGenerator mockAnswerGenerator = createMockAnswerGenerator();
        RedisUtil mockRedisUtil = createMockRedisUtil();
        ObjectMapper objectMapper = new ObjectMapper();
        
        RAGService ragService = new RAGServiceImpl(
                mockQueryEngine,
                mockAnswerGenerator,
                mockRedisUtil,
                objectMapper
        );
        
        // Execute
        QARequest request = QARequest.of(question, collectionName);
        QAResponse response = ragService.ask(request);
        
        // Verify response is successful
        assertThat(response.isSuccess())
                .as("Response should be successful")
                .isTrue();
        
        // Verify answer is not empty
        assertThat(response.answer() != null && !response.answer().isBlank())
                .as("Answer should not be null or blank")
                .isTrue();
        
        // Verify citations list exists (may be empty but not null)
        assertThat(response.citations() != null)
                .as("Citations list should not be null")
                .isTrue();
        
        // Verify contexts are included
        assertThat(response.contexts() != null && !response.contexts().isEmpty())
                .as("Contexts should be included in response")
                .isTrue();
        
        // Verify metadata contains expected fields
        assertThat(response.metadata() != null)
                .as("Metadata should not be null")
                .isTrue();
        
        assertThat(response.metadata().containsKey("contextCount"))
                .as("Metadata should contain contextCount")
                .isTrue();
    }

    /**
     * Property 21: 查询缓存有效性
     * 
     * *For any* 相同的问答查询（相同问题和知识库），第二次查询应命中缓存。
     * 
     * **Validates: Requirements 10.1**
     */
    @Property(tries = 100)
    void samQueryShouldHitCacheOnSecondCall(
            @ForAll("nonBlankText") String question,
            @ForAll("collectionName") String collectionName,
            @ForAll("contextList") List<RetrievedContext> contexts) {
        
        // Skip if no contexts
        if (contexts.isEmpty()) {
            return;
        }
        
        // Setup mocks
        QueryEngine mockQueryEngine = createMockQueryEngine(contexts);
        AnswerGenerator mockAnswerGenerator = createMockAnswerGenerator();
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Setup Redis mock that simulates caching behavior
        RedisUtil mockRedisUtil = mock(RedisUtil.class);
        final Map<String, String> cache = new HashMap<>();
        
        // Mock getString to return cached value if exists
        when(mockRedisUtil.getString(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return cache.get(key);
        });
        
        // Mock setString to store the value
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            cache.put(key, value);
            return null;
        }).when(mockRedisUtil).setString(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        
        RAGService ragService = new RAGServiceImpl(
                mockQueryEngine,
                mockAnswerGenerator,
                mockRedisUtil,
                objectMapper
        );
        
        // First call - should not hit cache
        QARequest request = QARequest.of(question, collectionName);
        QAResponse firstResponse = ragService.ask(request);
        
        // Verify first call was successful
        assertThat(firstResponse.isSuccess())
                .as("First response should be successful")
                .isTrue();
        
        // Verify cache was populated
        assertThat(!cache.isEmpty())
                .as("Cache should be populated after first call")
                .isTrue();
        
        // Second call - should hit cache
        QAResponse secondResponse = ragService.ask(request);
        
        // Verify second call was successful
        assertThat(secondResponse.isSuccess())
                .as("Second response should be successful")
                .isTrue();
        
        // Verify cached flag in metadata
        assertThat(Boolean.TRUE.equals(secondResponse.metadata().get("cached")))
                .as("Second response should be marked as cached")
                .isTrue();
        
        // Verify answers are the same
        assertThat(firstResponse.answer().equals(secondResponse.answer()))
                .as("Cached response should have same answer")
                .isTrue();
        
        // Verify query engine was only called once (second call used cache)
        verify(mockQueryEngine, times(1)).retrieve(eq(question), any(RetrieveOptions.class));
    }


    /**
     * 验证无结果情况的处理
     */
    @Property(tries = 50)
    void noResultsShouldReturnAppropriateResponse(
            @ForAll("nonBlankText") String question,
            @ForAll("collectionName") String collectionName) {
        
        // Setup mocks with empty results
        QueryEngine mockQueryEngine = createMockQueryEngine(List.of());
        AnswerGenerator mockAnswerGenerator = createMockAnswerGenerator();
        RedisUtil mockRedisUtil = createMockRedisUtil();
        ObjectMapper objectMapper = new ObjectMapper();
        
        RAGService ragService = new RAGServiceImpl(
                mockQueryEngine,
                mockAnswerGenerator,
                mockRedisUtil,
                objectMapper
        );
        
        // Execute
        QARequest request = QARequest.of(question, collectionName);
        QAResponse response = ragService.ask(request);
        
        // Verify response indicates no result
        assertThat(!response.hasResult())
                .as("Response should indicate no result")
                .isTrue();
        
        // Verify answer is not empty (should contain helpful message)
        assertThat(response.answer() != null && !response.answer().isBlank())
                .as("No-result response should still have an answer message")
                .isTrue();
        
        // Verify contexts are empty
        assertThat(response.contexts().isEmpty())
                .as("No-result response should have empty contexts")
                .isTrue();
    }

    /**
     * 验证空问题的处理
     */
    @Property(tries = 50)
    void emptyQuestionShouldReturnError(
            @ForAll("collectionName") String collectionName) {
        
        // Setup mocks
        QueryEngine mockQueryEngine = createMockQueryEngine(List.of());
        AnswerGenerator mockAnswerGenerator = createMockAnswerGenerator();
        RedisUtil mockRedisUtil = createMockRedisUtil();
        ObjectMapper objectMapper = new ObjectMapper();
        
        RAGService ragService = new RAGServiceImpl(
                mockQueryEngine,
                mockAnswerGenerator,
                mockRedisUtil,
                objectMapper
        );
        
        // Test with empty question
        QARequest emptyRequest = new QARequest("", collectionName, 5, Map.of(), true, false);
        QAResponse emptyResponse = ragService.ask(emptyRequest);
        
        assertThat(!emptyResponse.isSuccess())
                .as("Empty question should result in error response")
                .isTrue();
        
        // Test with blank question
        QARequest blankRequest = new QARequest("   ", collectionName, 5, Map.of(), true, false);
        QAResponse blankResponse = ragService.ask(blankRequest);
        
        assertThat(!blankResponse.isSuccess())
                .as("Blank question should result in error response")
                .isTrue();
    }

    // ==================== Providers ====================

    @Provide
    Arbitrary<String> nonBlankText() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(100);
    }

    @Provide
    Arbitrary<String> collectionName() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20)
                .map(s -> "kb_" + s);
    }

    @Provide
    Arbitrary<List<RetrievedContext>> contextList() {
        return contextArbitrary()
                .list()
                .ofMinSize(1)
                .ofMaxSize(5);
    }

    @Provide
    Arbitrary<RetrievedContext> contextArbitrary() {
        return Combinators.combine(
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(20).ofMaxLength(200),
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(5).ofMaxLength(30).map(s -> "doc_" + s),
                Arbitraries.floats().between(0.5f, 1.0f)
        ).as((content, source, score) -> new RetrievedContext(content, source, score, Map.of()));
    }

    // ==================== Helper Methods ====================

    private QueryEngine createMockQueryEngine(List<RetrievedContext> contexts) {
        QueryEngine mock = Mockito.mock(QueryEngine.class);
        when(mock.retrieve(anyString(), any(RetrieveOptions.class))).thenReturn(contexts);
        return mock;
    }

    private AnswerGenerator createMockAnswerGenerator() {
        AnswerGenerator mock = Mockito.mock(AnswerGenerator.class);
        when(mock.getModelName()).thenReturn("mock-model");
        when(mock.generate(anyString(), anyList())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0);
            List<RetrievedContext> contexts = invocation.getArgument(1);
            
            // Generate a mock answer based on query
            String answer = "Based on the provided context, here is the answer to: " + query;
            
            // Generate citations from contexts
            List<Citation> citations = contexts.stream()
                    .map(ctx -> Citation.of(ctx.source(), truncate(ctx.content(), 50)))
                    .toList();
            
            return GeneratedAnswer.of(answer, citations, Map.of("model", "mock-model"));
        });
        return mock;
    }

    private RedisUtil createMockRedisUtil() {
        RedisUtil mock = Mockito.mock(RedisUtil.class);
        when(mock.getString(anyString())).thenReturn(null); // No cache by default
        return mock;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    // ==================== Assertion Helper ====================

    private BooleanAssert assertThat(boolean condition) {
        return new BooleanAssert(condition);
    }

    private static class BooleanAssert {
        private final boolean actual;
        private String description;

        BooleanAssert(boolean actual) {
            this.actual = actual;
        }

        BooleanAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isTrue() {
            if (!actual) {
                throw new AssertionError(description != null ? description : "Expected true but was false");
            }
        }
    }
}
