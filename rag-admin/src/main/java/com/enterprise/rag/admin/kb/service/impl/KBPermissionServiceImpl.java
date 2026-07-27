package com.enterprise.rag.admin.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.rag.admin.kb.entity.KBPermission;
import com.enterprise.rag.admin.kb.entity.PermissionType;
import com.enterprise.rag.admin.kb.mapper.KBPermissionMapper;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 知识库权限服务实现
 */
@Service
@RequiredArgsConstructor
public class KBPermissionServiceImpl implements KBPermissionService {

    private final KBPermissionMapper permissionMapper;

    @Override
    @Transactional
    public KBPermission grant(Long kbId, Long userId, PermissionType permissionType) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    public KBPermission grant(long tenantId, Long kbId, Long userId, PermissionType permissionType) {
        if (!permissionMapper.tenantResourceAndUserExist(tenantId, kbId, userId)) {
            throw new BusinessException(
                    "PERMISSION_001", "知识库或目标用户不存在", HttpStatus.NOT_FOUND);
        }

        KBPermission existing = permissionMapper.selectByTenantAndResource(tenantId, kbId, userId);
        if (existing != null) {
            int updated = permissionMapper.updateTypeByTenantAndResource(
                    tenantId, kbId, userId, permissionType.name());
            if (updated != 1) {
                throw new BusinessException(
                        "PERMISSION_001", "知识库或目标用户不存在", HttpStatus.NOT_FOUND);
            }
            existing.setPermissionType(permissionType.name());
            return existing;
        }

        KBPermission permission = new KBPermission();
        permission.setTenantId(tenantId);
        permission.setKbId(kbId);
        permission.setUserId(userId);
        permission.setPermissionType(permissionType.name());
        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    @Transactional
    public void revoke(Long kbId, Long userId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    public void revoke(long tenantId, Long kbId, Long userId) {
        permissionMapper.deleteByTenantAndResource(tenantId, kbId, userId);
    }

    @Override
    public Optional<KBPermission> getPermission(Long kbId, Long userId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public Optional<KBPermission> getPermission(long tenantId, Long kbId, Long userId) {
        return Optional.ofNullable(permissionMapper.selectByTenantAndResource(tenantId, kbId, userId));
    }

    @Override
    public boolean hasPermission(Long kbId, Long userId, PermissionType permissionType) {
        Optional<KBPermission> permission = getPermission(kbId, userId);
        if (permission.isEmpty()) {
            return false;
        }

        PermissionType userPermission = PermissionType.valueOf(permission.get().getPermissionType());
        
        // ADMIN 拥有所有权限
        if (userPermission == PermissionType.ADMIN) {
            return true;
        }
        
        // WRITE 拥有 READ 权限
        if (userPermission == PermissionType.WRITE && permissionType == PermissionType.READ) {
            return true;
        }
        
        return userPermission == permissionType;
    }

    @Override
    public boolean hasPermission(long tenantId, Long kbId, Long userId, PermissionType permissionType) {
        String storedPermission = permissionMapper.findPermissionTypeByTenantAndResource(
                tenantId, kbId, userId);
        if (storedPermission == null) {
            return false;
        }

        PermissionType userPermission = PermissionType.valueOf(storedPermission);
        if (userPermission == PermissionType.ADMIN) {
            return true;
        }
        if (userPermission == PermissionType.WRITE && permissionType == PermissionType.READ) {
            return true;
        }
        return userPermission == permissionType;
    }

    @Override
    public boolean canAccess(Long kbId, Long userId, Boolean isPublic, Long ownerId) {
        // 公开知识库任何人都可以访问
        if (Boolean.TRUE.equals(isPublic)) {
            return true;
        }
        
        // 所有者可以访问
        if (userId != null && userId.equals(ownerId)) {
            return true;
        }
        
        // 检查是否有权限
        return userId != null && hasPermission(kbId, userId, PermissionType.READ);
    }

    @Override
    public List<KBPermission> getByKnowledgeBaseId(Long kbId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public List<KBPermission> getByKnowledgeBaseId(long tenantId, Long kbId) {
        return permissionMapper.selectByTenantAndKnowledgeBase(tenantId, kbId);
    }

    @Override
    public List<Long> getAccessibleKnowledgeBaseIds(Long userId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public List<Long> getAccessibleKnowledgeBaseIds(long tenantId, Long userId) {
        LambdaQueryWrapper<KBPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KBPermission::getTenantId, tenantId)
               .eq(KBPermission::getUserId, userId)
               .select(KBPermission::getKbId);
        return permissionMapper.selectList(wrapper)
                .stream()
                .map(KBPermission::getKbId)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByKnowledgeBaseId(Long kbId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    public void deleteByKnowledgeBaseId(long tenantId, Long kbId) {
        permissionMapper.deleteByTenantAndKnowledgeBase(tenantId, kbId);
    }
}
