package com.enterprise.rag.core.rag.router;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag.router")
public class RouterProperties {
    private boolean enabled = false;
    private String classifierVersion = "fact-intent-v1";
    private String policyVersion = "evidence-no-answer-v1";
    private Fact fact = new Fact();

    public void validate() {
        if (!"fact-intent-v1".equals(classifierVersion)) {
            throw new IllegalStateException("Unsupported router classifier version");
        }
        if (!"evidence-no-answer-v1".equals(policyVersion)) {
            throw new IllegalStateException("Unsupported router policy version");
        }
        if (fact == null
                || fact.maxQueryVariants < 1 || fact.maxQueryVariants > 32
                || fact.maxRetrievalPasses != 1
                || fact.maxRerankCalls < 0 || fact.maxRerankCalls > 1
                || fact.maxGenerationCalls != 1
                || fact.maxContextTokens < 1 || fact.maxContextTokens > 1200
                || fact.maxOutputTokens < 1 || fact.maxOutputTokens > 2048
                || fact.deadlineMillis < 1000L || fact.deadlineMillis > 120_000L) {
            throw new IllegalStateException("Invalid bounded router budget");
        }
    }

    @Data
    public static class Fact {
        private int maxQueryVariants = 8;
        private int maxRetrievalPasses = 1;
        private int maxRerankCalls = 1;
        private int maxGenerationCalls = 1;
        private int maxContextTokens = 1200;
        private int maxOutputTokens = 2048;
        private long deadlineMillis = 120_000L;

        public QueryExecutionBudget toBudget() {
            return new QueryExecutionBudget(
                    maxQueryVariants,
                    maxRetrievalPasses,
                    maxRerankCalls,
                    maxGenerationCalls,
                    maxContextTokens,
                    maxOutputTokens,
                    deadlineMillis);
        }
    }
}
