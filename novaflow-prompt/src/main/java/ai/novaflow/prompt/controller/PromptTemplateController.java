package ai.novaflow.prompt.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.prompt.domain.dto.PromptTemplateSaveRequest;
import ai.novaflow.prompt.domain.dto.PromptTestRequest;
import ai.novaflow.prompt.domain.vo.PromptTemplateVO;
import ai.novaflow.prompt.domain.vo.PromptTestResultVO;
import ai.novaflow.prompt.domain.vo.PromptVersionVO;
import ai.novaflow.prompt.service.PromptTemplateService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    @GetMapping
    public ApiResult<PageResult<PromptTemplateVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return ApiResult.ok(promptTemplateService.page(page, pageSize, keyword, category));
    }

    @GetMapping("/options")
    public ApiResult<List<PromptTemplateVO>> options(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(promptTemplateService.listOptions(keyword));
    }

    @GetMapping("/{id}")
    public ApiResult<PromptTemplateVO> detail(@PathVariable Long id) {
        return ApiResult.ok(promptTemplateService.detail(id));
    }

    @GetMapping("/{id}/versions")
    public ApiResult<List<PromptVersionVO>> versions(@PathVariable Long id) {
        return ApiResult.ok(promptTemplateService.listVersions(id));
    }

    @PostMapping
    public ApiResult<PromptTemplateVO> create(@Valid @RequestBody PromptTemplateSaveRequest request) {
        return ApiResult.ok(promptTemplateService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<PromptTemplateVO> update(
            @PathVariable Long id,
            @Valid @RequestBody PromptTemplateSaveRequest request) {
        return ApiResult.ok(promptTemplateService.update(id, request));
    }

    @PostMapping("/{id}/rollback")
    public ApiResult<PromptTemplateVO> rollback(
            @PathVariable Long id,
            @RequestParam Integer version) {
        return ApiResult.ok(promptTemplateService.rollback(id, version));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        promptTemplateService.delete(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/test")
    public ApiResult<PromptTestResultVO> test(
            @PathVariable Long id,
            @RequestBody(required = false) PromptTestRequest request) {
        return ApiResult.ok(promptTemplateService.test(id, request));
    }
}
