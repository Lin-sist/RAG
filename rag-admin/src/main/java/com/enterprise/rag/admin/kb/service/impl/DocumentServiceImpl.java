package com.enterprise.rag.admin.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.storage.IndexInputStore;
import com.enterprise.rag.admin.kb.storage.IndexInputStorageException;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 文档服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final VectorStore vectorStore;
    private final KeywordIndex keywordIndex;
    private final IndexInputStore indexInputStore;

    @Override
    @Transactional
    public Document create(Document document) {
        documentMapper.insert(document);
        return document;
    }

    @Override
    public Optional<Document> getById(Long id) {
        return Optional.ofNullable(documentMapper.selectById(id));
    }

    @Override
    public Optional<Document> getById(long tenantId, Long id) {
        return Optional.ofNullable(documentMapper.selectByTenantAndId(tenantId, id));
    }

    @Override
    public List<Document> getByKnowledgeBaseId(Long kbId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public List<Document> getByKnowledgeBaseId(long tenantId, Long kbId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getTenantId, tenantId)
                .eq(Document::getKbId, kbId)
                .orderByDesc(Document::getCreatedAt);
        return documentMapper.selectList(wrapper).stream()
                .filter(document -> document.getTenantId() != null
                        && document.getTenantId() == tenantId)
                .toList();
    }

    @Override
    public Optional<Document> getByContentHash(String contentHash) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getContentHash, contentHash);
        return Optional.ofNullable(documentMapper.selectOne(wrapper));
    }

    @Override
    public Optional<Document> getByKnowledgeBaseAndContentHash(Long kbId, String contentHash) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKbId, kbId)
                .eq(Document::getContentHash, contentHash)
                .last("LIMIT 1");
        return Optional.ofNullable(documentMapper.selectOne(wrapper));
    }

    @Override
    public Optional<Document> getByKnowledgeBaseAndContentHash(
            long tenantId, Long kbId, String contentHash) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getTenantId, tenantId)
                .eq(Document::getKbId, kbId)
                .eq(Document::getContentHash, contentHash)
                .last("LIMIT 1");
        Document document = documentMapper.selectOne(wrapper);
        if (document == null || !Long.valueOf(tenantId).equals(document.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(document);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    public void updateStatus(long tenantId, Long id, String status) {
        LambdaUpdateWrapper<Document> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Document::getTenantId, tenantId)
                .eq(Document::getId, id)
                .set(Document::getStatus, status);
        documentMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void updateChunkCount(Long id, int chunkCount) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    public void updateChunkCount(long tenantId, Long id, int chunkCount) {
        LambdaUpdateWrapper<Document> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Document::getTenantId, tenantId)
                .eq(Document::getId, id)
                .set(Document::getChunkCount, chunkCount);
        documentMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void updateContentHash(Long id, String contentHash) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    public void updateContentHash(long tenantId, Long id, String contentHash) {
        LambdaUpdateWrapper<Document> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Document::getTenantId, tenantId)
                .eq(Document::getId, id)
                .set(Document::getContentHash, contentHash);
        documentMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void updateInputState(Long id, String inputState) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    public void updateInputState(long tenantId, Long id, String inputState) {
        LambdaUpdateWrapper<Document> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Document::getTenantId, tenantId)
                .eq(Document::getId, id)
                .set(Document::getInputState, inputState);
        documentMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    public boolean delete(long tenantId, Long id) {
        Optional<Document> docOpt = getById(tenantId, id);
        if (docOpt.isEmpty()) {
            return false;
        }

        Document document = docOpt.get();
        List<DocumentChunk> chunks = chunkMapper.selectByTenantAndDocumentId(tenantId, id);
        List<String> vectorIds = chunks.stream()
                .map(DocumentChunk::getVectorId)
                .filter(vectorId -> vectorId != null && !vectorId.isEmpty())
                .toList();

        if (!vectorIds.isEmpty()) {
            KnowledgeBase kb = knowledgeBaseMapper.selectByTenantAndId(tenantId, document.getKbId());
            if (kb != null && kb.getVectorCollection() != null) {
                vectorStore.delete(kb.getVectorCollection(), vectorIds);
                keywordIndex.delete(kb.getVectorCollection(), vectorIds);
                log.info("Deleted tenant-scoped vectors for document");
            } else {
                log.warn("向量删除前无法确认 tenant 内知识库集合，拒绝继续删除文档: tenantId={}, kbId={}, documentId={}",
                        tenantId, document.getKbId(), id);
                throw VectorDependencyException.indexUnavailable("delete", null);
            }
        }

        if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
            IndexInputStore.DeleteResult deleteResult = indexInputStore.delete(tenantId, document.getFilePath());
            if (deleteResult != IndexInputStore.DeleteResult.DELETED
                    && deleteResult != IndexInputStore.DeleteResult.ALREADY_MISSING) {
                throw IndexInputStorageException.cleanupFailed();
            }
        }

        chunkMapper.deleteByTenantAndDocumentId(tenantId, id);
        return documentMapper.deleteByTenantAndId(tenantId, id) > 0;
    }

    @Override
    @Transactional
    public void deleteByKnowledgeBaseId(Long kbId) {
        List<Document> documents = getByKnowledgeBaseId(kbId);
        for (Document document : documents) {
            delete(document.getId());
        }
    }

    @Override
    @Transactional
    public void deleteByKnowledgeBaseId(long tenantId, Long kbId) {
        List<Document> documents = getByKnowledgeBaseId(tenantId, kbId);
        for (Document document : documents) {
            delete(tenantId, document.getId());
        }
    }

    @Override
    @Transactional
    public void saveChunks(List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            chunkMapper.insert(chunk);
        }
    }

    @Override
    public List<DocumentChunk> getChunksByDocumentId(Long documentId) {
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentChunk::getDocumentId, documentId)
                .orderByAsc(DocumentChunk::getChunkIndex);
        return chunkMapper.selectList(wrapper);
    }

    @Override
    public List<DocumentChunk> getChunksByDocumentId(long tenantId, Long documentId) {
        return chunkMapper.selectByTenantAndDocumentId(tenantId, documentId);
    }

    @Override
    public List<String> getVectorIdsByDocumentId(Long documentId) {
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentChunk::getDocumentId, documentId)
                .isNotNull(DocumentChunk::getVectorId)
                .select(DocumentChunk::getVectorId);
        return chunkMapper.selectList(wrapper)
                .stream()
                .map(DocumentChunk::getVectorId)
                .filter(id -> id != null && !id.isEmpty())
                .toList();
    }

    @Override
    public int countByKnowledgeBaseId(Long kbId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKbId, kbId);
        return Math.toIntExact(documentMapper.selectCount(wrapper));
    }

    @Override
    public int countByKnowledgeBaseId(long tenantId, Long kbId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getTenantId, tenantId)
                .eq(Document::getKbId, kbId);
        return Math.toIntExact(documentMapper.selectCount(wrapper));
    }

    private void deleteChunksByDocumentId(Long documentId) {
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentChunk::getDocumentId, documentId);
        chunkMapper.delete(wrapper);
    }
}
