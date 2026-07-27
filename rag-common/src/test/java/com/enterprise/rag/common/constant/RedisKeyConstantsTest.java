package com.enterprise.rag.common.constant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisKeyConstantsTest {

    @Test
    void buildsTenantScopedV2BusinessKeys() {
        assertEquals("session:v2:11:21", RedisKeyConstants.userSessionV2Key(11L, 21L));
        assertEquals(
                "idempotency:v2:11:21:%2Fapi%2Fkb%3Acreate:key%3A1",
                RedisKeyConstants.idempotencyV2Key(11L, 21L, "/api/kb:create", "key:1"));
        assertEquals(
                "embedding:v2:11:openai/text-embedding-3-small:content%3Ahash",
                RedisKeyConstants.embeddingCacheV2Key(
                        11L, "openai", "text-embedding-3-small", "content:hash"));
        assertEquals(
                "qa:cache:v2:11:31:query%3Ahash",
                RedisKeyConstants.qaCacheV2Key(11L, 31L, "query:hash"));
        assertEquals(
                "task:status:v2:11:task%3A1",
                RedisKeyConstants.taskStatusV2Key(11L, "task:1"));
        assertEquals(
                "lock:v2:11:document%3A31",
                RedisKeyConstants.tenantBusinessLockV2Key(11L, "document:31"));
    }

    @Test
    void encodesFreeFormComponentsWithoutDelimiterCollisions() {
        assertNotEquals(
                RedisKeyConstants.idempotencyV2Key(1L, 2L, "a:b", "c"),
                RedisKeyConstants.idempotencyV2Key(1L, 2L, "a", "b:c"));
        assertNotEquals(
                RedisKeyConstants.embeddingCacheV2Key(1L, "provider/model", "v1", "hash"),
                RedisKeyConstants.embeddingCacheV2Key(1L, "provider", "model/v1", "hash"));
        assertNotEquals(
                RedisKeyConstants.taskStatusV2Key(1L, "task:value"),
                RedisKeyConstants.taskStatusV2Key(1L, "task%3Avalue"));
    }

    @Test
    void rejectsNonPositiveIdsAndEmptyComponents() {
        assertThrows(IllegalArgumentException.class,
                () -> RedisKeyConstants.userSessionV2Key(0L, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> RedisKeyConstants.userSessionV2Key(1L, -1L));
        assertThrows(IllegalArgumentException.class,
                () -> RedisKeyConstants.idempotencyV2Key(1L, 2L, " ", "key"));
        assertThrows(IllegalArgumentException.class,
                () -> RedisKeyConstants.idempotencyV2Key(1L, 2L, "endpoint", null));
        assertThrows(IllegalArgumentException.class,
                () -> RedisKeyConstants.embeddingCacheV2Key(1L, "", "model", "hash"));
        assertThrows(IllegalArgumentException.class,
                () -> RedisKeyConstants.embeddingCacheV2Key(1L, "provider", "\t", "hash"));
        assertThrows(IllegalArgumentException.class,
                () -> RedisKeyConstants.qaCacheV2Key(1L, 0L, "hash"));
        assertThrows(IllegalArgumentException.class,
                () -> RedisKeyConstants.qaCacheV2Key(1L, 2L, null));
        assertThrows(IllegalArgumentException.class,
                () -> RedisKeyConstants.taskStatusV2Key(-1L, "task"));
        assertThrows(IllegalArgumentException.class,
                () -> RedisKeyConstants.taskStatusV2Key(1L, ""));
        assertThrows(IllegalArgumentException.class,
                () -> RedisKeyConstants.tenantBusinessLockV2Key(1L, "\n"));
    }

    @Test
    void keepsExplicitGlobalSecurityKeysUnversioned() {
        assertEquals("token:blacklist:token-hash", RedisKeyConstants.tokenBlacklistKey("token-hash"));
        assertEquals("ratelimit:ip:127.0.0.1", RedisKeyConstants.rateLimitKey("ip", "127.0.0.1"));
    }

    @Test
    void marksLegacyTenantBusinessHelpersDeprecated() throws Exception {
        assertDeprecated("userSessionKey", Long.class);
        assertDeprecated("idempotencyKey", String.class);
        assertDeprecated("embeddingCacheKey", String.class);
        assertDeprecated("qaCacheKey", String.class, Long.class);
        assertDeprecated("taskStatusKey", String.class);
        assertDeprecated("lockKey", String.class);
    }

    private void assertDeprecated(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = RedisKeyConstants.class.getMethod(methodName, parameterTypes);
        assertTrue(method.isAnnotationPresent(Deprecated.class), methodName + " should be deprecated");
    }
}
