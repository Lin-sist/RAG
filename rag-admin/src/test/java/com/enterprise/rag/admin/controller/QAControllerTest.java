package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.model.QAResponse;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.rag.service.RAGService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QAControllerTest {

        private QueryEngine queryEngine;
        private RAGService ragService;
        private KnowledgeBaseService knowledgeBaseService;
        private QAHistoryService qaHistoryService;
        private CurrentUserService currentUserService;
        private AuthorizationService authorizationService;
        private QAController qaController;
        private UserDetails userDetails;
        private AtomicBoolean queryEngineCalled;

        @BeforeEach
        void setUp() {
                ragService = mock(RAGService.class);
                knowledgeBaseService = mock(KnowledgeBaseService.class);
                qaHistoryService = mock(QAHistoryService.class);
                currentUserService = mock(CurrentUserService.class);
                authorizationService = mock(AuthorizationService.class);
                queryEngineCalled = new AtomicBoolean(false);
                queryEngine = new QueryEngine() {
                        @Override
                        public List<RetrievedContext> retrieve(String query, RetrieveOptions options) {
                                queryEngineCalled.set(true);
                                assertEquals("什么是RAG", query);
                                assertEquals("kb_test_vector", options.collectionName());
                                assertEquals(20, options.topK());
                                assertEquals(1.0f, options.minScore());
                                assertEquals(Map.of(), options.filter());
                                assertEquals(true, options.enableRerank());
                                return List.of(
                                                new RetrievedContext("第一段内容\n包含 空格", "doc-a", 0.91f,
                                                                Map.of("title", "Doc A")),
                                                new RetrievedContext("第二段内容", "doc-b", 0.67f, Map.of()));
                        }
                };
                userDetails = mock(UserDetails.class);

                qaController = new QAController(
                                ragService,
                                knowledgeBaseService,
                                qaHistoryService,
                                currentUserService,
                                authorizationService,
                                queryEngine);

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

        @Test
        void debugRetrieveShouldReturnContextsWithoutSideEffects() {
                QAController.RetrievalDebugRequest request = new QAController.RetrievalDebugRequest(
                                10L,
                                "什么是RAG",
                                99,
                                1.5f,
                                null,
                                null);

                var responseEntity = qaController.debugRetrieve(request, userDetails);

                assertEquals(200, responseEntity.getStatusCode().value());
                assertNotNull(responseEntity.getBody());
                assertNotNull(responseEntity.getBody().getData());

                var data = responseEntity.getBody().getData();
                assertEquals(10L, data.kbId());
                assertEquals("什么是RAG", data.question());
                assertEquals(20, data.topK());
                assertEquals(1.0f, data.minScore());
                assertEquals(true, data.enableRerank());
                assertEquals(2, data.contextCount());
                assertEquals(0.91d, data.topScore(), 0.0001d);
                assertEquals((0.91d + 0.67d) / 2.0d, data.avgScore(), 0.0001d);
                assertEquals(2, data.contexts().size());
                assertEquals(1, data.contexts().get(0).rank());
                assertEquals("doc-a", data.contexts().get(0).source());
                assertEquals(0.91d, data.contexts().get(0).score(), 0.0001d);
                assertEquals("第一段内容 包含 空格", data.contexts().get(0).snippet());
                assertEquals(11, data.contexts().get(0).contentLength());
                assertEquals("Doc A", data.contexts().get(0).metadata().get("title"));
                assertEquals(true, queryEngineCalled.get());
                verify(knowledgeBaseService, times(0)).incrementQueryCount(anyLong());
                verify(qaHistoryService, times(0)).save(any());
                verify(ragService, times(0)).ask(any());
                verify(ragService, times(0)).askStream(any());
        }
}
