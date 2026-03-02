package com.enterprise.rag.core.embedding;

import com.enterprise.rag.common.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 嵌入服务属性测试
 * 
 * Feature: enterprise-rag-qa-system
 * Validates: Requirements 3.2, 3.4
 */
class EmbeddingServicePropertyTest {

    private static final int TEST_DIMENSION = 1536;
    private static final long CACHE_TTL = 3600L;

    /**
     * Property 9: 嵌入向量有效性
     * 
     * *For any* 非空文本输入，Embedding Service 返回的向量应具有正确的维度且所有元素非 NaN。
     * 
     * **Validates: Requirements 3.2**
     */
    @Property(tries = 100)
    void embeddingVectorShouldBeValidForAnyNonEmptyText(
            @ForAll("nonBlankText") String text) {
        
        // Setup mock provider that returns valid embeddings
        EmbeddingProvider mockProvider = createMockProvider(TEST_DIMENSION);
        RedisUtil mockRedisUtil = createMockRedisUtil();
        ObjectMapper objectMapper = new ObjectMapper();
        
        EmbeddingService service = new EmbeddingServiceImpl(
                List.of(mockProvider),
                mockRedisUtil,
                objectMapper,
                true,
                CACHE_TTL
        );
        
        // Execute
        float[] embedding = service.embed(text);
        
        // Verify dimension is correct
        assertThat(embedding.length == TEST_DIMENSION)
                .as("Embedding dimension should be %d but was %d", TEST_DIMENSION, embedding.length)
                .isTrue();
        
        // Verify no NaN values
        for (int i = 0; i < embedding.length; i++) {
            assertThat(!Float.isNaN(embedding[i]))
                    .as("Embedding element at index %d should not be NaN", i)
                    .isTrue();
        }
        
        // Verify no Infinity values
        for (int i = 0; i < embedding.length; i++) {
            assertThat(!Float.isInfinite(embedding[i]))
                    .as("Embedding element at index %d should not be Infinite", i)
                    .isTrue();
        }
    }

    /**
     * Property 10: 嵌入缓存一致性
     * 
     * *For any* 文本输入，多次调用 Embedding Service 应返回相同的向量结果（缓存命中）。
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(tries = 100)
    void multipleCallsWithSameTextShouldReturnConsistentResults(
            @ForAll("nonBlankText") String text) {
        
        // Setup mock provider
        EmbeddingProvider mockProvider = createMockProvider(TEST_DIMENSION);
        
        // Setup Redis mock that simulates caching behavior
        RedisUtil mockRedisUtil = mock(RedisUtil.class);
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Track cached values
        final float[][] cachedValue = {null};
        
        // Mock getString to return cached value if exists
        when(mockRedisUtil.getString(anyString())).thenAnswer(invocation -> {
            if (cachedValue[0] != null) {
                try {
                    return objectMapper.writeValueAsString(cachedValue[0]);
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
        
        // Mock setString to store the value
        doAnswer(invocation -> {
            String json = invocation.getArgument(1);
            try {
                cachedValue[0] = objectMapper.readValue(json, float[].class);
            } catch (Exception e) {
                // ignore
            }
            return null;
        }).when(mockRedisUtil).setString(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        
        EmbeddingService service = new EmbeddingServiceImpl(
                List.of(mockProvider),
                mockRedisUtil,
                objectMapper,
                true,
                CACHE_TTL
        );
        
        // First call - should call provider
        float[] firstResult = service.embed(text);
        
        // Second call - should use cache
        float[] secondResult = service.embed(text);
        
        // Third call - should also use cache
        float[] thirdResult = service.embed(text);
        
        // Verify all results are identical
        assertThat(Arrays.equals(firstResult, secondResult))
                .as("First and second call should return identical results")
                .isTrue();
        
        assertThat(Arrays.equals(secondResult, thirdResult))
                .as("Second and third call should return identical results")
                .isTrue();
        
        // Verify provider was only called once (subsequent calls used cache)
        verify(mockProvider, times(1)).getEmbedding(text);
    }

    /**
     * 验证批量嵌入的向量有效性
     */
    @Property(tries = 50)
    void batchEmbeddingShouldReturnValidVectorsForAllTexts(
            @ForAll("nonEmptyTextList") List<String> texts) {
        
        // Setup mock provider
        EmbeddingProvider mockProvider = createMockProviderForBatch(TEST_DIMENSION);
        RedisUtil mockRedisUtil = createMockRedisUtil();
        ObjectMapper objectMapper = new ObjectMapper();
        
        EmbeddingService service = new EmbeddingServiceImpl(
                List.of(mockProvider),
                mockRedisUtil,
                objectMapper,
                true,
                CACHE_TTL
        );
        
        // Execute batch embedding
        List<float[]> embeddings = service.embedBatch(texts);
        
        // Verify count matches
        assertThat(embeddings.size() == texts.size())
                .as("Number of embeddings should match number of texts")
                .isTrue();
        
        // Verify each embedding is valid
        for (int i = 0; i < embeddings.size(); i++) {
            float[] embedding = embeddings.get(i);
            
            assertThat(embedding.length == TEST_DIMENSION)
                    .as("Embedding %d dimension should be %d", i, TEST_DIMENSION)
                    .isTrue();
            
            for (int j = 0; j < embedding.length; j++) {
                assertThat(!Float.isNaN(embedding[j]) && !Float.isInfinite(embedding[j]))
                        .as("Embedding %d element %d should be a valid float", i, j)
                        .isTrue();
            }
        }
    }

