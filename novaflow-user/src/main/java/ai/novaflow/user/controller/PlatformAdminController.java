package ai.novaflow.user.controller;
import ai.novaflow.common.security.PermissionCodes;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.user.domain.dto.IpBlacklistCreateRequest;
import ai.novaflow.user.domain.dto.IpBlacklistUpdateRequest;
import ai.novaflow.user.domain.dto.PlatformModelCatalogSaveRequest;
import ai.novaflow.user.domain.dto.PlatformModelProviderUpdateRequest;
import ai.novaflow.user.domain.dto.PlatformSettingsUpdateRequest;
import ai.novaflow.user.domain.dto.PlatformOwnerPasswordResetRequest;
import ai.novaflow.user.domain.dto.PlatformTenantCreateRequest;
import ai.novaflow.user.domain.dto.PlatformTenantUpdateRequest;
import ai.novaflow.user.domain.dto.PlatformUserUpdateRequest;
import ai.novaflow.user.domain.vo.AuditLogVO;
import ai.novaflow.user.domain.vo.IpBlacklistVO;
import ai.novaflow.user.domain.vo.PlatformApiAlertEventVO;
import ai.novaflow.user.domain.vo.PlatformApiMonitorVO;
import ai.novaflow.user.domain.vo.PlatformBillingOverviewVO;
import ai.novaflow.user.domain.vo.PlatformGlobalStatsVO;
import ai.novaflow.user.domain.vo.PlatformModelCatalogVO;
import ai.novaflow.user.domain.vo.PlatformModelOverviewVO;
import ai.novaflow.user.domain.vo.PlatformModelProviderVO;
import ai.novaflow.user.domain.vo.PlatformSettingsVO;
import ai.novaflow.user.domain.vo.PlatformDashboardOverviewVO;
import ai.novaflow.user.domain.vo.PlatformTenantDetailVO;
import ai.novaflow.user.domain.vo.PlatformOnboardingTemplateVO;
import ai.novaflow.user.domain.vo.PlatformOwnerPasswordResetResultVO;
import ai.novaflow.user.domain.vo.PlatformTenantCreateResultVO;
import ai.novaflow.user.domain.vo.PlatformTenantVO;
import ai.novaflow.user.domain.vo.PlatformUserVO;
import ai.novaflow.user.service.AuditLogQueryService;
import ai.novaflow.user.service.IpBlacklistService;
import ai.novaflow.user.service.PlatformAdminService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;
    private final AuditLogQueryService auditLogQueryService;
    private final IpBlacklistService ipBlacklistService;

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/tenants")
    public ApiResult<PageResult<PlatformTenantVO>> pageTenants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(platformAdminService.pageTenants(page, pageSize, keyword));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/tenants/{id}")
    public ApiResult<PlatformTenantVO> getTenant(@PathVariable Long id) {
        return ApiResult.ok(platformAdminService.getTenant(id));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/tenants/{id}/detail")
    public ApiResult<PlatformTenantDetailVO> getTenantDetail(@PathVariable Long id) {
        return ApiResult.ok(platformAdminService.getTenantDetail(id));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/onboarding/templates")
    public ApiResult<List<PlatformOnboardingTemplateVO>> onboardingTemplates() {
        return ApiResult.ok(platformAdminService.listOnboardingTemplates());
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PostMapping("/tenants")
    public ApiResult<PlatformTenantCreateResultVO> createTenant(@Valid @RequestBody PlatformTenantCreateRequest request) {
        return ApiResult.ok(platformAdminService.createTenant(request));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PostMapping("/tenants/{id}/owner/reset-password")
    public ApiResult<PlatformOwnerPasswordResetResultVO> resetTenantOwnerPassword(
            @PathVariable Long id,
            @RequestBody PlatformOwnerPasswordResetRequest request) {
        return ApiResult.ok(platformAdminService.resetTenantOwnerPassword(id, request));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PutMapping("/tenants/{id}")
    public ApiResult<PlatformTenantVO> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody PlatformTenantUpdateRequest request) {
        return ApiResult.ok(platformAdminService.updateTenant(id, request));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @DeleteMapping("/tenants/{id}")
    public ApiResult<Void> deleteTenant(@PathVariable Long id) {
        platformAdminService.deleteTenant(id);
        return ApiResult.ok();
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/stats")
    public ApiResult<PlatformGlobalStatsVO> globalStats() {
        return ApiResult.ok(platformAdminService.globalStats());
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/dashboard/overview")
    public ApiResult<PlatformDashboardOverviewVO> dashboardOverview() {
        return ApiResult.ok(platformAdminService.dashboardOverview());
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/billing/overview")
    public ApiResult<PlatformBillingOverviewVO> billingOverview(
            @RequestParam(required = false) String month) {
        return ApiResult.ok(platformAdminService.billingOverview(month));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/billing/export")
    public ResponseEntity<byte[]> exportBilling(@RequestParam(required = false) String month) {
        byte[] data = platformAdminService.exportBillingCsv(month);
        String filename = "platform-billing-" + (month != null ? month : "current") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(data);
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/models/overview")
    public ApiResult<PlatformModelOverviewVO> modelOverview() {
        return ApiResult.ok(platformAdminService.modelOverview());
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/api-monitor/overview")
    public ApiResult<PlatformApiMonitorVO> apiMonitorOverview() {
        return ApiResult.ok(platformAdminService.apiMonitorOverview());
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/api-monitor/alerts")
    public ApiResult<PageResult<PlatformApiAlertEventVO>> pageApiAlertEvents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(platformAdminService.pageApiAlertEvents(page, pageSize, status));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PostMapping("/api-monitor/alerts/{id}/ack")
    public ApiResult<PlatformApiAlertEventVO> acknowledgeApiAlert(@PathVariable Long id) {
        return ApiResult.ok(platformAdminService.acknowledgeApiAlert(id));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/models/catalog")
    public ApiResult<PageResult<PlatformModelCatalogVO>> pageModelCatalog(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(platformAdminService.pageModelCatalog(page, pageSize, keyword));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PostMapping("/models/catalog")
    public ApiResult<PlatformModelCatalogVO> createModelCatalog(
            @Valid @RequestBody PlatformModelCatalogSaveRequest request) {
        return ApiResult.ok(platformAdminService.saveModelCatalog(null, request));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PutMapping("/models/catalog/{id}")
    public ApiResult<PlatformModelCatalogVO> updateModelCatalog(
            @PathVariable Long id,
            @Valid @RequestBody PlatformModelCatalogSaveRequest request) {
        return ApiResult.ok(platformAdminService.saveModelCatalog(id, request));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @DeleteMapping("/models/catalog/{id}")
    public ApiResult<Void> deleteModelCatalog(@PathVariable Long id) {
        platformAdminService.deleteModelCatalog(id);
        return ApiResult.ok();
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/models/providers")
    public ApiResult<PageResult<PlatformModelProviderVO>> pageModelProviders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) Integer enabled) {
        return ApiResult.ok(platformAdminService.pageModelProviders(
                page, pageSize, keyword, tenantId, providerCode, enabled));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PutMapping("/models/providers/{id}")
    public ApiResult<PlatformModelProviderVO> updateModelProvider(
            @PathVariable Long id,
            @Valid @RequestBody PlatformModelProviderUpdateRequest request) {
        return ApiResult.ok(platformAdminService.updateModelProvider(id, request));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/users")
    public ApiResult<PageResult<PlatformUserVO>> pageUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String accountType) {
        return ApiResult.ok(platformAdminService.pageUsers(page, pageSize, keyword, status, accountType));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/users/{id}")
    public ApiResult<PlatformUserVO> getUser(@PathVariable Long id) {
        return ApiResult.ok(platformAdminService.getUser(id));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PutMapping("/users/{id}")
    public ApiResult<PlatformUserVO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody PlatformUserUpdateRequest request) {
        return ApiResult.ok(platformAdminService.updateUser(id, request));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PostMapping("/users/{id}/logout")
    public ApiResult<Void> forceLogoutUser(@PathVariable Long id) {
        platformAdminService.forceLogoutUser(id);
        return ApiResult.ok();
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @DeleteMapping("/users/{id}")
    public ApiResult<Void> deleteUser(@PathVariable Long id) {
        platformAdminService.deleteUser(id);
        return ApiResult.ok();
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/settings")
    public ApiResult<PlatformSettingsVO> getSettings() {
        return ApiResult.ok(platformAdminService.getSettings());
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PutMapping("/settings")
    public ApiResult<PlatformSettingsVO> updateSettings(@RequestBody PlatformSettingsUpdateRequest request) {
        return ApiResult.ok(platformAdminService.updateSettings(request));
    }

    @SaCheckPermission(value = {PermissionCodes.AUDIT_VIEW, PermissionCodes.PLATFORM_MANAGE}, mode = SaMode.OR)
    @GetMapping("/audit-logs")
    public ApiResult<PageResult<AuditLogVO>> pageAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(auditLogQueryService.pagePlatform(
                page, pageSize, action, resourceType, startDate, endDate, keyword));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/login-logs")
    public ApiResult<PageResult<AuditLogVO>> pageLoginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResult.ok(platformAdminService.pageLoginLogs(page, pageSize, keyword, startDate, endDate));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @GetMapping("/ip-blacklist")
    public ApiResult<PageResult<IpBlacklistVO>> pageIpBlacklist(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(ipBlacklistService.page(page, pageSize, keyword));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PostMapping("/ip-blacklist")
    public ApiResult<IpBlacklistVO> createIpBlacklist(@Valid @RequestBody IpBlacklistCreateRequest request) {
        return ApiResult.ok(ipBlacklistService.create(request));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @PutMapping("/ip-blacklist/{id}")
    public ApiResult<IpBlacklistVO> updateIpBlacklist(
            @PathVariable Long id,
            @Valid @RequestBody IpBlacklistUpdateRequest request) {
        return ApiResult.ok(ipBlacklistService.update(id, request));
    }

    @SaCheckPermission(PermissionCodes.PLATFORM_MANAGE)
    @DeleteMapping("/ip-blacklist/{id}")
    public ApiResult<Void> deleteIpBlacklist(@PathVariable Long id) {
        ipBlacklistService.delete(id);
        return ApiResult.ok();
    }
}
