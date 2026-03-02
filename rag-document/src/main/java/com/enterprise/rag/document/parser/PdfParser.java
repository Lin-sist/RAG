package com.enterprise.rag.document.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * PDF 文档解析器
 * 使用 Apache PDFBox 解析 PDF 文件
 */
@Component
public class PdfParser implements DocumentParser {
    
    private static final String[] SUPPORTED_TYPES = {"pdf"};
    
    @Override
    public String parse(InputStream input) throws DocumentParseException {
        try {
            byte[] bytes = input.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse PDF document", e);
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
