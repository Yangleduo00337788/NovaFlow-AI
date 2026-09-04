package ai.novaflow.tool.controller;
import ai.novaflow.common.security.PermissionCodes;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.tool.domain.dto.ToolDefinitionSaveRequest;
import ai.novaflow.tool.domain.dto.ToolTestRequest;
import ai.novaflow.tool.domain.vo.ToolDefinitionVO;
import ai.novaflow.tool.domain.vo.ToolTestResultVO;
import ai.novaflow.tool.service.ToolDefinitionService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
public class ToolDefinitionController {

    private final ToolDefinitionService toolDefinitionService;

    @SaCheckPermission(value = {PermissionCodes.TOOL_READ, PermissionCodes.AGENT_EDIT, PermissionCodes.AGENT_CREATE}, mode = SaMode.OR)
    @GetMapping
    public ApiResult<PageResult<ToolDefinitionVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String toolType) {
        return ApiResult.ok(toolDefinitionService.page(page, pageSize, keyword, toolType));
    }

    @SaCheckPermission(value = {PermissionCodes.TOOL_READ, PermissionCodes.AGENT_EDIT, PermissionCodes.AGENT_CREATE}, mode = SaMode.OR)
    @GetMapping("/options")
    public ApiResult<List<ToolDefinitionVO>> options(@RequestParam(required = false) String keyword) {
        return ApiResult.ok(toolDefinitionService.listEnabled(keyword));
    }

    @SaCheckPermission(value = {PermissionCodes.TOOL_READ, PermissionCodes.AGENT_EDIT, PermissionCodes.AGENT_CREATE}, mode = SaMode.OR)
    @GetMapping("/{id}")
    public ApiResult<ToolDefinitionVO> detail(@PathVariable Long id) {
        return ApiResult.ok(toolDefinitionService.detail(id));
    }

    @SaCheckPermission(value = {PermissionCodes.TOOL_CREATE, PermissionCodes.AGENT_EDIT}, mode = SaMode.OR)
    @PostMapping
    public ApiResult<ToolDefinitionVO> create(@Valid @RequestBody ToolDefinitionSaveRequest request) {
        return ApiResult.ok(toolDefinitionService.create(request));
    }

    @SaCheckPermission(value = {PermissionCodes.TOOL_UPDATE, PermissionCodes.AGENT_EDIT}, mode = SaMode.OR)
    @PutMapping("/{id}")
    public ApiResult<ToolDefinitionVO> update(
            @PathVariable Long id,
            @Valid @RequestBody ToolDefinitionSaveRequest request) {
        return ApiResult.ok(toolDefinitionService.update(id, request));
    }

    @SaCheckPermission(value = {PermissionCodes.TOOL_DELETE, PermissionCodes.AGENT_EDIT}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        toolDefinitionService.delete(id);
        return ApiResult.ok();
    }

    @SaCheckPermission(value = {PermissionCodes.TOOL_UPDATE, PermissionCodes.AGENT_EDIT}, mode = SaMode.OR)
    @PostMapping("/{id}/test")
    public ApiResult<ToolTestResultVO> test(
            @PathVariable Long id,
            @RequestBody(required = false) ToolTestRequest request) {
        return ApiResult.ok(toolDefinitionService.test(id, request));
    }
}
