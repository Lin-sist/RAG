package com.enterprise.rag.core.rag.router;

import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvidenceNoAnswerPolicyTest {

    private final EvidenceNoAnswerPolicy policy = new EvidenceNoAnswerPolicy();

    @Test
    void requiresCitationValidatorEvidenceAndPreservesModelRefusalReason() {
        Citation citation = Citation.grounded(
                "chunk-1", 1L, "chunk-1", 0.9d, "evidence", -1, -1);

        assertEquals(
                new EvidenceDecision(QueryFinalState.NO_ANSWER, NoAnswerReason.UNVALIDATED_EVIDENCE),
                policy.afterGeneration(GeneratedAnswer.of(
                        "answer", List.of(citation), Map.of("validCitations", 0))));
        assertEquals(
                new EvidenceDecision(QueryFinalState.ANSWER, NoAnswerReason.NONE),
                policy.afterGeneration(GeneratedAnswer.of(
                        "answer", List.of(citation), Map.of("validCitations", 1))));
        assertEquals(
                new EvidenceDecision(QueryFinalState.NO_ANSWER, NoAnswerReason.MODEL_REFUSAL),
                policy.afterGeneration(GeneratedAnswer.of(
                        "cannot answer", List.of(citation), Map.of("status", "no_result", "validCitations", 1))));
    }

    @Test
    void preGenerationAdmissionRequiresAtLeastOneCanonicalRetrievedContext() {
        assertEquals(EvidenceAdmission.insufficient(), policy.beforeGeneration(List.of()));
        assertEquals(EvidenceAdmission.insufficient(), policy.beforeGeneration(null));
        assertEquals(
                EvidenceAdmission.proceed(),
                policy.beforeGeneration(List.of(new RetrievedContext(
                        "evidence", "chunk-1", 0.8f, Map.of()))));
    }
}
