package ai.novaflow.common.security;

import java.util.Set;

/**
 * 自定义角色可分配的权限边界（禁止平台级与 Owner 专属权限）。
 */
public final class CustomRolePermissionPolicy {

    /** 自定义角色不可包含的权限码 */
    public static final Set<String> FORBIDDEN = Set.of(
            PermissionCodes.PLATFORM_MANAGE,
            PermissionCodes.TENANT_DELETE,
            PermissionCodes.TENANT_TRANSFER
    );

    private CustomRolePermissionPolicy() {
    }

    public static boolean isAllowed(String permissionCode) {
        return permissionCode != null && !FORBIDDEN.contains(permissionCode);
    }

    public static void validateAll(java.util.Collection<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            throw new IllegalArgumentException("至少选择一个权限");
        }
        for (String code : permissionCodes) {
            if (!isAllowed(code)) {
                throw new IllegalArgumentException("自定义角色不可包含权限: " + code);
            }
        }
    }
}