    /**
     * 验证降级机制正常工作
     */
    @Property(tries = 50)
    void fallbackShouldWorkWhenPrimaryProviderFails(
            @ForAll("nonBlankText") String text) {
        
        // Setup failing primary provider
        EmbeddingProvider failingProvider = mock(EmbeddingProvider.class);
        when(failingProvider.getModelName()).thenReturn("failing");
        when(failingProvider.isAvailable()).thenReturn(true);
        when(failingProvider.getPriority()).thenReturn(1);
        when(failingProvider.getDimension()).thenReturn(TEST_DIMENSION);
        when(failingProvider.getEmbedding(anyString()))
                .thenThrow(new EmbeddingException("Primary failed", "failing", true));
        
        // Setup working fallback provider
        EmbeddingProvider fallbackProvider = createMockProvider(TEST_DIMENSION);
        when(fallbackProvider.getPriority()).thenReturn(2);
        
        RedisUtil mockRedisUtil = createMockRedisUtil();
        ObjectMapper objectMapper = new ObjectMapper();
        
        EmbeddingService service = new EmbeddingServiceImpl(
                List.of(failingProvider, fallbackProvider),
                mockRedisUtil,
                objectMapper,
                true, // enable fallback
                CACHE_TTL
        );
        
        // Execute - should fallback to second provider
        float[] embedding = service.embed(text);
        
        // Verify we got a valid result from fallback
        assertThat(embedding != null)
                .as("Should get embedding from fallback provider")
                .isTrue();
        
        assertThat(embedding.length == TEST_DIMENSION)
                .as("Fallback embedding should have correct dimension")
                .isTrue();
    }

    @Provide
    Arbitrary<List<String>> nonEmptyTextList() {
        return nonBlankText()
                .list()
                .ofMinSize(1)
                .ofMaxSize(10);
    }

    @Provide
    Arbitrary<String> nonBlankText() {
        // Generate strings that are guaranteed to be non-blank
        // Combine alphanumeric characters to ensure non-whitespace content
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(100);
    }

    // Helper methods
    
    private EmbeddingProvider createMockProvider(int dimension) {
        EmbeddingProvider mock = Mockito.mock(EmbeddingProvider.class);
        when(mock.getModelName()).thenReturn("mock");
        when(mock.isAvailable()).thenReturn(true);
        when(mock.getPriority()).thenReturn(1);
        when(mock.getDimension()).thenReturn(dimension);
        when(mock.getEmbedding(anyString())).thenAnswer(invocation -> {
            return generateValidEmbedding(dimension);
        });
        return mock;
    }

    private EmbeddingProvider createMockProviderForBatch(int dimension) {
        EmbeddingProvider mock = createMockProvider(dimension);
        when(mock.getEmbeddings(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            return texts.stream()
                    .map(t -> generateValidEmbedding(dimension))
                    .toList();
        });
        return mock;
    }

    private RedisUtil createMockRedisUtil() {
        RedisUtil mock = Mockito.mock(RedisUtil.class);
        when(mock.getString(anyString())).thenReturn(null); // No cache by default
        return mock;
    }

    private float[] generateValidEmbedding(int dimension) {
        float[] embedding = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            // Generate normalized values between -1 and 1
            embedding[i] = (float) (Math.random() * 2 - 1);
        }
        return embedding;
    }

    // Simple assertion helper
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
