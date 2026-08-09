package ai.novaflow.user.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.user.domain.dto.MemberInviteRequest;
import ai.novaflow.user.domain.dto.MemberUpdateRequest;
import ai.novaflow.user.domain.dto.TenantUpdateRequest;
import ai.novaflow.user.domain.dto.WorkspaceSaveRequest;
import ai.novaflow.user.domain.vo.MemberVO;
import ai.novaflow.user.domain.vo.TenantPlanSummaryVO;
import ai.novaflow.user.domain.vo.TenantVO;
import ai.novaflow.user.domain.vo.WorkspaceVO;
import ai.novaflow.user.service.OrganizationService;
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

    @GetMapping("/tenant")
    public ApiResult<TenantVO> tenant() {
        return ApiResult.ok(organizationService.getTenant());
    }

    @PutMapping("/tenant")
    public ApiResult<TenantVO> updateTenant(@Valid @RequestBody TenantUpdateRequest request) {
        return ApiResult.ok(organizationService.updateTenant(request));
    }

    @GetMapping("/plan-summary")
    public ApiResult<TenantPlanSummaryVO> planSummary() {
        return ApiResult.ok(organizationService.getPlanSummary());
    }

    @GetMapping("/workspaces")
    public ApiResult<List<WorkspaceVO>> workspaces() {
        return ApiResult.ok(organizationService.listWorkspaces());
    }

    @PostMapping("/workspaces")
    public ApiResult<WorkspaceVO> createWorkspace(@Valid @RequestBody WorkspaceSaveRequest request) {
        return ApiResult.ok(organizationService.createWorkspace(request));
    }

    @PutMapping("/workspaces/{id}")
    public ApiResult<WorkspaceVO> updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody WorkspaceSaveRequest request) {
        return ApiResult.ok(organizationService.updateWorkspace(id, request));
    }

    @DeleteMapping("/workspaces/{id}")
    public ApiResult<Void> deleteWorkspace(@PathVariable Long id) {
        organizationService.deleteWorkspace(id);
        return ApiResult.ok();
    }

    @GetMapping("/members")
    public ApiResult<PageResult<MemberVO>> members(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(organizationService.pageMembers(page, pageSize, keyword));
    }

    @PostMapping("/members/invite")
    public ApiResult<MemberVO> inviteMember(@Valid @RequestBody MemberInviteRequest request) {
        return ApiResult.ok(organizationService.inviteMember(request));
    }

    @PutMapping("/members/{id}")
    public ApiResult<MemberVO> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody MemberUpdateRequest request) {
        return ApiResult.ok(organizationService.updateMember(id, request));
    }

    @DeleteMapping("/members/{id}")
    public ApiResult<Void> removeMember(@PathVariable Long id) {
        organizationService.removeMember(id);
        return ApiResult.ok();
    }
}
