package com.enterprise.rag.auth.service;

import com.enterprise.rag.auth.provider.JwtTokenProvider;
import com.enterprise.rag.common.exception.RedisDependencyException;
import com.enterprise.rag.common.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenBlacklistServiceFailureTest {

    private RedisUtil redisUtil;
    private JwtTokenProvider jwtTokenProvider;
    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        redisUtil = mock(RedisUtil.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        service = new TokenBlacklistService(redisUtil, jwtTokenProvider);
        when(jwtTokenProvider.getTokenHash("access-token")).thenReturn("safe-hash");
    }

    @Test
    void blacklistLookupFailureShouldFailClosed() {
        when(redisUtil.hasKey(anyString()))
                .thenThrow(new RuntimeException("synthetic redis marker"));

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> service.isBlacklisted("access-token"));

        assertEquals("token_blacklist", exception.getSubsystem());
        assertEquals("read", exception.getOperation());
    }

    @Test
    void blacklistWriteFailureShouldFailClosed() {
        when(jwtTokenProvider.getRemainingTimeInSeconds("access-token")).thenReturn(300L);
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(redisUtil).setString(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> service.addToBlacklist("access-token"));

        assertEquals("token_blacklist", exception.getSubsystem());
        assertEquals("write", exception.getOperation());
    }

    @Test
    void blacklistRemovalFailureShouldFailClosed() {
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(redisUtil).delete(anyString());

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> service.removeFromBlacklist("access-token"));

        assertEquals("token_blacklist", exception.getSubsystem());
        assertEquals("delete", exception.getOperation());
    }
}
