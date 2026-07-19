package com.enterprise.rag.core.rag.query;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "retrieval")
public class RetrievalProperties {
    private Hybrid hybrid = new Hybrid();
    private Keyword keyword = new Keyword();
    private Rerank rerank = new Rerank();

    @Data
    public static class Hybrid {
        private boolean enabled = true;
        private int rrfK = 60;
        private int keywordTopKMultiplier = 2;
    }

    @Data
    public static class Keyword {
        private boolean enabled = true;
    }

    @Data
    public static class Rerank {
        private String provider = "heuristic";
        private int topN = 20;
        private int topK = 5;
        private ModelReranker model = new ModelReranker();
        private NvidiaReranker nvidia = new NvidiaReranker();
    }

    @Data
    public static class ModelReranker {
        private boolean enabled = false;
        private String baseUrl = "";
        private String endpointPath = "/rerank";
        private String apiKey = "";
        private String model = "";
        private int timeoutMillis = 3000;
        private boolean healthCheckEnabled = true;
        private String healthPath = "/health";
        private long healthCacheMillis = 60000;
    }

    @Data
    public static class NvidiaReranker {
        private boolean enabled = false;
        private String baseUrl = "";
        private String endpointPath = "/v1/ranking";
        private String apiKey = "";
        private String model = "";
        private String truncate = "NONE";
        private int timeoutMillis = 3000;
        private int maxCandidates = 100;
        private boolean healthCheckEnabled = false;
        private String healthPath = "/health";
        private long healthCacheMillis = 60000;
    }
}
