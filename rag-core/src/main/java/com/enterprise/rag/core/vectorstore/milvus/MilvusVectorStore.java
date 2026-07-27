package com.enterprise.rag.core.vectorstore.milvus;

import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.SearchResult;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.maintenance.LegacyVectorSourceReader;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.enterprise.rag.core.vectorstore.config.VectorStoreProperties;
import com.google.gson.Gson;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.DescribeCollectionResponse;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.QueryResults;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DescribeCollectionParam;
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
import io.milvus.response.DescCollResponseWrapper;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Supplier;

/**
 * Milvus 向量存储实现
 * 基于 Milvus 2.x SDK 实现向量的存储、检索和管理
 */
public class MilvusVectorStore implements VectorStore, LegacyVectorSourceReader {

    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStore.class);
    private static final Gson gson = new Gson();

    // 字段名称常量
    private static final String FIELD_ID = "id";
    private static final String FIELD_VECTOR = "vector";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_TENANT_ID = "tenant_id";
    private static final String FIELD_KB_ID = "kb_id";

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
            log.info("Milvus collection already exists");
            return;
        }

        log.info("Creating Milvus collection with dimension {}", dimension);

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

        R<RpcStatus> response = mutationCall("create", () -> milvusClient.createCollection(createParam));
        handleResponse(response, "create");

        // 创建索引
        createIndex(collectionName, dimension);

        // 加载集合到内存
        loadCollection(collectionName);

        log.info("Milvus collection created successfully");
    }

    @Override
    public void createCollection(TenantVectorScope scope, int dimension) {
        Objects.requireNonNull(scope, "scope");
        if (hasCollection(scope)) {
            log.info("Tenant-aware Milvus collection already exists");
            return;
        }

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
        FieldType tenantField = FieldType.newBuilder()
                .withName(FIELD_TENANT_ID)
                .withDataType(DataType.Int64)
                .build();
        FieldType knowledgeBaseField = FieldType.newBuilder()
                .withName(FIELD_KB_ID)
                .withDataType(DataType.Int64)
                .build();

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(scope.collectionName())
                .withDescription("Tenant-aware RAG document vectors")
                .addFieldType(idField)
                .addFieldType(vectorField)
                .addFieldType(contentField)
                .addFieldType(metadataField)
                .addFieldType(tenantField)
                .addFieldType(knowledgeBaseField)
                .build();
        R<RpcStatus> response = mutationCall("create", () -> milvusClient.createCollection(createParam));
        handleResponse(response, "create");
        createIndex(scope.collectionName(), dimension);
        loadCollection(scope.collectionName());
        log.info("Tenant-aware Milvus collection created successfully");
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

        R<RpcStatus> response = mutationCall("create_index", () -> milvusClient.createIndex(indexParam));
        handleResponse(response, "create_index");
    }

    private void loadCollection(String collectionName) {
        LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<RpcStatus> response;
        try {
            response = milvusClient.loadCollection(loadParam);
        } catch (RuntimeException e) {
            throw VectorDependencyException.unavailable("load", e);
        }
        handleResponse(response, "load");
    }

    @Override
    public boolean hasCollection(String collectionName) {
        HasCollectionParam param = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<Boolean> response;
        try {
            response = milvusClient.hasCollection(param);
        } catch (RuntimeException e) {
            throw VectorDependencyException.unavailable("has_collection", e);
        }
        handleResponse(response, "has_collection");
        return response.getData();
    }

    @Override
    public boolean hasCollection(TenantVectorScope scope) {
        Objects.requireNonNull(scope, "scope");
        return hasCollection(scope.collectionName());
    }

    @Override
    public void dropCollection(String collectionName) {
        if (!hasCollection(collectionName)) {
            log.info("Milvus collection does not exist, skip dropping");
            return;
        }

        log.info("Dropping Milvus collection");

        // 先释放集合
        ReleaseCollectionParam releaseParam = ReleaseCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
        R<RpcStatus> releaseResponse = mutationCall("release", () -> milvusClient.releaseCollection(releaseParam));
        handleResponse(releaseResponse, "release");

        // 删除集合
        DropCollectionParam dropParam = DropCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<RpcStatus> response = mutationCall("drop", () -> milvusClient.dropCollection(dropParam));
        handleResponse(response, "drop");

        log.info("Milvus collection dropped successfully");
    }

    @Override
    public void dropCollection(TenantVectorScope scope) {
        Objects.requireNonNull(scope, "scope");
        if (!isCanonicalTenantCollection(scope)) {
            throw VectorDependencyException.scopeMismatch("drop");
        }
        if (hasCollection(scope.collectionName()) && countForeignRows(scope) > 0) {
            throw VectorDependencyException.scopeMismatch("drop");
        }
        dropCollection(scope.collectionName());
    }

    @Override
    public void upsert(String collectionName, List<VectorDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        log.debug("Upserting {} documents to Milvus", documents.size());

        if (!hasCollection(collectionName)) {
            int dimension = documents.get(0).vector().length;
            log.warn("Milvus collection not found before upsert; creating with dimension {}", dimension);
            createCollection(collectionName, dimension);
        }

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

        R<MutationResult> response = mutationCall("upsert", () -> milvusClient.insert(insertParam));
        handleResponse(response, "upsert");

        log.debug("Successfully upserted {} documents", documents.size());
    }

    @Override
    public void upsert(TenantVectorScope scope, List<VectorDocument> documents) {
        Objects.requireNonNull(scope, "scope");
        if (documents == null || documents.isEmpty()) {
            return;
        }
        List<VectorDocument> scopedDocuments = documents.stream()
                .map(document -> withServerScope(scope, document))
                .toList();
        if (!hasCollection(scope)) {
            createCollection(scope, scopedDocuments.get(0).vector().length);
        }
        List<String> ids = scopedDocuments.stream().map(VectorDocument::id).toList();
        deleteIfExists(scope, ids);

        List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field(FIELD_ID, scopedDocuments.stream().map(VectorDocument::id).toList()),
                new InsertParam.Field(FIELD_VECTOR, scopedDocuments.stream()
                        .map(document -> toFloatList(document.vector())).toList()),
                new InsertParam.Field(FIELD_CONTENT, scopedDocuments.stream()
                        .map(document -> document.content() == null ? "" : document.content()).toList()),
                new InsertParam.Field(FIELD_METADATA, scopedDocuments.stream()
                        .map(document -> gson.toJson(document.metadata())).toList()),
                new InsertParam.Field(FIELD_TENANT_ID, Collections.nCopies(scopedDocuments.size(), scope.tenantId())),
                new InsertParam.Field(FIELD_KB_ID,
                        Collections.nCopies(scopedDocuments.size(), scope.knowledgeBaseId())));
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(scope.collectionName())
                .withFields(fields)
                .build();
        R<MutationResult> response = mutationCall("upsert", () -> milvusClient.insert(insertParam));
        handleResponse(response, "upsert");
    }

    private VectorDocument withServerScope(TenantVectorScope scope, VectorDocument document) {
        Objects.requireNonNull(document, "document");
        Map<String, Object> metadata = new LinkedHashMap<>(
                document.metadata() == null ? Map.of() : document.metadata());
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        rejectConflictingScope(metadata, "tenantId", scope.tenantId());
        rejectConflictingScope(metadata, "kbId", scope.knowledgeBaseId());
        metadata.put("tenantId", scope.tenantId());
        metadata.put("kbId", scope.knowledgeBaseId());
        return new VectorDocument(document.id(), document.vector(), document.content(), Map.copyOf(metadata));
    }

    private void rejectConflictingScope(Map<String, Object> metadata, String key, long expected) {
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String normalized = entry.getKey() == null
                    ? ""
                    : entry.getKey().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
            if (!normalized.equals(key.toLowerCase(Locale.ROOT))) {
                continue;
            }
            Object actual = entry.getValue();
            if (actual == null || !String.valueOf(expected).equals(String.valueOf(actual))) {
                throw new IllegalArgumentException("Vector metadata scope conflicts with server scope");
            }
        }
    }

    private void deleteIfExists(TenantVectorScope scope, List<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(scope.collectionName())
                .withExpr(scopeExpression(scope) + " and " + idsExpression(ids))
                .build();
        R<MutationResult> response = mutationCall(
                "upsert_delete_existing", () -> milvusClient.delete(deleteParam));
        handleResponse(response, "upsert_delete_existing");
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

        R<MutationResult> response = mutationCall("upsert_delete_existing", () -> milvusClient.delete(deleteParam));
        handleResponse(response, "upsert_delete_existing");
    }

    @Override
    public List<SearchResult> search(String collectionName, float[] queryVector, SearchOptions options) {
        try {
            return searchOnce(collectionName, queryVector, options);
        } catch (VectorDependencyException e) {
            throw e;
        } catch (RuntimeException e) {
            throw VectorDependencyException.unavailable("search", e);
        }
    }

    @Override
    public List<SearchResult> search(TenantVectorScope scope, float[] queryVector, SearchOptions options) {
        Objects.requireNonNull(scope, "scope");
        try {
            return searchTenantOnce(scope, queryVector, options);
        } catch (VectorDependencyException e) {
            throw e;
        } catch (RuntimeException e) {
            throw VectorDependencyException.unavailable("search", e);
        }
    }

    private List<SearchResult> searchTenantOnce(
            TenantVectorScope scope, float[] queryVector, SearchOptions options) {
        if (!hasCollection(scope)) {
            throw VectorDependencyException.indexUnavailable("search", null);
        }
        loadCollection(scope.collectionName());
        List<String> outputFields = Arrays.asList(
                FIELD_ID, FIELD_CONTENT, FIELD_METADATA, FIELD_TENANT_ID, FIELD_KB_ID);
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(scope.collectionName())
                .withMetricType(MetricType.valueOf(properties.getMetricType()))
                .withOutFields(outputFields)
                .withTopK(options.topK())
                .withVectors(Collections.singletonList(toFloatList(queryVector)))
                .withVectorFieldName(FIELD_VECTOR)
                .withParams("{\"nprobe\":" + properties.getNprobe() + "}")
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withExpr(scopeExpression(scope))
                .build();
        R<SearchResults> response = milvusClient.search(searchParam);
        handleResponse(response, "search");

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<SearchResult> results = new ArrayList<>();
        if (wrapper.getRowRecords(0) == null) {
            return results;
        }
        for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
            QueryResultsWrapper.RowRecord row = wrapper.getRowRecords(0).get(i);
            requireScopeMarker(scope, row, "search");
            SearchResultsWrapper.IDScore idScore = wrapper.getIDScore(0).get(i);
            float score = idScore.getScore();
            if (score < options.minScore()) {
                continue;
            }
            Map<String, Object> metadata = parseMetadata(row.get(FIELD_METADATA));
            if (!matchesFilter(metadata, options.filter())) {
                continue;
            }
            results.add(new SearchResult(
                    idScore.getStrID(),
                    (String) row.get(FIELD_CONTENT),
                    score,
                    metadata));
        }
        results.sort((a, b) -> Float.compare(b.score(), a.score()));
        return results;
    }

    private List<SearchResult> searchOnce(String collectionName, float[] queryVector, SearchOptions options) {
        log.debug("Searching Milvus with topK={}", options.topK());

        if (!hasCollection(collectionName)) {
            throw VectorDependencyException.indexUnavailable("search", null);
        }

        // 确保集合已加载到内存（Milvus 重启后集合会被卸载，必须重新 load 才能搜索）
        loadCollection(collectionName);

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
        handleResponse(response, "search");

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<SearchResult> results = new ArrayList<>();

        int totalRawResults = 0;
        if (wrapper.getRowRecords(0) != null) {
            totalRawResults = wrapper.getRowRecords(0).size();
            for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
                QueryResultsWrapper.RowRecord row = wrapper.getRowRecords(0).get(i);
                SearchResultsWrapper.IDScore idScore = wrapper.getIDScore(0).get(i);
                float score = idScore.getScore();
                String id = idScore.getStrID();
                String content = (String) row.get(FIELD_CONTENT);

                log.debug("Search result [{}]: id={}, score={}, minScore={}",
                        i, id, score, options.minScore());

                // 过滤低于最小分数的结果
                if (score < options.minScore()) {
                    log.debug("Filtered out result [{}] with score {} < minScore {}", i, score, options.minScore());
                    continue;
                }

                String metadataJson = (String) row.get(FIELD_METADATA);

                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = gson.fromJson(metadataJson, Map.class);

                results.add(new SearchResult(id, content, score, metadata != null ? metadata : Map.of()));
            }
        }

        // 确保按分数降序排列
        results.sort((a, b) -> Float.compare(b.score(), a.score()));

        log.info("Milvus search complete: rawResults={}, filteredResults={}",
                totalRawResults, results.size());
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

        log.debug("Deleting {} documents from Milvus", ids.size());

        String expr = FIELD_ID + " in [" + ids.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",")) + "]";

        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();

        R<MutationResult> response = mutationCall("delete", () -> milvusClient.delete(deleteParam));
        handleResponse(response, "delete");

        log.debug("Successfully deleted {} documents", ids.size());
    }

    @Override
    public void delete(TenantVectorScope scope, List<String> ids) {
        Objects.requireNonNull(scope, "scope");
        if (ids == null || ids.isEmpty()) {
            return;
        }
        rejectForeignIds(scope, ids, "delete");
        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(scope.collectionName())
                .withExpr(scopeExpression(scope) + " and " + idsExpression(ids))
                .build();
        R<MutationResult> response = mutationCall("delete", () -> milvusClient.delete(deleteParam));
        handleResponse(response, "delete");
    }

    public VectorDocument getById(String collectionName, String id) {
        List<VectorDocument> docs = getByIds(collectionName, List.of(id));
        return docs.isEmpty() ? null : docs.get(0);
    }

    @Override
    public VectorDocument getById(TenantVectorScope scope, String id) {
        List<VectorDocument> documents = getByIds(scope, List.of(id));
        return documents.isEmpty() ? null : documents.get(0);
    }

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

        R<QueryResults> response;
        try {
            response = milvusClient.query(queryParam);
        } catch (RuntimeException e) {
            throw VectorDependencyException.unavailable("query", e);
        }
        handleResponse(response, "query");

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
    public List<VectorDocument> readByIds(String sourceCollection, List<String> ids) {
        return getByIds(sourceCollection, ids);
    }

    @Override
    public int readDimension(String sourceCollection) {
        DescribeCollectionParam param = DescribeCollectionParam.newBuilder()
                .withCollectionName(sourceCollection)
                .build();
        R<DescribeCollectionResponse> response;
        try {
            response = milvusClient.describeCollection(param);
        } catch (RuntimeException e) {
            throw VectorDependencyException.unavailable("describe_collection", e);
        }
        handleResponse(response, "describe_collection");
        FieldType vectorField = new DescCollResponseWrapper(response.getData()).getVectorField();
        if (vectorField == null || vectorField.getDimension() <= 0) {
            throw VectorDependencyException.malformedResponse(
                    "describe_collection", new IllegalStateException("Milvus vector dimension missing"));
        }
        return vectorField.getDimension();
    }

    @Override
    public List<VectorDocument> getByIds(TenantVectorScope scope, List<String> ids) {
        Objects.requireNonNull(scope, "scope");
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        QueryParam queryParam = QueryParam.newBuilder()
                .withCollectionName(scope.collectionName())
                .withExpr(scopeExpression(scope) + " and " + idsExpression(ids))
                .withOutFields(Arrays.asList(
                        FIELD_ID, FIELD_VECTOR, FIELD_CONTENT, FIELD_METADATA, FIELD_TENANT_ID, FIELD_KB_ID))
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .build();
        R<QueryResults> response;
        try {
            response = milvusClient.query(queryParam);
        } catch (RuntimeException e) {
            throw VectorDependencyException.unavailable("query", e);
        }
        handleResponse(response, "query");
        QueryResultsWrapper wrapper = new QueryResultsWrapper(response.getData());
        List<VectorDocument> results = new ArrayList<>();
        for (QueryResultsWrapper.RowRecord row : wrapper.getRowRecords()) {
            requireScopeMarker(scope, row, "query");
            @SuppressWarnings("unchecked")
            List<Float> vectorList = (List<Float>) row.get(FIELD_VECTOR);
            results.add(new VectorDocument(
                    String.valueOf(row.get(FIELD_ID)),
                    toFloatArray(vectorList),
                    (String) row.get(FIELD_CONTENT),
                    parseMetadata(row.get(FIELD_METADATA))));
        }
        Set<String> returnedIds = results.stream().map(VectorDocument::id).collect(Collectors.toSet());
        List<String> missingIds = ids.stream().filter(id -> !returnedIds.contains(id)).toList();
        rejectForeignIds(scope, missingIds, "query");
        return results;
    }

    private void rejectForeignIds(TenantVectorScope scope, List<String> ids, String operation) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (VectorDocument document : getByIds(scope.collectionName(), ids)) {
            if (asLong(document.metadata().get("tenantId")) != scope.tenantId()
                    || asLong(document.metadata().get("kbId")) != scope.knowledgeBaseId()) {
                throw VectorDependencyException.scopeMismatch(operation);
            }
        }
    }

    @Override
    public long count(String collectionName) {
        GetCollectionStatisticsParam param = GetCollectionStatisticsParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<GetCollectionStatisticsResponse> response;
        try {
            response = milvusClient.getCollectionStatistics(param);
        } catch (RuntimeException e) {
            throw VectorDependencyException.unavailable("count", e);
        }
        handleResponse(response, "count");

        GetCollStatResponseWrapper wrapper = new GetCollStatResponseWrapper(response.getData());
        return wrapper.getRowCount();
    }

    @Override
    public long count(TenantVectorScope scope) {
        Objects.requireNonNull(scope, "scope");
        return countByExpression(scope.collectionName(), scopeExpression(scope), "count");
    }

    private long countForeignRows(TenantVectorScope scope) {
        String foreignExpression = FIELD_TENANT_ID + " != " + scope.tenantId()
                + " or " + FIELD_KB_ID + " != " + scope.knowledgeBaseId();
        return countByExpression(scope.collectionName(), foreignExpression, "drop_scope_audit");
    }

    private long countByExpression(String collectionName, String expression, String operation) {
        QueryParam queryParam = QueryParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expression)
                .withOutFields(List.of("count(*)"))
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .build();
        R<QueryResults> response;
        try {
            response = milvusClient.query(queryParam);
        } catch (RuntimeException e) {
            throw VectorDependencyException.unavailable(operation, e);
        }
        handleResponse(response, operation);
        if (response.getData().getFieldsDataCount() == 0) {
            return 0L;
        }
        var countField = response.getData().getFieldsData(0);
        if (countField.hasScalars()
                && countField.getScalars().hasLongData()
                && countField.getScalars().getLongData().getDataCount() == 1) {
            return countField.getScalars().getLongData().getData(0);
        }
        throw VectorDependencyException.malformedResponse(
                operation, new IllegalStateException("Milvus count response shape mismatch"));
    }

    @Override
    public String getType() {
        return "milvus";
    }

    // 辅助方法
    private String scopeExpression(TenantVectorScope scope) {
        return FIELD_TENANT_ID + " == " + scope.tenantId()
                + " and " + FIELD_KB_ID + " == " + scope.knowledgeBaseId();
    }

    private String idsExpression(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Vector ids must not be empty");
        }
        return FIELD_ID + " in [" + ids.stream()
                .map(this::quotedLiteral)
                .collect(Collectors.joining(",")) + "]";
    }

    private String quotedLiteral(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Vector id must not be blank");
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void requireScopeMarker(
            TenantVectorScope scope, QueryResultsWrapper.RowRecord row, String operation) {
        if (asLong(row.get(FIELD_TENANT_ID)) != scope.tenantId()
                || asLong(row.get(FIELD_KB_ID)) != scope.knowledgeBaseId()) {
            throw VectorDependencyException.scopeMismatch(operation);
        }
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (RuntimeException e) {
            return Long.MIN_VALUE;
        }
    }

    private Map<String, Object> parseMetadata(Object value) {
        if (!(value instanceof String json) || json.isBlank()) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = gson.fromJson(json, Map.class);
        return metadata == null ? Map.of() : metadata;
    }

    private boolean matchesFilter(Map<String, Object> metadata, Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            Object actual = metadata.get(entry.getKey());
            if (entry.getValue() != null
                    && !String.valueOf(entry.getValue()).equals(String.valueOf(actual))) {
                return false;
            }
        }
        return true;
    }

    private boolean isCanonicalTenantCollection(TenantVectorScope scope) {
        return scope.collectionName().startsWith(
                "tenant_" + scope.tenantId() + "_kb_" + scope.knowledgeBaseId() + "_");
    }

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

    private <T> R<T> mutationCall(String operation, Supplier<R<T>> call) {
        try {
            return call.get();
        } catch (VectorDependencyException e) {
            throw e;
        } catch (RuntimeException e) {
            throw VectorDependencyException.outcomeUnknown(operation, e);
        }
    }

    private <T> void handleResponse(R<T> response, String operation) {
        if (response == null || response.getStatus() == null) {
            throw VectorDependencyException.malformedResponse(operation, null);
        }
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw VectorDependencyException.rpcFailure(operation, response.getException());
        }
    }
}
