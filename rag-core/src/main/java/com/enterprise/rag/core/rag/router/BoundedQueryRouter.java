package com.enterprise.rag.core.rag.router;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public final class BoundedQueryRouter {
    private final RouterProperties properties;
    private final DeterministicFactIntentClassifier classifier;

    public BoundedQueryRouter(
            RouterProperties properties,
            DeterministicFactIntentClassifier classifier) {
        this.properties = properties;
        this.classifier = classifier;
        properties.validate();
    }

    public Optional<QueryRoutePlan> plan(String query) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        QueryClassification classification = classifier.classify(query);
        QueryStrategyId strategy = classification.intent() == QueryIntent.FACT
                ? QueryStrategyId.FACT_V1
                : null;
        return Optional.of(new QueryRoutePlan(
                properties.getClassifierVersion(),
                properties.getPolicyVersion(),
                classification.intent(),
                classification.reason(),
                strategy,
                properties.getFact().toBudget()));
    }
}
