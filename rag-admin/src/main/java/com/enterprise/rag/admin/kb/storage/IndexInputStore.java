package com.enterprise.rag.admin.kb.storage;

import java.io.InputStream;

public interface IndexInputStore {

    StoredIndexInput put(long tenantId, InputStream input);

    InputStream openVerified(long tenantId, String storageKey, long expectedSizeBytes, String expectedSha256);

    DeleteResult delete(long tenantId, String storageKey);

    default StoredIndexInput put(InputStream input) {
        throw IndexInputStorageException.unavailable(null);
    }

    default InputStream openVerified(String storageKey, long expectedSizeBytes, String expectedSha256) {
        throw IndexInputStorageException.unavailable(null);
    }

    default DeleteResult delete(String storageKey) {
        throw IndexInputStorageException.unavailable(null);
    }

    enum DeleteResult {
        DELETED,
        ALREADY_MISSING,
        FAILED
    }
}
