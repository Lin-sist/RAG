package com.enterprise.rag.auth.service;

import com.enterprise.rag.auth.dto.AuthResponse;
import com.enterprise.rag.auth.dto.LoginRequest;
import com.enterprise.rag.auth.exception.AuthException;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.auth.provider.JwtTokenProvider;
import com.enterprise.rag.auth.service.impl.AuthServiceImpl;
import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.exception.RedisDependencyException;
import com.enterprise.rag.common.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
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
                .tenantId(901L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserPrincipalFromToken(refreshToken)).thenReturn(principal);
        String v2SessionKey = RedisKeyConstants.userSessionV2Key(901L, 7L);
        when(redisUtil.hasKey(v2SessionKey)).thenReturn(false);
        when(redisUtil.hasKey("session:7")).thenReturn(true);

        assertThrows(AuthException.class, () -> authService.refreshToken(refreshToken));
        verify(redisUtil).hasKey(v2SessionKey);
        verify(redisUtil, never()).hGet("session:7", "refreshToken");
    }

    @Test
    void shouldRejectRefreshWhenSessionTokenMismatch() {
        String refreshToken = "refresh-token";
        UserPrincipal principal = UserPrincipal.builder()
                .id(7L)
                .tenantId(901L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserPrincipalFromToken(refreshToken)).thenReturn(principal);
        String sessionKey = RedisKeyConstants.userSessionV2Key(901L, 7L);
        when(redisUtil.hasKey(sessionKey)).thenReturn(true);
        when(redisUtil.hGet(sessionKey, "tenantId")).thenReturn(901L);
        when(redisUtil.hGet(sessionKey, "userId")).thenReturn(7L);
        when(redisUtil.hGet(sessionKey, "refreshToken"))
                .thenReturn("another-refresh-token");

        assertThrows(AuthException.class, () -> authService.refreshToken(refreshToken));
    }

    @Test
    void shouldRejectRefreshWhenSessionTenantPayloadMismatch() {
        String refreshToken = "refresh-token";
        UserPrincipal principal = principal(7L, 901L, "alice");
        String sessionKey = RedisKeyConstants.userSessionV2Key(901L, 7L);

        stubRefreshTokenValidation(refreshToken, principal);
        when(redisUtil.hasKey(sessionKey)).thenReturn(true);
        when(redisUtil.hGet(sessionKey, "tenantId")).thenReturn(902L);
        when(redisUtil.hGet(sessionKey, "userId")).thenReturn(7L);
        when(redisUtil.hGet(sessionKey, "refreshToken")).thenReturn(refreshToken);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.refreshToken(refreshToken));

        assertEquals("AUTH_006", exception.getErrorCode());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void shouldRejectRefreshWhenSessionUserPayloadMismatch() {
        String refreshToken = "refresh-token";
        UserPrincipal principal = principal(7L, 901L, "alice");
        String sessionKey = RedisKeyConstants.userSessionV2Key(901L, 7L);

        stubRefreshTokenValidation(refreshToken, principal);
        when(redisUtil.hasKey(sessionKey)).thenReturn(true);
        when(redisUtil.hGet(sessionKey, "tenantId")).thenReturn(901L);
        when(redisUtil.hGet(sessionKey, "userId")).thenReturn(8L);
        when(redisUtil.hGet(sessionKey, "refreshToken")).thenReturn(refreshToken);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.refreshToken(refreshToken));

        assertEquals("AUTH_006", exception.getErrorCode());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void shouldRejectRefreshWhenReloadedDatabaseUserIsDisabled() {
        String refreshToken = "refresh-token";
        UserPrincipal tokenPrincipal = UserPrincipal.builder()
                .id(7L)
                .tenantId(901L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
        UserPrincipal disabledDatabaseUser = UserPrincipal.builder()
                .id(7L)
                .tenantId(901L)
                .username("alice")
                .enabled(false)
                .roles(Set.of("USER"))
                .build();

        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserPrincipalFromToken(refreshToken)).thenReturn(tokenPrincipal);
        stubValidSession(tokenPrincipal, refreshToken);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(disabledDatabaseUser);

        assertThrows(AuthException.class, () -> authService.refreshToken(refreshToken));
    }

    @Test
    void shouldRejectRefreshWhenDatabaseUserNoLongerExists() {
        String refreshToken = "refresh-token";
        UserPrincipal tokenPrincipal = UserPrincipal.builder()
                .id(7L)
                .tenantId(901L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserPrincipalFromToken(refreshToken)).thenReturn(tokenPrincipal);
        stubValidSession(tokenPrincipal, refreshToken);
        when(userDetailsService.loadUserByUsername("alice"))
                .thenThrow(new UsernameNotFoundException("用户不存在"));

        assertThrows(UsernameNotFoundException.class, () -> authService.refreshToken(refreshToken));
    }

    @Test
    void shouldUseReloadedDatabaseRolesWhenRefreshingTokens() {
        String refreshToken = "refresh-token";
        UserPrincipal tokenPrincipal = UserPrincipal.builder()
                .id(7L)
                .tenantId(901L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
        UserPrincipal freshDatabaseUser = UserPrincipal.builder()
                .id(7L)
                .tenantId(901L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("ADMIN"))
                .build();

        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserPrincipalFromToken(refreshToken)).thenReturn(tokenPrincipal);
        stubValidSession(tokenPrincipal, refreshToken);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(freshDatabaseUser);
        when(jwtTokenProvider.generateAccessToken(freshDatabaseUser)).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(freshDatabaseUser)).thenReturn("new-refresh-token");

        authService.refreshToken(refreshToken);

        verify(jwtTokenProvider).generateAccessToken(same(freshDatabaseUser));
        verify(jwtTokenProvider).generateRefreshToken(same(freshDatabaseUser));
        verify(redisUtil).hSetAll(
                eq(RedisKeyConstants.userSessionV2Key(901L, 7L)),
                argThat(session -> Long.valueOf(901L).equals(session.get("tenantId"))
                        && Long.valueOf(7L).equals(session.get("userId"))
                        && "new-access-token".equals(session.get("accessToken"))
                        && "new-refresh-token".equals(session.get("refreshToken"))));
    }

    @Test
    void shouldRejectRefreshWhenReloadedTenantDiffersFromTokenPrincipal() {
        String refreshToken = "refresh-token";
        UserPrincipal tokenPrincipal = principal(7L, 901L, "alice");
        UserPrincipal differentTenantPrincipal = principal(7L, 902L, "alice");

        stubRefreshTokenValidation(refreshToken, tokenPrincipal);
        stubValidSession(tokenPrincipal, refreshToken);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(differentTenantPrincipal);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.refreshToken(refreshToken));

        assertEquals("AUTH_006", exception.getErrorCode());
        verify(jwtTokenProvider, never()).generateAccessToken(any(UserPrincipal.class));
        verify(tokenBlacklistService, never()).addToBlacklist(refreshToken);
    }

    @Test
    void shouldRejectRefreshWhenReloadedUserIdDiffersFromTokenPrincipal() {
        String refreshToken = "refresh-token";
        UserPrincipal tokenPrincipal = principal(7L, 901L, "alice");
        UserPrincipal differentUserPrincipal = principal(8L, 901L, "alice");

        stubRefreshTokenValidation(refreshToken, tokenPrincipal);
        stubValidSession(tokenPrincipal, refreshToken);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(differentUserPrincipal);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.refreshToken(refreshToken));

        assertEquals("AUTH_006", exception.getErrorCode());
        verify(jwtTokenProvider, never()).generateAccessToken(any(UserPrincipal.class));
        verify(tokenBlacklistService, never()).addToBlacklist(refreshToken);
    }

    @Test
    void shouldBlacklistSessionRefreshTokenOnLogout() {
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        UserPrincipal principal = UserPrincipal.builder()
                .id(9L)
                .tenantId(901L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        when(jwtTokenProvider.getUserPrincipalFromToken(accessToken)).thenReturn(principal);
        stubValidSession(principal, refreshToken);
        doNothing().when(tokenBlacklistService).addToBlacklist(any(String.class));

        authService.logout(accessToken);

        verify(tokenBlacklistService).addToBlacklist(accessToken);
        verify(tokenBlacklistService).addToBlacklist(refreshToken);
        verify(jwtTokenProvider).getUserPrincipalFromToken(accessToken);
        verify(jwtTokenProvider, never()).getUserIdFromToken(accessToken);
        verify(redisUtil).delete(RedisKeyConstants.userSessionV2Key(901L, 9L));
        verify(redisUtil, never()).hGet("session:9", "refreshToken");
    }

    @Test
    void shouldRejectLogoutWhenSessionTenantPayloadMismatch() {
        String accessToken = "access-token";
        UserPrincipal principal = principal(9L, 901L, "alice");
        String sessionKey = RedisKeyConstants.userSessionV2Key(901L, 9L);

        when(jwtTokenProvider.getUserPrincipalFromToken(accessToken)).thenReturn(principal);
        when(redisUtil.hasKey(sessionKey)).thenReturn(true);
        when(redisUtil.hGet(sessionKey, "tenantId")).thenReturn(902L);
        when(redisUtil.hGet(sessionKey, "userId")).thenReturn(9L);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.logout(accessToken));

        assertEquals("AUTH_003", exception.getErrorCode());
        verify(tokenBlacklistService).addToBlacklist(accessToken);
        verify(redisUtil, never()).delete(sessionKey);
    }

    @Test
    void shouldRejectLogoutWhenSessionUserPayloadMismatch() {
        String accessToken = "access-token";
        UserPrincipal principal = principal(9L, 901L, "alice");
        String sessionKey = RedisKeyConstants.userSessionV2Key(901L, 9L);

        when(jwtTokenProvider.getUserPrincipalFromToken(accessToken)).thenReturn(principal);
        when(redisUtil.hasKey(sessionKey)).thenReturn(true);
        when(redisUtil.hGet(sessionKey, "tenantId")).thenReturn(901L);
        when(redisUtil.hGet(sessionKey, "userId")).thenReturn(10L);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.logout(accessToken));

        assertEquals("AUTH_003", exception.getErrorCode());
        verify(tokenBlacklistService).addToBlacklist(accessToken);
        verify(redisUtil, never()).delete(sessionKey);
    }

    @Test
    void shouldUseAccessTokenExpirationAsExpiresIn() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(1L)
                .tenantId(901L)
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
        verify(redisUtil).hSetAll(
                eq(RedisKeyConstants.userSessionV2Key(901L, 1L)),
                argThat(session -> Long.valueOf(901L).equals(session.get("tenantId"))
                        && Long.valueOf(1L).equals(session.get("userId"))
                        && "new-access".equals(session.get("accessToken"))
                        && "new-refresh".equals(session.get("refreshToken"))));
        verify(redisUtil, never()).hSetAll(eq("session:1"), anyMap());
    }

    @Test
    void loginShouldReturnStableUnavailableWhenSessionWriteFails() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(1L)
                .tenantId(901L)
                .username("admin")
                .enabled(true)
                .roles(Set.of("ADMIN"))
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(jwtTokenProvider.generateAccessToken(principal)).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(principal)).thenReturn("new-refresh");
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(redisUtil).hSetAll(anyString(), anyMap());
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> authService.login(request));

        assertEquals("auth_session", exception.getSubsystem());
        assertEquals("write", exception.getOperation());
    }

    @Test
    void refreshShouldReturnStableUnavailableWhenSessionReadFails() {
        String refreshToken = "refresh-token";
        UserPrincipal principal = UserPrincipal.builder()
                .id(7L)
                .tenantId(901L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserPrincipalFromToken(refreshToken)).thenReturn(principal);
        when(redisUtil.hasKey(RedisKeyConstants.userSessionV2Key(901L, 7L)))
                .thenThrow(new RuntimeException("synthetic redis marker"));

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> authService.refreshToken(refreshToken));

        assertEquals("auth_session", exception.getSubsystem());
        assertEquals("read", exception.getOperation());
    }

    @Test
    void refreshShouldNotReturnTokensWhenSessionWriteFails() {
        String refreshToken = "refresh-token";
        UserPrincipal principal = UserPrincipal.builder()
                .id(7L)
                .tenantId(901L)
                .username("alice")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserPrincipalFromToken(refreshToken)).thenReturn(principal);
        stubValidSession(principal, refreshToken);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(principal);
        when(jwtTokenProvider.generateAccessToken(principal)).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(principal)).thenReturn("new-refresh");
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(redisUtil).hSetAll(anyString(), anyMap());

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> authService.refreshToken(refreshToken));

        assertEquals("auth_session", exception.getSubsystem());
        assertEquals("write", exception.getOperation());
    }

    @Test
    void logoutShouldNotReportSuccessWhenRevocationWriteFails() {
        doThrow(RedisDependencyException.unavailable(
                "token_blacklist", "write", new RuntimeException("synthetic redis marker")))
                .when(tokenBlacklistService).addToBlacklist("access-token");

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> authService.logout("access-token"));

        assertEquals("token_blacklist", exception.getSubsystem());
        assertEquals("write", exception.getOperation());
    }

    private void stubRefreshTokenValidation(String refreshToken, UserPrincipal principal) {
        when(jwtTokenProvider.isTokenValid(refreshToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(false);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserPrincipalFromToken(refreshToken)).thenReturn(principal);
    }

    private void stubValidSession(UserPrincipal principal, String refreshToken) {
        String sessionKey = RedisKeyConstants.userSessionV2Key(
                principal.getTenantId(), principal.getId());
        when(redisUtil.hasKey(sessionKey)).thenReturn(true);
        when(redisUtil.hGet(sessionKey, "tenantId")).thenReturn(principal.getTenantId());
        when(redisUtil.hGet(sessionKey, "userId")).thenReturn(principal.getId());
        when(redisUtil.hGet(sessionKey, "refreshToken")).thenReturn(refreshToken);
    }

    private UserPrincipal principal(long userId, long tenantId, String username) {
        return UserPrincipal.builder()
                .id(userId)
                .tenantId(tenantId)
                .username(username)
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
    }
}
