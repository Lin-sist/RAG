package com.enterprise.rag.document.chunker;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentChunkerRegressionTest {

    private final DocumentChunker chunker = new DocumentChunker();

    @Test
    void semanticChunkingShouldSplitOversizedMarkdownParagraphWithoutSentenceBoundary() {
        String paragraph = "# JWT\n\n" + "tokenflow".repeat(180);
        ChunkConfig config = ChunkConfig.semantic(120, 20);

        List<DocumentChunk> chunks = chunker.chunk(paragraph, config);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.content().length() <= config.chunkSize()));
        assertEquals(0, chunks.get(0).startIndex());
        assertTrue(chunks.get(chunks.size() - 1).endIndex() <= paragraph.length());
    }

    @Test
    void semanticChunkingShouldSplitLongMarkdownTableRowsIntoSafeChunks() {
        String longCell = "jwt_authentication_column_".repeat(40);
        String markdown = """
                | Column | Description |
                | --- | --- |
                | token | %s |
                | expires | %s |
                """.formatted(longCell, longCell);
        ChunkConfig config = ChunkConfig.semantic(140, 20);

        List<DocumentChunk> chunks = chunker.chunk(markdown, config);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().allMatch(chunk -> chunk.content().length() <= config.chunkSize()));
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).metadata().get("chunkIndex"));
        }
    }

    @Test
    void semanticChunkingShouldSplitVeryLongMarkdownCodeLines() {
        String longCodeLine = "constJwtSecret=" + "\"x\"".repeat(220) + ";";
        String markdown = """
                ## Sample

                ```js
                %s
                ```
                """.formatted(longCodeLine);
        ChunkConfig config = ChunkConfig.semantic(100, 10);

        List<DocumentChunk> chunks = chunker.chunk(markdown, config);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.content().length() <= config.chunkSize()));
    }
}
