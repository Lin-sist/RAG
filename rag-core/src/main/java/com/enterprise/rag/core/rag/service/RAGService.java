package com.enterprise.rag.core.rag.service;

import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.QAResponse;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
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
    default QAResponse ask(String question, TenantVectorScope scope) {
        return ask(QARequest.of(question, scope));
    }

    /** @deprecated Raw collection names cannot establish tenant scope. */
    @Deprecated(since = "C13b", forRemoval = false)
    default QAResponse ask(String question, String collectionName) {
        throw new IllegalArgumentException("Tenant vector scope is required");
    }

    /**
     * 清除查询缓存
     *
     * @param question       问题
     * @param scope          服务端解析的租户向量范围
     */
    void evictCache(String question, TenantVectorScope scope);

    /** @deprecated Global/unscoped cache eviction is forbidden. */
    @Deprecated(since = "C13b", forRemoval = false)
    default void evictCache(String question, String collectionName) {
        throw new IllegalArgumentException("Tenant vector scope is required");
    }

    /**
     * 清除所有查询缓存
     */
    void clearCache(TenantVectorScope scope);

    /** @deprecated Global business cache clearing is forbidden. */
    @Deprecated(since = "C13b", forRemoval = false)
    default void clearAllCache() {
        throw new IllegalStateException("Global QA cache clear is disabled");
    }
}
