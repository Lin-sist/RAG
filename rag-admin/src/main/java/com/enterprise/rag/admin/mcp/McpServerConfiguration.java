package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.common.ratelimit.RateLimiter;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.service.RAGService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(McpProperties.class)
@ConditionalOnProperty(prefix = "rag.mcp", name = "enabled", havingValue = "true")
public class McpServerConfiguration {

    static final String ENDPOINT = "/mcp";
    static final String SERVER_NAME = "enterprise-rag-readonly";
    static final String SERVER_VERSION = "c15-v1";

    @Bean
    McpJsonMapper mcpJsonMapper(ObjectMapper objectMapper) {
        return new JacksonMcpJsonMapper(objectMapper.copy());
    }

    @Bean
    McpRequestIdentityResolver mcpRequestIdentityResolver(CurrentUserService currentUserService) {
        return new McpRequestIdentityResolver(currentUserService);
    }

    @Bean
    McpKnowledgeResourceService mcpKnowledgeResourceService(
            KnowledgeBaseService knowledgeBaseService,
            DocumentService documentService,
            AuthorizationService authorizationService,
            ObjectMapper objectMapper,
            McpProperties properties) {
        return new McpKnowledgeResourceService(
                knowledgeBaseService,
                documentService,
                authorizationService,
                objectMapper,
                properties.getResourcePageSize(),
                properties.getMaxChunkBytes());
    }

    @Bean
    McpCitationReader mcpCitationReader(
            AuthorizationService authorizationService,
            DocumentService documentService,
            McpProperties properties) {
        return new McpCitationReader(
                authorizationService, documentService, properties.getMaxChunkBytes());
    }

    @Bean
    McpResultMapper mcpResultMapper(
            ObjectMapper objectMapper, McpProperties properties) {
        return new McpResultMapper(objectMapper, properties.getMaxResultBytes());
    }

    @Bean(destroyMethod = "close")
    McpToolExecutionGuard mcpToolExecutionGuard(
            RateLimiter rateLimiter, McpProperties properties) {
        return new McpToolExecutionGuard(
                rateLimiter,
                properties.getExpensiveConcurrencyPerUser(),
                properties.getReadTimeout(),
                properties.getSearchTimeout(),
                properties.getAskTimeout());
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "rag.mcp",
            name = "external-tools-enabled",
            havingValue = "true")
    McpExternalReadService mcpExternalReadService(
            AuthorizationService authorizationService,
            KnowledgeBaseService knowledgeBaseService,
            QueryEngine queryEngine,
            RAGService ragService,
            McpCitationReader citationReader,
            McpProperties properties) {
        return new McpExternalReadService(
                authorizationService,
                knowledgeBaseService,
                queryEngine,
                ragService,
                citationReader,
                properties.isCacheEnabled());
    }

    @Bean
    McpReadOnlyToolService mcpReadOnlyToolService(
            McpCitationReader citationReader,
            ObjectProvider<McpExternalReadService> externalReadService,
            McpResultMapper resultMapper,
            McpProperties properties,
            McpToolExecutionGuard executionGuard) {
        return new McpReadOnlyToolService(
                citationReader,
                externalReadService.getIfAvailable(),
                resultMapper,
                properties,
                executionGuard);
    }

    @Bean
    McpToolRequestValidator mcpToolRequestValidator(McpProperties properties) {
        return new McpToolRequestValidator(properties);
    }

    @Bean
    HttpServletStatelessServerTransport mcpServletTransport(
            McpJsonMapper mcpJsonMapper,
            McpRequestIdentityResolver identityResolver) {
        return HttpServletStatelessServerTransport.builder()
                .jsonMapper(mcpJsonMapper)
                .messageEndpoint(ENDPOINT)
                .contextExtractor(request -> McpTransportContext.create(Map.of(
                        McpRequestIdentityResolver.CONTEXT_KEY,
                        identityResolver.resolve(request))))
                .build();
    }

    @Bean
    McpResourceListTransport mcpResourceListTransport(
            HttpServletStatelessServerTransport transport,
            McpKnowledgeResourceService resourceService,
            McpRequestIdentityResolver identityResolver,
            McpJsonMapper mcpJsonMapper,
            McpToolRequestValidator toolRequestValidator) {
        return new McpResourceListTransport(
                transport,
                resourceService,
                identityResolver,
                mcpJsonMapper,
                toolRequestValidator);
    }

    @Bean(destroyMethod = "closeGracefully")
    McpStatelessSyncServer mcpStatelessServer(
            McpResourceListTransport transport,
            McpJsonMapper mcpJsonMapper,
            McpKnowledgeResourceService resourceService,
            McpRequestIdentityResolver identityResolver,
            McpReadOnlyToolService toolService,
            McpProperties properties) {
        McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
                .resources(false, false)
                .tools(false)
                .build();

        return McpServer.sync(transport)
                .serverInfo(SERVER_NAME, SERVER_VERSION)
                .capabilities(capabilities)
                .jsonMapper(mcpJsonMapper)
                .strictToolNameValidation(true)
                .validateToolInputs(true)
                .tools(McpToolSpecifications.specifications(
                        toolService, identityResolver, properties))
                .resourceTemplates(McpResourceTemplates.specifications(
                        resourceService, identityResolver))
                .build();
    }

    @Bean
    ServletRegistrationBean<HttpServletStatelessServerTransport> mcpServletRegistration(
            HttpServletStatelessServerTransport transport,
            McpStatelessSyncServer server) {
        ServletRegistrationBean<HttpServletStatelessServerTransport> registration =
                new ServletRegistrationBean<>(transport, ENDPOINT);
        registration.setName("enterpriseRagMcpServlet");
        registration.setAsyncSupported(true);
        registration.setLoadOnStartup(1);
        return registration;
    }

    @Bean
    FilterRegistrationBean<McpOriginAndExposureFilter> mcpOriginAndExposureFilterRegistration(
            McpProperties properties) {
        FilterRegistrationBean<McpOriginAndExposureFilter> registration =
                new FilterRegistrationBean<>(new McpOriginAndExposureFilter(properties));
        registration.setName("enterpriseRagMcpOriginAndExposureFilter");
        registration.addUrlPatterns(ENDPOINT);
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
