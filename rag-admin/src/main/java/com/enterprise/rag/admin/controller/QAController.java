package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.qa.dto.SaveQAHistoryRequest;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.model.ApiResponse;
import com.enterprise.rag.common.ratelimit.RateLimit;
import com.enterprise.rag.common.ratelimit.RateLimitDimension;
import com.enterprise.rag.common.trace.TraceContext;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.QAResponse;
import com.enterprise.rag.core.rag.service.RAGService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 问答 API 控制器
 * <p>
 * 提供 RAG 问答功能，支持同步和流式响应。
 */
@Slf4j
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@Tag(name = "问答服务", description = "RAG 问答接口：同步问答、流式问答")
public class QAController {

    private final RAGService ragService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final QAHistoryService qaHistoryService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;

    /**
     * 同步问答
     */
    @PostMapping("/ask")
    @RateLimit(maxRequests = 30, windowSeconds = 60, dimension = RateLimitDimension.USER, message = "问答请求过于频繁，请稍后重试")
    @Operation(summary = "问答", description = "向知识库提问并获取 AI 生成的答案")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "问答成功", content = @Content(schema = @Schema(implementation = QAResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    public ResponseEntity<ApiResponse<QAResponse>> ask(
            @Valid @RequestBody AskRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = currentUserService.requireUserId(userDetails);
        String traceId = TraceContext.getTraceId();
        log.info("问答请求: kbId={}, question={}, userId={}",
                request.kbId(), truncate(request.question(), 50), userId);

        var kb = authorizationService.requireKnowledgeBaseReadAccess(request.kbId(), userId);

        long startTime = System.currentTimeMillis();

        // 构建 QA 请求
        QARequest qaRequest = new QARequest(
                request.question(),
                kb.getVectorCollection(),
                request.topK() != null ? request.topK() : QARequest.DEFAULT_TOP_K,
                request.minScore() != null ? request.minScore() : QARequest.DEFAULT_MIN_SCORE,
                request.filter() != null ? request.filter() : Map.of(),
                request.enableCache() != null ? request.enableCache() : true,
                false);

        // 执行问答
        QAResponse response = ragService.ask(qaRequest);

        // RAG-04: 问答入口计入知识库查询次数
        knowledgeBaseService.incrementQueryCount(request.kbId());

        long latencyMs = System.currentTimeMillis() - startTime;
        log.info("问答完成: traceId={}, kbId={}, userId={}, latencyMs={}, hasResult={}",
                traceId, request.kbId(), userId, latencyMs, response.hasResult());

        // 保存问答历史
        try {
            qaHistoryService.save(SaveQAHistoryRequest.builder()
                    .userId(userId)
                    .kbId(request.kbId())
                    .question(request.question())
                    .answer(response.answer())
                    .citations(response.citations())
                    .traceId(TraceContext.getTraceId())
                    .latencyMs((int) latencyMs)
                    .build());
        } catch (Exception e) {
            log.warn("保存问答历史失败: {}", e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 流式问答
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(maxRequests = 10, windowSeconds = 60, dimension = RateLimitDimension.USER, message = "流式问答请求过于频繁，请稍后重试")
    @Operation(summary = "流式问答", description = "向知识库提问并以流式方式获取 AI 生成的答案")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "流式响应开始"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    public SseEmitter askStream(
            @Valid @RequestBody AskRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = currentUserService.requireUserId(userDetails);
        String traceId = TraceContext.getTraceId();
        log.info("流式问答请求: kbId={}, question={}, userId={}",
                request.kbId(), truncate(request.question(), 50), userId);

        var kb = authorizationService.requireKnowledgeBaseReadAccess(request.kbId(), userId);
        long startTime = System.currentTimeMillis();

        // 构建流式 QA 请求
        QARequest qaRequest = QARequest.stream(
                request.question(),
                kb.getVectorCollection(),
                request.topK() != null ? request.topK() : QARequest.DEFAULT_TOP_K,
                request.minScore() != null ? request.minScore() : QARequest.DEFAULT_MIN_SCORE,
                request.filter() != null ? request.filter() : Map.of(),
                request.enableCache() != null ? request.enableCache() : true);

        // RAG-04: 流式问答入口同样计入查询次数
        knowledgeBaseService.incrementQueryCount(request.kbId());

        // 使用 SseEmitter 而不是 Flux<String>
        // 原因：Flux<String> + text/event-stream 会触发 Tomcat 异步分发，
        // 导致 Spring Security 的 OncePerRequestFilter 在异步线程中丢失安全上下文，
        // 抛出 AccessDeniedException。SseEmitter 直接写入响应流，完全避免这个问题。
        SseEmitter emitter = new SseEmitter(120_000L); // 120秒超时
        StringBuffer answerBuffer = new StringBuffer();

        Disposable subscription = ragService.askStream(qaRequest)
                .doOnSubscribe(s -> log.debug("流式问答开始: kbId={}", request.kbId()))
                .subscribe(
                        chunk -> {
                            if (!"[DONE]".equals(chunk)) {
                                answerBuffer.append(chunk);
                            }
                            try {
                                // SseEmitter.send() 会自动格式化为 "data:chunk\n\n"
                                emitter.send(chunk, MediaType.TEXT_PLAIN);
                            } catch (IOException e) {
                                log.warn("SSE发送失败: {}", e.getMessage());
                                emitter.complete();
                            }
                        },
                        error -> {
                            long latencyMs = System.currentTimeMillis() - startTime;
                            log.error("流式问答错误: traceId={}, kbId={}, userId={}, latencyMs={}, error={}",
                                    traceId, request.kbId(), userId, latencyMs, error.getMessage(), error);
                            saveStreamHistory(userId, request.kbId(), request.question(), answerBuffer.toString(),
                                    startTime);
                            try {
                                String clientError = toStreamClientErrorMessage(error);
                                emitter.send("[ERROR] " + clientError, MediaType.TEXT_PLAIN);
                                emitter.send("[DONE]", MediaType.TEXT_PLAIN);
                                emitter.complete();
                            } catch (IOException ioException) {
                                log.warn("SSE错误消息发送失败: {}", ioException.getMessage());
                                emitter.complete();
                            }
                        },
                        () -> {
                            long latencyMs = System.currentTimeMillis() - startTime;
                            saveStreamHistory(userId, request.kbId(), request.question(), answerBuffer.toString(),
                                    startTime);
                            try {
                                emitter.send("[DONE]", MediaType.TEXT_PLAIN);
                                emitter.complete();
                                log.info("流式问答完成: traceId={}, kbId={}, userId={}, latencyMs={}, answerLength={}",
                                        traceId, request.kbId(), userId, latencyMs, answerBuffer.length());
                            } catch (IOException e) {
                                emitter.complete();
                            }
                        });

        emitter.onCompletion(() -> {
            if (!subscription.isDisposed()) {
                subscription.dispose();
            }
        });
        emitter.onTimeout(() -> {
            if (!subscription.isDisposed()) {
                subscription.dispose();
            }
            emitter.complete();
        });
        emitter.onError(error -> {
            if (!subscription.isDisposed()) {
                subscription.dispose();
            }
        });

        return emitter;
    }

    private void saveStreamHistory(Long userId, Long kbId, String question, String answer, long startTime) {
        try {
            qaHistoryService.save(SaveQAHistoryRequest.builder()
                    .userId(userId)
                    .kbId(kbId)
                    .question(question)
                    .answer(answer)
                    .citations(List.of())
                    .traceId(TraceContext.getTraceId())
                    .latencyMs((int) (System.currentTimeMillis() - startTime))
                    .build());
        } catch (Exception e) {
            log.warn("保存流式问答历史失败: {}", e.getMessage());
        }
    }

    private String toStreamClientErrorMessage(Throwable error) {
        String message = error != null ? error.getMessage() : null;
        if (message != null && message.contains("Max retries exceeded")) {
            return "模型服务当前不稳定（重试耗尽），请稍后重试";
        }
        if (message != null && message.toLowerCase().contains("collection not found")) {
            return "知识库向量索引不存在，请重新上传文档以重建索引";
        }
        if (message != null && (message.contains("429") || message.toLowerCase().contains("too many requests"))) {
            return "模型服务触发限流，请稍后重试";
        }
        if (message != null && message.toLowerCase().contains("timeout")) {
            return "模型服务响应超时，请稍后重试";
        }
        if (message != null && message.contains("No embedding provider is available")) {
            return "系统未配置可用的向量化服务，请先配置 NVIDIA_API_KEY 或启用本地 BGE 服务";
        }
        return "问答服务暂时不可用，请稍后重试";
    }

    /**
     * 简单问答（GET 方式）
     */
    @GetMapping("/ask")
    @Operation(summary = "简单问答", description = "使用 GET 方式进行简单问答")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "问答成功", content = @Content(schema = @Schema(implementation = QAResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    public ResponseEntity<ApiResponse<QAResponse>> askSimple(
            @Parameter(description = "知识库 ID", required = true) @RequestParam Long kbId,
            @Parameter(description = "问题", required = true) @RequestParam String question,
            @Parameter(description = "返回结果数量") @RequestParam(required = false, defaultValue = "5") Integer topK,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        AskRequest request = new AskRequest(kbId, question, topK, null, null, true);
        return ask(request, userDetails);
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null)
            return null;
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    /**
     * 问答请求
     */
    public record AskRequest(
            @NotNull(message = "知识库 ID 不能为空") Long kbId,

            @NotBlank(message = "问题不能为空") String question,

            Integer topK,

            Float minScore,

            Map<String, Object> filter,

            Boolean enableCache) {
    }
}
