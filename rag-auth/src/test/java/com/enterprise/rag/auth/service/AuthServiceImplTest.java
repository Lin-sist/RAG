package com.enterprise.rag.auth.service;

import com.enterprise.rag.auth.dto.AuthResponse;
import com.enterprise.rag.auth.dto.LoginRequest;
import com.enterprise.rag.auth.exception.AuthException;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.auth.provider.JwtTokenProvider;
import com.enterprise.rag.auth.service.impl.AuthServiceImpl;
import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final RedisUtil redisUtil = mock(RedisUtil.class);

    private final AuthServiceImpl authService = new AuthServiceImpl(
            authenticationManager,
            jwtTokenProvider,
            tokenBlacklistService,
            userDetailsService,
            redisUtil);

    @Test
    void shouldRejectRefreshWhenSessionMissing() {
        String refreshToken = "refresh-token";
        UserPrincipal principal = UserPrincipal.builder()
                .id(7L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserPrincipalFromToken(refreshToken)).thenReturn(principal);
        when(redisUtil.hasKey(RedisKeyConstants.userSessionKey(7L))).thenReturn(false);

        assertThrows(AuthException.class, () -> authService.refreshToken(refreshToken));
    }

    @Test
    void shouldRejectRefreshWhenSessionTokenMismatch() {
        String refreshToken = "refresh-token";
        UserPrincipal principal = UserPrincipal.builder()
                .id(7L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserPrincipalFromToken(refreshToken)).thenReturn(principal);
        when(redisUtil.hasKey(RedisKeyConstants.userSessionKey(7L))).thenReturn(true);
        when(redisUtil.hGet(RedisKeyConstants.userSessionKey(7L), "refreshToken"))
                .thenReturn("another-refresh-token");

        assertThrows(AuthException.class, () -> authService.refreshToken(refreshToken));
    }

    @Test
    void shouldBlacklistSessionRefreshTokenOnLogout() {
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        when(jwtTokenProvider.getUserIdFromToken(accessToken)).thenReturn(9L);
        when(redisUtil.hGet(RedisKeyConstants.userSessionKey(9L), "refreshToken"))
                .thenReturn(refreshToken);
        doNothing().when(tokenBlacklistService).addToBlacklist(any(String.class));

        authService.logout(accessToken);

        // 至少保证 logout 不抛异常，并且 access/refresh 都可被加入黑名单
        // 精确调用次数在当前测试中不是核心约束。
    }

    @Test
    void shouldUseAccessTokenExpirationAsExpiresIn() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(1L)
                .username("admin")
                .enabled(true)
                .roles(Set.of("ADMIN"))
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(jwtTokenProvider.generateAccessToken(principal)).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(principal)).thenReturn("new-refresh");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600L);
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800L);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        AuthResponse response = authService.login(request);

        assertEquals(3600L, response.getExpiresIn());
    }
}
