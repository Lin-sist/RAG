package com.enterprise.rag.core.vectorstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantVectorScopeTest {

    @Test
    void scopeRequiresServerResolvedTenantKnowledgeBaseAndCollection() {
        TenantVectorScope scope = new TenantVectorScope(11L, 101L, "tenant_11_kb_101");

        assertEquals(11L, scope.tenantId());
        assertEquals(101L, scope.knowledgeBaseId());
        assertEquals("tenant_11_kb_101", scope.collectionName());
        assertNotEquals(scope, new TenantVectorScope(12L, 101L, "tenant_11_kb_101"));

        assertThrows(IllegalArgumentException.class,
                () -> new TenantVectorScope(0L, 101L, "tenant_11_kb_101"));
        assertThrows(IllegalArgumentException.class,
                () -> new TenantVectorScope(11L, 0L, "tenant_11_kb_101"));
        assertThrows(IllegalArgumentException.class,
                () -> new TenantVectorScope(11L, 101L, " "));
    }
}
