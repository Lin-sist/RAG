package com.enterprise.rag.core.rag.router;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;

@Component
public final class DeterministicFactIntentClassifier {

    static final int MAX_QUERY_LENGTH = 512;

    private static final List<String> MULTI_HOP_CUES = List.of(
            "比较", "共同点", "区别", "差异", "综合", "分别");
    private static final List<String> GLOBAL_CUES = List.of(
            "所有文档", "全部文档", "全库", "整体总结", "总结全部", "总结所有", "全面分析");
    private static final List<String> HIGH_RISK_CUES = List.of(
            "投资建议", "医疗诊断", "诊断建议", "用药建议", "法律意见", "法律建议");
    private static final List<String> DEFINITION_PREFIXES = List.of("什么是", "什么叫", "何谓");
    private static final List<String> FACT_PREFIXES = List.of("谁是", "何时", "什么时候", "哪里", "多少", "哪一年");
    private static final List<String> FACT_SUFFIXES = List.of(
            "是什么", "是指什么", "是谁", "何时", "什么时候", "在哪里", "多少", "哪一年");

    public QueryClassification classify(String query) {
        if (query == null || query.isBlank() || query.length() > MAX_QUERY_LENGTH || containsControlCharacter(query)) {
            return new QueryClassification(QueryIntent.INVALID, QueryRouteReason.INVALID_INPUT);
        }
        String normalized = normalize(query);
        if (containsAny(normalized, MULTI_HOP_CUES)) {
            return new QueryClassification(QueryIntent.UNSUPPORTED, QueryRouteReason.MULTI_HOP_CUE);
        }
        if (containsAny(normalized, GLOBAL_CUES)) {
            return new QueryClassification(QueryIntent.UNSUPPORTED, QueryRouteReason.GLOBAL_CUE);
        }
        if (containsAny(normalized, HIGH_RISK_CUES)) {
            return new QueryClassification(QueryIntent.UNSUPPORTED, QueryRouteReason.HIGH_RISK_CUE);
        }
        if (startsWithAny(normalized, DEFINITION_PREFIXES)) {
            return new QueryClassification(QueryIntent.FACT, QueryRouteReason.DEFINITION_CUE);
        }
        if (startsWithAny(normalized, FACT_PREFIXES) || endsWithAny(stripTrailingPunctuation(normalized), FACT_SUFFIXES)) {
            return new QueryClassification(QueryIntent.FACT, QueryRouteReason.FACT_LOOKUP_CUE);
        }
        return new QueryClassification(QueryIntent.UNSUPPORTED, QueryRouteReason.AMBIGUOUS);
    }

    private String normalize(String query) {
        return Normalizer.normalize(query, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ");
    }

    private boolean containsControlCharacter(String query) {
        return query.chars().anyMatch(Character::isISOControl);
    }

    private boolean containsAny(String query, List<String> cues) {
        return cues.stream().anyMatch(query::contains);
    }

    private boolean startsWithAny(String query, List<String> cues) {
        return cues.stream().anyMatch(query::startsWith);
    }

    private boolean endsWithAny(String query, List<String> cues) {
        return cues.stream().anyMatch(query::endsWith);
    }

    private String stripTrailingPunctuation(String query) {
        int end = query.length();
        while (end > 0 && "?？!！。.".indexOf(query.charAt(end - 1)) >= 0) {
            end--;
        }
        return query.substring(0, end).trim();
    }
}
