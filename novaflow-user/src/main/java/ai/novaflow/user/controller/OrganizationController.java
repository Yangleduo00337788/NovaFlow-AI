package ai.novaflow.user.controller;
import ai.novaflow.common.security.PermissionCodes;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.user.domain.dto.MemberInviteRequest;
import ai.novaflow.user.domain.dto.MemberUpdateRequest;
import ai.novaflow.user.domain.dto.TenantUpdateRequest;
import ai.novaflow.user.domain.dto.TransferOwnerRequest;
import ai.novaflow.user.domain.dto.WorkspaceSaveRequest;
import ai.novaflow.user.domain.vo.MemberVO;
import ai.novaflow.user.domain.vo.TenantPlanSummaryVO;
import ai.novaflow.user.domain.vo.TenantVO;
import ai.novaflow.user.domain.vo.WorkspaceVO;
import ai.novaflow.user.service.OrganizationService;
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
@RequestMapping("/api/v1/org")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @SaCheckPermission(PermissionCodes.TENANT_MANAGE)
    @GetMapping("/tenant")
    public ApiResult<TenantVO> tenant() {
        return ApiResult.ok(organizationService.getTenant());
    }

    @SaCheckPermission(PermissionCodes.TENANT_MANAGE)
    @PutMapping("/tenant")
    public ApiResult<TenantVO> updateTenant(@Valid @RequestBody TenantUpdateRequest request) {
        return ApiResult.ok(organizationService.updateTenant(request));
    }

    @SaCheckPermission(PermissionCodes.TENANT_DELETE)
    @DeleteMapping("/tenant")
    public ApiResult<Void> deleteTenant() {
        organizationService.deleteOwnedTenant();
        return ApiResult.ok();
    }

    @SaCheckPermission(PermissionCodes.TENANT_TRANSFER)
    @PostMapping("/tenant/transfer-owner")
    public ApiResult<Void> transferOwner(@Valid @RequestBody TransferOwnerRequest request) {
        organizationService.transferOwnership(request.getMemberId());
        return ApiResult.ok();
    }

    @SaCheckPermission(value = {PermissionCodes.DASHBOARD_VIEW, PermissionCodes.BILLING_VIEW, PermissionCodes.TENANT_MANAGE}, mode = SaMode.OR)
    @GetMapping("/plan-summary")
    public ApiResult<TenantPlanSummaryVO> planSummary() {
        return ApiResult.ok(organizationService.getPlanSummary());
    }

    @SaCheckPermission(PermissionCodes.TENANT_MANAGE)
    @GetMapping("/workspaces")
    public ApiResult<List<WorkspaceVO>> workspaces() {
        return ApiResult.ok(organizationService.listWorkspaces());
    }

    @SaCheckPermission(PermissionCodes.TENANT_MANAGE)
    @PostMapping("/workspaces")
    public ApiResult<WorkspaceVO> createWorkspace(@Valid @RequestBody WorkspaceSaveRequest request) {
        return ApiResult.ok(organizationService.createWorkspace(request));
    }

    @SaCheckPermission(PermissionCodes.TENANT_MANAGE)
    @PutMapping("/workspaces/{id}")
    public ApiResult<WorkspaceVO> updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody WorkspaceSaveRequest request) {
        return ApiResult.ok(organizationService.updateWorkspace(id, request));
    }

    @SaCheckPermission(PermissionCodes.TENANT_MANAGE)
    @DeleteMapping("/workspaces/{id}")
    public ApiResult<Void> deleteWorkspace(@PathVariable Long id) {
        organizationService.deleteWorkspace(id);
        return ApiResult.ok();
    }

    @SaCheckPermission(value = {PermissionCodes.USER_READ, PermissionCodes.MEMBER_MANAGE, PermissionCodes.TENANT_MANAGE}, mode = SaMode.OR)
    @GetMapping("/members")
    public ApiResult<PageResult<MemberVO>> members(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId) {
        return ApiResult.ok(organizationService.pageMembers(page, pageSize, keyword, departmentId));
    }

    @SaCheckPermission(value = {PermissionCodes.USER_CREATE, PermissionCodes.MEMBER_MANAGE, PermissionCodes.TENANT_MANAGE}, mode = SaMode.OR)
    @PostMapping("/members/invite")
    public ApiResult<MemberVO> inviteMember(@Valid @RequestBody MemberInviteRequest request) {
        return ApiResult.ok(organizationService.inviteMember(request));
    }

    @SaCheckPermission(value = {PermissionCodes.USER_UPDATE, PermissionCodes.MEMBER_MANAGE, PermissionCodes.TENANT_MANAGE}, mode = SaMode.OR)
    @PutMapping("/members/{id}")
    public ApiResult<MemberVO> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody MemberUpdateRequest request) {
        return ApiResult.ok(organizationService.updateMember(id, request));
    }

    @SaCheckPermission(value = {PermissionCodes.USER_DELETE, PermissionCodes.MEMBER_MANAGE, PermissionCodes.TENANT_MANAGE}, mode = SaMode.OR)
    @DeleteMapping("/members/{id}")
    public ApiResult<Void> removeMember(@PathVariable Long id) {
        organizationService.removeMember(id);
        return ApiResult.ok();
    }
}
