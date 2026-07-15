package com.enterprise.rag.auth.filter;

import com.enterprise.rag.auth.provider.JwtTokenProvider;
import com.enterprise.rag.auth.service.TokenBlacklistService;
import com.enterprise.rag.common.exception.RedisDependencyException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterFailureTest {

    @Test
    void blacklistDependencyFailureShouldReturn503WithoutContinuingChain() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, blacklistService);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(tokenProvider.isTokenValid("access-token")).thenReturn(true);
        when(blacklistService.isBlacklisted("access-token"))
                .thenThrow(RedisDependencyException.unavailable(
                        "token_blacklist", "read", new RuntimeException("synthetic redis marker")));

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(chain, never()).doFilter(request, response);
        assertTrue(body.toString().contains("REDIS_DEPENDENCY_UNAVAILABLE"));
        assertEquals(null, org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication());
    }
}
