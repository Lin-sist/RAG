package com.enterprise.rag.core.rag.generator;

import com.enterprise.rag.common.trace.TraceContext;
import com.enterprise.rag.core.rag.citation.CitationValidator;
import com.enterprise.rag.core.rag.prompt.PromptBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.awaitility.Awaitility.await;

@ExtendWith(OutputCaptureExtension.class)
class AnswerGeneratorStreamDiagnosticsTest {
    private static final String QUESTION = "private-question-marker";
    private static final String ANSWER = "private-answer-marker";
    private static final String REASONING = "private-reasoning-marker";
    private static final String CREDENTIAL = "private-credential-marker";

    @AfterEach
    void clearTrace() {
        TraceContext.clear();
    }

    @Test
    void reasoningOnlyStreamReportsLengthAndNumericUsageWithoutLeakingText(CapturedOutput output) {
        String body = event("{\"choices\":[{\"delta\":{\"reasoning_content\":\"" + REASONING
                + "\"},\"finish_reason\":\"length\"}]}")
                + event("{\"choices\":[],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":20,"
                + "\"total_tokens\":32,\"completion_tokens_details\":{\"reasoning_tokens\":20},"
                + "\"private_usage\":\"private-usage-marker\"}}") + event("[DONE]");
        AtomicInteger attempts = new AtomicInteger();
        LLMException error = catchThrowableOfType(() -> generator(request -> {
            attempts.incrementAndGet();
            return response(HttpStatus.OK, "text/event-stream", body);
        }, 0).generateStream(QUESTION, List.of()).collectList().block(), LLMException.class);

        assertThat(error.diagnostics()).containsEntry("errorCategory", "invalid_response");
        assertThat(attempts).hasValue(1);
        String summary = summary(output);
        assertThat(summary).contains("httpStatus=200", "contentType=sse", "finishReason=length",
                "reasoningEvents=1", "reasoningChars=" + REASONING.length(), "contentChars=0",
                "visibleChars=0", "emptyChoicesEvents=1", "doneEvents=1", "promptTokens=12",
                "completionTokens=20", "reasoningTokens=20", "outcome=error");
        assertPrivateTextAbsent(output, "private-usage-marker");
    }

    @Test
    void emptyUsageChoiceDoesNotChangeSuccessfulContent(CapturedOutput output) {
        var chunks = generator(request -> response(HttpStatus.OK, "text/event-stream",
                event("{\"choices\":[{\"delta\":{\"content\":\"" + ANSWER + "\"}}]}")
                + event("{\"choices\":[],\"usage\":{\"completion_tokens\":4}}") + event("[DONE]")), 0)
                .generateStream(QUESTION, List.of()).collectList().block();
        assertThat(chunks).containsExactly(ANSWER);
        assertThat(summary(output)).contains("emptyChoicesEvents=1", "contentEvents=1",
                "contentChars=" + ANSWER.length(), "visibleChars=" + ANSWER.length(), "outcome=complete");
        assertPrivateTextAbsent(output);
    }

    @Test
    void reasoningIsCountedButNeverBecomesVisibleAnswer(CapturedOutput output) {
        var chunks = generator(request -> response(HttpStatus.OK, "text/event-stream",
                event("{\"choices\":[{\"delta\":{\"reasoning_content\":\"" + REASONING + "\"}}]}")
                + event("{\"choices\":[{\"delta\":{\"content\":\"" + ANSWER
                + "\"},\"finish_reason\":\"stop\"}]}") + event("[DONE]")), 0)
                .generateStream(QUESTION, List.of()).collectList().block();
        assertThat(chunks).containsExactly(ANSWER);
        assertThat(summary(output)).contains("reasoningEvents=1", "contentEvents=1", "finishReason=stop");
        assertPrivateTextAbsent(output);
    }

