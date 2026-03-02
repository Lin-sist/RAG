package com.enterprise.rag.document.processor;

import java.io.InputStream;
import java.util.Map;

/**
 * 文档输入
 *
 * @param inputStream 文档输入流
 * @param fileName    文件名
 * @param fileType    文件类型（扩展名）
 * @param metadata    元数据
 */
public record DocumentInput(
        InputStream inputStream,
        String fileName,
        String fileType,
        Map<String, Object> metadata
) {
    public DocumentInput {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }
        if (fileType == null || fileType.isBlank()) {
            throw new IllegalArgumentException("File type cannot be null or blank");
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
    
    /**
     * 从文件名提取文件类型
     */
    public static String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
    
    /**
     * 创建文档输入
     */
    public static DocumentInput of(InputStream inputStream, String fileName) {
        return new DocumentInput(
                inputStream,
                fileName,
                extractFileType(fileName),
                Map.of()
        );
    }
    
    /**
     * 创建带元数据的文档输入
     */
    public static DocumentInput of(InputStream inputStream, String fileName, Map<String, Object> metadata) {
        return new DocumentInput(
                inputStream,
                fileName,
                extractFileType(fileName),
                metadata
        );
    }
}
