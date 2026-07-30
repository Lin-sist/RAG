package com.enterprise.rag.core.rag.router;

import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public final class QueryStrategyRegistry {
    private final EnumSet<QueryStrategyId> registered = EnumSet.of(QueryStrategyId.FACT_V1);

    public QueryStrategyId require(String strategyId) {
        for (QueryStrategyId candidate : registered) {
            if (candidate.id().equals(strategyId)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unregistered router strategy");
    }

    public boolean contains(QueryStrategyId strategyId) {
        return strategyId != null && registered.contains(strategyId);
    }
}
