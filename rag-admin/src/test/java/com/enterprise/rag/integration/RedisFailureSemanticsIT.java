package com.enterprise.rag.integration;

import com.enterprise.rag.admin.controller.AuthController;
import com.enterprise.rag.auth.dto.AuthResponse;
import com.enterprise.rag.auth.dto.LoginRequest;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.auth.service.AuthService;
import com.enterprise.rag.common.async.RedisAsyncTaskManager;
import com.enterprise.rag.common.async.DurableTaskStatusStore;
import com.enterprise.rag.common.async.TaskStatus;
import com.enterprise.rag.common.async.TaskHandle;
import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.exception.RedisDependencyException;
import com.enterprise.rag.common.exception.GlobalExceptionHandler;
import com.enterprise.rag.common.ratelimit.RateLimitInterceptor;
import com.enterprise.rag.common.ratelimit.RateLimitWebConfig;
import com.enterprise.rag.common.ratelimit.SlidingWindowRateLimiter;
import com.enterprise.rag.common.util.RedisUtil;
import com.enterprise.rag.core.embedding.EmbeddingProvider;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.embedding.EmbeddingServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("c4c-redis-fault")
@SpringBootTest(
        classes = RedisFailureSemanticsIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.data.redis.connect-timeout=250ms",
                "spring.data.redis.timeout=250ms"
        })
class RedisFailureSemanticsIT {

    private static final String REDIS_IMAGE =
            "redis:7-alpine@sha256:8b81dd37ff027bec4e516d41acfbe9fe2460070dc6d4a4570a2ac5b9d59df065";
    private static final String REDIS_PASSWORD = "c4c-" + UUID.randomUUID();
    private static final int REDIS_HOST_PORT = findAvailablePort();

    @Container
    private static final IsolatedRedisContainer REDIS =
            new IsolatedRedisContainer(DockerImageName.parse(REDIS_IMAGE), REDIS_HOST_PORT)
            .withCommand("redis-server", "--requirepass", REDIS_PASSWORD)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
            .withStartupTimeout(Duration.ofMinutes(1));

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private RedisAsyncTaskManager taskManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SyntheticDurableTaskStatusStore durableTaskStatusStore;

    @BeforeEach
    void resetSyntheticState() {
        authService.loginCalls.set(0);
    }

