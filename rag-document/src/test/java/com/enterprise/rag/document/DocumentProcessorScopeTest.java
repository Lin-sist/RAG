package com.enterprise.rag.document;

import com.enterprise.rag.document.chunker.DocumentChunker;
import com.enterprise.rag.document.parser.DocumentParser;
import com.enterprise.rag.document.parser.DocumentParserFactory;
import com.enterprise.rag.document.parser.PlainTextParser;
import com.enterprise.rag.document.processor.DocumentInput;
import com.enterprise.rag.document.processor.DocumentProcessorImpl;
import com.enterprise.rag.document.processor.ProcessResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentProcessorScopeTest {

    private final DocumentProcessorImpl processor;

    DocumentProcessorScopeTest() {
        List<DocumentParser> parsers = List.of(new PlainTextParser());
        this.processor = new DocumentProcessorImpl(new DocumentParserFactory(parsers), new DocumentChunker());
    }

    @Test
    void sameContentShouldNotBeTreatedAsDuplicateAcrossKnowledgeBases() {
        processor.clearProcessedHashes();
        String content = "JWT is a compact token format for transmitting claims.";

        ProcessResult first = processor.process(new DocumentInput(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "jwt-kb1.txt",
                "txt",
                Map.of("kbId", 1L)));

        ProcessResult second = processor.process(new DocumentInput(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "jwt-kb2.txt",
                "txt",
                Map.of("kbId", 2L)));

        assertTrue(first.isNew());
        assertTrue(second.isNew());
        assertFalse(first.chunks().isEmpty());
        assertFalse(second.chunks().isEmpty());
    }

    @Test
    void sameContentShouldStillBeDeduplicatedWithinSameKnowledgeBase() {
        processor.clearProcessedHashes();
        String content = "JWT is a compact token format for transmitting claims.";

        ProcessResult first = processor.process(new DocumentInput(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "jwt-kb1-a.txt",
                "txt",
                Map.of("kbId", 1L)));

        ProcessResult second = processor.process(new DocumentInput(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                "jwt-kb1-b.txt",
                "txt",
                Map.of("kbId", 1L)));

        assertTrue(first.isNew());
        assertFalse(second.isNew());
        assertTrue(second.chunks().isEmpty());
    }
}
