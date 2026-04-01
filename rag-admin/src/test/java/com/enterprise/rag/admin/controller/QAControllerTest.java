package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.QAResponse;
import com.enterprise.rag.core.rag.service.RAGService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QAControllerTest {

        private RAGService ragService;
        private KnowledgeBaseService knowledgeBaseService;
        private QAHistoryService qaHistoryService;
        private CurrentUserService currentUserService;
        private AuthorizationService authorizationService;
        private QAController qaController;
        private UserDetails userDetails;

        @BeforeEach
        void setUp() {
                ragService = mock(RAGService.class);
                knowledgeBaseService = mock(KnowledgeBaseService.class);
                qaHistoryService = mock(QAHistoryService.class);
                currentUserService = mock(CurrentUserService.class);
                authorizationService = mock(AuthorizationService.class);
                userDetails = mock(UserDetails.class);

                qaController = new QAController(
                                ragService,
                                knowledgeBaseService,
                                qaHistoryService,
                                currentUserService,
                                authorizationService);

                when(currentUserService.requireUserId(any())).thenReturn(1001L);
                doReturn(KnowledgeBaseDTO.builder()
                                .id(10L)
                                .ownerId(1001L)
                                .vectorCollection("kb_test_vector")
                                .isPublic(false)
                                .build())
                                .when(authorizationService)
                                .requireKnowledgeBaseReadAccess(anyLong(), anyLong());
        }

        @Test
        void askShouldIncrementQueryCount() {
                when(ragService.ask(any(QARequest.class))).thenReturn(
                                QAResponse.success("问题", "答案", List.of(), List.of(), Map.of()));

                QAController.AskRequest request = new QAController.AskRequest(
                                10L,
                                "什么是RAG",
                                5,
                                null,
                                Map.of(),
                                true);

                qaController.ask(request, userDetails);

                verify(knowledgeBaseService, times(1)).incrementQueryCount(10L);
                verify(qaHistoryService, times(1)).save(any());
        }

        @Test
        void askStreamShouldIncrementQueryCount() {
                when(ragService.askStream(any(QARequest.class))).thenReturn(Flux.just("chunk-1", "chunk-2", "[DONE]"));

                QAController.AskRequest request = new QAController.AskRequest(
                                10L,
                                "什么是RAG",
                                5,
                                null,
                                Map.of(),
                                false);

                qaController.askStream(request, userDetails);

                verify(ragService, times(1)).askStream(argThat(qaRequest -> qaRequest != null
                                && qaRequest.stream()
                                && qaRequest.topK() == 5
                                && qaRequest.minScore() == QARequest.DEFAULT_MIN_SCORE
                                && qaRequest.enableCache() == false
                                && qaRequest.filter().isEmpty()));
                verify(knowledgeBaseService, times(1)).incrementQueryCount(10L);
                verify(qaHistoryService, times(1)).save(argThat(saveReq -> saveReq != null
                                && "什么是RAG".equals(saveReq.getQuestion())
                                && "chunk-1chunk-2".equals(saveReq.getAnswer())));
        }
}
