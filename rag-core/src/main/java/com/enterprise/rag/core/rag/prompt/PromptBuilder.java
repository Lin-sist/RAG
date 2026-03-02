package com.enterprise.rag.core.rag.prompt;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Prompt 构建器
 * 负责将检索到的上下文和用户问题组合成 LLM 可用的 Prompt
 */
@Slf4j
@Component
public class PromptBuilder {

    private static final String SIMPLE_TEMPLATE = """
            Based on the following context, please answer the question.
            
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
            
            ## Context
            %s
            
            ## Question
            %s
            
            ## Answer""";

    private static final String CHAIN_OF_THOUGHT_TEMPLATE = """
            You are a helpful assistant. Please answer the question step by step.
            
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
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[Source %d: %s (Score: %.2f)]%n", 
                index, context.source(), context.relevanceScore()));
        sb.append(context.content());
        return sb.toString();
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
        if (text == null) return "null";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
