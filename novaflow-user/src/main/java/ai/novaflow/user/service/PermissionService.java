package ai.novaflow.user.service;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.user.entity.PermissionEntity;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.user.entity.RolePermissionEntity;
import ai.novaflow.tenant.entity.TenantMemberEntity;
import ai.novaflow.user.mapper.PermissionMapper;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.user.mapper.RolePermissionMapper;
import ai.novaflow.tenant.mapper.TenantMemberMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        List<RolePermissionEntity> links = rolePermissionMapper.selectListByQuery(
                QueryWrapper.create().eq("role_id", role.getId())
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

    public RoleEntity resolveRole(long userId, Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        TenantMemberEntity member = tenantMemberMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("user_id", userId)
                        .eq("tenant_id", tenantId)
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
        RoleEntity role = resolveRole(userId, tenantId);
        if (role != null && isAdminRole(role.getRoleCode())) {
            return;
        }
        List<String> granted = getPermissionCodes(userId, tenantId);
        boolean matched = Arrays.stream(permissionCodes).anyMatch(granted::contains);
        if (!matched) {
            throw new BusinessException("无操作权限");
        }
    }

    public boolean isAdminRole(String roleCode) {
        return "tenant_admin".equals(roleCode) || "super_admin".equals(roleCode);
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
