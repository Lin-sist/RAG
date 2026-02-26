package com.enterprise.rag.core.rag.query;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;

import java.util.List;

/**
 * 查询引擎接口
 * 负责将用户问题转换为向量并检索相关文档
 */
public interface QueryEngine {

    /**
     * 检索与查询相关的文档上下文
     *
     * @param query   用户查询问题
     * @param options 检索选项
     * @return 检索到的上下文列表，按相关性降序排列
     */
    List<RetrievedContext> retrieve(String query, RetrieveOptions options);

    /**
     * 使用默认选项检索文档
     *
     * @param query          用户查询问题
     * @param collectionName 向量集合名称
     * @return 检索到的上下文列表
     */
    default List<RetrievedContext> retrieve(String query, String collectionName) {
        return retrieve(query, RetrieveOptions.of(collectionName));
    }

    /**
     * 检索并返回指定数量的结果
     *
     * @param query          用户查询问题
     * @param collectionName 向量集合名称
     * @param topK           返回结果数量
     * @return 检索到的上下文列表
     */
    default List<RetrievedContext> retrieve(String query, String collectionName, int topK) {
        return retrieve(query, RetrieveOptions.of(collectionName, topK));
    }
}