    @Test
    void malformedJsonAndMessageShapeHaveDistinctCounters(CapturedOutput output) {
        var generator = generator(request -> response(HttpStatus.OK, "text/event-stream",
                event("{private-malformed-marker")
                + event("{\"choices\":[{\"message\":{\"content\":\"" + ANSWER + "\"}}]}")
                + event("[DONE]")), 0);
        assertThat(catchThrowableOfType(() -> generator.generateStream(QUESTION, List.of())
                .collectList().block(), LLMException.class)).isNotNull();
        assertThat(summary(output)).contains("jsonParseFailures=1", "messageEvents=1",
                "deltaEvents=0", "contentChars=0", "outcome=error");
        assertPrivateTextAbsent(output, "private-malformed-marker");
    }

    @Test
    void providerControlledLabelsAndNonNumericUsageAreNeverLogged(CapturedOutput output) {
        String body = event("{\"choices\":[{\"delta\":{},\"finish_reason\":\"private-finish-marker\\nsecret\"}],"
                + "\"usage\":{\"prompt_tokens\":\"private-token-marker\",\"completion_tokens\":-7,"
                + "\"total_tokens\":999999999999999999999999999999}}") + event("[DONE]");
        var generator = generator(request -> response(HttpStatus.OK, "application/json; private-header-marker=1", body), 0);
        assertThat(catchThrowableOfType(() -> generator.generateStream(QUESTION, List.of())
                .collectList().block(), LLMException.class)).isNotNull();
        assertThat(summary(output)).contains("contentType=json", "finishReason=other",
                "promptTokens=-1", "completionTokens=-1", "totalTokens=-1");
        assertPrivateTextAbsent(output, "private-finish-marker", "private-token-marker", "private-header-marker");
    }

    @Test
    void httpFailureRetainsErrorSemanticsAndDoesNotLogErrorBody(CapturedOutput output) {
        AtomicInteger attempts = new AtomicInteger();
        var generator = generator(request -> {
            attempts.incrementAndGet();
            return response(HttpStatus.SERVICE_UNAVAILABLE, "application/json", "private-error-body-marker");
        }, 0);
        LLMException error = catchThrowableOfType(() -> generator.generateStream(QUESTION, List.of())
                .collectList().block(), LLMException.class);
        assertThat(error.diagnostics()).containsEntry("httpStatus", 503).containsEntry("retryCount", 0);
        assertThat(attempts).hasValue(1);
        assertThat(summary(output)).contains("httpStatus=503", "contentType=json", "events=0", "outcome=error");
        assertPrivateTextAbsent(output, "private-error-body-marker");
    }

