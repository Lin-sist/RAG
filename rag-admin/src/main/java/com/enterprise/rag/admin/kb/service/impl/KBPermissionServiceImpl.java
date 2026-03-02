package com.enterprise.rag.admin.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.rag.admin.kb.entity.KBPermission;
import com.enterprise.rag.admin.kb.entity.PermissionType;
import com.enterprise.rag.admin.kb.mapper.KBPermissionMapper;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import lombok.RequiredArgsConstructor;
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
        // 检查是否已存在权限
        Optional<KBPermission> existing = getPermission(kbId, userId);
        if (existing.isPresent()) {
            // 更新现有权限
            KBPermission permission = existing.get();
            permission.setPermissionType(permissionType.name());
            permissionMapper.updateById(permission);
            return permission;
        }

        // 创建新权限
        KBPermission permission = new KBPermission();
        permission.setKbId(kbId);
        permission.setUserId(userId);
        permission.setPermissionType(permissionType.name());
        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    @Transactional
    public void revoke(Long kbId, Long userId) {
        LambdaQueryWrapper<KBPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KBPermission::getKbId, kbId)
               .eq(KBPermission::getUserId, userId);
        permissionMapper.delete(wrapper);
    }

    @Override
    public Optional<KBPermission> getPermission(Long kbId, Long userId) {
        LambdaQueryWrapper<KBPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KBPermission::getKbId, kbId)
               .eq(KBPermission::getUserId, userId);
        return Optional.ofNullable(permissionMapper.selectOne(wrapper));
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
        LambdaQueryWrapper<KBPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KBPermission::getKbId, kbId);
        return permissionMapper.selectList(wrapper);
    }

    @Override
    public List<Long> getAccessibleKnowledgeBaseIds(Long userId) {
        LambdaQueryWrapper<KBPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KBPermission::getUserId, userId)
               .select(KBPermission::getKbId);
        return permissionMapper.selectList(wrapper)
                .stream()
                .map(KBPermission::getKbId)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByKnowledgeBaseId(Long kbId) {
        LambdaQueryWrapper<KBPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KBPermission::getKbId, kbId);
        permissionMapper.delete(wrapper);
    }
}
