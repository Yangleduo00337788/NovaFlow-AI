package ai.novaflow.user.service;

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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourcePermissionAdminService {

    private final ResourcePermissionMapper resourcePermissionMapper;
    private final ResourceAccessService resourceAccessService;

    public List<ResourcePermissionEntity> list(String resourceType, Long resourceId) {
        Long tenantId = requireTenantId();
        return resourceAccessService.listByResource(tenantId, resourceType, resourceId);
    }

    @Transactional
    public List<ResourcePermissionEntity> replace(
            String resourceType,
            Long resourceId,
            ResourcePermissionSaveRequest request
    ) {
        Long tenantId = requireTenantId();
        long operatorId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        List<ResourcePermissionEntity> existing = resourcePermissionMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("resource_type", resourceType)
                        .eq("resource_id", resourceId)
                        .eq("is_deleted", 0)
        );
        for (ResourcePermissionEntity entity : existing) {
            entity.setIsDeleted(1);
            entity.setUpdatedAt(now);
            resourcePermissionMapper.update(entity);
        }

        List<ResourcePermissionEntity> saved = new ArrayList<>();
        if (request.getGrants() != null) {
            for (ResourcePermissionSaveRequest.GrantItem grant : request.getGrants()) {
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
                saved.add(entity);
            }
        }
        return saved;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }
}
