package com.enterprise.rag.admin.kb.storage;

public enum IndexInputState {
    AVAILABLE,
    CLEANUP_PENDING,
    CLEANED,
    MISSING,
    CORRUPT
}
