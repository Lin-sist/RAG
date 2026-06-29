package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Existing keyword heuristic reranker, kept as the safe default fallback.
 */
@Component
public class HeuristicReranker implements Reranker {

    private static final Pattern LATIN_TOKEN_PATTERN = Pattern.compile("[\\p{Alnum}]+");
    private static final Pattern CJK_SEGMENT_PATTERN = Pattern.compile("[\\p{IsHan}]+");
    private static final Pattern TRAILING_PUNCTUATION_PATTERN = Pattern.compile("[\\s？?！!。,.，；;：:]+$");
    private static final Pattern LEADING_POLITE_PATTERN = Pattern.compile(
            "^(请问|请教一下|请教|想问一下|想问|麻烦问下|麻烦问一下|帮我|请你|请|你认为|你觉得|你看|可以说说|说说|聊聊|分析一下|分析下|帮忙分析一下|帮忙分析下)\\s*");
    private static final Pattern EXPLANATION_PATTERN = Pattern.compile("^(什么是|什么叫|何谓|介绍一下|讲讲|解释一下|解释下|说明一下)\\s*(.+)$");
    private static final Pattern REVERSED_EXPLANATION_PATTERN = Pattern.compile("^(.+?)\\s*(是什么|是啥|指什么|是什么意思)$");
    private static final Pattern HOW_QUERY_PATTERN = Pattern.compile("^(.+?)\\s*(?:是)?如何(?:运作|工作|运行|实现|发挥作用)的?$");
    private static final Pattern HOW_QUERY_ALT_PATTERN = Pattern.compile("^(.+?)\\s*(?:是)?怎么(?:运作|工作|运行|实现|发挥作用)的?$");
    private static final Pattern WHY_QUERY_PATTERN = Pattern.compile("^为什么(?:需要|要|会)?\\s*(.+)$");
    private static final Pattern WHY_SUBJECT_PATTERN = Pattern.compile("^(.+?)\\s*为什么(?:重要|需要|有用)$");
    private static final Pattern PRINCIPLE_QUERY_PATTERN = Pattern.compile(
            "^(.+?)\\s*(?:的)?(?:工作原理|运行原理|原理|机制|流程|作用|实现方式)(?:是什么)?$");

