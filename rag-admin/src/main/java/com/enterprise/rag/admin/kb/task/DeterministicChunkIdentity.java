package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.document.chunker.DocumentChunk;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.IntStream;

/**
 * C5b 新任务的稳定 chunk/vector identity；旧记录不迁移、不重写。
 */
public final class DeterministicChunkIdentity {

    public static final String CONTRACT_VERSION = "c5b-v1";

    private DeterministicChunkIdentity() {
    }

    public static List<DocumentChunk> remap(
            String contractVersion,
            Long documentId,
            String contentHash,
            List<DocumentChunk> chunks) {
        return IntStream.range(0, chunks.size())
                .mapToObj(index -> remapOne(contractVersion, documentId, contentHash, index, chunks.get(index)))
                .toList();
    }

    private static DocumentChunk remapOne(
            String contractVersion,
            Long documentId,
            String contentHash,
            int index,
            DocumentChunk chunk) {
        String contentDigest = sha256(chunk.content());
        String identity = String.join(":",
                contractVersion,
                String.valueOf(documentId),
                contentHash,
                String.valueOf(index),
                contentDigest);
        return new DocumentChunk(
                sha256(identity),
                chunk.content(),
                chunk.startIndex(),
                chunk.endIndex(),
                chunk.metadata());
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
