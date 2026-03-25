package com.enterprise.rag.document.chunker;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 文档分块器
 * 将文档内容按配置的策略分割成多个块
 */
@Component
public class DocumentChunker {
    
    // 段落分隔符模式
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\s*\\n");
    // 句子分隔符模式
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?。！？])\\s+");
    // 代码块分隔符模式（函数、类定义等）
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "(?=\\n(?:public|private|protected|class|interface|def|function|func|fn)\\s)"
    );
    
    /**
     * 将文档内容分块
     *
     * @param content 文档内容
     * @param config  分块配置
     * @return 文档块列表
     */
    public List<DocumentChunk> chunk(String content, ChunkConfig config) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        
        return switch (config.strategy()) {
            case FIXED_SIZE -> chunkByFixedSize(content, config);
            case SEMANTIC -> chunkBySemantic(content, config);
            case CODE -> chunkByCode(content, config);
        };
    }
    
    /**
     * 使用默认配置分块
     */
    public List<DocumentChunk> chunk(String content) {
        return chunk(content, ChunkConfig.DEFAULT);
    }
    
    /**
     * 固定大小分块
     */
    private List<DocumentChunk> chunkByFixedSize(String content, ChunkConfig config) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkSize = config.chunkSize();
        int overlap = config.chunkOverlap();
        int step = chunkSize - overlap;
        
        int index = 0;
        int position = 0;
        
        while (position < content.length()) {
            int endPos = Math.min(position + chunkSize, content.length());
            String chunkContent = content.substring(position, endPos);
            
            chunks.add(new DocumentChunk(
                    generateChunkId(),
                    chunkContent,
                    position,
                    endPos,
                    Map.of("chunkIndex", index)
            ));
            
            position += step;
            index++;
        }
        
        return chunks;
    }
    
    /**
     * 语义分块（按段落和句子边界）
     */
    private List<DocumentChunk> chunkBySemantic(String content, ChunkConfig config) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkSize = config.chunkSize();
        int overlap = config.chunkOverlap();
        
        // 首先按段落分割
        String[] paragraphs = PARAGRAPH_PATTERN.split(content);
        
        StringBuilder currentChunk = new StringBuilder();
        int currentStart = 0;
        int position = 0;
        int index = 0;
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                position += 2; // 段落分隔符
                continue;
            }
            
            // 如果当前块加上新段落超过大小限制
            if (currentChunk.length() + paragraph.length() + 1 > chunkSize && currentChunk.length() > 0) {
                // 保存当前块
                chunks.add(createChunk(currentChunk.toString().trim(), currentStart, position, index++));
                
                // 计算重叠部分
                String overlapText = getOverlapText(currentChunk.toString(), overlap);
                currentChunk = new StringBuilder(overlapText);
                currentStart = position - overlapText.length();
            }
            
            // 如果单个段落超过大小限制，按句子分割
            if (paragraph.length() > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(currentChunk.toString().trim(), currentStart, position, index++));
                    currentChunk = new StringBuilder();
                }
                
                List<DocumentChunk> sentenceChunks = chunkBySentence(paragraph, position, config, index);
                chunks.addAll(sentenceChunks);
                index += sentenceChunks.size();
                currentStart = position + paragraph.length();
            } else {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            }
            
            position += paragraph.length() + 2; // 段落内容 + 分隔符
        }
        
        // 添加最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(currentChunk.toString().trim(), currentStart, content.length(), index));
        }
        
        return chunks;
    }
    
    /**
     * 按句子分块
     */
    private List<DocumentChunk> chunkBySentence(String text, int basePosition, ChunkConfig config, int startIndex) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] sentences = SENTENCE_PATTERN.split(text);
        
        StringBuilder currentChunk = new StringBuilder();
        int currentStart = basePosition;
        int position = basePosition;
        int index = startIndex;
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) {
                continue;
            }

            // Markdown 里的长表格行、长代码行或无标点大段文本，可能整个段落都被视为一个“句子”。
            // 这时退回固定大小切分，避免生成超大 chunk 拖垮后续 embedding / 向量写入。
            if (sentence.length() > config.chunkSize()) {
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(currentChunk.toString().trim(), currentStart, position, index++));
                    currentChunk = new StringBuilder();
                }

                List<DocumentChunk> oversizedChunks = splitOversizedText(sentence, position, config, index);
                chunks.addAll(oversizedChunks);
                index += oversizedChunks.size();
                position += sentence.length() + 1;
                currentStart = position;
                continue;
            }
            
            if (currentChunk.length() + sentence.length() + 1 > config.chunkSize() && currentChunk.length() > 0) {
                chunks.add(createChunk(currentChunk.toString().trim(), currentStart, position, index++));
                
                String overlapText = getOverlapText(currentChunk.toString(), config.chunkOverlap());
                currentChunk = new StringBuilder(overlapText);
                currentStart = position - overlapText.length();
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence);
            position += sentence.length() + 1;
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(currentChunk.toString().trim(), currentStart, basePosition + text.length(), index));
        }
        
        return chunks;
    }

    /**
     * 将超长文本回退为固定大小分块，并保留语义分块中的基准位置信息。
     */
    private List<DocumentChunk> splitOversizedText(String text, int basePosition, ChunkConfig config, int startIndex) {
        List<DocumentChunk> fixedChunks = chunkByFixedSize(text, ChunkConfig.fixedSize(config.chunkSize(), config.chunkOverlap()));
        List<DocumentChunk> rebasedChunks = new ArrayList<>(fixedChunks.size());

        for (int i = 0; i < fixedChunks.size(); i++) {
            DocumentChunk fixedChunk = fixedChunks.get(i);
            rebasedChunks.add(new DocumentChunk(
                    fixedChunk.id(),
                    fixedChunk.content(),
                    basePosition + fixedChunk.startIndex(),
                    basePosition + fixedChunk.endIndex(),
                    Map.of("chunkIndex", startIndex + i)
            ));
        }

        return rebasedChunks;
    }
    
    /**
     * 代码分块（按代码结构）
     */
    private List<DocumentChunk> chunkByCode(String content, ChunkConfig config) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] blocks = CODE_BLOCK_PATTERN.split(content);
        
        StringBuilder currentChunk = new StringBuilder();
        int currentStart = 0;
        int position = 0;
        int index = 0;
        
        for (String block : blocks) {
            if (block.isEmpty()) {
                continue;
            }
            
            if (currentChunk.length() + block.length() > config.chunkSize() && currentChunk.length() > 0) {
                chunks.add(createChunk(currentChunk.toString().trim(), currentStart, position, index++));
                
                String overlapText = getOverlapText(currentChunk.toString(), config.chunkOverlap());
                currentChunk = new StringBuilder(overlapText);
                currentStart = position - overlapText.length();
            }
            
            // 如果单个代码块超过大小限制，使用固定大小分块
            if (block.length() > config.chunkSize()) {
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(currentChunk.toString().trim(), currentStart, position, index++));
                    currentChunk = new StringBuilder();
                }
                
                List<DocumentChunk> subChunks = chunkByFixedSize(block, config);
                for (DocumentChunk subChunk : subChunks) {
                    chunks.add(new DocumentChunk(
                            subChunk.id(),
                            subChunk.content(),
                            position + subChunk.startIndex(),
                            position + subChunk.endIndex(),
                            Map.of("chunkIndex", index++)
                    ));
                }
                currentStart = position + block.length();
            } else {
                currentChunk.append(block);
            }
            
            position += block.length();
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(currentChunk.toString().trim(), currentStart, content.length(), index));
        }
        
        return chunks;
    }
    
    /**
     * 获取重叠文本
     */
    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) {
            return text;
        }
        return text.substring(text.length() - overlapSize);
    }
    
    /**
     * 创建文档块
     */
    private DocumentChunk createChunk(String content, int startIndex, int endIndex, int chunkIndex) {
        return new DocumentChunk(
                generateChunkId(),
                content,
                startIndex,
                endIndex,
                Map.of("chunkIndex", chunkIndex)
        );
    }
    
    /**
     * 生成块 ID
     */
    private String generateChunkId() {
        return UUID.randomUUID().toString();
    }
}
