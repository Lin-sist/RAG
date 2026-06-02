package com.enterprise.rag.core.rag.citation;

import com.enterprise.rag.core.rag.model.Citation;

import java.util.List;
import java.util.Map;

/**
 * Citation 校验结果。
 */
public record CitationValidationResult(
        List<ValidatedCitation> validCitations,
        List<Citation> droppedCitations,
        double citationCoverage) {

    public List<Citation> citations() {
        return validCitations.stream()
                .map(ValidatedCitation::citation)
                .toList();
    }

    public Map<String, Object> metadata() {
        return Map.of(
                "validCitations", validCitations.size(),
                "droppedCitations", droppedCitations.size(),
                "citationCoverage", citationCoverage);
    }
}
