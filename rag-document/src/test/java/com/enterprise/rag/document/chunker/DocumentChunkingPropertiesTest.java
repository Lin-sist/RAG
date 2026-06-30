package com.enterprise.rag.document.chunker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentChunkingPropertiesTest {

    @Test
    void defaultsShouldMatchExistingChunkConfigDefault() {
        DocumentChunkingProperties properties = new DocumentChunkingProperties();

        assertEquals(ChunkConfig.DEFAULT, properties.toChunkConfig());
    }

    @Test
    void shouldBindRuntimeChunkingProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("document.chunking.chunk-size", "640")
                .withProperty("document.chunking.chunk-overlap", "96")
                .withProperty("document.chunking.strategy", "semantic");

        DocumentChunkingProperties properties = Binder.get(environment)
                .bind("document.chunking", Bindable.of(DocumentChunkingProperties.class))
                .orElseThrow(() -> new AssertionError("document.chunking properties should bind"));

        assertEquals(new ChunkConfig(640, 96, ChunkStrategy.SEMANTIC), properties.toChunkConfig());
    }
}
