package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.common.exception.BusinessException;
import io.modelcontextprotocol.common.McpTransportContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpRequestIdentityResolverTest {

    private final McpRequestIdentityResolver resolver =
            new McpRequestIdentityResolver(new CurrentUserService());

    @Test
    void identityComesOnlyFromTheAuthenticatedServerPrincipal() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(41L)
                .tenantId(7L)
                .username("synthetic-user")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("tenantId", "999");
        request.addHeader("X-Tenant-Id", "999");
        request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));

        RequestIdentity identity = resolver.resolve(request);

        assertEquals(41L, identity.userId());
        assertEquals(7L, identity.tenantId());
    }

    @Test
    void missingTenantPrincipalFailsClosed() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(41L)
                .tenantId(null)
                .username("invalid-user")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));

        BusinessException error = assertThrows(BusinessException.class,
                () -> resolver.resolve(request));

        assertEquals("AUTH_001", error.getErrorCode());
    }

    @Test
    void handlersCanOnlyReadTheServerResolvedIdentityFromTransportContext() {
        RequestIdentity expected = new RequestIdentity(41L, 7L);
        McpTransportContext context = McpTransportContext.create(
                Map.of(McpRequestIdentityResolver.CONTEXT_KEY, expected));

        assertEquals(expected, resolver.requireContextIdentity(context));

        McpTransportContext spoofed = McpTransportContext.create(
                Map.of("tenantId", 999L, "userId", 999L));
        BusinessException error = assertThrows(BusinessException.class,
                () -> resolver.requireContextIdentity(spoofed));
        assertEquals("AUTH_001", error.getErrorCode());
    }
}
