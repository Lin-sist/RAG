package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.admin.kb.service.impl.DocumentIndexingServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentIndexTaskRecoveryExecutor implements IndexTaskRecoveryExecutor {

    private final DocumentIndexingServiceImpl indexingService;

    @Override
    public void resume(IndexTaskRecord task, IndexTaskLeaseGuard leaseGuard) {
        indexingService.resumeIndexTask(task, leaseGuard);
    }
}
