package com.enterprise.rag.admin.kb.storage;

import com.enterprise.rag.common.exception.BusinessException;

public final class IndexInputStorageException extends BusinessException {

    private IndexInputStorageException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    static IndexInputStorageException writeFailed(Throwable cause) {
        return new IndexInputStorageException("INDEX_INPUT_WRITE_FAILED", "索引输入持久化失败", cause);
    }

    static IndexInputStorageException tooLarge() {
        return new IndexInputStorageException("INDEX_INPUT_TOO_LARGE", "索引输入超过允许大小", null);
    }

    static IndexInputStorageException unavailable(Throwable cause) {
        return new IndexInputStorageException("INDEX_INPUT_UNAVAILABLE", "索引输入不可用", cause);
    }

    public static IndexInputStorageException corrupt() {
        return new IndexInputStorageException("INDEX_INPUT_CORRUPT", "索引输入完整性校验失败", null);
    }

    private static IndexInputStorageException cleanupFailed(Throwable cause) {
        return new IndexInputStorageException("INDEX_INPUT_CLEANUP_FAILED", "索引输入清理失败", cause);
    }

    public static IndexInputStorageException cleanupFailed() {
        return cleanupFailed(null);
    }
}
