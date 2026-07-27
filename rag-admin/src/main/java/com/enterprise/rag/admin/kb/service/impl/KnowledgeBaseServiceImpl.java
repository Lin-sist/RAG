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
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.exception.RedisDependencyException;
import com.enterprise.rag.common.idempotency.Idempotent;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
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

    private static final String VECTOR_READY = "READY";
    private static final String VECTOR_INITIALIZING = "INITIALIZING";

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
    public KnowledgeBaseDTO create(CreateKnowledgeBaseRequest request, RequestIdentity identity) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setOwnerId(identity.userId());
        kb.setTenantId(identity.tenantId());
        kb.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);
        kb.setDocumentCount(0);
        kb.setVectorReadiness(VECTOR_INITIALIZING);

        // 生成唯一的向量集合名称
        knowledgeBaseMapper.insert(kb);

        String collectionName = "tenant_" + identity.tenantId() + "_kb_" + kb.getId() + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        kb.setVectorCollection(collectionName);
        knowledgeBaseMapper.updateById(kb);
        TenantVectorScope vectorScope = new TenantVectorScope(identity.tenantId(), kb.getId(), collectionName);

        // 创建向量集合（使用当前 Embedding 模型的实际维度）
        try {
            int dimension = embeddingService.getDimension();
            vectorStore.createCollection(vectorScope, dimension);
            kb.setVectorReadiness(VECTOR_READY);
            kb.setVectorSourceCollection(null);
            kb.setVectorShadowCollection(null);
            knowledgeBaseMapper.updateById(kb);
            log.info("Created vector collection: {} with dimension: {}", collectionName, dimension);
        } catch (VectorDependencyException e) {
            log.error("Vector collection create failed: dependency=milvus, operation={}, errorCategory={}, failMode={}",
                    e.getOperation(), e.getErrorCategory(), e.getFailMode());
            throw e;
        } catch (Exception e) {
            log.error("Failed to create vector collection; rollback create: errorType={}",
                    e.getClass().getSimpleName());
            throw new BusinessException("KB_005", "创建知识库失败：向量集合初始化失败", e);
        }

        return toDTO(kb);
    }

    @Override
    public Optional<KnowledgeBaseDTO> getById(Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public Optional<KnowledgeBaseDTO> getById(long tenantId, Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectByTenantAndId(tenantId, id);
        return Optional.ofNullable(kb).map(value -> toDTO(value, tenantId));
    }

    @Override
    public Optional<KnowledgeBaseDTO> getById(Long id, RequestIdentity identity) {
        KnowledgeBase kb = knowledgeBaseMapper.selectByTenantAndId(identity.tenantId(), id);
        return Optional.ofNullable(kb).map(value -> toDTO(value, identity.tenantId()));
    }

    @Override
    public TenantVectorScope requireReadyVectorScope(long tenantId, Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectByTenantAndId(tenantId, id);
        if (kb == null) {
            throw new BusinessException("KB_001", "知识库不存在", HttpStatus.NOT_FOUND);
        }
        if (!VECTOR_READY.equals(kb.getVectorReadiness())
                || kb.getVectorCollection() == null || kb.getVectorCollection().isBlank()) {
            throw VectorDependencyException.indexNotReady("resolve_scope");
        }
        return new TenantVectorScope(tenantId, id, kb.getVectorCollection());
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
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public List<KnowledgeBaseDTO> getAccessibleByIdentity(RequestIdentity identity) {
        Set<Long> accessibleIds = new HashSet<>();

        // 获取用户拥有的知识库
        LambdaQueryWrapper<KnowledgeBase> ownedWrapper = new LambdaQueryWrapper<>();
        ownedWrapper.eq(KnowledgeBase::getTenantId, identity.tenantId())
                .eq(KnowledgeBase::getOwnerId, identity.userId());
        knowledgeBaseMapper.selectList(ownedWrapper)
                .forEach(kb -> accessibleIds.add(kb.getId()));

        // 获取用户有权限的知识库
        accessibleIds.addAll(permissionService.getAccessibleKnowledgeBaseIds(
                identity.tenantId(), identity.userId()));

        // 获取公开的知识库
        LambdaQueryWrapper<KnowledgeBase> publicWrapper = new LambdaQueryWrapper<>();
        publicWrapper.eq(KnowledgeBase::getTenantId, identity.tenantId())
                .eq(KnowledgeBase::getIsPublic, true);
        knowledgeBaseMapper.selectList(publicWrapper)
                .forEach(kb -> accessibleIds.add(kb.getId()));

        if (accessibleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询所有可访问的知识库
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getTenantId, identity.tenantId())
                .in(KnowledgeBase::getId, accessibleIds)
                .orderByDesc(KnowledgeBase::getCreatedAt);
        return knowledgeBaseMapper.selectList(wrapper)
                .stream()
                .filter(kb -> kb.getTenantId() != null
                        && kb.getTenantId() == identity.tenantId())
                .map(kb -> toDTO(kb, identity.tenantId()))
                .toList();
    }

    @Override
    @Transactional
    @Idempotent(keyPrefix = "kb:update", required = false, ttlSeconds = 3600)
    public KnowledgeBaseDTO update(Long id, UpdateKnowledgeBaseRequest request) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    @Idempotent(keyPrefix = "kb:update", required = false, ttlSeconds = 3600)
    public KnowledgeBaseDTO update(Long id, UpdateKnowledgeBaseRequest request, RequestIdentity identity) {
        KnowledgeBase kb = knowledgeBaseMapper.selectByTenantAndId(identity.tenantId(), id);
        if (kb == null) {
            throw new BusinessException("KB_001", "知识库不存在", HttpStatus.NOT_FOUND);
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

        int updated = knowledgeBaseMapper.updateMutableFieldsByTenantAndId(
                identity.tenantId(), id, kb.getName(), kb.getDescription(), kb.getIsPublic());
        if (updated != 1) {
            throw new BusinessException("KB_001", "知识库不存在", HttpStatus.NOT_FOUND);
        }
        return toDTO(kb, identity.tenantId());
    }

    @Override
    @Transactional
    @Idempotent(keyPrefix = "kb:delete", required = false, ttlSeconds = 600)
    public void delete(Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    @Idempotent(keyPrefix = "kb:delete", required = false, ttlSeconds = 600)
    public void delete(Long id, RequestIdentity identity) {
        KnowledgeBase kb = knowledgeBaseMapper.selectByTenantAndId(identity.tenantId(), id);
        if (kb == null) {
            throw new BusinessException("KB_001", "知识库不存在", HttpStatus.NOT_FOUND);
        }

        documentService.deleteByKnowledgeBaseId(identity.tenantId(), id);
        permissionService.deleteByKnowledgeBaseId(identity.tenantId(), id);

        if (kb.getVectorCollection() != null) {
            vectorStore.dropCollection(requireReadyVectorScope(identity.tenantId(), id));
            log.info("Dropped tenant-scoped vector collection for knowledge base");
        }

        try {
            redisTemplate.delete(queryCountKey(identity.tenantId(), id));
        } catch (Exception e) {
            log.warn("Query count cleanup degraded: dependency=redis, subsystem=query_counter, "
                            + "operation=delete, failMode=open, errorType={}",
                    e.getClass().getSimpleName());
        }

        int deleted = knowledgeBaseMapper.deleteByTenantAndId(identity.tenantId(), id);
        if (deleted != 1) {
            throw new BusinessException("KB_001", "知识库不存在", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public boolean exists(Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public KnowledgeBaseStatistics getStatistics(Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public KnowledgeBaseStatistics getStatistics(Long id, RequestIdentity identity) {
        KnowledgeBase kb = knowledgeBaseMapper.selectByTenantAndId(identity.tenantId(), id);
        if (kb == null) {
            throw new BusinessException("KB_001", "知识库不存在", HttpStatus.NOT_FOUND);
        }

        int documentCount = documentService.countByKnowledgeBaseId(identity.tenantId(), id);
        long vectorCount = 0;
        if (kb.getVectorCollection() != null) {
            try {
                vectorCount = vectorStore.count(requireReadyVectorScope(identity.tenantId(), id));
            } catch (VectorDependencyException e) {
                throw e;
            } catch (Exception e) {
                log.error("Vector count read failed: dependency=milvus, operation=count, failMode=closed, errorType={}",
                        e.getClass().getSimpleName());
                throw VectorDependencyException.unavailable("count", e);
            }
        }

        long queryCount = 0;
        String countStr;
        try {
            countStr = redisTemplate.opsForValue().get(queryCountKey(identity.tenantId(), id));
        } catch (Exception e) {
            log.error("Query count read failed: dependency=redis, subsystem=query_counter, "
                            + "operation=read, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("query_counter", "read", e);
        }
        if (countStr != null) {
            try {
                queryCount = Long.parseLong(countStr);
            } catch (NumberFormatException e) {
                log.error("Query count deserialize failed: dependency=redis, subsystem=query_counter, "
                                + "operation=deserialize, failMode=closed, errorType={}",
                        e.getClass().getSimpleName());
                throw RedisDependencyException.unavailable("query_counter", "deserialize", e);
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
    @Transactional
    public void updateDocumentCount(long tenantId, Long id, int delta) {
        knowledgeBaseMapper.updateDocumentCountByTenantAndId(tenantId, id, delta);
    }

    @Override
    public void incrementQueryCount(Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public void incrementQueryCount(long tenantId, Long id) {
        try {
            redisTemplate.opsForValue().increment(queryCountKey(tenantId, id));
        } catch (Exception e) {
            log.warn("Query count increment degraded: dependency=redis, subsystem=query_counter, "
                            + "operation=increment, failMode=open, errorType={}",
                    e.getClass().getSimpleName());
        }
    }

    private KnowledgeBaseDTO toDTO(KnowledgeBase kb) {
        // 使用实时查询的文档数量，而不是缓存的 documentCount 字段
        // 因为异步处理失败的文档不会递增计数器，删除失败文档却会递减，导致计数器漂移
        int realDocumentCount = documentService.countByKnowledgeBaseId(kb.getId());
        return toDTO(kb, realDocumentCount);
    }

    private String queryCountKey(long tenantId, Long kbId) {
        return QUERY_COUNT_KEY_PREFIX + "v2:" + tenantId + ":" + kbId;
    }

    private KnowledgeBaseDTO toDTO(KnowledgeBase kb, long tenantId) {
        int realDocumentCount = documentService.countByKnowledgeBaseId(tenantId, kb.getId());
        return toDTO(kb, realDocumentCount);
    }

    private KnowledgeBaseDTO toDTO(KnowledgeBase kb, int realDocumentCount) {
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
