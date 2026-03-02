package com.enterprise.rag.core.vectorstore.elasticsearch;

import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.SearchResult;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.VectorStoreException;
import com.enterprise.rag.core.vectorstore.config.VectorStoreProperties.ElasticsearchProperties;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Elasticsearch 向量存储实现
 * 基于 Elasticsearch 8.x Java Client 实现向量的存储、检索和管理
 * 使用 dense_vector 字段类型和 kNN 搜索
 */
public class ElasticsearchVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchVectorStore.class);

    // 字段名称常量
    private static final String FIELD_VECTOR = "vector";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_METADATA = "metadata";

    private final ElasticsearchClient esClient;
    private final ElasticsearchProperties properties;

    public ElasticsearchVectorStore(ElasticsearchClient esClient, ElasticsearchProperties properties) {
        this.esClient = esClient;
        this.properties = properties;
    }

    @Override
    public void createCollection(String collectionName, int dimension) {
        try {
            if (hasCollection(collectionName)) {
                log.info("Index {} already exists", collectionName);
                return;
            }

            log.info("Creating index {} with dimension {}", collectionName, dimension);

            // 构建映射
            Map<String, Property> propertyMap = new HashMap<>();
            
            // 向量字段
            propertyMap.put(FIELD_VECTOR, Property.of(p -> p
                    .denseVector(DenseVectorProperty.of(dv -> dv
                            .dims(dimension)
                            .index(true)
                            .similarity(properties.getSimilarity())
                    ))
            ));
            
            // 内容字段
            propertyMap.put(FIELD_CONTENT, Property.of(p -> p
                    .text(TextProperty.of(t -> t))
            ));
            
            // 元数据字段（作为 object 存储）
            propertyMap.put(FIELD_METADATA, Property.of(p -> p
                    .object(o -> o.enabled(true))
            ));

            CreateIndexRequest request = CreateIndexRequest.of(r -> r
                    .index(collectionName)
                    .mappings(TypeMapping.of(m -> m.properties(propertyMap)))
            );

            esClient.indices().create(request);

            log.info("Index {} created successfully", collectionName);
        } catch (IOException e) {
            throw new VectorStoreException("Failed to create index: " + collectionName, e);
        }
    }

    @Override
    public boolean hasCollection(String collectionName) {
        try {
            return esClient.indices().exists(ExistsRequest.of(r -> r.index(collectionName))).value();
        } catch (IOException e) {
            throw new VectorStoreException("Failed to check index existence", e);
        }
    }

    @Override
    public void dropCollection(String collectionName) {
        try {
            if (!hasCollection(collectionName)) {
                log.info("Index {} does not exist, skip dropping", collectionName);
                return;
            }

            log.info("Dropping index {}", collectionName);
            esClient.indices().delete(DeleteIndexRequest.of(r -> r.index(collectionName)));
            log.info("Index {} dropped successfully", collectionName);
        } catch (IOException e) {
            throw new VectorStoreException("Failed to drop index: " + collectionName, e);
        }
    }

    @Override
    public void upsert(String collectionName, List<VectorDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        log.debug("Upserting {} documents to index {}", documents.size(), collectionName);

        try {
            List<BulkOperation> operations = documents.stream()
                    .map(doc -> BulkOperation.of(op -> op
                            .index(IndexOperation.of(idx -> idx
                                    .index(collectionName)
                                    .id(doc.id())
                                    .document(toDocument(doc))
                            ))
                    ))
                    .collect(Collectors.toList());

            BulkRequest bulkRequest = BulkRequest.of(r -> r.operations(operations));
            BulkResponse response = esClient.bulk(bulkRequest);

            if (response.errors()) {
                throw new VectorStoreException("Bulk upsert had errors");
            }

            log.debug("Successfully upserted {} documents", documents.size());
        } catch (IOException e) {
            throw new VectorStoreException("Failed to upsert documents", e);
        }
    }

    private Map<String, Object> toDocument(VectorDocument doc) {
        Map<String, Object> document = new HashMap<>();
        document.put(FIELD_VECTOR, toFloatList(doc.vector()));
        document.put(FIELD_CONTENT, doc.content() != null ? doc.content() : "");
        document.put(FIELD_METADATA, doc.metadata() != null ? doc.metadata() : Map.of());
        return document;
    }

    @Override
    public List<SearchResult> search(String collectionName, float[] queryVector, SearchOptions options) {
        log.debug("Searching in index {} with topK={}", collectionName, options.topK());

        try {
            // 构建 kNN 查询
            KnnQuery.Builder knnBuilder = new KnnQuery.Builder()
                    .field(FIELD_VECTOR)
                    .queryVector(toFloatList(queryVector))
                    .k(options.topK())
                    .numCandidates(options.topK() * 2);

            // 添加过滤条件
            if (options.filter() != null && !options.filter().isEmpty()) {
                Query filterQuery = buildFilterQuery(options.filter());
                if (filterQuery != null) {
                    knnBuilder.filter(filterQuery);
                }
            }

            SearchRequest searchRequest = SearchRequest.of(r -> r
                    .index(collectionName)
                    .knn(knnBuilder.build())
                    .size(options.topK())
            );

            SearchResponse<Map> response = esClient.search(searchRequest, Map.class);

            List<SearchResult> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                float score = hit.score() != null ? hit.score().floatValue() : 0.0f;
                
                // 过滤低于最小分数的结果
                if (score < options.minScore()) {
                    continue;
                }

                Map<String, Object> source = hit.source();
                if (source != null) {
                    String content = (String) source.getOrDefault(FIELD_CONTENT, "");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = (Map<String, Object>) source.getOrDefault(FIELD_METADATA, Map.of());
                    
                    results.add(new SearchResult(hit.id(), content, score, metadata));
                }
            }

            // 确保按分数降序排列
            results.sort((a, b) -> Float.compare(b.score(), a.score()));

            log.debug("Found {} results", results.size());
            return results;
        } catch (IOException e) {
            throw new VectorStoreException("Failed to search", e);
        }
    }

    private Query buildFilterQuery(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return null;
        }

        List<Query> mustQueries = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = FIELD_METADATA + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                mustQueries.add(Query.of(q -> q
                        .term(t -> t.field(key).value((String) value))
                ));
            } else if (value instanceof Number) {
                mustQueries.add(Query.of(q -> q
                        .term(t -> t.field(key).value(((Number) value).longValue()))
                ));
            }
        }

        if (mustQueries.isEmpty()) {
            return null;
        }

        return Query.of(q -> q.bool(b -> b.must(mustQueries)));
    }

    @Override
    public void delete(String collectionName, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        log.debug("Deleting {} documents from index {}", ids.size(), collectionName);

        try {
            DeleteByQueryRequest request = DeleteByQueryRequest.of(r -> r
                    .index(collectionName)
                    .query(Query.of(q -> q
                            .ids(i -> i.values(ids))
                    ))
            );

            esClient.deleteByQuery(request);

            log.debug("Successfully deleted {} documents", ids.size());
        } catch (IOException e) {
            throw new VectorStoreException("Failed to delete documents", e);
        }
    }

    @Override
    public VectorDocument getById(String collectionName, String id) {
        try {
            GetResponse<Map> response = esClient.get(g -> g
                    .index(collectionName)
                    .id(id), Map.class);

            if (!response.found() || response.source() == null) {
                return null;
            }

            return toVectorDocument(id, response.source());
        } catch (IOException e) {
            throw new VectorStoreException("Failed to get document by ID", e);
        }
    }

    @Override
    public List<VectorDocument> getByIds(String collectionName, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        try {
            MgetRequest request = MgetRequest.of(r -> r
                    .index(collectionName)
                    .ids(ids)
            );

            MgetResponse<Map> response = esClient.mget(request, Map.class);

            List<VectorDocument> results = new ArrayList<>();
            for (MultiGetResponseItem<Map> item : response.docs()) {
                if (item.isResult() && item.result().found() && item.result().source() != null) {
                    results.add(toVectorDocument(item.result().id(), item.result().source()));
                }
            }

            return results;
        } catch (IOException e) {
            throw new VectorStoreException("Failed to get documents by IDs", e);
        }
    }

    @SuppressWarnings("unchecked")
    private VectorDocument toVectorDocument(String id, Map<String, Object> source) {
        String content = (String) source.getOrDefault(FIELD_CONTENT, "");
        Map<String, Object> metadata = (Map<String, Object>) source.getOrDefault(FIELD_METADATA, Map.of());
        
        // 获取向量
        List<Number> vectorList = (List<Number>) source.get(FIELD_VECTOR);
        float[] vector = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            vector[i] = vectorList.get(i).floatValue();
        }

        return new VectorDocument(id, vector, content, metadata);
    }

    @Override
    public long count(String collectionName) {
        try {
            return esClient.count(c -> c.index(collectionName)).count();
        } catch (IOException e) {
            throw new VectorStoreException("Failed to get document count", e);
        }
    }

    @Override
    public String getType() {
        return "elasticsearch";
    }

    // 辅助方法
    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }
}
