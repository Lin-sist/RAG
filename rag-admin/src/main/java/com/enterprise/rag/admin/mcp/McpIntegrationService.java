package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.model.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class McpIntegrationService {
    static final int MAX_BYTES = 131_072;
    private final McpProperties properties;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public McpIntegrationService(McpProperties properties, CurrentUserService currentUserService,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    public record Server(String name, String version) { }
    public record Metadata(boolean enabled, boolean externalToolsConfigured, boolean externalToolsEffective,
                           boolean localOnly, String endpointPath, String transport, String sessionMode,
                           String authMode, boolean oauthSupported, String targetProtocolVersion, Server server,
                           String originPolicy, List<McpCatalog.Tool> implementedTools,
                           List<String> configuredToolNames,
                           List<McpCatalog.ResourceTemplate> implementedResourceTemplates,
                           List<String> configuredResourceTemplateNames) { }

    public Metadata describe(UserDetails principal) {
        currentUserService.requireIdentity(principal);
        var tools = McpCatalog.tools(properties.getMaxQueryChars());
        var resources = McpCatalog.resourceTemplates();
        var enabled = properties.isEnabled();
        return new Metadata(enabled, properties.isExternalToolsEnabled(), enabled && properties.isExternalToolsEnabled(),
                properties.isLocalOnly(), McpCatalog.ENDPOINT, "streamable-http", "stateless",
                "project-access-jwt", false, McpCatalog.TARGET_PROTOCOL,
                new Server(McpCatalog.SERVER_NAME, McpCatalog.SERVER_VERSION), "exact-allowlist", tools,
                enabled ? tools.stream().filter(t -> !t.externalCallPossible() || properties.isExternalToolsEnabled())
                        .map(McpCatalog.Tool::name).toList() : List.of(), resources,
                enabled ? resources.stream().map(McpCatalog.ResourceTemplate::name).toList() : List.of());
    }

    public byte[] response(UserDetails principal) {
        var metadata = describe(principal);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(ApiResponse.success(metadata));
            if (bytes.length > MAX_BYTES) {
                throw new BusinessException("MCP_METADATA_TOO_LARGE", "MCP 接入信息超过大小限制", HttpStatus.SERVICE_UNAVAILABLE);
            }
            return bytes;
        } catch (JsonProcessingException exception) {
            throw new BusinessException("MCP_METADATA_UNAVAILABLE", "MCP 接入信息暂不可用", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
