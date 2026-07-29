package com.enterprise.rag.admin.mcp;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the official stateless transport and lifecycle while replacing the SDK's
 * process-global static resources/list registry and enforcing the repository's strict
 * resources/read parameter shape before delegating valid reads back to the SDK.
 */
final class McpResourceListTransport implements McpStatelessServerTransport {

    private static final String RESOURCES_LIST_METHOD = "resources/list";
    private static final String RESOURCES_READ_METHOD = "resources/read";
    private static final Set<String> PAGINATED_PARAMETER_NAMES = Set.of("cursor", "_meta");
    private static final Set<String> READ_PARAMETER_NAMES = Set.of("uri", "_meta");

    private final McpStatelessServerTransport delegate;
    private final McpKnowledgeResourceService resourceService;
    private final McpRequestIdentityResolver identityResolver;
    private final McpJsonMapper jsonMapper;

    McpResourceListTransport(
            McpStatelessServerTransport delegate,
            McpKnowledgeResourceService resourceService,
            McpRequestIdentityResolver identityResolver,
            McpJsonMapper jsonMapper) {
        this.delegate = delegate;
        this.resourceService = resourceService;
        this.identityResolver = identityResolver;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void setMcpHandler(McpStatelessServerHandler handler) {
        delegate.setMcpHandler(new RequestScopedResourceListHandler(handler));
    }

    @Override
    public Mono<Void> closeGracefully() {
        return delegate.closeGracefully();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public List<String> protocolVersions() {
        return delegate.protocolVersions();
    }

    private final class RequestScopedResourceListHandler implements McpStatelessServerHandler {

        private final McpStatelessServerHandler delegateHandler;

        private RequestScopedResourceListHandler(McpStatelessServerHandler delegateHandler) {
            this.delegateHandler = delegateHandler;
        }

        @Override
        public Mono<McpSchema.JSONRPCResponse> handleRequest(
                io.modelcontextprotocol.common.McpTransportContext context,
                McpSchema.JSONRPCRequest request) {
            if (!RESOURCES_LIST_METHOD.equals(request.method())) {
                if (RESOURCES_READ_METHOD.equals(request.method())
                        && !validReadParameters(request.params())) {
                    return Mono.just(invalidArgument(request.id()));
                }
                return delegateHandler.handleRequest(context, request);
            }
            return Mono.fromSupplier(() -> handleResourcesList(context, request))
                    .onErrorReturn(McpSchema.JSONRPCResponse.error(
                            request.id(),
                            new McpSchema.JSONRPCResponse.JSONRPCError(
                                    -32603, "MCP_INTERNAL_ERROR")));
        }

        @Override
        public Mono<Void> handleNotification(
                io.modelcontextprotocol.common.McpTransportContext context,
                McpSchema.JSONRPCNotification notification) {
            return delegateHandler.handleNotification(context, notification);
        }

        private McpSchema.JSONRPCResponse handleResourcesList(
                io.modelcontextprotocol.common.McpTransportContext context,
                McpSchema.JSONRPCRequest request) {
            McpSchema.PaginatedRequest parameters;
            try {
                parameters = paginatedRequest(request.params());
            } catch (RuntimeException exception) {
                return invalidArgument(request.id());
            }
            try {
                var identity = identityResolver.requireContextIdentity(context);
                var result = resourceService.list(identity, parameters.cursor());
                return McpSchema.JSONRPCResponse.result(request.id(), result);
            } catch (McpKnowledgeResourceService.InvalidResourceCursorException exception) {
                return invalidArgument(request.id());
            }
        }

        private McpSchema.PaginatedRequest paginatedRequest(Object rawParameters) {
            if (rawParameters == null) {
                return new McpSchema.PaginatedRequest();
            }
            if (!(rawParameters instanceof Map<?, ?> parameterMap)
                    || !PAGINATED_PARAMETER_NAMES.containsAll(parameterMap.keySet())) {
                throw new IllegalArgumentException("Invalid MCP resources/list parameters");
            }
            return jsonMapper.convertValue(rawParameters, McpSchema.PaginatedRequest.class);
        }

        private boolean validReadParameters(Object rawParameters) {
            if (!(rawParameters instanceof Map<?, ?> parameterMap)
                    || !READ_PARAMETER_NAMES.containsAll(parameterMap.keySet())) {
                return false;
            }
            return parameterMap.get("uri") instanceof String;
        }

        private McpSchema.JSONRPCResponse invalidArgument(Object requestId) {
            return McpSchema.JSONRPCResponse.error(
                    requestId,
                    new McpSchema.JSONRPCResponse.JSONRPCError(
                            -32602, "MCP_INVALID_ARGUMENT"));
        }
    }
}
