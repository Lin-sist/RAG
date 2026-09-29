package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.query.RetrievalResult;
import com.enterprise.rag.core.rag.model.QAResponse;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.rag.service.RAGService;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

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
                        public RetrievalResult retrieveWithDiagnostics(String query, RetrieveOptions options) {
                                return new RetrievalResult(retrieve(query, options), Map.of(
                                                "rerankRequestedProvider", "nvidia",
                                                "rerankEffectiveProvider", "heuristic",
                                                "rerankFallbackReason", "timeout"));
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
                when(documentService.getById(11L, 4L)).thenReturn(Optional.of(doc));

                qaController = new QAController(
                                ragService,
                                knowledgeBaseService,
                                qaHistoryService,
                                currentUserService,
                                authorizationService,
                                queryEngine,
                                documentService);

                RequestIdentity identity = new RequestIdentity(1001L, 11L);
                when(currentUserService.requireIdentity(any())).thenReturn(identity);
                when(knowledgeBaseService.requireReadyVectorScope(anyLong(), any(RequestIdentity.class)))
                                .thenReturn(new TenantVectorScope(11L, 10L, "kb_test_vector"));
                doReturn(KnowledgeBaseDTO.builder()
                                .id(10L)
                                .ownerId(1001L)
                                .vectorCollection("kb_test_vector")
                                .isPublic(false)
                                .build())
                                .when(authorizationService)
                                .requireKnowledgeBaseReadAccess(anyLong(), any(RequestIdentity.class));
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

                verify(knowledgeBaseService, times(1)).incrementQueryCount(11L, 10L);
                verify(qaHistoryService, times(1)).save(any(RequestIdentity.class), any());
        }

        @Test
        void askShouldCountButNotSaveHistoryWhenGenerationFails() {
                when(ragService.ask(any(QARequest.class))).thenReturn(
                                QAResponse.error("什么是RAG", "模型服务暂时不可用，请稍后重试"));

                QAController.AskRequest request = new QAController.AskRequest(
                                10L,
                                "什么是RAG",
                                5,
                                null,
                                Map.of(),
                                true);

                var responseEntity = qaController.ask(request, userDetails);

                assertEquals(200, responseEntity.getStatusCode().value());
                assertNotNull(responseEntity.getBody());
                assertNotNull(responseEntity.getBody().getData());
                assertEquals("error", responseEntity.getBody().getData().metadata().get("status"));
                assertTrue(responseEntity.getBody().getData().citations().isEmpty());
                assertTrue(responseEntity.getBody().getData().contexts().isEmpty());
                verify(knowledgeBaseService, times(1)).incrementQueryCount(11L, 10L);
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        @Test
        void askDoesNotSaveNoAnswerOrUnsupportedAsSuccessfulHistory() {
                when(ragService.ask(any(QARequest.class)))
                                .thenReturn(QAResponse.noResult("什么是RAG"))
                                .thenReturn(QAResponse.unsupported("什么是RAG", "fact-intent-v1",
                                                "evidence-no-answer-v1", "UNSUPPORTED", "MULTI_HOP_CUE"));

                qaController.ask(streamRequest(), userDetails);
                qaController.ask(streamRequest(), userDetails);

                verify(knowledgeBaseService, times(2)).incrementQueryCount(11L, 10L);
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        @Test
        void askStreamShouldIncrementQueryCount() {
                Citation citation = Citation.of("chunk-1", "validated snippet");
                when(ragService.askStream(any(QARequest.class))).thenReturn(Flux.deferContextual(context -> {
                        RAGService.StreamTerminalSignal signal = context.get(
                                        RAGService.STREAM_TERMINAL_SIGNAL_CONTEXT_KEY);
                        signal.recordExecutionResult("ANSWER", "NONE", "chunk-1chunk-2",
                                        List.of(citation), Map.of("validCitations", 1));
                        return Flux.just("chunk-1", "chunk-2");
                }));

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
                verify(knowledgeBaseService, times(1)).incrementQueryCount(11L, 10L);
                verify(qaHistoryService, times(1)).save(any(RequestIdentity.class), argThat(saveReq -> saveReq != null
                                && "什么是RAG".equals(saveReq.getQuestion())
                                && "chunk-1chunk-2".equals(saveReq.getAnswer())
                                && List.of(citation).equals(saveReq.getCitations())));
        }

        @Test
        void askStreamShouldCountButNotSavePartialHistoryWhenGenerationFails() {
                when(ragService.askStream(any(QARequest.class))).thenReturn(
                                Flux.concat(Flux.just("partial"), Flux.error(new RuntimeException("synthetic failure"))));

                QAController.AskRequest request = new QAController.AskRequest(
                                10L,
                                "什么是RAG",
                                5,
                                null,
                                Map.of(),
                                false);

                qaController.askStream(request, userDetails);

                verify(knowledgeBaseService, times(1)).incrementQueryCount(11L, 10L);
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        @Test
        void unknownStreamContractFailsBeforeAnyQuestionSideEffect() {
                QAController.AskRequest request = new QAController.AskRequest(
                                10L, "什么是RAG", 5, null, Map.of(), false);

                BusinessException error = assertThrows(BusinessException.class,
                                () -> qaController.askStream(request, "structured-v2", userDetails));

                assertEquals("UNSUPPORTED_STREAM_CONTRACT", error.getErrorCode());
                verify(ragService, never()).askStream(any());
                verify(knowledgeBaseService, never()).incrementQueryCount(anyLong(), anyLong());
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        @Test
        void structuredNoAnswerDoesNotSaveNormalHistory() {
                when(ragService.askStream(any(QARequest.class))).thenReturn(Flux.deferContextual(context -> {
                        RAGService.StreamTerminalSignal signal = context.get(
                                        RAGService.STREAM_TERMINAL_SIGNAL_CONTEXT_KEY);
                        signal.recordExecutionResult("NO_ANSWER", "INSUFFICIENT_EVIDENCE",
                                        "未找到证据", List.of(), Map.of());
                        return Flux.just("未找到证据");
                }));
                QAController.AskRequest request = new QAController.AskRequest(
                                10L, "什么是RAG", 5, null, Map.of(), false);

                qaController.askStream(request, "structured-v1", userDetails);

                verify(knowledgeBaseService, times(1)).incrementQueryCount(11L, 10L);
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        @Test
        void structuredWireUsesNamedTextAndOneTerminalWithoutLegacyDone() throws Exception {
                when(ragService.askStream(any(QARequest.class))).thenReturn(Flux.deferContextual(context -> {
                        RAGService.StreamTerminalSignal signal = context.get(
                                        RAGService.STREAM_TERMINAL_SIGNAL_CONTEXT_KEY);
                        signal.recordExecutionResult("NO_ANSWER", "INSUFFICIENT_EVIDENCE",
                                        "未找到证据", List.of(), Map.of("validCitations", 0));
                        return Flux.just("未找到证据");
                }));
                String wire = streamWire("structured-v1");

                assertTrue(wire.contains("event:text"));
                assertTrue(wire.contains("event:terminal"));
                assertEquals(1, wire.split("event:terminal", -1).length - 1);
                assertTrue(wire.contains("\"finalState\":\"NO_ANSWER\""));
                assertTrue(wire.contains("\"reason\":\"INSUFFICIENT_EVIDENCE\""));
                assertFalse(wire.contains("[DONE]"));
        }

        @Test
        void legacyWireKeepsTextAndDoneWithoutTerminalEvent() throws Exception {
                when(ragService.askStream(any(QARequest.class))).thenReturn(Flux.deferContextual(context -> {
                        RAGService.StreamTerminalSignal signal = context.get(
                                        RAGService.STREAM_TERMINAL_SIGNAL_CONTEXT_KEY);
                        signal.recordExecutionResult("ANSWER", "NONE", "answer", List.of(), Map.of());
                        return Flux.just("answer");
                }));

                String wire = streamWire(null);

                assertTrue(wire.contains("data:answer"));
                assertTrue(wire.contains("[DONE]"));
                assertFalse(wire.contains("event:terminal"));
        }

        @Test
        void structuredAnswerWireCarriesSameRunCitationAndSavesItOnce() throws Exception {
                Citation citation = Citation.of("chunk-1", "validated snippet");
                when(ragService.askStream(any(QARequest.class))).thenReturn(Flux.deferContextual(context -> {
                        RAGService.StreamTerminalSignal signal = context.get(
                                        RAGService.STREAM_TERMINAL_SIGNAL_CONTEXT_KEY);
                        signal.recordExecutionResult("ANSWER", "NONE", "answer", List.of(citation),
                                        Map.of("validCitations", 1));
                        return Flux.just("answer");
                }));

                String wire = streamWire("structured-v1");

                assertTrue(wire.contains("event:text"));
                assertEquals(1, wire.split("event:terminal", -1).length - 1);
                assertTrue(wire.contains("\"finalState\":\"ANSWER\""));
                assertTrue(wire.contains("\"source\":\"chunk-1\""));
                assertTrue(wire.contains("\"snippet\":\"validated snippet\""));
                assertFalse(wire.contains("[DONE]"));
                verify(qaHistoryService, times(1)).save(any(RequestIdentity.class),
                                argThat(saveReq -> List.of(citation).equals(saveReq.getCitations())));
        }

        @Test
        void structuredWireReportsOneSafeErrorAfterPartialText() throws Exception {
                when(ragService.askStream(any(QARequest.class))).thenReturn(
                                Flux.concat(Flux.just("partial"),
                                                Flux.error(new RuntimeException("secret provider body"))));

                String wire = streamWire("structured-v1");

                assertTrue(wire.contains("event:text"));
                assertTrue(wire.contains("partial"));
                assertEquals(1, wire.split("event:terminal", -1).length - 1);
                assertTrue(wire.contains("\"finalState\":\"ERROR\""));
                assertTrue(wire.contains("\"reason\":\"STREAM_FAILED\""));
                assertFalse(wire.contains("secret provider body"));
                assertFalse(wire.contains("[DONE]"));
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        @Test
        void structuredWireClassifiesStreamTimeoutWithoutSuccessfulHistory() throws Exception {
                when(ragService.askStream(any(QARequest.class))).thenReturn(
                                Flux.error(new RuntimeException(new java.util.concurrent.TimeoutException("private"))));

                String wire = streamWire("structured-v1");

                assertEquals(1, wire.split("event:terminal", -1).length - 1);
                assertTrue(wire.contains("\"finalState\":\"ERROR\""));
                assertTrue(wire.contains("\"reason\":\"TIMEOUT\""));
                assertFalse(wire.contains("private"));
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        @Test
        void disconnectAfterLastTextPreventsTerminalAndSuccessfulHistory() throws Exception {
                SseEmitter emitter = mockStreamEmitter();
                AtomicReference<Runnable> completion = new AtomicReference<>();
                AtomicInteger sends = new AtomicInteger();
                doAnswer(invocation -> {
                        completion.set(invocation.getArgument(0));
                        return null;
                }).when(emitter).onCompletion(any(Runnable.class));
                doAnswer(invocation -> {
                        sends.incrementAndGet();
                        assertNotNull(completion.get());
                        completion.get().run();
                        return null;
                }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
                when(ragService.askStream(any(QARequest.class))).thenReturn(completedAnswerStream());

                qaController.askStream(streamRequest(), "structured-v1", userDetails);

                assertEquals(1, sends.get());
                verify(knowledgeBaseService, times(1)).incrementQueryCount(11L, 10L);
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        @Test
        void terminalSendFailurePreventsSuccessfulHistory() throws Exception {
                SseEmitter emitter = mockStreamEmitter();
                AtomicInteger sends = new AtomicInteger();
                doAnswer(invocation -> {
                        if (sends.incrementAndGet() == 2) {
                                throw new IOException("synthetic terminal delivery failure");
                        }
                        return null;
                }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
                when(ragService.askStream(any(QARequest.class))).thenReturn(completedAnswerStream());

                qaController.askStream(streamRequest(), "structured-v1", userDetails);

                assertEquals(2, sends.get());
                verify(knowledgeBaseService, times(1)).incrementQueryCount(11L, 10L);
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        @Test
        void textSendFailureCancelsDeliveryWithoutTerminalOrHistory() throws Exception {
                SseEmitter emitter = mockStreamEmitter();
                AtomicInteger sends = new AtomicInteger();
                doAnswer(invocation -> {
                        sends.incrementAndGet();
                        throw new IOException("synthetic text delivery failure");
                }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
                when(ragService.askStream(any(QARequest.class))).thenReturn(completedAnswerStream());

                qaController.askStream(streamRequest(), "structured-v1", userDetails);

                assertEquals(1, sends.get());
                verify(knowledgeBaseService, times(1)).incrementQueryCount(11L, 10L);
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        @Test
        void streamUsesAuthenticatedTenantScopeForExecutionCountAndHistory() {
                RequestIdentity tenant = new RequestIdentity(2002L, 22L);
                TenantVectorScope scope = new TenantVectorScope(22L, 10L, "kb_tenant_22");
                when(currentUserService.requireIdentity(any())).thenReturn(tenant);
                when(knowledgeBaseService.requireReadyVectorScope(10L, tenant)).thenReturn(scope);
                when(ragService.askStream(any(QARequest.class))).thenReturn(completedAnswerStream());

                qaController.askStream(streamRequest(), "structured-v1", userDetails);

                verify(authorizationService).requireKnowledgeBaseReadAccess(10L, tenant);
                verify(ragService).askStream(argThat(request -> scope.equals(request.scope())));
                verify(knowledgeBaseService).incrementQueryCount(22L, 10L);
                verify(qaHistoryService).save(argThat(tenant::equals),
                                argThat(saved -> saved.getUserId() == 2002L
                                                && saved.getKbId() == 10L));
        }

        @Test
        void unauthorizedStreamFailsBeforeCountingOrRetrieval() {
                doAnswer(invocation -> {
                        throw new BusinessException("AUTH_004", "forbidden");
                }).when(authorizationService).requireKnowledgeBaseReadAccess(anyLong(),
                                any(RequestIdentity.class));

                assertThrows(BusinessException.class,
                                () -> qaController.askStream(streamRequest(), "structured-v1", userDetails));

                verify(knowledgeBaseService, never()).incrementQueryCount(anyLong(), anyLong());
                verify(ragService, never()).askStream(any());
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        @Test
        void serverTimeoutCancelsUpstreamAndPreventsHistory() {
                SseEmitter emitter = mockStreamEmitter();
                AtomicReference<Runnable> timeout = new AtomicReference<>();
                AtomicReference<RAGService.StreamTerminalSignal> signalRef = new AtomicReference<>();
                AtomicBoolean cancelled = new AtomicBoolean();
                doAnswer(invocation -> {
                        timeout.set(invocation.getArgument(0));
                        return null;
                }).when(emitter).onTimeout(any(Runnable.class));
                when(ragService.askStream(any(QARequest.class))).thenReturn(Flux.deferContextual(context -> {
                        signalRef.set(context.get(RAGService.STREAM_TERMINAL_SIGNAL_CONTEXT_KEY));
                        return Flux.<String>never().doOnCancel(() -> cancelled.set(true));
                }));

                qaController.askStream(streamRequest(), "structured-v1", userDetails);
                assertNotNull(timeout.get());
                timeout.get().run();

                assertTrue(cancelled.get());
                assertTrue(signalRef.get().isTimeout());
                verify(knowledgeBaseService, times(1)).incrementQueryCount(11L, 10L);
                verify(qaHistoryService, never()).save(any(RequestIdentity.class), any());
        }

        private SseEmitter mockStreamEmitter() {
                qaController = spy(qaController);
                SseEmitter emitter = mock(SseEmitter.class);
                doReturn(emitter).when(qaController).createStreamEmitter();
                return emitter;
        }

        private Flux<String> completedAnswerStream() {
                return Flux.deferContextual(context -> {
                        RAGService.StreamTerminalSignal signal = context.get(
                                        RAGService.STREAM_TERMINAL_SIGNAL_CONTEXT_KEY);
                        signal.recordExecutionResult("ANSWER", "NONE", "answer", List.of(), Map.of());
                        return Flux.just("answer");
                });
        }

        private QAController.AskRequest streamRequest() {
                return new QAController.AskRequest(10L, "什么是RAG", 5, null, Map.of(), false);
        }

        private String streamWire(String contract) throws Exception {
                MockMvc mvc = MockMvcBuilders.standaloneSetup(qaController)
                                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                                .build();
                SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(userDetails, null, List.of()));
                try {
                        var builder = post("/api/qa/ask/stream")
                                        .contentType("application/json")
                                        .content("{\"kbId\":10,\"question\":\"什么是RAG\"}");
                        if (contract != null) {
                                builder.header("X-RAG-Stream-Contract", contract);
                        }
                        MvcResult started = mvc.perform(builder)
                                        .andExpect(request().asyncStarted()).andReturn();
                        return mvc.perform(asyncDispatch(started)).andReturn()
                                        .getResponse().getContentAsString();
                } finally {
                        SecurityContextHolder.clearContext();
                }
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
                assertEquals("nvidia", data.diagnostics().get("rerankRequestedProvider"));
                assertEquals("heuristic", data.diagnostics().get("rerankEffectiveProvider"));
                assertEquals("timeout", data.diagnostics().get("rerankFallbackReason"));
                assertEquals(true, queryEngineCalled.get());
                verify(documentService, times(1)).getById(11L, 4L);
                verify(knowledgeBaseService, times(0)).incrementQueryCount(anyLong(), anyLong());
                verify(qaHistoryService, times(0)).save(any(RequestIdentity.class), any());
                verify(ragService, times(0)).ask(any());
                verify(ragService, times(0)).askStream(any());
                assertEquals("ok", data.status());
        }

        @Test
        void debugRetrieveShouldNotLeakDocumentTitleFromOtherKnowledgeBase() {
                Document doc = new Document();
                doc.setKbId(999L);
                doc.setTitle("不应泄露的标题.md");
                when(documentService.getById(11L, 4L)).thenReturn(Optional.of(doc));

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
                verify(documentService, times(1)).getById(11L, 4L);
        }

        @Test
        void debugRetrieveShouldFallbackWhenDocumentLookupFails() {
                when(documentService.getById(11L, 4L)).thenThrow(new IllegalStateException("db down"));

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
                verify(documentService, times(1)).getById(11L, 4L);
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
                assertEquals("unknown", data.diagnostics().get("rerankRequestedProvider"));
                assertEquals("not_run", data.diagnostics().get("rerankEffectiveProvider"));
                assertEquals(0, data.diagnostics().get("rerankModelCallCount"));
                assertNull(data.contexts().stream().findFirst().orElse(null));
        }
}
