package com.enterprise.rag.admin.kb.service;

import com.enterprise.rag.admin.kb.dto.DocumentUploadResponse;

/**
 * 文档索引应用服务
 * <p>
 * 负责文档上传后的完整索引编排：解析 → 向量化 → 写向量库 → 持久化分块 → 更新状态。
 * 控制器只需提交任务，不再承载任何业务编排逻辑。
 */
public interface DocumentIndexingService {

    /**
     * 提交文档索引任务（异步执行）
     *
     * @param kbId        知识库 ID
     * @param uploaderId  上传者用户 ID
     * @param fileContent 文件字节内容
     * @param fileName    原始文件名
     * @param title       可选标题（为 null 时取文件名）
     * @return 上传响应（含 documentId 和 taskId）
     */
    DocumentUploadResponse submitIndexing(Long kbId, Long uploaderId,
            byte[] fileContent, String fileName, String title);
}
