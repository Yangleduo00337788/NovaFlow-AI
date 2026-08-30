package ai.novaflow.knowledge.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.knowledge.domain.dto.KnowledgeBaseSaveRequest;
import ai.novaflow.knowledge.domain.vo.DocumentVO;
import ai.novaflow.knowledge.domain.vo.KnowledgeBaseVO;
import ai.novaflow.knowledge.service.DocumentService;
import ai.novaflow.knowledge.service.KnowledgeBaseService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;

    @SaCheckPermission(value = {"knowledge:create", "knowledge:upload"}, mode = SaMode.OR)
    @GetMapping
    public ApiResult<PageResult<KnowledgeBaseVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(knowledgeBaseService.page(page, pageSize, keyword));
    }

    @SaCheckPermission(value = {"knowledge:create", "knowledge:upload"}, mode = SaMode.OR)
    @GetMapping("/{id}")
    public ApiResult<KnowledgeBaseVO> detail(@PathVariable Long id) {
        return ApiResult.ok(knowledgeBaseService.detail(id));
    }

    @SaCheckPermission("knowledge:create")
    @PostMapping
    public ApiResult<KnowledgeBaseVO> create(@Valid @RequestBody KnowledgeBaseSaveRequest request) {
        return ApiResult.ok(knowledgeBaseService.create(request));
    }

    @SaCheckPermission("knowledge:create")
    @PutMapping("/{id}")
    public ApiResult<KnowledgeBaseVO> update(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeBaseSaveRequest request) {
        return ApiResult.ok(knowledgeBaseService.update(id, request));
    }

    @SaCheckPermission("knowledge:create")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return ApiResult.ok();
    }

    @SaCheckPermission(value = {"knowledge:create", "knowledge:upload"}, mode = SaMode.OR)
    @GetMapping("/{id}/documents")
    public ApiResult<PageResult<DocumentVO>> pageDocuments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(documentService.page(id, page, pageSize, keyword));
    }

    @SaCheckPermission("knowledge:upload")
    @PostMapping("/{id}/documents/upload")
    public ApiResult<DocumentVO> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(documentService.upload(id, file));
    }

    @SaCheckPermission("knowledge:upload")
    @PostMapping("/{id}/documents/{documentId}/reprocess")
    public ApiResult<Void> reprocessDocument(
            @PathVariable Long id,
            @PathVariable Long documentId) {
        documentService.triggerReprocess(id, documentId);
        return ApiResult.ok();
    }

    @SaCheckPermission("knowledge:create")
    @DeleteMapping("/{id}/documents/{documentId}")
    public ApiResult<Void> deleteDocument(
            @PathVariable Long id,
            @PathVariable Long documentId) {
        documentService.delete(id, documentId);
        return ApiResult.ok();
    }
}
