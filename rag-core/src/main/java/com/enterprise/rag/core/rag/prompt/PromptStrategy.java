package com.enterprise.rag.core.rag.prompt;

/**
 * Prompt 策略枚举
 * 定义不同的 Prompt 构建策略
 */
public enum PromptStrategy {
    /**
     * 简单策略：直接将上下文拼接到问题前
     */
    SIMPLE,

    /**
     * 结构化策略：使用结构化模板，明确区分上下文和问题
     */
    STRUCTURED,

    /**
     * 思维链策略：引导模型逐步推理
     */
    CHAIN_OF_THOUGHT,

    /**
     * 代码专用策略：针对代码问答优化
     */
    CODE_FOCUSED
}
