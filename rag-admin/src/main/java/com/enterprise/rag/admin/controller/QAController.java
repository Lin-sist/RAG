package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.qa.dto.SaveQAHistoryRequest;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.model.ApiResponse;
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
import reactor.core.publisher.Flux;

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

    /**
     * 同步问答
     */
    @PostMapping("/ask")
    @Operation(summary = "问答", description = "向知识库提问并获取 AI 生成的答案")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "问答成功",
            content = @Content(schema = @Schema(implementation = QAResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "请求参数错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "知识库不存在"
        )
    })
    public ResponseEntity<ApiResponse<QAResponse>> ask(
            @Valid @RequestBody AskRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("问答请求: kbId={}, question={}, userId={}", 
                request.kbId(), truncate(request.question(), 50), userId);
        
        // 验证知识库存在
        var kb = knowledgeBaseService.getById(request.kbId())
                .orElseThrow(() -> new BusinessException("KB_001", "知识库不存在: " + request.kbId()));
        
        long startTime = System.currentTimeMillis();
        
        // 构建 QA 请求
        QARequest qaRequest = new QARequest(
                request.question(),
                kb.getVectorCollection(),
                request.topK() != null ? request.topK() : QARequest.DEFAULT_TOP_K,
                request.filter() != null ? request.filter() : Map.of(),
                request.enableCache() != null ? request.enableCache() : true,
                false
        );
        
        // 执行问答
        QAResponse response = ragService.ask(qaRequest);
        
        long latencyMs = System.currentTimeMillis() - startTime;
        log.info("问答完成: kbId={}, latencyMs={}, hasResult={}", 
                request.kbId(), latencyMs, response.hasResult());
        
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
    @Operation(summary = "流式问答", description = "向知识库提问并以流式方式获取 AI 生成的答案")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "流式响应开始"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "请求参数错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "知识库不存在"
        )
    })
    public Flux<String> askStream(
            @Valid @RequestBody AskRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = extractUserId(userDetails);
        log.info("流式问答请求: kbId={}, question={}, userId={}", 
                request.kbId(), truncate(request.question(), 50), userId);
        
        // 验证知识库存在
        var kb = knowledgeBaseService.getById(request.kbId())
                .orElseThrow(() -> new BusinessException("KB_001", "知识库不存在: " + request.kbId()));
        
        // 构建流式 QA 请求
        QARequest qaRequest = QARequest.stream(request.question(), kb.getVectorCollection());
        
        // 返回流式响应
        return ragService.askStream(qaRequest)
                .doOnSubscribe(s -> log.debug("流式问答开始: kbId={}", request.kbId()))
                .doOnComplete(() -> log.debug("流式问答完成: kbId={}", request.kbId()))
                .doOnError(e -> log.error("流式问答错误: kbId={}, error={}", request.kbId(), e.getMessage()));
    }

    /**
     * 简单问答（GET 方式）
     */
    @GetMapping("/ask")
    @Operation(summary = "简单问答", description = "使用 GET 方式进行简单问答")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "问答成功",
            content = @Content(schema = @Schema(implementation = QAResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "请求参数错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "知识库不存在"
        )
    })
    public ResponseEntity<ApiResponse<QAResponse>> askSimple(
            @Parameter(description = "知识库 ID", required = true)
            @RequestParam Long kbId,
            @Parameter(description = "问题", required = true)
            @RequestParam String question,
            @Parameter(description = "返回结果数量")
            @RequestParam(required = false, defaultValue = "5") Integer topK,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        AskRequest request = new AskRequest(kbId, question, topK, null, true);
        return ask(request, userDetails);
    }

    /**
     * 从 UserDetails 中提取用户 ID
     */
    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            return 1L; // 默认用户 ID（开发环境）
        }
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException e) {
            return 1L;
        }
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    /**
     * 问答请求
     */
    public record AskRequest(
            @NotNull(message = "知识库 ID 不能为空")
            Long kbId,
            
            @NotBlank(message = "问题不能为空")
            String question,
            
            Integer topK,
            
            Map<String, Object> filter,
            
            Boolean enableCache
    ) {}
}
