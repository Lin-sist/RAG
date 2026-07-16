package com.enterprise.rag.core.vectorstore.milvus;

import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.config.VectorStoreProperties;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.R;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
}
