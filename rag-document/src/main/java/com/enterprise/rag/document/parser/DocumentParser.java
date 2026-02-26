package com.enterprise.rag.document.parser;

import java.io.InputStream;

/**
 * 文档解析器接口
 * 负责解析各类文档并提取文本内容
 */
public interface DocumentParser {
    
    /**
     * 解析文档并提取文本内容
     *
     * @param input 文档输入流
     * @return 提取的文本内容
     * @throws DocumentParseException 解析失败时抛出
     */
    String parse(InputStream input) throws DocumentParseException;
    
    /**
     * 检查是否支持指定的文件类型
     *
     * @param fileType 文件类型（扩展名，如 "pdf", "md", "docx"）
     * @return 是否支持
     */
    boolean supports(String fileType);
    
    /**
     * 获取支持的文件类型列表
     *
     * @return 支持的文件类型数组
     */
    String[] getSupportedTypes();
}
