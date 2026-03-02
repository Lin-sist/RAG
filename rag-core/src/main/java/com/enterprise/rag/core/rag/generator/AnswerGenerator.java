package com.enterprise.rag.core.rag.generator;

import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 答案生成器接口
 * 负责调用 LLM 生成答案
 */
public interface AnswerGenerator {

    /**
     * 生成答案（同步）
     *
     * @param query    用户问题
     * @param contexts 检索到的上下文列表
     * @return 生成的答案
     */
    GeneratedAnswer generate(String query, List<RetrievedContext> contexts);

    /**
     * 生成答案（流式）
     *
     * @param query    用户问题
     * @param contexts 检索到的上下文列表
     * @return 答案文本流
     */
    Flux<String> generateStream(String query, List<RetrievedContext> contexts);

    /**
     * 获取当前使用的模型名称
     *
     * @return 模型名称
     */
    String getModelName();
}
