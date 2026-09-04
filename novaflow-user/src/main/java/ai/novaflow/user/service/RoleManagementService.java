package ai.novaflow.user.service;
import ai.novaflow.common.context.TenantContexts;
import ai.novaflow.common.security.PermissionCodes;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.RoleCodes;
import ai.novaflow.user.domain.vo.MemberVO;
import ai.novaflow.user.domain.vo.PermissionVO;
import ai.novaflow.user.domain.vo.RoleVO;
import ai.novaflow.user.entity.PermissionEntity;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.user.mapper.PermissionMapper;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.user.mapper.UserMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            RoleCodes.VIEWER,
            RoleCodes.PLATFORM_ADMIN
    );

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;

    public List<RoleVO> listRoles() {
        requireMemberManagePermission();
        Long tenantId = TenantContexts.requireTenantId();
        List<RoleEntity> roles = roleMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", 0)
                        .eq("is_deleted", 0)
                        .in("role_code", RoleCodes.ALL_SYSTEM_ROLES)
                        .orderBy("id", true)
        );
        Map<Long, Long> memberCountMap = countMembersByRole(tenantId);
        return roles.stream()
                .sorted(Comparator.comparingInt(role -> {
                    int index = ROLE_DISPLAY_ORDER.indexOf(role.getRoleCode());
                    return index < 0 ? 99 : index;
                }))
                .map(role -> toRoleVO(role, memberCountMap.getOrDefault(role.getId(), 0L)))
                .toList();
    }

    public RoleVO getRole(Long roleId) {
        requireMemberManagePermission();
        RoleEntity role = getSystemRoleOrThrow(roleId);
        Long tenantId = TenantContexts.requireTenantId();
        long memberCount = tenantMemberMapper.selectCountByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", tenantId)
                        .eq("role_id", roleId)
                        .eq("is_deleted", 0)
        );
        return toRoleVO(role, memberCount);
    }

    public List<MemberVO> listRoleMembers(Long roleId) {
        requireMemberManagePermission();
        getSystemRoleOrThrow(roleId);
        Long tenantId = TenantContexts.requireTenantId();
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
        requireMemberManagePermission();
        return permissionMapper.selectListByQuery(
                QueryWrapper.create().orderBy("module", true).orderBy("id", true)
        ).stream().map(this::toPermissionVO).toList();
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

    private RoleEntity getSystemRoleOrThrow(Long roleId) {
        RoleEntity role = roleMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", roleId)
                        .eq("tenant_id", 0)
                        .eq("is_deleted", 0)
        );
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    private RoleVO toRoleVO(RoleEntity role, long memberCount) {
        return RoleVO.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .description(resolveRoleDescription(role.getRoleCode()))
                .isSystem(Objects.equals(role.getIsSystem(), 1))
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
            case RoleCodes.PLATFORM_ADMIN -> "平台级超级管理员，含企业管理员全部权限，并管理所有租户";
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


    private void requireMemberManagePermission() {
        permissionService.requireAnyPermission(
                StpUtil.getLoginIdAsLong(),
                TenantContexts.requireTenantId(),
                PermissionCodes.MEMBER_MANAGE,
                PermissionCodes.TENANT_MANAGE
        );
    }
}
