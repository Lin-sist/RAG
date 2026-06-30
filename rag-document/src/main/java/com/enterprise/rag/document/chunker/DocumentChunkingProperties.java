package com.enterprise.rag.document.chunker;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Runtime document chunking defaults used by the upload/indexing pipeline.
 */
@Data
@Component
@ConfigurationProperties(prefix = "document.chunking")
public class DocumentChunkingProperties {

    private int chunkSize = ChunkConfig.DEFAULT.chunkSize();
    private int chunkOverlap = ChunkConfig.DEFAULT.chunkOverlap();
    private ChunkStrategy strategy = ChunkConfig.DEFAULT.strategy();

    public ChunkConfig toChunkConfig() {
        return new ChunkConfig(chunkSize, chunkOverlap, strategy);
    }
}
