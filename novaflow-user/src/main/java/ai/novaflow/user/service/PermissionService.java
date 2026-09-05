package ai.novaflow.user.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.AccountTypes;
import ai.novaflow.common.security.PermissionCodes;
import ai.novaflow.common.security.RoleCodes;
import ai.novaflow.user.entity.PermissionEntity;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.user.entity.RolePermissionEntity;
import ai.novaflow.user.entity.UserEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.user.mapper.PermissionMapper;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.user.mapper.RolePermissionMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import ai.novaflow.user.mapper.UserMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final TenantMemberMapper tenantMemberMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;

    public List<String> getRoleCodes(long userId, Long tenantId) {
        RoleEntity role = resolveRole(userId, tenantId);
        if (role == null || role.getRoleCode() == null) {
            return Collections.emptyList();
        }
        return List.of(role.getRoleCode());
    }

    public List<String> getPermissionCodes(long userId, Long tenantId) {
        RoleEntity role = resolveRole(userId, tenantId);
        if (role == null) {
            return Collections.emptyList();
        }
        return getPermissionCodesByRoleId(role.getId());
    }

    public RoleEntity resolvePlatformRole(UserEntity user) {
        String roleCode = StringUtils.hasText(user.getPlatformRoleCode())
                ? user.getPlatformRoleCode().trim()
                : RoleCodes.PLATFORM_ADMIN;
        return requireSystemRole(roleCode);
    }

    public RoleEntity resolveRole(long userId, Long tenantId) {
        UserEntity user = userMapper.selectOneById(userId);
        if (user != null && AccountTypes.isPlatform(user.getAccountType())) {
            return resolvePlatformRole(user);
        }
        if (tenantId == null) {
            return null;
        }
        TenantMemberEntity member = tenantMemberMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("user_id", userId)
                        .eq("tenant_id", tenantId)
                        .eq("status", 1)
                        .eq("is_deleted", 0)
                        .limit(1)
        );
        if (member == null) {
            return null;
        }
        return roleMapper.selectOneById(member.getRoleId());
    }

    public void requireAnyPermission(long userId, Long tenantId, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return;
        }
        List<String> granted = getPermissionCodes(userId, tenantId);
        boolean matched = Arrays.stream(permissionCodes).anyMatch(granted::contains);
        if (!matched) {
            throw new BusinessException("无操作权限");
        }
    }

    public void requireSuperAdmin(long userId, Long tenantId) {
        UserEntity user = userMapper.selectOneById(userId);
        if (user != null && AccountTypes.isPlatform(user.getAccountType())) {
            RoleEntity role = resolvePlatformRole(user);
            if (getPermissionCodesByRoleId(role.getId()).contains(PermissionCodes.PLATFORM_MANAGE)) {
                return;
            }
        }
        throw new BusinessException("需要平台超级管理员权限");
    }

    public RoleEntity requireSystemRole(String roleCode) {
        RoleEntity role = roleMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("tenant_id", 0)
                        .eq("role_code", roleCode)
                        .eq("is_deleted", 0)
        );
        if (role == null) {
            throw new BusinessException("角色不存在: " + roleCode);
        }
        return role;
    }

    public RoleEntity requireAssignableRole(Long tenantId, String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new BusinessException("不能分配该角色");
        }
        if (RoleCodes.ASSIGNABLE_TENANT_ROLES.contains(roleCode)) {
            return requireSystemRole(roleCode);
        }
        if (RoleCodes.isCustomRole(roleCode)) {
            RoleEntity custom = roleMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .eq("tenant_id", tenantId)
                            .eq("role_code", roleCode)
                            .eq("is_system", 0)
                            .eq("is_deleted", 0)
            );
            if (custom == null) {
                throw new BusinessException("不能分配该角色");
            }
            return custom;
        }
        throw new BusinessException("不能分配该角色");
    }

    public Map<Long, RoleEntity> getRolesByIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        return roleMapper.selectListByQuery(QueryWrapper.create().in("id", roleIds))
                .stream()
                .collect(Collectors.toMap(RoleEntity::getId, Function.identity(), (a, b) -> a));
    }

    public List<String> getPermissionCodesByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        List<RolePermissionEntity> links = rolePermissionMapper.selectListByQuery(
                QueryWrapper.create().eq("role_id", roleId)
        );
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> permissionIds = links.stream().map(RolePermissionEntity::getPermissionId).toList();
        return permissionMapper.selectListByQuery(
                QueryWrapper.create().in("id", permissionIds)
        ).stream()
                .map(PermissionEntity::getPermissionCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }
}
