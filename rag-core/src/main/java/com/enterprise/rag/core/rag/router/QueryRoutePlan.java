package com.enterprise.rag.core.rag.router;

public record QueryRoutePlan(
        String classifierVersion,
        String policyVersion,
        QueryIntent intent,
        QueryRouteReason reason,
        QueryStrategyId effectiveStrategy,
        QueryExecutionBudget budget) {
}
