package com.enterprise.rag.core.vectorstore.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VectorStoreTenantEnforcementGuardTest {

    private final VectorStoreConfig config = new VectorStoreConfig();

    @Test
    void qdrantAndElasticsearchFailBeforeClientCreationWhenTenantEnforcementIsEnabled() {
        VectorStoreProperties properties = new VectorStoreProperties();
        properties.setTenantEnforcementEnabled(true);

        TenantVectorAdapterConfigurationException qdrant = assertThrows(
                TenantVectorAdapterConfigurationException.class,
                () -> config.qdrantClient(properties));
        assertEquals("unsupported_tenant_enforcement", qdrant.getErrorCategory());
        assertEquals("qdrant", qdrant.getAdapter());

        TenantVectorAdapterConfigurationException elasticsearch = assertThrows(
                TenantVectorAdapterConfigurationException.class,
                () -> config.elasticsearchClient(properties));
        assertEquals("unsupported_tenant_enforcement", elasticsearch.getErrorCategory());
        assertEquals("elasticsearch", elasticsearch.getAdapter());
    }

    @Test
    void milvusPassesTheStartupCapabilityGuard() {
        config.requireSupportedTenantAdapter("milvus", true);
    }
}
