package com.enterprise.rag.document.parser;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Markdown 文档解析器
 * 使用 Flexmark 解析 Markdown 文件并提取纯文本
 */
@Component
public class MarkdownParser implements DocumentParser {
    
    private static final String[] SUPPORTED_TYPES = {"md", "markdown"};
    
    private final Parser parser;
    
    public MarkdownParser() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
    }
    
    @Override
    public String parse(InputStream input) throws DocumentParseException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String markdown = reader.lines().collect(Collectors.joining("\n"));
            Node document = parser.parse(markdown);
            // Extract plain text from AST
            return extractText(document);
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse Markdown document", e);
        }
    }
    
    /**
     * 从 Markdown AST 中提取纯文本
     */
    private String extractText(Node node) {
        StringBuilder sb = new StringBuilder();
        extractTextRecursive(node, sb);
        return sb.toString().trim();
    }
    
    private void extractTextRecursive(Node node, StringBuilder sb) {
        if (node.hasChildren()) {
            for (Node child : node.getChildren()) {
                extractTextRecursive(child, sb);
            }
        } else {
            String text = node.getChars().toString();
            if (!text.isBlank()) {
                sb.append(text);
                if (!text.endsWith("\n")) {
                    sb.append(" ");
                }
            }
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
