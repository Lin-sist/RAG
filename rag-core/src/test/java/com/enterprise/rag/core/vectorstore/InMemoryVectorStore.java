package com.enterprise.rag.core.vectorstore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存向量存储实现（仅用于测试）
 * 提供完整的 VectorStore 接口实现，用于属性测试
 */
public class InMemoryVectorStore implements VectorStore {

    private final Map<String, Collection> collections = new ConcurrentHashMap<>();

    private static class Collection {
        final int dimension;
        final Map<String, VectorDocument> documents = new ConcurrentHashMap<>();

        Collection(int dimension) {
            this.dimension = dimension;
        }
    }

    @Override
    public void createCollection(String collectionName, int dimension) {
        collections.putIfAbsent(collectionName, new Collection(dimension));
    }

    @Override
    public boolean hasCollection(String collectionName) {
        return collections.containsKey(collectionName);
    }

    @Override
    public void dropCollection(String collectionName) {
        collections.remove(collectionName);
    }

    @Override
    public void upsert(String collectionName, List<VectorDocument> documents) {
        Collection collection = collections.get(collectionName);
        if (collection == null) {
            throw new VectorStoreException("Collection not found: " + collectionName);
        }
        for (VectorDocument doc : documents) {
            collection.documents.put(doc.id(), doc);
        }
    }

    @Override
    public List<SearchResult> search(String collectionName, float[] queryVector, SearchOptions options) {
        Collection collection = collections.get(collectionName);
        if (collection == null) {
            throw new VectorStoreException("Collection not found: " + collectionName);
        }

        List<SearchResult> results = new ArrayList<>();
        
        for (VectorDocument doc : collection.documents.values()) {
            // 计算余弦相似度
            float score = cosineSimilarity(queryVector, doc.vector());
            
            // 过滤低于最小分数的结果
            if (score < options.minScore()) {
                continue;
            }
            
            // 应用元数据过滤
            if (!matchesFilter(doc.metadata(), options.filter())) {
                continue;
            }
            
            results.add(new SearchResult(doc.id(), doc.content(), score, doc.metadata()));
        }

        // 按分数降序排列
        results.sort((a, b) -> Float.compare(b.score(), a.score()));

        // 限制返回数量
        if (results.size() > options.topK()) {
            results = results.subList(0, options.topK());
        }

        return results;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0f;
        }
        
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0.0f || normB == 0.0f) {
            return 0.0f;
        }
        
        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private boolean matchesFilter(Map<String, Object> metadata, Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            Object metaValue = metadata.get(entry.getKey());
            if (metaValue == null || !metaValue.equals(entry.getValue())) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void delete(String collectionName, List<String> ids) {
        Collection collection = collections.get(collectionName);
        if (collection == null) {
            return;
        }
        for (String id : ids) {
            collection.documents.remove(id);
        }
    }

    @Override
    public VectorDocument getById(String collectionName, String id) {
        Collection collection = collections.get(collectionName);
        if (collection == null) {
            return null;
        }
        return collection.documents.get(id);
    }

    @Override
    public List<VectorDocument> getByIds(String collectionName, List<String> ids) {
        Collection collection = collections.get(collectionName);
        if (collection == null) {
            return List.of();
        }
        return ids.stream()
                .map(id -> collection.documents.get(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public long count(String collectionName) {
        Collection collection = collections.get(collectionName);
        if (collection == null) {
            return 0;
        }
        return collection.documents.size();
    }

    @Override
    public String getType() {
        return "in-memory";
    }
}
