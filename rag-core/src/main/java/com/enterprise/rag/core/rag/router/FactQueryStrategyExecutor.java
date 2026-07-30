package com.enterprise.rag.core.rag.router;

import com.enterprise.rag.core.rag.generator.AnswerGenerator;
import com.enterprise.rag.core.rag.generator.GenerationBudget;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

/** Closed-world fact-v1 generation and evidence-policy executor. */
public final class FactQueryStrategyExecutor {
    private final AnswerGenerator answerGenerator;
    private final EvidenceNoAnswerPolicy evidencePolicy;

    public FactQueryStrategyExecutor(AnswerGenerator answerGenerator) {
        this(answerGenerator, new EvidenceNoAnswerPolicy());
    }

    FactQueryStrategyExecutor(AnswerGenerator answerGenerator, EvidenceNoAnswerPolicy evidencePolicy) {
        this.answerGenerator = Objects.requireNonNull(answerGenerator, "answerGenerator");
        this.evidencePolicy = Objects.requireNonNull(evidencePolicy, "evidencePolicy");
    }

    public QueryBudgetLedger openBudget(QueryRoutePlan plan) {
        requireFactPlan(plan);
        return new QueryBudgetLedger(plan.budget());
    }

    public EvidenceAdmission beforeGeneration(List<RetrievedContext> contexts) {
        return evidencePolicy.beforeGeneration(contexts);
    }

    public GeneratedAnswer generate(
            String question, List<RetrievedContext> contexts, QueryRoutePlan plan) {
        requireFactPlan(plan);
        return answerGenerator.generate(question, contexts, generationBudget(plan));
    }

    public Flux<String> generateStream(
            String question, List<RetrievedContext> contexts, QueryRoutePlan plan) {
        requireFactPlan(plan);
        return answerGenerator.generateStream(question, contexts, generationBudget(plan));
    }

    public GeneratedAnswer finalizeStream(
            String question,
            String answer,
            List<RetrievedContext> contexts,
            QueryRoutePlan plan) {
        requireFactPlan(plan);
        return answerGenerator.finalizeStream(question, answer, contexts, generationBudget(plan));
    }

    public EvidenceDecision afterGeneration(GeneratedAnswer answer) {
        return evidencePolicy.afterGeneration(answer);
    }

    private GenerationBudget generationBudget(QueryRoutePlan plan) {
        return new GenerationBudget(
                plan.budget().maxContextTokens(),
                plan.budget().maxOutputTokens());
    }

    private void requireFactPlan(QueryRoutePlan plan) {
        if (plan == null
                || plan.intent() != QueryIntent.FACT
                || plan.effectiveStrategy() != QueryStrategyId.FACT_V1) {
            throw new IllegalStateException("fact-v1 executor requires a registered FACT plan");
        }
    }
}
