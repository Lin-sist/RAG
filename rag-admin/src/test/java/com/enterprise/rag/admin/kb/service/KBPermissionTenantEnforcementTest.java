package com.enterprise.rag.admin.kb.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.enterprise.rag.admin.kb.entity.KBPermission;
import com.enterprise.rag.admin.kb.entity.PermissionType;
import com.enterprise.rag.admin.kb.mapper.KBPermissionMapper;
import com.enterprise.rag.admin.kb.service.impl.KBPermissionServiceImpl;
import com.enterprise.rag.common.exception.BusinessException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KBPermissionTenantEnforcementTest {

    @BeforeAll
    static void initializeMyBatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "kb-permission-tenant-test");
        TableInfoHelper.initTableInfo(assistant, KBPermission.class);
    }

    @Test
    void unscopedGrantFailsClosedBeforeDatabaseMutation() {
        KBPermissionMapper permissionMapper = mock(KBPermissionMapper.class);
        KBPermissionService service = new KBPermissionServiceImpl(permissionMapper);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.grant(100L, 1001L, PermissionType.READ));

        assertEquals("TENANT_IDENTITY_REQUIRED", exception.getMessage());
        verify(permissionMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void crossTenantTargetUserIsNotFoundBeforePermissionMutation() {
        KBPermissionMapper permissionMapper = mock(KBPermissionMapper.class);
        KBPermissionService service = new KBPermissionServiceImpl(permissionMapper);
        when(permissionMapper.tenantResourceAndUserExist(901L, 100L, 2002L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.grant(901L, 100L, 2002L, PermissionType.READ));

        assertEquals("PERMISSION_001", exception.getErrorCode());
        verify(permissionMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sameTenantGrantPersistsServerTenantIdentity() {
        KBPermissionMapper permissionMapper = mock(KBPermissionMapper.class);
        KBPermissionService service = new KBPermissionServiceImpl(permissionMapper);
        when(permissionMapper.tenantResourceAndUserExist(901L, 100L, 1002L)).thenReturn(true);

        KBPermission permission = service.grant(901L, 100L, 1002L, PermissionType.WRITE);

        ArgumentCaptor<KBPermission> captor = ArgumentCaptor.forClass(KBPermission.class);
        verify(permissionMapper).insert(captor.capture());
        assertEquals(901L, captor.getValue().getTenantId());
        assertEquals(100L, captor.getValue().getKbId());
        assertEquals(1002L, captor.getValue().getUserId());
        assertEquals(PermissionType.WRITE.name(), permission.getPermissionType());
    }

    @Test
    void sameTenantExistingGrantUsesTenantScopedUpdate() {
        KBPermissionMapper permissionMapper = mock(KBPermissionMapper.class);
        KBPermissionService service = new KBPermissionServiceImpl(permissionMapper);
        KBPermission existing = new KBPermission();
        existing.setId(300L);
        existing.setTenantId(901L);
        existing.setKbId(100L);
        existing.setUserId(1002L);
        existing.setPermissionType(PermissionType.READ.name());
        when(permissionMapper.tenantResourceAndUserExist(901L, 100L, 1002L)).thenReturn(true);
        when(permissionMapper.selectByTenantAndResource(901L, 100L, 1002L)).thenReturn(existing);
        when(permissionMapper.updateTypeByTenantAndResource(
                901L, 100L, 1002L, PermissionType.ADMIN.name())).thenReturn(1);

        KBPermission permission = service.grant(901L, 100L, 1002L, PermissionType.ADMIN);

        assertEquals(PermissionType.ADMIN.name(), permission.getPermissionType());
        verify(permissionMapper).updateTypeByTenantAndResource(
                901L, 100L, 1002L, PermissionType.ADMIN.name());
        verify(permissionMapper, never()).updateById(existing);
    }

    @Test
    void unscopedRevokeFailsClosed() {
        KBPermissionService service = new KBPermissionServiceImpl(mock(KBPermissionMapper.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.revoke(100L, 1002L));

        assertEquals("TENANT_IDENTITY_REQUIRED", exception.getMessage());
    }

    @Test
    void unscopedPermissionLookupFailsClosed() {
        KBPermissionService service = new KBPermissionServiceImpl(mock(KBPermissionMapper.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.getPermission(100L, 1002L));

        assertEquals("TENANT_IDENTITY_REQUIRED", exception.getMessage());
    }

    @Test
    void unscopedPermissionListFailsClosed() {
        KBPermissionService service = new KBPermissionServiceImpl(mock(KBPermissionMapper.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.getByKnowledgeBaseId(100L));

        assertEquals("TENANT_IDENTITY_REQUIRED", exception.getMessage());
    }

    @Test
    void unscopedPermissionCascadeDeleteFailsClosed() {
        KBPermissionService service = new KBPermissionServiceImpl(mock(KBPermissionMapper.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.deleteByKnowledgeBaseId(100L));

        assertEquals("TENANT_IDENTITY_REQUIRED", exception.getMessage());
    }
}
