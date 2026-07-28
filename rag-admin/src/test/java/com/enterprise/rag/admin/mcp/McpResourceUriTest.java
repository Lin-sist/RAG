package com.enterprise.rag.admin.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpResourceUriTest {

    @Test
    void knowledgeBaseUriParsesAndFormatsCanonically() {
        McpResourceUri parsed = McpResourceUri.parse("rag://knowledge-bases/42");

        assertEquals(new McpResourceUri.KnowledgeBase(42L), parsed);
        assertEquals("rag://knowledge-bases/42", parsed.uri());
        assertEquals(parsed, McpResourceUri.parse(parsed.uri()));
    }

    @Test
    void documentUriParsesAndFormatsCanonically() {
        McpResourceUri parsed = McpResourceUri.parse(
                "rag://knowledge-bases/42/documents/73");

        assertEquals(new McpResourceUri.Document(42L, 73L), parsed);
        assertEquals("rag://knowledge-bases/42/documents/73", parsed.uri());
        assertEquals(parsed, McpResourceUri.parse(parsed.uri()));
    }

    @Test
    void chunkUriAllowsZeroIndexAndFormatsCanonically() {
        McpResourceUri parsed = McpResourceUri.parse(
                "rag://knowledge-bases/42/documents/73/chunks/0");

        assertEquals(new McpResourceUri.Chunk(42L, 73L, 0), parsed);
        assertEquals("rag://knowledge-bases/42/documents/73/chunks/0", parsed.uri());
        assertEquals(parsed, McpResourceUri.parse(parsed.uri()));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {
            "rag://knowledge-bases/42?tenantId=9",
            "rag://knowledge-bases/42#fragment",
            "rag://attacker@knowledge-bases/42",
            "rag://knowledge-bases:443/42",
            "rag://knowledge-bases/42%2Fdocuments%2F73",
            "rag://knowledge-bases/42%252Fdocuments%252F73",
            "rag://knowledge-bases/42/../43",
            "rag://knowledge-bases//42",
            "rag://knowledge-bases/-1",
            "rag://knowledge-bases/+1",
            "rag://knowledge-bases/0",
            "rag://knowledge-bases/042",
            "rag://knowledge-bases/1e3",
            "rag://knowledge-bases/9223372036854775808",
            "rag://knowledge-bases/42/extra",
            "RAG://knowledge-bases/42",
            "rag://Knowledge-Bases/42",
            "rag://knowledge-bases/42/documents/0",
            "rag://knowledge-bases/42/documents/-1",
            "rag://knowledge-bases/42/documents/73/extra",
            "rag://knowledge-bases/42/documents/73/chunks/-1",
            "rag://knowledge-bases/42/documents/73/chunks/01",
            "rag://knowledge-bases/42/documents/73/chunks/2147483648",
            "rag://knowledge-bases/42/documents/73/chunks/0?version=1",
            "rag://knowledge-bases/42/documents/73/versions/1",
            " rag://knowledge-bases/42"
    })
    void nonCanonicalOrMalformedUrisFailClosedWithoutEchoingInput(String rawUri) {
        McpResourceUri.InvalidResourceUriException error = assertThrows(
                McpResourceUri.InvalidResourceUriException.class,
                () -> McpResourceUri.parse(rawUri));

        assertEquals("Invalid MCP resource URI", error.getMessage());
        if (rawUri != null && !rawUri.isEmpty()) {
            assertFalse(error.getMessage().contains(rawUri));
        }
    }
}
