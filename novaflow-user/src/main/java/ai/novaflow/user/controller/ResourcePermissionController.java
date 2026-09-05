package ai.novaflow.user.controller;
import ai.novaflow.common.security.PermissionCodes;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.common.security.ResourceTypes;
import ai.novaflow.tenant.entity.ResourcePermissionEntity;
import ai.novaflow.user.domain.dto.ResourcePermissionSaveRequest;
import ai.novaflow.user.service.ResourcePermissionAdminService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourcePermissionController {

    private final ResourcePermissionAdminService resourcePermissionAdminService;

    @SaCheckPermission(value = {PermissionCodes.MEMBER_MANAGE, PermissionCodes.TENANT_MANAGE, PermissionCodes.ROLE_UPDATE}, mode = SaMode.OR)
    @GetMapping("/{resourceType}/{resourceId}/permissions")
    public ApiResult<List<ResourcePermissionEntity>> list(
            @PathVariable String resourceType,
            @PathVariable Long resourceId
    ) {
        validateResourceType(resourceType);
        return ApiResult.ok(resourcePermissionAdminService.list(resourceType, resourceId));
    }

    @SaCheckPermission(value = {PermissionCodes.MEMBER_MANAGE, PermissionCodes.TENANT_MANAGE, PermissionCodes.ROLE_UPDATE}, mode = SaMode.OR)
    @PutMapping("/{resourceType}/{resourceId}/permissions")
    public ApiResult<List<ResourcePermissionEntity>> save(
            @PathVariable String resourceType,
            @PathVariable Long resourceId,
            @Valid @RequestBody ResourcePermissionSaveRequest request
    ) {
        validateResourceType(resourceType);
        return ApiResult.ok(resourcePermissionAdminService.replace(resourceType, resourceId, request));
    }

    private void validateResourceType(String resourceType) {
        if (!ResourceTypes.AGENT.equals(resourceType)
                && !ResourceTypes.WORKFLOW.equals(resourceType)
                && !ResourceTypes.KNOWLEDGE.equals(resourceType)
                && !ResourceTypes.APPLICATION.equals(resourceType)
                && !ResourceTypes.MODEL.equals(resourceType)
                && !ResourceTypes.TOOL.equals(resourceType)
                && !ResourceTypes.MCP.equals(resourceType)
                && !ResourceTypes.PROMPT.equals(resourceType)) {
            throw new BusinessException("不支持的资源类型: " + resourceType);
        }
    }
}
