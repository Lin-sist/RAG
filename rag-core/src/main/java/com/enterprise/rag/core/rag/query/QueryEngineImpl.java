package com.enterprise.rag.core.rag.query;

import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.SearchResult;
import com.enterprise.rag.core.vectorstore.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询引擎实现
 * 实现问题向量化、相似度检索和结果重排序
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryEngineImpl implements QueryEngine {

    private static final Pattern LATIN_TOKEN_PATTERN = Pattern.compile("[\\p{Alnum}]+");
    private static final Pattern CJK_SEGMENT_PATTERN = Pattern.compile("[\\p{IsHan}]+");
    private static final Pattern TRAILING_PUNCTUATION_PATTERN = Pattern.compile("[\\s？?！!。,.，；;：:]+$");
    private static final Pattern LEADING_POLITE_PATTERN = Pattern.compile("^(请问|请教一下|请教|想问一下|想问|麻烦问下|麻烦问一下|帮我|请你|请)\\s*");
    private static final Pattern EXPLANATION_PATTERN = Pattern.compile("^(什么是|什么叫|何谓|介绍一下|讲讲|解释一下|解释下|说明一下)\\s*(.+)$");
    private static final Pattern REVERSED_EXPLANATION_PATTERN = Pattern.compile("^(.+?)\\s*(是什么|是啥|指什么|是什么意思)$");
    private static final Map<String, List<String>> QUERY_SYNONYMS = Map.of(
            "jwt", List.of("json web token", "jwt token", "token认证"),
            "oauth", List.of("oauth 2.0", "oauth2", "授权协议"),
            "sso", List.of("single sign on", "单点登录"),
            "rbac", List.of("role based access control", "基于角色的访问控制"),
            "csrf", List.of("cross site request forgery", "跨站请求伪造"),
            "xss", List.of("cross site scripting", "跨站脚本攻击"));

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    @Override
    public List<RetrievedContext> retrieve(String query, RetrieveOptions options) {
        if (query == null || query.isBlank()) {
            log.warn("Empty query received, returning empty results");
            return List.of();
        }

        log.debug("Retrieving contexts for query: '{}' from collection: {}",
                truncateForLog(query), options.collectionName());

        // 1. 构建搜索选项
        SearchOptions searchOptions = new SearchOptions(
                options.topK(),
                options.minScore(),
                options.filter());

        // 2. 对口语化/缩写问题生成少量检索变体，并合并多路召回结果
        List<QueryVariant> queryVariants = buildQueryVariants(query);
        List<RetrievedContext> contexts = mergeRetrievedContexts(queryVariants, options.collectionName(), searchOptions);
        log.debug("Merged {} contexts from {} query variants", contexts.size(), queryVariants.size());

        // 3. 如果启用重排序，执行重排序
        if (options.enableRerank() && !contexts.isEmpty()) {
            contexts = rerank(query, contexts);
            log.debug("Reranked {} contexts", contexts.size());
        }

        return contexts.stream()
                .limit(options.topK())
                .toList();
    }

    /**
     * 将搜索结果转换为检索上下文
     */
    private RetrievedContext toRetrievedContext(SearchResult result) {
        return new RetrievedContext(
                result.content(),
                result.id(),
                result.score(),
                result.metadata());
    }

    private List<RetrievedContext> mergeRetrievedContexts(List<QueryVariant> queryVariants,
            String collectionName,
            SearchOptions searchOptions) {
        Map<String, RetrievedContext> merged = new LinkedHashMap<>();

        for (QueryVariant queryVariant : queryVariants) {
            float[] queryVector = embeddingService.embed(queryVariant.query());
            log.debug("Query variant embedded: '{}' weight={}", truncateForLog(queryVariant.query()), queryVariant.weight());

            List<SearchResult> searchResults = vectorStore.search(collectionName, queryVector, searchOptions);
            log.debug("Vector search returned {} results for variant '{}'", searchResults.size(),
                    truncateForLog(queryVariant.query()));

            for (SearchResult searchResult : searchResults) {
                RetrievedContext context = toRetrievedContext(searchResult);
                String dedupKey = buildResultKey(context);
                float weightedScore = context.relevanceScore() * queryVariant.weight();

                RetrievedContext existing = merged.get(dedupKey);
                if (existing == null || weightedScore > existing.relevanceScore()) {
                    merged.put(dedupKey, new RetrievedContext(
                            context.content(),
                            context.source(),
                            weightedScore,
                            context.metadata()));
                }
            }
        }

        return merged.values().stream()
                .sorted(Comparator.comparingDouble(RetrievedContext::relevanceScore).reversed())
                .toList();
    }

    /**
     * 重排序检索结果
     * 使用简单的基于关键词匹配的重排序策略
     * 可以扩展为使用专门的重排序模型（如 BGE-Reranker）
     */
    private List<RetrievedContext> rerank(String query, List<RetrievedContext> contexts) {
        if (contexts.size() <= 1) {
            return contexts;
        }

        // 提取查询关键词：同时兼容英文词和中文连续文本
        List<String> queryTerms = extractQueryTerms(query);

        // 计算每个上下文的重排序分数
        List<ScoredContext> scoredContexts = new ArrayList<>();
        for (RetrievedContext context : contexts) {
            float rerankScore = calculateRerankScore(context, queryTerms);
            scoredContexts.add(new ScoredContext(context, rerankScore));
        }

        // 按重排序分数降序排列
        return scoredContexts.stream()
                .sorted(Comparator.comparingDouble(ScoredContext::score).reversed())
                .map(ScoredContext::context)
                .toList();
    }

    /**
     * 计算重排序分数
     * 综合考虑原始相似度分数和关键词匹配度
     */
    private float calculateRerankScore(RetrievedContext context, List<String> queryTerms) {
        String contentLower = context.content() == null ? "" : context.content().toLowerCase();

        // 计算关键词匹配度
        int matchCount = 0;
        for (String term : queryTerms) {
            if (!term.isBlank() && contentLower.contains(term)) {
                matchCount++;
            }
        }
        float keywordScore = !queryTerms.isEmpty()
                ? (float) matchCount / queryTerms.size()
                : 0f;

        // 综合分数 = 原始分数 * 0.7 + 关键词分数 * 0.3
        return context.relevanceScore() * 0.7f + keywordScore * 0.3f;
    }

    /**
     * 截断日志输出
     */
    private String truncateForLog(String text) {
        if (text == null)
            return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    private List<QueryVariant> buildQueryVariants(String query) {
        LinkedHashMap<String, Float> variants = new LinkedHashMap<>();
        addQueryVariant(variants, query, 1.0f);

        String normalized = normalizeQuery(query);
        if (!normalized.equals(query)) {
            addQueryVariant(variants, normalized, 0.99f);
        }

        String conversationalCore = stripConversationalNoise(normalized);
        if (!conversationalCore.equals(normalized)) {
            addQueryVariant(variants, conversationalCore, 0.98f);
        }

        for (String token : extractLatinTokens(conversationalCore)) {
            List<String> expansions = QUERY_SYNONYMS.get(token);
            if (expansions != null) {
                for (String expansion : expansions) {
                    addQueryVariant(variants, expansion, 0.96f);
                    addQueryVariant(variants, token + " " + expansion, 0.97f);
                }
            }
        }

        return variants.entrySet().stream()
                .map(entry -> new QueryVariant(entry.getKey(), entry.getValue()))
                .toList();
    }

    private void addQueryVariant(Map<String, Float> variants, String candidate, float weight) {
        String normalized = normalizeQuery(candidate);
        if (normalized.isBlank()) {
            return;
        }
        variants.merge(normalized, weight, Math::max);
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

    private List<String> extractLatinTokens(String query) {
        List<String> tokens = new ArrayList<>();
        Matcher latinMatcher = LATIN_TOKEN_PATTERN.matcher(query == null ? "" : query.toLowerCase(Locale.ROOT));
        while (latinMatcher.find()) {
            String token = latinMatcher.group().trim();
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String buildResultKey(RetrievedContext context) {
        if (context.source() != null && !context.source().isBlank()) {
            return "source:" + context.source();
        }
        return "content:" + (context.content() == null ? "" : context.content().trim().toLowerCase(Locale.ROOT));
    }

    /**
     * 查询分词：
     * 1) 英文/数字按词提取（长度 >= 2）
     * 2) 中文按连续片段提取，并补充 2-gram，增强中文短词命中率
     */
    private List<String> extractQueryTerms(String query) {
        Set<String> terms = new LinkedHashSet<>();
        String normalized = query == null ? "" : query.toLowerCase();

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

        return new ArrayList<>(terms);
    }

    /**
     * 带分数的上下文记录
     */
    private record ScoredContext(RetrievedContext context, float score) {
    }

    private record QueryVariant(String query, float weight) {
    }
}
