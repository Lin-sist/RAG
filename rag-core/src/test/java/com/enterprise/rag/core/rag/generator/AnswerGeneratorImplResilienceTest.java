package com.enterprise.rag.core.rag.generator;

import com.enterprise.rag.core.rag.citation.CitationValidator;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.prompt.PromptBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Signal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@ExtendWith(OutputCaptureExtension.class)
class AnswerGeneratorImplResilienceTest {

    private HttpServer server;
    private AtomicInteger attempts;

    @Test
    void shouldKeepRetriesDisabledByDefault() {
        assertThat(new LLMProperties().getMaxRetries()).isZero();
    }

    @BeforeEach
    void setUp() throws IOException {
        attempts = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldRetryTransientRateLimitWithinConfiguredBudget() {
        server.createContext("/v1/chat/completions", exchange -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                sendJson(exchange, 429, "{\"error\":\"synthetic rate limit\"}");
                return;
            }
            sendJson(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"重试成功\"}}]}");
        });

        GeneratedAnswer result = generator(1).generate("测试重试", List.of());

        assertThat(result.answer()).isEqualTo("重试成功");
        assertThat(attempts).hasValue(2);
    }

    @Test
    void shouldExposeSafeAttemptDiagnosticsWhenRetriesAreExhausted(CapturedOutput output) {
        server.createContext("/v1/chat/completions", exchange -> {
            attempts.incrementAndGet();
            sendJson(exchange, 503, "{\"error\":\"secret provider detail\"}");
        });

        LLMException error = catchThrowableOfType(
                () -> generator(1).generate("synthetic prompt marker", List.of()),
                LLMException.class);

        assertThat(attempts).hasValue(2);
        assertThat(error.diagnostics())
                .containsEntry("attemptCount", 2)
                .containsEntry("retryCount", 1)
                .containsEntry("retryExhausted", true)
                .containsEntry("errorCategory", "provider_5xx")
                .containsEntry("httpStatus", 503);
        assertThat(error.getMessage()).doesNotContain("secret provider detail");
        assertThat(error.diagnostics().toString())
                .doesNotContain("secret provider detail", "synthetic prompt marker", "synthetic-api-key-marker");
        assertThat(output)
                .doesNotContain("secret provider detail", "synthetic prompt marker", "synthetic-api-key-marker");
    }

    @Test
    void shouldNotRetryMalformedSuccessResponse() {
        server.createContext("/v1/chat/completions", exchange -> {
            attempts.incrementAndGet();
            sendJson(exchange, 200, "{malformed synthetic body");
        });

        LLMException error = catchThrowableOfType(
                () -> generator(2).generate("测试非法响应", List.of()),
                LLMException.class);

        assertThat(attempts).hasValue(1);
        assertThat(error.diagnostics())
                .containsEntry("attemptCount", 1)
                .containsEntry("retryCount", 0)
                .containsEntry("retryExhausted", false)
                .containsEntry("errorCategory", "invalid_response");
        assertThat(error.getMessage()).doesNotContain("malformed synthetic body");
    }

    @Test
    void shouldNotRetryNonTransientClientError() {
        server.createContext("/v1/chat/completions", exchange -> {
            attempts.incrementAndGet();
            sendJson(exchange, 401, "{\"error\":\"synthetic unauthorized detail\"}");
        });

        LLMException error = catchThrowableOfType(
                () -> generator(2).generate("测试四零一", List.of()),
                LLMException.class);

        assertThat(attempts).hasValue(1);
        assertThat(error.diagnostics())
                .containsEntry("attemptCount", 1)
                .containsEntry("retryCount", 0)
                .containsEntry("retryExhausted", false)
                .containsEntry("errorCategory", "provider_http_error")
                .containsEntry("httpStatus", 401);
        assertThat(error.getMessage()).doesNotContain("synthetic unauthorized detail");
    }

    @Test
    void shouldRetryTimeoutWithinConfiguredBudget() {
        server.createContext("/v1/chat/completions", exchange -> {
            attempts.incrementAndGet();
            try {
                Thread.sleep(1_200L);
                sendJson(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"late\"}}]}");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                exchange.close();
            }
        });

        LLMException error = catchThrowableOfType(
                () -> generator(1, 1).generate("测试超时", List.of()),
                LLMException.class);

        assertThat(attempts).hasValue(2);
        assertThat(error.diagnostics())
                .containsEntry("attemptCount", 2)
                .containsEntry("retryCount", 1)
                .containsEntry("retryExhausted", true)
                .containsEntry("errorCategory", "timeout");
    }

    @Test
    void shouldNotResubscribeAfterFirstVisibleStreamChunk() {
        server.createContext("/v1/chat/completions", exchange -> {
            attempts.incrementAndGet();
            sendTruncatedEvent(exchange, "alpha");
        });

        List<Signal<String>> signals = generator(1)
                .generateStream("测试流式截断", List.of())
                .materialize()
                .collectList()
                .block();

        assertThat(signals).isNotNull();
        assertThat(signals.stream().filter(Signal::isOnNext).map(Signal::get).toList())
                .containsExactly("alpha");
        Throwable error = signals.stream()
                .filter(Signal::isOnError)
                .map(Signal::getThrowable)
                .findFirst()
                .orElseThrow();
        assertThat(error).isInstanceOf(LLMException.class);
        LLMException llmError = (LLMException) error;
        assertThat(llmError.diagnostics())
                .containsEntry("attemptCount", 1)
                .containsEntry("retryCount", 0)
                .containsEntry("retryExhausted", false);

        assertThat(attempts).hasValue(1);
    }

    @Test
    void shouldRetryTransientFailureBeforeFirstVisibleStreamChunk() {
        server.createContext("/v1/chat/completions", exchange -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                sendJson(exchange, 503, "{\"error\":\"synthetic unavailable\"}");
                return;
            }
            sendEvent(exchange, "beta");
        });

        List<String> chunks = generator(1)
                .generateStream("测试首包前重试", List.of())
                .collectList()
                .block();

        assertThat(chunks).containsExactly("beta");
        assertThat(attempts).hasValue(2);
    }

    @Test
    void shouldNotRetryStreamThatCompletesWithoutVisibleContent() {
        server.createContext("/v1/chat/completions", exchange -> {
            attempts.incrementAndGet();
            sendRawEvent(exchange, "{malformed synthetic stream body");
        });

        List<Signal<String>> signals = generator(2)
                .generateStream("测试流式非法响应", List.of())
                .materialize()
                .collectList()
                .block();

        Throwable error = signals.stream()
                .filter(Signal::isOnError)
                .map(Signal::getThrowable)
                .findFirst()
                .orElseThrow();
        assertThat(error).isInstanceOf(LLMException.class);
        assertThat(((LLMException) error).diagnostics())
                .containsEntry("attemptCount", 1)
                .containsEntry("retryCount", 0)
                .containsEntry("retryExhausted", false)
                .containsEntry("errorCategory", "invalid_response");
        assertThat(attempts).hasValue(1);
    }

    private AnswerGeneratorImpl generator(int maxRetries) {
        return generator(maxRetries, 2);
    }

    private AnswerGeneratorImpl generator(int maxRetries, int timeoutSeconds) {
        LLMProperties properties = new LLMProperties();
        properties.setProvider("openai");
        properties.setMaxRetries(maxRetries);
        properties.setTimeout(timeoutSeconds);
        properties.getOpenai().setApiKey("synthetic-api-key-marker");
        properties.getOpenai().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        properties.getOpenai().setModel("test-model");
        return new AnswerGeneratorImpl(
                properties,
                new PromptBuilder(),
                new CitationValidator(),
                WebClient.builder());
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private void sendTruncatedEvent(HttpExchange exchange, String content) throws IOException {
        byte[] bytes = ("data: {\"choices\":[{\"delta\":{\"content\":\""
                + content
                + "\"}}]}\n\n").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, bytes.length + 100L);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().flush();
        exchange.close();
    }

    private void sendEvent(HttpExchange exchange, String content) throws IOException {
        byte[] bytes = ("data: {\"choices\":[{\"delta\":{\"content\":\""
                + content
                + "\"}}]}\n\n").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private void sendRawEvent(HttpExchange exchange, String data) throws IOException {
        byte[] bytes = ("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
