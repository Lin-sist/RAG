package com.enterprise.rag.document.processor;

import com.enterprise.rag.document.chunker.ChunkConfig;
import com.enterprise.rag.document.chunker.DocumentChunk;
import com.enterprise.rag.document.chunker.DocumentChunker;
import com.enterprise.rag.document.parser.DocumentParseException;
import com.enterprise.rag.document.parser.DocumentParser;
import com.enterprise.rag.document.parser.DocumentParserFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档处理器实现
 * 协调解析、分块流程，实现幂等性检查
 */
@Service
public class DocumentProcessorImpl implements DocumentProcessor {
    
    private final DocumentParserFactory parserFactory;
    private final DocumentChunker chunker;
    
    // 内存存储已处理文档的哈希（生产环境应使用 Redis 或数据库）
    private final Map<String, String> processedHashes = new ConcurrentHashMap<>();
    
    public DocumentProcessorImpl(DocumentParserFactory parserFactory, DocumentChunker chunker) {
        this.parserFactory = parserFactory;
        this.chunker = chunker;
    }
    
    @Override
    public ProcessResult process(DocumentInput input) {
        return process(input, ChunkConfig.DEFAULT);
    }
    
    @Override
    public ProcessResult process(DocumentInput input, ChunkConfig config) {
        // 1. 获取解析器
        DocumentParser parser = parserFactory.getParser(input.fileType())
                .orElseThrow(() -> new DocumentParseException(
                        "Unsupported file type: " + input.fileType()));
        
        // 2. 读取输入流内容（需要多次使用）
        byte[] content;
        try {
            content = input.inputStream().readAllBytes();
        } catch (IOException e) {
            throw new DocumentParseException("Failed to read document content", e);
        }
        
        // 3. 解析文档
        String rawContent;
        try (InputStream is = new ByteArrayInputStream(content)) {
            rawContent = parser.parse(is);
        } catch (IOException e) {
            throw new DocumentParseException("Failed to parse document", e);
        }
        
        // 4. 计算内容哈希
        String contentHash = computeHash(rawContent);
        
        // 5. 幂等性检查
        if (exists(contentHash)) {
            String existingDocId = processedHashes.get(contentHash);
            return ProcessResult.duplicate(existingDocId, contentHash);
        }
        
        // 6. 分块
        List<DocumentChunk> chunks = chunk(rawContent, config);
        
        // 7. 生成文档 ID 并记录
        String documentId = UUID.randomUUID().toString();
        processedHashes.put(contentHash, documentId);
        
        return ProcessResult.newDocument(documentId, contentHash, rawContent, chunks);
    }
    
    @Override
    public List<DocumentChunk> chunk(String content, ChunkConfig config) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        return chunker.chunk(content, config);
    }
    
    @Override
    public boolean exists(String contentHash) {
        return processedHashes.containsKey(contentHash);
    }
    
    @Override
    public String computeHash(String content) {
        if (content == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * 清除已处理的哈希记录（用于测试）
     */
    public void clearProcessedHashes() {
        processedHashes.clear();
    }
    
    /**
     * 手动添加哈希记录（用于测试）
     */
    public void addProcessedHash(String hash, String documentId) {
        processedHashes.put(hash, documentId);
    }
}
