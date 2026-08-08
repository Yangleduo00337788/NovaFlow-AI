package ai.novaflow.rag.controller;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.rag.domain.dto.RetrievalTestRequest;
import ai.novaflow.rag.domain.vo.RetrievalTestResultVO;
import ai.novaflow.rag.retrieval.KnowledgeRetrievalService;
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

    @PostMapping("/{id}/retrieve")
    public ApiResult<RetrievalTestResultVO> retrieve(
            @PathVariable Long id,
            @Valid @RequestBody RetrievalTestRequest request) {
        return ApiResult.ok(knowledgeRetrievalService.testRetrieve(id, requireTenantId(), request));
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}
