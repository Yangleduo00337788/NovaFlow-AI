package ai.novaflow.user.service;
import ai.novaflow.common.context.TenantContexts;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.tenant.entity.ResourcePermissionEntity;
import ai.novaflow.tenant.mapper.ResourcePermissionMapper;
import ai.novaflow.user.domain.dto.ResourcePermissionSaveRequest;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResourcePermissionAdminService {

    private final ResourcePermissionMapper resourcePermissionMapper;
    private final ResourceAccessService resourceAccessService;

    public List<ResourcePermissionEntity> list(String resourceType, Long resourceId) {
        Long tenantId = TenantContexts.requireTenantId();
        return resourceAccessService.listByResource(tenantId, resourceType, resourceId);
    }

    @Transactional
    public List<ResourcePermissionEntity> replace(
            String resourceType,
            Long resourceId,
            ResourcePermissionSaveRequest request
    ) {
        Long tenantId = TenantContexts.requireTenantId();
        long operatorId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        List<ResourcePermissionEntity> allRows = resourcePermissionMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("resource_type", resourceType)
                        .eq("resource_id", resourceId)
        );
        Map<String, ResourcePermissionEntity> byKey = new LinkedHashMap<>();
        for (ResourcePermissionEntity row : allRows) {
            byKey.put(grantKey(row.getUserId(), row.getPermissionCode()), row);
        }

        Set<String> desiredKeys = new HashSet<>();
        List<ResourcePermissionEntity> saved = new ArrayList<>();
        if (request.getGrants() != null) {
            for (ResourcePermissionSaveRequest.GrantItem grant : request.getGrants()) {
                if (grant.getUserId() == null || grant.getPermissionCode() == null) {
                    continue;
                }
                String key = grantKey(grant.getUserId(), grant.getPermissionCode());
                if (!desiredKeys.add(key)) {
                    continue;
                }
                ResourcePermissionEntity existing = byKey.get(key);
                if (existing != null) {
                    if (existing.getIsDeleted() == null || existing.getIsDeleted() != 0) {
                        existing.setIsDeleted(0);
                        existing.setUpdatedAt(now);
                        resourcePermissionMapper.update(existing);
                    }
                    saved.add(existing);
                    continue;
                }
                ResourcePermissionEntity entity = new ResourcePermissionEntity();
                entity.setTenantId(tenantId);
                entity.setResourceType(resourceType);
                entity.setResourceId(resourceId);
                entity.setUserId(grant.getUserId());
                entity.setPermissionCode(grant.getPermissionCode());
                entity.setCreatedBy(operatorId);
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                entity.setIsDeleted(0);
                resourcePermissionMapper.insert(entity);
                byKey.put(key, entity);
                saved.add(entity);
            }
        }

        for (ResourcePermissionEntity row : allRows) {
            if (row.getIsDeleted() != null && row.getIsDeleted() == 0) {
                String key = grantKey(row.getUserId(), row.getPermissionCode());
                if (!desiredKeys.contains(key)) {
                    row.setIsDeleted(1);
                    row.setUpdatedAt(now);
                    resourcePermissionMapper.update(row);
                }
            }
        }
        return saved;
    }

    private static String grantKey(Long userId, String permissionCode) {
        return userId + "|" + permissionCode;
    }

}
