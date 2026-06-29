package com.enterprise.rag.document.chunker;

import java.util.List;

public interface TokenChunker {

    List<TokenSpan> split(String text, int basePosition, int maxTokens, int overlapTokens);

    record TokenSpan(String content, int startIndex, int endIndex, int tokenCount) {
    }
}
