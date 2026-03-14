package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.security.CurrentUserService;
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
        when(currentUserService.requireUserId(any())).thenReturn(1001L);
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
                2002L);

        when(asyncTaskManager.getStatus("task-1")).thenReturn(Optional.of(status));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> taskController.getTaskStatus("task-1", userDetails));

        assertEquals("AUTH_004", ex.getErrorCode());
    }

    @Test
    void cancelTaskShouldSucceedWhenOwnerMatches() {
        TaskStatus status = TaskStatus.pending("task-2", "DOCUMENT_INDEX", 1001L);
        when(asyncTaskManager.getStatus("task-2")).thenReturn(Optional.of(status));
        when(asyncTaskManager.cancel("task-2")).thenReturn(true);

        var response = taskController.cancelTask("task-2", userDetails);

        assertEquals(true, response.getBody().getData().cancelled());
        verify(asyncTaskManager).cancel("task-2");
        verify(taskStatusService, never()).isCompleted(anyString());
    }
}
