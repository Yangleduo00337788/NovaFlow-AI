package ai.novaflow.workflow.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.workflow.domain.dto.WorkflowRunRequest;
import ai.novaflow.workflow.domain.dto.WorkflowSaveRequest;
import ai.novaflow.workflow.domain.vo.WorkflowDetailVO;
import ai.novaflow.workflow.domain.vo.WorkflowRunResultVO;
import ai.novaflow.workflow.domain.vo.WorkflowVO;
import ai.novaflow.workflow.service.WorkflowExecutionService;
import ai.novaflow.workflow.service.WorkflowService;
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

    @GetMapping
    public ApiResult<PageResult<WorkflowVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long applicationId) {
        return ApiResult.ok(workflowService.page(page, pageSize, keyword, applicationId));
    }

    @GetMapping("/options")
    public ApiResult<List<WorkflowVO>> options(@RequestParam(required = false) Long applicationId) {
        return ApiResult.ok(workflowService.listPublishedOptions(applicationId));
    }

    @GetMapping("/{id}")
    public ApiResult<WorkflowDetailVO> detail(@PathVariable Long id) {
        return ApiResult.ok(workflowService.detail(id));
    }

    @PostMapping
    public ApiResult<WorkflowDetailVO> create(@Valid @RequestBody WorkflowSaveRequest request) {
        return ApiResult.ok(workflowService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<WorkflowDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowSaveRequest request) {
        return ApiResult.ok(workflowService.update(id, request));
    }

    @PostMapping("/{id}/publish")
    public ApiResult<WorkflowDetailVO> publish(@PathVariable Long id) {
        return ApiResult.ok(workflowService.publish(id));
    }

    @PostMapping("/{id}/run")
    public ApiResult<WorkflowRunResultVO> run(
            @PathVariable Long id,
            @RequestBody(required = false) WorkflowRunRequest request) {
        return ApiResult.ok(workflowExecutionService.run(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        workflowService.delete(id);
        return ApiResult.ok();
    }
}
