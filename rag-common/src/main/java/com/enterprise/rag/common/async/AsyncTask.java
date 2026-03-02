package com.enterprise.rag.common.async;

import java.util.function.Consumer;

/**
 * 异步任务定义
 * <p>
 * 封装异步任务的执行逻辑和进度回调。
 *
 * @param <T> 任务结果类型
 */
@FunctionalInterface
public interface AsyncTask<T> {

    /**
     * 执行任务
     *
     * @param progressCallback 进度回调函数，接收 0-100 的进度值
     * @return 任务执行结果
     * @throws Exception 任务执行异常
     */
    T execute(Consumer<TaskProgress> progressCallback) throws Exception;

    /**
     * 获取任务类型
     * <p>
     * 默认返回类名，子类可覆盖。
     */
    default String getTaskType() {
        return this.getClass().getSimpleName();
    }

    /**
     * 任务进度信息
     */
    record TaskProgress(int progress, String message) {
        public static TaskProgress of(int progress, String message) {
            return new TaskProgress(Math.max(0, Math.min(100, progress)), message);
        }
    }
}
