package com.enterprise.rag.core.vectorstore.config;

/**
 * Stable startup failure for an adapter that has not passed the complete
 * tenant vector contract.
 */
public final class TenantVectorAdapterConfigurationException extends IllegalStateException {

    private final String adapter;
    private final String errorCategory;

    public TenantVectorAdapterConfigurationException(String adapter) {
        super("Configured vector adapter does not support tenant enforcement");
        this.adapter = adapter;
        this.errorCategory = "unsupported_tenant_enforcement";
    }

    public String getAdapter() {
        return adapter;
    }

    public String getErrorCategory() {
        return errorCategory;
    }
}
