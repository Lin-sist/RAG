package com.enterprise.rag.core.rag.generator;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnswerGeneratorImplSanitizerTest {

    @Test
    void shouldRemoveRawSourceMarkersFromAnswerText() {
        String rawAnswer = "RAG 是一种检索增强生成架构。[Source 1: 0 #chunk=0.0 (Score: 0.34)]中指出，它先检索再生成。";

        String sanitized = AnswerGeneratorImpl.sanitizeAnswerText(rawAnswer);

        assertEquals("RAG 是一种检索增强生成架构。中指出，它先检索再生成。", sanitized);
    }

    @Test
    void shouldRemoveSourceMarkersAcrossStreamChunks() {
        AnswerGeneratorImpl.StreamSanitizer sanitizer = new AnswerGeneratorImpl.StreamSanitizer();

        List<String> part1 = sanitizer.accept("RAG 是一种检索增强生成架构。[Sou");
        List<String> part2 = sanitizer.accept("rce 1: 0 #chunk=0.0 (Score: 0.34)]");
        List<String> part3 = sanitizer.accept("它先检索再生成。");
        List<String> tail = sanitizer.finish();

        assertEquals(List.of("RAG 是一种检索增强生成架构。"), part1);
        assertEquals(List.of(), part2);
        assertEquals(List.of("它先检索再生成。"), part3);
        assertEquals(List.of(), tail);
    }

    @Test
    void shouldSanitizeFluxStreamOutput() {
        TestAnswerGenerator generator = new TestAnswerGenerator();

        List<String> chunks = generator.sanitizeStream(Flux.just(
                "RAG 是一种检索增强生成架构。[Sou",
                "rce 1: 0 #chunk=0.0 (Score: 0.34)]",
                "它先检索再生成。")).collectList().block();

        assertEquals(List.of("RAG 是一种检索增强生成架构。", "它先检索再生成。"), chunks);
    }

    private static final class TestAnswerGenerator extends AnswerGeneratorImpl {
        private TestAnswerGenerator() {
            super(new LLMProperties(), new com.enterprise.rag.core.rag.prompt.PromptBuilder(),
                    org.springframework.web.reactive.function.client.WebClient.builder());
        }
    }
}
