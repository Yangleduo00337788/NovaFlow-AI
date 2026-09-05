package ai.novaflow.user.service;

import ai.novaflow.common.security.ResourceAclBypassChecker;
import ai.novaflow.common.security.RoleCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceAclBypassCheckerImpl implements ResourceAclBypassChecker {

    private final PermissionService permissionService;

    @Override
    public boolean bypassesResourceAcl(long userId, Long tenantId) {
        var role = permissionService.resolveRole(userId, tenantId);
        if (role == null || role.getRoleCode() == null) {
            return false;
        }
        String roleCode = role.getRoleCode();
        return RoleCodes.TENANT_OWNER.equals(roleCode)
                || RoleCodes.TENANT_ADMIN.equals(roleCode)
                || RoleCodes.PLATFORM_ADMIN.equals(roleCode);
    }
}
