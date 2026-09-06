package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.IpBlacklistChecker;
import ai.novaflow.common.security.MaintenanceModeChecker;
import ai.novaflow.security.ratelimit.AuthRateLimiter;
import ai.novaflow.security.ratelimit.LoginFailureLockService;
import ai.novaflow.security.session.SessionTenantIds;
import ai.novaflow.user.domain.dto.LoginRequest;
import ai.novaflow.user.domain.dto.RegisterRequest;
import ai.novaflow.user.domain.vo.LoginVO;
import ai.novaflow.common.security.RoleCodes;
import ai.novaflow.common.security.AccountTypes;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.tenant.entity.WorkspaceEntity;
import ai.novaflow.tenant.support.TenantLimits;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.user.mapper.UserMapper;
import ai.novaflow.tenant.mapper.WorkspaceMapper;
import ai.novaflow.user.service.AuditLogService;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");

    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final RoleMapper roleMapper;
    private final WorkspaceMapper workspaceMapper;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    private final AuthRateLimiter authRateLimiter;
    private final LoginFailureLockService loginFailureLockService;
    private final IpBlacklistChecker ipBlacklistChecker;
    private final AuditLogService auditLogService;
    private final PlatformSystemConfigService platformSystemConfigService;
    private final MaintenanceModeChecker maintenanceModeChecker;
    private final PlatformRiskControlService platformRiskControlService;

    public LoginVO login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail();
        String clientIp = httpRequest.getRemoteAddr();
        ipBlacklistChecker.requireAllowed(clientIp);
        authRateLimiter.checkLogin(email, clientIp);
        loginFailureLockService.checkLocked(email, clientIp);

        UserEntity user = userMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("email", email)
                        .eq("is_deleted", 0)
        );
        if (user == null || user.getStatus() != 1
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginFailureLockService.recordFailure(email, clientIp);
            auditLogService.record(
                    "auth.login.failed",
                    "user",
                    user != null ? user.getId() : null,
                    "登录失败",
                    null,
                    user != null ? user.getId() : null,
                    clientIp);
            throw new BusinessException("邮箱或密码错误");
        }
        loginFailureLockService.clearFailures(email, clientIp);

        if (AccountTypes.isPlatform(user.getAccountType())) {
            return loginPlatformUser(user, httpRequest, clientIp);
        }
        return loginTenantUser(user, httpRequest, clientIp);
    }

    private LoginVO loginPlatformUser(UserEntity user, HttpServletRequest httpRequest, String clientIp) {
        RoleEntity role = permissionService.resolvePlatformRole(user);

        StpUtil.login(user.getId());
        StpUtil.getSession().set("tenantId", 0L);
        StpUtil.getSession().set("accountType", AccountTypes.PLATFORM);
        StpUtil.getSession().set("roleCode", role.getRoleCode());

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userMapper.update(user);

        auditLogService.record(
                "auth.login",
                "user",
                user.getId(),
                "平台管理员登录",
                0L,
                user.getId(),
                clientIp);

        return buildPlatformLoginVO(user, role);
    }

    private LoginVO loginTenantUser(UserEntity user, HttpServletRequest httpRequest, String clientIp) {
        requireTenantAccessAllowed();
        TenantMemberEntity member = tenantMemberMapper.selectOneByQuery(
                QueryWrapper.create().where("user_id = ?", user.getId()).and("is_deleted = 0").limit(1)
        );
        if (member == null) {
            throw new BusinessException("用户未加入任何企业");
        }
        if (member.getStatus() == null || member.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        TenantEntity tenant = tenantMapper.selectOneById(member.getTenantId());
        if (tenant == null || Integer.valueOf(1).equals(tenant.getIsDeleted())
                || tenant.getStatus() == null || tenant.getStatus() != 1) {
            throw new BusinessException("企业已停用，请联系管理员");
        }
        if (tenant.getExpireAt() != null && tenant.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("企业已到期，请联系管理员续期");
        }
        RoleEntity role = roleMapper.selectOneById(member.getRoleId());

        String previousIp = user.getLastLoginIp();
        StpUtil.login(user.getId());
        StpUtil.getSession().set("tenantId", tenant.getId());
        StpUtil.getSession().set("accountType", AccountTypes.TENANT);
        StpUtil.getSession().set("roleCode", role != null ? role.getRoleCode() : "user");

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(httpRequest.getRemoteAddr());
        userMapper.update(user);

        platformRiskControlService.onTenantLoginSuccess(
                user, tenant, clientIp, resolveUserAgent(httpRequest), previousIp);

        TenantContext.setTenantId(tenant.getId());

        auditLogService.record(
                "auth.login",
                "user",
                user.getId(),
                "用户登录",
                tenant.getId(),
                user.getId(),
                clientIp);

        return buildLoginVO(user, tenant, role);
    }

    @Transactional
    public LoginVO register(RegisterRequest request, HttpServletRequest httpRequest) {
        requireTenantAccessAllowed();
        if (!platformSystemConfigService.isRegistrationEnabled()) {
            throw new BusinessException("当前环境未开放自助注册，请联系管理员邀请");
        }
        String clientIp = httpRequest.getRemoteAddr();
        ipBlacklistChecker.requireAllowed(clientIp);
        platformRiskControlService.checkBatchRegisterAllowed(clientIp);
        authRateLimiter.checkRegister(request.getEmail(), clientIp);
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        validatePassword(request.getPassword());

        long existing = userMapper.selectCountByQuery(
                QueryWrapper.create().eq("email", request.getEmail()).eq("is_deleted", 0)
        );
        if (existing > 0) {
            throw new BusinessException("该邮箱已被注册");
        }

        RoleEntity ownerRole = roleMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", 0)
                        .eq("role_code", RoleCodes.TENANT_OWNER)
                        .eq("is_deleted", 0)
        );
        if (ownerRole == null) {
            throw new BusinessException("系统角色未初始化，请联系管理员");
        }

        LocalDateTime now = LocalDateTime.now();

        boolean personal = "personal".equalsIgnoreCase(
                request.getPlanType() != null ? request.getPlanType().trim() : "");

        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode(generateTenantCode(request.getCompanyName()));
        tenant.setTenantName(request.getCompanyName().trim());
        tenant.setPlanType(personal ? "personal" : "free");
        tenant.setStatus(1);
        tenant.setExpireAt(now.plusYears(1));
        TenantLimits.applyPlanDefaults(tenant);
        tenant.setIsDeleted(0);
        tenant.setCreatedAt(now);
        tenant.setUpdatedAt(now);
        tenantMapper.insert(tenant);

        String username = buildUsername(request.getEmail());
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(resolveNickname(request));
        user.setAccountType(AccountTypes.TENANT);
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        TenantMemberEntity member = new TenantMemberEntity();
        member.setTenantId(tenant.getId());
        member.setUserId(user.getId());
        member.setRoleId(ownerRole.getId());
        member.setStatus(1);
        member.setIsDeleted(0);
        member.setJoinedAt(now);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        tenantMemberMapper.insert(member);

        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setTenantId(tenant.getId());
        workspace.setWorkspaceName("默认工作空间");
        workspace.setIsDefault(1);
        workspace.setCreatedBy(user.getId());
        workspace.setIsDeleted(0);
        workspace.setCreatedAt(now);
        workspace.setUpdatedAt(now);
        workspaceMapper.insert(workspace);

        StpUtil.login(user.getId());
        StpUtil.getSession().set("tenantId", tenant.getId());
        StpUtil.getSession().set("accountType", AccountTypes.TENANT);
        StpUtil.getSession().set("roleCode", ownerRole.getRoleCode());

        user.setLastLoginAt(now);
        user.setLastLoginIp(httpRequest.getRemoteAddr());
        userMapper.update(user);

        TenantContext.setTenantId(tenant.getId());

        auditLogService.record(
                "user.register",
                "tenant",
                tenant.getId(),
                "注册企业: " + tenant.getTenantName(),
                tenant.getId(),
                user.getId(),
                clientIp);

        platformRiskControlService.onRegisterSuccess(
                request.getEmail(), clientIp, resolveUserAgent(httpRequest), tenant.getId());

        return buildLoginVO(user, tenant, ownerRole);
    }

    public LoginVO currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        UserEntity user = userMapper.selectOneById(userId);
        if (user != null && AccountTypes.isPlatform(user.getAccountType())) {
            RoleEntity role = permissionService.resolvePlatformRole(user);
            return buildPlatformLoginVO(user, role);
        }

        Long tenantId = SessionTenantIds.toLong(StpUtil.getSession().get("tenantId"));
        TenantEntity tenant = tenantMapper.selectOneById(tenantId);
        RoleEntity role = permissionService.resolveRole(userId, tenantId);

        return buildLoginVO(user, tenant, role);
    }

    public void logout() {
        if (StpUtil.isLogin()) {
            long userId = StpUtil.getLoginIdAsLong();
            Long tenantId = SessionTenantIds.toLong(StpUtil.getSession().get("tenantId"));
            auditLogService.record("auth.logout", "user", userId, "用户登出", tenantId, userId);
        }
        StpUtil.logout();
    }

    private LoginVO buildLoginVO(UserEntity user, TenantEntity tenant, RoleEntity role) {
        List<String> permissions = permissionService.getPermissionCodes(
                user.getId(),
                tenant != null ? tenant.getId() : null
        );

        return LoginVO.builder()
                .token(StpUtil.getTokenValue())
                .user(LoginVO.UserInfoVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .accountType(AccountTypes.TENANT)
                        .roleCode(role != null ? role.getRoleCode() : null)
                        .roleName(role != null ? role.getRoleName() : null)
                        .build())
                .tenant(LoginVO.TenantInfoVO.builder()
                        .id(tenant.getId())
                        .tenantName(tenant.getTenantName())
                        .planType(tenant.getPlanType())
                        .build())
                .permissions(permissions)
                .build();
    }

    private LoginVO buildPlatformLoginVO(UserEntity user, RoleEntity role) {
        List<String> permissions = permissionService.getPermissionCodesByRoleId(role.getId());
        return LoginVO.builder()
                .token(StpUtil.getTokenValue())
                .user(LoginVO.UserInfoVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .accountType(AccountTypes.PLATFORM)
                        .roleCode(role.getRoleCode())
                        .roleName(role.getRoleName())
                        .build())
                .tenant(LoginVO.TenantInfoVO.builder()
                        .id(0L)
                        .tenantName("NovaFlow 平台")
                        .planType("platform")
                        .build())
                .permissions(permissions)
                .build();
    }

    private void requireTenantAccessAllowed() {
        if (maintenanceModeChecker != null && maintenanceModeChecker.isMaintenanceEnabled()) {
            throw new BusinessException(
                    MaintenanceModeChecker.MAINTENANCE_CODE,
                    maintenanceModeChecker.getMaintenanceMessage());
        }
    }

    private void validatePassword(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException("密码需至少包含一个字母和一个数字");
        }
    }

    private String resolveNickname(RegisterRequest request) {
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            return request.getNickname().trim();
        }
        return request.getEmail().substring(0, request.getEmail().indexOf('@'));
    }

    private String buildUsername(String email) {
        String base = email.substring(0, email.indexOf('@'))
                .replaceAll("[^a-zA-Z0-9_]", "")
                .toLowerCase(Locale.ROOT);
        if (base.isBlank()) {
            base = "user";
        }
        String candidate = base;
        int suffix = 1;
        while (userMapper.selectCountByQuery(
                QueryWrapper.create().eq("username", candidate).eq("is_deleted", 0)
        ) > 0) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private String generateTenantCode(String companyName) {
        String base = companyName.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.isBlank()) {
            base = "tenant";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        String candidate = base;
        while (tenantMapper.selectCountByQuery(
                QueryWrapper.create().eq("tenant_code", candidate).eq("is_deleted", 0)
        ) > 0) {
            candidate = base + "-" + UUID.randomUUID().toString().substring(0, 4);
        }
        return candidate;
    }

    private String resolveUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String userAgent = request.getHeader("User-Agent");
        if (!org.springframework.util.StringUtils.hasText(userAgent)) {
            return null;
        }
        String trimmed = userAgent.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }
}
