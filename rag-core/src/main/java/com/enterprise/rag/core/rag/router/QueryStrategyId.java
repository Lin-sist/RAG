package com.enterprise.rag.core.rag.router;

public enum QueryStrategyId {
    FACT_V1("fact-v1");

    private final String id;

    QueryStrategyId(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
