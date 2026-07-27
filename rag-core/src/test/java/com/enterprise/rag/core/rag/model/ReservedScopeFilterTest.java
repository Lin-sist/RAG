package com.enterprise.rag.core.rag.model;

import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.core.vectorstore.SearchOptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservedScopeFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "tenantId", "TENANT_ID", "tenant-id",
            "kbId", "KB_ID", "knowledge_base_id",
            "collectionName", "COLLECTION_NAME", "vector-collection"
    })
    void qaRequestRejectsClientControlledTenantKnowledgeBaseAndCollectionAliases(String key) {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> new QARequest(
                        "question", "server-mapped-collection", 5, 0.3f,
                        Map.of(key, "forged"), true, false));

        assertEquals("RAG_SCOPE_FILTER_RESERVED", exception.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"tenant_id", "Knowledge-Base-ID", "VECTOR_COLLECTION_NAME"})
    void retrieveOptionsRejectsReservedScopeAliases(String key) {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> new RetrieveOptions(
                        "server-mapped-collection", 5, 0.3f,
                        Map.of(key, "forged"), true));

        assertEquals("RAG_SCOPE_FILTER_RESERVED", exception.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TENANT-ID", "kb_id", "collectionName"})
    void searchOptionsRejectsReservedScopeAliases(String key) {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> new SearchOptions(5, 0.3f, Map.of(key, "forged")));

        assertEquals("RAG_SCOPE_FILTER_RESERVED", exception.getErrorCode());
    }
}
