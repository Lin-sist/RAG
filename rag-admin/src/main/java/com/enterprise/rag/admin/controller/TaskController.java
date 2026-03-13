package com.enterprise.rag.admin.controller;

import com.enterprise.rag.common.async.AsyncTaskManager;
import com.enterprise.rag.common.async.TaskStatus;
import com.enterprise.rag.common.async.TaskStatusResponse;
import com.enterprise.rag.common.async.TaskStatusService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 任务状态 API 控制器
 * <p>
 * 提供异步任务状态查询功能。
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "异步任务状态查询接口")
public class TaskController {

    private final AsyncTaskManager asyncTaskManager;
    private final TaskStatusService taskStatusService;

    /**
     * 获取任务状态
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "获取任务状态", description = "根据任务 ID 获取异步任务的执行状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功", content = @Content(schema = @Schema(implementation = TaskStatusResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在")
    })
    public ResponseEntity<ApiResponse<TaskStatusResponse>> getTaskStatus(
            @Parameter(description = "任务 ID", required = true) @PathVariable String taskId) {
        log.debug("查询任务状态: taskId={}", taskId);

        TaskStatus status = asyncTaskManager.getStatus(taskId)
                .orElseThrow(() -> new BusinessException("TASK_001", "任务不存在: " + taskId));

        TaskStatusResponse response = TaskStatusResponse.from(status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取任务状态（包含结果）
     */
    @GetMapping("/{taskId}/result")
    @Operation(summary = "获取任务结果", description = "获取已完成任务的执行结果")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功", content = @Content(schema = @Schema(implementation = TaskStatusResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "任务尚未完成")
    })
    public ResponseEntity<ApiResponse<TaskStatusResponse>> getTaskResult(
            @Parameter(description = "任务 ID", required = true) @PathVariable String taskId) {
        log.debug("查询任务结果: taskId={}", taskId);

        TaskStatus status = asyncTaskManager.getStatus(taskId)
                .orElseThrow(() -> new BusinessException("TASK_001", "任务不存在: " + taskId));

        if (!status.isTerminal()) {
            throw new BusinessException("TASK_002", "任务尚未完成: " + taskId);
        }

        // 获取结果
        Object result = null;
        if (status.result() != null) {
            result = asyncTaskManager.getResult(taskId, Object.class).orElse(null);
        }

        TaskStatusResponse response = TaskStatusResponse.from(status, result);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 取消任务
     */
    @PostMapping("/{taskId}/cancel")
    @RateLimit(maxRequests = 20, windowSeconds = 60, dimension = RateLimitDimension.USER, message = "任务取消请求过于频繁，请稍后重试")
    @Operation(summary = "取消任务", description = "取消正在执行的异步任务")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取消成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "任务已完成，无法取消")
    })
    public ResponseEntity<ApiResponse<CancelResponse>> cancelTask(
            @Parameter(description = "任务 ID", required = true) @PathVariable String taskId) {
        log.info("取消任务请求: taskId={}", taskId);

        if (!asyncTaskManager.exists(taskId)) {
            throw new BusinessException("TASK_001", "任务不存在: " + taskId);
        }

        TaskStatus status = asyncTaskManager.getStatus(taskId).orElse(null);
        if (status != null && status.isTerminal()) {
            throw new BusinessException("TASK_003", "任务已完成，无法取消: " + taskId);
        }

        boolean cancelled = asyncTaskManager.cancel(taskId);
        log.info("任务取消结果: taskId={}, cancelled={}", taskId, cancelled);

        return ResponseEntity.ok(ApiResponse.success(new CancelResponse(taskId, cancelled)));
    }

    /**
     * 检查任务是否存在
     */
    @GetMapping("/{taskId}/exists")
    @Operation(summary = "检查任务是否存在", description = "检查指定任务 ID 是否存在")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "检查成功")
    })
    public ResponseEntity<ApiResponse<ExistsResponse>> checkTaskExists(
            @Parameter(description = "任务 ID", required = true) @PathVariable String taskId) {
        log.debug("检查任务是否存在: taskId={}", taskId);
        boolean exists = asyncTaskManager.exists(taskId);
        return ResponseEntity.ok(ApiResponse.success(new ExistsResponse(taskId, exists)));
    }

    /**
     * 检查任务是否完成
     */
    @GetMapping("/{taskId}/completed")
    @Operation(summary = "检查任务是否完成", description = "检查指定任务是否已完成（成功、失败或取消）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "检查成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在")
    })
    public ResponseEntity<ApiResponse<CompletedResponse>> checkTaskCompleted(
            @Parameter(description = "任务 ID", required = true) @PathVariable String taskId) {
        log.debug("检查任务是否完成: taskId={}", taskId);

        if (!asyncTaskManager.exists(taskId)) {
            throw new BusinessException("TASK_001", "任务不存在: " + taskId);
        }

        boolean completed = taskStatusService.isCompleted(taskId);
        boolean successful = taskStatusService.isSuccessful(taskId);

        return ResponseEntity.ok(ApiResponse.success(new CompletedResponse(taskId, completed, successful)));
    }

    /**
     * 取消响应
     */
    public record CancelResponse(String taskId, boolean cancelled) {
    }

    /**
     * 存在检查响应
     */
    public record ExistsResponse(String taskId, boolean exists) {
    }

    /**
     * 完成检查响应
     */
    public record CompletedResponse(String taskId, boolean completed, boolean successful) {
    }
}
