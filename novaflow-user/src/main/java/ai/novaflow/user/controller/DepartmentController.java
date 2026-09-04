package ai.novaflow.user.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.user.domain.dto.DepartmentSaveRequest;
import ai.novaflow.user.domain.vo.DepartmentVO;
import ai.novaflow.user.service.DepartmentService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/org/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @SaCheckPermission(value = {"user:read", "member:manage", "tenant:manage"}, mode = SaMode.OR)
    @GetMapping
    public ApiResult<List<DepartmentVO>> list() {
        return ApiResult.ok(departmentService.listTree());
    }

    @SaCheckPermission(value = {"user:update", "member:manage", "tenant:manage"}, mode = SaMode.OR)
    @PostMapping
    public ApiResult<DepartmentVO> create(@Valid @RequestBody DepartmentSaveRequest request) {
        return ApiResult.ok(departmentService.create(request));
    }

    @SaCheckPermission(value = {"user:update", "member:manage", "tenant:manage"}, mode = SaMode.OR)
    @PutMapping("/{id}")
    public ApiResult<DepartmentVO> update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentSaveRequest request) {
        return ApiResult.ok(departmentService.update(id, request));
    }

    @SaCheckPermission(value = {"user:update", "member:manage", "tenant:manage"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResult.ok();
    }
}
