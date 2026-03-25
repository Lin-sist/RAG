package com.enterprise.rag.document;

import com.enterprise.rag.document.chunker.DocumentChunk;
import com.enterprise.rag.document.chunker.DocumentChunker;
import com.enterprise.rag.document.parser.DocumentParser;
import com.enterprise.rag.document.parser.DocumentParserFactory;
import com.enterprise.rag.document.parser.MarkdownParser;
import com.enterprise.rag.document.processor.DocumentInput;
import com.enterprise.rag.document.processor.DocumentProcessorImpl;
import com.enterprise.rag.document.processor.ProcessResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownProcessingRegressionTest {

    private final DocumentProcessorImpl processor;

    MarkdownProcessingRegressionTest() {
        List<DocumentParser> parsers = List.of(new MarkdownParser());
        this.processor = new DocumentProcessorImpl(new DocumentParserFactory(parsers), new DocumentChunker());
    }

    @Test
    void processShouldKeepLargeMarkdownChunksWithinDefaultBudget() {
        String markdown = """
                # JWT Guide

                %s

                ## Table

                | field | value |
                | --- | --- |
                | token | %s |
                """.formatted("authentication".repeat(220), "jwt_claim_".repeat(120));

        DocumentInput input = new DocumentInput(
                new ByteArrayInputStream(markdown.getBytes(StandardCharsets.UTF_8)),
                "jwt-guide.md",
                "md",
                null);

        ProcessResult result = processor.process(input);

        assertTrue(result.isNew());
        assertFalse(result.chunks().isEmpty());
        assertTrue(result.chunks().stream().map(DocumentChunk::content).allMatch(content -> content.length() <= 500));
    }
}
