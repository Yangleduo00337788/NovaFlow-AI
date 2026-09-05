package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.util.PageQueryUtils;
import ai.novaflow.model.domain.BillingCurrency;
import ai.novaflow.model.domain.ModelUsageAggregate;
import ai.novaflow.model.domain.ProviderCodeAggregate;
import ai.novaflow.model.domain.TenantTrafficSpikeAggregate;
import ai.novaflow.model.domain.TenantUsageAggregate;
import ai.novaflow.model.domain.UsageTrendPoint;
import ai.novaflow.model.entity.ModelConfigEntity;
import ai.novaflow.model.entity.ModelProviderEntity;
import ai.novaflow.model.mapper.ModelConfigMapper;
import ai.novaflow.model.mapper.ModelProviderMapper;
import ai.novaflow.model.mapper.TokenUsageMapper;
import ai.novaflow.common.util.CryptoService;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.common.security.AccountTypes;
import ai.novaflow.common.security.RoleCodes;
import ai.novaflow.user.domain.dto.PlatformModelCatalogSaveRequest;
import ai.novaflow.user.domain.dto.PlatformModelProviderUpdateRequest;
import ai.novaflow.user.domain.dto.PlatformSettingsUpdateRequest;
import ai.novaflow.user.domain.dto.PlatformOwnerPasswordResetRequest;
import ai.novaflow.user.domain.dto.PlatformTenantCreateRequest;
import ai.novaflow.user.domain.dto.PlatformTenantUpdateRequest;
import ai.novaflow.user.domain.dto.PlatformUserUpdateRequest;
import ai.novaflow.user.domain.vo.AuditLogVO;
import ai.novaflow.user.domain.vo.PlatformApiAlertEventVO;
import ai.novaflow.user.domain.vo.PlatformApiAlertVO;
import ai.novaflow.user.domain.vo.PlatformApiMonitorVO;
import ai.novaflow.user.domain.vo.PlatformBillingOverviewVO;
import ai.novaflow.user.domain.vo.PlatformGlobalStatsVO;
import ai.novaflow.user.domain.vo.PlatformModelCatalogVO;
import ai.novaflow.user.domain.vo.PlatformModelProviderVO;
import ai.novaflow.user.domain.vo.PlatformModelOverviewVO;
import ai.novaflow.user.domain.vo.PlatformModelUsageVO;
import ai.novaflow.user.domain.vo.PlatformProviderStatVO;
import ai.novaflow.user.domain.vo.PlatformSettingsVO;
import ai.novaflow.user.domain.vo.PlatformTenantTrafficSpikeVO;
import ai.novaflow.user.domain.vo.PlatformTenantUsageVO;
import ai.novaflow.user.domain.vo.PlatformDashboardOverviewVO;
import ai.novaflow.user.domain.vo.PlatformTenantDetailVO;
import ai.novaflow.user.domain.vo.PlatformOnboardingTemplateVO;
import ai.novaflow.user.domain.vo.PlatformOwnerPasswordResetResultVO;
import ai.novaflow.user.domain.vo.PlatformTenantCreateResultVO;
import ai.novaflow.user.domain.vo.PlatformTenantHealthVO;
import ai.novaflow.user.domain.vo.PlatformTenantVO;
import ai.novaflow.user.domain.vo.PlatformTrendPointVO;
import ai.novaflow.user.domain.vo.PlatformUserMembershipVO;
import ai.novaflow.user.domain.vo.PlatformUserVO;
import ai.novaflow.user.entity.PlatformApiAlertEventEntity;
import ai.novaflow.user.entity.PlatformModelCatalogEntity;
import ai.novaflow.user.entity.AuditLogEntity;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.user.mapper.AuditLogMapper;
import ai.novaflow.user.mapper.PlatformApiAlertEventMapper;
import ai.novaflow.user.mapper.PlatformModelCatalogMapper;
import ai.novaflow.user.mapper.PlatformStatsMapper;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.user.mapper.UserMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import ai.novaflow.tenant.support.TenantLimits;
import ai.novaflow.user.support.OnboardingPasswordGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformAdminService {

    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final PermissionService permissionService;
    private final TokenUsageMapper tokenUsageMapper;
    private final PlatformStatsMapper platformStatsMapper;
    private final AuditLogService auditLogService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final AuditLogMapper auditLogMapper;
    private final TenantOnboardingService tenantOnboardingService;
    private final PlatformSystemConfigService platformSystemConfigService;
    private final ModelProviderMapper modelProviderMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final CryptoService cryptoService;
    private final PlatformApiAlertEventMapper platformApiAlertEventMapper;
    private final PlatformModelCatalogMapper platformModelCatalogMapper;
    private final OnboardingMailService onboardingMailService;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> ONBOARDING_PLAN_TYPES = List.of(
            "personal", "free", "starter", "pro", "enterprise");

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

    public PlatformTenantDetailVO getTenantDetail(Long tenantId) {
        requireSuperAdmin();
        TenantEntity tenant = getTenantOrThrow(tenantId);
        PlatformTenantVO base = toPlatformTenantVO(tenant);

        YearMonth current = YearMonth.now();
        LocalDate monthStart = current.atDay(1);
        LocalDate monthEnd = current.atEndOfMonth();

        int agentCount = (int) safeLong(platformStatsMapper.countAgentsByTenant(tenantId));
        int knowledgeCount = (int) safeLong(platformStatsMapper.countKnowledgeBasesByTenant(tenantId));
        int applicationCount = (int) safeLong(platformStatsMapper.countApplicationsByTenant(tenantId));
        int workflowCount = (int) safeLong(platformStatsMapper.countWorkflowsByTenant(tenantId));

        int memberCount = base.getMemberCount() != null ? base.getMemberCount() : 0;
        int maxMembers = tenant.getMaxMembers() != null && tenant.getMaxMembers() > 0 ? tenant.getMaxMembers() : 100;
        long monthlyTokenQuota = tenant.getMonthlyTokenQuota() != null ? tenant.getMonthlyTokenQuota() : 0L;
        long usedTokens = base.getUsedTokensThisMonth() != null ? base.getUsedTokensThisMonth() : 0L;

        long callsThisMonth = safeLong(tokenUsageMapper.countCallsBetween(tenantId, monthStart, monthEnd));
        BigDecimal costCny = tokenUsageMapper.sumCostBetween(tenantId, BillingCurrency.CNY.getCode(), monthStart, monthEnd);
        if (costCny == null) {
            costCny = BigDecimal.ZERO;
        }

        List<PlatformTrendPointVO> dailyTrend = tokenUsageMapper.dailyTokenTrend(tenantId, monthStart, monthEnd)
                .stream()
                .map(point -> PlatformTrendPointVO.builder()
                        .label(point.getLabel())
                        .tokens(safeLong(point.getValue()))
                        .build())
                .toList();

        List<PlatformModelUsageVO> topModels = tokenUsageMapper.topModelsBetween(tenantId, monthStart, monthEnd, 5)
                .stream()
                .map(this::toPlatformModelUsageVO)
                .toList();

        boolean expired = tenant.getExpireAt() != null && tenant.getExpireAt().isBefore(LocalDateTime.now());
        Integer daysUntilExpiry = resolveDaysUntilExpiry(tenant.getExpireAt());

        return PlatformTenantDetailVO.builder()
                .tenant(base)
                .agentCount(agentCount)
                .knowledgeCount(knowledgeCount)
                .applicationCount(applicationCount)
                .workflowCount(workflowCount)
                .memberUsedPercent(calcPercent(memberCount, maxMembers))
                .tokenUsedPercent(calcPercent(usedTokens, monthlyTokenQuota))
                .callsThisMonth(callsThisMonth)
                .costCnyThisMonth(costCny)
                .expired(expired)
                .daysUntilExpiry(daysUntilExpiry)
                .dailyTokenTrend(dailyTrend)
                .topModelsThisMonth(topModels)
                .build();
    }

    @Transactional
    public PlatformTenantCreateResultVO createTenant(PlatformTenantCreateRequest request) {
        requireSuperAdmin();
        boolean generatePassword = Boolean.TRUE.equals(request.getGeneratePassword());
        String ownerPassword = request.getOwnerPassword();
        if (generatePassword) {
            ownerPassword = OnboardingPasswordGenerator.generate();
        } else if (!StringUtils.hasText(ownerPassword)) {
            throw new BusinessException("请填写初始密码或选择自动生成");
        }

        TenantOnboardingService.ProvisionResult result = tenantOnboardingService.provisionTenantWithOwner(
                request.getTenantName(),
                request.getPlanType(),
                request.getOwnerEmail(),
                ownerPassword,
                request.getOwnerNickname(),
                request.getContactName(),
                request.getContactEmail(),
                request.getContactPhone());

        boolean inviteEmailSent = false;
        if (Boolean.TRUE.equals(request.getSendInviteEmail())) {
            inviteEmailSent = onboardingMailService.sendOwnerInvite(
                    result.owner().getEmail(),
                    result.tenant().getTenantName(),
                    ownerPassword,
                    generatePassword);
        }

        auditLogService.record(
                "platform.tenant.create",
                "tenant",
                result.tenant().getId(),
                "代开户: " + result.tenant().getTenantName() + " (" + result.owner().getEmail() + ")",
                TenantContext.getTenantId(),
                StpUtil.getLoginIdAsLong());

        return PlatformTenantCreateResultVO.builder()
                .tenant(toPlatformTenantVO(result.tenant()))
                .ownerId(result.owner().getId())
                .ownerEmail(result.owner().getEmail())
                .generatedPassword(generatePassword ? ownerPassword : null)
                .inviteEmailSent(inviteEmailSent)
                .build();
    }

    public List<PlatformOnboardingTemplateVO> listOnboardingTemplates() {
        requireSuperAdmin();
        List<PlatformOnboardingTemplateVO> templates = new ArrayList<>();
        for (String planType : ONBOARDING_PLAN_TYPES) {
            TenantEntity sample = new TenantEntity();
            sample.setPlanType(planType);
            TenantLimits.applyPlanDefaults(sample);
            templates.add(PlatformOnboardingTemplateVO.builder()
                    .planType(planType)
                    .planTypeLabel(resolvePlanTypeLabel(planType))
                    .maxMembers(sample.getMaxMembers())
                    .maxAgents(sample.getMaxAgents())
                    .maxKnowledge(sample.getMaxKnowledge())
                    .maxStorageMb(sample.getMaxStorageMb())
                    .monthlyTokenQuota(sample.getMonthlyTokenQuota())
                    .build());
        }
        return templates;
    }

    @Transactional
    public PlatformOwnerPasswordResetResultVO resetTenantOwnerPassword(
            Long tenantId,
            PlatformOwnerPasswordResetRequest request) {
        requireSuperAdmin();
        getTenantOrThrow(tenantId);
        UserEntity owner = findTenantOwnerOrThrow(tenantId);

        boolean generatePassword = Boolean.TRUE.equals(request.getGeneratePassword());
        String newPassword = request.getNewPassword();
        if (generatePassword) {
            newPassword = OnboardingPasswordGenerator.generate();
        } else if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException("请填写新密码或选择自动生成");
        }
        tenantOnboardingService.validatePasswordStrength(newPassword);

        owner.setPasswordHash(passwordEncoder.encode(newPassword));
        owner.setUpdatedAt(LocalDateTime.now());
        userMapper.update(owner);
        StpUtil.logout(owner.getId());

        TenantEntity tenant = tenantMapper.selectOneById(tenantId);
        boolean inviteEmailSent = false;
        if (Boolean.TRUE.equals(request.getSendInviteEmail())) {
            inviteEmailSent = onboardingMailService.sendOwnerInvite(
                    owner.getEmail(),
                    tenant != null ? tenant.getTenantName() : "企业",
                    newPassword,
                    generatePassword);
        }

        auditLogService.record(
                "platform.tenant.owner_reset_password",
                "tenant",
                tenantId,
                "重置 Owner 密码: " + owner.getEmail(),
                TenantContext.getTenantId(),
                StpUtil.getLoginIdAsLong());

        return PlatformOwnerPasswordResetResultVO.builder()
                .ownerId(owner.getId())
                .ownerEmail(owner.getEmail())
                .generatedPassword(generatePassword ? newPassword : null)
                .inviteEmailSent(inviteEmailSent)
                .build();
    }

    private UserEntity findTenantOwnerOrThrow(Long tenantId) {
        RoleEntity ownerRole = roleMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", 0)
                        .eq("role_code", RoleCodes.TENANT_OWNER)
                        .eq("is_deleted", 0));
        if (ownerRole == null) {
            throw new BusinessException("系统角色未初始化");
        }
        TenantMemberEntity member = tenantMemberMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("role_id", ownerRole.getId())
                        .eq("is_deleted", 0)
                        .limit(1));
        if (member == null) {
            throw new BusinessException("未找到企业 Owner");
        }
        UserEntity owner = userMapper.selectOneByQuery(
                QueryWrapper.create().eq("id", member.getUserId()).eq("is_deleted", 0));
        if (owner == null) {
            throw new BusinessException("Owner 用户不存在");
        }
        return owner;
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
        long totalUsers = userMapper.selectCountByQuery(QueryWrapper.create().eq("is_deleted", 0));

        YearMonth current = YearMonth.now();
        LocalDate monthStart = current.atDay(1);
        LocalDate monthEnd = current.atEndOfMonth();
        Long tokensUsed = tokenUsageMapper.sumTokensBetweenAllTenants(monthStart, monthEnd);

        return PlatformGlobalStatsVO.builder()
                .tenantCount(tenantCount)
                .activeTenantCount(activeTenantCount)
                .totalMembers(totalMembers)
                .totalUsers(totalUsers)
                .totalAgents(safeLong(platformStatsMapper.countAgents()))
                .totalKnowledgeBases(safeLong(platformStatsMapper.countKnowledgeBases()))
                .totalWorkflows(safeLong(platformStatsMapper.countWorkflows()))
                .tokensUsedThisMonth(tokensUsed != null ? tokensUsed : 0L)
                .build();
    }

    public PlatformDashboardOverviewVO dashboardOverview() {
        requireSuperAdmin();
        PlatformGlobalStatsVO stats = globalStats();

        int trendDays = 14;
        LocalDate trendEnd = LocalDate.now();
        LocalDate trendStart = trendEnd.minusDays(trendDays - 1L);
        LocalDateTime growthSince = trendStart.atStartOfDay();

        List<PlatformTrendPointVO> tenantGrowthTrend = fillDailyTrend(
                trendStart,
                trendEnd,
                platformStatsMapper.tenantDailyGrowth(growthSince).stream()
                        .collect(Collectors.toMap(UsageTrendPoint::getLabel, point -> safeLong(point.getValue()), (a, b) -> b)));

        List<PlatformTrendPointVO> tokenUsageTrend = tokenUsageMapper
                .dailyTokenTrendAllTenants(trendStart, trendEnd)
                .stream()
                .map(point -> PlatformTrendPointVO.builder()
                        .label(point.getLabel())
                        .tokens(safeLong(point.getValue()))
                        .build())
                .toList();

        List<PlatformTenantHealthVO> tenantHealth = buildTenantHealthList();

        return PlatformDashboardOverviewVO.builder()
                .stats(stats)
                .tenantGrowthTrend(tenantGrowthTrend)
                .tokenUsageTrend(tokenUsageTrend)
                .tenantHealth(tenantHealth)
                .build();
    }

    private List<PlatformTenantHealthVO> buildTenantHealthList() {
        List<TenantEntity> tenants = tenantMapper.selectListByQuery(
                QueryWrapper.create().eq("is_deleted", 0).orderBy("updated_at", false).limit(200));
        YearMonth current = YearMonth.now();
        LocalDate monthStart = current.atDay(1);
        LocalDate monthEnd = current.atEndOfMonth();

        List<PlatformTenantHealthVO> healthList = new ArrayList<>();
        for (TenantEntity tenant : tenants) {
            int memberCount = (int) tenantMemberMapper.selectCountByQuery(
                    QueryWrapper.create().eq("tenant_id", tenant.getId()).eq("is_deleted", 0));
            int maxMembers = tenant.getMaxMembers() != null && tenant.getMaxMembers() > 0 ? tenant.getMaxMembers() : 100;
            long monthlyTokenQuota = tenant.getMonthlyTokenQuota() != null ? tenant.getMonthlyTokenQuota() : 0L;
            long usedTokens = safeLong(tokenUsageMapper.sumTokensBetween(tenant.getId(), monthStart, monthEnd));
            Integer tokenUsedPercent = calcPercent(usedTokens, monthlyTokenQuota);
            Integer memberUsedPercent = calcPercent(memberCount, maxMembers);
            Integer daysUntilExpiry = resolveDaysUntilExpiry(tenant.getExpireAt());

            List<String> reasons = new ArrayList<>();
            String healthStatus = "HEALTHY";
            if (tenant.getStatus() != null && tenant.getStatus() != 1) {
                healthStatus = "CRITICAL";
                reasons.add("租户已停用");
            }
            if (tenant.getExpireAt() != null && tenant.getExpireAt().isBefore(LocalDateTime.now())) {
                healthStatus = "CRITICAL";
                reasons.add("套餐已到期");
            }
            if (tokenUsedPercent != null && tokenUsedPercent >= 100) {
                healthStatus = "CRITICAL";
                reasons.add("Token 配额已用尽");
            } else if (tokenUsedPercent != null && tokenUsedPercent >= 80) {
                healthStatus = escalateHealth(healthStatus, "WARNING");
                reasons.add("Token 使用率超过 80%");
            }
            if (memberUsedPercent != null && memberUsedPercent >= 100) {
                healthStatus = "CRITICAL";
                reasons.add("成员席位已满");
            } else if (memberUsedPercent != null && memberUsedPercent >= 80) {
                healthStatus = escalateHealth(healthStatus, "WARNING");
                reasons.add("成员席位使用率超过 80%");
            }
            if (daysUntilExpiry != null && daysUntilExpiry <= 0) {
                healthStatus = "CRITICAL";
                if (!reasons.contains("套餐已到期")) {
                    reasons.add("套餐已到期");
                }
            } else if (daysUntilExpiry != null && daysUntilExpiry <= 30) {
                healthStatus = escalateHealth(healthStatus, "WARNING");
                reasons.add("套餐将在 " + daysUntilExpiry + " 天内到期");
            }

            if ("HEALTHY".equals(healthStatus)) {
                continue;
            }

            healthList.add(PlatformTenantHealthVO.builder()
                    .tenantId(tenant.getId())
                    .tenantName(tenant.getTenantName())
                    .healthStatus(healthStatus)
                    .reasons(reasons)
                    .tokenUsedPercent(tokenUsedPercent)
                    .memberUsedPercent(memberUsedPercent)
                    .daysUntilExpiry(daysUntilExpiry)
                    .status(tenant.getStatus())
                    .build());
        }

        healthList.sort(Comparator
                .comparing((PlatformTenantHealthVO item) -> healthRank(item.getHealthStatus()))
                .thenComparing(item -> item.getTokenUsedPercent() != null ? -item.getTokenUsedPercent() : 0));
        return healthList.stream().limit(10).toList();
    }

    private int healthRank(String status) {
        return switch (status) {
            case "CRITICAL" -> 0;
            case "WARNING" -> 1;
            default -> 2;
        };
    }

    private String escalateHealth(String current, String next) {
        if ("CRITICAL".equals(current)) {
            return current;
        }
        return next;
    }

    private List<PlatformTrendPointVO> fillDailyTrend(
            LocalDate start,
            LocalDate end,
            Map<String, Long> valueByLabel) {
        List<PlatformTrendPointVO> points = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String label = date.format(formatter);
            points.add(PlatformTrendPointVO.builder()
                    .label(label)
                    .tokens(valueByLabel.getOrDefault(label, 0L))
                    .build());
        }
        return points;
    }

    public PlatformBillingOverviewVO billingOverview(String month) {
        requireSuperAdmin();
        YearMonth current = resolveMonth(month);
        YearMonth previous = current.minusMonths(1);
        LocalDate currentStart = current.atDay(1);
        LocalDate currentEnd = current.atEndOfMonth();
        LocalDate previousStart = previous.atDay(1);
        LocalDate previousEnd = previous.atEndOfMonth();

        long totalTokens = safeLong(tokenUsageMapper.sumTokensBetweenAllTenants(currentStart, currentEnd));
        long prevMonthTokens = safeLong(tokenUsageMapper.sumTokensBetweenAllTenants(previousStart, previousEnd));
        long totalCalls = safeLong(tokenUsageMapper.countCallsBetweenAllTenants(currentStart, currentEnd));
        BigDecimal costCny = safeDecimal(tokenUsageMapper.sumCostBetweenAllTenants(
                BillingCurrency.CNY.getCode(), currentStart, currentEnd));
        BigDecimal costUsd = safeDecimal(tokenUsageMapper.sumCostBetweenAllTenants(
                BillingCurrency.USD.getCode(), currentStart, currentEnd));

        List<PlatformTrendPointVO> dailyTrend = tokenUsageMapper.dailyTokenTrendAllTenants(currentStart, currentEnd)
                .stream()
                .map(point -> PlatformTrendPointVO.builder()
                        .label(point.getLabel())
                        .tokens(safeLong(point.getValue()))
                        .build())
                .toList();

        List<PlatformTenantUsageVO> topTenants = tokenUsageMapper.topTenantsBetween(currentStart, currentEnd, 10)
                .stream()
                .map(this::toPlatformTenantUsageVO)
                .toList();

        List<PlatformModelUsageVO> topModels = tokenUsageMapper.topModelsAllTenants(currentStart, currentEnd, 10)
                .stream()
                .map(this::toPlatformModelUsageVO)
                .toList();

        return PlatformBillingOverviewVO.builder()
                .month(current.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                .totalTokens(totalTokens)
                .prevMonthTokens(prevMonthTokens)
                .totalCalls(totalCalls)
                .costCny(costCny)
                .costUsd(costUsd)
                .dailyTrend(dailyTrend)
                .topTenants(topTenants)
                .topModels(topModels)
                .build();
    }

    public byte[] exportBillingCsv(String month) {
        PlatformBillingOverviewVO overview = billingOverview(month);
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("section,field,value\n");
        csv.append("summary,month,").append(escapeCsv(overview.getMonth())).append('\n');
        csv.append("summary,totalTokens,").append(overview.getTotalTokens()).append('\n');
        csv.append("summary,prevMonthTokens,").append(overview.getPrevMonthTokens()).append('\n');
        csv.append("summary,totalCalls,").append(overview.getTotalCalls()).append('\n');
        csv.append("summary,costCny,").append(overview.getCostCny()).append('\n');
        csv.append("summary,costUsd,").append(overview.getCostUsd()).append('\n');
        csv.append('\n');
        csv.append("tenantId,tenantName,calls,tokens\n");
        for (PlatformTenantUsageVO tenant : overview.getTopTenants()) {
            csv.append(tenant.getTenantId()).append(',')
                    .append(escapeCsv(tenant.getTenantName())).append(',')
                    .append(tenant.getCalls()).append(',')
                    .append(tenant.getTokens()).append('\n');
        }
        csv.append('\n');
        csv.append("modelName,displayName,calls,tokens\n");
        for (PlatformModelUsageVO model : overview.getTopModels()) {
            csv.append(escapeCsv(model.getModelName())).append(',')
                    .append(escapeCsv(model.getDisplayName())).append(',')
                    .append(model.getCalls()).append(',')
                    .append(model.getTokens()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    public PlatformModelOverviewVO modelOverview() {
        requireSuperAdmin();
        List<PlatformProviderStatVO> providersByCode = platformStatsMapper.groupProvidersByCode()
                .stream()
                .map(this::toPlatformProviderStatVO)
                .toList();

        return PlatformModelOverviewVO.builder()
                .totalProviders(safeLong(platformStatsMapper.countModelProviders()))
                .enabledProviders(safeLong(platformStatsMapper.countEnabledModelProviders()))
                .totalModelConfigs(safeLong(platformStatsMapper.countModelConfigs()))
                .enabledModelConfigs(safeLong(platformStatsMapper.countEnabledModelConfigs()))
                .providersByCode(providersByCode)
                .build();
    }

    public PlatformApiMonitorVO apiMonitorOverview() {
        requireSuperAdmin();
        LocalDate today = LocalDate.now();
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long hourlyThreshold = platformSystemConfigService.getHourlyCallsThreshold();
        double spikeMultiplier = platformSystemConfigService.getTrafficSpikeMultiplier();

        long totalCallsToday = safeLong(tokenUsageMapper.countCallsBetweenAllTenants(today, today));
        long totalCallsLastHour = safeLong(tokenUsageMapper.countCallsSince(oneHourAgo));

        List<TenantUsageAggregate> topHourly = tokenUsageMapper.topTenantsByCallsSince(oneHourAgo, 10);
        List<PlatformTenantUsageVO> topTenantsLastHour = topHourly.stream()
                .map(this::toPlatformTenantUsageVO)
                .toList();

        List<PlatformApiAlertVO> alerts = new ArrayList<>();
        for (TenantUsageAggregate tenant : topHourly) {
            long calls = safeLong(tenant.getCalls());
            if (calls >= hourlyThreshold) {
                alerts.add(PlatformApiAlertVO.builder()
                        .type("HIGH_FREQUENCY")
                        .severity(calls >= hourlyThreshold * 2 ? "critical" : "warning")
                        .tenantId(tenant.getTenantId())
                        .tenantName(tenant.getTenantName())
                        .message("近 1 小时 API 调用 " + calls + " 次，超过阈值 " + hourlyThreshold)
                        .metricValue(calls)
                        .threshold(hourlyThreshold)
                        .build());
            }
        }

        List<PlatformTenantTrafficSpikeVO> trafficSpikes = new ArrayList<>();
        List<TenantTrafficSpikeAggregate> baselines = tokenUsageMapper.listTenantTrafficBaselines(today, 7, 20);
        for (TenantTrafficSpikeAggregate baseline : baselines) {
            long todayCalls = safeLong(baseline.getTodayCalls());
            long avgDaily = Math.max(1L, safeLong(baseline.getAvgDailyCalls()));
            double ratio = todayCalls / (double) avgDaily;
            if (todayCalls >= avgDaily * spikeMultiplier && todayCalls >= hourlyThreshold) {
                trafficSpikes.add(PlatformTenantTrafficSpikeVO.builder()
                        .tenantId(baseline.getTenantId())
                        .tenantName(baseline.getTenantName())
                        .todayCalls(todayCalls)
                        .avgDailyCalls(avgDaily)
                        .spikeRatio(ratio)
                        .build());
                alerts.add(PlatformApiAlertVO.builder()
                        .type("TRAFFIC_SPIKE")
                        .severity(ratio >= spikeMultiplier * 2 ? "critical" : "warning")
                        .tenantId(baseline.getTenantId())
                        .tenantName(baseline.getTenantName())
                        .message(String.format(
                                Locale.ROOT,
                                "今日调用 %d 次，为近 7 日均值 %d 的 %.1f 倍",
                                todayCalls,
                                avgDaily,
                                ratio))
                        .metricValue(todayCalls)
                        .threshold((long) Math.ceil(avgDaily * spikeMultiplier))
                        .build());
            }
        }
        trafficSpikes.sort(Comparator.comparing(PlatformTenantTrafficSpikeVO::getSpikeRatio).reversed());
        persistApiAlerts(alerts, today);

        return PlatformApiMonitorVO.builder()
                .totalCallsToday(totalCallsToday)
                .totalCallsLastHour(totalCallsLastHour)
                .hourlyCallsThreshold(hourlyThreshold)
                .trafficSpikeMultiplier(spikeMultiplier)
                .alerts(alerts)
                .topTenantsLastHour(topTenantsLastHour)
                .trafficSpikes(trafficSpikes)
                .build();
    }

    public PageResult<PlatformApiAlertEventVO> pageApiAlertEvents(int page, int pageSize, String status) {
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);
        requireSuperAdmin();
        QueryWrapper query = QueryWrapper.create().orderBy("created_at", false);
        if (StringUtils.hasText(status)) {
            query.eq("status", status.trim().toUpperCase(Locale.ROOT));
        }
        Page<PlatformApiAlertEventEntity> result = platformApiAlertEventMapper.paginate(Page.of(page, pageSize), query);
        List<PlatformApiAlertEventVO> list = result.getRecords().stream().map(this::toApiAlertEventVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    @Transactional
    public PlatformApiAlertEventVO acknowledgeApiAlert(Long alertId) {
        requireSuperAdmin();
        PlatformApiAlertEventEntity entity = platformApiAlertEventMapper.selectOneById(alertId);
        if (entity == null) {
            throw new BusinessException("告警不存在");
        }
        entity.setStatus("ACKED");
        entity.setAckedBy(StpUtil.getLoginIdAsLong());
        entity.setAckedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        platformApiAlertEventMapper.update(entity);
        auditLogService.record(
                "platform.api_alert.ack",
                "api_alert",
                alertId,
                "确认告警: " + entity.getMessage(),
                TenantContext.getTenantId(),
                StpUtil.getLoginIdAsLong());
        return toApiAlertEventVO(entity);
    }

    public PageResult<PlatformModelCatalogVO> pageModelCatalog(int page, int pageSize, String keyword) {
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);
        requireSuperAdmin();
        QueryWrapper query = QueryWrapper.create().eq("is_deleted", 0).orderBy("provider_code", true).orderBy("model_name", true);
        if (StringUtils.hasText(keyword)) {
            query.and("(provider_code LIKE ? OR model_name LIKE ? OR display_name LIKE ?)",
                    "%" + keyword.trim() + "%",
                    "%" + keyword.trim() + "%",
                    "%" + keyword.trim() + "%");
        }
        Page<PlatformModelCatalogEntity> result = platformModelCatalogMapper.paginate(Page.of(page, pageSize), query);
        List<PlatformModelCatalogVO> list = result.getRecords().stream().map(this::toModelCatalogVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    @Transactional
    public PlatformModelCatalogVO saveModelCatalog(Long id, PlatformModelCatalogSaveRequest request) {
        requireSuperAdmin();
        PlatformModelCatalogEntity entity;
        if (id != null) {
            entity = platformModelCatalogMapper.selectOneByQuery(
                    QueryWrapper.create().eq("id", id).eq("is_deleted", 0));
            if (entity == null) {
                throw new BusinessException("模型目录项不存在");
            }
        } else {
            entity = new PlatformModelCatalogEntity();
            entity.setCreatedAt(LocalDateTime.now());
            entity.setIsDeleted(0);
        }
        entity.setProviderCode(request.getProviderCode().trim().toLowerCase(Locale.ROOT));
        entity.setModelName(request.getModelName().trim());
        entity.setDisplayName(trimToNull(request.getDisplayName()));
        entity.setModelType(StringUtils.hasText(request.getModelType()) ? request.getModelType().trim() : "chat");
        entity.setInputPricePer1k(request.getInputPricePer1k());
        entity.setOutputPricePer1k(request.getOutputPricePer1k());
        entity.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency().trim().toUpperCase(Locale.ROOT) : "CNY");
        entity.setEnabled(request.getEnabled() != null && request.getEnabled() == 1 ? 1 : 0);
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setUpdatedAt(LocalDateTime.now());
        if (id == null) {
            platformModelCatalogMapper.insert(entity);
        } else {
            platformModelCatalogMapper.update(entity);
        }
        return toModelCatalogVO(entity);
    }

    @Transactional
    public void deleteModelCatalog(Long id) {
        requireSuperAdmin();
        PlatformModelCatalogEntity entity = platformModelCatalogMapper.selectOneByQuery(
                QueryWrapper.create().eq("id", id).eq("is_deleted", 0));
        if (entity == null) {
            throw new BusinessException("模型目录项不存在");
        }
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        platformModelCatalogMapper.update(entity);
    }

    public PageResult<PlatformModelProviderVO> pageModelProviders(
            int page, int pageSize, String keyword, Long tenantId, String providerCode, Integer enabled) {
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);
        requireSuperAdmin();

        QueryWrapper query = QueryWrapper.create().eq("is_deleted", 0);
        if (tenantId != null) {
            query.eq("tenant_id", tenantId);
        }
        if (StringUtils.hasText(providerCode)) {
            query.eq("provider_code", providerCode.trim().toLowerCase(Locale.ROOT));
        }
        if (enabled != null) {
            query.eq("is_enabled", enabled);
        }
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            query.and("(provider_name LIKE ? OR provider_code LIKE ? OR base_url LIKE ?)", like, like, like);
        }
        query.orderBy("updated_at", false);

        Page<ModelProviderEntity> result = modelProviderMapper.paginate(Page.of(page, pageSize), query);
        List<PlatformModelProviderVO> list = toPlatformModelProviderVOs(result.getRecords());
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    @Transactional
    public PlatformModelProviderVO updateModelProvider(Long providerId, PlatformModelProviderUpdateRequest request) {
        requireSuperAdmin();
        ModelProviderEntity provider = getModelProviderOrThrow(providerId);
        int enabled = request.getEnabled() != null && request.getEnabled() == 1 ? 1 : 0;
        provider.setIsEnabled(enabled);
        provider.setUpdatedAt(LocalDateTime.now());
        modelProviderMapper.update(provider);

        TenantEntity tenant = tenantMapper.selectOneById(provider.getTenantId());
        String tenantLabel = tenant != null ? tenant.getTenantName() : String.valueOf(provider.getTenantId());
        auditLogService.record(
                "platform.model_provider.update",
                "model_provider",
                provider.getId(),
                (enabled == 1 ? "启用" : "停用") + "模型供应商: "
                        + provider.getProviderCode() + " @ " + tenantLabel,
                TenantContext.getTenantId(),
                StpUtil.getLoginIdAsLong());

        return toPlatformModelProviderVO(provider, loadModelCountMaps(List.of(provider.getId())));
    }

    public PageResult<PlatformUserVO> pageUsers(int page, int pageSize, String keyword, Integer status, String accountType) {
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);
        requireSuperAdmin();

        QueryWrapper query = QueryWrapper.create().eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            query.and("(email LIKE ? OR username LIKE ? OR nickname LIKE ?)", like, like, like);
        }
        if (status != null) {
            query.eq("status", status);
        }
        if (StringUtils.hasText(accountType)) {
            query.eq("account_type", accountType.trim().toLowerCase(Locale.ROOT));
        }
        query.orderBy("created_at", false);

        Page<UserEntity> result = userMapper.paginate(Page.of(page, pageSize), query);
        List<PlatformUserVO> list = result.getRecords().stream().map(this::toPlatformUserVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    public PlatformUserVO getUser(Long userId) {
        requireSuperAdmin();
        UserEntity user = getUserOrThrow(userId);
        return toPlatformUserVO(user);
    }

    @Transactional
    public PlatformUserVO updateUser(Long userId, PlatformUserUpdateRequest request) {
        requireSuperAdmin();
        long currentUserId = StpUtil.getLoginIdAsLong();
        if (userId == currentUserId && request.getStatus() != 1) {
            throw new BusinessException("不能封禁自己");
        }

        UserEntity user = getUserOrThrow(userId);
        if (AccountTypes.isPlatform(user.getAccountType()) && request.getStatus() != 1) {
            long activePlatformUsers = userMapper.selectCountByQuery(
                    QueryWrapper.create()
                            .eq("account_type", AccountTypes.PLATFORM)
                            .eq("status", 1)
                            .eq("is_deleted", 0));
            if (activePlatformUsers <= 1) {
                throw new BusinessException("不能封禁最后一个平台管理员账号");
            }
        }

        user.setStatus(request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);

        List<TenantMemberEntity> members = tenantMemberMapper.selectListByQuery(
                QueryWrapper.create().eq("user_id", userId).eq("is_deleted", 0));
        for (TenantMemberEntity member : members) {
            member.setStatus(request.getStatus());
            member.setUpdatedAt(LocalDateTime.now());
            tenantMemberMapper.update(member);
        }
        if (request.getStatus() != 1) {
            StpUtil.logout(userId);
        }

        String action = request.getStatus() == 1 ? "platform.user.unban" : "platform.user.ban";
        auditLogService.record(
                action,
                "user",
                userId,
                (request.getStatus() == 1 ? "解封用户: " : "封禁用户: ") + user.getEmail(),
                TenantContext.getTenantId(),
                currentUserId);

        return toPlatformUserVO(user);
    }

    public void forceLogoutUser(Long userId) {
        requireSuperAdmin();
        UserEntity user = getUserOrThrow(userId);
        StpUtil.logout(userId);
        auditLogService.record(
                "platform.user.logout",
                "user",
                userId,
                "强制下线: " + user.getEmail(),
                TenantContext.getTenantId(),
                StpUtil.getLoginIdAsLong());
    }

    @Transactional
    public void deleteUser(Long userId) {
        requireSuperAdmin();
        long currentUserId = StpUtil.getLoginIdAsLong();
        if (userId == currentUserId) {
            throw new BusinessException("不能注销自己");
        }

        UserEntity user = getUserOrThrow(userId);
        if (AccountTypes.isPlatform(user.getAccountType())) {
            long activePlatformUsers = userMapper.selectCountByQuery(
                    QueryWrapper.create()
                            .eq("account_type", AccountTypes.PLATFORM)
                            .eq("status", 1)
                            .eq("is_deleted", 0));
            if (activePlatformUsers <= 1) {
                throw new BusinessException("不能注销最后一个平台管理员账号");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        user.setIsDeleted(1);
        user.setStatus(0);
        user.setUpdatedAt(now);
        userMapper.update(user);

        List<TenantMemberEntity> members = tenantMemberMapper.selectListByQuery(
                QueryWrapper.create().eq("user_id", userId).eq("is_deleted", 0));
        for (TenantMemberEntity member : members) {
            member.setIsDeleted(1);
            member.setStatus(0);
            member.setUpdatedAt(now);
            tenantMemberMapper.update(member);
        }

        StpUtil.logout(userId);
        auditLogService.record(
                "platform.user.delete",
                "user",
                userId,
                "注销用户: " + user.getEmail(),
                TenantContext.getTenantId(),
                currentUserId);
    }

    public PlatformSettingsVO getSettings() {
        requireSuperAdmin();
        Set<String> allowed = platformSystemConfigService.getAllowedProviderCodes();
        return PlatformSettingsVO.builder()
                .registrationEnabled(platformSystemConfigService.isRegistrationEnabled())
                .hourlyCallsThreshold(platformSystemConfigService.getHourlyCallsThreshold())
                .trafficSpikeMultiplier(platformSystemConfigService.getTrafficSpikeMultiplier())
                .allowedProviderCodes(List.copyOf(allowed))
                .providerWhitelistEnabled(!allowed.isEmpty())
                .maintenanceEnabled(platformSystemConfigService.isMaintenanceEnabled())
                .maintenanceMessage(platformSystemConfigService.getMaintenanceMessage())
                .platformAnnouncement(platformSystemConfigService.getPlatformAnnouncement())
                .build();
    }

    public PlatformSettingsVO updateSettings(PlatformSettingsUpdateRequest request) {
        requireSuperAdmin();
        long operatorId = StpUtil.getLoginIdAsLong();
        StringBuilder detail = new StringBuilder("更新系统配置:");
        if (request.getRegistrationEnabled() != null) {
            platformSystemConfigService.setRegistrationEnabled(request.getRegistrationEnabled(), operatorId);
            detail.append(" registrationEnabled=").append(request.getRegistrationEnabled());
        }
        if (request.getHourlyCallsThreshold() != null) {
            platformSystemConfigService.setHourlyCallsThreshold(request.getHourlyCallsThreshold(), operatorId);
            detail.append(" hourlyCallsThreshold=").append(request.getHourlyCallsThreshold());
        }
        if (request.getTrafficSpikeMultiplier() != null) {
            platformSystemConfigService.setTrafficSpikeMultiplier(request.getTrafficSpikeMultiplier(), operatorId);
            detail.append(" trafficSpikeMultiplier=").append(request.getTrafficSpikeMultiplier());
        }
        if (request.getAllowedProviderCodes() != null) {
            Set<String> codes = request.getAllowedProviderCodes().stream()
                    .filter(StringUtils::hasText)
                    .map(code -> code.trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            platformSystemConfigService.setAllowedProviderCodes(codes, operatorId);
            detail.append(" allowedProviderCodes=").append(codes);
        }
        if (request.getMaintenanceEnabled() != null) {
            platformSystemConfigService.setMaintenanceEnabled(request.getMaintenanceEnabled(), operatorId);
            detail.append(" maintenanceEnabled=").append(request.getMaintenanceEnabled());
        }
        if (request.getMaintenanceMessage() != null) {
            platformSystemConfigService.setMaintenanceMessage(request.getMaintenanceMessage(), operatorId);
            detail.append(" maintenanceMessage=updated");
        }
        if (request.getPlatformAnnouncement() != null) {
            platformSystemConfigService.setPlatformAnnouncement(request.getPlatformAnnouncement(), operatorId);
            detail.append(" platformAnnouncement=updated");
        }
        if (detail.length() > "更新系统配置:".length()) {
            auditLogService.record(
                    "platform.settings.update",
                    "system_config",
                    null,
                    detail.toString(),
                    TenantContext.getTenantId(),
                    operatorId);
        }
        return getSettings();
    }

    public PageResult<AuditLogVO> pageLoginLogs(
            int page, int pageSize, String keyword, LocalDate startDate, LocalDate endDate) {
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);
        requireSuperAdmin();

        QueryWrapper query = QueryWrapper.create()
                .and("(action LIKE 'auth.login%' OR action LIKE 'auth.logout%')");
        if (startDate != null) {
            query.ge("created_at", LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            query.le("created_at", LocalDateTime.of(endDate, LocalTime.MAX));
        }
        if (StringUtils.hasText(keyword)) {
            query.and("(detail LIKE ? OR client_ip LIKE ? OR action LIKE ?)",
                    "%" + keyword.trim() + "%",
                    "%" + keyword.trim() + "%",
                    "%" + keyword.trim() + "%");
        }
        query.orderBy("created_at", false);

        Page<AuditLogEntity> result = auditLogMapper.paginate(Page.of(page, pageSize), query);
        List<AuditLogVO> list = result.getRecords().stream().map(this::toAuditLogVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    private Integer resolveDaysUntilExpiry(LocalDateTime expireAt) {
        if (expireAt == null) {
            return null;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expireAt.toLocalDate());
        return (int) days;
    }

    private Integer calcPercent(long used, long limit) {
        if (limit <= 0) {
            return null;
        }
        return Math.min(100, (int) Math.round(used * 100.0 / limit));
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private YearMonth resolveMonth(String month) {
        if (!StringUtils.hasText(month)) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (Exception ex) {
            throw new BusinessException("月份格式无效，请使用 yyyy-MM");
        }
    }

    private PlatformTenantUsageVO toPlatformTenantUsageVO(TenantUsageAggregate aggregate) {
        return PlatformTenantUsageVO.builder()
                .tenantId(aggregate.getTenantId())
                .tenantName(aggregate.getTenantName())
                .calls(safeLong(aggregate.getCalls()))
                .tokens(safeLong(aggregate.getTokens()))
                .build();
    }

    private PlatformModelUsageVO toPlatformModelUsageVO(ModelUsageAggregate aggregate) {
        return PlatformModelUsageVO.builder()
                .modelName(aggregate.getModelName())
                .displayName(aggregate.getDisplayName())
                .calls(safeLong(aggregate.getCalls()))
                .tokens(safeLong(aggregate.getTokens()))
                .build();
    }

    private PlatformProviderStatVO toPlatformProviderStatVO(ProviderCodeAggregate aggregate) {
        return PlatformProviderStatVO.builder()
                .providerCode(aggregate.getProviderCode())
                .count(safeLong(aggregate.getCount()))
                .build();
    }

    private List<PlatformModelProviderVO> toPlatformModelProviderVOs(List<ModelProviderEntity> providers) {
        if (providers.isEmpty()) {
            return List.of();
        }
        ModelCountMaps countMaps = loadModelCountMaps(
                providers.stream().map(ModelProviderEntity::getId).toList());
        return providers.stream().map(provider -> toPlatformModelProviderVO(provider, countMaps)).toList();
    }

    private PlatformModelProviderVO toPlatformModelProviderVO(ModelProviderEntity provider, ModelCountMaps countMaps) {
        TenantEntity tenant = tenantMapper.selectOneById(provider.getTenantId());
        return PlatformModelProviderVO.builder()
                .id(provider.getId())
                .tenantId(provider.getTenantId())
                .tenantName(tenant != null ? tenant.getTenantName() : null)
                .providerCode(provider.getProviderCode())
                .providerName(provider.getProviderName())
                .baseUrl(provider.getBaseUrl())
                .apiKeyMasked(maskApiKey(provider.getApiKeyEncrypted()))
                .enabled(provider.getIsEnabled() != null && provider.getIsEnabled() == 1)
                .modelCount(countMaps.total().getOrDefault(provider.getId(), 0L).intValue())
                .enabledModelCount(countMaps.enabled().getOrDefault(provider.getId(), 0L).intValue())
                .createdAt(provider.getCreatedAt())
                .updatedAt(provider.getUpdatedAt())
                .build();
    }

    private ModelCountMaps loadModelCountMaps(List<Long> providerIds) {
        if (providerIds.isEmpty()) {
            return new ModelCountMaps(Map.of(), Map.of());
        }
        List<ModelConfigEntity> configs = modelConfigMapper.selectListByQuery(
                QueryWrapper.create().in("provider_id", providerIds).eq("is_deleted", 0));
        Map<Long, Long> total = configs.stream()
                .collect(Collectors.groupingBy(ModelConfigEntity::getProviderId, Collectors.counting()));
        Map<Long, Long> enabled = configs.stream()
                .filter(config -> config.getIsEnabled() != null && config.getIsEnabled() == 1)
                .collect(Collectors.groupingBy(ModelConfigEntity::getProviderId, Collectors.counting()));
        return new ModelCountMaps(total, enabled);
    }

    private String maskApiKey(String encrypted) {
        if (!StringUtils.hasText(encrypted)) {
            return "未配置";
        }
        String decrypted = cryptoService.tryDecrypt(encrypted);
        if (decrypted == null) {
            return "密钥无效";
        }
        return cryptoService.maskSecret(decrypted);
    }

    private ModelProviderEntity getModelProviderOrThrow(Long providerId) {
        ModelProviderEntity provider = modelProviderMapper.selectOneByQuery(
                QueryWrapper.create().eq("id", providerId).eq("is_deleted", 0));
        if (provider == null) {
            throw new BusinessException("模型供应商不存在");
        }
        return provider;
    }

    private record ModelCountMaps(Map<Long, Long> total, Map<Long, Long> enabled) {
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

    private UserEntity getUserOrThrow(Long userId) {
        UserEntity user = userMapper.selectOneByQuery(
                QueryWrapper.create().eq("id", userId).eq("is_deleted", 0));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private PlatformUserVO toPlatformUserVO(UserEntity user) {
        return PlatformUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .accountType(user.getAccountType())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .createdAt(user.getCreatedAt())
                .memberships(loadMemberships(user.getId()))
                .build();
    }

    private List<PlatformUserMembershipVO> loadMemberships(Long userId) {
        List<TenantMemberEntity> members = tenantMemberMapper.selectListByQuery(
                QueryWrapper.create().eq("user_id", userId).eq("is_deleted", 0));
        if (members.isEmpty()) {
            return List.of();
        }

        List<Long> tenantIds = members.stream().map(TenantMemberEntity::getTenantId).distinct().toList();
        List<Long> roleIds = members.stream().map(TenantMemberEntity::getRoleId).filter(Objects::nonNull).distinct().toList();

        Map<Long, TenantEntity> tenantMap = tenantMapper.selectListByQuery(
                        QueryWrapper.create().in("id", tenantIds))
                .stream()
                .collect(Collectors.toMap(TenantEntity::getId, t -> t, (a, b) -> a));
        Map<Long, RoleEntity> roleMap = roleIds.isEmpty()
                ? Map.of()
                : roleMapper.selectListByQuery(QueryWrapper.create().in("id", roleIds))
                        .stream()
                        .collect(Collectors.toMap(RoleEntity::getId, r -> r, (a, b) -> a));

        List<PlatformUserMembershipVO> memberships = new ArrayList<>();
        for (TenantMemberEntity member : members) {
            TenantEntity tenant = tenantMap.get(member.getTenantId());
            RoleEntity role = member.getRoleId() != null ? roleMap.get(member.getRoleId()) : null;
            memberships.add(PlatformUserMembershipVO.builder()
                    .tenantId(member.getTenantId())
                    .tenantName(tenant != null ? tenant.getTenantName() : null)
                    .roleCode(role != null ? role.getRoleCode() : null)
                    .roleName(role != null ? role.getRoleName() : null)
                    .status(member.getStatus())
                    .build());
        }
        return memberships;
    }

    private AuditLogVO toAuditLogVO(AuditLogEntity entity) {
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

    private void persistApiAlerts(List<PlatformApiAlertVO> alerts, LocalDate eventDate) {
        for (PlatformApiAlertVO alert : alerts) {
            PlatformApiAlertEventEntity existing = platformApiAlertEventMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("alert_type", alert.getType())
                            .eq("tenant_id", alert.getTenantId())
                            .eq("event_date", eventDate));
            LocalDateTime now = LocalDateTime.now();
            if (existing == null) {
                PlatformApiAlertEventEntity entity = new PlatformApiAlertEventEntity();
                entity.setAlertType(alert.getType());
                entity.setSeverity(alert.getSeverity());
                entity.setTenantId(alert.getTenantId());
                entity.setTenantName(alert.getTenantName());
                entity.setMessage(alert.getMessage());
                entity.setMetricValue(alert.getMetricValue());
                entity.setThreshold(alert.getThreshold());
                entity.setStatus("OPEN");
                entity.setEventDate(eventDate);
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                platformApiAlertEventMapper.insert(entity);
                continue;
            }
            if ("ACKED".equals(existing.getStatus())) {
                continue;
            }
            existing.setSeverity(alert.getSeverity());
            existing.setTenantName(alert.getTenantName());
            existing.setMessage(alert.getMessage());
            existing.setMetricValue(alert.getMetricValue());
            existing.setThreshold(alert.getThreshold());
            existing.setUpdatedAt(now);
            platformApiAlertEventMapper.update(existing);
        }
    }

    private PlatformApiAlertEventVO toApiAlertEventVO(PlatformApiAlertEventEntity entity) {
        return PlatformApiAlertEventVO.builder()
                .id(entity.getId())
                .alertType(entity.getAlertType())
                .severity(entity.getSeverity())
                .tenantId(entity.getTenantId())
                .tenantName(entity.getTenantName())
                .message(entity.getMessage())
                .metricValue(entity.getMetricValue())
                .threshold(entity.getThreshold())
                .status(entity.getStatus())
                .ackedBy(entity.getAckedBy())
                .ackedAt(entity.getAckedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private PlatformModelCatalogVO toModelCatalogVO(PlatformModelCatalogEntity entity) {
        return PlatformModelCatalogVO.builder()
                .id(entity.getId())
                .providerCode(entity.getProviderCode())
                .modelName(entity.getModelName())
                .displayName(entity.getDisplayName())
                .modelType(entity.getModelType())
                .inputPricePer1k(entity.getInputPricePer1k())
                .outputPricePer1k(entity.getOutputPricePer1k())
                .currency(entity.getCurrency())
                .enabled(entity.getEnabled() != null && entity.getEnabled() == 1)
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
