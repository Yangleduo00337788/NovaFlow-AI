package ai.novaflow.user.service;

import ai.novaflow.user.entity.PermissionEntity;
import ai.novaflow.user.entity.RoleEntity;
import ai.novaflow.user.entity.RolePermissionEntity;
import ai.novaflow.user.entity.TenantMemberEntity;
import ai.novaflow.user.mapper.PermissionMapper;
import ai.novaflow.user.mapper.RoleMapper;
import ai.novaflow.user.mapper.RolePermissionMapper;
import ai.novaflow.user.mapper.TenantMemberMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
}
