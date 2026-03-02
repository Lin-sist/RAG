package com.enterprise.rag.core.vectorstore.qdrant;

import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.SearchResult;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.VectorStoreException;
import com.google.gson.Gson;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.CollectionInfo;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.FieldCondition;
import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.Match;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import io.qdrant.client.grpc.Points.WithVectorsSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

/**
 * Qdrant 向量存储实现
 * 基于 Qdrant Java Client 实现向量的存储、检索和管理
 */
public class QdrantVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStore.class);
    private static final Gson gson = new Gson();

    // Payload 字段名称常量
    private static final String PAYLOAD_CONTENT = "content";
    private static final String PAYLOAD_METADATA = "metadata";
    private static final String PAYLOAD_ORIGINAL_ID = "original_id";

    private final QdrantClient qdrantClient;

    public QdrantVectorStore(QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
    }

    @Override
    public void createCollection(String collectionName, int dimension) {
        try {
            if (hasCollection(collectionName)) {
                log.info("Collection {} already exists", collectionName);
                return;
            }

            log.info("Creating collection {} with dimension {}", collectionName, dimension);

            qdrantClient.createCollectionAsync(
                    collectionName,
                    VectorParams.newBuilder()
                            .setDistance(Distance.Cosine)
                            .setSize(dimension)
                            .build()
            ).get();

            log.info("Collection {} created successfully", collectionName);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Failed to create collection: " + collectionName, e);
        }
    }

    @Override
    public boolean hasCollection(String collectionName) {
        try {
            // 尝试获取集合信息，如果不存在会抛出异常
            qdrantClient.getCollectionInfoAsync(collectionName).get();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Failed to check collection existence", e);
        } catch (ExecutionException e) {
            // 集合不存在时会抛出异常
            return false;
        }
    }

    @Override
    public void dropCollection(String collectionName) {
        try {
            if (!hasCollection(collectionName)) {
                log.info("Collection {} does not exist, skip dropping", collectionName);
                return;
            }

            log.info("Dropping collection {}", collectionName);
            qdrantClient.deleteCollectionAsync(collectionName).get();
            log.info("Collection {} dropped successfully", collectionName);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Failed to drop collection: " + collectionName, e);
        }
    }

    @Override
    public void upsert(String collectionName, List<VectorDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        log.debug("Upserting {} documents to collection {}", documents.size(), collectionName);

        try {
            List<PointStruct> points = documents.stream()
                    .map(this::toPointStruct)
                    .collect(Collectors.toList());

            qdrantClient.upsertAsync(collectionName, points).get();

            log.debug("Successfully upserted {} documents", documents.size());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Failed to upsert documents", e);
        }
    }

    private PointStruct toPointStruct(VectorDocument doc) {
        Map<String, JsonWithInt.Value> payload = new HashMap<>();
        payload.put(PAYLOAD_CONTENT, value(doc.content() != null ? doc.content() : ""));
        payload.put(PAYLOAD_METADATA, value(gson.toJson(doc.metadata() != null ? doc.metadata() : Map.of())));
        payload.put(PAYLOAD_ORIGINAL_ID, value(doc.id()));

        return PointStruct.newBuilder()
                .setId(id(UUID.nameUUIDFromBytes(doc.id().getBytes())))
                .setVectors(vectors(toFloatList(doc.vector())))
                .putAllPayload(payload)
                .build();
    }

    @Override
    public List<SearchResult> search(String collectionName, float[] queryVector, SearchOptions options) {
        log.debug("Searching in collection {} with topK={}", collectionName, options.topK());

        try {
            Points.SearchPoints.Builder searchBuilder = Points.SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(toFloatList(queryVector))
                    .setLimit(options.topK())
                    .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build());

            // 添加过滤条件
            if (options.filter() != null && !options.filter().isEmpty()) {
                Filter filter = buildFilter(options.filter());
                if (filter != null) {
                    searchBuilder.setFilter(filter);
                }
            }

            // 设置最小分数阈值
            if (options.minScore() > 0) {
                searchBuilder.setScoreThreshold(options.minScore());
            }

            List<ScoredPoint> scoredPoints = qdrantClient.searchAsync(
                    searchBuilder.build()
            ).get();

            List<SearchResult> results = scoredPoints.stream()
                    .map(this::toSearchResult)
                    .sorted((a, b) -> Float.compare(b.score(), a.score()))
                    .collect(Collectors.toList());

            log.debug("Found {} results", results.size());
            return results;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Failed to search", e);
        }
    }

    private Filter buildFilter(Map<String, Object> filterMap) {
        if (filterMap == null || filterMap.isEmpty()) {
            return null;
        }

        List<Condition> conditions = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : filterMap.entrySet()) {
            String key = "metadata." + entry.getKey();
            Object filterValue = entry.getValue();

            if (filterValue instanceof String) {
                conditions.add(Condition.newBuilder()
                        .setField(FieldCondition.newBuilder()
                                .setKey(key)
                                .setMatch(Match.newBuilder()
                                        .setKeyword((String) filterValue)
                                        .build())
                                .build())
                        .build());
            } else if (filterValue instanceof Number) {
                conditions.add(Condition.newBuilder()
                        .setField(FieldCondition.newBuilder()
                                .setKey(key)
                                .setMatch(Match.newBuilder()
                                        .setInteger(((Number) filterValue).longValue())
                                        .build())
                                .build())
                        .build());
            }
        }

        if (conditions.isEmpty()) {
            return null;
        }

        return Filter.newBuilder()
                .addAllMust(conditions)
                .build();
    }

    private SearchResult toSearchResult(ScoredPoint point) {
        Map<String, JsonWithInt.Value> payload = point.getPayloadMap();
        
        String content = payload.containsKey(PAYLOAD_CONTENT) 
                ? payload.get(PAYLOAD_CONTENT).getStringValue() 
                : "";
        
        String metadataJson = payload.containsKey(PAYLOAD_METADATA) 
                ? payload.get(PAYLOAD_METADATA).getStringValue() 
                : "{}";

        String originalId = payload.containsKey(PAYLOAD_ORIGINAL_ID)
                ? payload.get(PAYLOAD_ORIGINAL_ID).getStringValue()
                : point.getId().getUuid();

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = gson.fromJson(metadataJson, Map.class);

        return new SearchResult(originalId, content, point.getScore(), metadata != null ? metadata : Map.of());
    }

    @Override
    public void delete(String collectionName, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        log.debug("Deleting {} documents from collection {}", ids.size(), collectionName);

        try {
            List<Points.PointId> pointIds = ids.stream()
                    .map(idStr -> Points.PointId.newBuilder()
                            .setUuid(UUID.nameUUIDFromBytes(idStr.getBytes()).toString())
                            .build())
                    .collect(Collectors.toList());

            qdrantClient.deleteAsync(collectionName, pointIds).get();

            log.debug("Successfully deleted {} documents", ids.size());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Failed to delete documents", e);
        }
    }

    @Override
    public VectorDocument getById(String collectionName, String id) {
        List<VectorDocument> docs = getByIds(collectionName, List.of(id));
        return docs.isEmpty() ? null : docs.get(0);
    }

    @Override
    public List<VectorDocument> getByIds(String collectionName, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        try {
            List<Points.PointId> pointIds = ids.stream()
                    .map(idStr -> Points.PointId.newBuilder()
                            .setUuid(UUID.nameUUIDFromBytes(idStr.getBytes()).toString())
                            .build())
                    .collect(Collectors.toList());

            List<Points.RetrievedPoint> points = qdrantClient.retrieveAsync(
                    collectionName,
                    pointIds,
                    WithPayloadSelector.newBuilder().setEnable(true).build(),
                    WithVectorsSelector.newBuilder().setEnable(true).build(),
                    null
            ).get();

            return points.stream()
                    .map(this::toVectorDocument)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Failed to get documents by IDs", e);
        }
    }

    private VectorDocument toVectorDocument(Points.RetrievedPoint point) {
        Map<String, JsonWithInt.Value> payload = point.getPayloadMap();
        
        String content = payload.containsKey(PAYLOAD_CONTENT) 
                ? payload.get(PAYLOAD_CONTENT).getStringValue() 
                : "";
        
        String metadataJson = payload.containsKey(PAYLOAD_METADATA) 
                ? payload.get(PAYLOAD_METADATA).getStringValue() 
                : "{}";

        String originalId = payload.containsKey(PAYLOAD_ORIGINAL_ID)
                ? payload.get(PAYLOAD_ORIGINAL_ID).getStringValue()
                : point.getId().getUuid();

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = gson.fromJson(metadataJson, Map.class);
        
        // 获取向量
        List<Float> vectorList = point.getVectors().getVector().getDataList();
        float[] vector = toFloatArray(vectorList);

        return new VectorDocument(originalId, vector, content, metadata != null ? metadata : Map.of());
    }

    @Override
    public long count(String collectionName) {
        try {
            CollectionInfo info = qdrantClient.getCollectionInfoAsync(collectionName).get();
            return info.getPointsCount();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Failed to get collection count", e);
        }
    }

    @Override
    public String getType() {
        return "qdrant";
    }

    // 辅助方法
    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
