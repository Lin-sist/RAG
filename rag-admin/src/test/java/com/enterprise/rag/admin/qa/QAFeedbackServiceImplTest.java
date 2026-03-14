package com.enterprise.rag.admin.qa;

import com.enterprise.rag.admin.qa.dto.SubmitFeedbackRequest;
import com.enterprise.rag.admin.qa.mapper.QAFeedbackMapper;
import com.enterprise.rag.admin.qa.service.impl.QAFeedbackServiceImpl;
import com.enterprise.rag.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QAFeedbackServiceImplTest {

    private QAFeedbackMapper qaFeedbackMapper;
    private QAFeedbackServiceImpl qaFeedbackService;

    @BeforeEach
    void setUp() {
        qaFeedbackMapper = mock(QAFeedbackMapper.class);
        qaFeedbackService = new QAFeedbackServiceImpl(qaFeedbackMapper);
        when(qaFeedbackMapper.selectCount(any())).thenReturn(0L);
    }

    @Test
    void submitShouldRejectWhenUniqueConstraintConflict() {
        SubmitFeedbackRequest request = new SubmitFeedbackRequest();
        request.setQaId(10L);
        request.setUserId(1001L);
        request.setRating(5);
        request.setComment("good");

        doThrow(new DuplicateKeyException("duplicate"))
                .when(qaFeedbackMapper).insert(any());

        BusinessException ex = assertThrows(BusinessException.class, () -> qaFeedbackService.submit(request));
        assertEquals("FEEDBACK_002", ex.getErrorCode());
    }

    @Test
    void submitShouldRejectWhenRatingInvalid() {
        SubmitFeedbackRequest request = new SubmitFeedbackRequest();
        request.setQaId(10L);
        request.setUserId(1001L);
        request.setRating(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> qaFeedbackService.submit(request));
        assertEquals("FEEDBACK_001", ex.getErrorCode());
    }
}
