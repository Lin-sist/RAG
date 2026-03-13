package com.enterprise.rag.admin.kb.dto;

/**
 * 文档上传响应 DTO
 */
public record DocumentUploadResponse(
        Long documentId,
        String taskId,
        String fileName,
        String fileType,
        String status) {
}
