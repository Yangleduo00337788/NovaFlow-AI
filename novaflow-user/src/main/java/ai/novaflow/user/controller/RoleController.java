package ai.novaflow.user.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.user.domain.vo.MemberVO;
import ai.novaflow.user.domain.vo.PermissionVO;
import ai.novaflow.user.domain.vo.RoleVO;
import ai.novaflow.user.service.RoleManagementService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoleController {

    private final RoleManagementService roleManagementService;

    @SaCheckPermission("member:manage")
    @GetMapping("/roles")
    public ApiResult<List<RoleVO>> listRoles() {
        return ApiResult.ok(roleManagementService.listRoles());
    }

    @SaCheckPermission("member:manage")
    @GetMapping("/roles/{id}")
    public ApiResult<RoleVO> roleDetail(@PathVariable Long id) {
        return ApiResult.ok(roleManagementService.getRole(id));
    }

    @SaCheckPermission("member:manage")
    @GetMapping("/roles/{id}/members")
    public ApiResult<List<MemberVO>> roleMembers(@PathVariable Long id) {
        return ApiResult.ok(roleManagementService.listRoleMembers(id));
    }

    @SaCheckPermission("member:manage")
    @GetMapping("/permissions")
    public ApiResult<List<PermissionVO>> listPermissions() {
        return ApiResult.ok(roleManagementService.listPermissions());
    }

    @SaCheckPermission("member:manage")
    @GetMapping("/permissions/grouped")
    public ApiResult<Map<String, List<PermissionVO>>> groupedPermissions() {
        return ApiResult.ok(roleManagementService.listPermissionsGrouped());
    }
}
