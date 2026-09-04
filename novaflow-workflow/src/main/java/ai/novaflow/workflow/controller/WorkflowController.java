package ai.novaflow.workflow.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.security.PermissionCodes;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.workflow.domain.dto.WorkflowRunOptions;
import ai.novaflow.workflow.domain.dto.WorkflowRunRequest;
import ai.novaflow.workflow.domain.dto.WorkflowSaveRequest;
import ai.novaflow.workflow.domain.vo.WorkflowDetailVO;
import ai.novaflow.workflow.domain.vo.WorkflowRunResultVO;
import ai.novaflow.workflow.domain.vo.WorkflowVO;
import ai.novaflow.workflow.service.WorkflowExecutionService;
import ai.novaflow.workflow.service.WorkflowService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
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
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowExecutionService workflowExecutionService;

    @SaCheckPermission(value = {PermissionCodes.WORKFLOW_READ, PermissionCodes.WORKFLOW_CREATE, PermissionCodes.WORKFLOW_EDIT}, mode = SaMode.OR)
    @GetMapping
    public ApiResult<PageResult<WorkflowVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long applicationId) {
        return ApiResult.ok(workflowService.page(page, pageSize, keyword, applicationId));
    }

    @SaCheckPermission(value = {PermissionCodes.WORKFLOW_READ, PermissionCodes.WORKFLOW_CREATE, PermissionCodes.WORKFLOW_EDIT, PermissionCodes.AGENT_EDIT}, mode = SaMode.OR)
    @GetMapping("/options")
    public ApiResult<List<WorkflowVO>> options(@RequestParam(required = false) Long applicationId) {
        return ApiResult.ok(workflowService.listPublishedOptions(applicationId));
    }

    @SaCheckPermission(value = {PermissionCodes.WORKFLOW_READ, PermissionCodes.WORKFLOW_CREATE, PermissionCodes.WORKFLOW_EDIT}, mode = SaMode.OR)
    @GetMapping("/{id}")
    public ApiResult<WorkflowDetailVO> detail(@PathVariable Long id) {
        return ApiResult.ok(workflowService.detail(id));
    }

    @SaCheckPermission(PermissionCodes.WORKFLOW_CREATE)
    @PostMapping
    public ApiResult<WorkflowDetailVO> create(@Valid @RequestBody WorkflowSaveRequest request) {
        return ApiResult.ok(workflowService.create(request));
    }

    @SaCheckPermission(PermissionCodes.WORKFLOW_EDIT)
    @PutMapping("/{id}")
    public ApiResult<WorkflowDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowSaveRequest request) {
        return ApiResult.ok(workflowService.update(id, request));
    }

    @SaCheckPermission(PermissionCodes.WORKFLOW_PUBLISH)
    @PostMapping("/{id}/publish")
    public ApiResult<WorkflowDetailVO> publish(@PathVariable Long id) {
        return ApiResult.ok(workflowService.publish(id));
    }

    @SaCheckPermission(value = {PermissionCodes.WORKFLOW_EXECUTE, PermissionCodes.WORKFLOW_EDIT}, mode = SaMode.OR)
    @PostMapping("/{id}/run")
    public ApiResult<WorkflowRunResultVO> run(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowRunRequest request) {
        return ApiResult.ok(workflowExecutionService.run(id, request,
                WorkflowRunOptions.builder()
                        .triggeredByUserId(resolveTriggeredBy())
                        .recordUsage(true)
                        .build()));
    }

    private Long resolveTriggeredBy() {
        return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
    }

    @SaCheckPermission(PermissionCodes.WORKFLOW_DELETE)
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        workflowService.delete(id);
        return ApiResult.ok();
    }
}
