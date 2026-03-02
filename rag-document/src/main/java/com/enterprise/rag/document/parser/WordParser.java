package com.enterprise.rag.document.parser;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Word 文档解析器
 * 使用 Apache POI 解析 Word 文件（.docx）
 */
@Component
public class WordParser implements DocumentParser {
    
    private static final String[] SUPPORTED_TYPES = {"docx", "doc"};
    
    @Override
    public String parse(InputStream input) throws DocumentParseException {
        try (XWPFDocument document = new XWPFDocument(input);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse Word document", e);
        }
    }
    
    @Override
    public boolean supports(String fileType) {
        if (fileType == null) {
            return false;
        }
        String type = fileType.toLowerCase().trim();
        for (String supported : SUPPORTED_TYPES) {
            if (supported.equals(type)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String[] getSupportedTypes() {
        return SUPPORTED_TYPES.clone();
    }
}
