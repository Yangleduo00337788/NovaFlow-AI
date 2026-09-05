package ai.novaflow.tenant.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.ResourceAclBypassChecker;
import ai.novaflow.common.security.ResourcePermissionHierarchy;
import ai.novaflow.tenant.entity.ResourcePermissionEntity;
import ai.novaflow.tenant.mapper.ResourcePermissionMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资源级 ACL：某资源一旦配置了授权记录，则除企业 Owner/Admin 外须命中显式授权。
 */
@Service
@RequiredArgsConstructor
public class ResourceAccessService {

    private final ResourcePermissionMapper resourcePermissionMapper;
    private final ResourceAclBypassChecker resourceAclBypassChecker;

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

    public void requireResourceAccessAny(
            long userId,
            Long tenantId,
            String resourceType,
            Long resourceId,
            String... permissionCodes
    ) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            throw new BusinessException(40301, "无权限访问该资源");
        }
        for (String permissionCode : permissionCodes) {
            if (canAccessResource(userId, tenantId, resourceType, resourceId, permissionCode)) {
                return;
            }
        }
        throw new BusinessException(40301, "无权限访问该资源");
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
        if (resourceAclBypassChecker.bypassesResourceAcl(userId, tenantId)) {
            return true;
        }
        return resourcePermissionMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("resource_type", resourceType)
                        .eq("resource_id", resourceId)
                        .eq("user_id", userId)
                        .in("permission_code", ResourcePermissionHierarchy.acceptableGrantCodes(permissionCode))
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
        if (resourceAclBypassChecker.bypassesResourceAcl(userId, tenantId)) {
            return new HashSet<>(candidateIds);
        }
        List<Long> distinctCandidates = candidateIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctCandidates.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> aclProtectedIds = loadAclProtectedResourceIds(tenantId, resourceType, distinctCandidates);
        Set<Long> accessible = new HashSet<>();
        for (Long resourceId : distinctCandidates) {
            if (!aclProtectedIds.contains(resourceId)) {
                accessible.add(resourceId);
            }
        }
        if (aclProtectedIds.isEmpty()) {
            return accessible;
        }
        Set<String> grantCodes = ResourcePermissionHierarchy.acceptableGrantCodes(permissionCode);
        if (grantCodes.isEmpty()) {
            return accessible;
        }
        List<ResourcePermissionEntity> grants = resourcePermissionMapper.selectListByQuery(
                QueryWrapper.create()
                        .select("resource_id")
                        .eq("tenant_id", tenantId)
                        .eq("resource_type", resourceType)
                        .eq("user_id", userId)
                        .in("resource_id", aclProtectedIds)
                        .in("permission_code", grantCodes)
                        .eq("is_deleted", 0)
        );
        for (ResourcePermissionEntity grant : grants) {
            if (grant.getResourceId() != null) {
                accessible.add(grant.getResourceId());
            }
        }
        return accessible;
    }

    private Set<Long> loadAclProtectedResourceIds(
            Long tenantId,
            String resourceType,
            List<Long> candidateIds
    ) {
        List<ResourcePermissionEntity> rows = resourcePermissionMapper.selectListByQuery(
                QueryWrapper.create()
                        .select("resource_id")
                        .eq("tenant_id", tenantId)
                        .eq("resource_type", resourceType)
                        .in("resource_id", candidateIds)
                        .eq("is_deleted", 0)
        );
        Set<Long> protectedIds = new HashSet<>();
        for (ResourcePermissionEntity row : rows) {
            if (row.getResourceId() != null) {
                protectedIds.add(row.getResourceId());
            }
        }
        return protectedIds;
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

    /**
     * 将资源 ACL 可读过滤下推到 SQL，保证分页结果与 total 一致。
     *
     * @param resourceIdColumn 外层表主键列，如 agent.id
     */
    public void applyReadableFilter(
            QueryWrapper query,
            long userId,
            Long tenantId,
            String resourceType,
            String permissionCode,
            String resourceIdColumn
    ) {
        if (tenantId == null || !StringUtils.hasText(resourceType) || !StringUtils.hasText(resourceIdColumn)) {
            return;
        }
        if (resourceAclBypassChecker.bypassesResourceAcl(userId, tenantId)) {
            return;
        }
        Set<String> grantCodes = ResourcePermissionHierarchy.acceptableGrantCodes(permissionCode);
        if (grantCodes.isEmpty()) {
            query.and("1 = 0");
            return;
        }
        String inClause = grantCodes.stream().map(code -> "?").collect(Collectors.joining(", "));
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        params.add(resourceType);
        params.add(tenantId);
        params.add(resourceType);
        params.add(userId);
        params.addAll(grantCodes);
        query.and("""
                (
                  NOT EXISTS (
                    SELECT 1 FROM resource_permission rp_acl
                    WHERE rp_acl.tenant_id = ?
                      AND rp_acl.resource_type = ?
                      AND rp_acl.resource_id = %s
                      AND rp_acl.is_deleted = 0
                  )
                  OR EXISTS (
                    SELECT 1 FROM resource_permission rp_grant
                    WHERE rp_grant.tenant_id = ?
                      AND rp_grant.resource_type = ?
                      AND rp_grant.resource_id = %s
                      AND rp_grant.user_id = ?
                      AND rp_grant.is_deleted = 0
                      AND rp_grant.permission_code IN (%s)
                  )
                )
                """.formatted(resourceIdColumn, resourceIdColumn, inClause),
                params.toArray());
    }
}
