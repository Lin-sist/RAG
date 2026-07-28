package com.enterprise.rag.admin.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "rag.mcp")
public class McpProperties {

    static final int DEFAULT_MAX_REQUEST_BYTES = 131_072;
    static final int MAX_CONFIGURABLE_REQUEST_BYTES = 1_048_576;
    static final int DEFAULT_RESOURCE_PAGE_SIZE = 50;
    static final int MAX_RESOURCE_PAGE_SIZE = 100;

    private boolean enabled;

    private boolean localOnly = true;

    private List<String> allowedOrigins = List.of();

    private int maxRequestBytes = DEFAULT_MAX_REQUEST_BYTES;

    private int resourcePageSize = DEFAULT_RESOURCE_PAGE_SIZE;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLocalOnly() {
        return localOnly;
    }

    public void setLocalOnly(boolean localOnly) {
        this.localOnly = localOnly;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }

    public int getMaxRequestBytes() {
        return maxRequestBytes;
    }

    public void setMaxRequestBytes(int maxRequestBytes) {
        if (maxRequestBytes <= 0 || maxRequestBytes > MAX_CONFIGURABLE_REQUEST_BYTES) {
            throw new IllegalArgumentException(
                    "MCP max request bytes must be between 1 and "
                            + MAX_CONFIGURABLE_REQUEST_BYTES);
        }
        this.maxRequestBytes = maxRequestBytes;
    }

    public int getResourcePageSize() {
        return resourcePageSize;
    }

    public void setResourcePageSize(int resourcePageSize) {
        if (resourcePageSize <= 0 || resourcePageSize > MAX_RESOURCE_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "MCP resource page size must be between 1 and "
                            + MAX_RESOURCE_PAGE_SIZE);
        }
        this.resourcePageSize = resourcePageSize;
    }
}
