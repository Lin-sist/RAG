package com.enterprise.rag.core.rag.service;

import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.QAResponse;
import reactor.core.publisher.Flux;

/**
 * RAG 服务接口
 * 提供检索增强生成的问答能力
 */
public interface RAGService {

    String STREAM_TERMINAL_SIGNAL_CONTEXT_KEY = "rag.stream.terminal.signal";

    /** SSE adapter 可在取消订阅前标记真实 timeout，避免与主动断连混淆。 */
    final class StreamTerminalSignal {
        private final java.util.concurrent.atomic.AtomicBoolean timeout =
                new java.util.concurrent.atomic.AtomicBoolean();

        public void markTimeout() {
            timeout.set(true);
        }

        public boolean isTimeout() {
            return timeout.get();
        }
    }

    /**
     * 执行问答（同步）
     *
     * @param request 问答请求
     * @return 问答响应
     */
    QAResponse ask(QARequest request);

    /**
     * 执行问答（流式）
     *
     * @param request 问答请求
     * @return 答案文本流
     */
    Flux<String> askStream(QARequest request);

    /**
     * 简单问答接口
     *
     * @param question       问题
     * @param collectionName 知识库集合名称
     * @return 问答响应
     */
    default QAResponse ask(String question, String collectionName) {
        return ask(QARequest.of(question, collectionName));
    }

    /**
     * 清除查询缓存
     *
     * @param question       问题
     * @param collectionName 知识库集合名称
     */
    void evictCache(String question, String collectionName);

    /**
     * 清除所有查询缓存
     */
    void clearAllCache();
}
