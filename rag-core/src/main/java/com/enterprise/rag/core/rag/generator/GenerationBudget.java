package com.enterprise.rag.core.rag.generator;

public record GenerationBudget(int maxContextTokens, int maxOutputTokens) {
    public GenerationBudget {
        if (maxContextTokens < 1 || maxOutputTokens < 1) {
            throw new IllegalArgumentException("Generation token ceilings must be positive");
        }
    }
}
