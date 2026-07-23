package com.enterprise.rag.core.rag.generator;

import com.enterprise.rag.common.trace.GenAiTelemetry;
import com.enterprise.rag.core.rag.citation.CitationValidator;
import com.enterprise.rag.core.rag.prompt.PromptBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerGeneratorTelemetryTest {

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
    void syncGenerationEmitsPromptLlmAndCitationStagesWithoutRawContent() {
        LLMProperties properties = new LLMProperties();
        properties.setProvider("openai");
        properties.getOpenai().setApiKey("raw-sensitive-credential");
        properties.getOpenai().setModel("test-model");
        WebClient.Builder webClient = WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("{\"choices\":[{\"message\":{\"content\":\"raw-sensitive-answer\"}}],"
                                + "\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":7}}")
                        .build()));
        AnswerGeneratorImpl generator = new AnswerGeneratorImpl(
                properties, new PromptBuilder(), new CitationValidator(), webClient,
                new GenAiTelemetry(openTelemetry));

        generator.generate("raw-sensitive-question", List.of());

        Set<String> names = exporter.getFinishedSpanItems().stream()
                .map(span -> span.getName())
                .collect(Collectors.toSet());
        assertTrue(names.containsAll(Set.of(
                GenAiTelemetry.SpanNames.PROMPT_BUILD,
                GenAiTelemetry.SpanNames.LLM_REQUEST,
                GenAiTelemetry.SpanNames.CITATION_VALIDATE)));
        var llm = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.LLM_REQUEST.equals(span.getName()))
                .findFirst().orElseThrow();
        assertEquals("openai", llm.getAttributes().get(GenAiTelemetry.Attributes.PROVIDER_REQUESTED));
        assertEquals("openai", llm.getAttributes().get(GenAiTelemetry.Attributes.PROVIDER_EFFECTIVE));
        assertEquals("test-model", llm.getAttributes().get(GenAiTelemetry.Attributes.MODEL));
        assertEquals(1L, llm.getAttributes().get(GenAiTelemetry.Attributes.ATTEMPT_COUNT));
        assertEquals(0L, llm.getAttributes().get(GenAiTelemetry.Attributes.RETRY_COUNT));
        assertEquals(12L, llm.getAttributes().get(GenAiTelemetry.Attributes.TOKEN_INPUT));
        assertEquals(7L, llm.getAttributes().get(GenAiTelemetry.Attributes.TOKEN_OUTPUT));
        var prompt = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.PROMPT_BUILD.equals(span.getName()))
                .findFirst().orElseThrow();
        assertTrue(prompt.getAttributes().asMap()
                .containsKey(GenAiTelemetry.Attributes.PROMPT_ESTIMATED_TOKENS));
        String exported = exporter.getFinishedSpanItems().toString();
        assertFalse(exported.contains("raw-sensitive-question"));
        assertFalse(exported.contains("raw-sensitive-answer"));
        assertFalse(exported.contains("raw-sensitive-credential"));
    }
}
