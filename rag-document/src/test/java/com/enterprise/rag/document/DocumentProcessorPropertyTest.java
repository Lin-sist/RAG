package com.enterprise.rag.document;

import com.enterprise.rag.document.chunker.ChunkConfig;
import com.enterprise.rag.document.chunker.ChunkStrategy;
import com.enterprise.rag.document.chunker.DocumentChunk;
import com.enterprise.rag.document.chunker.DocumentChunker;
import com.enterprise.rag.document.parser.*;
import com.enterprise.rag.document.processor.DocumentInput;
import com.enterprise.rag.document.processor.DocumentProcessorImpl;
import com.enterprise.rag.document.processor.ProcessResult;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文档处理属性测试
 * 
 * Feature: enterprise-rag-qa-system
 * 
 * Property 5: 文档解析完整性
 * Property 6: 文档分块覆盖性
 * Property 8: 文档上传幂等性
 * 
 * Validates: Requirements 2.1, 2.2, 2.6
 */
class DocumentProcessorPropertyTest {

    private final DocumentChunker chunker = new DocumentChunker();
    private final DocumentParserFactory parserFactory;
    private final DocumentProcessorImpl processor;

    DocumentProcessorPropertyTest() {
        List<DocumentParser> parsers = List.of(
                new PlainTextParser(),
                new MarkdownParser(),
                new CodeParser()
        );
        this.parserFactory = new DocumentParserFactory(parsers);
        this.processor = new DocumentProcessorImpl(parserFactory, chunker);
    }

    // ==================== Property 5: 文档解析完整性 ====================

    /**
     * Property 5: 文档解析完整性
     * 
     * *For any* 支持格式的文档文件（PDF/Markdown/Word/代码），解析后提取的文本应包含文档的主要内容。
     * 
     * 测试策略：
     * 1. 生成随机文本内容（ASCII字符，避免编码问题）
     * 2. 使用 PlainTextParser 解析
     * 3. 验证解析结果包含原始内容
     * 
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void plainTextParserShouldPreserveContent(
            @ForAll("asciiContent") String content) {
        
        PlainTextParser parser = new PlainTextParser();
        ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        
        String parsed = parser.parse(input);
        
        // 验证解析结果包含原始内容
        assertThat(parsed).isEqualTo(content);
    }

    @Provide
    Arbitrary<String> asciiContent() {
        // 使用ASCII字符生成内容，避免Unicode编码问题
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',', '!', '?')
                .ofMinLength(10)
                .ofMaxLength(1000);
    }

    /**
     * Property 5: Markdown 解析完整性
     * 
     * 验证 Markdown 解析器能提取文本内容
     * 
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void markdownParserShouldExtractTextContent(
            @ForAll("markdownContent") String markdown) {
        
        MarkdownParser parser = new MarkdownParser();
        ByteArrayInputStream input = new ByteArrayInputStream(markdown.getBytes(StandardCharsets.UTF_8));
        
        String parsed = parser.parse(input);
        
        // 验证解析结果非空
        assertThat(parsed).isNotNull();
        assertThat(parsed.trim()).isNotEmpty();
    }

    @Provide
    Arbitrary<String> markdownContent() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(5)
                .ofMaxLength(100)
                .map(text -> "# Heading\n\n" + text + "\n\n## Section\n\n" + text);
    }

    /**
     * Property 5: 代码解析完整性
     * 
     * 验证代码解析器能完整保留代码内容
     * 
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    void codeParserShouldPreserveCodeContent(
            @ForAll("javaCode") String code) {
        
        CodeParser parser = new CodeParser();
        ByteArrayInputStream input = new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8));
        
        String parsed = parser.parse(input);
        
        // 验证解析结果与原始代码相同
        assertThat(parsed).isEqualTo(code);
    }

    @Provide
    Arbitrary<String> javaCode() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(50)
                .map(name -> "public class " + name + " {\n    public void method() {\n        // code\n    }\n}");
    }

    // ==================== Property 6: 文档分块覆盖性 ====================

    /**
     * Property 6: 文档分块覆盖性
     * 
     * *For any* 文档内容，分块后所有块的内容拼接应能覆盖原始内容（考虑重叠部分），
     * 且每个块大小不超过配置的最大值。
     * 
     * 测试策略：
     * 1. 生成随机文本内容
     * 2. 使用配置的块大小和重叠进行分块
     * 3. 验证每个块大小不超过配置值
     * 4. 验证所有块覆盖原始内容
     * 
     * **Validates: Requirements 2.2**
     */
    @Property(tries = 100)
    void chunksShouldCoverOriginalContent(
            @ForAll @StringLength(min = 100, max = 2000) String content,
            @ForAll @IntRange(min = 100, max = 500) int chunkSize,
            @ForAll @IntRange(min = 10, max = 50) int overlap) {
        
        // 确保 overlap < chunkSize
        Assume.that(overlap < chunkSize);
        
        ChunkConfig config = new ChunkConfig(chunkSize, overlap, ChunkStrategy.FIXED_SIZE);
        List<DocumentChunk> chunks = chunker.chunk(content, config);
        
        // 验证至少有一个块
        assertThat(chunks).isNotEmpty();
        
        // 验证每个块大小不超过配置
        for (DocumentChunk chunk : chunks) {
            assertThat(chunk.content().length())
                    .as("Chunk size should not exceed configured max")
                    .isLessThanOrEqualTo(chunkSize);
        }
        
        // 验证块覆盖原始内容
        // 检查原始内容的开头和结尾是否被覆盖
        String firstChunkContent = chunks.get(0).content();
        String lastChunkContent = chunks.get(chunks.size() - 1).content();
        
        assertThat(content.startsWith(firstChunkContent.substring(0, Math.min(50, firstChunkContent.length()))))
                .as("First chunk should cover beginning of content")
                .isTrue();
        
        // 验证最后一个块覆盖内容结尾
        String contentEnd = content.substring(Math.max(0, content.length() - 50));
        String lastChunkEnd = lastChunkContent.substring(Math.max(0, lastChunkContent.length() - 50));
        assertThat(contentEnd.contains(lastChunkEnd) || lastChunkEnd.contains(contentEnd))
                .as("Last chunk should cover end of content")
                .isTrue();
    }

