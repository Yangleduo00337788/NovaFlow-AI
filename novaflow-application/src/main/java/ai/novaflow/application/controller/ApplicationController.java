package ai.novaflow.application.controller;

import ai.novaflow.application.domain.dto.ApplicationSaveRequest;
import ai.novaflow.application.domain.vo.ApplicationPublishVO;
import ai.novaflow.application.domain.vo.ApplicationVO;
import ai.novaflow.application.service.ApplicationService;
import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
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
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @SaCheckPermission(value = {"application:read", "application:manage"}, mode = SaMode.OR)
    @GetMapping
    public ApiResult<PageResult<ApplicationVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(applicationService.page(page, pageSize, keyword));
    }

    @SaCheckPermission(value = {
            "application:read", "application:manage", "agent:create", "agent:edit", "workflow:create"
    }, mode = SaMode.OR)
    @GetMapping("/options")
    public ApiResult<List<ApplicationVO>> options() {
        return ApiResult.ok(applicationService.listOptions());
    }

    @SaCheckPermission(value = {"application:read", "application:manage"}, mode = SaMode.OR)
    @GetMapping("/{id}")
    public ApiResult<ApplicationVO> detail(@PathVariable Long id) {
        return ApiResult.ok(applicationService.detail(id));
    }

    @SaCheckPermission("application:manage")
    @PostMapping
    public ApiResult<ApplicationVO> create(@Valid @RequestBody ApplicationSaveRequest request) {
        return ApiResult.ok(applicationService.create(request));
    }

    @SaCheckPermission("application:manage")
    @PutMapping("/{id}")
    public ApiResult<ApplicationVO> update(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationSaveRequest request) {
        return ApiResult.ok(applicationService.update(id, request));
    }

    @SaCheckPermission("application:manage")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return ApiResult.ok();
    }

    @SaCheckPermission(value = {"application:read", "application:manage"}, mode = SaMode.OR)
    @GetMapping("/{id}/publish")
    public ApiResult<ApplicationPublishVO> publishInfo(@PathVariable Long id) {
        return ApiResult.ok(applicationService.getPublishInfo(id));
    }

    @SaCheckPermission("application:manage")
    @PostMapping("/{id}/publish")
    public ApiResult<ApplicationPublishVO> publish(@PathVariable Long id) {
        return ApiResult.ok(applicationService.publish(id));
    }

    @SaCheckPermission("application:manage")
    @PostMapping("/{id}/unpublish")
    public ApiResult<ApplicationPublishVO> unpublish(@PathVariable Long id) {
        return ApiResult.ok(applicationService.unpublish(id));
    }
}
