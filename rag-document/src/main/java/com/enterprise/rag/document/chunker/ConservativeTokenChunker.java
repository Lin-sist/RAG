package com.enterprise.rag.document.chunker;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConservativeTokenChunker implements TokenChunker {

    private final TokenCounter tokenCounter;

    public ConservativeTokenChunker(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }

    @Override
    public List<TokenSpan> split(String text, int basePosition, int maxTokens, int overlapTokens) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        int safeMaxTokens = Math.max(1, maxTokens);
        int safeOverlapTokens = Math.max(0, Math.min(overlapTokens, safeMaxTokens - 1));
        List<TokenSpan> spans = new ArrayList<>();
        int position = 0;

        while (position < text.length()) {
            int end = findEndWithinBudget(text, position, safeMaxTokens);
            String content = text.substring(position, end);
            spans.add(new TokenSpan(content, basePosition + position, basePosition + end, tokenCounter.count(content)));

            if (end >= text.length()) {
                break;
            }
            position = findOverlapStart(text, position, end, safeOverlapTokens);
        }

        return spans;
    }

    private int findEndWithinBudget(String text, int start, int maxTokens) {
        int end = start + 1;
        while (end <= text.length()) {
            String candidate = text.substring(start, end);
            if (tokenCounter.count(candidate) > maxTokens) {
                return Math.max(start + 1, end - 1);
            }
            end++;
        }
        return text.length();
    }

    private int findOverlapStart(String text, int start, int end, int overlapTokens) {
        if (overlapTokens <= 0) {
            return end;
        }

        int overlapStart = end;
        while (overlapStart > start) {
            String candidate = text.substring(overlapStart - 1, end);
            if (tokenCounter.count(candidate) > overlapTokens) {
                break;
            }
            overlapStart--;
        }
        return Math.max(start + 1, overlapStart);
    }
}
