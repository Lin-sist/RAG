package com.enterprise.rag.core.rag.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * 问答响应记录类
 *
 * @param question  原始问题
 * @param answer    生成的答案
 * @param citations 引用来源列表
 * @param contexts  检索到的上下文（可选）
 * @param metadata  元数据
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QAResponse(
        String question,
        String answer,
        List<Citation> citations,
        List<RetrievedContext> contexts,
        Map<String, Object> metadata
) {
    /**
     * 创建成功响应
     */
    public static QAResponse success(String question, String answer, 
                                     List<Citation> citations, 
                                     List<RetrievedContext> contexts,
                                     Map<String, Object> metadata) {
        return new QAResponse(question, answer, citations, contexts, metadata);
    }

    /**
     * 创建无结果响应
     */
    public static QAResponse noResult(String question) {
        return new QAResponse(
                question,
                "抱歉，未能找到与您问题相关的信息。请尝试换一种方式提问或提供更多细节。",
                List.of(),
                List.of(),
                Map.of("status", "no_result")
        );
    }

    /**
     * 创建错误响应
     */
    public static QAResponse error(String question, String errorMessage) {
        return new QAResponse(
                question,
                "抱歉，处理您的问题时发生错误：" + errorMessage,
                List.of(),
                List.of(),
                Map.of("status", "error", "error", errorMessage)
        );
    }

    /**
     * 检查是否成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return answer != null && !answer.isBlank() 
                && !metadata.containsKey("error");
    }

    /**
     * 检查是否有结果
     */
    @JsonIgnore
    public boolean hasResult() {
        return !"no_result".equals(metadata.get("status"));
    }
}
