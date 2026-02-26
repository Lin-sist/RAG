package com.enterprise.rag.document.parser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 纯文本文件解析器
 * 作为默认解析器处理 .txt 文件
 */
@Component
public class PlainTextParser implements DocumentParser {
    
    private static final String[] SUPPORTED_TYPES = {"txt", "text"};
    
    @Override
    public String parse(InputStream input) throws DocumentParseException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse text file", e);
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
