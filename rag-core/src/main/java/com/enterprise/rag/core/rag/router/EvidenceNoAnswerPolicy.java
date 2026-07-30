package com.enterprise.rag.core.rag.router;

import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.RetrievedContext;

import java.util.List;

public final class EvidenceNoAnswerPolicy {

    public EvidenceAdmission beforeGeneration(List<RetrievedContext> contexts) {
        return contexts == null || contexts.isEmpty()
                ? EvidenceAdmission.insufficient()
                : EvidenceAdmission.proceed();
    }

    public EvidenceDecision afterGeneration(GeneratedAnswer answer) {
        if (answer != null
                && answer.metadata() != null
                && "no_result".equals(answer.metadata().get("status"))) {
            return new EvidenceDecision(QueryFinalState.NO_ANSWER, NoAnswerReason.MODEL_REFUSAL);
        }
        if (answer == null
                || answer.citations() == null
                || answer.citations().isEmpty()
                || validatedCitationCount(answer) < 1) {
            return new EvidenceDecision(QueryFinalState.NO_ANSWER, NoAnswerReason.UNVALIDATED_EVIDENCE);
        }
        return new EvidenceDecision(QueryFinalState.ANSWER, NoAnswerReason.NONE);
    }

    private int validatedCitationCount(GeneratedAnswer answer) {
        Object count = answer.metadata() == null ? null : answer.metadata().get("validCitations");
        if (count instanceof Number number) {
            return number.intValue();
        }
        Object nested = answer.metadata() == null ? null : answer.metadata().get("citationValidation");
        if (nested instanceof java.util.Map<?, ?> validation
                && validation.get("validCitations") instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
