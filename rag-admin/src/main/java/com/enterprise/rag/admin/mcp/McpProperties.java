package com.enterprise.rag.admin.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "rag.mcp")
public class McpProperties {

    static final int DEFAULT_MAX_REQUEST_BYTES = 131_072;
    static final int MAX_CONFIGURABLE_REQUEST_BYTES = 1_048_576;
    static final int DEFAULT_RESOURCE_PAGE_SIZE = 50;
    static final int MAX_RESOURCE_PAGE_SIZE = 100;
    static final int DEFAULT_MAX_CHUNK_BYTES = 65_536;
    static final int MAX_CONFIGURABLE_CHUNK_BYTES = 1_048_576;
    static final int DEFAULT_MAX_RESULT_BYTES = 131_072;
    static final int MAX_CONFIGURABLE_RESULT_BYTES = 1_048_576;
    static final int DEFAULT_MAX_QUERY_CHARS = 2_000;
    static final int MAX_CONFIGURABLE_QUERY_CHARS = 10_000;

    private boolean enabled;

    private boolean externalToolsEnabled;

    private boolean cacheEnabled;

    private boolean localOnly = true;

    private List<String> allowedOrigins = List.of();

    private int maxRequestBytes = DEFAULT_MAX_REQUEST_BYTES;

    private int resourcePageSize = DEFAULT_RESOURCE_PAGE_SIZE;

    private int maxChunkBytes = DEFAULT_MAX_CHUNK_BYTES;

    private int maxResultBytes = DEFAULT_MAX_RESULT_BYTES;

    private int maxQueryChars = DEFAULT_MAX_QUERY_CHARS;

    private Duration readTimeout = Duration.ofSeconds(5);

    private Duration searchTimeout = Duration.ofSeconds(30);

    private Duration askTimeout = Duration.ofSeconds(120);

    private int expensiveConcurrencyPerUser = 2;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isExternalToolsEnabled() {
        return externalToolsEnabled;
    }

    public void setExternalToolsEnabled(boolean externalToolsEnabled) {
        this.externalToolsEnabled = externalToolsEnabled;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
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

    public int getMaxChunkBytes() {
        return maxChunkBytes;
    }

    public void setMaxChunkBytes(int maxChunkBytes) {
        if (maxChunkBytes <= 0 || maxChunkBytes > MAX_CONFIGURABLE_CHUNK_BYTES) {
            throw new IllegalArgumentException(
                    "MCP max chunk bytes must be between 1 and "
                            + MAX_CONFIGURABLE_CHUNK_BYTES);
        }
        this.maxChunkBytes = maxChunkBytes;
    }

    public int getMaxResultBytes() {
        return maxResultBytes;
    }

    public void setMaxResultBytes(int maxResultBytes) {
        if (maxResultBytes <= 0 || maxResultBytes > MAX_CONFIGURABLE_RESULT_BYTES) {
            throw new IllegalArgumentException(
                    "MCP max result bytes must be between 1 and "
                            + MAX_CONFIGURABLE_RESULT_BYTES);
        }
        this.maxResultBytes = maxResultBytes;
    }

    public int getMaxQueryChars() {
        return maxQueryChars;
    }

    public void setMaxQueryChars(int maxQueryChars) {
        if (maxQueryChars <= 0 || maxQueryChars > MAX_CONFIGURABLE_QUERY_CHARS) {
            throw new IllegalArgumentException(
                    "MCP max query chars must be between 1 and "
                            + MAX_CONFIGURABLE_QUERY_CHARS);
        }
        this.maxQueryChars = maxQueryChars;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = requireDuration(readTimeout, Duration.ofSeconds(30), "read timeout");
    }

    public Duration getSearchTimeout() {
        return searchTimeout;
    }

    public void setSearchTimeout(Duration searchTimeout) {
        this.searchTimeout = requireDuration(searchTimeout, Duration.ofMinutes(2), "search timeout");
    }

    public Duration getAskTimeout() {
        return askTimeout;
    }

    public void setAskTimeout(Duration askTimeout) {
        this.askTimeout = requireDuration(askTimeout, Duration.ofMinutes(10), "ask timeout");
    }

    public int getExpensiveConcurrencyPerUser() {
        return expensiveConcurrencyPerUser;
    }

    public void setExpensiveConcurrencyPerUser(int expensiveConcurrencyPerUser) {
        if (expensiveConcurrencyPerUser <= 0 || expensiveConcurrencyPerUser > 16) {
            throw new IllegalArgumentException(
                    "MCP expensive concurrency per user must be between 1 and 16");
        }
        this.expensiveConcurrencyPerUser = expensiveConcurrencyPerUser;
    }

    private Duration requireDuration(Duration value, Duration maximum, String name) {
        if (value == null || value.isZero() || value.isNegative()
                || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Invalid MCP " + name);
        }
        return value;
    }
}
