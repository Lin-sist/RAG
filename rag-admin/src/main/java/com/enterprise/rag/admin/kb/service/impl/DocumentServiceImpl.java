package com.enterprise.rag.admin.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.core.vectorstore.VectorStore;
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
    private final VectorStore vectorStore;

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
    public List<Document> getByKnowledgeBaseId(Long kbId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKbId, kbId)
               .orderByDesc(Document::getCreatedAt);
        return documentMapper.selectList(wrapper);
    }

    @Override
    public Optional<Document> getByContentHash(String contentHash) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getContentHash, contentHash);
        return Optional.ofNullable(documentMapper.selectOne(wrapper));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        LambdaUpdateWrapper<Document> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Document::getId, id)
               .set(Document::getStatus, status);
        documentMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void updateChunkCount(Long id, int chunkCount) {
        LambdaUpdateWrapper<Document> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Document::getId, id)
               .set(Document::getChunkCount, chunkCount);
        documentMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Optional<Document> docOpt = getById(id);
        if (docOpt.isEmpty()) {
            return;
        }

        Document document = docOpt.get();
        
        // 获取所有向量ID
        List<String> vectorIds = getVectorIdsByDocumentId(id);
        
        // 删除向量数据
        if (!vectorIds.isEmpty()) {
            try {
                // 获取知识库的向量集合名称
                String collectionName = getCollectionName(document.getKbId());
                vectorStore.delete(collectionName, vectorIds);
                log.info("Deleted {} vectors for document {}", vectorIds.size(), id);
            } catch (Exception e) {
                log.error("Failed to delete vectors for document {}: {}", id, e.getMessage());
            }
        }
        
        // 删除分块记录
        deleteChunksByDocumentId(id);
        
        // 删除文档记录
        documentMapper.deleteById(id);
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

    private void deleteChunksByDocumentId(Long documentId) {
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentChunk::getDocumentId, documentId);
        chunkMapper.delete(wrapper);
    }

    private String getCollectionName(Long kbId) {
        return "kb_" + kbId;
    }
}
