package com.enterprise.rag.core.rag.prompt;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Prompt 构建器
 * 负责将检索到的上下文和用户问题组合成 LLM 可用的 Prompt
 */
@Slf4j
@Component
public class PromptBuilder {

    public static final int DEFAULT_CONTEXT_TOKEN_BUDGET = 1200;

    private static final String SIMPLE_TEMPLATE = """
            Based on the following context, please answer the question.
            You MUST reply in the SAME language as the user's question.

            Context:
            %s

            Question: %s

            Answer:""";

    private static final String STRUCTURED_TEMPLATE = """
            You are a helpful assistant that answers questions based on the provided context.

            ## Instructions
            - Answer the question based ONLY on the provided context
            - If the context doesn't contain enough information, say so
            - Cite the source when possible
            - Be concise and accurate
            - You MUST reply in the SAME language as the user's question (e.g. Chinese question → Chinese answer)

            ## Context
            %s

            ## Question
            %s

            ## Answer""";

    private static final String CHAIN_OF_THOUGHT_TEMPLATE = """
            You are a helpful assistant. Please answer the question step by step.
            You MUST reply in the SAME language as the user's question.

            ## Context
            %s

            ## Question
            %s

            ## Let's think step by step:
            1. First, identify the key information in the context
            2. Then, analyze how it relates to the question
            3. Finally, provide a clear answer

            ## Answer""";

    private static final String CODE_FOCUSED_TEMPLATE = """
            You are a technical assistant specialized in code and documentation.

            ## Context (Code/Documentation)
            %s

            ## Question
            %s

            ## Instructions
            - Provide accurate technical answers
            - Include code examples when relevant
            - Reference specific parts of the context
            - You MUST reply in the SAME language as the user's question

            ## Answer""";

    private static final String NO_CONTEXT_TEMPLATE = """
            I don't have enough context to answer your question accurately.

            Question: %s

            Please provide more specific information or rephrase your question.""";

    /**
     * 使用默认策略构建 Prompt
     */
    public String build(String query, List<RetrievedContext> contexts) {
        return build(query, contexts, PromptStrategy.STRUCTURED);
    }

