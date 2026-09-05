package ai.novaflow.user.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.AccountTypes;
import ai.novaflow.common.security.RoleCodes;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.tenant.entity.WorkspaceEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.tenant.mapper.WorkspaceMapper;
import ai.novaflow.tenant.support.TenantLimits;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.user.mapper.UserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TenantOnboardingService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");

    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final RoleMapper roleMapper;
    private final WorkspaceMapper workspaceMapper;
    private final PasswordEncoder passwordEncoder;

    public record ProvisionResult(TenantEntity tenant, UserEntity owner) {
    }

    public ProvisionResult provisionTenantWithOwner(
            String tenantName,
            String planType,
            String ownerEmail,
            String ownerPassword,
            String ownerNickname,
            String contactName,
            String contactEmail,
            String contactPhone) {
        validatePassword(ownerPassword);

        String normalizedEmail = ownerEmail.trim().toLowerCase(Locale.ROOT);
        long existing = userMapper.selectCountByQuery(
                QueryWrapper.create().eq("email", normalizedEmail).eq("is_deleted", 0));
        if (existing > 0) {
            throw new BusinessException("该邮箱已被注册");
        }

        RoleEntity ownerRole = roleMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", 0)
                        .eq("role_code", RoleCodes.TENANT_OWNER)
                        .eq("is_deleted", 0));
        if (ownerRole == null) {
            throw new BusinessException("系统角色未初始化，请联系管理员");
        }

        LocalDateTime now = LocalDateTime.now();
        String normalizedPlan = normalizePlanType(planType);

        TenantEntity tenant = new TenantEntity();
        tenant.setTenantCode(generateTenantCode(tenantName));
        tenant.setTenantName(tenantName.trim());
        tenant.setPlanType(normalizedPlan);
        tenant.setContactName(trimToNull(contactName));
        tenant.setContactEmail(trimToNull(contactEmail));
        tenant.setContactPhone(trimToNull(contactPhone));
        tenant.setStatus(1);
        tenant.setExpireAt(now.plusYears(1));
        TenantLimits.applyPlanDefaults(tenant);
        tenant.setIsDeleted(0);
        tenant.setCreatedAt(now);
        tenant.setUpdatedAt(now);
        tenantMapper.insert(tenant);

        String username = buildUsername(normalizedEmail);
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(ownerPassword));
        user.setNickname(resolveNickname(ownerNickname, normalizedEmail));
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

        return new ProvisionResult(tenant, user);
    }

    private void validatePassword(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException("密码需至少包含一个字母和一个数字");
        }
    }

    private String resolveNickname(String nickname, String email) {
        if (StringUtils.hasText(nickname)) {
            return nickname.trim();
        }
        return email.substring(0, email.indexOf('@'));
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

    private String normalizePlanType(String planType) {
        if (!StringUtils.hasText(planType)) {
            return "free";
        }
        return planType.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
