package com.enterprise.rag.core.vectorstore;

/**
 * Immutable vector/query scope resolved from a tenant-scoped knowledge base.
 * Client request fields must never be used to construct this value directly.
 */
public record TenantVectorScope(
        long tenantId,
        long knowledgeBaseId,
        String collectionName) {

    public TenantVectorScope {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        if (knowledgeBaseId <= 0) {
            throw new IllegalArgumentException("knowledgeBaseId must be positive");
        }
        if (collectionName == null || collectionName.isBlank()) {
            throw new IllegalArgumentException("collectionName must not be blank");
        }
        collectionName = collectionName.trim();
    }
}
