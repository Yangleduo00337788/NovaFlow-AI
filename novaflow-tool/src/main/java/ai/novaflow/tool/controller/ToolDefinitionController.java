package ai.novaflow.tool.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.tool.domain.dto.ToolDefinitionSaveRequest;
import ai.novaflow.tool.domain.dto.ToolTestRequest;
import ai.novaflow.tool.domain.vo.ToolDefinitionVO;
import ai.novaflow.tool.domain.vo.ToolTestResultVO;
import ai.novaflow.tool.service.ToolDefinitionService;
import cn.dev33.satoken.annotation.SaCheckPermission;
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
@SaCheckPermission("agent:edit")
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
public class ToolDefinitionController {

    private final ToolDefinitionService toolDefinitionService;

    @GetMapping
    public ApiResult<PageResult<ToolDefinitionVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String toolType) {
        return ApiResult.ok(toolDefinitionService.page(page, pageSize, keyword, toolType));
    }

    @GetMapping("/options")
    public ApiResult<List<ToolDefinitionVO>> options(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(toolDefinitionService.listEnabled(keyword));
    }

    @GetMapping("/{id}")
    public ApiResult<ToolDefinitionVO> detail(@PathVariable Long id) {
        return ApiResult.ok(toolDefinitionService.detail(id));
    }

    @PostMapping
    public ApiResult<ToolDefinitionVO> create(@Valid @RequestBody ToolDefinitionSaveRequest request) {
        return ApiResult.ok(toolDefinitionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<ToolDefinitionVO> update(
            @PathVariable Long id,
            @Valid @RequestBody ToolDefinitionSaveRequest request) {
        return ApiResult.ok(toolDefinitionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        toolDefinitionService.delete(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/test")
    public ApiResult<ToolTestResultVO> test(
            @PathVariable Long id,
            @RequestBody(required = false) ToolTestRequest request) {
        return ApiResult.ok(toolDefinitionService.test(id, request));
    }
}