    /**
     * Property 6: 语义分块覆盖性
     * 
     * 验证语义分块策略的覆盖性
     * 
     * **Validates: Requirements 2.2**
     */
    @Property(tries = 100)
    void semanticChunksShouldCoverContent(
            @ForAll("paragraphContent") String content,
            @ForAll @IntRange(min = 200, max = 800) int chunkSize) {
        
        ChunkConfig config = new ChunkConfig(chunkSize, 50, ChunkStrategy.SEMANTIC);
        List<DocumentChunk> chunks = chunker.chunk(content, config);
        
        // 验证至少有一个块
        assertThat(chunks).isNotEmpty();
        
        // 验证每个块大小不超过配置（允许一定的弹性）
        for (DocumentChunk chunk : chunks) {
            // 语义分块可能略微超过配置大小以保持语义完整性
            assertThat(chunk.content().length())
                    .as("Chunk size should be reasonable")
                    .isLessThanOrEqualTo(chunkSize * 2);
        }
        
        // 验证所有块的内容都来自原始内容
        for (DocumentChunk chunk : chunks) {
            String chunkContent = chunk.content().trim();
            // 检查块内容的关键部分是否在原始内容中
            if (chunkContent.length() > 20) {
                String sample = chunkContent.substring(0, 20);
                assertThat(content.contains(sample))
                        .as("Chunk content should come from original content")
                        .isTrue();
            }
        }
    }

