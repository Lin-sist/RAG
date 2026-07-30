package com.enterprise.rag.admin.mcp;

import java.nio.charset.StandardCharsets;

/** Produces a UTF-8 byte-bounded prefix without splitting a Unicode code point. */
final class McpUtf8Bounder {

    private McpUtf8Bounder() {
    }

    static BoundedText bound(String text, int maxBytes) {
        if (text == null || maxBytes <= 0) {
            throw new IllegalArgumentException("Invalid bounded text input");
        }
        if (text.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return new BoundedText(text, false);
        }
        int end = 0;
        int bytes = 0;
        while (end < text.length()) {
            int codePoint = text.codePointAt(end);
            int codePointBytes = utf8Length(codePoint);
            if (bytes + codePointBytes > maxBytes) {
                break;
            }
            bytes += codePointBytes;
            end += Character.charCount(codePoint);
        }
        return new BoundedText(text.substring(0, end), true);
    }

    private static int utf8Length(int codePoint) {
        if (codePoint <= 0x7F) {
            return 1;
        }
        if (codePoint <= 0x7FF) {
            return 2;
        }
        return codePoint <= 0xFFFF ? 3 : 4;
    }

    record BoundedText(String text, boolean truncated) {
    }
}
