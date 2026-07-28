package com.enterprise.rag.admin.mcp;

/**
 * Strict, canonical value types for C15 knowledge resources.
 */
public sealed interface McpResourceUri permits
        McpResourceUri.KnowledgeBase,
        McpResourceUri.Document,
        McpResourceUri.Chunk {

    String uri();

    static McpResourceUri parse(String rawUri) {
        if (rawUri != null && rawUri.matches("rag://knowledge-bases/[1-9][0-9]*")) {
            return new KnowledgeBase(parsePositiveLong(
                    rawUri.substring(rawUri.lastIndexOf('/') + 1)));
        }
        if (rawUri != null && rawUri.matches(
                "rag://knowledge-bases/[1-9][0-9]*/documents/[1-9][0-9]*")) {
            String[] segments = rawUri.split("/", -1);
            return new Document(parsePositiveLong(segments[3]), parsePositiveLong(segments[5]));
        }
        if (rawUri != null && rawUri.matches(
                "rag://knowledge-bases/[1-9][0-9]*/documents/[1-9][0-9]*"
                        + "/chunks/(0|[1-9][0-9]*)")) {
            String[] segments = rawUri.split("/", -1);
            return new Chunk(
                    parsePositiveLong(segments[3]),
                    parsePositiveLong(segments[5]),
                    parseNonNegativeInt(segments[7]));
        }
        throw invalid();
    }

    private static long parsePositiveLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            throw invalid();
        }
    }

    private static int parseNonNegativeInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            throw invalid();
        }
    }

    private static InvalidResourceUriException invalid() {
        return new InvalidResourceUriException();
    }

    record KnowledgeBase(long knowledgeBaseId) implements McpResourceUri {

        public KnowledgeBase {
            if (knowledgeBaseId <= 0) {
                throw invalid();
            }
        }

        @Override
        public String uri() {
            return "rag://knowledge-bases/" + knowledgeBaseId;
        }
    }

    record Document(long knowledgeBaseId, long documentId) implements McpResourceUri {

        public Document {
            if (knowledgeBaseId <= 0 || documentId <= 0) {
                throw invalid();
            }
        }

        @Override
        public String uri() {
            return "rag://knowledge-bases/" + knowledgeBaseId
                    + "/documents/" + documentId;
        }
    }

    record Chunk(long knowledgeBaseId, long documentId, int chunkIndex)
            implements McpResourceUri {

        public Chunk {
            if (knowledgeBaseId <= 0 || documentId <= 0 || chunkIndex < 0) {
                throw invalid();
            }
        }

        @Override
        public String uri() {
            return "rag://knowledge-bases/" + knowledgeBaseId
                    + "/documents/" + documentId
                    + "/chunks/" + chunkIndex;
        }
    }

    final class InvalidResourceUriException extends IllegalArgumentException {

        private InvalidResourceUriException() {
            super("Invalid MCP resource URI");
        }
    }
}