    @Provide
    Arbitrary<String> paragraphContent() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(20)
                .ofMaxLength(100)
                .list()
                .ofMinSize(3)
                .ofMaxSize(10)
                .map(paragraphs -> String.join("\n\n", paragraphs));
    }

    /**
     * Property 6: 块索引连续性
     * 
     * 验证分块后的索引是连续的
     * 
     * **Validates: Requirements 2.2**
     */
    @Property(tries = 100)
    void chunkIndicesShouldBeContinuous(
            @ForAll @StringLength(min = 200, max = 1000) String content,
            @ForAll @IntRange(min = 50, max = 200) int chunkSize) {
        
        ChunkConfig config = new ChunkConfig(chunkSize, 20, ChunkStrategy.FIXED_SIZE);
        List<DocumentChunk> chunks = chunker.chunk(content, config);
        
        // 验证块索引连续
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            Object chunkIndex = chunk.metadata().get("chunkIndex");
            assertThat(chunkIndex)
                    .as("Chunk should have index metadata")
                    .isNotNull();
            assertThat((Integer) chunkIndex)
                    .as("Chunk index should be continuous")
                    .isEqualTo(i);
        }
    }

    // ==================== Property 8: 文档上传幂等性 ====================

    /**
     * Property 8: 文档上传幂等性
     * 
     * *For any* 文档，重复上传相同内容的文档（相同 content hash）不应产生重复的索引数据。
     * 
     * 测试策略：
     * 1. 生成随机文本内容
     * 2. 首次处理文档
     * 3. 使用相同内容再次处理
     * 4. 验证第二次处理返回重复标记
     * 
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    void duplicateDocumentsShouldBeDetected(
            @ForAll("nonEmptyAlphanumericContent") String content) {
        
        // 清除之前的记录
        processor.clearProcessedHashes();
        
        ByteArrayInputStream input1 = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        DocumentInput docInput1 = new DocumentInput(input1, "test.txt", "txt", null);
        
        // 首次处理
        ProcessResult result1 = processor.process(docInput1);
        
        // 验证首次处理是新文档
        assertThat(result1.isNew())
                .as("First upload should be marked as new")
                .isTrue();
        assertThat(result1.rawContent())
                .as("First upload should have raw content")
                .isEqualTo(content);
        assertThat(result1.chunks())
                .as("First upload should have chunks")
                .isNotEmpty();
        
        // 使用相同内容再次处理
        ByteArrayInputStream input2 = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        DocumentInput docInput2 = new DocumentInput(input2, "test2.txt", "txt", null);
        
        ProcessResult result2 = processor.process(docInput2);
        
        // 验证第二次处理是重复文档
        assertThat(result2.isNew())
                .as("Second upload should be marked as duplicate")
                .isFalse();
        assertThat(result2.contentHash())
                .as("Content hash should be the same")
                .isEqualTo(result1.contentHash());
        assertThat(result2.chunks())
                .as("Duplicate upload should have empty chunks")
                .isEmpty();
    }

    @Provide
    Arbitrary<String> nonEmptyAlphanumericContent() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(50)
                .ofMaxLength(500);
    }

    /**
     * Property 8: 内容哈希一致性
     * 
     * 验证相同内容产生相同的哈希值
     * 
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    void sameContentShouldProduceSameHash(
            @ForAll @StringLength(min = 10, max = 500) String content) {
        
        String hash1 = processor.computeHash(content);
        String hash2 = processor.computeHash(content);
        
        assertThat(hash1)
                .as("Same content should produce same hash")
                .isEqualTo(hash2);
        
        // 验证哈希非空
        assertThat(hash1)
                .as("Hash should not be empty")
                .isNotEmpty();
    }

    /**
     * Property 8: 不同内容产生不同哈希
     * 
     * 验证不同内容产生不同的哈希值
     * 
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    void differentContentShouldProduceDifferentHash(
            @ForAll @StringLength(min = 10, max = 200) String content1,
            @ForAll @StringLength(min = 10, max = 200) String content2) {
        
        Assume.that(!content1.equals(content2));
        
        String hash1 = processor.computeHash(content1);
        String hash2 = processor.computeHash(content2);
        
        assertThat(hash1)
                .as("Different content should produce different hash")
                .isNotEqualTo(hash2);
    }

    /**
     * Property 8: exists 方法正确性
     * 
     * 验证 exists 方法在处理前后返回正确状态
     * 
     * **Validates: Requirements 2.6**
     */
    @Property(tries = 100)
    void existsShouldReturnCorrectStatus(
            @ForAll("nonEmptyAlphanumericContent") String content) {
        
        // 清除之前的记录
        processor.clearProcessedHashes();
        
        // 处理文档
        ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        DocumentInput docInput = new DocumentInput(input, "test.txt", "txt", null);
        ProcessResult result = processor.process(docInput);
        
        // 处理后存在
        assertThat(processor.exists(result.contentHash()))
                .as("Hash should exist after processing")
                .isTrue();
        
        // 不同的哈希不存在
        String differentHash = processor.computeHash(content + "different");
        assertThat(processor.exists(differentHash))
                .as("Different hash should not exist")
                .isFalse();
    }

    // ==================== 辅助断言类 ====================

    private static <T> ObjectAssert<T> assertThat(T actual) {
        return new ObjectAssert<>(actual);
    }

    private static BooleanAssert assertThat(boolean actual) {
        return new BooleanAssert(actual);
    }

    private static IntAssert assertThat(int actual) {
        return new IntAssert(actual);
    }

    private static ListAssert assertThat(List<?> actual) {
        return new ListAssert(actual);
    }

    private static class ObjectAssert<T> {
        private final T actual;
        private String description;

        ObjectAssert(T actual) {
            this.actual = actual;
        }

        ObjectAssert<T> as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isNull() {
            if (actual != null) {
                throw new AssertionError(msg("Expected null but was " + actual));
            }
        }

        void isNotNull() {
            if (actual == null) {
                throw new AssertionError(msg("Expected non-null but was null"));
            }
        }

        void isEqualTo(T expected) {
            if (actual == null && expected == null) return;
            if (actual == null || !actual.equals(expected)) {
                throw new AssertionError(msg("Expected " + expected + " but was " + actual));
            }
        }

        void isNotEqualTo(T expected) {
            if (actual == expected || (actual != null && actual.equals(expected))) {
                throw new AssertionError(msg("Expected not equal to " + expected + " but was " + actual));
            }
        }

        void isNotEmpty() {
            if (actual == null || actual.toString().isEmpty()) {
                throw new AssertionError(msg("Expected non-empty but was empty or null"));
            }
        }

        private String msg(String defaultMsg) {
            return description != null ? description + ": " + defaultMsg : defaultMsg;
        }
    }

    private static class BooleanAssert {
        private final boolean actual;
        private String description;

        BooleanAssert(boolean actual) {
            this.actual = actual;
        }

        BooleanAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isTrue() {
            if (!actual) {
                throw new AssertionError(msg("Expected true but was false"));
            }
        }

        void isFalse() {
            if (actual) {
                throw new AssertionError(msg("Expected false but was true"));
            }
        }

        private String msg(String defaultMsg) {
            return description != null ? description + ": " + defaultMsg : defaultMsg;
        }
    }

    private static class IntAssert {
        private final int actual;
        private String description;

        IntAssert(int actual) {
            this.actual = actual;
        }

        IntAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isEqualTo(int expected) {
            if (actual != expected) {
                throw new AssertionError(msg("Expected " + expected + " but was " + actual));
            }
        }

        void isLessThanOrEqualTo(int expected) {
            if (actual > expected) {
                throw new AssertionError(msg("Expected <= " + expected + " but was " + actual));
            }
        }

        private String msg(String defaultMsg) {
            return description != null ? description + ": " + defaultMsg : defaultMsg;
        }
    }

    private static class ListAssert {
        private final List<?> actual;
        private String description;

        ListAssert(List<?> actual) {
            this.actual = actual;
        }

        ListAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isNotEmpty() {
            if (actual == null || actual.isEmpty()) {
                throw new AssertionError(msg("Expected non-empty list but was empty or null"));
            }
        }

        void isEmpty() {
            if (actual != null && !actual.isEmpty()) {
                throw new AssertionError(msg("Expected empty list but had " + actual.size() + " elements"));
            }
        }

        private String msg(String defaultMsg) {
            return description != null ? description + ": " + defaultMsg : defaultMsg;
        }
    }
}
