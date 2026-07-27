package com.enterprise.rag.admin.qa.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.enterprise.rag.admin.qa.dto.SubmitFeedbackRequest;
import com.enterprise.rag.admin.qa.entity.QAFeedback;
import com.enterprise.rag.admin.qa.mapper.QAFeedbackMapper;
import com.enterprise.rag.admin.qa.service.impl.QAFeedbackServiceImpl;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.exception.BusinessException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QAFeedbackTenantEnforcementTest {

    @BeforeAll
    static void initializeMyBatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "qa-feedback-tenant-test");
        TableInfoHelper.initTableInfo(assistant, QAFeedback.class);
    }

    @Test
    void unscopedSubmitFailsClosedBeforeDatabaseAccess() {
        QAFeedbackMapper feedbackMapper = mock(QAFeedbackMapper.class);
        QAFeedbackService service = new QAFeedbackServiceImpl(feedbackMapper);
        SubmitFeedbackRequest request = new SubmitFeedbackRequest();
        request.setQaId(100L);
        request.setUserId(1001L);
        request.setRating(5);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.submit(request));

        assertEquals("TENANT_IDENTITY_REQUIRED", exception.getMessage());
        verify(feedbackMapper, never()).selectCount(any());
        verify(feedbackMapper, never()).insert(any());
    }

    @Test
    void feedbackForHistoryOutsideIdentityScopeIsNotFoundBeforeInsert() {
        QAFeedbackMapper feedbackMapper = mock(QAFeedbackMapper.class);
        QAFeedbackService service = new QAFeedbackServiceImpl(feedbackMapper);
        SubmitFeedbackRequest request = new SubmitFeedbackRequest();
        request.setQaId(100L);
        request.setUserId(1001L);
        request.setRating(5);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submit(new RequestIdentity(1001L, 901L), request));

        assertEquals("HISTORY_001", exception.getErrorCode());
        verify(feedbackMapper, never()).insert(any());
    }

    @Test
    void sameTenantSubmitPersistsServerIdentityTenant() {
        QAFeedbackMapper feedbackMapper = mock(QAFeedbackMapper.class);
        QAFeedbackService service = new QAFeedbackServiceImpl(feedbackMapper);
        SubmitFeedbackRequest request = new SubmitFeedbackRequest();
        request.setQaId(100L);
        request.setUserId(1001L);
        request.setRating(5);
        when(feedbackMapper.historyOwnedByTenantAndUser(901L, 100L, 1001L)).thenReturn(true);
        when(feedbackMapper.countByTenantQaAndUser(901L, 100L, 1001L)).thenReturn(0L);

        service.submit(new RequestIdentity(1001L, 901L), request);

        ArgumentCaptor<QAFeedback> captor = ArgumentCaptor.forClass(QAFeedback.class);
        verify(feedbackMapper).insert(captor.capture());
        assertEquals(901L, captor.getValue().getTenantId());
        assertEquals(100L, captor.getValue().getQaId());
        assertEquals(1001L, captor.getValue().getUserId());
    }

    @Test
    void allUnscopedFeedbackReadsAndDeletesFailClosed() {
        QAFeedbackService service = new QAFeedbackServiceImpl(mock(QAFeedbackMapper.class));

        assertAll(
                () -> assertEquals("TENANT_IDENTITY_REQUIRED",
                        assertThrows(IllegalStateException.class, () -> service.getById(1L)).getMessage()),
                () -> assertEquals("TENANT_IDENTITY_REQUIRED",
                        assertThrows(IllegalStateException.class, () -> service.getByQaId(100L)).getMessage()),
                () -> assertEquals("TENANT_IDENTITY_REQUIRED",
                        assertThrows(IllegalStateException.class, () -> service.listByQaId(100L)).getMessage()),
                () -> assertEquals("TENANT_IDENTITY_REQUIRED",
                        assertThrows(IllegalStateException.class, () -> service.listByUserId(1001L)).getMessage()),
                () -> assertEquals("TENANT_IDENTITY_REQUIRED",
                        assertThrows(IllegalStateException.class,
                                () -> service.hasUserFeedback(100L, 1001L)).getMessage()),
                () -> assertEquals("TENANT_IDENTITY_REQUIRED",
                        assertThrows(IllegalStateException.class, () -> service.delete(1L)).getMessage()),
                () -> assertEquals("TENANT_IDENTITY_REQUIRED",
                        assertThrows(IllegalStateException.class, () -> service.deleteByQaId(100L)).getMessage()));
    }
}
