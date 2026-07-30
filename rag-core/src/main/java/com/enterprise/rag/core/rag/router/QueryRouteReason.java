package com.enterprise.rag.core.rag.router;

public enum QueryRouteReason {
    DEFINITION_CUE,
    FACT_LOOKUP_CUE,
    MULTI_HOP_CUE,
    GLOBAL_CUE,
    HIGH_RISK_CUE,
    AMBIGUOUS,
    INVALID_INPUT
}
