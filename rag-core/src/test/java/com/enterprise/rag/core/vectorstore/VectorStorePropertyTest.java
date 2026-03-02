package com.enterprise.rag.core.vectorstore;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量存储属性测试
 * 
 * Feature: enterprise-rag-qa-system
 * Validates: Requirements 2.4, 4.2, 4.3, 4.4
 */
class VectorStorePropertyTest {

    private static final int TEST_DIMENSION = 128;
    private static final String TEST_COLLECTION = "test_collection";

    /**
     * Property 7: 向量存储完整性
     * 
     * *For any* 存储到向量数据库的文档块，应能通过 ID 检索到完整的向量数据和元数据。
     * 
     * **Validates: Requirements 2.4, 4.2**
     */
    @Property(tries = 100)
    void storedDocumentsShouldBeRetrievableById(
            @ForAll("validVectorDocuments") List<VectorDocument> documents) {
        
        // Setup
        VectorStore store = new InMemoryVectorStore();
        store.createCollection(TEST_COLLECTION, TEST_DIMENSION);
        
        // Store documents
        store.upsert(TEST_COLLECTION, documents);
        
        // Verify each document can be retrieved by ID
        for (VectorDocument original : documents) {
            VectorDocument retrieved = store.getById(TEST_COLLECTION, original.id());
            
            assertThat(retrieved != null)
                    .as("Document with ID %s should be retrievable", original.id())
                    .isTrue();
            
            assertThat(retrieved.id().equals(original.id()))
                    .as("Retrieved document ID should match original")
                    .isTrue();
            
            assertThat(retrieved.content().equals(original.content()))
                    .as("Retrieved document content should match original")
                    .isTrue();
            
            assertThat(Arrays.equals(retrieved.vector(), original.vector()))
                    .as("Retrieved document vector should match original")
                    .isTrue();
            
            assertThat(retrieved.metadata().equals(original.metadata()))
                    .as("Retrieved document metadata should match original")
                    .isTrue();
        }
        
        // Verify count matches
        assertThat(store.count(TEST_COLLECTION) == documents.size())
                .as("Document count should match number of stored documents")
                .isTrue();
    }

    /**
     * Property 11: 向量搜索排序正确性
     * 
     * *For any* 向量搜索请求，返回的结果应按相似度分数降序排列，且结果数量不超过请求的 Top-K。
     * 
     * **Validates: Requirements 4.3**
     */
    @Property(tries = 100)
    void searchResultsShouldBeSortedByScoreDescending(
            @ForAll("validVectorDocuments") List<VectorDocument> documents,
            @ForAll @IntRange(min = 1, max = 20) int topK) {
        
        if (documents.isEmpty()) {
            return;
        }
        
        // Setup
        VectorStore store = new InMemoryVectorStore();
        store.createCollection(TEST_COLLECTION, TEST_DIMENSION);
        store.upsert(TEST_COLLECTION, documents);
        
        // Generate a query vector
        float[] queryVector = generateNormalizedVector(TEST_DIMENSION);
        
        // Search
        List<SearchResult> results = store.search(TEST_COLLECTION, queryVector, SearchOptions.withTopK(topK));
        
        // Verify results count does not exceed topK
        assertThat(results.size() <= topK)
                .as("Results count (%d) should not exceed topK (%d)", results.size(), topK)
                .isTrue();
        
        // Verify results are sorted by score descending
        for (int i = 0; i < results.size() - 1; i++) {
            float currentScore = results.get(i).score();
            float nextScore = results.get(i + 1).score();
            
            assertThat(currentScore >= nextScore)
                    .as("Results should be sorted by score descending: score[%d]=%f >= score[%d]=%f",
                            i, currentScore, i + 1, nextScore)
                    .isTrue();
        }
    }

    /**
     * Property 12: 向量搜索过滤正确性
     * 
     * *For any* 带元数据过滤条件的搜索请求，返回的所有结果都应满足过滤条件。
     * 
     * **Validates: Requirements 4.4**
     */
    @Property(tries = 100)
    void searchResultsShouldMatchFilterConditions(
            @ForAll("documentsWithCategory") List<VectorDocument> documents,
            @ForAll("categoryFilter") String filterCategory) {
        
        if (documents.isEmpty()) {
            return;
        }
        
        // Setup
        VectorStore store = new InMemoryVectorStore();
        store.createCollection(TEST_COLLECTION, TEST_DIMENSION);
        store.upsert(TEST_COLLECTION, documents);
        
        // Generate a query vector
        float[] queryVector = generateNormalizedVector(TEST_DIMENSION);
        
        // Create filter
        Map<String, Object> filter = Map.of("category", filterCategory);
        SearchOptions options = SearchOptions.withFilter(10, filter);
        
        // Search with filter
        List<SearchResult> results = store.search(TEST_COLLECTION, queryVector, options);
        
        // Verify all results match the filter
        for (SearchResult result : results) {
            Object category = result.metadata().get("category");
            
            assertThat(filterCategory.equals(category))
                    .as("Result category '%s' should match filter category '%s'", category, filterCategory)
                    .isTrue();
        }
    }

