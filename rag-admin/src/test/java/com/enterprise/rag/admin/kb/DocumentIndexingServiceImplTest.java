package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.dto.DocumentUploadResponse;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.kb.service.impl.DocumentIndexingServiceImpl;
import com.enterprise.rag.common.async.AsyncTask;
import com.enterprise.rag.common.async.AsyncTaskManager;
import com.enterprise.rag.common.async.TaskHandle;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.document.parser.DocumentParserFactory;
import com.enterprise.rag.document.processor.DocumentProcessor;
import com.enterprise.rag.document.processor.ProcessResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentIndexingServiceImplTest {

    private final DocumentService documentService = mock(DocumentService.class);
    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
    private final DocumentProcessor documentProcessor = mock(DocumentProcessor.class);
    private final DocumentParserFactory documentParserFactory = mock(DocumentParserFactory.class);
    private final AsyncTaskManager asyncTaskManager = mock(AsyncTaskManager.class);
    private final EmbeddingService embeddingService = mock(EmbeddingService.class);
    private final VectorStore vectorStore = mock(VectorStore.class);

    private final DocumentIndexingServiceImpl service = new DocumentIndexingServiceImpl(
            documentService,
            knowledgeBaseService,
            documentProcessor,
            documentParserFactory,
            asyncTaskManager,
            embeddingService,
            vectorStore);

    @Test
    void submitIndexingShouldRejectLegacyDocFiles() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "legacy.doc",
                "application/msword",
                "legacy-doc".getBytes());

        when(documentParserFactory.isSupported("doc")).thenReturn(false);
        when(documentParserFactory.getSupportedTypes()).thenReturn(Set.of("pdf", "docx", "md"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitIndexing(10L, 20L, file, null));

        assertEquals("DOC_001", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("不支持的文件类型: doc"));
        verify(documentService, never()).create(any());
        verify(asyncTaskManager, never()).submit(eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any());
    }

    @Test
    void submitIndexingShouldCreateTaskForSupportedDocxFiles() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx-content".getBytes());

        Document created = new Document();
        created.setId(99L);

        when(documentParserFactory.isSupported("docx")).thenReturn(true);
        when(documentService.create(any(Document.class))).thenReturn(created);
        when(asyncTaskManager.submit(eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any()))
                .thenReturn(new TaskHandle<>("task-99", CompletableFuture.completedFuture(mock(ProcessResult.class))));

        DocumentUploadResponse response = service.submitIndexing(10L, 20L, file, "manual");

        assertEquals(99L, response.documentId());
        assertEquals("task-99", response.taskId());
        assertEquals("manual.docx", response.fileName());
        assertEquals("docx", response.fileType());
        assertEquals("PROCESSING", response.status());
        verify(documentService).create(any(Document.class));
        verify(asyncTaskManager).submit(eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any());
    }
}
