package com.enterprise.rag.core.vectorstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量存储配置属性
 */
@ConfigurationProperties(prefix = "rag.vectorstore")
public class VectorStoreProperties {

    /**
     * 向量存储类型：milvus, qdrant, elasticsearch
     */
    private String type = "milvus";

    /**
     * Milvus 配置
     */
    private MilvusProperties milvus = new MilvusProperties();

    /**
     * Qdrant 配置
     */
    private QdrantProperties qdrant = new QdrantProperties();

    /**
     * Elasticsearch 配置
     */
    private ElasticsearchProperties elasticsearch = new ElasticsearchProperties();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MilvusProperties getMilvus() {
        return milvus;
    }

    public void setMilvus(MilvusProperties milvus) {
        this.milvus = milvus;
    }

    public QdrantProperties getQdrant() {
        return qdrant;
    }

    public void setQdrant(QdrantProperties qdrant) {
        this.qdrant = qdrant;
    }

    public ElasticsearchProperties getElasticsearch() {
        return elasticsearch;
    }

    public void setElasticsearch(ElasticsearchProperties elasticsearch) {
        this.elasticsearch = elasticsearch;
    }

    /**
     * Milvus 配置属性
     */
    public static class MilvusProperties {
        /**
         * Milvus 服务地址
         */
        private String host = "localhost";

        /**
         * Milvus 服务端口
         */
        private int port = 19530;

        /**
         * 数据库名称
         */
        private String database = "default";

        /**
         * 连接超时时间（秒）
         */
        private int connectTimeout = 10;

        /**
         * 索引类型：IVF_FLAT, IVF_SQ8, HNSW
         */
        private String indexType = "IVF_FLAT";

        /**
         * 度量类型：L2, IP, COSINE
         */
        private String metricType = "COSINE";

        /**
         * nlist 参数（用于 IVF 索引）
         */
        private int nlist = 1024;

        /**
         * nprobe 参数（搜索时使用）
         */
        private int nprobe = 16;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public String getIndexType() {
            return indexType;
        }

        public void setIndexType(String indexType) {
            this.indexType = indexType;
        }

        public String getMetricType() {
            return metricType;
        }

        public void setMetricType(String metricType) {
            this.metricType = metricType;
        }

        public int getNlist() {
            return nlist;
        }

        public void setNlist(int nlist) {
            this.nlist = nlist;
        }

        public int getNprobe() {
            return nprobe;
        }

        public void setNprobe(int nprobe) {
            this.nprobe = nprobe;
        }
    }

    /**
     * Qdrant 配置属性
     */
    public static class QdrantProperties {
        /**
         * Qdrant 服务地址
         */
        private String host = "localhost";

        /**
         * Qdrant gRPC 端口
         */
        private int grpcPort = 6334;

        /**
         * 是否使用 TLS
         */
        private boolean useTls = false;

        /**
         * API Key（可选）
         */
        private String apiKey;

        /**
         * 连接超时时间（秒）
         */
        private int connectTimeout = 10;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getGrpcPort() {
            return grpcPort;
        }

        public void setGrpcPort(int grpcPort) {
            this.grpcPort = grpcPort;
        }

        public boolean isUseTls() {
            return useTls;
        }

        public void setUseTls(boolean useTls) {
            this.useTls = useTls;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }
    }

    /**
     * Elasticsearch 配置属性
     */
    public static class ElasticsearchProperties {
        /**
         * Elasticsearch 服务地址
         */
        private String host = "localhost";

        /**
         * Elasticsearch 端口
         */
        private int port = 9200;

        /**
         * 协议：http 或 https
         */
        private String scheme = "http";

        /**
         * 用户名（可选）
         */
        private String username;

        /**
         * 密码（可选）
         */
        private String password;

        /**
         * 连接超时时间（秒）
         */
        private int connectTimeout = 10;

        /**
         * 相似度算法：cosine, dot_product, l2_norm
         */
        private String similarity = "cosine";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public String getSimilarity() {
            return similarity;
        }

        public void setSimilarity(String similarity) {
            this.similarity = similarity;
        }
    }
}
