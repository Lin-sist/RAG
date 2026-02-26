package com.enterprise.rag.core.vectorstore.milvus;

import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.SearchResult;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.VectorStoreException;
import com.enterprise.rag.core.vectorstore.config.VectorStoreProperties;
import com.google.gson.Gson;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.QueryResults;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.ReleaseCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.response.GetCollStatResponseWrapper;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Milvus 向量存储实现
 * 基于 Milvus 2.x SDK 实现向量的存储、检索和管理
 */
public class MilvusVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStore.class);
    private static final Gson gson = new Gson();

    // 字段名称常量
    private static final String FIELD_ID = "id";
    private static final String FIELD_VECTOR = "vector";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_METADATA = "metadata";

    private final MilvusServiceClient milvusClient;
    private final VectorStoreProperties.MilvusProperties properties;

    public MilvusVectorStore(MilvusServiceClient milvusClient,
            VectorStoreProperties.MilvusProperties properties) {
        this.milvusClient = milvusClient;
        this.properties = properties;
    }

    @Override
    public void createCollection(String collectionName, int dimension) {
        if (hasCollection(collectionName)) {
            log.info("Collection {} already exists", collectionName);
            return;
        }

        log.info("Creating collection {} with dimension {}", collectionName, dimension);

        // 定义字段
        FieldType idField = FieldType.newBuilder()
                .withName(FIELD_ID)
                .withDataType(DataType.VarChar)
                .withMaxLength(256)
                .withPrimaryKey(true)
                .build();

        FieldType vectorField = FieldType.newBuilder()
                .withName(FIELD_VECTOR)
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build();

        FieldType contentField = FieldType.newBuilder()
                .withName(FIELD_CONTENT)
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build();

        FieldType metadataField = FieldType.newBuilder()
                .withName(FIELD_METADATA)
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build();

        // 创建集合
        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("RAG document vectors")
                .addFieldType(idField)
                .addFieldType(vectorField)
                .addFieldType(contentField)
                .addFieldType(metadataField)
                .build();

        R<RpcStatus> response = milvusClient.createCollection(createParam);
        handleResponse(response, "Failed to create collection: " + collectionName);

        // 创建索引
        createIndex(collectionName, dimension);

        // 加载集合到内存
        loadCollection(collectionName);

        log.info("Collection {} created successfully", collectionName);
    }

    private void createIndex(String collectionName, int dimension) {
        IndexType indexType = IndexType.valueOf(properties.getIndexType());
        MetricType metricType = MetricType.valueOf(properties.getMetricType());

        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(FIELD_VECTOR)
                .withIndexType(indexType)
                .withMetricType(metricType)
                .withExtraParam("{\"nlist\":" + properties.getNlist() + "}")
                .build();

        R<RpcStatus> response = milvusClient.createIndex(indexParam);
        handleResponse(response, "Failed to create index for collection: " + collectionName);
    }

    private void loadCollection(String collectionName) {
        LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<RpcStatus> response = milvusClient.loadCollection(loadParam);
        handleResponse(response, "Failed to load collection: " + collectionName);
    }

    @Override
    public boolean hasCollection(String collectionName) {
        HasCollectionParam param = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<Boolean> response = milvusClient.hasCollection(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new VectorStoreException("Failed to check collection existence: " + response.getMessage());
        }
        return response.getData();
    }

    @Override
    public void dropCollection(String collectionName) {
        if (!hasCollection(collectionName)) {
            log.info("Collection {} does not exist, skip dropping", collectionName);
            return;
        }

        log.info("Dropping collection {}", collectionName);

        // 先释放集合
        ReleaseCollectionParam releaseParam = ReleaseCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
        milvusClient.releaseCollection(releaseParam);

        // 删除集合
        DropCollectionParam dropParam = DropCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<RpcStatus> response = milvusClient.dropCollection(dropParam);
        handleResponse(response, "Failed to drop collection: " + collectionName);

        log.info("Collection {} dropped successfully", collectionName);
    }

    @Override
    public void upsert(String collectionName, List<VectorDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        log.debug("Upserting {} documents to collection {}", documents.size(), collectionName);

        // 先删除已存在的文档
        List<String> ids = documents.stream()
                .map(VectorDocument::id)
                .collect(Collectors.toList());
        deleteIfExists(collectionName, ids);

        // 准备插入数据
        List<String> idList = new ArrayList<>();
        List<List<Float>> vectorList = new ArrayList<>();
        List<String> contentList = new ArrayList<>();
        List<String> metadataList = new ArrayList<>();

        for (VectorDocument doc : documents) {
            idList.add(doc.id());
            vectorList.add(toFloatList(doc.vector()));
            contentList.add(doc.content() != null ? doc.content() : "");
            metadataList.add(gson.toJson(doc.metadata() != null ? doc.metadata() : Map.of()));
        }

        List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field(FIELD_ID, idList),
                new InsertParam.Field(FIELD_VECTOR, vectorList),
                new InsertParam.Field(FIELD_CONTENT, contentList),
                new InsertParam.Field(FIELD_METADATA, metadataList));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        R<MutationResult> response = milvusClient.insert(insertParam);
        handleResponse(response, "Failed to insert documents");

        log.debug("Successfully upserted {} documents", documents.size());
    }

    private void deleteIfExists(String collectionName, List<String> ids) {
        if (ids.isEmpty()) {
            return;
        }

        String expr = FIELD_ID + " in [" + ids.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",")) + "]";

        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();

        milvusClient.delete(deleteParam);
    }

    @Override
    public List<SearchResult> search(String collectionName, float[] queryVector, SearchOptions options) {
        log.debug("Searching in collection {} with topK={}", collectionName, options.topK());

        List<String> outputFields = Arrays.asList(FIELD_ID, FIELD_CONTENT, FIELD_METADATA);

        SearchParam.Builder searchBuilder = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.valueOf(properties.getMetricType()))
                .withOutFields(outputFields)
                .withTopK(options.topK())
                .withVectors(Collections.singletonList(toFloatList(queryVector)))
                .withVectorFieldName(FIELD_VECTOR)
                .withParams("{\"nprobe\":" + properties.getNprobe() + "}")
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG);

        // 添加过滤条件
        if (options.filter() != null && !options.filter().isEmpty()) {
            String filterExpr = buildFilterExpression(options.filter());
            if (!filterExpr.isEmpty()) {
                searchBuilder.withExpr(filterExpr);
            }
        }

        R<SearchResults> response = milvusClient.search(searchBuilder.build());
        handleResponse(response, "Failed to search");

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<SearchResult> results = new ArrayList<>();

        if (wrapper.getRowRecords(0) != null) {
            for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
                QueryResultsWrapper.RowRecord row = wrapper.getRowRecords(0).get(i);
                float score = wrapper.getIDScore(0).get(i).getScore();

                // 过滤低于最小分数的结果
                if (score < options.minScore()) {
                    continue;
                }

                String id = String.valueOf(row.get(FIELD_ID));
                String content = (String) row.get(FIELD_CONTENT);
                String metadataJson = (String) row.get(FIELD_METADATA);

                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = gson.fromJson(metadataJson, Map.class);

                results.add(new SearchResult(id, content, score, metadata != null ? metadata : Map.of()));
            }
        }

        // 确保按分数降序排列
        results.sort((a, b) -> Float.compare(b.score(), a.score()));

        log.debug("Found {} results", results.size());
        return results;
    }

    private String buildFilterExpression(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return "";
        }

        // 构建基于 metadata JSON 字段的过滤表达式
        // Milvus 2.x 支持 JSON 字段过滤
        List<String> conditions = new ArrayList<>();

        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 对于简单的字符串匹配，使用 JSON_CONTAINS
            if (value instanceof String) {
                conditions.add(String.format("JSON_CONTAINS(%s, '\"%s\"', '$.%s')",
                        FIELD_METADATA, value, key));
            } else if (value instanceof Number) {
                conditions.add(String.format("JSON_CONTAINS(%s, '%s', '$.%s')",
                        FIELD_METADATA, value, key));
            }
        }

        return String.join(" and ", conditions);
    }

    @Override
    public void delete(String collectionName, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        log.debug("Deleting {} documents from collection {}", ids.size(), collectionName);

        String expr = FIELD_ID + " in [" + ids.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",")) + "]";

        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();

        R<MutationResult> response = milvusClient.delete(deleteParam);
        handleResponse(response, "Failed to delete documents");

        log.debug("Successfully deleted {} documents", ids.size());
    }

    @Override
    public VectorDocument getById(String collectionName, String id) {
        List<VectorDocument> docs = getByIds(collectionName, List.of(id));
        return docs.isEmpty() ? null : docs.get(0);
    }

    @Override
    public List<VectorDocument> getByIds(String collectionName, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        String expr = FIELD_ID + " in [" + ids.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",")) + "]";

        List<String> outputFields = Arrays.asList(FIELD_ID, FIELD_VECTOR, FIELD_CONTENT, FIELD_METADATA);

        QueryParam queryParam = QueryParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .withOutFields(outputFields)
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .build();

        R<QueryResults> response = milvusClient.query(queryParam);
        handleResponse(response, "Failed to query documents");

        QueryResultsWrapper wrapper = new QueryResultsWrapper(response.getData());
        List<VectorDocument> results = new ArrayList<>();

        List<QueryResultsWrapper.RowRecord> records = wrapper.getRowRecords();
        for (QueryResultsWrapper.RowRecord row : records) {
            String docId = String.valueOf(row.get(FIELD_ID));
            String content = (String) row.get(FIELD_CONTENT);
            String metadataJson = (String) row.get(FIELD_METADATA);

            @SuppressWarnings("unchecked")
            List<Float> vectorList = (List<Float>) row.get(FIELD_VECTOR);
            float[] vector = toFloatArray(vectorList);

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = gson.fromJson(metadataJson, Map.class);

            results.add(new VectorDocument(docId, vector, content, metadata != null ? metadata : Map.of()));
        }

        return results;
    }

    @Override
    public long count(String collectionName) {
        GetCollectionStatisticsParam param = GetCollectionStatisticsParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<GetCollectionStatisticsResponse> response = milvusClient.getCollectionStatistics(param);
        handleResponse(response, "Failed to get collection statistics");

        GetCollStatResponseWrapper wrapper = new GetCollStatResponseWrapper(response.getData());
        return wrapper.getRowCount();
    }

    @Override
    public String getType() {
        return "milvus";
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

    private <T> void handleResponse(R<T> response, String errorMessage) {
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new VectorStoreException(errorMessage + ": " + response.getMessage());
        }
    }
}
