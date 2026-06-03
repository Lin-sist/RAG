package com.enterprise.rag.core.rag.citation;

import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 只接受能回连到本轮 retrieved contexts 的引用。
 */
@Component
public class CitationValidator {
    private static final double MIN_TOKEN_OVERLAP = 0.58d;
    private static final Pattern LATIN_TOKEN_PATTERN = Pattern.compile("[\\p{Alnum}]+");
    private static final Pattern CJK_SEGMENT_PATTERN = Pattern.compile("[\\p{IsHan}]+");

    public CitationValidationResult validate(List<Citation> citations, List<RetrievedContext> contexts) {
        return validate(citations, contexts, false);
    }

    public CitationValidationResult validate(List<Citation> citations, List<RetrievedContext> contexts,
            boolean noAnswer) {
        List<Citation> input = citations == null ? List.of() : citations;
        if (input.isEmpty()) {
            return new CitationValidationResult(List.of(), List.of(), 1.0d);
        }

        if (noAnswer || contexts == null || contexts.isEmpty()) {
            return new CitationValidationResult(List.of(), input, 0.0d);
        }

        List<ValidatedCitation> valid = new ArrayList<>();
        List<Citation> dropped = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (Citation citation : input) {
            EvidenceMatch match = findEvidence(citation, contexts);
            if (match == null) {
                dropped.add(citation);
                continue;
            }

            String dedupKey = dedupKey(match);
            if (!seen.add(dedupKey)) {
                dropped.add(citation);
                continue;
            }

            Citation groundedCitation = Citation.grounded(
                    match.source(),
                    match.sourceFileName(),
                    match.documentTitle(),
                    match.documentId(),
                    match.chunkId(),
                    match.score(),
                    match.snippet(),
                    citation.startIndex(),
                    citation.endIndex());
            valid.add(new ValidatedCitation(groundedCitation, match));
        }

        double coverage = input.isEmpty() ? 1.0d : (double) valid.size() / (double) input.size();
        return new CitationValidationResult(List.copyOf(valid), List.copyOf(dropped), coverage);
    }

    private EvidenceMatch findEvidence(Citation citation, List<RetrievedContext> contexts) {
        for (int i = 0; i < contexts.size(); i++) {
            RetrievedContext context = contexts.get(i);
            if (!matchesIdentity(citation, context)) {
                continue;
            }

            SnippetMatch snippetMatch = matchSnippet(citation.snippet(), context.content());
            if (snippetMatch == null) {
                continue;
            }

            return new EvidenceMatch(
                    context.source(),
                    firstMetadataText(context.metadata(), "sourceFileName", "originalFilename", "fileName", "filename"),
                    firstMetadataText(context.metadata(), "documentTitle", "title"),
                    metadataLong(context.metadata(), "documentId"),
                    chunkId(context),
                    (double) context.relevanceScore(),
                    snippetMatch.snippet(),
                    snippetMatch.matchType(),
                    snippetMatch.overlapRatio(),
                    i + 1);
        }
        return null;
    }

    private boolean matchesIdentity(Citation citation, RetrievedContext context) {
        String citationChunkId = normalize(citation.chunkId());
        if (!citationChunkId.isBlank()) {
            return citationChunkId.equals(normalize(chunkId(context)));
        }

        Long citationDocumentId = citation.documentId();
        if (citationDocumentId != null) {
            Long contextDocumentId = metadataLong(context.metadata(), "documentId");
            return citationDocumentId.equals(contextDocumentId);
        }

        String citationSource = normalize(citation.source());
        if (!citationSource.isBlank() && sourceCandidates(context).contains(citationSource)) {
            return true;
        }

        String citationSourceFileName = normalize(citation.sourceFileName());
        if (!citationSourceFileName.isBlank() && sourceCandidates(context).contains(citationSourceFileName)) {
            return true;
        }

        String citationDocumentTitle = normalize(citation.documentTitle());
        return !citationDocumentTitle.isBlank() && sourceCandidates(context).contains(citationDocumentTitle);
    }

    private SnippetMatch matchSnippet(String snippet, String content) {
        String normalizedSnippet = normalizeText(snippet);
        String normalizedContent = normalizeText(content);
        if (normalizedSnippet.isBlank() || normalizedContent.isBlank()) {
            return null;
        }

        if (normalizedContent.contains(normalizedSnippet)) {
            return new SnippetMatch(snippet.trim(), "exact", 1.0d);
        }

        Set<String> snippetTokens = tokens(normalizedSnippet);
        Set<String> contentTokens = tokens(normalizedContent);
        if (snippetTokens.isEmpty() || contentTokens.isEmpty()) {
            return null;
        }

        long overlap = snippetTokens.stream().filter(contentTokens::contains).count();
        double ratio = (double) overlap / (double) snippetTokens.size();
        if (ratio >= MIN_TOKEN_OVERLAP) {
            return new SnippetMatch(snippet.trim(), "token_overlap", ratio);
        }
        return null;
    }

    private String chunkId(RetrievedContext context) {
        return context == null ? null : context.source();
    }

    private Long metadataLong(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String firstMetadataText(Map<String, Object> metadata, String... keys) {
        if (metadata == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private Set<String> sourceCandidates(RetrievedContext context) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, context == null ? null : context.source());
        if (context != null) {
            addMetadataCandidates(candidates, context.metadata(), "source", "sourceFileName",
                    "originalFilename", "fileName", "filename", "documentTitle", "title");
        }
        return candidates;
    }

    private void addMetadataCandidates(Set<String> candidates, Map<String, Object> metadata, String... keys) {
        if (metadata == null || keys == null) {
            return;
        }
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null) {
                addCandidate(candidates, String.valueOf(value));
            }
        }
    }

    private void addCandidate(Set<String> candidates, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            candidates.add(normalized);
        }
    }

    private Set<String> tokens(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();

        Matcher latinMatcher = LATIN_TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (latinMatcher.find()) {
            String token = latinMatcher.group();
            if (token.length() >= 2) {
                result.add(token);
            }
        }

        Matcher cjkMatcher = CJK_SEGMENT_PATTERN.matcher(text);
        while (cjkMatcher.find()) {
            String segment = cjkMatcher.group();
            if (segment.length() == 1) {
                result.add(segment);
                continue;
            }
            for (int i = 0; i < segment.length() - 1; i++) {
                result.add(segment.substring(i, i + 2));
            }
        }

        return result;
    }

    private String dedupKey(EvidenceMatch match) {
        return normalize(match.source()) + "|"
                + normalize(match.chunkId()) + "|"
                + normalizeText(match.snippet());
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private record SnippetMatch(String snippet, String matchType, double overlapRatio) {
    }
}
