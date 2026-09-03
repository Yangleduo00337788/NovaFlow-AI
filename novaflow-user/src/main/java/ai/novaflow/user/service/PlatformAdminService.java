package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.util.PageQueryUtils;
import ai.novaflow.model.mapper.TokenUsageMapper;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.user.domain.dto.PlatformTenantUpdateRequest;
import ai.novaflow.user.domain.vo.PlatformGlobalStatsVO;
import ai.novaflow.user.domain.vo.PlatformTenantVO;
import ai.novaflow.user.mapper.PlatformStatsMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PlatformAdminService {

    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final PermissionService permissionService;
    private final TokenUsageMapper tokenUsageMapper;
    private final PlatformStatsMapper platformStatsMapper;
    private final AuditLogService auditLogService;

    public PageResult<PlatformTenantVO> pageTenants(int page, int pageSize, String keyword) {
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);
        requireSuperAdmin();
        QueryWrapper query = QueryWrapper.create().eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            query.and("(tenant_name LIKE ? OR tenant_code LIKE ? OR contact_email LIKE ?)",
                    "%" + keyword.trim() + "%",
                    "%" + keyword.trim() + "%",
                    "%" + keyword.trim() + "%");
        }
        query.orderBy("created_at", false);
        Page<TenantEntity> result = tenantMapper.paginate(Page.of(page, pageSize), query);
        List<PlatformTenantVO> list = result.getRecords().stream().map(this::toPlatformTenantVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    public PlatformTenantVO getTenant(Long tenantId) {
        requireSuperAdmin();
        TenantEntity tenant = getTenantOrThrow(tenantId);
        return toPlatformTenantVO(tenant);
    }

    @Transactional
    public PlatformTenantVO updateTenant(Long tenantId, PlatformTenantUpdateRequest request) {
        requireSuperAdmin();
        TenantEntity tenant = getTenantOrThrow(tenantId);
        tenant.setTenantName(request.getTenantName().trim());
        tenant.setContactName(trimToNull(request.getContactName()));
        tenant.setContactEmail(trimToNull(request.getContactEmail()));
        tenant.setContactPhone(trimToNull(request.getContactPhone()));
        if (StringUtils.hasText(request.getPlanType())) {
            tenant.setPlanType(normalizePlanType(request.getPlanType()));
        }
        if (request.getStatus() != null) {
            tenant.setStatus(request.getStatus());
            if (request.getStatus() != 1) {
                kickTenantSessions(tenant.getId());
            }
        }
        if (request.getExpireAt() != null) {
            tenant.setExpireAt(request.getExpireAt());
        }
        if (request.getMaxMembers() != null) {
            tenant.setMaxMembers(request.getMaxMembers());
        }
        if (request.getMaxAgents() != null) {
            tenant.setMaxAgents(request.getMaxAgents());
        }
        if (request.getMaxKnowledge() != null) {
            tenant.setMaxKnowledge(request.getMaxKnowledge());
        }
        if (request.getMaxStorageMb() != null) {
            tenant.setMaxStorageMb(request.getMaxStorageMb());
        }
        if (request.getMonthlyTokenQuota() != null) {
            tenant.setMonthlyTokenQuota(request.getMonthlyTokenQuota());
        }
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantMapper.update(tenant);

        auditLogService.record(
                "platform.tenant.update",
                "tenant",
                tenant.getId(),
                "更新租户: " + tenant.getTenantName(),
                TenantContext.getTenantId(),
                StpUtil.getLoginIdAsLong());

        return toPlatformTenantVO(tenant);
    }

    @Transactional
    public void deleteTenant(Long tenantId) {
        requireSuperAdmin();
        TenantEntity tenant = getTenantOrThrow(tenantId);
        tenant.setIsDeleted(1);
        tenant.setStatus(0);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantMapper.update(tenant);
        kickTenantSessions(tenant.getId());

        auditLogService.record(
                "platform.tenant.delete",
                "tenant",
                tenant.getId(),
                "删除租户: " + tenant.getTenantName(),
                TenantContext.getTenantId(),
                StpUtil.getLoginIdAsLong());
    }

    public PlatformGlobalStatsVO globalStats() {
        requireSuperAdmin();
        long tenantCount = tenantMapper.selectCountByQuery(QueryWrapper.create().eq("is_deleted", 0));
        long activeTenantCount = tenantMapper.selectCountByQuery(
                QueryWrapper.create().eq("is_deleted", 0).eq("status", 1));
        long totalMembers = tenantMemberMapper.selectCountByQuery(QueryWrapper.create().eq("is_deleted", 0));

        YearMonth current = YearMonth.now();
        LocalDate monthStart = current.atDay(1);
        LocalDate monthEnd = current.atEndOfMonth();
        Long tokensUsed = tokenUsageMapper.sumTokensBetweenAllTenants(monthStart, monthEnd);

        return PlatformGlobalStatsVO.builder()
                .tenantCount(tenantCount)
                .activeTenantCount(activeTenantCount)
                .totalMembers(totalMembers)
                .totalAgents(safeLong(platformStatsMapper.countAgents()))
                .totalKnowledgeBases(safeLong(platformStatsMapper.countKnowledgeBases()))
                .totalWorkflows(safeLong(platformStatsMapper.countWorkflows()))
                .tokensUsedThisMonth(tokensUsed != null ? tokensUsed : 0L)
                .build();
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private PlatformTenantVO toPlatformTenantVO(TenantEntity tenant) {
        int memberCount = (int) tenantMemberMapper.selectCountByQuery(
                QueryWrapper.create().eq("tenant_id", tenant.getId()).eq("is_deleted", 0));
        YearMonth current = YearMonth.now();
        long usedTokens = safeLong(tokenUsageMapper.sumTokensBetween(
                tenant.getId(), current.atDay(1), current.atEndOfMonth()));
        return PlatformTenantVO.builder()
                .id(tenant.getId())
                .tenantCode(tenant.getTenantCode())
                .tenantName(tenant.getTenantName())
                .contactName(tenant.getContactName())
                .contactEmail(tenant.getContactEmail())
                .contactPhone(tenant.getContactPhone())
                .planType(tenant.getPlanType())
                .planTypeLabel(resolvePlanTypeLabel(tenant.getPlanType()))
                .status(tenant.getStatus())
                .expireAt(tenant.getExpireAt())
                .maxMembers(tenant.getMaxMembers())
                .maxAgents(tenant.getMaxAgents())
                .maxKnowledge(tenant.getMaxKnowledge())
                .maxStorageMb(tenant.getMaxStorageMb())
                .monthlyTokenQuota(tenant.getMonthlyTokenQuota())
                .memberCount(memberCount)
                .usedTokensThisMonth(usedTokens)
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }

    private TenantEntity getTenantOrThrow(Long tenantId) {
        TenantEntity tenant = tenantMapper.selectOneByQuery(
                QueryWrapper.create().eq("id", tenantId).eq("is_deleted", 0));
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        return tenant;
    }

    private void requireSuperAdmin() {
        permissionService.requireSuperAdmin(StpUtil.getLoginIdAsLong(), TenantContext.getTenantId());
    }

    private void kickTenantSessions(Long tenantId) {
        List<TenantMemberEntity> members = tenantMemberMapper.selectListByQuery(
                QueryWrapper.create().eq("tenant_id", tenantId).eq("is_deleted", 0));
        for (TenantMemberEntity member : members) {
            if (member.getUserId() != null) {
                StpUtil.logout(member.getUserId());
            }
        }
    }

    private String normalizePlanType(String planType) {
        return planType != null ? planType.trim().toLowerCase(Locale.ROOT) : "starter";
    }

    private String resolvePlanTypeLabel(String planType) {
        if (!StringUtils.hasText(planType)) {
            return "未设置";
        }
        return switch (planType.trim().toLowerCase(Locale.ROOT)) {
            case "personal" -> "个人版";
            case "free" -> "免费版";
            case "starter" -> "入门版";
            case "pro" -> "专业版";
            case "enterprise" -> "企业版";
            default -> planType;
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
