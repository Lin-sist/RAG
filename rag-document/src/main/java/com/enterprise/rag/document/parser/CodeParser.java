package com.enterprise.rag.document.parser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代码文件解析器
 * 支持多种编程语言的源代码文件
 */
@Component
public class CodeParser implements DocumentParser {
    
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            // Java
            "java",
            // JavaScript/TypeScript
            "js", "jsx", "ts", "tsx",
            // Python
            "py",
            // Go
            "go",
            // Rust
            "rs",
            // C/C++
            "c", "cpp", "h", "hpp",
            // C#
            "cs",
            // Ruby
            "rb",
            // PHP
            "php",
            // Shell
            "sh", "bash",
            // SQL
            "sql",
            // YAML/JSON
            "yaml", "yml", "json",
            // XML
            "xml",
            // HTML/CSS
            "html", "htm", "css",
            // Kotlin
            "kt", "kts",
            // Scala
            "scala",
            // Swift
            "swift"
    );
    
    @Override
    public String parse(InputStream input) throws DocumentParseException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse code file", e);
        }
    }
    
    @Override
    public boolean supports(String fileType) {
        if (fileType == null) {
            return false;
        }
        return SUPPORTED_TYPES.contains(fileType.toLowerCase().trim());
    }
    
    @Override
    public String[] getSupportedTypes() {
        return SUPPORTED_TYPES.toArray(new String[0]);
    }
}
