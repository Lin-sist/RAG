package com.enterprise.rag.core.rag.router;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class BoundedQueryRouter {
    private final RouterProperties properties;
    private final DeterministicFactIntentClassifier classifier;
    private final QueryStrategyRegistry strategyRegistry;

    public BoundedQueryRouter(
            RouterProperties properties,
            DeterministicFactIntentClassifier classifier) {
        this(properties, classifier, new QueryStrategyRegistry());
    }

    @Autowired
    public BoundedQueryRouter(
            RouterProperties properties,
            DeterministicFactIntentClassifier classifier,
            QueryStrategyRegistry strategyRegistry) {
        this.properties = properties;
        this.classifier = classifier;
        this.strategyRegistry = strategyRegistry;
        properties.validate();
        strategyRegistry.require(properties.getStrategyVersion());
    }

    public Optional<QueryRoutePlan> plan(String query) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        QueryClassification classification = classifier.classify(query);
        QueryStrategyId strategy = classification.intent() == QueryIntent.FACT
                ? strategyRegistry.require(properties.getStrategyVersion())
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
