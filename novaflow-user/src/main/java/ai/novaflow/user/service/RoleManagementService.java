package ai.novaflow.user.service;
import ai.novaflow.common.context.TenantContexts;
import ai.novaflow.common.security.CustomRolePermissionPolicy;
import ai.novaflow.common.security.PermissionCodes;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.RoleCodes;
import ai.novaflow.user.domain.dto.RoleSaveRequest;
import ai.novaflow.user.domain.vo.MemberVO;
import ai.novaflow.user.domain.vo.PermissionVO;
import ai.novaflow.user.domain.vo.RoleVO;
import ai.novaflow.user.entity.PermissionEntity;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.user.entity.RolePermissionEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.user.mapper.PermissionMapper;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.user.mapper.RolePermissionMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.user.mapper.UserMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private static final List<String> ROLE_DISPLAY_ORDER = List.of(
            RoleCodes.TENANT_OWNER,
            RoleCodes.TENANT_ADMIN,
            RoleCodes.DEVELOPER,
            RoleCodes.OPERATOR,
            RoleCodes.MEMBER,
            RoleCodes.VIEWER
    );

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;

    public List<RoleVO> listRoles() {
        requireRoleReadPermission();
        Long tenantId = TenantContexts.requireTenantId();
        Map<Long, Long> memberCountMap = countMembersByRole(tenantId);

        List<RoleEntity> systemRoles = roleMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", 0)
                        .eq("is_deleted", 0)
                        .in("role_code", RoleCodes.TENANT_SYSTEM_ROLES)
                        .orderBy("id", true)
        );
        List<RoleEntity> customRoles = roleMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .eq("is_system", 0)
                        .orderBy("created_at", true)
        );

        List<RoleVO> result = new ArrayList<>();
        systemRoles.stream()
                .sorted(Comparator.comparingInt(role -> {
                    int index = ROLE_DISPLAY_ORDER.indexOf(role.getRoleCode());
                    return index < 0 ? 99 : index;
                }))
                .map(role -> toRoleVO(role, memberCountMap.getOrDefault(role.getId(), 0L)))
                .forEach(result::add);
        customRoles.stream()
                .map(role -> toRoleVO(role, memberCountMap.getOrDefault(role.getId(), 0L)))
                .forEach(result::add);
        return result;
    }

    /** 组织邀请/成员编辑时可分配的角色（系统内置 + 本企业自定义） */
    public List<RoleVO> listAssignableRoles() {
        requireMemberManagePermission();
        Long tenantId = TenantContexts.requireTenantId();
        Map<Long, Long> memberCountMap = countMembersByRole(tenantId);

        List<RoleEntity> systemRoles = roleMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", 0)
                        .eq("is_deleted", 0)
                        .in("role_code", RoleCodes.ASSIGNABLE_TENANT_ROLES)
                        .orderBy("id", true)
        );
        List<RoleEntity> customRoles = roleMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
                        .eq("is_system", 0)
                        .orderBy("role_name", true)
        );

        List<RoleVO> result = new ArrayList<>();
        systemRoles.stream()
                .sorted(Comparator.comparingInt(role -> {
                    int index = ROLE_DISPLAY_ORDER.indexOf(role.getRoleCode());
                    return index < 0 ? 99 : index;
                }))
                .map(role -> toRoleVO(role, memberCountMap.getOrDefault(role.getId(), 0L)))
                .forEach(result::add);
        customRoles.stream()
                .map(role -> toRoleVO(role, memberCountMap.getOrDefault(role.getId(), 0L)))
                .forEach(result::add);
        return result;
    }

    public RoleVO getRole(Long roleId) {
        requireRoleReadPermission();
        Long tenantId = TenantContexts.requireTenantId();
        RoleEntity role = getAccessibleRoleOrThrow(roleId, tenantId);
        long memberCount = tenantMemberMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("role_id", roleId)
                        .eq("is_deleted", 0)
        );
        return toRoleVO(role, memberCount);
    }

    @Transactional
    public RoleVO createRole(RoleSaveRequest request) {
        requireRoleCreatePermission();
        Long tenantId = TenantContexts.requireTenantId();
        List<String> permissionCodes = normalizePermissionCodes(request.getPermissionCodes());
        validateCustomPermissions(permissionCodes);

        String roleName = request.getRoleName().trim();
        ensureUniqueRoleName(tenantId, roleName, null);

        LocalDateTime now = LocalDateTime.now();
        RoleEntity role = new RoleEntity();
        role.setTenantId(tenantId);
        role.setRoleCode(generateCustomRoleCode());
        role.setRoleName(roleName);
        role.setDescription(trimToNull(request.getDescription()));
        role.setIsSystem(0);
        role.setIsDeleted(0);
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        roleMapper.insert(role);
        replaceRolePermissions(role.getId(), permissionCodes);
        return toRoleVO(role, 0);
    }

    @Transactional
    public RoleVO updateRole(Long roleId, RoleSaveRequest request) {
        requireRoleUpdatePermission();
        Long tenantId = TenantContexts.requireTenantId();
        RoleEntity role = getCustomRoleOrThrow(roleId, tenantId);

        String roleName = request.getRoleName().trim();
        ensureUniqueRoleName(tenantId, roleName, roleId);

        List<String> permissionCodes = normalizePermissionCodes(request.getPermissionCodes());
        validateCustomPermissions(permissionCodes);

        role.setRoleName(roleName);
        role.setDescription(trimToNull(request.getDescription()));
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.update(role);
        replaceRolePermissions(role.getId(), permissionCodes);
        long memberCount = tenantMemberMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("role_id", roleId)
                        .eq("is_deleted", 0)
        );
        return toRoleVO(role, memberCount);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        requireRoleDeletePermission();
        Long tenantId = TenantContexts.requireTenantId();
        RoleEntity role = getCustomRoleOrThrow(roleId, tenantId);
        long memberCount = tenantMemberMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("role_id", roleId)
                        .eq("is_deleted", 0)
        );
        if (memberCount > 0) {
            throw new BusinessException("该角色仍有成员，无法删除");
        }
        role.setIsDeleted(1);
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.update(role);
        rolePermissionMapper.deleteByQuery(QueryWrapper.create().eq("role_id", role.getId()));
    }

    public List<MemberVO> listRoleMembers(Long roleId) {
        requireRoleReadPermission();
        Long tenantId = TenantContexts.requireTenantId();
        getAccessibleRoleOrThrow(roleId, tenantId);
        List<TenantMemberEntity> members = tenantMemberMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("role_id", roleId)
                        .eq("is_deleted", 0)
                        .orderBy("joined_at", false)
        );
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, UserEntity> userMap = userMapper.selectListByQuery(
                QueryWrapper.create().in(
                        "id",
                        members.stream().map(TenantMemberEntity::getUserId).distinct().toList()
                )
        ).stream().collect(Collectors.toMap(UserEntity::getId, user -> user, (a, b) -> a));

        RoleEntity role = roleMapper.selectOneById(roleId);
        return members.stream()
                .map(member -> MemberVO.builder()
                        .id(member.getId())
                        .userId(member.getUserId())
                        .username(userMap.containsKey(member.getUserId()) ? userMap.get(member.getUserId()).getUsername() : null)
                        .nickname(userMap.containsKey(member.getUserId()) ? userMap.get(member.getUserId()).getNickname() : null)
                        .email(userMap.containsKey(member.getUserId()) ? userMap.get(member.getUserId()).getEmail() : null)
                        .roleCode(role != null ? role.getRoleCode() : null)
                        .roleName(role != null ? role.getRoleName() : null)
                        .status(member.getStatus())
                        .joinedAt(member.getJoinedAt())
                        .lastLoginAt(userMap.containsKey(member.getUserId()) ? userMap.get(member.getUserId()).getLastLoginAt() : null)
                        .build())
                .toList();
    }

    public List<PermissionVO> listPermissions() {
        requireRoleReadPermission();
        return permissionMapper.selectListByQuery(
                QueryWrapper.create().orderBy("module", true).orderBy("id", true)
        ).stream()
                .filter(perm -> CustomRolePermissionPolicy.isAllowed(perm.getPermissionCode()))
                .map(this::toPermissionVO)
                .toList();
    }

    public Map<String, List<PermissionVO>> listPermissionsGrouped() {
        return listPermissions().stream()
                .collect(Collectors.groupingBy(
                        PermissionVO::getModule,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<Long, Long> countMembersByRole(Long tenantId) {
        List<TenantMemberEntity> members = tenantMemberMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        return members.stream()
                .collect(Collectors.groupingBy(TenantMemberEntity::getRoleId, Collectors.counting()));
    }

    private RoleEntity getAccessibleRoleOrThrow(Long roleId, Long tenantId) {
        RoleEntity role = roleMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", roleId)
                        .eq("is_deleted", 0)
        );
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (Objects.equals(role.getTenantId(), 0L)) {
            if (!RoleCodes.TENANT_SYSTEM_ROLES.contains(role.getRoleCode())) {
                throw new BusinessException("角色不存在");
            }
            return role;
        }
        if (!Objects.equals(role.getTenantId(), tenantId)) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    private RoleEntity getCustomRoleOrThrow(Long roleId, Long tenantId) {
        RoleEntity role = roleMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", roleId)
                        .eq("tenant_id", tenantId)
                        .eq("is_system", 0)
                        .eq("is_deleted", 0)
        );
        if (role == null) {
            throw new BusinessException("自定义角色不存在");
        }
        return role;
    }

    private RoleVO toRoleVO(RoleEntity role, long memberCount) {
        boolean isSystem = Objects.equals(role.getIsSystem(), 1) || Objects.equals(role.getTenantId(), 0L);
        return RoleVO.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .description(isSystem ? resolveRoleDescription(role.getRoleCode()) : role.getDescription())
                .isSystem(isSystem)
                .memberCount((int) memberCount)
                .permissionCodes(permissionService.getPermissionCodesByRoleId(role.getId()))
                .build();
    }

    private String resolveRoleDescription(String roleCode) {
        return switch (roleCode) {
            case RoleCodes.TENANT_OWNER -> "企业最高管理员，可删除企业与转移所有权";
            case RoleCodes.TENANT_ADMIN -> "管理本企业资源、成员与权限，不含跨租户总控";
            case RoleCodes.DEVELOPER -> "创建和编辑 Agent、工作流、知识库等 AI 资源";
            case RoleCodes.OPERATOR -> "发布、运行与监控 AI 应用，不改核心配置";
            case RoleCodes.MEMBER -> "使用已发布的 AI 应用与工作台";
            case RoleCodes.VIEWER -> "只读查看企业 AI 资源与运行数据";
            default -> "";
        };
    }

    private PermissionVO toPermissionVO(PermissionEntity entity) {
        return PermissionVO.builder()
                .id(entity.getId())
                .permissionCode(entity.getPermissionCode())
                .permissionName(entity.getPermissionName())
                .module(entity.getModule())
                .build();
    }

    private void replaceRolePermissions(Long roleId, List<String> permissionCodes) {
        rolePermissionMapper.deleteByQuery(QueryWrapper.create().eq("role_id", roleId));
        Map<String, Long> permissionIdMap = permissionMapper.selectListByQuery(
                QueryWrapper.create().in("permission_code", permissionCodes)
        ).stream().collect(Collectors.toMap(PermissionEntity::getPermissionCode, PermissionEntity::getId, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();
        for (String code : permissionCodes) {
            Long permissionId = permissionIdMap.get(code);
            if (permissionId == null) {
                throw new BusinessException("未知权限: " + code);
            }
            RolePermissionEntity link = new RolePermissionEntity();
            link.setRoleId(roleId);
            link.setPermissionId(permissionId);
            link.setCreatedAt(now);
            rolePermissionMapper.insert(link);
        }
    }

    private List<String> normalizePermissionCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new BusinessException("至少选择一个权限");
        }
        return codes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .sorted()
                .toList();
    }

    private void validateCustomPermissions(List<String> permissionCodes) {
        for (String code : permissionCodes) {
            if (!CustomRolePermissionPolicy.isAllowed(code)) {
                throw new BusinessException("自定义角色不可包含权限: " + code);
            }
        }
        Set<String> known = permissionMapper.selectListByQuery(QueryWrapper.create())
                .stream()
                .map(PermissionEntity::getPermissionCode)
                .collect(Collectors.toSet());
        for (String code : permissionCodes) {
            if (!known.contains(code)) {
                throw new BusinessException("未知权限: " + code);
            }
        }
    }

    private void ensureUniqueRoleName(Long tenantId, String roleName, Long excludeRoleId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("role_name", roleName)
                .eq("is_deleted", 0);
        if (excludeRoleId != null) {
            query.ne("id", excludeRoleId);
        }
        if (roleMapper.selectCountByQuery(query) > 0) {
            throw new BusinessException("角色名称已存在");
        }
    }

    private String generateCustomRoleCode() {
        return RoleCodes.CUSTOM_ROLE_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void requireRoleReadPermission() {
        permissionService.requireAnyPermission(
                StpUtil.getLoginIdAsLong(),
                TenantContexts.requireTenantId(),
                PermissionCodes.ROLE_READ,
                PermissionCodes.MEMBER_MANAGE,
                PermissionCodes.TENANT_MANAGE
        );
    }

    private void requireRoleCreatePermission() {
        permissionService.requireAnyPermission(
                StpUtil.getLoginIdAsLong(),
                TenantContexts.requireTenantId(),
                PermissionCodes.ROLE_CREATE,
                PermissionCodes.TENANT_MANAGE
        );
    }

    private void requireRoleUpdatePermission() {
        permissionService.requireAnyPermission(
                StpUtil.getLoginIdAsLong(),
                TenantContexts.requireTenantId(),
                PermissionCodes.ROLE_UPDATE,
                PermissionCodes.TENANT_MANAGE
        );
    }

    private void requireRoleDeletePermission() {
        permissionService.requireAnyPermission(
                StpUtil.getLoginIdAsLong(),
                TenantContexts.requireTenantId(),
                PermissionCodes.ROLE_DELETE,
                PermissionCodes.TENANT_MANAGE
        );
    }

    private void requireMemberManagePermission() {
        permissionService.requireAnyPermission(
                StpUtil.getLoginIdAsLong(),
                TenantContexts.requireTenantId(),
                PermissionCodes.MEMBER_MANAGE,
                PermissionCodes.TENANT_MANAGE
        );
    }
}