    /**
     * 使用指定策略构建 Prompt
     *
     * @param query    用户问题
     * @param contexts 检索到的上下文列表
     * @param strategy Prompt 策略
     * @return 构建好的 Prompt
     */
    public String build(String query, List<RetrievedContext> contexts, PromptStrategy strategy) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or blank");
        }

        if (contexts == null || contexts.isEmpty()) {
            log.warn("No contexts provided for query: {}", truncateForLog(query));
            return String.format(NO_CONTEXT_TEMPLATE, query);
        }

        String formattedContext = formatContexts(contexts);
        String template = getTemplate(strategy);

        String prompt = String.format(template, formattedContext, query);
        log.debug("Built prompt with strategy {} for query: {}", strategy, truncateForLog(query));

        return prompt;
    }

    /**
     * 使用上下文优化策略构建 Prompt：去重 + token 预算裁剪 + 来源增强
     */
    public PromptBuildResult buildOptimized(String query,
            List<RetrievedContext> contexts,
            PromptStrategy strategy,
            int contextTokenBudget) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or blank");
        }

        if (contexts == null || contexts.isEmpty()) {
            log.warn("No contexts provided for query: {}", truncateForLog(query));
            return new PromptBuildResult(String.format(NO_CONTEXT_TEMPLATE, query), List.of(), 0, 0, 0,
                    contextTokenBudget);
        }

        List<RetrievedContext> deduplicatedContexts = deduplicateContexts(contexts);
        int removedByDedup = Math.max(0, contexts.size() - deduplicatedContexts.size());

        List<RetrievedContext> budgetedContexts = applyTokenBudget(deduplicatedContexts, contextTokenBudget);
        int removedByBudget = Math.max(0, deduplicatedContexts.size() - budgetedContexts.size());

        String formattedContext = formatContexts(budgetedContexts);
        String template = getTemplate(strategy);
        String prompt = String.format(template, formattedContext, query);

        int estimatedTokens = estimateTokens(formattedContext);
        return new PromptBuildResult(
                prompt,
                budgetedContexts,
                estimatedTokens,
                removedByDedup,
                removedByBudget,
                contextTokenBudget);
    }

    /**
     * 格式化上下文列表
     */
    private String formatContexts(List<RetrievedContext> contexts) {
        return IntStream.range(0, contexts.size())
                .mapToObj(i -> formatSingleContext(i + 1, contexts.get(i)))
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 格式化单个上下文
     */
    private String formatSingleContext(int index, RetrievedContext context) {
        Map<String, Object> metadata = context.metadata() == null ? Map.of() : context.metadata();
        String sourceTitle = String.valueOf(metadata.getOrDefault("title", ""));
        Object chunkIndex = metadata.get("chunkIndex");
        String sourceHint = sourceTitle.isBlank() ? context.source() : sourceTitle + " / " + context.source();
        String chunkHint = chunkIndex == null ? "" : " #chunk=" + chunkIndex;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[Source %d: %s (Score: %.2f)]%n",
                index, sourceHint + chunkHint, context.relevanceScore()));
        sb.append(context.content() == null ? "" : context.content());
        return sb.toString();
    }

    private List<RetrievedContext> deduplicateContexts(List<RetrievedContext> contexts) {
        Set<String> seen = new LinkedHashSet<>();
        List<RetrievedContext> deduplicated = new ArrayList<>();

        for (RetrievedContext context : contexts) {
            String key = normalizeForDedup(context.source()) + "::" + normalizeForDedup(context.content());
            if (seen.add(key)) {
                deduplicated.add(context);
            }
        }
        return deduplicated;
    }

    private List<RetrievedContext> applyTokenBudget(List<RetrievedContext> contexts, int tokenBudget) {
        if (tokenBudget <= 0) {
            return contexts;
        }

        List<RetrievedContext> selected = new ArrayList<>();
        int used = 0;
        for (RetrievedContext context : contexts) {
            int contextTokens = estimateTokens(
                    (context.content() == null ? "" : context.content()) + " " + context.source());
            if (selected.isEmpty() || used + contextTokens <= tokenBudget) {
                selected.add(context);
                used += contextTokens;
            }
        }

        return selected;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        int cjkCount = 0;
        int otherCount = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                cjkCount++;
            } else {
                otherCount++;
            }
        }

        int otherTokenEstimate = (int) Math.ceil(otherCount / 4.0d);
        return cjkCount + otherTokenEstimate;
    }

    private String normalizeForDedup(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /**
     * 获取对应策略的模板
     */
    private String getTemplate(PromptStrategy strategy) {
        return switch (strategy) {
            case SIMPLE -> SIMPLE_TEMPLATE;
            case STRUCTURED -> STRUCTURED_TEMPLATE;
            case CHAIN_OF_THOUGHT -> CHAIN_OF_THOUGHT_TEMPLATE;
            case CODE_FOCUSED -> CODE_FOCUSED_TEMPLATE;
        };
    }

    /**
     * 检查 Prompt 是否包含所有上下文内容
     * 用于验证 Prompt 构建的正确性
     */
    public boolean containsAllContexts(String prompt, List<RetrievedContext> contexts) {
        if (prompt == null || contexts == null) {
            return false;
        }
        return contexts.stream()
                .allMatch(ctx -> prompt.contains(ctx.content()));
    }

    /**
     * 截断日志输出
     */
    private String truncateForLog(String text) {
        if (text == null)
            return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    public record PromptBuildResult(
            String prompt,
            List<RetrievedContext> contexts,
            int estimatedContextTokens,
            int removedByDedup,
            int removedByBudget,
            int tokenBudget) {
    }
}
