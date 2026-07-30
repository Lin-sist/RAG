package com.enterprise.rag.admin.mcp;

/** Internal stable-category exception; raw dependency details never cross the MCP boundary. */
final class McpToolExecutionException extends RuntimeException {

    McpToolExecutionException(String category) {
        super(category);
    }

    String category() {
        return getMessage();
    }
}
