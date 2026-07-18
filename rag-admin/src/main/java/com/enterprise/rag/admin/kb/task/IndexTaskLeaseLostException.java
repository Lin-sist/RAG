package com.enterprise.rag.admin.kb.task;

/**
 * 当前 worker 已不能证明仍持有 task lease。
 */
public class IndexTaskLeaseLostException extends RuntimeException {

    public IndexTaskLeaseLostException(String taskId) {
        super("Index task lease is no longer owned: " + taskId);
    }
}