    /**
     * 验证删除操作正确移除文档
     */
    @Property(tries = 100)
    void deletedDocumentsShouldNotBeRetrievable(
            @ForAll("validVectorDocuments") List<VectorDocument> documents) {
        
        if (documents.isEmpty()) {
            return;
        }
        
        // Setup
        VectorStore store = new InMemoryVectorStore();
        store.createCollection(TEST_COLLECTION, TEST_DIMENSION);
        store.upsert(TEST_COLLECTION, documents);
        
        // Select some documents to delete
        List<String> idsToDelete = documents.stream()
                .limit(Math.max(1, documents.size() / 2))
                .map(VectorDocument::id)
                .collect(Collectors.toList());
        
        // Delete
        store.delete(TEST_COLLECTION, idsToDelete);
        
        // Verify deleted documents are not retrievable
        for (String deletedId : idsToDelete) {
            VectorDocument retrieved = store.getById(TEST_COLLECTION, deletedId);
            
            assertThat(retrieved == null)
                    .as("Deleted document with ID %s should not be retrievable", deletedId)
                    .isTrue();
        }
        
        // Verify remaining documents are still retrievable
        Set<String> deletedIds = new HashSet<>(idsToDelete);
        for (VectorDocument doc : documents) {
            if (!deletedIds.contains(doc.id())) {
                VectorDocument retrieved = store.getById(TEST_COLLECTION, doc.id());
                
                assertThat(retrieved != null)
                        .as("Non-deleted document with ID %s should still be retrievable", doc.id())
                        .isTrue();
            }
        }
    }

    /**
     * 验证 upsert 操作正确更新已存在的文档
     */
    @Property(tries = 100)
    void upsertShouldUpdateExistingDocuments(
            @ForAll("validVectorDocument") VectorDocument original) {
        
        // Setup
        VectorStore store = new InMemoryVectorStore();
        store.createCollection(TEST_COLLECTION, TEST_DIMENSION);
        
        // Insert original
        store.upsert(TEST_COLLECTION, List.of(original));
        
        // Create updated version with same ID
        String updatedContent = "updated_" + original.content();
        Map<String, Object> updatedMetadata = new HashMap<>(original.metadata());
        updatedMetadata.put("updated", true);
        
        VectorDocument updated = new VectorDocument(
                original.id(),
                original.vector(),
                updatedContent,
                updatedMetadata
        );
        
        // Upsert updated version
        store.upsert(TEST_COLLECTION, List.of(updated));
        
        // Verify only one document exists
        assertThat(store.count(TEST_COLLECTION) == 1)
                .as("Should have exactly one document after upsert")
                .isTrue();
        
        // Verify the document has updated content
        VectorDocument retrieved = store.getById(TEST_COLLECTION, original.id());
        
        assertThat(retrieved.content().equals(updatedContent))
                .as("Retrieved document should have updated content")
                .isTrue();
        
        assertThat(retrieved.metadata().get("updated") != null)
                .as("Retrieved document should have updated metadata")
                .isTrue();
    }

    /**
     * 验证最小分数过滤正确工作
     */
    @Property(tries = 50)
    void searchShouldRespectMinScoreThreshold(
            @ForAll("validVectorDocuments") List<VectorDocument> documents,
            @ForAll @FloatRange(min = 0.0f, max = 1.0f) float minScore) {
        
        if (documents.isEmpty()) {
            return;
        }
        
        // Setup
        VectorStore store = new InMemoryVectorStore();
        store.createCollection(TEST_COLLECTION, TEST_DIMENSION);
        store.upsert(TEST_COLLECTION, documents);
        
        // Generate a query vector
        float[] queryVector = generateNormalizedVector(TEST_DIMENSION);
        
        // Search with minScore
        SearchOptions options = new SearchOptions(10, minScore, Map.of());
        List<SearchResult> results = store.search(TEST_COLLECTION, queryVector, options);
        
        // Verify all results have score >= minScore
        for (SearchResult result : results) {
            assertThat(result.score() >= minScore)
                    .as("Result score %f should be >= minScore %f", result.score(), minScore)
                    .isTrue();
        }
    }

    // Providers

    @Provide
    Arbitrary<VectorDocument> validVectorDocument() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100)
        ).as((id, content) -> {
            float[] vector = generateNormalizedVector(TEST_DIMENSION);
            Map<String, Object> metadata = Map.of("source", "test");
            return new VectorDocument(id, vector, content, metadata);
        });
    }

    @Provide
    Arbitrary<List<VectorDocument>> validVectorDocuments() {
        return validVectorDocument()
                .list()
                .ofMinSize(1)
                .ofMaxSize(20)
                .map(docs -> {
                    // Ensure unique IDs
                    Set<String> seenIds = new HashSet<>();
                    return docs.stream()
                            .filter(doc -> seenIds.add(doc.id()))
                            .collect(Collectors.toList());
                })
                .filter(docs -> !docs.isEmpty());
    }

    @Provide
    Arbitrary<List<VectorDocument>> documentsWithCategory() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100),
                categoryFilter()
        ).as((id, content, category) -> {
            float[] vector = generateNormalizedVector(TEST_DIMENSION);
            Map<String, Object> metadata = Map.of("category", category, "source", "test");
            return new VectorDocument(id, vector, content, metadata);
        }).list()
                .ofMinSize(1)
                .ofMaxSize(20)
                .map(docs -> {
                    // Ensure unique IDs
                    Set<String> seenIds = new HashSet<>();
                    return docs.stream()
                            .filter(doc -> seenIds.add(doc.id()))
                            .collect(Collectors.toList());
                })
                .filter(docs -> !docs.isEmpty());
    }

    @Provide
    Arbitrary<String> categoryFilter() {
        return Arbitraries.of("tech", "science", "business", "health");
    }

    // Helper methods

    private float[] generateNormalizedVector(int dimension) {
        float[] vector = new float[dimension];
        float norm = 0.0f;
        
        Random random = new Random();
        for (int i = 0; i < dimension; i++) {
            vector[i] = random.nextFloat() * 2 - 1;
            norm += vector[i] * vector[i];
        }
        
        // Normalize
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                vector[i] /= norm;
            }
        }
        
        return vector;
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