    @Override
    public String provider() {
        return "heuristic";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public List<RetrievedContext> rerank(String query, List<RetrievedContext> contexts) {
        if (contexts == null || contexts.size() <= 1) {
            return contexts == null ? List.of() : contexts;
        }

        List<String> queryTerms = extractQueryTerms(query);
        List<ScoredContext> scoredContexts = new ArrayList<>();
        for (RetrievedContext context : contexts) {
            float rerankScore = calculateRerankScore(context, queryTerms);
            scoredContexts.add(new ScoredContext(context, rerankScore));
        }

        return scoredContexts.stream()
                .sorted(Comparator.comparingDouble(ScoredContext::score).reversed())
                .map(ScoredContext::context)
                .toList();
    }

    private float calculateRerankScore(RetrievedContext context, List<String> queryTerms) {
        String contentLower = context.content() == null ? "" : context.content().toLowerCase(Locale.ROOT);

        int matchCount = 0;
        for (String term : queryTerms) {
            if (!term.isBlank() && contentLower.contains(term)) {
                matchCount++;
            }
        }
        float keywordScore = !queryTerms.isEmpty()
                ? (float) matchCount / queryTerms.size()
                : 0f;

        return context.relevanceScore() * 0.7f + keywordScore * 0.3f;
    }

    private List<String> extractQueryTerms(String query) {
        Set<String> terms = new LinkedHashSet<>();
        String normalized = stripConversationalNoise(query == null ? "" : query).toLowerCase(Locale.ROOT);

        Matcher latinMatcher = LATIN_TOKEN_PATTERN.matcher(normalized);
        while (latinMatcher.find()) {
            String token = latinMatcher.group().trim();
            if (token.length() >= 2) {
                terms.add(token);
            }
        }

        Matcher cjkMatcher = CJK_SEGMENT_PATTERN.matcher(normalized);
        while (cjkMatcher.find()) {
            String segment = cjkMatcher.group().trim();
            if (segment.isEmpty()) {
                continue;
            }

            if (segment.length() >= 2) {
                terms.add(segment);
                for (int i = 0; i < segment.length() - 1; i++) {
                    terms.add(segment.substring(i, i + 2));
                }
            } else {
                terms.add(segment);
            }
        }

        ExplanationIntent intent = parseExplanationIntent(normalized);
        if (intent != null) {
            addExplanationTerms(terms, intent);
        }

        return new ArrayList<>(terms);
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return TRAILING_PUNCTUATION_PATTERN.matcher(query.trim()).replaceAll("").replaceAll("\\s+", " ");
    }

    private String stripConversationalNoise(String query) {
        String stripped = normalizeQuery(query);
        stripped = LEADING_POLITE_PATTERN.matcher(stripped).replaceFirst("");

        Matcher explanationMatcher = EXPLANATION_PATTERN.matcher(stripped);
        if (explanationMatcher.matches()) {
            return normalizeQuery(explanationMatcher.group(2));
        }

        Matcher reversedMatcher = REVERSED_EXPLANATION_PATTERN.matcher(stripped);
        if (reversedMatcher.matches()) {
            return normalizeQuery(reversedMatcher.group(1));
        }

        return stripped;
    }

    private ExplanationIntent parseExplanationIntent(String query) {
        String normalized = normalizeQuery(query);
        if (normalized.isBlank()) {
            return null;
        }

        String subject = matchSubject(HOW_QUERY_PATTERN, normalized);
        if (subject != null) {
            return new ExplanationIntent(ExplanationType.HOW, subject);
        }

        subject = matchSubject(HOW_QUERY_ALT_PATTERN, normalized);
        if (subject != null) {
            return new ExplanationIntent(ExplanationType.HOW, subject);
        }

        subject = matchSubject(WHY_QUERY_PATTERN, normalized);
        if (subject != null) {
            return new ExplanationIntent(ExplanationType.WHY, subject);
        }

        subject = matchSubject(WHY_SUBJECT_PATTERN, normalized);
        if (subject != null) {
            return new ExplanationIntent(ExplanationType.WHY, subject);
        }

        subject = matchSubject(PRINCIPLE_QUERY_PATTERN, normalized);
        if (subject != null) {
            return new ExplanationIntent(ExplanationType.GENERAL, subject);
        }

        if (containsExplanationCue(normalized)) {
            return new ExplanationIntent(ExplanationType.GENERAL, normalized);
        }

        return null;
    }

    private String matchSubject(Pattern pattern, String query) {
        Matcher matcher = pattern.matcher(query);
        if (!matcher.matches()) {
            return null;
        }
        return normalizeQuery(matcher.group(1));
    }

    private boolean containsExplanationCue(String query) {
        return query.contains("如何")
                || query.contains("怎么")
                || query.contains("为什么")
                || query.contains("原理")
                || query.contains("机制")
                || query.contains("流程")
                || query.contains("运作")
                || query.contains("工作")
                || query.contains("运行");
    }

    private void addExplanationTerms(Set<String> terms, ExplanationIntent intent) {
        String subject = intent.subject().toLowerCase(Locale.ROOT);
        if (!subject.isBlank()) {
            terms.add(subject);
        }

        switch (intent.type()) {
            case HOW -> {
                terms.add("原理");
                terms.add("流程");
                terms.add("机制");
            }
            case WHY -> {
                terms.add("作用");
                terms.add("目的");
                terms.add("原因");
            }
            case GENERAL -> {
                terms.add("原理");
                terms.add("机制");
            }
        }
    }

    private record ScoredContext(RetrievedContext context, float score) {
    }

    private record ExplanationIntent(ExplanationType type, String subject) {
    }

    private enum ExplanationType {
        HOW,
        WHY,
        GENERAL
    }
}
