package com.enterprise.rag.admin.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "rag.mcp")
public class McpProperties {

    static final int DEFAULT_MAX_REQUEST_BYTES = 131_072;
    static final int MAX_CONFIGURABLE_REQUEST_BYTES = 1_048_576;

    private boolean enabled;

    private boolean localOnly = true;

    private List<String> allowedOrigins = List.of();

    private int maxRequestBytes = DEFAULT_MAX_REQUEST_BYTES;

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
}
