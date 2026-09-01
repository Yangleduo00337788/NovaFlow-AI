package ai.novaflow.user.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.user.domain.dto.PlatformTenantUpdateRequest;
import ai.novaflow.user.domain.vo.PlatformGlobalStatsVO;
import ai.novaflow.user.domain.vo.PlatformTenantVO;
import ai.novaflow.user.service.PlatformAdminService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;

    @SaCheckPermission("platform:manage")
    @GetMapping("/tenants")
    public ApiResult<PageResult<PlatformTenantVO>> pageTenants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(platformAdminService.pageTenants(page, pageSize, keyword));
    }

    @SaCheckPermission("platform:manage")
    @GetMapping("/tenants/{id}")
    public ApiResult<PlatformTenantVO> getTenant(@PathVariable Long id) {
        return ApiResult.ok(platformAdminService.getTenant(id));
    }

    @SaCheckPermission("platform:manage")
    @PutMapping("/tenants/{id}")
    public ApiResult<PlatformTenantVO> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody PlatformTenantUpdateRequest request) {
        return ApiResult.ok(platformAdminService.updateTenant(id, request));
    }

    @SaCheckPermission("platform:manage")
    @DeleteMapping("/tenants/{id}")
    public ApiResult<Void> deleteTenant(@PathVariable Long id) {
        platformAdminService.deleteTenant(id);
        return ApiResult.ok();
    }

    @SaCheckPermission("platform:manage")
    @GetMapping("/stats")
    public ApiResult<PlatformGlobalStatsVO> globalStats() {
        return ApiResult.ok(platformAdminService.globalStats());
    }
}
