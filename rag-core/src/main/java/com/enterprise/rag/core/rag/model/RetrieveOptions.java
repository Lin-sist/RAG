package com.enterprise.rag.core.rag.model;

import com.enterprise.rag.core.vectorstore.TenantVectorScope;

import java.util.Map;

/**
 * 检索选项记录类
 * 配置查询引擎的检索参数
 *
 * @param scope          服务端解析的租户向量范围
 * @param topK           返回的最大结果数量
 * @param minScore       最小相关性分数阈值
 * @param filter         元数据过滤条件
 * @param enableRerank   是否启用重排序
 * @param maxQueryVariants 服务端允许执行的最大查询变体数
 */
public record RetrieveOptions(
        TenantVectorScope scope,
        int topK,
        float minScore,
        Map<String, Object> filter,
        boolean enableRerank,
        int maxQueryVariants) {

    public RetrieveOptions {
        if (scope == null) {
            throw new IllegalArgumentException("Tenant vector scope is required");
        }
        if (maxQueryVariants < 1) {
            throw new IllegalArgumentException("maxQueryVariants must be at least 1");
        }
        filter = ReservedScopeFilterValidator.validateAndCopy(filter);
    }

    public RetrieveOptions(TenantVectorScope scope, int topK, float minScore,
            Map<String, Object> filter, boolean enableRerank) {
        this(scope, topK, minScore, filter, enableRerank, Integer.MAX_VALUE);
    }

    public String collectionName() {
        return scope.collectionName();
    }

    /**
     * 默认检索选项
     */
    public static final int DEFAULT_TOP_K = 5;
    public static final float DEFAULT_MIN_SCORE = 0.3f;

    /**
     * 创建基本检索选项
     */
    public static RetrieveOptions of(TenantVectorScope scope) {
        return new RetrieveOptions(scope, DEFAULT_TOP_K, DEFAULT_MIN_SCORE, Map.of(), true);
    }

    /**
     * 创建指定topK的检索选项
     */
    public static RetrieveOptions of(TenantVectorScope scope, int topK) {
        return new RetrieveOptions(scope, topK, DEFAULT_MIN_SCORE, Map.of(), true);
    }

    /**
     * 创建带过滤条件的检索选项
     */
    public static RetrieveOptions withFilter(TenantVectorScope scope, int topK, Map<String, Object> filter) {
        return new RetrieveOptions(scope, topK, DEFAULT_MIN_SCORE, filter, true);
    }

    /** @deprecated Raw collection names cannot establish tenant scope. */
    @Deprecated(since = "C13b", forRemoval = false)
    public RetrieveOptions(String collectionName, int topK, float minScore,
            Map<String, Object> filter, boolean enableRerank) {
        this(rejectUnscopedCollection(collectionName), topK, minScore, filter, enableRerank);
    }

    /** @deprecated Raw collection names cannot establish tenant scope. */
    @Deprecated(since = "C13b", forRemoval = false)
    public static RetrieveOptions of(String collectionName) {
        return of(rejectUnscopedCollection(collectionName));
    }

    /** @deprecated Raw collection names cannot establish tenant scope. */
    @Deprecated(since = "C13b", forRemoval = false)
    public static RetrieveOptions of(String collectionName, int topK) {
        return of(rejectUnscopedCollection(collectionName), topK);
    }

    /** @deprecated Raw collection names cannot establish tenant scope. */
    @Deprecated(since = "C13b", forRemoval = false)
    public static RetrieveOptions withFilter(String collectionName, int topK, Map<String, Object> filter) {
        return withFilter(rejectUnscopedCollection(collectionName), topK, filter);
    }

    private static TenantVectorScope rejectUnscopedCollection(String collectionName) {
        throw new IllegalArgumentException("Tenant vector scope is required");
    }
}
