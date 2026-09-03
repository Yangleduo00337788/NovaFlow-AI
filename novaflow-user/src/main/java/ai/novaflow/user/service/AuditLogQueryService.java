package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.util.PageQueryUtils;
import ai.novaflow.user.domain.vo.AuditLogVO;
import ai.novaflow.user.entity.AuditLogEntity;
import ai.novaflow.user.mapper.AuditLogMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogMapper auditLogMapper;
    private final PermissionService permissionService;

    public PageResult<AuditLogVO> page(
            int page,
            int pageSize,
            String action,
            String resourceType,
            LocalDate startDate,
            LocalDate endDate,
            String keyword) {
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);
        long userId = StpUtil.getLoginIdAsLong();
        Long tenantId = TenantContext.getTenantId();
        permissionService.requireAnyPermission(userId, tenantId, "audit:view", "platform:manage");

        QueryWrapper query = QueryWrapper.create();
        if (!isSuperAdmin(userId, tenantId)) {
            query.eq("tenant_id", tenantId);
        }
        if (StringUtils.hasText(action)) {
            query.like("action", action.trim());
        }
        if (StringUtils.hasText(resourceType)) {
            query.eq("resource_type", resourceType.trim());
        }
        if (startDate != null) {
            query.ge("created_at", LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            query.le("created_at", LocalDateTime.of(endDate, LocalTime.MAX));
        }
        if (StringUtils.hasText(keyword)) {
            query.and("(detail LIKE ? OR action LIKE ? OR client_ip LIKE ?)",
                    "%" + keyword.trim() + "%",
                    "%" + keyword.trim() + "%",
                    "%" + keyword.trim() + "%");
        }
        query.orderBy("created_at", false);

        Page<AuditLogEntity> result = auditLogMapper.paginate(Page.of(page, pageSize), query);
        List<AuditLogVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    private boolean isSuperAdmin(long userId, Long tenantId) {
        try {
            permissionService.requireSuperAdmin(userId, tenantId);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private AuditLogVO toVO(AuditLogEntity entity) {
        return AuditLogVO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .userId(entity.getUserId())
                .action(entity.getAction())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .detail(entity.getDetail())
                .clientIp(entity.getClientIp())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
