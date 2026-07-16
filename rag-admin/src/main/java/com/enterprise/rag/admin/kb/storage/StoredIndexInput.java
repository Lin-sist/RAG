package com.enterprise.rag.admin.kb.storage;

public record StoredIndexInput(
        String storageKey,
        long sizeBytes,
        String sha256) {
}
