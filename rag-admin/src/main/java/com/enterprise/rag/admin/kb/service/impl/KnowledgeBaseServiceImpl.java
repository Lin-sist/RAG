package com.enterprise.rag.admin.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.enterprise.rag.admin.kb.dto.CreateKnowledgeBaseRequest;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseStatistics;
import com.enterprise.rag.admin.kb.dto.UpdateKnowledgeBaseRequest;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.idempotency.Idempotent;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.vectorstore.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 知识库服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentService documentService;
    private final KBPermissionService permissionService;
    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final StringRedisTemplate redisTemplate;

    private static final String QUERY_COUNT_KEY_PREFIX = "kb:query:count:";

    @Override
    @Transactional
    @Idempotent(keyPrefix = "kb:create", required = false, ttlSeconds = 3600)
    public KnowledgeBaseDTO create(CreateKnowledgeBaseRequest request, Long ownerId) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setOwnerId(ownerId);
        kb.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);
        kb.setDocumentCount(0);

        // 生成唯一的向量集合名称
        String collectionName = "kb_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        kb.setVectorCollection(collectionName);

        knowledgeBaseMapper.insert(kb);

        // 创建向量集合（使用当前 Embedding 模型的实际维度）
        try {
            int dimension = embeddingService.getDimension();
            vectorStore.createCollection(collectionName, dimension);
            log.info("Created vector collection: {} with dimension: {}", collectionName, dimension);
        } catch (Exception e) {
            log.warn("Failed to create vector collection {}: {}", collectionName, e.getMessage());
        }

        return toDTO(kb);
    }

    @Override
    public Optional<KnowledgeBaseDTO> getById(Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        return Optional.ofNullable(kb).map(this::toDTO);
    }

    @Override
    public List<KnowledgeBaseDTO> getByOwnerId(Long userId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getOwnerId, userId)
                .orderByDesc(KnowledgeBase::getCreatedAt);
        return knowledgeBaseMapper.selectList(wrapper)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<KnowledgeBaseDTO> getAccessibleByUserId(Long userId) {
        Set<Long> accessibleIds = new HashSet<>();

        // 获取用户拥有的知识库
        LambdaQueryWrapper<KnowledgeBase> ownedWrapper = new LambdaQueryWrapper<>();
        ownedWrapper.eq(KnowledgeBase::getOwnerId, userId);
        knowledgeBaseMapper.selectList(ownedWrapper)
                .forEach(kb -> accessibleIds.add(kb.getId()));

        // 获取用户有权限的知识库
        accessibleIds.addAll(permissionService.getAccessibleKnowledgeBaseIds(userId));

        // 获取公开的知识库
        LambdaQueryWrapper<KnowledgeBase> publicWrapper = new LambdaQueryWrapper<>();
        publicWrapper.eq(KnowledgeBase::getIsPublic, true);
        knowledgeBaseMapper.selectList(publicWrapper)
                .forEach(kb -> accessibleIds.add(kb.getId()));

        if (accessibleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询所有可访问的知识库
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KnowledgeBase::getId, accessibleIds)
                .orderByDesc(KnowledgeBase::getCreatedAt);
        return knowledgeBaseMapper.selectList(wrapper)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    @Idempotent(keyPrefix = "kb:update", required = false, ttlSeconds = 3600)
    public KnowledgeBaseDTO update(Long id, UpdateKnowledgeBaseRequest request) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException("KB_001", "知识库不存在");
        }

        if (request.getName() != null) {
            kb.setName(request.getName());
        }
        if (request.getDescription() != null) {
            kb.setDescription(request.getDescription());
        }
        if (request.getIsPublic() != null) {
            kb.setIsPublic(request.getIsPublic());
        }

        knowledgeBaseMapper.updateById(kb);
        return toDTO(kb);
    }

    @Override
    @Transactional
    @Idempotent(keyPrefix = "kb:delete", required = false, ttlSeconds = 600)
    public void delete(Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            return;
        }

        // 删除所有文档（级联删除向量数据）
        documentService.deleteByKnowledgeBaseId(id);

        // 删除所有权限记录
        permissionService.deleteByKnowledgeBaseId(id);

        // 删除向量集合
        if (kb.getVectorCollection() != null) {
            try {
                vectorStore.dropCollection(kb.getVectorCollection());
                log.info("Dropped vector collection: {}", kb.getVectorCollection());
            } catch (Exception e) {
                log.warn("Failed to drop vector collection {}: {}", kb.getVectorCollection(), e.getMessage());
            }
        }

        // 删除查询计数
        redisTemplate.delete(QUERY_COUNT_KEY_PREFIX + id);

        // 删除知识库记录
        knowledgeBaseMapper.deleteById(id);
    }

    @Override
    public boolean exists(Long id) {
        return knowledgeBaseMapper.selectById(id) != null;
    }

    @Override
    public KnowledgeBaseStatistics getStatistics(Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null) {
            throw new BusinessException("KB_001", "知识库不存在");
        }

        // 获取实际文档数量
        int documentCount = documentService.countByKnowledgeBaseId(id);

        // 获取向量数量
        long vectorCount = 0;
        if (kb.getVectorCollection() != null) {
            try {
                vectorCount = vectorStore.count(kb.getVectorCollection());
            } catch (Exception e) {
                log.warn("Failed to get vector count for collection {}: {}",
                        kb.getVectorCollection(), e.getMessage());
            }
        }

        // 获取查询次数
        long queryCount = 0;
        String countStr = redisTemplate.opsForValue().get(QUERY_COUNT_KEY_PREFIX + id);
        if (countStr != null) {
            try {
                queryCount = Long.parseLong(countStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid query count for kb {}: {}", id, countStr);
            }
        }

        return KnowledgeBaseStatistics.builder()
                .kbId(id)
                .documentCount(documentCount)
                .vectorCount(vectorCount)
                .queryCount(queryCount)
                .build();
    }

    @Override
    @Transactional
    public void updateDocumentCount(Long id, int delta) {
        LambdaUpdateWrapper<KnowledgeBase> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(KnowledgeBase::getId, id)
                .setSql("document_count = document_count + " + delta);
        knowledgeBaseMapper.update(null, wrapper);
    }

    @Override
    public void incrementQueryCount(Long id) {
        try {
            redisTemplate.opsForValue().increment(QUERY_COUNT_KEY_PREFIX + id);
        } catch (Exception e) {
            log.warn("Failed to increment query count for kb {}: {}", id, e.getMessage());
        }
    }

    private KnowledgeBaseDTO toDTO(KnowledgeBase kb) {
        // 使用实时查询的文档数量，而不是缓存的 documentCount 字段
        // 因为异步处理失败的文档不会递增计数器，删除失败文档却会递减，导致计数器漂移
        int realDocumentCount = documentService.countByKnowledgeBaseId(kb.getId());
        return KnowledgeBaseDTO.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .ownerId(kb.getOwnerId())
                .vectorCollection(kb.getVectorCollection())
                .documentCount(realDocumentCount)
                .isPublic(kb.getIsPublic())
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
                .build();
    }
}
