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
}
