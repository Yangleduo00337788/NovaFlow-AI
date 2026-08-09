package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.user.domain.dto.MemberInviteRequest;
import ai.novaflow.user.domain.dto.MemberUpdateRequest;
import ai.novaflow.user.domain.dto.TenantUpdateRequest;
import ai.novaflow.user.domain.dto.WorkspaceSaveRequest;
import ai.novaflow.user.domain.vo.MemberVO;
import ai.novaflow.user.domain.vo.TenantPlanSummaryVO;
import ai.novaflow.user.domain.vo.TenantVO;
import ai.novaflow.user.domain.vo.WorkspaceVO;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.user.entity.TenantEntity;
import ai.novaflow.user.entity.TenantMemberEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.user.entity.WorkspaceEntity;
import ai.novaflow.user.mapper.TenantMapper;
import ai.novaflow.user.mapper.TenantMemberMapper;
import ai.novaflow.user.mapper.UserMapper;
import ai.novaflow.user.mapper.WorkspaceMapper;
import ai.novaflow.user.mapper.ApplicationMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");

    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final WorkspaceMapper workspaceMapper;
    private final ApplicationMapper applicationMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;

    public TenantVO getTenant() {
        Long tenantId = requireTenantId();
        requireTenantManagePermission();
        TenantEntity tenant = getTenantOrThrow(tenantId);
        return toTenantVO(tenant, countActiveMembers(tenantId));
    }

    public TenantPlanSummaryVO getPlanSummary() {
        Long tenantId = requireTenantId();
        TenantEntity tenant = getTenantOrThrow(tenantId);
        int memberCount = countActiveMembers(tenantId);
        int maxMembers = tenant.getMaxMembers() != null && tenant.getMaxMembers() > 0 ? tenant.getMaxMembers() : 100;
        int usedPercent = Math.min(100, (int) Math.round(memberCount * 100.0 / maxMembers));
        return TenantPlanSummaryVO.builder()
                .planType(tenant.getPlanType())
                .planTypeLabel(resolvePlanTypeLabel(tenant.getPlanType()))
                .expireAt(tenant.getExpireAt())
                .memberCount(memberCount)
                .maxMembers(maxMembers)
                .usedPercent(usedPercent)
                .build();
    }

    @Transactional
    public TenantVO updateTenant(TenantUpdateRequest request) {
        Long tenantId = requireTenantId();
        requireTenantManagePermission();
        TenantEntity tenant = getTenantOrThrow(tenantId);
        tenant.setTenantName(request.getTenantName().trim());
        tenant.setLogoUrl(trimToNull(request.getLogoUrl()));
        tenant.setContactName(trimToNull(request.getContactName()));
        tenant.setContactEmail(trimToNull(request.getContactEmail()));
        tenant.setContactPhone(trimToNull(request.getContactPhone()));
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantMapper.update(tenant);
        return toTenantVO(tenant, countActiveMembers(tenantId));
    }

    public List<WorkspaceVO> listWorkspaces() {
        Long tenantId = requireTenantId();
        requireTenantManagePermission();
        return workspaceMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .orderBy("is_default", false)
                        .orderBy("created_at", true)
        ).stream().map(this::toWorkspaceVO).toList();
    }

    @Transactional
    public WorkspaceVO createWorkspace(WorkspaceSaveRequest request) {
        Long tenantId = requireTenantId();
        long userId = StpUtil.getLoginIdAsLong();
        requireTenantManagePermission();
        ensureWorkspaceNameUnique(tenantId, request.getWorkspaceName(), null);

        LocalDateTime now = LocalDateTime.now();
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setTenantId(tenantId);
        entity.setWorkspaceName(request.getWorkspaceName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setIsDefault(0);
        entity.setCreatedBy(userId);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        workspaceMapper.insert(entity);
        return toWorkspaceVO(entity);
    }

    @Transactional
    public WorkspaceVO updateWorkspace(Long id, WorkspaceSaveRequest request) {
        Long tenantId = requireTenantId();
        requireTenantManagePermission();
        WorkspaceEntity entity = getWorkspaceOrThrow(id, tenantId);
        ensureWorkspaceNameUnique(tenantId, request.getWorkspaceName(), id);
        entity.setWorkspaceName(request.getWorkspaceName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setUpdatedAt(LocalDateTime.now());
        workspaceMapper.update(entity);
        return toWorkspaceVO(entity);
    }

    @Transactional
    public void deleteWorkspace(Long id) {
        Long tenantId = requireTenantId();
        requireTenantManagePermission();
        WorkspaceEntity entity = getWorkspaceOrThrow(id, tenantId);
        if (Objects.equals(entity.getIsDefault(), 1)) {
            throw new BusinessException("默认工作空间不能删除");
        }
        long appCount = countApplicationsByWorkspace(id);
        if (appCount > 0) {
            throw new BusinessException("工作空间下仍有应用，无法删除");
        }
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        workspaceMapper.update(entity);
    }

    public PageResult<MemberVO> pageMembers(int page, int pageSize, String keyword) {
        Long tenantId = requireTenantId();
        requireMemberManagePermission();

        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            List<Long> userIds = userMapper.selectListByQuery(
                    QueryWrapper.create()
                            .eq("is_deleted", 0)
                            .and("(email like ? or nickname like ? or username like ?)",
                                    "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%")
            ).stream().map(UserEntity::getId).toList();
            if (userIds.isEmpty()) {
                return PageResult.of(List.of(), 0, page, pageSize);
            }
            query.in("user_id", userIds);
        }
        query.orderBy("joined_at", false);

        Page<TenantMemberEntity> result = tenantMemberMapper.paginate(Page.of(page, pageSize), query);
        Map<Long, UserEntity> userMap = loadUsers(result.getRecords());
        Map<Long, RoleEntity> roleMap = loadRoles(result.getRecords());
        List<MemberVO> list = result.getRecords().stream()
                .map(member -> toMemberVO(member, userMap.get(member.getUserId()), roleMap.get(member.getRoleId())))
                .toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    @Transactional
    public MemberVO inviteMember(MemberInviteRequest request) {
        Long tenantId = requireTenantId();
        requireMemberManagePermission();
        TenantEntity tenant = getTenantOrThrow(tenantId);
        int memberCount = countActiveMembers(tenantId);
        int maxMembers = tenant.getMaxMembers() != null && tenant.getMaxMembers() > 0 ? tenant.getMaxMembers() : 100;
        if (memberCount >= maxMembers) {
            throw new BusinessException("成员数已达上限（" + maxMembers + "）");
        }

        RoleEntity role = permissionService.requireSystemRole(request.getRoleCode());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        UserEntity user = userMapper.selectOneByQuery(
                QueryWrapper.create().eq("email", email).eq("is_deleted", 0)
        );

        LocalDateTime now = LocalDateTime.now();
        if (user == null) {
            if (!StringUtils.hasText(request.getPassword())) {
                throw new BusinessException("新用户需设置初始密码");
            }
            validatePassword(request.getPassword());
            user = new UserEntity();
            user.setEmail(email);
            user.setUsername(buildUsername(email));
            user.setNickname(resolveNickname(request.getNickname(), email));
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setStatus(1);
            user.setIsDeleted(0);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            userMapper.insert(user);
        } else {
            ensureUserNotInOtherTenant(user.getId(), tenantId);
        }

        TenantMemberEntity existing = tenantMemberMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("user_id", user.getId())
                        .eq("is_deleted", 0)
        );
        if (existing != null) {
            throw new BusinessException("该用户已是企业成员");
        }

        TenantMemberEntity member = new TenantMemberEntity();
        member.setTenantId(tenantId);
        member.setUserId(user.getId());
        member.setRoleId(role.getId());
        member.setStatus(1);
        member.setJoinedAt(now);
        member.setIsDeleted(0);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        tenantMemberMapper.insert(member);
        return toMemberVO(member, user, role);
    }

    @Transactional
    public MemberVO updateMember(Long memberId, MemberUpdateRequest request) {
        Long tenantId = requireTenantId();
        long currentUserId = StpUtil.getLoginIdAsLong();
        requireMemberManagePermission();

        TenantMemberEntity member = getMemberOrThrow(memberId, tenantId);
        UserEntity user = userMapper.selectOneById(member.getUserId());
        RoleEntity currentRole = permissionService.resolveRole(member.getUserId(), tenantId);

        if (request.getRoleCode() != null) {
            RoleEntity newRole = permissionService.requireSystemRole(request.getRoleCode());
            if (member.getUserId() == currentUserId && !request.getRoleCode().equals(currentRole.getRoleCode())) {
                throw new BusinessException("不能修改自己的角色");
            }
            if (currentRole != null
                    && "tenant_admin".equals(currentRole.getRoleCode())
                    && !"tenant_admin".equals(request.getRoleCode())) {
                ensureAnotherTenantAdminExists(tenantId, member.getUserId());
            }
            member.setRoleId(newRole.getId());
            currentRole = newRole;
        }

        if (request.getStatus() != null) {
            if (member.getUserId() == currentUserId && request.getStatus() != 1) {
                throw new BusinessException("不能禁用自己");
            }
            if (request.getStatus() != 1
                    && currentRole != null
                    && "tenant_admin".equals(currentRole.getRoleCode())) {
                ensureAnotherTenantAdminExists(tenantId, member.getUserId());
            }
            member.setStatus(request.getStatus());
            if (user != null) {
                user.setStatus(request.getStatus());
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.update(user);
            }
        }

        member.setUpdatedAt(LocalDateTime.now());
        tenantMemberMapper.update(member);
        return toMemberVO(member, user, currentRole);
    }

    @Transactional
    public void removeMember(Long memberId) {
        Long tenantId = requireTenantId();
        long currentUserId = StpUtil.getLoginIdAsLong();
        requireMemberManagePermission();

        TenantMemberEntity member = getMemberOrThrow(memberId, tenantId);
        if (member.getUserId() == currentUserId) {
            throw new BusinessException("不能移除自己");
        }
        RoleEntity role = permissionService.resolveRole(member.getUserId(), tenantId);
        if (role != null && "tenant_admin".equals(role.getRoleCode())) {
            ensureAnotherTenantAdminExists(tenantId, member.getUserId());
        }

        member.setIsDeleted(1);
        member.setUpdatedAt(LocalDateTime.now());
        tenantMemberMapper.update(member);
    }

    private void ensureAnotherTenantAdminExists(Long tenantId, Long excludeUserId) {
        RoleEntity adminRole = permissionService.requireSystemRole("tenant_admin");
        long count = tenantMemberMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("role_id", adminRole.getId())
                        .eq("status", 1)
                        .eq("is_deleted", 0)
                        .ne("user_id", excludeUserId)
        );
        if (count == 0) {
            throw new BusinessException("企业至少保留一名管理员");
        }
    }

    private void ensureUserNotInOtherTenant(Long userId, Long currentTenantId) {
        TenantMemberEntity other = tenantMemberMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("user_id", userId)
                        .eq("is_deleted", 0)
                        .ne("tenant_id", currentTenantId)
                        .limit(1)
        );
        if (other != null) {
            throw new BusinessException("该用户已属于其他企业");
        }
    }

    private void ensureWorkspaceNameUnique(Long tenantId, String workspaceName, Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("workspace_name", workspaceName.trim())
                .eq("is_deleted", 0);
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        if (workspaceMapper.selectCountByQuery(query) > 0) {
            throw new BusinessException("工作空间名称已存在");
        }
    }

    private long countApplicationsByWorkspace(Long workspaceId) {
        return applicationMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("workspace_id", workspaceId)
                        .eq("is_deleted", 0)
        );
    }

    private int countActiveMembers(Long tenantId) {
        return (int) tenantMemberMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("status", 1)
                        .eq("is_deleted", 0)
        );
    }

    private TenantEntity getTenantOrThrow(Long tenantId) {
        TenantEntity tenant = tenantMapper.selectOneByQuery(
                QueryWrapper.create().eq("id", tenantId).eq("is_deleted", 0)
        );
        if (tenant == null) {
            throw new BusinessException("企业不存在");
        }
        return tenant;
    }

    private WorkspaceEntity getWorkspaceOrThrow(Long id, Long tenantId) {
        WorkspaceEntity entity = workspaceMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("工作空间不存在");
        }
        return entity;
    }

    private TenantMemberEntity getMemberOrThrow(Long memberId, Long tenantId) {
        TenantMemberEntity member = tenantMemberMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", memberId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        if (member == null) {
            throw new BusinessException("成员不存在");
        }
        return member;
    }

    private Map<Long, UserEntity> loadUsers(List<TenantMemberEntity> members) {
        List<Long> userIds = members.stream().map(TenantMemberEntity::getUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectListByQuery(QueryWrapper.create().in("id", userIds))
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user, (a, b) -> a));
    }

    private Map<Long, RoleEntity> loadRoles(List<TenantMemberEntity> members) {
        List<Long> roleIds = members.stream().map(TenantMemberEntity::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return permissionService.getRolesByIds(roleIds);
    }

    private TenantVO toTenantVO(TenantEntity tenant, int memberCount) {
        return TenantVO.builder()
                .id(tenant.getId())
                .tenantCode(tenant.getTenantCode())
                .tenantName(tenant.getTenantName())
                .logoUrl(tenant.getLogoUrl())
                .contactName(tenant.getContactName())
                .contactEmail(tenant.getContactEmail())
                .contactPhone(tenant.getContactPhone())
                .planType(tenant.getPlanType())
                .planTypeLabel(resolvePlanTypeLabel(tenant.getPlanType()))
                .status(tenant.getStatus())
                .expireAt(tenant.getExpireAt())
                .maxMembers(tenant.getMaxMembers())
                .memberCount(memberCount)
                .maxAgents(tenant.getMaxAgents())
                .maxKnowledge(tenant.getMaxKnowledge())
                .maxStorageMb(tenant.getMaxStorageMb())
                .monthlyTokenQuota(tenant.getMonthlyTokenQuota())
                .createdAt(tenant.getCreatedAt())
                .build();
    }

    private WorkspaceVO toWorkspaceVO(WorkspaceEntity entity) {
        return WorkspaceVO.builder()
                .id(entity.getId())
                .workspaceName(entity.getWorkspaceName())
                .description(entity.getDescription())
                .isDefault(Objects.equals(entity.getIsDefault(), 1))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MemberVO toMemberVO(TenantMemberEntity member, UserEntity user, RoleEntity role) {
        return MemberVO.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .username(user != null ? user.getUsername() : null)
                .nickname(user != null ? user.getNickname() : null)
                .email(user != null ? user.getEmail() : null)
                .roleCode(role != null ? role.getRoleCode() : null)
                .roleName(role != null ? role.getRoleName() : null)
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .lastLoginAt(user != null ? user.getLastLoginAt() : null)
                .build();
    }

    private String resolvePlanTypeLabel(String planType) {
        if (planType == null) {
            return "企业版";
        }
        return switch (planType) {
            case "free" -> "免费版";
            case "professional" -> "专业版";
            case "enterprise" -> "企业版";
            default -> planType;
        };
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException("密码至少 8 位，且需包含字母和数字");
        }
    }

    private String resolveNickname(String nickname, String email) {
        if (StringUtils.hasText(nickname)) {
            return nickname.trim();
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }

    private void requireTenantManagePermission() {
        permissionService.requireAnyPermission(
                StpUtil.getLoginIdAsLong(),
                requireTenantId(),
                "tenant:manage"
        );
    }

    private void requireMemberManagePermission() {
        permissionService.requireAnyPermission(
                StpUtil.getLoginIdAsLong(),
                requireTenantId(),
                "member:manage",
                "tenant:manage"
        );
    }
}
