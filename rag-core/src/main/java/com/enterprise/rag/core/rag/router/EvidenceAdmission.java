package com.enterprise.rag.core.rag.router;

import java.util.Objects;

public record EvidenceAdmission(boolean generationAllowed, NoAnswerReason noAnswerReason) {

    public EvidenceAdmission {
        Objects.requireNonNull(noAnswerReason, "noAnswerReason");
        if (generationAllowed != (noAnswerReason == NoAnswerReason.NONE)) {
            throw new IllegalArgumentException("Evidence admission state is inconsistent");
        }
    }

    public static EvidenceAdmission proceed() {
        return new EvidenceAdmission(true, NoAnswerReason.NONE);
    }

    public static EvidenceAdmission insufficient() {
        return new EvidenceAdmission(false, NoAnswerReason.INSUFFICIENT_EVIDENCE);
    }
}
