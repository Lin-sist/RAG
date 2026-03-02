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
import java.util.List;

/**
 * 查询引擎实现
 * 实现问题向量化、相似度检索和结果重排序
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryEngineImpl implements QueryEngine {

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

        // 1. 将问题转换为向量
        float[] queryVector = embeddingService.embed(query);
        log.debug("Query embedded to vector with dimension: {}", queryVector.length);

        // 2. 构建搜索选项
        SearchOptions searchOptions = new SearchOptions(
                options.topK(),
                options.minScore(),
                options.filter()
        );

        // 3. 执行向量相似度搜索
        List<SearchResult> searchResults = vectorStore.search(
                options.collectionName(),
                queryVector,
                searchOptions
        );
        log.debug("Vector search returned {} results", searchResults.size());

        // 4. 转换为检索上下文
        List<RetrievedContext> contexts = searchResults.stream()
                .map(this::toRetrievedContext)
                .toList();

        // 5. 如果启用重排序，执行重排序
        if (options.enableRerank() && !contexts.isEmpty()) {
            contexts = rerank(query, contexts);
            log.debug("Reranked {} contexts", contexts.size());
        }

        return contexts;
    }


    /**
     * 将搜索结果转换为检索上下文
     */
    private RetrievedContext toRetrievedContext(SearchResult result) {
        return new RetrievedContext(
                result.content(),
                result.id(),
                result.score(),
                result.metadata()
        );
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

        // 提取查询关键词
        String[] queryTerms = query.toLowerCase().split("\\s+");

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
    private float calculateRerankScore(RetrievedContext context, String[] queryTerms) {
        String contentLower = context.content().toLowerCase();
        
        // 计算关键词匹配度
        int matchCount = 0;
        for (String term : queryTerms) {
            if (term.length() > 2 && contentLower.contains(term)) {
                matchCount++;
            }
        }
        float keywordScore = queryTerms.length > 0 
                ? (float) matchCount / queryTerms.length 
                : 0f;

        // 综合分数 = 原始分数 * 0.7 + 关键词分数 * 0.3
        return context.relevanceScore() * 0.7f + keywordScore * 0.3f;
    }

    /**
     * 截断日志输出
     */
    private String truncateForLog(String text) {
        if (text == null) return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    /**
     * 带分数的上下文记录
     */
    private record ScoredContext(RetrievedContext context, float score) {}
}
