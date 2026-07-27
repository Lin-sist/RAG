package com.enterprise.rag.auth.idempotency;

import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.common.idempotency.IdempotencyScope;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPrincipalIdempotencyScopeResolverTest {

    private final UserPrincipalIdempotencyScopeResolver resolver =
            new UserPrincipalIdempotencyScopeResolver();

    @Test
    void resolvesAuthenticatedUserPrincipalWithoutReflectionOrThreadLocal() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(21L)
                .tenantId(11L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(authentication);

        Optional<IdempotencyScope> resolved = resolver.resolve(request);

        assertEquals(Optional.of(new IdempotencyScope(11L, 21L)), resolved);
    }

    @Test
    void rejectsUnauthenticatedOrUnsupportedPrincipal() {
        Authentication unauthenticated = mock(Authentication.class);
        when(unauthenticated.isAuthenticated()).thenReturn(false);
        MockHttpServletRequest unauthenticatedRequest = new MockHttpServletRequest();
        unauthenticatedRequest.setUserPrincipal(unauthenticated);

        Authentication unsupported = mock(Authentication.class);
        when(unsupported.isAuthenticated()).thenReturn(true);
        when(unsupported.getPrincipal()).thenReturn("alice");
        MockHttpServletRequest unsupportedRequest = new MockHttpServletRequest();
        unsupportedRequest.setUserPrincipal(unsupported);

        assertTrue(resolver.resolve(unauthenticatedRequest).isEmpty());
        assertTrue(resolver.resolve(unsupportedRequest).isEmpty());
        assertTrue(resolver.resolve(null).isEmpty());
    }

    @Test
    void rejectsPrincipalWithoutPositiveTenantAndUserIdentity() {
        UserPrincipal invalidPrincipal = UserPrincipal.builder()
                .id(0L)
                .tenantId(11L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(invalidPrincipal);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(authentication);

        assertTrue(resolver.resolve(request).isEmpty());
    }
}