    @Test
    void retryAttemptsHaveIndependentCountsAndOneSummaryEach(CapturedOutput output) {
        AtomicInteger attempts = new AtomicInteger();
        var chunks = generator(request -> attempts.incrementAndGet() == 1
                ? response(HttpStatus.SERVICE_UNAVAILABLE, "application/json", "private-error-body-marker")
                : response(HttpStatus.OK, "text/event-stream",
                        event("{\"choices\":[{\"delta\":{\"content\":\"" + ANSWER + "\"}}]}") + event("[DONE]")), 1)
                .generateStream(QUESTION, List.of()).collectList().block();
        assertThat(chunks).containsExactly(ANSWER);
        assertThat(attempts).hasValue(2);
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(summaries(output)).hasSize(2));
        assertThat(summaries(output).get(0)).contains("attempt=1", "httpStatus=503", "events=0");
        assertThat(summaries(output).get(1)).contains("attempt=2", "httpStatus=200", "contentEvents=1");
        assertThat(summaries(output).get(0).split("streamId=")[1].split(" ")[0])
                .isEqualTo(summaries(output).get(1).split("streamId=")[1].split(" ")[0]);
        assertPrivateTextAbsent(output, "private-error-body-marker");
    }

    @Test
    void cancellationProducesOneSummaryWithoutResubscribing(CapturedOutput output) {
        AtomicInteger attempts = new AtomicInteger();
        var subscription = generator(request -> {
            attempts.incrementAndGet();
            return Mono.never();
        }, 0).generateStream(QUESTION, List.of()).subscribe();
        subscription.dispose();
        assertThat(attempts).hasValue(1);
        assertThat(summary(output)).contains("outcome=cancel", "httpStatus=-1", "events=0");
        assertThat(summaries(output)).hasSize(1);
        assertPrivateTextAbsent(output);
    }

    @Test
    void partialStreamFailureIsReportedAsErrorEvenWhenRetryOperatorCancelsItsSource(CapturedOutput output) {
        AtomicInteger attempts = new AtomicInteger();
        byte[] bytes = event("{\"choices\":[{\"delta\":{\"content\":\"" + ANSWER + "\"}}]}")
                .getBytes(StandardCharsets.UTF_8);
        var signals = generator(request -> {
            attempts.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "text/event-stream")
                    .body(Flux.<DataBuffer>just(DefaultDataBufferFactory.sharedInstance.wrap(bytes))
                            .concatWith(Flux.error(new IOException("private-transport-error-marker")))).build());
        }, 1).generateStream(QUESTION, List.of()).materialize().collectList().block();
        assertThat(signals).anyMatch(signal -> signal.isOnError());
        assertThat(attempts).hasValue(1);
        assertThat(summary(output)).contains("contentEvents=1", "outcome=error");
        assertThat(summaries(output)).hasSize(1);
        assertPrivateTextAbsent(output, "private-transport-error-marker");
    }

    @Test
    void traceIsCapturedBeforeAsyncResponseAndUntrustedTraceIsRedacted(CapturedOutput output) {
        String trace = "0123456789abcdef0123456789abcdef";
        TraceContext.setTraceId(trace);
        var stream = generator(request -> response(HttpStatus.OK, "text/event-stream",
                event("{\"choices\":[{\"delta\":{\"content\":\"" + ANSWER + "\"}}]}"))
                .delayElement(Duration.ofMillis(10)), 0).generateStream(QUESTION, List.of());
        TraceContext.clear();
        assertThat(stream.collectList().block()).containsExactly(ANSWER);
        assertThat(summary(output)).contains("traceId=" + trace);
        TraceContext.setTraceId("private-trace-marker\nsecret");
        generator(request -> response(HttpStatus.OK, "text/event-stream",
                event("{\"choices\":[{\"delta\":{\"content\":\"" + ANSWER + "\"}}]}")), 0)
                .generateStream(QUESTION, List.of()).collectList().block();
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(summaries(output)).hasSize(2));
        assertThat(summaries(output).get(1)).contains("traceId=missing");
        assertThat(summaries(output).get(1)).doesNotContain("private-trace-marker", "secret");
    }

    private AnswerGeneratorImpl generator(ExchangeFunction exchange, int retries) {
        LLMProperties properties = new LLMProperties();
        properties.setProvider("openai"); properties.setMaxRetries(retries); properties.setTimeout(2);
        properties.getOpenai().setBaseUrl("http://127.0.0.1/in-memory-only");
        properties.getOpenai().setApiKey(CREDENTIAL); properties.getOpenai().setModel("synthetic-model");
        return new AnswerGeneratorImpl(properties, new PromptBuilder(), new CitationValidator(),
                WebClient.builder().exchangeFunction(exchange));
    }

    private Mono<ClientResponse> response(HttpStatus status, String contentType, String body) {
        return Mono.just(ClientResponse.create(status).header("Content-Type", contentType).body(body).build());
    }

    private static String event(String json) { return "data: " + json + "\n\n"; }

    private static List<String> summaries(CapturedOutput output) {
        return output.toString().lines().filter(line -> line.contains("llm_stream_attempt ")).toList();
    }

    private static String summary(CapturedOutput output) {
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(summaries(output)).isNotEmpty());
        return summaries(output).get(summaries(output).size() - 1);
    }

    private static void assertPrivateTextAbsent(CapturedOutput output, String... extra) {
        assertThat(output).doesNotContain(QUESTION, ANSWER, REASONING, CREDENTIAL);
        if (extra.length > 0) assertThat(output).doesNotContain(extra);
    }
}
