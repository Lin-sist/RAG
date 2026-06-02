package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.service.DocumentService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        private DocumentService documentService;
        private QAController qaController;
        private UserDetails userDetails;
        private AtomicBoolean queryEngineCalled;
        private AtomicReference<Float> expectedMinScore;
        private List<RetrievedContext> debugContexts;
        private List<QueryEngine.QueryVariantInfo> debugQueryVariants;
        private RuntimeException retrieveFailure;
        private RuntimeException queryVariantsFailure;

        @BeforeEach
        void setUp() {
                ragService = mock(RAGService.class);
                knowledgeBaseService = mock(KnowledgeBaseService.class);
                qaHistoryService = mock(QAHistoryService.class);
                currentUserService = mock(CurrentUserService.class);
                authorizationService = mock(AuthorizationService.class);
                documentService = mock(DocumentService.class);
                queryEngineCalled = new AtomicBoolean(false);
                expectedMinScore = new AtomicReference<>(1.0f);
                debugContexts = List.of(
                                new RetrievedContext("第一段内容\n包含 空格", "0", 0.91f,
                                                Map.of(
                                                                "title", "Doc A",
                                                                "documentId", 4L,
                                                                "chunkIndex", 0,
                                                                "startIndex", 0,
                                                                "endIndex", 155)),
                                new RetrievedContext("第二段内容", "doc-b", 0.67f, Map.of()));
                debugQueryVariants = List.of(new QueryEngine.QueryVariantInfo("什么是RAG", 1.0f));
                retrieveFailure = null;
                queryVariantsFailure = null;
                queryEngine = new QueryEngine() {
                        @Override
                        public List<RetrievedContext> retrieve(String query, RetrieveOptions options) {
                                if (retrieveFailure != null) {
                                        throw retrieveFailure;
                                }
                                queryEngineCalled.set(true);
                                assertEquals("什么是RAG", query);
                                assertEquals("kb_test_vector", options.collectionName());
                                assertEquals(20, options.topK());
                                assertEquals(expectedMinScore.get(), options.minScore());
                                assertEquals(Map.of(), options.filter());
                                assertEquals(true, options.enableRerank());
                                return debugContexts;
                        }

                        @Override
                        public List<QueryEngine.QueryVariantInfo> explainQueryVariants(String query) {
                                if (queryVariantsFailure != null) {
                                        throw queryVariantsFailure;
                                }
                                return debugQueryVariants;
                        }
                };
                userDetails = mock(UserDetails.class);

                Document doc = new Document();
                doc.setKbId(10L);
                doc.setTitle("Spring Boot 入门测试文档.md");
                when(documentService.getById(4L)).thenReturn(Optional.of(doc));

                qaController = new QAController(
                                ragService,
                                knowledgeBaseService,
                                qaHistoryService,
                                currentUserService,
                                authorizationService,
                                queryEngine,
                                documentService);

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
                assertEquals("0", data.contexts().get(0).source());
                assertEquals("Spring Boot 入门测试文档.md", data.contexts().get(0).displaySource());
                assertEquals(4L, data.contexts().get(0).documentId());
                assertEquals(0, data.contexts().get(0).chunkIndex());
                assertEquals(0, data.contexts().get(0).startIndex());
                assertEquals(155, data.contexts().get(0).endIndex());
                assertEquals(0.91d, data.contexts().get(0).score(), 0.0001d);
                assertEquals("第一段内容 包含 空格", data.contexts().get(0).snippet());
                assertEquals(11, data.contexts().get(0).contentLength());
                assertEquals("Doc A", data.contexts().get(0).metadata().get("title"));
                assertEquals(true, queryEngineCalled.get());
                verify(documentService, times(1)).getById(4L);
                verify(knowledgeBaseService, times(0)).incrementQueryCount(anyLong());
                verify(qaHistoryService, times(0)).save(any());
                verify(ragService, times(0)).ask(any());
                verify(ragService, times(0)).askStream(any());
                assertEquals("ok", data.status());
        }

        @Test
        void debugRetrieveShouldNotLeakDocumentTitleFromOtherKnowledgeBase() {
                Document doc = new Document();
                doc.setKbId(999L);
                doc.setTitle("不应泄露的标题.md");
                when(documentService.getById(4L)).thenReturn(Optional.of(doc));

                QAController.RetrievalDebugRequest request = new QAController.RetrievalDebugRequest(
                                10L,
                                "什么是RAG",
                                99,
                                1.5f,
                                null,
                                true);

                var responseEntity = qaController.debugRetrieve(request, userDetails);

                assertEquals(200, responseEntity.getStatusCode().value());
                assertNotNull(responseEntity.getBody());
                assertNotNull(responseEntity.getBody().getData());
                assertEquals("0", responseEntity.getBody().getData().contexts().get(0).source());
                assertEquals("0", responseEntity.getBody().getData().contexts().get(0).displaySource());
                verify(documentService, times(1)).getById(4L);
        }

        @Test
        void debugRetrieveShouldFallbackWhenDocumentLookupFails() {
                when(documentService.getById(4L)).thenThrow(new IllegalStateException("db down"));

                QAController.RetrievalDebugRequest request = new QAController.RetrievalDebugRequest(
                                10L,
                                "什么是RAG",
                                99,
                                1.5f,
                                null,
                                true);

                var responseEntity = qaController.debugRetrieve(request, userDetails);

                assertEquals(200, responseEntity.getStatusCode().value());
                assertNotNull(responseEntity.getBody());
                assertNotNull(responseEntity.getBody().getData());
                assertEquals("0", responseEntity.getBody().getData().contexts().get(0).source());
                assertEquals("0", responseEntity.getBody().getData().contexts().get(0).displaySource());
                verify(documentService, times(1)).getById(4L);
        }

        @Test
        void debugRetrieveShouldDefaultNonFiniteMinScore() {
                expectedMinScore.set(QARequest.DEFAULT_MIN_SCORE);
                QAController.RetrievalDebugRequest request = new QAController.RetrievalDebugRequest(
                                10L,
                                "什么是RAG",
                                99,
                                Float.NaN,
                                null,
                                null);

                var responseEntity = qaController.debugRetrieve(request, userDetails);

                assertEquals(200, responseEntity.getStatusCode().value());
                assertNotNull(responseEntity.getBody());
                assertNotNull(responseEntity.getBody().getData());
                assertEquals(QARequest.DEFAULT_MIN_SCORE, responseEntity.getBody().getData().minScore());
                assertEquals(true, queryEngineCalled.get());
        }

        @Test
        void debugRetrieveShouldHandleNullMetadataAndBlankContent() {
                debugContexts = new ArrayList<>();
                debugContexts.add(new RetrievedContext(null, null, Float.NaN, null));
                debugContexts.add(new RetrievedContext("", "", 0.5f, new HashMap<>()));

                QAController.RetrievalDebugRequest request = new QAController.RetrievalDebugRequest(
                                10L,
                                "什么是RAG",
                                99,
                                1.5f,
                                null,
                                null);

                var responseEntity = qaController.debugRetrieve(request, userDetails);

                assertEquals(200, responseEntity.getStatusCode().value());
                var data = responseEntity.getBody().getData();
                assertEquals("ok", data.status());
                assertEquals(2, data.contexts().size());
                assertEquals("unknown", data.contexts().get(0).source());
                assertEquals("unknown", data.contexts().get(0).chunkId());
                assertEquals("", data.contexts().get(0).contentPreview());
                assertEquals(0, data.contexts().get(0).contentLength());
                assertEquals(0.0d, data.contexts().get(0).score(), 0.0001d);
                assertTrue(data.contexts().get(0).metadata().isEmpty());
        }

        @Test
        void debugRetrieveShouldExtractDocumentAndChunkIdsFromLongIntegerAndStringMetadata() {
                debugContexts = List.of(
                                new RetrievedContext("Long metadata", null, 0.9f,
                                                Map.of("documentId", 11L, "chunkId", 21L)),
                                new RetrievedContext("Integer metadata", null, 0.8f,
                                                Map.of("documentId", 12, "chunkId", 22)),
                                new RetrievedContext("String metadata", null, 0.7f,
                                                Map.of("documentId", "13", "chunkId", "23")));

                QAController.RetrievalDebugRequest request = new QAController.RetrievalDebugRequest(
                                10L,
                                "什么是RAG",
                                99,
                                1.5f,
                                null,
                                null);

                var data = qaController.debugRetrieve(request, userDetails).getBody().getData();

                assertEquals(11L, data.contexts().get(0).documentId());
                assertEquals("21", data.contexts().get(0).chunkId());
                assertEquals(12L, data.contexts().get(1).documentId());
                assertEquals("22", data.contexts().get(1).chunkId());
                assertEquals(13L, data.contexts().get(2).documentId());
                assertEquals("23", data.contexts().get(2).chunkId());
        }

        @Test
        void debugRetrieveShouldFlattenJsonStringMetadataPayload() {
                debugContexts = List.of(new RetrievedContext(
                                "json metadata",
                                null,
                                0.8f,
                                Map.of("metadata", "{\"source\":\"springboot-basics.md\",\"documentId\":\"44\",\"chunkId\":\"44-2\"}")));

                QAController.RetrievalDebugRequest request = new QAController.RetrievalDebugRequest(
                                10L,
                                "什么是RAG",
                                99,
                                1.5f,
                                null,
                                null);

                var item = qaController.debugRetrieve(request, userDetails).getBody().getData().contexts().get(0);

                assertEquals("springboot-basics.md", item.source());
                assertEquals(44L, item.documentId());
                assertEquals("44-2", item.chunkId());
        }

        @Test
        void debugRetrieveShouldAllowEmptyQueryVariants() {
                debugQueryVariants = List.of();

                QAController.RetrievalDebugRequest request = new QAController.RetrievalDebugRequest(
                                10L,
                                "什么是RAG",
                                99,
                                1.5f,
                                null,
                                null);

                var data = qaController.debugRetrieve(request, userDetails).getBody().getData();

                assertTrue(data.queryVariants().isEmpty());
                assertEquals("ok", data.status());
        }

        @Test
        void debugRetrieveShouldKeepWorkingWhenQueryVariantExplanationFails() {
                queryVariantsFailure = new IllegalStateException("variant boom");

                QAController.RetrievalDebugRequest request = new QAController.RetrievalDebugRequest(
                                10L,
                                "什么是RAG",
                                99,
                                1.5f,
                                null,
                                null);

                var data = qaController.debugRetrieve(request, userDetails).getBody().getData();

                assertTrue(data.queryVariants().isEmpty());
                assertEquals("ok", data.status());
                assertEquals(1, data.warnings().size());
                assertTrue(data.warnings().get(0).contains("queryVariants"));
        }

        @Test
        void debugRetrieveShouldReturnDebugMessageWhenRetrieveFails() {
                retrieveFailure = new IllegalStateException("collection not found: kb_test_vector");

                QAController.RetrievalDebugRequest request = new QAController.RetrievalDebugRequest(
                                10L,
                                "什么是RAG",
                                99,
                                1.5f,
                                null,
                                null);

                var responseEntity = qaController.debugRetrieve(request, userDetails);

                assertEquals(200, responseEntity.getStatusCode().value());
                var data = responseEntity.getBody().getData();
                assertEquals("retrieve_failed", data.status());
                assertTrue(data.contexts().isEmpty());
                assertTrue(data.message().contains("向量集合不存在"));
                assertNull(data.contexts().stream().findFirst().orElse(null));
        }
}
