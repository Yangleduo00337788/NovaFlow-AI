package ai.novaflow.rag.controller;
import ai.novaflow.common.context.TenantContexts;
import ai.novaflow.common.security.PermissionCodes;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.rag.domain.dto.RetrievalTestRequest;
import ai.novaflow.rag.domain.vo.RetrievalTestResultVO;
import ai.novaflow.rag.retrieval.KnowledgeRetrievalService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeRetrievalController {

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    @SaCheckPermission(value = {
            PermissionCodes.KNOWLEDGE_READ, PermissionCodes.KNOWLEDGE_SEARCH, PermissionCodes.KNOWLEDGE_CREATE, PermissionCodes.KNOWLEDGE_UPLOAD
    }, mode = SaMode.OR)
    @PostMapping("/{id}/retrieve")
    public ApiResult<RetrievalTestResultVO> retrieve(
            @PathVariable Long id,
            @Valid @RequestBody RetrievalTestRequest request) {
        return ApiResult.ok(knowledgeRetrievalService.testRetrieve(id, TenantContexts.requireTenantId(), request));
    }

}
