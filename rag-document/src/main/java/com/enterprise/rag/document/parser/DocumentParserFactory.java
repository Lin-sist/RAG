package com.enterprise.rag.document.parser;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文档解析器工厂
 * 根据文件类型选择合适的解析器
 */
@Component
public class DocumentParserFactory {
    
    private final List<DocumentParser> parsers;
    
    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }
    
    /**
     * 根据文件类型获取解析器
     *
     * @param fileType 文件类型（扩展名）
     * @return 解析器（如果支持）
     */
    public Optional<DocumentParser> getParser(String fileType) {
        if (fileType == null) {
            return Optional.empty();
        }
        return parsers.stream()
                .filter(parser -> parser.supports(fileType))
                .findFirst();
    }
    
    /**
     * 检查是否支持指定的文件类型
     *
     * @param fileType 文件类型
     * @return 是否支持
     */
    public boolean isSupported(String fileType) {
        return getParser(fileType).isPresent();
    }
    
    /**
     * 获取所有支持的文件类型
     *
     * @return 支持的文件类型集合
     */
    public Set<String> getSupportedTypes() {
        return parsers.stream()
                .flatMap(parser -> java.util.Arrays.stream(parser.getSupportedTypes()))
                .collect(Collectors.toSet());
    }
}
