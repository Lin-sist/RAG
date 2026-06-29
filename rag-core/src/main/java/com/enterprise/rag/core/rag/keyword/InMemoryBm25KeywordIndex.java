package com.enterprise.rag.core.rag.keyword;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight BM25 keyword index. It avoids a new external dependency while
 * preserving a true sparse retrieval route for RRF fusion.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "retrieval.keyword", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InMemoryBm25KeywordIndex implements KeywordIndex {

    private static final Pattern LATIN_TOKEN_PATTERN = Pattern.compile("[\\p{Alnum}]+");
    private static final Pattern CJK_SEGMENT_PATTERN = Pattern.compile("[\\p{IsHan}]+");
    private static final double K1 = 1.5d;
    private static final double B = 0.75d;

    private final Map<String, CollectionIndex> collections = new ConcurrentHashMap<>();

    @Override
    public void upsert(String collectionName, List<KeywordDocument> documents) {
        if (collectionName == null || collectionName.isBlank() || documents == null || documents.isEmpty()) {
            return;
        }
        CollectionIndex index = collections.computeIfAbsent(collectionName, ignored -> new CollectionIndex());
        synchronized (index) {
            for (KeywordDocument document : documents) {
                if (document != null && document.isValid()) {
                    index.put(document);
                }
            }
            index.recalculate();
        }
        log.info("BM25 keyword index upserted: collection={}, docs={}", collectionName, documents.size());
    }

    @Override
    public void rebuildCollection(String collectionName, List<KeywordDocument> documents) {
        if (collectionName == null || collectionName.isBlank()) {
            return;
        }
        CollectionIndex index = new CollectionIndex();
        if (documents != null) {
            for (KeywordDocument document : documents) {
                if (document != null && document.isValid()) {
                    index.put(document);
                }
            }
        }
        synchronized (index) {
            index.recalculate();
        }
        collections.put(collectionName, index);
        log.info("BM25 keyword index rebuilt: collection={}, docs={}", collectionName, index.documents.size());
    }

    @Override
    public void delete(String collectionName, List<String> ids) {
        if (collectionName == null || ids == null || ids.isEmpty()) {
            return;
        }
        CollectionIndex index = collections.get(collectionName);
        if (index == null) {
            return;
        }
        synchronized (index) {
            for (String id : ids) {
                if (id != null) {
                    index.documents.remove(id);
                }
            }
            index.recalculate();
        }
        log.info("BM25 keyword index deleted: collection={}, ids={}", collectionName, ids.size());
    }

    @Override
    public void dropCollection(String collectionName) {
        if (collectionName != null) {
            collections.remove(collectionName);
            log.info("BM25 keyword index dropped: collection={}", collectionName);
        }
    }

    @Override
    public List<RetrievedContext> search(String collectionName, String query, int topK, Map<String, Object> filter) {
        if (collectionName == null || query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }

        CollectionIndex index = collections.get(collectionName);
        if (index == null || index.documents.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> queryTerms = termFrequencies(tokenize(query));
        if (queryTerms.isEmpty()) {
            return List.of();
        }

        List<ScoredKeywordDocument> scored = new ArrayList<>();
        synchronized (index) {
            for (IndexedKeywordDocument document : index.documents.values()) {
                if (!matchesFilter(document.metadata(), filter)) {
                    continue;
                }
                double score = bm25Score(index, document, queryTerms);
                if (score > 0d) {
                    scored.add(new ScoredKeywordDocument(document, score));
                }
            }
        }

        if (scored.isEmpty()) {
            return List.of();
        }

        scored.sort(Comparator.comparingDouble(ScoredKeywordDocument::score).reversed());
        double maxScore = scored.get(0).score();

        return scored.stream()
                .limit(topK)
                .map(item -> toRetrievedContext(item.document(), normalizedScore(item.score(), maxScore)))
                .toList();
    }

    private double bm25Score(CollectionIndex index, IndexedKeywordDocument document, Map<String, Integer> queryTerms) {
        double score = 0d;
        double avgDocLength = index.averageDocumentLength <= 0d ? 1d : index.averageDocumentLength;
        double docLength = Math.max(1, document.length());

        for (String term : queryTerms.keySet()) {
            Integer termFrequency = document.termFrequencies().get(term);
            if (termFrequency == null || termFrequency <= 0) {
                continue;
            }

            int documentFrequency = index.documentFrequencies.getOrDefault(term, 0);
            if (documentFrequency <= 0) {
                continue;
            }

            double idf = Math.log(1d + (index.documents.size() - documentFrequency + 0.5d) / (documentFrequency + 0.5d));
            double numerator = termFrequency * (K1 + 1d);
            double denominator = termFrequency + K1 * (1d - B + B * docLength / avgDocLength);
            score += idf * numerator / denominator;
        }

        return score;
    }

    private RetrievedContext toRetrievedContext(IndexedKeywordDocument document, float normalizedScore) {
        return new RetrievedContext(
                document.content(),
                document.id(),
                normalizedScore,
                document.metadata());
    }

    private float normalizedScore(double score, double maxScore) {
        if (maxScore <= 0d) {
            return 0f;
        }
        return (float) Math.max(0d, Math.min(1d, score / maxScore));
    }

    private boolean matchesFilter(Map<String, Object> metadata, Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            Object actual = metadata.get(entry.getKey());
            Object expected = entry.getValue();
            if (expected != null && actual != null && !String.valueOf(expected).equals(String.valueOf(actual))) {
                return false;
            }
            if (expected != null && actual == null) {
                return false;
            }
        }
        return true;
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        String normalized = text.toLowerCase(Locale.ROOT);

        Matcher latinMatcher = LATIN_TOKEN_PATTERN.matcher(normalized);
        while (latinMatcher.find()) {
            String token = latinMatcher.group().trim();
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }

        Matcher cjkMatcher = CJK_SEGMENT_PATTERN.matcher(normalized);
        while (cjkMatcher.find()) {
            String segment = cjkMatcher.group().trim();
            if (segment.length() >= 2) {
                tokens.add(segment);
                for (int i = 0; i < segment.length() - 1; i++) {
                    tokens.add(segment.substring(i, i + 2));
                }
            } else if (!segment.isBlank()) {
                tokens.add(segment);
            }
        }

        return tokens;
    }

    private Map<String, Integer> termFrequencies(List<String> tokens) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String token : tokens) {
            frequencies.merge(token, 1, Integer::sum);
        }
        return frequencies;
    }

    private final class CollectionIndex {
        private final Map<String, IndexedKeywordDocument> documents = new LinkedHashMap<>();
        private final Map<String, Integer> documentFrequencies = new HashMap<>();
        private double averageDocumentLength;

        private void put(KeywordDocument document) {
            Map<String, Integer> terms = termFrequencies(tokenize(document.content()));
            documents.put(document.id(), new IndexedKeywordDocument(
                    document.id(),
                    document.content(),
                    document.metadata(),
                    terms,
                    terms.values().stream().mapToInt(Integer::intValue).sum()));
        }

        private void recalculate() {
            documentFrequencies.clear();
            int totalLength = 0;
            for (IndexedKeywordDocument document : documents.values()) {
                totalLength += document.length();
                for (String term : document.termFrequencies().keySet()) {
                    documentFrequencies.merge(term, 1, Integer::sum);
                }
            }
            averageDocumentLength = documents.isEmpty() ? 0d : (double) totalLength / documents.size();
        }
    }

    private record IndexedKeywordDocument(
            String id,
            String content,
            Map<String, Object> metadata,
            Map<String, Integer> termFrequencies,
            int length) {
    }

    private record ScoredKeywordDocument(IndexedKeywordDocument document, double score) {
    }
}
