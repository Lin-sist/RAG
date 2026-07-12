package com.enterprise.rag.core.rag.generator;

import com.enterprise.rag.core.rag.citation.CitationValidator;
import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.QAResponse;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.prompt.PromptBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AnswerGeneratorImplCitationFallbackTest {

    private final TestAnswerGenerator generator = new TestAnswerGenerator();

    @Test
    void shouldKeepEnglishCitationFromOriginalExtraction() {
        RetrievedContext context = context(
                "chunk-en-1",
                "Thread pools reuse worker threads to reduce creation overhead and bound concurrency. "
                        + "They also provide queueing for submitted tasks.",
                11L,
                Map.of("sourceFileName", "threads.md", "documentTitle", "Thread Pool Guide"));
        String answer = "Thread pools reuse worker threads to reduce creation overhead and bound concurrency.";

        AnswerGeneratorImpl.CitationResolution resolution = generator.resolveCitations(
                "What do thread pools do?",
                answer,
                List.of(context));

        assertThat(resolution.fallbackUsed()).isFalse();
        assertThat(resolution.validation().citations()).hasSize(1);
        Citation citation = resolution.validation().citations().get(0);
        assertThat(citation.source()).isEqualTo("chunk-en-1");
        assertThat(citation.chunkId()).isEqualTo("chunk-en-1");
        assertThat(citation.documentId()).isEqualTo(11L);
    }

    @Test
    void shouldFallbackForChineseSummaryWhenOriginalExtractionCannotMatch() {
        RetrievedContext context = context(
                "chunk-cn-1",
                "RAG 通过检索可靠资料来约束模型输出。先检索相关上下文，然后把证据拼接进提示词，最后由模型生成回答。",
                21L,
                Map.of("sourceFileName", "rag-intro.md", "documentTitle", "RAG 入门"));
        String answer = "它通过先找相关资料再组织回答来降低幻觉。";

        AnswerGeneratorImpl.CitationResolution resolution = generator.resolveCitations(
                "RAG 如何降低幻觉？",
                answer,
                List.of(context));

        assertThat(resolution.fallbackUsed()).isTrue();
        assertThat(resolution.fallbackCount()).isEqualTo(1);
        assertThat(resolution.validation().citationCoverage()).isEqualTo(1.0d);
        Citation citation = resolution.validation().citations().get(0);
        assertThat(citation.source()).isEqualTo("chunk-cn-1");
        assertThat(citation.sourceFileName()).isEqualTo("rag-intro.md");
        assertThat(citation.documentTitle()).isEqualTo("RAG 入门");
        assertThat(citation.documentId()).isEqualTo(21L);
        assertThat(citation.chunkId()).isEqualTo("chunk-cn-1");
        assertThat(citation.score()).isCloseTo(0.91d, within(0.0001d));
        assertThat(citation.snippet()).contains("检索");
    }

    @Test
    void shouldNotFallbackForNoAnswerOrNoResult() {
        RetrievedContext context = context(
                "chunk-no-answer",
                "向量数据库可以根据 embedding 相似度召回候选上下文。",
                31L,
                Map.of());

        AnswerGeneratorImpl.CitationResolution resolution = generator.resolveCitations(
                "认证系统怎么做？",
                "没有足够信息回答这个问题。",
                List.of(context));

        assertThat(resolution.fallbackUsed()).isFalse();
        assertThat(resolution.validation().citations()).isEmpty();

        QAResponse noResult = QAResponse.noResult("认证系统怎么做？");
        assertThat(noResult.citations()).isEmpty();
        assertThat(noResult.hasResult()).isFalse();
        assertThat(noResult.metadata()).containsEntry("citationFallbackUsed", false);
        assertThat(noResult.metadata()).containsEntry("citationFallbackCount", 0);
        assertThat(noResult.metadata()).containsEntry("validCitations", 0);
        assertThat(noResult.metadata()).containsEntry("droppedCitations", 0);
        assertThat(noResult.metadata()).containsEntry("citationCoverage", 1.0d);
    }

    @Test
    void shouldNotFallbackWhenModelSaysContextDoesNotContainTheAnswer() {
        RetrievedContext context = context(
                "chunk-unrelated",
                "RAG 系统通过向量检索找到与问题相近的文档片段。",
                32L,
                Map.of("sourceFileName", "rag-guide.md", "documentTitle", "RAG 指南"));
        String answer = "提供的上下文未包含有关 Kubernetes 的信息，因此无法根据现有内容回答该问题。";

        AnswerGeneratorImpl.CitationResolution resolution = generator.resolveCitations(
                "Kubernetes 如何进行滚动更新？",
                answer,
                List.of(context));

        assertThat(resolution.fallbackUsed()).isFalse();
        assertThat(resolution.fallbackCount()).isZero();
        assertThat(resolution.validation().citations()).isEmpty();
    }

    @Test
    void shouldMarkGeneratedRefusalAsNoResult() {
        LLMProperties properties = new LLMProperties();
        properties.setMaxRetries(0);
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("{\"choices\":[{\"message\":{\"content\":\"提供的上下文未包含有关 Kubernetes 的信息，因此无法根据现有内容回答该问题。\"}}]}")
                        .build()));
        AnswerGeneratorImpl publicGenerator = new AnswerGeneratorImpl(
                properties,
                new PromptBuilder(),
                new CitationValidator(),
                webClientBuilder);

        GeneratedAnswer result = publicGenerator.generate(
                "Kubernetes 如何进行滚动更新？",
                List.of(context("chunk-unrelated", "RAG 使用检索上下文约束模型回答。", 33L, Map.of())));

        assertThat(result.citations()).isEmpty();
        assertThat(result.metadata()).containsEntry("status", "no_result");
        assertThat(result.metadata()).containsEntry("citationFallbackUsed", false);
    }

    @Test
    void fallbackCitationShouldStillPassCitationValidator() {
        RetrievedContext context = context(
                "chunk-valid-1",
                "Citation fallback 只从本轮 retrieved contexts 中选取证据，并且仍然需要经过 CitationValidator 校验。",
                41L,
                Map.of("sourceFileName", "citation.md", "documentTitle", "Citation Design"));

        AnswerGeneratorImpl.CitationResolution resolution = generator.resolveCitations(
                "fallback citation 如何保证可信？",
                "fallback 会使用本轮检索到的证据，并继续校验。",
                List.of(context));

        CitationValidator validator = new CitationValidator();
        assertThat(validator.validate(resolution.validation().citations(), List.of(context)).citations())
                .hasSize(1);
    }

    @Test
    void shouldNotGenerateCitationWhenContextIsEmpty() {
        AnswerGeneratorImpl.CitationResolution resolution = generator.resolveCitations(
                "RAG 是什么？",
                "RAG 会结合检索和生成。",
                List.of());

        assertThat(resolution.fallbackUsed()).isFalse();
        assertThat(resolution.fallbackCount()).isZero();
        assertThat(resolution.validation().citations()).isEmpty();
    }

    @Test
    void shouldNotGenerateDuplicateFallbackCitationsForDuplicateContexts() {
        RetrievedContext first = context(
                "chunk-dup-1",
                "检索阶段返回候选上下文，生成阶段只能基于这些上下文作答。",
                51L,
                Map.of("sourceFileName", "pipeline.md", "documentTitle", "RAG Pipeline"));
        RetrievedContext duplicate = context(
                "chunk-dup-1",
                "检索阶段返回候选上下文，生成阶段只能基于这些上下文作答。",
                51L,
                Map.of("sourceFileName", "pipeline.md", "documentTitle", "RAG Pipeline"));

        AnswerGeneratorImpl.CitationResolution resolution = generator.resolveCitations(
                "RAG 的流程是什么？",
                "流程是先找上下文，再依据上下文生成答案。",
                List.of(first, duplicate));

        assertThat(resolution.fallbackUsed()).isTrue();
        assertThat(resolution.validation().citations()).hasSize(1);
        assertThat(resolution.validation().droppedCitations()).isEmpty();
    }

    private RetrievedContext context(String source, String content, Long documentId, Map<String, Object> metadata) {
        java.util.HashMap<String, Object> values = new java.util.HashMap<>(metadata);
        values.put("documentId", documentId);
        return new RetrievedContext(content, source, 0.91f, values);
    }

    private static final class TestAnswerGenerator extends AnswerGeneratorImpl {
        private TestAnswerGenerator() {
            super(new LLMProperties(), new PromptBuilder(), new CitationValidator(), WebClient.builder());
        }
    }
}
