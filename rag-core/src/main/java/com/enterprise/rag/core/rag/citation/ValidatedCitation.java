package com.enterprise.rag.core.rag.citation;

import com.enterprise.rag.core.rag.model.Citation;

/**
 * 通过校验后的 citation 及其证据匹配信息。
 */
public record ValidatedCitation(
        Citation citation,
        EvidenceMatch evidenceMatch) {
}
