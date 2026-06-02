package com.enterprise.rag.core.rag.citation;

import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CitationValidatorTest {

    private final CitationValidator validator = new CitationValidator();

    @Test
    void shouldKeepCitationWhenSnippetMatchesContext() {
        RetrievedContext context = context("chunk-1", "Spring Boot 自动配置会根据 classpath 和配置创建 Bean。", 1L);
        Citation citation = Citation.grounded("chunk-1", 1L, "chunk-1", 0.91d,
                "Spring Boot 自动配置会根据 classpath 和配置创建 Bean", -1, -1);

        CitationValidationResult result = validator.validate(List.of(citation), List.of(context));

        assertThat(result.validCitations()).hasSize(1);
        assertThat(result.droppedCitations()).isEmpty();
        assertThat(result.citations().get(0).documentId()).isEqualTo(1L);
        assertThat(result.citations().get(0).chunkId()).isEqualTo("chunk-1");
        assertThat(result.citations().get(0).score()).isCloseTo(0.91d, within(0.0001d));
        assertThat(result.citationCoverage()).isEqualTo(1.0d);
    }

    @Test
    void shouldDropCitationWhenSourceMatchesButSnippetDoesNot() {
        RetrievedContext context = context("chunk-1", "JWT 由 header、payload、signature 三部分组成。", 1L);
        Citation citation = Citation.grounded("chunk-1", 1L, "chunk-1", 0.9d,
                "Spring Boot 自动配置会创建 Bean", -1, -1);

        CitationValidationResult result = validator.validate(List.of(citation), List.of(context));

        assertThat(result.validCitations()).isEmpty();
        assertThat(result.droppedCitations()).containsExactly(citation);
        assertThat(result.citationCoverage()).isZero();
    }

    @Test
    void shouldDropCitationWhenChunkIdDoesNotExist() {
        RetrievedContext context = context("chunk-1", "RAG 先检索相关上下文，再生成回答。", 1L);
        Citation citation = Citation.grounded("chunk-1", 1L, "missing-chunk", 0.9d,
                "RAG 先检索相关上下文，再生成回答", -1, -1);

        CitationValidationResult result = validator.validate(List.of(citation), List.of(context));

        assertThat(result.validCitations()).isEmpty();
        assertThat(result.droppedCitations()).containsExactly(citation);
    }

    @Test
    void shouldDropDuplicateCitations() {
        RetrievedContext context = context("chunk-1", "Redis 常用于缓存热点数据和减少数据库压力。", 1L);
        Citation first = Citation.grounded("chunk-1", 1L, "chunk-1", 0.8d,
                "Redis 常用于缓存热点数据", -1, -1);
        Citation duplicate = Citation.grounded("chunk-1", 1L, "chunk-1", 0.8d,
                "Redis 常用于缓存热点数据", -1, -1);

        CitationValidationResult result = validator.validate(List.of(first, duplicate), List.of(context));

        assertThat(result.validCitations()).hasSize(1);
        assertThat(result.droppedCitations()).containsExactly(duplicate);
        assertThat(result.citationCoverage()).isEqualTo(0.5d);
    }

    @Test
    void shouldDropAllCitationsForNoAnswer() {
        RetrievedContext context = context("chunk-1", "MySQL 索引可以提升查询效率。", 1L);
        Citation citation = Citation.grounded("chunk-1", 1L, "chunk-1", 0.9d,
                "MySQL 索引可以提升查询效率", -1, -1);

        CitationValidationResult result = validator.validate(List.of(citation), List.of(context), true);

        assertThat(result.validCitations()).isEmpty();
        assertThat(result.droppedCitations()).containsExactly(citation);
        assertThat(result.citationCoverage()).isZero();
    }

    @Test
    void shouldMatchChineseSnippetByTokenOverlap() {
        RetrievedContext context = context("chunk-1",
                "检索增强生成的流程包括问题向量化、向量检索、上下文拼接和答案生成。", 1L);
        Citation citation = Citation.grounded("chunk-1", 1L, "chunk-1", 0.93d,
                "检索增强生成包括问题向量化、上下文拼接与答案生成", -1, -1);

        CitationValidationResult result = validator.validate(List.of(citation), List.of(context));

        assertThat(result.validCitations()).hasSize(1);
        assertThat(result.validCitations().get(0).evidenceMatch().matchType()).isEqualTo("token_overlap");
        assertThat(result.validCitations().get(0).evidenceMatch().overlapRatio()).isGreaterThanOrEqualTo(0.58d);
    }

    private RetrievedContext context(String source, String content, Long documentId) {
        return new RetrievedContext(content, source, 0.91f, Map.of("documentId", documentId));
    }
}
