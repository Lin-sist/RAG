package com.enterprise.rag.core.rag.query;

import com.enterprise.rag.common.trace.GenAiTelemetry;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.rag.keyword.NoOpKeywordIndex;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.rag.rerank.HeuristicReranker;
import com.enterprise.rag.core.rag.rerank.ModelReranker;
import com.enterprise.rag.core.rag.rerank.NvidiaReranker;
import com.enterprise.rag.core.rag.rerank.RerankOutcome;
import com.enterprise.rag.core.rag.rerank.RerankerRegistry;
import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.SearchResult;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class QueryEngineImpl implements QueryEngine {

    private static final Pattern LATIN_TOKEN_PATTERN = Pattern.compile("[\\p{Alnum}]+");
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
    private static final Map<String, List<String>> QUERY_SYNONYMS = Map.of(
            "jwt", List.of("json web token", "jwt token", "token认证"),
            "oauth", List.of("oauth 2.0", "oauth2", "授权协议"),
            "sso", List.of("single sign on", "单点登录"),
            "rbac", List.of("role based access control", "基于角色的访问控制"),
            "csrf", List.of("cross site request forgery", "跨站请求伪造"),
            "xss", List.of("cross site scripting", "跨站脚本攻击"));

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final KeywordIndex keywordIndex;
    private final RetrievalProperties retrievalProperties;
    private final RerankerRegistry rerankerRegistry;
    private final GenAiTelemetry telemetry;

    @Autowired
    public QueryEngineImpl(EmbeddingService embeddingService,
            VectorStore vectorStore,
            KeywordIndex keywordIndex,
            RetrievalProperties retrievalProperties,
            RerankerRegistry rerankerRegistry,
            GenAiTelemetry telemetry) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.keywordIndex = keywordIndex;
        this.retrievalProperties = retrievalProperties;
        this.rerankerRegistry = rerankerRegistry;
        this.telemetry = telemetry == null ? GenAiTelemetry.noop() : telemetry;
    }

    public QueryEngineImpl(EmbeddingService embeddingService,
            VectorStore vectorStore,
            KeywordIndex keywordIndex,
            RetrievalProperties retrievalProperties,
            RerankerRegistry rerankerRegistry) {
        this(embeddingService, vectorStore, keywordIndex, retrievalProperties,
                rerankerRegistry, GenAiTelemetry.noop());
    }

    QueryEngineImpl(EmbeddingService embeddingService, VectorStore vectorStore) {
        this(embeddingService, vectorStore, new NoOpKeywordIndex(), new RetrievalProperties());
    }

    QueryEngineImpl(EmbeddingService embeddingService,
            VectorStore vectorStore,
            KeywordIndex keywordIndex,
            RetrievalProperties retrievalProperties) {
        this(embeddingService, vectorStore, keywordIndex, retrievalProperties,
                new RerankerRegistry(
                        List.of(
                                new HeuristicReranker(),
                                new ModelReranker(retrievalProperties),
                                new NvidiaReranker(retrievalProperties)),
                        retrievalProperties));
    }

    @Override
    public List<RetrievedContext> retrieve(String query, RetrieveOptions options) {
        return retrieveWithDiagnostics(query, options).contexts();
    }

    @Override
    public RetrievalResult retrieveWithDiagnostics(String query, RetrieveOptions options) {
        if (query == null || query.isBlank()) {
            log.warn("Empty query received, returning empty results");
            return RetrievalResult.complete(List.of());
        }

        log.debug("Retrieving contexts from collection: {}", options.collectionName());

        // 1. 构建搜索选项
        SearchOptions searchOptions = new SearchOptions(
                options.topK(),
                options.minScore(),
                options.filter());

        // 2. 对口语化/缩写问题生成少量检索变体，并合并多路召回结果
        List<QueryVariant> queryVariants = buildQueryVariants(query);
        List<RetrievedContext> vectorContexts;
        VectorDependencyException vectorFailure = null;
        try {
            vectorContexts = mergeRetrievedContexts(queryVariants, options.collectionName(), searchOptions);
            log.debug("Vector route merged {} contexts from {} query variants", vectorContexts.size(), queryVariants.size());
        } catch (VectorDependencyException e) {
            vectorContexts = List.of();
            vectorFailure = e;
            log.warn("Vector route unavailable: dependency={}, operation={}, errorCategory={}, failMode={}",
                    e.getDependency(), e.getOperation(), e.getErrorCategory(), e.getFailMode());
        }

        List<RetrievedContext> contexts = vectorContexts;
        if (isHybridEnabled()) {
            List<RetrievedContext> keywordContexts;
            try {
                keywordContexts = traceStage(GenAiTelemetry.SpanNames.KEYWORD_SEARCH,
                        () -> keywordIndex.search(
                                options.collectionName(),
                                query,
                                keywordTopK(options.topK()),
                                options.filter()));
            } catch (RuntimeException keywordFailure) {
                if (vectorFailure != null) {
                    throw vectorFailure;
                }
                throw keywordFailure;
            }
            if (vectorFailure != null && (keywordContexts == null || keywordContexts.isEmpty())) {
                throw vectorFailure;
            }
            List<RetrievedContext> finalVectorContexts = vectorContexts;
            contexts = traceStage(GenAiTelemetry.SpanNames.RETRIEVAL_FUSION,
                    () -> fuseByRrf(finalVectorContexts, keywordContexts, rrfK()));
            log.debug("Hybrid retrieval fused vectorContexts={}, keywordContexts={}, fused={}",
                    vectorContexts.size(), keywordContexts.size(), contexts.size());
        } else if (vectorFailure != null) {
            throw vectorFailure;
        }

        Map<String, Object> rerankDiagnostics;
        // 3. 如果启用重排序，执行重排序
        if (options.enableRerank() && !contexts.isEmpty()) {
            List<RetrievedContext> rerankCandidates = contexts.stream()
                    .limit(rerankTopN())
                    .toList();
            RerankOutcome rerankOutcome;
            try (GenAiTelemetry.SpanScope rerank = telemetry.startSpan(
                    GenAiTelemetry.SpanNames.RERANK, Map.of())) {
                try {
                    rerankOutcome = rerankerRegistry.rerankWithDiagnostics(query, rerankCandidates);
                    Map<String, Object> diagnostics = rerankOutcome.diagnostics().toMap();
                    long fallbackCount = diagnostics.get("rerankFallbackCount") instanceof Number number
                            ? number.longValue() : 0L;
                    rerank.diagnostics(diagnostics)
                            .longFact(GenAiTelemetry.Attributes.CANDIDATE_COUNT, rerankCandidates.size())
                            .longFact(GenAiTelemetry.Attributes.SELECTED_COUNT, rerankOutcome.contexts().size())
                            .outcome(fallbackCount > 0L ? "FALLBACK_SUCCESS" : "SUCCESS");
                } catch (RuntimeException failure) {
                    rerank.safeError(failure, "rerank", "RERANK_FAILED").outcome("ERROR");
                    throw failure;
                }
            }
            contexts = rerankOutcome.contexts();
            rerankDiagnostics = rerankOutcome.diagnostics().toMap();
            log.debug("Reranked {} contexts", contexts.size());
        } else {
            rerankDiagnostics = rerankerRegistry
                    .diagnosticsWithoutExecution(options.enableRerank(), contexts.size())
                    .toMap();
        }

        List<RetrievedContext> finalContexts = contexts.stream()
                .limit(finalTopK(options.topK()))
                .toList();
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        if (vectorFailure != null) {
            diagnostics.putAll(RetrievalResult.keywordOnly(finalContexts).diagnostics());
        }
        diagnostics.putAll(rerankDiagnostics);
        return new RetrievalResult(finalContexts, diagnostics);
    }

    private boolean isHybridEnabled() {
        return retrievalProperties.getHybrid().isEnabled() && retrievalProperties.getKeyword().isEnabled();
    }

    private int keywordTopK(int topK) {
        int multiplier = Math.max(1, retrievalProperties.getHybrid().getKeywordTopKMultiplier());
        return Math.max(topK, topK * multiplier);
    }

    private int rrfK() {
        return Math.max(1, retrievalProperties.getHybrid().getRrfK());
    }

    private int rerankTopN() {
        return Math.max(1, retrievalProperties.getRerank().getTopN());
    }

    private int finalTopK(int requestedTopK) {
        int configuredTopK = Math.max(1, retrievalProperties.getRerank().getTopK());
        return Math.max(1, Math.min(requestedTopK, configuredTopK));
    }

    @Override
    public List<QueryVariantInfo> explainQueryVariants(String query) {
        return buildQueryVariants(query).stream()
                .map(variant -> new QueryVariantInfo(variant.query(), variant.weight()))
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
            float[] queryVector = traceStage(GenAiTelemetry.SpanNames.QUERY_EMBEDDING,
                    () -> embeddingService.embed(queryVariant.query()));
            log.debug("Query variant embedded: weight={}", queryVariant.weight());

            List<SearchResult> searchResults = traceStage(GenAiTelemetry.SpanNames.VECTOR_SEARCH,
                    () -> vectorStore.search(collectionName, queryVector, searchOptions));
            log.debug("Vector search returned {} results for query variant", searchResults.size());

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

    private <T> T traceStage(String spanName, java.util.function.Supplier<T> action) {
        try (GenAiTelemetry.SpanScope stage = telemetry.startSpan(spanName, Map.of())) {
            try {
                T result = action.get();
                stage.outcome("SUCCESS");
                return result;
            } catch (RuntimeException failure) {
                stage.safeError(failure, "retrieval", "STAGE_FAILED").outcome("ERROR");
                throw failure;
            }
        }
    }

    private List<RetrievedContext> fuseByRrf(List<RetrievedContext> vectorContexts,
            List<RetrievedContext> keywordContexts,
            int rrfK) {
        if ((keywordContexts == null || keywordContexts.isEmpty())) {
            return vectorContexts;
        }
        if (vectorContexts == null || vectorContexts.isEmpty()) {
            return keywordContexts;
        }

        Map<String, RrfContext> fused = new LinkedHashMap<>();
        addRrfRoute(fused, vectorContexts, rrfK, "vector");
        addRrfRoute(fused, keywordContexts, rrfK, "keyword");

        return fused.values().stream()
                .sorted(Comparator.comparingDouble(RrfContext::score).reversed())
                .map(RrfContext::toRetrievedContext)
                .toList();
    }

    private void addRrfRoute(Map<String, RrfContext> fused,
            List<RetrievedContext> contexts,
            int rrfK,
            String routeName) {
        if (contexts == null) {
            return;
        }
        for (int i = 0; i < contexts.size(); i++) {
            RetrievedContext context = contexts.get(i);
            String key = buildResultKey(context);
            double contribution = 1.0d / (rrfK + i + 1);
            fused.compute(key, (ignored, existing) -> {
                if (existing == null) {
                    return new RrfContext(context, contribution, routeName);
                }
                return existing.add(context, contribution, routeName);
            });
        }
    }

    /**
     * 截断日志输出
     */
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

        addExplanationVariants(variants, conversationalCore);

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

    private void addExplanationVariants(Map<String, Float> variants, String query) {
        ExplanationIntent intent = parseExplanationIntent(query);
        if (intent == null || intent.subject().isBlank()) {
            return;
        }

        addQueryVariant(variants, intent.subject(), 0.985f);

        switch (intent.type()) {
            case HOW -> {
                addQueryVariant(variants, intent.subject() + " 工作原理", 0.98f);
                addQueryVariant(variants, intent.subject() + " 运行流程", 0.97f);
                addQueryVariant(variants, intent.subject() + " 核心机制", 0.96f);
            }
            case WHY -> {
                addQueryVariant(variants, intent.subject() + " 作用", 0.98f);
                addQueryVariant(variants, intent.subject() + " 目的", 0.97f);
                addQueryVariant(variants, "为什么需要 " + intent.subject(), 0.96f);
            }
            case GENERAL -> {
                addQueryVariant(variants, intent.subject() + " 原理", 0.97f);
                addQueryVariant(variants, intent.subject() + " 机制", 0.96f);
                addQueryVariant(variants, intent.subject() + " 流程", 0.95f);
            }
        }
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

    private record RrfContext(RetrievedContext context, double score, Set<String> routes) {
        private RrfContext(RetrievedContext context, double score, String route) {
            this(context, score, new LinkedHashSet<>(List.of(route)));
        }

        private RrfContext add(RetrievedContext candidate, double contribution, String route) {
            RetrievedContext chosen = candidate.relevanceScore() > context.relevanceScore() ? candidate : context;
            LinkedHashSet<String> nextRoutes = new LinkedHashSet<>(routes);
            nextRoutes.add(route);
            return new RrfContext(chosen, score + contribution, nextRoutes);
        }

        private RetrievedContext toRetrievedContext() {
            Map<String, Object> metadata = new LinkedHashMap<>(context.metadata() == null ? Map.of() : context.metadata());
            metadata.put("retrievalRoutes", List.copyOf(routes));
            metadata.put("rrfScore", score);
            return new RetrievedContext(context.content(), context.source(), (float) score, metadata);
        }
    }

    private record QueryVariant(String query, float weight) {
    }

    private record ExplanationIntent(ExplanationType type, String subject) {
    }

    private enum ExplanationType {
        HOW,
        WHY,
        GENERAL
    }
}
