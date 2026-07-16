package com.enterprise.rag.admin.kb.storage;

import java.io.InputStream;

public interface IndexInputStore {

    StoredIndexInput put(InputStream input);

    InputStream openVerified(String storageKey, long expectedSizeBytes, String expectedSha256);

    DeleteResult delete(String storageKey);

    enum DeleteResult {
        DELETED,
        ALREADY_MISSING,
        FAILED
    }
}
