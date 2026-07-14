package com.enterprise.rag.admin.kb.service.impl;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.entity.DocumentStatus;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.core.rag.keyword.KeywordDocument;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rebuilds the in-memory BM25 route from persisted chunks after restart.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordIndexBootstrap {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentService documentService;
    private final KeywordIndex keywordIndex;

    @EventListener(ApplicationReadyEvent.class)
    public void rebuildKeywordIndex() {
        try {
            List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectList(null);
            for (KnowledgeBase knowledgeBase : knowledgeBases) {
                if (knowledgeBase.getVectorCollection() == null || knowledgeBase.getVectorCollection().isBlank()) {
                    continue;
                }
                rebuildCollection(knowledgeBase);
            }
        } catch (Exception e) {
            log.warn("关键词 BM25 索引启动重建失败，保留向量检索主链路: errorType={}",
                    e.getClass().getSimpleName());
        }
    }

    private void rebuildCollection(KnowledgeBase knowledgeBase) {
        List<KeywordDocument> keywordDocuments = new ArrayList<>();
        List<Document> documents = documentService.getByKnowledgeBaseId(knowledgeBase.getId());
        for (Document document : documents) {
            if (!DocumentStatus.COMPLETED.name().equalsIgnoreCase(document.getStatus())) {
                continue;
            }
            for (DocumentChunk chunk : documentService.getChunksByDocumentId(document.getId())) {
                if (chunk.getVectorId() == null || chunk.getVectorId().isBlank()) {
                    continue;
                }
                keywordDocuments.add(new KeywordDocument(
                        chunk.getVectorId(),
                        chunk.getContent(),
                        buildMetadata(knowledgeBase, document, chunk)));
            }
        }
        keywordIndex.rebuildCollection(knowledgeBase.getVectorCollection(), keywordDocuments);
        log.info("关键词 BM25 索引启动重建完成: kbId={}, collection={}, chunks={}",
                knowledgeBase.getId(), knowledgeBase.getVectorCollection(), keywordDocuments.size());
    }

    private Map<String, Object> buildMetadata(KnowledgeBase knowledgeBase, Document document, DocumentChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("documentId", document.getId());
        metadata.put("kbId", document.getKbId());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        metadata.put("startIndex", chunk.getStartPos());
        metadata.put("endIndex", chunk.getEndPos());
        metadata.put("documentTitle", document.getTitle());
        metadata.put("title", document.getTitle());
        metadata.put("sourceFileName", document.getTitle());
        metadata.put("fileName", document.getTitle());
        metadata.put("vectorCollection", knowledgeBase.getVectorCollection());
        return metadata;
    }
}
