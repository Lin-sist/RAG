package com.enterprise.rag.core.vectorstore.config;

import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.elasticsearch.ElasticsearchVectorStore;
import com.enterprise.rag.core.vectorstore.milvus.MilvusVectorStore;
import com.enterprise.rag.core.vectorstore.qdrant.QdrantVectorStore;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 向量存储配置类
 */
@Configuration
@EnableConfigurationProperties(VectorStoreProperties.class)
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    /**
     * 创建 Milvus 客户端
     */
    @Bean
    @ConditionalOnProperty(name = "rag.vectorstore.type", havingValue = "milvus", matchIfMissing = true)
    public MilvusServiceClient milvusServiceClient(VectorStoreProperties properties) {
        VectorStoreProperties.MilvusProperties milvusProps = properties.getMilvus();
        
        log.info("Connecting to Milvus at {}:{}", milvusProps.getHost(), milvusProps.getPort());
        
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(milvusProps.getHost())
                .withPort(milvusProps.getPort())
                .withDatabaseName(milvusProps.getDatabase())
                .withConnectTimeout(milvusProps.getConnectTimeout(), TimeUnit.SECONDS)
                .build();

        return new MilvusServiceClient(connectParam);
    }

    /**
     * 创建 Milvus VectorStore
     */
    @Bean
    @ConditionalOnProperty(name = "rag.vectorstore.type", havingValue = "milvus", matchIfMissing = true)
    public VectorStore milvusVectorStore(MilvusServiceClient milvusClient, VectorStoreProperties properties) {
        return new MilvusVectorStore(milvusClient, properties.getMilvus());
    }

    /**
     * 创建 Qdrant 客户端
     */
    @Bean
    @ConditionalOnProperty(name = "rag.vectorstore.type", havingValue = "qdrant")
    public QdrantClient qdrantClient(VectorStoreProperties properties) {
        VectorStoreProperties.QdrantProperties qdrantProps = properties.getQdrant();
        
        log.info("Connecting to Qdrant at {}:{}", qdrantProps.getHost(), qdrantProps.getGrpcPort());
        
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                qdrantProps.getHost(), 
                qdrantProps.getGrpcPort(), 
                qdrantProps.isUseTls()
        );
        
        if (qdrantProps.getApiKey() != null && !qdrantProps.getApiKey().isEmpty()) {
            builder.withApiKey(qdrantProps.getApiKey());
        }
        
        return new QdrantClient(builder.build());
    }

    /**
     * 创建 Qdrant VectorStore
     */
    @Bean
    @ConditionalOnProperty(name = "rag.vectorstore.type", havingValue = "qdrant")
    public VectorStore qdrantVectorStore(QdrantClient qdrantClient) {
        return new QdrantVectorStore(qdrantClient);
    }

    /**
     * 创建 Elasticsearch 客户端
     */
    @Bean
    @ConditionalOnProperty(name = "rag.vectorstore.type", havingValue = "elasticsearch")
    public ElasticsearchClient elasticsearchClient(VectorStoreProperties properties) {
        VectorStoreProperties.ElasticsearchProperties esProps = properties.getElasticsearch();
        
        log.info("Connecting to Elasticsearch at {}://{}:{}", 
                esProps.getScheme(), esProps.getHost(), esProps.getPort());
        
        RestClientBuilder builder = RestClient.builder(
                new HttpHost(esProps.getHost(), esProps.getPort(), esProps.getScheme())
        );
        
        // 配置认证
        if (esProps.getUsername() != null && !esProps.getUsername().isEmpty()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(esProps.getUsername(), esProps.getPassword()));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        
        RestClient restClient = builder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        
        return new ElasticsearchClient(transport);
    }

    /**
     * 创建 Elasticsearch VectorStore
     */
    @Bean
    @ConditionalOnProperty(name = "rag.vectorstore.type", havingValue = "elasticsearch")
    public VectorStore elasticsearchVectorStore(ElasticsearchClient esClient, VectorStoreProperties properties) {
        return new ElasticsearchVectorStore(esClient, properties.getElasticsearch());
    }
}
