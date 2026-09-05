package ai.novaflow.user.service;
import ai.novaflow.common.security.AccountTypes;
import ai.novaflow.common.security.PermissionCodes;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.user.mapper.UserMapper;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
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
    private final UserMapper userMapper;

    /** 租户域审计：仅本企业记录 */
    public PageResult<AuditLogVO> pageTenant(
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
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("需要企业上下文");
        }
        permissionService.requireAnyPermission(userId, tenantId, PermissionCodes.AUDIT_VIEW);
        return executePage(tenantId, page, pageSize, action, resourceType, startDate, endDate, keyword);
    }

    /** 平台域审计：跨租户全量 */
    public PageResult<AuditLogVO> pagePlatform(
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
        UserEntity user = userMapper.selectOneById(userId);
        if (user == null || !AccountTypes.isPlatform(user.getAccountType())) {
            throw new BusinessException("需要平台管理员权限");
        }
        permissionService.requireAnyPermission(
                userId, tenantId, PermissionCodes.AUDIT_VIEW, PermissionCodes.PLATFORM_MANAGE);
        return executePage(null, page, pageSize, action, resourceType, startDate, endDate, keyword);
    }

    private PageResult<AuditLogVO> executePage(
            Long tenantId,
            int page,
            int pageSize,
            String action,
            String resourceType,
            LocalDate startDate,
            LocalDate endDate,
            String keyword) {
        QueryWrapper query = QueryWrapper.create();
        if (tenantId != null) {
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
