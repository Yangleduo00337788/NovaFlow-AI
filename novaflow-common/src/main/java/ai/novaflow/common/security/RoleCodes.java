package ai.novaflow.common.security;

import java.util.Set;

/**
 * 系统内置角色：平台管理员、企业管理员、普通用户。
 */
public final class RoleCodes {

    public static final String PLATFORM_ADMIN = "super_admin";
    public static final String TENANT_ADMIN = "tenant_admin";
    /** 普通用户（门户）；角色码沿用历史 {@code member} */
    public static final String USER = "member";

    /** @deprecated 使用 {@link #USER} */
    public static final String MEMBER = USER;

    public static final Set<String> TENANT_SYSTEM_ROLES = Set.of(TENANT_ADMIN, USER);

    public static final String CUSTOM_ROLE_PREFIX = "custom_";
    public static final Set<String> ASSIGNABLE_TENANT_ROLES = Set.of(TENANT_ADMIN, USER);

    public static final Set<String> ALL_SYSTEM_ROLES = Set.of(
            PLATFORM_ADMIN,
            TENANT_ADMIN,
            USER
    );

    private RoleCodes() {
    }

    public static boolean isPlatformAdmin(String roleCode) {
        return PLATFORM_ADMIN.equals(roleCode);
    }

    public static boolean isProtectedMemberRole(String roleCode) {
        return false;
    }

    public static boolean isCustomRole(String roleCode) {
        return roleCode != null && roleCode.startsWith(CUSTOM_ROLE_PREFIX);
    }

    public static boolean isTenantAdmin(String roleCode) {
        return TENANT_ADMIN.equals(roleCode);
    }

    public static boolean isPortalUser(String roleCode) {
        return USER.equals(roleCode);
    }
}
