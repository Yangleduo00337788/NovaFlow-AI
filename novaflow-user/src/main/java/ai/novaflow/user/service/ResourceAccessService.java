package ai.novaflow.user.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.RoleCodes;
import ai.novaflow.tenant.entity.ResourcePermissionEntity;
import ai.novaflow.tenant.mapper.ResourcePermissionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 资源级 ACL：某资源一旦配置了授权记录，则除企业 Owner/Admin 外须命中显式授权。
 */
@Service
@RequiredArgsConstructor
public class ResourceAccessService {

    private final ResourcePermissionMapper resourcePermissionMapper;
    private final PermissionService permissionService;

    public boolean hasResourceAcl(Long tenantId, String resourceType, Long resourceId) {
        if (tenantId == null || !StringUtils.hasText(resourceType) || resourceId == null) {
            return false;
        }
        return resourcePermissionMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("resource_type", resourceType)
                        .eq("resource_id", resourceId)
                        .eq("is_deleted", 0)
        ) > 0;
    }

    public void requireResourceAccess(
            long userId,
            Long tenantId,
            String resourceType,
            Long resourceId,
            String permissionCode
    ) {
        if (!canAccessResource(userId, tenantId, resourceType, resourceId, permissionCode)) {
            throw new BusinessException(40301, "无权限访问该资源");
        }
    }

    public boolean canAccessResource(
            long userId,
            Long tenantId,
            String resourceType,
            Long resourceId,
            String permissionCode
    ) {
        if (tenantId == null || resourceId == null || !StringUtils.hasText(resourceType)) {
            return false;
        }
        if (!hasResourceAcl(tenantId, resourceType, resourceId)) {
            return true;
        }
        if (bypassesResourceAcl(userId, tenantId)) {
            return true;
        }
        return resourcePermissionMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("resource_type", resourceType)
                        .eq("resource_id", resourceId)
                        .eq("user_id", userId)
                        .eq("permission_code", permissionCode)
                        .eq("is_deleted", 0)
        ) > 0;
    }

    public Set<Long> listAccessibleResourceIds(
            long userId,
            Long tenantId,
            String resourceType,
            String permissionCode,
            List<Long> candidateIds
    ) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return Collections.emptySet();
        }
        if (bypassesResourceAcl(userId, tenantId)) {
            return new HashSet<>(candidateIds);
        }
        Set<Long> restricted = new HashSet<>();
        for (Long resourceId : candidateIds) {
            if (!hasResourceAcl(tenantId, resourceType, resourceId)) {
                restricted.add(resourceId);
                continue;
            }
            if (canAccessResource(userId, tenantId, resourceType, resourceId, permissionCode)) {
                restricted.add(resourceId);
            }
        }
        return restricted;
    }

    public List<ResourcePermissionEntity> listByResource(Long tenantId, String resourceType, Long resourceId) {
        return resourcePermissionMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("resource_type", resourceType)
                        .eq("resource_id", resourceId)
                        .eq("is_deleted", 0)
                        .orderBy("id", true)
        );
    }

    private boolean bypassesResourceAcl(long userId, Long tenantId) {
        var role = permissionService.resolveRole(userId, tenantId);
        if (role == null || role.getRoleCode() == null) {
            return false;
        }
        String roleCode = role.getRoleCode();
        return RoleCodes.TENANT_OWNER.equals(roleCode)
                || RoleCodes.TENANT_ADMIN.equals(roleCode)
                || RoleCodes.PLATFORM_ADMIN.equals(roleCode);
    }
}
