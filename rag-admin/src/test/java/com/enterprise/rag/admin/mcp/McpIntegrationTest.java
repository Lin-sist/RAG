package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.controller.McpIntegrationController;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class McpIntegrationTest {
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
    private final UserPrincipal principal = UserPrincipal.builder().id(1L).tenantId(1L)
            .username("synthetic").enabled(true).roles(Set.of("USER")).build();

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void allSwitchCombinationsKeepDisabledCatalogSeparateFromRegisteredCapabilities() {
        for (boolean enabled : new boolean[]{false, true}) {
            for (boolean external : new boolean[]{false, true}) {
                var properties = new McpProperties();
                properties.setEnabled(enabled);
                properties.setExternalToolsEnabled(external);
                var metadata = new McpIntegrationService(properties, new CurrentUserService(), mapper).describe(principal);
                assertEquals(4, metadata.implementedTools().size());
                assertEquals(3, metadata.implementedResourceTemplates().size());
                assertEquals(enabled && external, metadata.externalToolsEffective());
                assertEquals(enabled ? (external ? 4 : 2) : 0, metadata.configuredToolNames().size());
                assertEquals(enabled ? 3 : 0, metadata.configuredResourceTemplateNames().size());
                var registry = McpToolSpecifications.specifications(null, null, properties);
                if (enabled) assertEquals(metadata.configuredToolNames(), registry.stream().map(s -> s.tool().name()).toList());
                for (var specification : registry) {
                    var definition = metadata.implementedTools().stream()
                            .filter(t -> t.name().equals(specification.tool().name())).findFirst().orElseThrow();
                    assertEquals(definition.inputSchema(), specification.tool().inputSchema());
                    assertEquals(definition.outputSchema(), specification.tool().outputSchema());
                }
                assertEquals(metadata.implementedResourceTemplates().stream().map(McpCatalog.ResourceTemplate::uriTemplate).toList(),
                        McpResourceTemplates.specifications(null, null).stream().map(s -> s.resourceTemplate().uriTemplate()).toList());
            }
        }
    }

    @Test void disabledPropertyBindingDoesNotCreateTransportOrHandlers() {
        new ApplicationContextRunner().withUserConfiguration(McpIntegrationConfiguration.class, McpServerConfiguration.class)
                .withPropertyValues("rag.mcp.external-tools-enabled=true").run(context -> {
                    assertFalse(context.getBean(McpProperties.class).isEnabled());
                    assertTrue(context.getBean(McpProperties.class).isExternalToolsEnabled());
                    assertFalse(context.containsBean("mcpServletRegistration"));
                    assertFalse(context.containsBean("mcpReadOnlyToolService"));
                });
    }

    @Test void controllerUsesPrincipalAndReturnsOnlyBoundedWhitelistWithoutSecrets() throws Exception {
        var properties = new McpProperties();
        properties.setAllowedOrigins(java.util.List.of("https://private-origin.invalid"));
        var service = new McpIntegrationService(properties, new CurrentUserService(), mapper);
        var mvc = MockMvcBuilders.standaloneSetup(new McpIntegrationController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/integrations/mcp")).andExpect(status().isUnauthorized());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        byte[] body = mvc.perform(get("/api/integrations/mcp").param("tenantId", "999").param("endpoint", "https://unknown.invalid"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.enabled").value(false)).andExpect(jsonPath("$.data.endpointPath").value("/mcp"))
                .andReturn().getResponse().getContentAsByteArray();
        assertTrue(body.length <= McpIntegrationService.MAX_BYTES);
        String json = new String(body, java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(json.contains("private-origin"));
        assertFalse(json.contains("tenantId"));
        assertFalse(json.contains("userId"));
        assertFalse(json.contains("accessToken"));
        assertFalse(json.contains("unknown.invalid"));
        var fields = mapper.readTree(body).path("data").properties().stream().map(java.util.Map.Entry::getKey).collect(Collectors.toSet());
        assertEquals(Set.of("enabled", "externalToolsConfigured", "externalToolsEffective", "localOnly", "endpointPath",
                "transport", "sessionMode", "authMode", "oauthSupported", "targetProtocolVersion", "server", "originPolicy",
                "implementedTools", "configuredToolNames", "implementedResourceTemplates", "configuredResourceTemplateNames"), fields);
    }

    @Test void invalidPrincipalAndOversizedSerializationFailClosed() throws Exception {
        var service = new McpIntegrationService(new McpProperties(), new CurrentUserService(), mapper);
        assertThrows(BusinessException.class, () -> service.describe(null));
        assertThrows(BusinessException.class, () -> service.describe(User.withUsername("plain").password("unused").roles("USER").build()));
        var oversized = mock(ObjectMapper.class);
        when(oversized.writeValueAsBytes(any())).thenReturn(new byte[McpIntegrationService.MAX_BYTES + 1]);
        var exception = assertThrows(BusinessException.class,
                () -> new McpIntegrationService(new McpProperties(), new CurrentUserService(), oversized).response(principal));
        assertEquals("MCP_METADATA_TOO_LARGE", exception.getErrorCode());
    }
}
