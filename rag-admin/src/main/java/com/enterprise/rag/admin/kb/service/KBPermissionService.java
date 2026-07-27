package com.enterprise.rag.admin.kb.service;

import com.enterprise.rag.admin.kb.entity.KBPermission;
import com.enterprise.rag.admin.kb.entity.PermissionType;

import java.util.List;
import java.util.Optional;

/**
 * 知识库权限服务接口
 */
public interface KBPermissionService {

    /**
     * 授予用户知识库权限
     *
     * @param kbId           知识库ID
     * @param userId         用户ID
     * @param permissionType 权限类型
     * @return 创建的权限记录
     */
    KBPermission grant(Long kbId, Long userId, PermissionType permissionType);

    /**
     * 在指定 tenant 内授权，并验证知识库与目标用户归属一致。
     */
    default KBPermission grant(long tenantId, Long kbId, Long userId, PermissionType permissionType) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 撤销用户知识库权限
     *
     * @param kbId   知识库ID
     * @param userId 用户ID
     */
    void revoke(Long kbId, Long userId);

    default void revoke(long tenantId, Long kbId, Long userId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 获取用户对知识库的权限
     *
     * @param kbId   知识库ID
     * @param userId 用户ID
     * @return 权限记录（如果存在）
     */
    Optional<KBPermission> getPermission(Long kbId, Long userId);

    default Optional<KBPermission> getPermission(long tenantId, Long kbId, Long userId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 检查用户是否有指定权限
     *
     * @param kbId           知识库ID
     * @param userId         用户ID
     * @param permissionType 权限类型
     * @return true 如果有权限
     */
    boolean hasPermission(Long kbId, Long userId, PermissionType permissionType);

    /**
     * 在明确 tenant 边界内检查用户权限。
     *
     * <p>未实现 tenant-aware 查询的实现默认拒绝，禁止回退到裸 kbId/userId 查询。</p>
     */
    default boolean hasPermission(long tenantId, Long kbId, Long userId, PermissionType permissionType) {
        return false;
    }

    /**
     * 检查用户是否可以访问知识库（考虑公开状态和权限）
     *
     * @param kbId     知识库ID
     * @param userId   用户ID
     * @param isPublic 知识库是否公开
     * @param ownerId  知识库所有者ID
     * @return true 如果可以访问
     */
    boolean canAccess(Long kbId, Long userId, Boolean isPublic, Long ownerId);

    /**
     * 获取知识库的所有权限记录
     *
     * @param kbId 知识库ID
     * @return 权限记录列表
     */
    List<KBPermission> getByKnowledgeBaseId(Long kbId);

    default List<KBPermission> getByKnowledgeBaseId(long tenantId, Long kbId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 获取用户有权限的所有知识库ID
     *
     * @param userId 用户ID
     * @return 知识库ID列表
     */
    List<Long> getAccessibleKnowledgeBaseIds(Long userId);

    /**
     * 获取明确 tenant 边界内用户有权限的知识库 ID。
     */
    default List<Long> getAccessibleKnowledgeBaseIds(long tenantId, Long userId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 删除知识库的所有权限记录
     *
     * @param kbId 知识库ID
     */
    void deleteByKnowledgeBaseId(Long kbId);

    /**
     * 删除指定 tenant 内知识库的所有权限记录。
     */
    default void deleteByKnowledgeBaseId(long tenantId, Long kbId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }
}
