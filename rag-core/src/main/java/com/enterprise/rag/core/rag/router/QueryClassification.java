package com.enterprise.rag.core.rag.router;

public record QueryClassification(QueryIntent intent, QueryRouteReason reason) {
}
