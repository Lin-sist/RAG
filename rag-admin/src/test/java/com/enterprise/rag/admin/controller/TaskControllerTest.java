package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.async.AsyncTaskManager;
import com.enterprise.rag.common.async.TaskState;
import com.enterprise.rag.common.async.TaskStatus;
import com.enterprise.rag.common.async.TaskStatusService;
import com.enterprise.rag.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskControllerTest {

    private AsyncTaskManager asyncTaskManager;
    private TaskStatusService taskStatusService;
    private CurrentUserService currentUserService;
    private TaskController taskController;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        asyncTaskManager = mock(AsyncTaskManager.class);
        taskStatusService = mock(TaskStatusService.class);
        currentUserService = mock(CurrentUserService.class);
        userDetails = mock(UserDetails.class);

        taskController = new TaskController(asyncTaskManager, taskStatusService, currentUserService);
        when(currentUserService.requireIdentity(any())).thenReturn(new RequestIdentity(1001L, 77L));
    }

    @Test
    void getTaskStatusShouldRejectWhenOwnerMismatch() {
        TaskStatus status = new TaskStatus(
                "task-1",
                "DOCUMENT_INDEX",
                TaskState.RUNNING,
                50,
                "processing",
                null,
                null,
                Instant.now(),
                Instant.now(),
                2002L,
                77L);

        when(asyncTaskManager.getStatus(77L, "task-1")).thenReturn(Optional.of(status));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> taskController.getTaskStatus("task-1", userDetails));

        assertEquals("AUTH_004", ex.getErrorCode());
    }

    @Test
    void cancelTaskShouldSucceedWhenOwnerMatches() {
        TaskStatus status = TaskStatus.pending(77L, "task-2", "DOCUMENT_INDEX", 1001L);
        when(asyncTaskManager.getStatus(77L, "task-2")).thenReturn(Optional.of(status));
        when(asyncTaskManager.cancel(77L, "task-2")).thenReturn(true);

        var response = taskController.cancelTask("task-2", userDetails);

        assertEquals(true, response.getBody().getData().cancelled());
        verify(asyncTaskManager).cancel(77L, "task-2");
        verify(taskStatusService, never()).isCompleted(anyString());
    }

    @Test
    void crossTenantTaskLookupUsesTenantScopedProjectionAndReturnsNotFound() {
        when(asyncTaskManager.getStatus(77L, "shared-task")).thenReturn(Optional.empty());

        BusinessException error = assertThrows(BusinessException.class,
                () -> taskController.getTaskStatus("shared-task", userDetails));

        assertEquals("TASK_001", error.getErrorCode());
        verify(asyncTaskManager).getStatus(77L, "shared-task");
        verify(asyncTaskManager, never()).getStatus("shared-task");
    }

    @Test
    void existsAndCompletedReuseTenantScopedStatus() {
        TaskStatus status = TaskStatus.completed(77L, "task-3", "DOCUMENT_INDEX", null, 1001L);
        when(asyncTaskManager.getStatus(77L, "task-3")).thenReturn(Optional.of(status));

        assertEquals(true, taskController.checkTaskExists("task-3", userDetails).getBody().getData().exists());
        var completed = taskController.checkTaskCompleted("task-3", userDetails).getBody().getData();

        assertEquals(true, completed.completed());
        assertEquals(true, completed.successful());
        verify(asyncTaskManager, never()).getStatus("task-3");
    }

    @Test
    void completedResultUsesSameTenantForStatusAndPayload() {
        TaskStatus status = TaskStatus.completed(
                77L, "task-4", "DOCUMENT_INDEX", "{\"ok\":true}", 1001L);
        when(asyncTaskManager.getStatus(77L, "task-4")).thenReturn(Optional.of(status));
        when(asyncTaskManager.getResult(77L, "task-4", Object.class)).thenReturn(Optional.of("done"));

        var response = taskController.getTaskResult("task-4", userDetails);

        assertEquals("task-4", response.getBody().getData().taskId());
        verify(asyncTaskManager).getResult(77L, "task-4", Object.class);
        verify(asyncTaskManager, never()).getResult("task-4", Object.class);
    }
}
