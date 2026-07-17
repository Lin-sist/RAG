package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.document.chunker.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DeterministicChunkIdentityTest {

    @Test
    void equivalentPreparedFactsProduceStableIdsWithoutUsingRandomSourceIds() {
        List<DocumentChunk> first = List.of(
                new DocumentChunk("random-a", "alpha", 0, 5, Map.of()),
                new DocumentChunk("random-b", "beta", 6, 10, Map.of()));
        List<DocumentChunk> second = List.of(
                new DocumentChunk("other-a", "alpha", 0, 5, Map.of()),
                new DocumentChunk("other-b", "beta", 6, 10, Map.of()));

        List<DocumentChunk> firstIds = DeterministicChunkIdentity.remap("c5b-v1", 99L, "content-hash", first);
        List<DocumentChunk> secondIds = DeterministicChunkIdentity.remap("c5b-v1", 99L, "content-hash", second);

        assertEquals(firstIds.stream().map(DocumentChunk::id).toList(),
                secondIds.stream().map(DocumentChunk::id).toList());
        assertNotEquals(firstIds.get(0).id(), firstIds.get(1).id());
        assertNotEquals(firstIds.get(0).id(),
                DeterministicChunkIdentity.remap("c5b-v2", 99L, "content-hash", first).get(0).id());
    }
}
