package com.enterprise.rag.core.rag;

import com.enterprise.rag.common.trace.GenAiTelemetry;
import com.enterprise.rag.common.util.RedisUtil;
import com.enterprise.rag.core.rag.generator.AnswerGenerator;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.query.RetrievalResult;
import com.enterprise.rag.core.rag.service.RAGServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RAGServiceTelemetryTest {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final SdkTracerProvider provider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();
    private final OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(provider)
            .build();

    @AfterEach
    void cleanup() {
        provider.close();
        exporter.reset();
    }

    @Test
    void successfulAskExportsBoundedStageTopologyAndFinalLineageOnly() {
        QueryEngine queryEngine = mock(QueryEngine.class);
        AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
        RedisUtil redis = mock(RedisUtil.class);
        RetrievedContext selected = new RetrievedContext(
                "raw-sensitive-context",
                "raw-sensitive-source",
                0.91f,
                Map.of("ingestTaskId", "task-99", "documentId", 99L, "chunkId", "chunk-99"));
        when(queryEngine.retrieveWithDiagnostics(any(), any(RetrieveOptions.class)))
                .thenReturn(new RetrievalResult(List.of(selected), Map.of(
                        "rerankRequestedProvider", "nvidia",
                        "rerankEffectiveProvider", "heuristic",
                        "rerankFallbackCount", 1,
                        "rerankFallbackReason", "timeout")));
        when(answerGenerator.generate(any(), any()))
                .thenReturn(GeneratedAnswer.of("raw-sensitive-answer", List.of(), Map.of()));
        when(answerGenerator.getModelName()).thenReturn("test-model");
        RAGServiceImpl service = new RAGServiceImpl(
                queryEngine, answerGenerator, redis, new ObjectMapper(), new GenAiTelemetry(openTelemetry));

        var response = service.ask(new QARequest(
                "raw-sensitive-question", "raw-sensitive-collection", 1, 0.3f, Map.of(), false, false));

        assertTrue(response.hasResult());
        Set<String> names = exporter.getFinishedSpanItems().stream()
                .map(span -> span.getName())
                .collect(Collectors.toSet());
        assertTrue(names.containsAll(Set.of(
                GenAiTelemetry.SpanNames.ASK,
                GenAiTelemetry.SpanNames.RETRIEVAL,
                GenAiTelemetry.SpanNames.GENERATION)));
        assertFalse(names.contains(GenAiTelemetry.SpanNames.CACHE_LOOKUP));
        var ask = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.ASK.equals(span.getName()))
                .findFirst().orElseThrow();
        assertEquals(1, ask.getEvents().stream()
                .filter(event -> "rag.lineage.context".equals(event.getName()))
                .count());
        assertTrue(ask.getEvents().stream()
                .filter(event -> "rag.lineage.context".equals(event.getName()))
                .allMatch(event -> "COMPLETE".equals(event.getAttributes()
                        .get(GenAiTelemetry.Attributes.LINEAGE_STATUS))));
        var retrieval = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.RETRIEVAL.equals(span.getName()))
                .findFirst().orElseThrow();
        assertEquals("nvidia", retrieval.getAttributes().get(GenAiTelemetry.Attributes.PROVIDER_REQUESTED));
        assertEquals("heuristic", retrieval.getAttributes().get(GenAiTelemetry.Attributes.PROVIDER_EFFECTIVE));
        assertEquals(1L, retrieval.getAttributes().get(GenAiTelemetry.Attributes.FALLBACK_COUNT));
        assertEquals("timeout", retrieval.getAttributes().get(GenAiTelemetry.Attributes.FALLBACK_REASON));
        String exported = exporter.getFinishedSpanItems().toString();
        assertFalse(exported.contains("raw-sensitive-question"));
        assertFalse(exported.contains("raw-sensitive-context"));
        assertFalse(exported.contains("raw-sensitive-answer"));
        assertFalse(exported.contains("raw-sensitive-source"));
        assertFalse(exported.contains("raw-sensitive-collection"));
    }

    @Test
    void cacheHitCreatesOnlyCacheStageAndSkipsRetrievalAndGeneration() throws Exception {
        QueryEngine queryEngine = mock(QueryEngine.class);
        AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
        RedisUtil redis = mock(RedisUtil.class);
        ObjectMapper objectMapper = new ObjectMapper();
        when(answerGenerator.getModelName()).thenReturn("test-model");
        when(redis.getString(any())).thenReturn(objectMapper.writeValueAsString(
                com.enterprise.rag.core.rag.model.QAResponse.noResult("cached-question")));
        RAGServiceImpl service = new RAGServiceImpl(
                queryEngine, answerGenerator, redis, objectMapper, new GenAiTelemetry(openTelemetry));

        service.ask(QARequest.of("cached-question", "kb"));

        Set<String> names = exporter.getFinishedSpanItems().stream()
                .map(span -> span.getName())
                .collect(Collectors.toSet());
        assertEquals(Set.of(GenAiTelemetry.SpanNames.ASK, GenAiTelemetry.SpanNames.CACHE_LOOKUP), names);
        verify(queryEngine, never()).retrieveWithDiagnostics(any(), any());
        verify(answerGenerator, never()).generate(any(), any());
    }

    @Test
    void streamingAskEndsExactlyOnceOnCancellation() {
        QueryEngine queryEngine = mock(QueryEngine.class);
        AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
        RedisUtil redis = mock(RedisUtil.class);
        RetrievedContext selected = new RetrievedContext("context", "source", 0.8f, Map.of());
        when(queryEngine.retrieveWithDiagnostics(any(), any(RetrieveOptions.class)))
                .thenReturn(RetrievalResult.complete(List.of(selected)));
        when(answerGenerator.generateStream(any(), any())).thenReturn(reactor.core.publisher.Flux.never());
        RAGServiceImpl service = new RAGServiceImpl(
                queryEngine, answerGenerator, redis, new ObjectMapper(), new GenAiTelemetry(openTelemetry));

        reactor.core.Disposable subscription = service.askStream(QARequest.stream("question", "kb")).subscribe();
        assertTrue(exporter.getFinishedSpanItems().stream()
                .noneMatch(span -> GenAiTelemetry.SpanNames.ASK.equals(span.getName())));

        subscription.dispose();

        List<io.opentelemetry.sdk.trace.data.SpanData> asks = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.ASK.equals(span.getName()))
                .toList();
        assertEquals(1, asks.size());
        assertEquals("CANCELLED", asks.get(0).getAttributes().get(GenAiTelemetry.Attributes.OUTCOME));
    }

    @Test
    void lineageIsBoundedToTopKAndClassifiesPartialAndMissingLegacyMetadata() {
        QueryEngine queryEngine = mock(QueryEngine.class);
        AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
        RedisUtil redis = mock(RedisUtil.class);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("one", "one", 0.9f,
                        Map.of("documentId", 1L, "chunkId", "chunk-1")),
                new RetrievedContext("two", "two", 0.8f, Map.of()),
                new RetrievedContext("three", "three", 0.7f,
                        Map.of("ingestTaskId", "task-3", "documentId", 3L, "chunkId", "chunk-3")));
        when(queryEngine.retrieveWithDiagnostics(any(), any(RetrieveOptions.class)))
                .thenReturn(RetrievalResult.complete(contexts));
        when(answerGenerator.generate(any(), any()))
                .thenReturn(GeneratedAnswer.of("answer", List.of(), Map.of()));
        when(answerGenerator.getModelName()).thenReturn("test-model");
        RAGServiceImpl service = new RAGServiceImpl(
                queryEngine, answerGenerator, redis, new ObjectMapper(), new GenAiTelemetry(openTelemetry));

        service.ask(new QARequest("question", "kb", 2, 0.3f, Map.of(), false, false));

        var events = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.ASK.equals(span.getName()))
                .findFirst().orElseThrow().getEvents().stream()
                .filter(event -> "rag.lineage.context".equals(event.getName()))
                .toList();
        assertEquals(2, events.size());
        assertEquals(List.of("PARTIAL", "MISSING"), events.stream()
                .map(event -> event.getAttributes().get(GenAiTelemetry.Attributes.LINEAGE_STATUS))
                .toList());
    }

    @Test
    void streamingTimeoutUsesTimeoutTerminalOutcomeExactlyOnce() {
        QueryEngine queryEngine = mock(QueryEngine.class);
        AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
        RedisUtil redis = mock(RedisUtil.class);
        when(queryEngine.retrieveWithDiagnostics(any(), any(RetrieveOptions.class)))
                .thenReturn(RetrievalResult.complete(List.of(
                        new RetrievedContext("context", "source", 0.8f, Map.of()))));
        when(answerGenerator.generateStream(any(), any())).thenReturn(
                reactor.core.publisher.Flux.error(new java.util.concurrent.TimeoutException("raw-timeout-message")));
        RAGServiceImpl service = new RAGServiceImpl(
                queryEngine, answerGenerator, redis, new ObjectMapper(), new GenAiTelemetry(openTelemetry));

        service.askStream(QARequest.stream("question", "kb"))
                .onErrorResume(ignored -> reactor.core.publisher.Flux.empty())
                .blockLast();

        List<io.opentelemetry.sdk.trace.data.SpanData> asks = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.ASK.equals(span.getName()))
                .toList();
        assertEquals(1, asks.size());
        assertEquals("TIMEOUT", asks.get(0).getAttributes().get(GenAiTelemetry.Attributes.OUTCOME));
        assertFalse(asks.get(0).toString().contains("raw-timeout-message"));
    }

    @Test
    void syncNoResultSkipsGenerationAndIsNotTelemetryError() {
        QueryEngine queryEngine = mock(QueryEngine.class);
        AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
        when(queryEngine.retrieveWithDiagnostics(any(), any(RetrieveOptions.class)))
                .thenReturn(RetrievalResult.complete(List.of()));
        RAGServiceImpl service = new RAGServiceImpl(
                queryEngine, answerGenerator, mock(RedisUtil.class), new ObjectMapper(),
                new GenAiTelemetry(openTelemetry));

        service.ask(new QARequest("RAG", "kb", 2, 0.3f, Map.of(), false, false));

        var ask = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.ASK.equals(span.getName()))
                .findFirst().orElseThrow();
        assertEquals("NO_RESULT", ask.getAttributes().get(GenAiTelemetry.Attributes.OUTCOME));
        assertEquals(io.opentelemetry.api.trace.StatusCode.UNSET, ask.getStatus().getStatusCode());
        assertTrue(exporter.getFinishedSpanItems().stream()
                .noneMatch(span -> GenAiTelemetry.SpanNames.GENERATION.equals(span.getName())));
    }

    @Test
    void streamingCompleteAndNonTimeoutErrorEachEndAskOnce() {
        QueryEngine queryEngine = mock(QueryEngine.class);
        AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
        when(queryEngine.retrieveWithDiagnostics(any(), any(RetrieveOptions.class)))
                .thenReturn(RetrievalResult.complete(List.of(
                        new RetrievedContext("context", "source", 0.8f, Map.of()))));
        when(answerGenerator.generateStream(any(), any()))
                .thenReturn(reactor.core.publisher.Flux.just("done"))
                .thenReturn(reactor.core.publisher.Flux.error(
                        new IllegalStateException("raw-provider-error-message")));
        RAGServiceImpl service = new RAGServiceImpl(
                queryEngine, answerGenerator, mock(RedisUtil.class), new ObjectMapper(),
                new GenAiTelemetry(openTelemetry));

        service.askStream(QARequest.stream("question", "kb")).blockLast();
        service.askStream(QARequest.stream("question", "kb"))
                .onErrorResume(ignored -> reactor.core.publisher.Flux.empty())
                .blockLast();

        List<io.opentelemetry.sdk.trace.data.SpanData> asks = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.ASK.equals(span.getName()))
                .toList();
        assertEquals(2, asks.size());
        assertEquals(List.of("SUCCESS", "ERROR"), asks.stream()
                .map(span -> span.getAttributes().get(GenAiTelemetry.Attributes.OUTCOME))
                .toList());
        assertFalse(asks.toString().contains("raw-provider-error-message"));
    }

    @Test
    void sseEmitterTimeoutSignalTurnsCancellationIntoTimeout() {
        QueryEngine queryEngine = mock(QueryEngine.class);
        AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
        when(queryEngine.retrieveWithDiagnostics(any(), any(RetrieveOptions.class)))
                .thenReturn(RetrievalResult.complete(List.of(
                        new RetrievedContext("context", "source", 0.8f, Map.of()))));
        when(answerGenerator.generateStream(any(), any())).thenReturn(reactor.core.publisher.Flux.never());
        RAGServiceImpl service = new RAGServiceImpl(
                queryEngine, answerGenerator, mock(RedisUtil.class), new ObjectMapper(),
                new GenAiTelemetry(openTelemetry));
        com.enterprise.rag.core.rag.service.RAGService.StreamTerminalSignal terminalSignal =
                new com.enterprise.rag.core.rag.service.RAGService.StreamTerminalSignal();

        reactor.core.Disposable subscription = service.askStream(QARequest.stream("question", "kb"))
                .contextWrite(context -> context.put(
                        com.enterprise.rag.core.rag.service.RAGService.STREAM_TERMINAL_SIGNAL_CONTEXT_KEY,
                        terminalSignal))
                .subscribe();
        terminalSignal.markTimeout();
        subscription.dispose();

        var ask = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.ASK.equals(span.getName()))
                .findFirst().orElseThrow();
        assertEquals("TIMEOUT", ask.getAttributes().get(GenAiTelemetry.Attributes.OUTCOME));
    }
}
