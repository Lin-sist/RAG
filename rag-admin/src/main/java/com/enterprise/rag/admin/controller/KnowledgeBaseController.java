package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.kb.dto.CreateKnowledgeBaseRequest;
import com.enterprise.rag.admin.kb.dto.DocumentUploadResponse;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseStatistics;
import com.enterprise.rag.admin.kb.dto.UpdateKnowledgeBaseRequest;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.service.DocumentIndexingService;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 知识库 API 控制器
 * <p>
 * 提供知识库的 CRUD 操作和文档上传功能。
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "知识库 CRUD 和文档上传接口")
public class KnowledgeBaseController {

        private final KnowledgeBaseService knowledgeBaseService;
        private final DocumentService documentService;
        private final DocumentIndexingService documentIndexingService;
        private final CurrentUserService currentUserService;
        private final AuthorizationService authorizationService;

        /**
         * 创建知识库
         */
        @PostMapping
        @RateLimit(maxRequests = 20, windowSeconds = 60, dimension = RateLimitDimension.USER, message = "创建知识库请求过于频繁，请稍后重试")
        @Operation(summary = "创建知识库", description = "创建新的知识库")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功", content = @Content(schema = @Schema(implementation = KnowledgeBaseDTO.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
        })
        public ResponseEntity<ApiResponse<KnowledgeBaseDTO>> create(
                        @Valid @RequestBody CreateKnowledgeBaseRequest request,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long ownerId = currentUserService.requireUserId(userDetails);
                log.info("创建知识库请求: name={}, ownerId={}", request.getName(), ownerId);
                KnowledgeBaseDTO kb = knowledgeBaseService.create(request, ownerId);
                log.info("知识库创建成功: id={}, name={}", kb.getId(), kb.getName());
                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(kb));
        }

        /**
         * 获取知识库详情
         */
        @GetMapping("/{id}")
        @Operation(summary = "获取知识库详情", description = "根据 ID 获取知识库详细信息")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功", content = @Content(schema = @Schema(implementation = KnowledgeBaseDTO.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
        })
        public ResponseEntity<ApiResponse<KnowledgeBaseDTO>> getById(
                        @Parameter(description = "知识库 ID", required = true) @PathVariable Long id,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long userId = currentUserService.requireUserId(userDetails);
                authorizationService.requireKnowledgeBaseReadAccess(id, userId);
                log.debug("获取知识库详情: id={}", id);
                return knowledgeBaseService.getById(id)
                                .map(kb -> ResponseEntity.ok(ApiResponse.success(kb)))
                                .orElseThrow(() -> new BusinessException("KB_001", "知识库不存在: " + id));
        }

        /**
         * 获取用户可访问的知识库列表
         */
        @GetMapping
        @Operation(summary = "获取知识库列表", description = "获取当前用户可访问的所有知识库")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
        })
        public ResponseEntity<ApiResponse<List<KnowledgeBaseDTO>>> list(
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long userId = currentUserService.requireUserId(userDetails);
                log.debug("获取知识库列表: userId={}", userId);
                List<KnowledgeBaseDTO> kbs = knowledgeBaseService.getAccessibleByUserId(userId);
                return ResponseEntity.ok(ApiResponse.success(kbs));
        }

        /**
         * 更新知识库
         */
        @PutMapping("/{id}")
        @RateLimit(maxRequests = 30, windowSeconds = 60, dimension = RateLimitDimension.USER, message = "更新知识库请求过于频繁，请稍后重试")
        @Operation(summary = "更新知识库", description = "更新知识库信息")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功", content = @Content(schema = @Schema(implementation = KnowledgeBaseDTO.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
        })
        public ResponseEntity<ApiResponse<KnowledgeBaseDTO>> update(
                        @Parameter(description = "知识库 ID", required = true) @PathVariable Long id,
                        @Valid @RequestBody UpdateKnowledgeBaseRequest request,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long userId = currentUserService.requireUserId(userDetails);
                authorizationService.requireKnowledgeBaseAdminAccess(id, userId);
                log.info("更新知识库请求: id={}", id);
                KnowledgeBaseDTO kb = knowledgeBaseService.update(id, request);
                log.info("知识库更新成功: id={}", id);
                return ResponseEntity.ok(ApiResponse.success(kb));
        }

        /**
         * 删除知识库
         */
        @DeleteMapping("/{id}")
        @RateLimit(maxRequests = 10, windowSeconds = 60, dimension = RateLimitDimension.USER, message = "删除知识库请求过于频繁，请稍后重试")
        @Operation(summary = "删除知识库", description = "删除知识库及其所有文档和向量数据")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
        })
        public ResponseEntity<ApiResponse<Void>> delete(
                        @Parameter(description = "知识库 ID", required = true) @PathVariable Long id,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long userId = currentUserService.requireUserId(userDetails);
                authorizationService.requireKnowledgeBaseAdminAccess(id, userId);
                log.info("删除知识库请求: id={}", id);
                knowledgeBaseService.delete(id);
                log.info("知识库删除成功: id={}", id);
                return ResponseEntity.ok(ApiResponse.success());
        }

        /**
         * 获取知识库统计信息
         */
        @GetMapping("/{id}/statistics")
        @Operation(summary = "获取知识库统计", description = "获取知识库的统计信息（文档数、向量数、查询次数）")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功", content = @Content(schema = @Schema(implementation = KnowledgeBaseStatistics.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
        })
        public ResponseEntity<ApiResponse<KnowledgeBaseStatistics>> getStatistics(
                        @Parameter(description = "知识库 ID", required = true) @PathVariable Long id,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long userId = currentUserService.requireUserId(userDetails);
                authorizationService.requireKnowledgeBaseReadAccess(id, userId);
                log.debug("获取知识库统计: id={}", id);
                KnowledgeBaseStatistics stats = knowledgeBaseService.getStatistics(id);
                return ResponseEntity.ok(ApiResponse.success(stats));
        }

        /**
         * 上传文档到知识库
         */
        @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @RateLimit(maxRequests = 15, windowSeconds = 60, dimension = RateLimitDimension.USER, message = "文档上传请求过于频繁，请稍后重试")
        @Operation(summary = "上传文档", description = "上传文档到知识库，支持 PDF、Markdown、Word、代码文件")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "文档上传成功，异步处理中"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "文件格式不支持或文件过大"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
        })
        public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
                        @Parameter(description = "知识库 ID", required = true) @PathVariable Long id,
                        @Parameter(description = "文档文件", required = true) @RequestParam("file") MultipartFile file,
                        @Parameter(description = "文档标题（可选）") @RequestParam(value = "title", required = false) String title,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) throws IOException {

                Long uploaderId = currentUserService.requireUserId(userDetails);
                authorizationService.requireKnowledgeBaseWriteAccess(id, uploaderId);

                // DOC-05: 基本校验（文件类型白名单和大小限制由 DocumentIndexingService 及 multipart 配置负责）
                if (file.isEmpty()) {
                        throw new BusinessException("DOC_002", "上传文件不能为空");
                }
                String fileName = file.getOriginalFilename();
                if (fileName == null || fileName.isBlank()) {
                        throw new BusinessException("DOC_003", "文件名不能为空");
                }

                log.info("文档上传请求: kbId={}, fileName={}, uploaderId={}", id, fileName, uploaderId);

                // DOC-01: 索引编排完全委托给 DocumentIndexingService
                byte[] fileContent = file.getBytes();
                DocumentUploadResponse response = documentIndexingService.submitIndexing(id, uploaderId, fileContent,
                                fileName, title);

                return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
        }

        /**
         * 获取知识库的文档列表
         */
        @GetMapping("/{id}/documents")
        @Operation(summary = "获取文档列表", description = "获取知识库中的所有文档")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库不存在")
        })
        public ResponseEntity<ApiResponse<List<Document>>> listDocuments(
                        @Parameter(description = "知识库 ID", required = true) @PathVariable Long id,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long userId = currentUserService.requireUserId(userDetails);
                authorizationService.requireKnowledgeBaseReadAccess(id, userId);
                log.debug("获取文档列表: kbId={}", id);
                List<Document> documents = documentService.getByKnowledgeBaseId(id);
                return ResponseEntity.ok(ApiResponse.success(documents));
        }

        /**
         * 删除文档
         */
        @DeleteMapping("/{kbId}/documents/{docId}")
        @RateLimit(maxRequests = 20, windowSeconds = 60, dimension = RateLimitDimension.USER, message = "删除文档请求过于频繁，请稍后重试")
        @Operation(summary = "删除文档", description = "删除知识库中的文档及其向量数据")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "知识库或文档不存在")
        })
        public ResponseEntity<ApiResponse<Void>> deleteDocument(
                        @Parameter(description = "知识库 ID", required = true) @PathVariable Long kbId,
                        @Parameter(description = "文档 ID", required = true) @PathVariable Long docId,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
                Long userId = currentUserService.requireUserId(userDetails);
                authorizationService.requireKnowledgeBaseWriteAccess(kbId, userId);
                log.info("删除文档请求: kbId={}, docId={}", kbId, docId);
                documentService.delete(docId);
                knowledgeBaseService.updateDocumentCount(kbId, -1);
                log.info("文档删除成功: docId={}", docId);
                return ResponseEntity.ok(ApiResponse.success());
        }
}