    @Autowired
    private SyntheticAuthService authService;

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
    }

    @AfterEach
    void ensureIsolatedRedisIsRunning() {
        if (REDIS.getContainerId() != null && !isContainerRunning()) {
            REDIS.getDockerClient().startContainerCmd(REDIS.getContainerId()).exec();
            awaitRedisRecovery();
        }
    }

    @Test
    void isolatedStopStartShouldExposeConsumerSpecificSemantics() {
        int mappedPortBeforeStop = REDIS.getMappedPort(6379);
        ResponseEntity<JsonNode> healthyLogin = login();
        assertEquals(HttpStatus.OK, healthyLogin.getStatusCode());
        assertEquals(1, authService.loginCalls.get());

        TaskHandle<String> task = taskManager.submit("C4C_SYNTHETIC", ignored -> "done");
        assertEquals("done", task.future().join());

        stopOnlyThisTestContainer();

        ResponseEntity<JsonNode> optional = http.getForEntity("/fault/optional-embedding", JsonNode.class);
        assertEquals(HttpStatus.OK, optional.getStatusCode());
        assertArrayEquals(new float[]{0.25F, 0.75F}, toFloatArray(optional.getBody()));

        ResponseEntity<JsonNode> unavailableLogin = login();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unavailableLogin.getStatusCode());
        assertEquals("REDIS_DEPENDENCY_UNAVAILABLE",
                unavailableLogin.getBody().path("errorCode").asText());
        assertEquals(1, authService.loginCalls.get(), "限流依赖失败时 controller 不得执行");

        ResponseEntity<JsonNode> unavailableTask = http.getForEntity(
                "/fault/tasks/" + task.taskId(), JsonNode.class);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unavailableTask.getStatusCode());
        assertEquals("REDIS_DEPENDENCY_UNAVAILABLE",
                unavailableTask.getBody().path("errorCode").asText());

        startOnlyThisTestContainer();
        awaitRedisRecovery();
        assertEquals(mappedPortBeforeStop, REDIS.getMappedPort(6379),
                "隔离 Redis stop/start 后宿主端口不得漂移");

        ResponseEntity<JsonNode> recoveredLogin = awaitApplicationRedisRecovery();
        assertEquals(HttpStatus.OK, recoveredLogin.getStatusCode());
        assertEquals(2, authService.loginCalls.get());
    }

    @Test
    void restartThenProjectionMissRebuildsOwnerStatusFromDurableStore() {
        String taskId = "c5-durable-restart-" + UUID.randomUUID();
        durableTaskStatusStore.put(TaskStatus.running(
                taskId, "DOCUMENT_INDEX", 65, "durable-running", 77L));
        stringRedisTemplate.delete(RedisKeyConstants.taskStatusKey(taskId));

        TaskStatus initialFallback = taskManager.getStatus(taskId).orElseThrow();
        assertEquals(77L, initialFallback.ownerId());
        assertNotNull(stringRedisTemplate.opsForValue().get(RedisKeyConstants.taskStatusKey(taskId)));

        stopOnlyThisTestContainer();
        assertThrows(RedisDependencyException.class, () -> taskManager.getStatus(taskId));

        startOnlyThisTestContainer();
        awaitRedisRecovery();
        awaitApplicationRedisRecovery();
        stringRedisTemplate.delete(RedisKeyConstants.taskStatusKey(taskId));

        TaskStatus rebuilt = taskManager.getStatus(taskId).orElseThrow();
        assertEquals(77L, rebuilt.ownerId());
        assertEquals(65, rebuilt.progress());
        assertNotNull(stringRedisTemplate.opsForValue().get(RedisKeyConstants.taskStatusKey(taskId)));
    }

    private ResponseEntity<JsonNode> login() {
        return http.postForEntity(
                "/auth/login",
                Map.of("username", "synthetic-user", "password", "Synthetic-Password-1!"),
                JsonNode.class);
    }

    private void stopOnlyThisTestContainer() {
        REDIS.getDockerClient()
                .stopContainerCmd(REDIS.getContainerId())
                .withTimeout(5)
                .exec();
        awaitContainerState(false);
    }

    private void startOnlyThisTestContainer() {
        REDIS.getDockerClient().startContainerCmd(REDIS.getContainerId()).exec();
        awaitContainerState(true);
    }

    private void awaitContainerState(boolean expectedRunning) {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            if (isContainerRunning() == expectedRunning) {
                return;
            }
            sleepBriefly();
        }
        fail("isolated Redis container did not reach running=" + expectedRunning);
    }

    private boolean isContainerRunning() {
        Boolean running = REDIS.getDockerClient()
                .inspectContainerCmd(REDIS.getContainerId())
                .exec()
                .getState()
                .getRunning();
        return Boolean.TRUE.equals(running);
    }

    private void awaitRedisRecovery() {
        Instant deadline = Instant.now().plusSeconds(20);
        while (Instant.now().isBefore(deadline)) {
            try {
                var result = REDIS.execInContainer(
                        "redis-cli", "-a", REDIS_PASSWORD, "PING");
                if (result.getExitCode() == 0 && result.getStdout().contains("PONG")) {
                    return;
                }
            } catch (Exception ignored) {
                // 容器进程和监听端口恢复存在短暂窗口。
            }
            sleepBriefly();
        }
        fail("isolated Redis container did not recover");
    }

    private ResponseEntity<JsonNode> awaitApplicationRedisRecovery() {
        Instant deadline = Instant.now().plusSeconds(20);
        ResponseEntity<JsonNode> lastResponse = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                lastResponse = login();
                if (lastResponse.getStatusCode() == HttpStatus.OK) {
                    return lastResponse;
                }
            } catch (Exception ignored) {
                // Lettuce reconnect and the first command after restart have a short recovery window.
            }
            sleepBriefly();
        }
        fail("application Redis connection did not recover, lastResponse=" + lastResponse);
        throw new IllegalStateException("unreachable");
    }

    private void sleepBriefly() {
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("interrupted while waiting for isolated Redis container", e);
        }
    }

    private static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("cannot allocate isolated Redis test port", e);
        }
    }

    private float[] toFloatArray(JsonNode body) {
        assertFalse(body == null || !body.isArray());
        float[] values = new float[body.size()];
        for (int i = 0; i < body.size(); i++) {
            values[i] = (float) body.get(i).asDouble();
        }
        return values;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class
    })
    @Import({
            AuthController.class,
            GlobalExceptionHandler.class,
            RateLimitWebConfig.class,
            FaultProbeController.class
    })
    static class TestApplication {

        @Bean
        StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
            return new StringRedisTemplate(connectionFactory);
        }

        @Bean
        RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.afterPropertiesSet();
            return template;
        }

        @Bean
        RedisUtil redisUtil(
                RedisTemplate<String, Object> redisTemplate,
                StringRedisTemplate stringRedisTemplate) {
            return new RedisUtil(redisTemplate, stringRedisTemplate);
        }

        @Bean
        SlidingWindowRateLimiter rateLimiter(StringRedisTemplate stringRedisTemplate) {
            return new SlidingWindowRateLimiter(stringRedisTemplate);
        }

        @Bean
        RateLimitInterceptor rateLimitInterceptor(SlidingWindowRateLimiter rateLimiter) {
            return new RateLimitInterceptor(rateLimiter);
        }

        @Bean
        SyntheticAuthService authService() {
            return new SyntheticAuthService();
        }

        @Bean
        RedisAsyncTaskManager taskManager(
                StringRedisTemplate stringRedisTemplate,
                ObjectMapper objectMapper,
                SyntheticDurableTaskStatusStore durableTaskStatusStore) {
            return new RedisAsyncTaskManager(
                    stringRedisTemplate, objectMapper, Runnable::run, durableTaskStatusStore);
        }

        @Bean
        SyntheticDurableTaskStatusStore durableTaskStatusStore() {
            return new SyntheticDurableTaskStatusStore();
        }

        @Bean
        EmbeddingService embeddingService(RedisUtil redisUtil, ObjectMapper objectMapper) {
            EmbeddingProvider provider = new EmbeddingProvider() {
                @Override
                public float[] getEmbedding(String text) {
                    return new float[]{0.25F, 0.75F};
                }

                @Override
                public int getDimension() {
                    return 2;
                }

                @Override
                public String getModelName() {
                    return "c4c-synthetic";
                }
            };
            return new EmbeddingServiceImpl(List.of(provider), redisUtil, objectMapper, false, 60);
        }
    }

    static class SyntheticAuthService implements AuthService {

        private final AtomicInteger loginCalls = new AtomicInteger();

        @Override
        public AuthResponse login(LoginRequest request) {
            loginCalls.incrementAndGet();
            return AuthResponse.builder()
                    .accessToken("synthetic-access-marker")
                    .refreshToken("synthetic-refresh-marker")
                    .expiresIn(60)
                    .build();
        }

        @Override
        public void logout(String accessToken) {
            // not used
        }

        @Override
        public AuthResponse refreshToken(String refreshToken) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public UserPrincipal validateToken(String token) {
            throw new UnsupportedOperationException("not used");
        }
    }

    static class SyntheticDurableTaskStatusStore implements DurableTaskStatusStore {

        private final Map<String, TaskStatus> statuses = new ConcurrentHashMap<>();

        void put(TaskStatus status) {
            statuses.put(status.taskId(), status);
        }

        @Override
        public Optional<TaskStatus> find(String taskId) {
            return Optional.ofNullable(statuses.get(taskId));
        }
    }

    static final class IsolatedRedisContainer extends GenericContainer<IsolatedRedisContainer> {

        IsolatedRedisContainer(DockerImageName imageName, int hostPort) {
            super(imageName);
            addFixedExposedPort(hostPort, 6379);
        }
    }

    @RestController
    @RequestMapping("/fault")
    static class FaultProbeController {

        private final EmbeddingService embeddingService;
        private final RedisAsyncTaskManager taskManager;

        FaultProbeController(
                EmbeddingService embeddingService,
                RedisAsyncTaskManager taskManager) {
            this.embeddingService = embeddingService;
            this.taskManager = taskManager;
        }

        @GetMapping("/optional-embedding")
        float[] optionalEmbedding() {
            return embeddingService.embed("c4c synthetic content");
        }

        @GetMapping("/tasks/{taskId}")
        Object taskStatus(@PathVariable String taskId) {
            return taskManager.getStatus(taskId).orElse(null);
        }
    }
}
