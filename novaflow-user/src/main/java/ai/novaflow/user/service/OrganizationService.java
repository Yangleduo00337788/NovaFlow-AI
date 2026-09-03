package ai.novaflow.user.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.security.RoleCodes;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.util.PageQueryUtils;
import ai.novaflow.user.domain.dto.MemberInviteRequest;
import ai.novaflow.user.domain.dto.MemberUpdateRequest;
import ai.novaflow.user.domain.dto.TenantUpdateRequest;
import ai.novaflow.user.domain.dto.WorkspaceSaveRequest;
import ai.novaflow.user.domain.vo.MemberVO;
import ai.novaflow.user.domain.vo.TenantPlanSummaryVO;
import ai.novaflow.user.domain.vo.TenantVO;
import ai.novaflow.user.domain.vo.WorkspaceVO;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.tenant.entity.DepartmentEntity;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.tenant.entity.WorkspaceEntity;
import ai.novaflow.tenant.mapper.DepartmentMapper;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.tenant.mapper.WorkspaceMapper;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.user.mapper.UserMapper;
import ai.novaflow.common.application.ApplicationWorkspaceChecker;
import ai.novaflow.model.mapper.TokenUsageMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");
    private static final Set<String> ASSIGNABLE_ROLE_CODES = RoleCodes.ASSIGNABLE_TENANT_ROLES;

    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final DepartmentMapper departmentMapper;
    private final WorkspaceMapper workspaceMapper;
    private final ApplicationWorkspaceChecker applicationWorkspaceChecker;
    private final UserMapper userMapper;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    private final TokenUsageMapper tokenUsageMapper;
    private final AuditLogService auditLogService;

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
        YearMonth current = YearMonth.now();
        LocalDate monthStart = current.atDay(1);
        LocalDate monthEnd = current.atEndOfMonth();
        long usedTokens = safeLong(tokenUsageMapper.sumTokensBetween(tenantId, monthStart, monthEnd));
        long monthlyTokenQuota = tenant.getMonthlyTokenQuota() != null ? tenant.getMonthlyTokenQuota() : 0L;
        return TenantPlanSummaryVO.builder()
                .planType(tenant.getPlanType())
                .planTypeLabel(resolvePlanTypeLabel(tenant.getPlanType()))
                .expireAt(tenant.getExpireAt())
                .memberCount(memberCount)
                .maxMembers(maxMembers)
                .usedPercent(usedPercent)
                .monthlyTokenQuota(monthlyTokenQuota > 0 ? monthlyTokenQuota : null)
                .usedTokens(usedTokens)
                .tokenUsedPercent(calcPercent(usedTokens, monthlyTokenQuota))
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
        auditLogService.record("tenant.update", "tenant", tenantId, "更新企业信息: " + tenant.getTenantName());
        return toTenantVO(tenant, countActiveMembers(tenantId));
    }

    @Transactional
    public void deleteOwnedTenant() {
        Long tenantId = requireTenantId();
        permissionService.requireAnyPermission(StpUtil.getLoginIdAsLong(), tenantId, "tenant:delete");
        TenantEntity tenant = getTenantOrThrow(tenantId);
        if ("demo".equalsIgnoreCase(tenant.getTenantCode())) {
            throw new BusinessException("演示企业不可删除");
        }
        tenant.setIsDeleted(1);
        tenant.setStatus(0);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantMapper.update(tenant);
        kickTenantSessions(tenantId);
        auditLogService.record("tenant.delete", "tenant", tenantId, "企业所有者删除企业: " + tenant.getTenantName());
    }

    @Transactional
    public void transferOwnership(Long targetMemberId) {
        Long tenantId = requireTenantId();
        long currentUserId = StpUtil.getLoginIdAsLong();
        RoleEntity currentRole = permissionService.resolveRole(currentUserId, tenantId);
        if (currentRole == null || !RoleCodes.TENANT_OWNER.equals(currentRole.getRoleCode())) {
            throw new BusinessException("仅企业所有者可转移所有权");
        }

        TenantMemberEntity targetMember = getMemberOrThrow(targetMemberId, tenantId);
        if (Objects.equals(targetMember.getUserId(), currentUserId)) {
            throw new BusinessException("不能转移给自己");
        }
        if (targetMember.getStatus() == null || targetMember.getStatus() != 1) {
            throw new BusinessException("目标成员不可用");
        }
        RoleEntity targetRole = permissionService.resolveRole(targetMember.getUserId(), tenantId);
        if (targetRole != null && RoleCodes.isProtectedMemberRole(targetRole.getRoleCode())) {
            throw new BusinessException("不能将所有权转移给该成员");
        }

        TenantMemberEntity currentMember = tenantMemberMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("user_id", currentUserId)
                        .eq("is_deleted", 0)
        );
        if (currentMember == null) {
            throw new BusinessException("当前成员记录不存在");
        }

        RoleEntity ownerRole = permissionService.requireSystemRole(RoleCodes.TENANT_OWNER);
        RoleEntity adminRole = permissionService.requireSystemRole(RoleCodes.TENANT_ADMIN);
        LocalDateTime now = LocalDateTime.now();
        currentMember.setRoleId(adminRole.getId());
        currentMember.setUpdatedAt(now);
        targetMember.setRoleId(ownerRole.getId());
        targetMember.setUpdatedAt(now);
        tenantMemberMapper.update(currentMember);
        tenantMemberMapper.update(targetMember);

        UserEntity targetUser = userMapper.selectOneById(targetMember.getUserId());
        auditLogService.record(
                "tenant.transfer_owner",
                "tenant",
                tenantId,
                "转移所有权至: " + (targetUser != null ? targetUser.getEmail() : targetMember.getUserId()));
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
        auditLogService.record("workspace.create", "workspace", entity.getId(), "创建工作空间: " + entity.getWorkspaceName());
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
        auditLogService.record("workspace.update", "workspace", entity.getId(), "更新工作空间: " + entity.getWorkspaceName());
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
        long appCount = applicationWorkspaceChecker.countByWorkspace(id);
        if (appCount > 0) {
            throw new BusinessException("工作空间下仍有应用，无法删除");
        }
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        workspaceMapper.update(entity);
        auditLogService.record("workspace.delete", "workspace", entity.getId(), "删除工作空间: " + entity.getWorkspaceName());
    }

    public PageResult<MemberVO> pageMembers(int page, int pageSize, String keyword, Long departmentId) {
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);
        Long tenantId = requireTenantId();
        requireMemberManagePermission();

        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0);
        if (departmentId != null && departmentId > 0) {
            query.eq("department_id", departmentId);
        }
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
        Map<Long, DepartmentEntity> departmentMap = loadDepartments(result.getRecords());
        List<MemberVO> list = result.getRecords().stream()
                .map(member -> toMemberVO(
                        member,
                        userMap.get(member.getUserId()),
                        roleMap.get(member.getRoleId()),
                        departmentMap.get(member.getDepartmentId())))
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

        RoleEntity role = requireAssignableRole(request.getRoleCode());
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
        member.setDepartmentId(resolveDepartmentId(request.getDepartmentId(), tenantId));
        member.setStatus(1);
        member.setJoinedAt(now);
        member.setIsDeleted(0);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        tenantMemberMapper.insert(member);
        auditLogService.record(
                "member.invite",
                "member",
                member.getId(),
                "邀请成员: " + email + "，角色 " + role.getRoleCode());
        return toMemberVO(member, user, role, loadDepartment(member.getDepartmentId(), tenantId));
    }

    @Transactional
    public MemberVO updateMember(Long memberId, MemberUpdateRequest request) {
        Long tenantId = requireTenantId();
        long currentUserId = StpUtil.getLoginIdAsLong();
        requireMemberManagePermission();

        TenantMemberEntity member = getMemberOrThrow(memberId, tenantId);
        UserEntity user = userMapper.selectOneById(member.getUserId());
        RoleEntity currentRole = permissionService.resolveRole(member.getUserId(), tenantId);
        ensureNotProtectedMemberRole(currentRole);

        if (request.getRoleCode() != null) {
            RoleEntity newRole = requireAssignableRole(request.getRoleCode());
            if (member.getUserId() == currentUserId && !request.getRoleCode().equals(currentRole.getRoleCode())) {
                throw new BusinessException("不能修改自己的角色");
            }
            if (currentRole != null
                    && isTenantGovernanceRole(currentRole.getRoleCode())
                    && !isTenantGovernanceRole(request.getRoleCode())) {
                ensureAnotherTenantGovernorExists(tenantId, member.getUserId());
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
                    && isTenantGovernanceRole(currentRole.getRoleCode())) {
                ensureAnotherTenantGovernorExists(tenantId, member.getUserId());
            }
            member.setStatus(request.getStatus());
            if (user != null) {
                user.setStatus(request.getStatus());
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.update(user);
            }
            if (request.getStatus() != 1) {
                StpUtil.logout(member.getUserId());
            }
        }

        if (request.getDepartmentId() != null) {
            Long departmentId = resolveDepartmentId(request.getDepartmentId(), tenantId);
            member.setDepartmentId(departmentId);
            tenantMemberMapper.updateDepartmentId(member.getId(), tenantId, departmentId);
        }

        member.setUpdatedAt(LocalDateTime.now());
        tenantMemberMapper.update(member);
        String detail = "更新成员: " + (user != null ? user.getEmail() : member.getUserId());
        if (request.getRoleCode() != null) {
            detail += "，角色 " + request.getRoleCode();
        }
        if (request.getStatus() != null) {
            detail += "，状态 " + request.getStatus();
        }
        if (request.getDepartmentId() != null) {
            detail += "，部门 " + (request.getDepartmentId() <= 0 ? "未分配" : request.getDepartmentId());
        }
        auditLogService.record("member.update", "member", member.getId(), detail);
        return toMemberVO(member, user, currentRole, loadDepartment(member.getDepartmentId(), tenantId));
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
        ensureNotProtectedMemberRole(role);
        if (role != null && isTenantGovernanceRole(role.getRoleCode())) {
            ensureAnotherTenantGovernorExists(tenantId, member.getUserId());
        }

        member.setIsDeleted(1);
        member.setUpdatedAt(LocalDateTime.now());
        tenantMemberMapper.update(member);
        UserEntity removedUser = userMapper.selectOneById(member.getUserId());
        auditLogService.record(
                "member.remove",
                "member",
                member.getId(),
                "移除成员: " + (removedUser != null ? removedUser.getEmail() : member.getUserId()));
    }

    private boolean isTenantGovernanceRole(String roleCode) {
        return RoleCodes.TENANT_OWNER.equals(roleCode) || RoleCodes.TENANT_ADMIN.equals(roleCode);
    }

    private void ensureAnotherTenantGovernorExists(Long tenantId, Long excludeUserId) {
        RoleEntity ownerRole = permissionService.requireSystemRole(RoleCodes.TENANT_OWNER);
        RoleEntity adminRole = permissionService.requireSystemRole(RoleCodes.TENANT_ADMIN);
        long count = tenantMemberMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .in("role_id", List.of(ownerRole.getId(), adminRole.getId()))
                        .eq("status", 1)
                        .eq("is_deleted", 0)
                        .ne("user_id", excludeUserId)
        );
        if (count == 0) {
            throw new BusinessException("企业至少保留一名 Owner 或管理员");
        }
    }

    private void ensureAnotherTenantAdminExists(Long tenantId, Long excludeUserId) {
        ensureAnotherTenantGovernorExists(tenantId, excludeUserId);
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

    private Map<Long, DepartmentEntity> loadDepartments(List<TenantMemberEntity> members) {
        List<Long> departmentIds = members.stream()
                .map(TenantMemberEntity::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (departmentIds.isEmpty()) {
            return Map.of();
        }
        return departmentMapper.selectListByQuery(QueryWrapper.create().in("id", departmentIds).eq("is_deleted", 0))
                .stream()
                .collect(Collectors.toMap(DepartmentEntity::getId, item -> item, (a, b) -> a));
    }

    private DepartmentEntity loadDepartment(Long departmentId, Long tenantId) {
        if (departmentId == null) {
            return null;
        }
        return departmentMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", departmentId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .limit(1));
    }

    private Long resolveDepartmentId(Long departmentId, Long tenantId) {
        if (departmentId == null || departmentId <= 0) {
            return null;
        }
        DepartmentEntity department = loadDepartment(departmentId, tenantId);
        if (department == null) {
            throw new BusinessException("部门不存在");
        }
        return department.getId();
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

    private MemberVO toMemberVO(TenantMemberEntity member, UserEntity user, RoleEntity role, DepartmentEntity department) {
        return MemberVO.builder()
                .id(member.getId())
                .userId(member.getUserId())
                .username(user != null ? user.getUsername() : null)
                .nickname(user != null ? user.getNickname() : null)
                .email(user != null ? user.getEmail() : null)
                .roleCode(role != null ? role.getRoleCode() : null)
                .roleName(role != null ? role.getRoleName() : null)
                .departmentId(member.getDepartmentId())
                .departmentName(department != null ? department.getDeptName() : null)
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .lastLoginAt(user != null ? user.getLastLoginAt() : null)
                .build();
    }

    private Integer calcPercent(long used, long limit) {
        if (limit <= 0) {
            return null;
        }
        return (int) Math.min(100, Math.round(used * 100.0 / limit));
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private String resolvePlanTypeLabel(String planType) {
        if (planType == null) {
            return "企业版";
        }
        return switch (planType) {
            case "personal" -> "个人版";
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

    private RoleEntity requireAssignableRole(String roleCode) {
        if (roleCode == null || !ASSIGNABLE_ROLE_CODES.contains(roleCode)) {
            throw new BusinessException("不能分配该角色");
        }
        return permissionService.requireSystemRole(roleCode);
    }

    private void ensureNotProtectedMemberRole(RoleEntity role) {
        if (role != null && RoleCodes.isProtectedMemberRole(role.getRoleCode())) {
            throw new BusinessException("不能对企业内的受保护角色进行该操作");
        }
    }

    private void requireMemberManagePermission() {
        permissionService.requireAnyPermission(
                StpUtil.getLoginIdAsLong(),
                requireTenantId(),
                "member:manage",
                "tenant:manage"
        );
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
}
