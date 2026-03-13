package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.qa.dto.*;
import com.enterprise.rag.admin.qa.service.QAFeedbackService;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.model.ApiResponse;
import com.enterprise.rag.common.ratelimit.RateLimit;
import com.enterprise.rag.common.ratelimit.RateLimitDimension;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 问答历史与反馈 API 控制器
 * <p>
 * 提供问答历史查询和反馈提交功能。
 */
@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Tag(name = "问答历史", description = "问答历史查询和反馈提交接口")
public class HistoryController {

        private final QAHistoryService qaHistoryService;
        private final QAFeedbackService qaFeedbackService;
        private final CurrentUserService currentUserService;
        private final AuthorizationService authorizationService;

        /**
         * 分页查询问答历史
         */
        @GetMapping
        @Operation(summary = "查询问答历史", description = "分页查询当前用户的问答历史，按时间倒序排列")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
        })
        public ResponseEntity<ApiResponse<PageResult<QAHistoryDTO>>> getHistory(
                        @Parameter(description = "知识库 ID（可选）") @RequestParam(required = false) Long kbId,
                        @Parameter(description = "页码（从 1 开始）") @RequestParam(defaultValue = "1") @Min(1) int page,
                        @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

                Long userId = currentUserService.requireUserId(userDetails);
                log.debug("查询问答历史: userId={}, kbId={}, page={}, size={}", userId, kbId, page, size);

                QAHistoryPageRequest request = new QAHistoryPageRequest();
                request.setUserId(userId);
                request.setKbId(kbId);
                request.setPage(page);
                request.setSize(size);

                PageResult<QAHistoryDTO> result = qaHistoryService.getPage(request);
                return ResponseEntity.ok(ApiResponse.success(result));
        }

        /**
         * 获取问答历史详情
         */
        @GetMapping("/{id}")
        @Operation(summary = "获取历史详情", description = "根据 ID 获取问答历史详细信息")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功", content = @Content(schema = @Schema(implementation = QAHistoryDTO.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "历史记录不存在")
        })
        public ResponseEntity<ApiResponse<QAHistoryDTO>> getHistoryById(
                        @Parameter(description = "历史记录 ID", required = true) @PathVariable Long id,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long userId = currentUserService.requireUserId(userDetails);
                QAHistoryDTO history = authorizationService.requireHistoryOwner(id, userId);
                log.debug("获取问答历史详情: id={}", id);
                return ResponseEntity.ok(ApiResponse.success(history));
        }

        /**
         * 删除问答历史
         */
        @DeleteMapping("/{id}")
        @RateLimit(maxRequests = 30, windowSeconds = 60, dimension = RateLimitDimension.USER, message = "删除历史请求过于频繁，请稍后重试")
        @Operation(summary = "删除历史记录", description = "删除指定的问答历史记录")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "历史记录不存在")
        })
        public ResponseEntity<ApiResponse<Void>> deleteHistory(
                        @Parameter(description = "历史记录 ID", required = true) @PathVariable Long id,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long userId = currentUserService.requireUserId(userDetails);
                authorizationService.requireHistoryOwner(id, userId);
                log.info("删除问答历史: id={}", id);
                qaHistoryService.delete(id);
                return ResponseEntity.ok(ApiResponse.success());
        }

        /**
         * 提交反馈
         */
        @PostMapping("/{id}/feedback")
        @RateLimit(maxRequests = 20, windowSeconds = 60, dimension = RateLimitDimension.USER, message = "反馈提交过于频繁，请稍后重试")
        @Operation(summary = "提交反馈", description = "对问答结果提交反馈（有用/无用）")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "反馈提交成功", content = @Content(schema = @Schema(implementation = QAFeedbackDTO.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误或已提交过反馈"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "历史记录不存在")
        })
        public ResponseEntity<ApiResponse<QAFeedbackDTO>> submitFeedback(
                        @Parameter(description = "历史记录 ID", required = true) @PathVariable Long id,
                        @Valid @RequestBody FeedbackRequest request,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

                Long userId = currentUserService.requireUserId(userDetails);
                log.info("提交反馈: qaId={}, userId={}, rating={}", id, userId, request.rating());

                authorizationService.requireHistoryOwner(id, userId);

                // 检查是否已提交过反馈
                if (qaFeedbackService.hasUserFeedback(id, userId)) {
                        throw new BusinessException("FEEDBACK_001", "您已对此问答提交过反馈");
                }

                SubmitFeedbackRequest submitRequest = new SubmitFeedbackRequest();
                submitRequest.setQaId(id);
                submitRequest.setUserId(userId);
                submitRequest.setRating(request.rating());
                submitRequest.setComment(request.comment());

                QAFeedbackDTO feedback = qaFeedbackService.submit(submitRequest);
                log.info("反馈提交成功: feedbackId={}", feedback.getId());

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(feedback));
        }

        /**
         * 获取问答的反馈
         */
        @GetMapping("/{id}/feedback")
        @Operation(summary = "获取反馈", description = "获取指定问答的反馈信息")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "历史记录不存在")
        })
        public ResponseEntity<ApiResponse<List<QAFeedbackDTO>>> getFeedback(
                        @Parameter(description = "历史记录 ID", required = true) @PathVariable Long id,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long userId = currentUserService.requireUserId(userDetails);
                authorizationService.requireHistoryOwner(id, userId);
                log.debug("获取问答反馈: qaId={}", id);

                List<QAFeedbackDTO> feedbacks = qaFeedbackService.listByQaId(id);
                return ResponseEntity.ok(ApiResponse.success(feedbacks));
        }

        /**
         * 获取用户的所有反馈
         */
        @GetMapping("/feedback/my")
        @Operation(summary = "获取我的反馈", description = "获取当前用户提交的所有反馈")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
        })
        public ResponseEntity<ApiResponse<List<QAFeedbackDTO>>> getMyFeedbacks(
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long userId = currentUserService.requireUserId(userDetails);
                log.debug("获取用户反馈: userId={}", userId);
                List<QAFeedbackDTO> feedbacks = qaFeedbackService.listByUserId(userId);
                return ResponseEntity.ok(ApiResponse.success(feedbacks));
        }

        /**
         * 反馈请求
         */
        public record FeedbackRequest(
                        @NotNull(message = "评分不能为空") @Min(value = 1, message = "评分最小为 1") @Max(value = 5, message = "评分最大为 5") Integer rating,

                        String comment) {
        }
}
