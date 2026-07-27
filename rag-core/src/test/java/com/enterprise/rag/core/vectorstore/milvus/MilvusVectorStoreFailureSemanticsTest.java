package com.enterprise.rag.core.vectorstore.milvus;

import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.enterprise.rag.core.vectorstore.config.VectorStoreProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.R;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.grpc.DataType;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MilvusVectorStoreFailureSemanticsTest {

    private static final String SENSITIVE_MARKER = "secret-host:19530/collection-private";

    private MilvusServiceClient milvusClient;
    private MilvusVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        milvusClient = mock(MilvusServiceClient.class);
        VectorStoreProperties.MilvusProperties properties = new VectorStoreProperties.MilvusProperties();
        properties.setMetricType("COSINE");
        vectorStore = new MilvusVectorStore(milvusClient, properties);
        when(milvusClient.hasCollection(any(HasCollectionParam.class))).thenReturn(R.success(true));
    }

    @Test
    void searchConnectionFailureShouldExposeStableUnavailableWithoutRawMessage() {
        when(milvusClient.loadCollection(any(LoadCollectionParam.class)))
                .thenThrow(new IllegalStateException(SENSITIVE_MARKER, new ConnectException("refused")));

        VectorDependencyException exception = assertThrows(
                VectorDependencyException.class,
                () -> vectorStore.search(
                        "collection-private",
                        new float[] { 0.1f, 0.2f },
                        new SearchOptions(3, 0.0f, Map.of())));

        assertEquals(VectorDependencyException.ERROR_CODE_UNAVAILABLE, exception.getErrorCode());
        assertEquals("milvus", exception.getDependency());
        assertEquals("load", exception.getOperation());
        assertEquals("connection", exception.getErrorCategory());
        assertEquals("closed", exception.getFailMode());
        assertFalse(exception.getMessage().contains(SENSITIVE_MARKER));
    }

    @Test
    void insertThrownExceptionShouldExposeOutcomeUnknownWithoutAutomaticReplay() {
        when(milvusClient.delete(any(DeleteParam.class))).thenReturn(R.success());
        when(milvusClient.insert(any(InsertParam.class)))
                .thenThrow(new IllegalStateException(SENSITIVE_MARKER));

        VectorDependencyException exception = assertThrows(
                VectorDependencyException.class,
                () -> vectorStore.upsert("collection-private", List.of(
                        new VectorDocument("doc-1", new float[] { 0.1f, 0.2f }, "content", Map.of()))));

        assertEquals(VectorDependencyException.ERROR_CODE_OUTCOME_UNKNOWN, exception.getErrorCode());
        assertEquals("upsert", exception.getOperation());
        assertEquals("outcome_unknown", exception.getFailMode());
        org.mockito.Mockito.verify(milvusClient, org.mockito.Mockito.times(1)).insert(any(InsertParam.class));
        assertFalse(exception.getMessage().contains(SENSITIVE_MARKER));
    }

    @Test
    void missingCollectionShouldBeDifferentFromNoResultAndShouldNotAutoCreateOnSearch() {
        when(milvusClient.hasCollection(any(HasCollectionParam.class))).thenReturn(R.success(false));

        VectorDependencyException exception = assertThrows(
                VectorDependencyException.class,
                () -> vectorStore.search(
                        "collection-private",
                        new float[] { 0.1f, 0.2f },
                        new SearchOptions(3, 0.0f, Map.of())));

        assertEquals(VectorDependencyException.ERROR_CODE_INDEX_UNAVAILABLE, exception.getErrorCode());
        org.mockito.Mockito.verify(milvusClient, org.mockito.Mockito.never())
                .createCollection(any(CreateCollectionParam.class));
        org.mockito.Mockito.verify(milvusClient, org.mockito.Mockito.never()).search(any(SearchParam.class));
    }

    @Test
    void nonSuccessSdkStatusShouldUseStableRpcCategoryWithoutRawMessage() {
        when(milvusClient.loadCollection(any(LoadCollectionParam.class)))
                .thenReturn(R.failed(R.Status.UnexpectedError, SENSITIVE_MARKER));

        VectorDependencyException exception = assertThrows(
                VectorDependencyException.class,
                () -> vectorStore.search(
                        "collection-private",
                        new float[] { 0.1f, 0.2f },
                        new SearchOptions(3, 0.0f, Map.of())));

        assertEquals("rpc", exception.getErrorCategory());
        assertFalse(exception.getMessage().contains(SENSITIVE_MARKER));
    }

    @Test
    void nullSdkResponseShouldUseStableSerializationCategory() {
        when(milvusClient.loadCollection(any(LoadCollectionParam.class))).thenReturn(null);

        VectorDependencyException exception = assertThrows(
                VectorDependencyException.class,
                () -> vectorStore.search(
                        "collection-private",
                        new float[] { 0.1f, 0.2f },
                        new SearchOptions(3, 0.0f, Map.of())));

        assertEquals("serialization", exception.getErrorCategory());
    }

    @Test
    void upsertShouldRejectMetadataThatConflictsWithServerTenantScope() {
        TenantVectorScope scope = new TenantVectorScope(11L, 31L, "collection-private");

        assertThrows(IllegalArgumentException.class,
                () -> vectorStore.upsert(scope, List.of(new VectorDocument(
                        "doc-1", new float[] {0.1f, 0.2f}, "content",
                        Map.of("tenantId", 12L, "kbId", 31L)))));

        verify(milvusClient, never()).insert(any(InsertParam.class));
    }

    @Test
    void createShouldDeclareIndependentTenantAndKnowledgeBaseScalarFields() {
        when(milvusClient.hasCollection(any(HasCollectionParam.class))).thenReturn(R.success(false));
        when(milvusClient.createCollection(any(CreateCollectionParam.class))).thenReturn(R.success());
        when(milvusClient.createIndex(any(CreateIndexParam.class))).thenReturn(R.success());
        when(milvusClient.loadCollection(any(LoadCollectionParam.class))).thenReturn(R.success());
        TenantVectorScope scope = new TenantVectorScope(11L, 31L, "tenant_11_kb_31_v2");

        vectorStore.createCollection(scope, 2);

        ArgumentCaptor<CreateCollectionParam> captor = ArgumentCaptor.forClass(CreateCollectionParam.class);
        verify(milvusClient).createCollection(captor.capture());
        Map<String, DataType> fields = captor.getValue().getFieldTypes().stream()
                .collect(java.util.stream.Collectors.toMap(
                        io.milvus.param.collection.FieldType::getName,
                        io.milvus.param.collection.FieldType::getDataType));
        assertEquals(DataType.Int64, fields.get("tenant_id"));
        assertEquals(DataType.Int64, fields.get("kb_id"));
    }

    @Test
    void upsertShouldWriteServerScopeMarkersAndScopeItsReplacementDelete() {
        when(milvusClient.delete(any(DeleteParam.class))).thenReturn(R.success());
        when(milvusClient.insert(any(InsertParam.class)))
                .thenThrow(new IllegalStateException("synthetic insert stop"));
        TenantVectorScope scope = new TenantVectorScope(11L, 31L, "tenant_11_kb_31_v2");

        assertThrows(VectorDependencyException.class,
                () -> vectorStore.upsert(scope, List.of(new VectorDocument(
                        "doc-1", new float[] {0.1f, 0.2f}, "content", Map.of()))));

        ArgumentCaptor<DeleteParam> deleteCaptor = ArgumentCaptor.forClass(DeleteParam.class);
        verify(milvusClient).delete(deleteCaptor.capture());
        assertTrue(deleteCaptor.getValue().getExpr().contains("tenant_id == 11"));
        assertTrue(deleteCaptor.getValue().getExpr().contains("kb_id == 31"));
        ArgumentCaptor<InsertParam> insertCaptor = ArgumentCaptor.forClass(InsertParam.class);
        verify(milvusClient).insert(insertCaptor.capture());
        Map<String, List<?>> fields = insertCaptor.getValue().getFields().stream()
                .collect(java.util.stream.Collectors.toMap(
                        InsertParam.Field::getName, InsertParam.Field::getValues));
        assertEquals(List.of(11L), fields.get("tenant_id"));
        assertEquals(List.of(31L), fields.get("kb_id"));
    }

    @Test
    void searchDeleteGetAndCountShouldAllCarryServerScopePredicates() {
        when(milvusClient.loadCollection(any(LoadCollectionParam.class))).thenReturn(R.success());
        when(milvusClient.search(any(SearchParam.class)))
                .thenThrow(new IllegalStateException("synthetic search stop"));
        when(milvusClient.delete(any(DeleteParam.class))).thenReturn(R.success());
        when(milvusClient.query(any(QueryParam.class)))
                .thenReturn(R.success(io.milvus.grpc.QueryResults.newBuilder().build()))
                .thenThrow(new IllegalStateException("synthetic query stop"));
        TenantVectorScope scope = new TenantVectorScope(11L, 31L, "tenant_11_kb_31_v2");

        assertThrows(VectorDependencyException.class,
                () -> vectorStore.search(scope, new float[] {0.1f, 0.2f},
                        new SearchOptions(3, 0.0f, Map.of("docType", "md"))));
        vectorStore.delete(scope, List.of("doc-1"));
        assertThrows(VectorDependencyException.class,
                () -> vectorStore.getByIds(scope, List.of("doc-1")));
        assertThrows(VectorDependencyException.class, () -> vectorStore.count(scope));

        ArgumentCaptor<SearchParam> searchCaptor = ArgumentCaptor.forClass(SearchParam.class);
        verify(milvusClient).search(searchCaptor.capture());
        assertEquals("tenant_id == 11 and kb_id == 31", searchCaptor.getValue().getExpr());

        ArgumentCaptor<DeleteParam> deleteCaptor = ArgumentCaptor.forClass(DeleteParam.class);
        verify(milvusClient).delete(deleteCaptor.capture());
        assertTrue(deleteCaptor.getValue().getExpr().contains("tenant_id == 11"));
        assertTrue(deleteCaptor.getValue().getExpr().contains("kb_id == 31"));

        ArgumentCaptor<QueryParam> queryCaptor = ArgumentCaptor.forClass(QueryParam.class);
        verify(milvusClient, org.mockito.Mockito.times(3)).query(queryCaptor.capture());
        assertEquals("id in [\"doc-1\"]", queryCaptor.getAllValues().get(0).getExpr());
        assertTrue(queryCaptor.getAllValues().get(1).getExpr().contains("tenant_id == 11"));
        assertTrue(queryCaptor.getAllValues().get(1).getExpr().contains("kb_id == 31"));
        assertEquals("tenant_id == 11 and kb_id == 31", queryCaptor.getAllValues().get(2).getExpr());
    }
}
