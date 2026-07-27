package com.enterprise.rag.core.vectorstore;

import java.util.List;

/**
 * 向量存储接口
 * 定义向量数据库的标准操作，支持多种实现（Milvus、Qdrant、Elasticsearch）
 * 
 * @see VectorDocument
 * @see SearchResult
 * @see SearchOptions
 */
public interface VectorStore {

    /**
     * 创建向量集合
     *
     * @param collectionName 集合名称
     * @param dimension      向量维度
     * @throws VectorStoreException 当创建失败时抛出
     */
    void createCollection(String collectionName, int dimension);

    default void createCollection(TenantVectorScope scope, int dimension) {
        throw new UnsupportedOperationException("Tenant vector create is not supported by this adapter");
    }

    /**
     * 检查集合是否存在
     *
     * @param collectionName 集合名称
     * @return true 如果集合存在
     */
    boolean hasCollection(String collectionName);

    default boolean hasCollection(TenantVectorScope scope) {
        throw new UnsupportedOperationException("Tenant vector lookup is not supported by this adapter");
    }

    /**
     * 删除向量集合
     *
     * @param collectionName 集合名称
     * @throws VectorStoreException 当删除失败时抛出
     */
    void dropCollection(String collectionName);

    default void dropCollection(TenantVectorScope scope) {
        throw new UnsupportedOperationException("Tenant vector drop is not supported by this adapter");
    }

    /**
     * 插入或更新向量文档（批量）
     * 如果文档ID已存在则更新，否则插入
     *
     * @param collectionName 集合名称
     * @param documents      向量文档列表
     * @throws VectorStoreException 当操作失败时抛出
     */
    void upsert(String collectionName, List<VectorDocument> documents);

    default void upsert(TenantVectorScope scope, List<VectorDocument> documents) {
        throw new UnsupportedOperationException("Tenant vector upsert is not supported by this adapter");
    }

    default void upsert(TenantVectorScope scope, VectorDocument document) {
        upsert(scope, List.of(document));
    }

    /**
     * 插入或更新单个向量文档
     *
     * @param collectionName 集合名称
     * @param document       向量文档
     * @throws VectorStoreException 当操作失败时抛出
     */
    default void upsert(String collectionName, VectorDocument document) {
        upsert(collectionName, List.of(document));
    }

    /**
     * 向量相似度搜索
     *
     * @param collectionName 集合名称
     * @param queryVector    查询向量
     * @param options        搜索选项
     * @return 搜索结果列表，按相似度降序排列
     * @throws VectorStoreException 当搜索失败时抛出
     */
    List<SearchResult> search(String collectionName, float[] queryVector, SearchOptions options);

    /**
     * Tenant-scoped search entry point. Adapter implementations must override this
     * during C13b; the temporary bridge is removed once the adapter contract lands.
     */
    default List<SearchResult> search(
            TenantVectorScope scope, float[] queryVector, SearchOptions options) {
        throw new UnsupportedOperationException("Tenant vector search is not supported by this adapter");
    }

    /**
     * 使用默认选项进行向量搜索
     *
     * @param collectionName 集合名称
     * @param queryVector    查询向量
     * @param topK           返回结果数量
     * @return 搜索结果列表
     */
    default List<SearchResult> search(String collectionName, float[] queryVector, int topK) {
        return search(collectionName, queryVector, SearchOptions.withTopK(topK));
    }

    /**
     * 根据ID删除向量文档
     *
     * @param collectionName 集合名称
     * @param ids            要删除的文档ID列表
     * @throws VectorStoreException 当删除失败时抛出
     */
    void delete(String collectionName, List<String> ids);

    default void delete(TenantVectorScope scope, List<String> ids) {
        throw new UnsupportedOperationException("Tenant vector delete is not supported by this adapter");
    }

    default void delete(TenantVectorScope scope, String id) {
        delete(scope, List.of(id));
    }

    /**
     * 删除单个向量文档
     *
     * @param collectionName 集合名称
     * @param id             文档ID
     */
    default void delete(String collectionName, String id) {
        delete(collectionName, List.of(id));
    }

    default VectorDocument getById(TenantVectorScope scope, String id) {
        throw new UnsupportedOperationException("Tenant vector get is not supported by this adapter");
    }

    default List<VectorDocument> getByIds(TenantVectorScope scope, List<String> ids) {
        throw new UnsupportedOperationException("Tenant vector get is not supported by this adapter");
    }

    /**
     * 获取集合中的文档数量
     *
     * @param collectionName 集合名称
     * @return 文档数量
     */
    long count(String collectionName);

    default long count(TenantVectorScope scope) {
        throw new UnsupportedOperationException("Tenant vector count is not supported by this adapter");
    }

    /**
     * 获取向量存储类型名称
     *
     * @return 存储类型名称（如 "milvus", "qdrant", "elasticsearch"）
     */
    String getType();
}
