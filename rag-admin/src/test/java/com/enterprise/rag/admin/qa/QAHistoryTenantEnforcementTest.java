package com.enterprise.rag.admin.qa;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.rag.admin.qa.dto.QAHistoryPageRequest;
import com.enterprise.rag.admin.qa.dto.SaveQAHistoryRequest;
import com.enterprise.rag.admin.qa.entity.QAHistory;
import com.enterprise.rag.admin.qa.mapper.QAHistoryMapper;
import com.enterprise.rag.admin.qa.service.impl.QAHistoryServiceImpl;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QAHistoryTenantEnforcementTest {

    private static final RequestIdentity IDENTITY = new RequestIdentity(21L, 11L);

    private QAHistoryMapper historyMapper;
    private QAHistoryServiceImpl historyService;

    @BeforeAll
    static void initializeMyBatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "qa-history-tenant-test"),
                QAHistory.class);
    }

    @BeforeEach
    void setUp() {
        historyMapper = mock(QAHistoryMapper.class);
        historyService = new QAHistoryServiceImpl(historyMapper, new ObjectMapper());
    }

    @Test
    void saveShouldPersistAuthenticatedTenantAndRejectUserMismatch() {
        doAnswer(invocation -> {
            QAHistory history = invocation.getArgument(0);
            history.setId(31L);
            return 1;
        }).when(historyMapper).insert(any(QAHistory.class));

        SaveQAHistoryRequest request = requestFor(IDENTITY.userId(), 41L);
        historyService.save(IDENTITY, request);

        ArgumentCaptor<QAHistory> entityCaptor = ArgumentCaptor.forClass(QAHistory.class);
        verify(historyMapper).insert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getTenantId()).isEqualTo(IDENTITY.tenantId());
        assertThat(entityCaptor.getValue().getUserId()).isEqualTo(IDENTITY.userId());

        SaveQAHistoryRequest mismatch = requestFor(999L, 41L);
        assertThatThrownBy(() -> historyService.save(IDENTITY, mismatch))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo("AUTH_004");
    }

    @Test
    void readPageAndDeleteShouldAlwaysCarryTenantAndAuthenticatedUserPredicates() {
        QAHistory row = new QAHistory();
        row.setId(31L);
        row.setTenantId(IDENTITY.tenantId());
        row.setUserId(IDENTITY.userId());
        row.setCitations("[]");
        when(historyMapper.selectOne(any())).thenReturn(row);

        assertThat(historyService.getById(IDENTITY, 31L)).isPresent();
        assertScoped(tenantWrapperCapturedBySelectOne(), 11L, 21L, 31L);

        Page<QAHistory> returnedPage = new Page<>(1, 20);
        returnedPage.setRecords(List.of(row));
        returnedPage.setTotal(1L);
        when(historyMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(returnedPage);

        QAHistoryPageRequest pageRequest = new QAHistoryPageRequest();
        pageRequest.setUserId(IDENTITY.userId());
        pageRequest.setKbId(41L);
        pageRequest.setPage(1);
        pageRequest.setSize(20);
        historyService.getPage(IDENTITY, pageRequest);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<QAHistory>> pageWrapper = ArgumentCaptor.forClass(Wrapper.class);
        verify(historyMapper).selectPage(any(Page.class), pageWrapper.capture());
        assertScoped(pageWrapper.getValue(), 11L, 21L, 41L);

        historyService.delete(IDENTITY, 31L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<QAHistory>> deleteWrapper = ArgumentCaptor.forClass(Wrapper.class);
        verify(historyMapper).delete(deleteWrapper.capture());
        assertScoped(deleteWrapper.getValue(), 11L, 21L, 31L);
    }

    @Test
    void legacyUnscopedEntryPointsShouldFailClosedWithoutTouchingMapper() {
        assertThatThrownBy(() -> historyService.getById(31L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_IDENTITY_REQUIRED");
        assertThatThrownBy(() -> historyService.delete(31L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_IDENTITY_REQUIRED");

        verify(historyMapper, never()).selectById(any());
        verify(historyMapper, never()).deleteById(any());
    }

    @SuppressWarnings("unchecked")
    private Wrapper<QAHistory> tenantWrapperCapturedBySelectOne() {
        ArgumentCaptor<Wrapper<QAHistory>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(historyMapper).selectOne(wrapperCaptor.capture());
        return wrapperCaptor.getValue();
    }

    private void assertScoped(Wrapper<QAHistory> wrapper, Long... expectedValues) {
        assertThat(wrapper.getSqlSegment())
                .contains("tenant_id")
                .contains("user_id");
        assertThat(wrapper).isInstanceOf(AbstractWrapper.class);
        AbstractWrapper<?, ?, ?> scopedWrapper = (AbstractWrapper<?, ?, ?>) wrapper;
        assertThat(scopedWrapper.getParamNameValuePairs().values())
                .contains(expectedValues);
    }

    private SaveQAHistoryRequest requestFor(Long userId, Long kbId) {
        return SaveQAHistoryRequest.builder()
                .userId(userId)
                .kbId(kbId)
                .question("question")
                .answer("answer")
                .traceId("trace")
                .latencyMs(12)
                .build();
    }
}
