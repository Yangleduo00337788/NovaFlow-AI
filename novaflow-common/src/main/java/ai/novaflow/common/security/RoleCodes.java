package ai.novaflow.common.security;

import java.util.Set;

/**
 * 系统内置角色码（平台 1 + 租户 6）。
 */
public final class RoleCodes {

    public static final String PLATFORM_ADMIN = "super_admin";

    public static final String TENANT_OWNER = "tenant_owner";
    public static final String TENANT_ADMIN = "tenant_admin";
    public static final String DEVELOPER = "developer";
    public static final String OPERATOR = "operator";
    public static final String MEMBER = "member";
    public static final String VIEWER = "viewer";

    /** 组织邀请可分配的企业角色（不含平台超管与企业 Owner） */
    public static final Set<String> ASSIGNABLE_TENANT_ROLES = Set.of(
            TENANT_ADMIN, DEVELOPER, OPERATOR, MEMBER, VIEWER
    );

    /** 不可在组织内被降级/禁用/移除的保护角色 */
    public static final Set<String> PROTECTED_MEMBER_ROLES = Set.of(
            PLATFORM_ADMIN, TENANT_OWNER
    );

    public static final Set<String> ALL_SYSTEM_ROLES = Set.of(
            PLATFORM_ADMIN,
            TENANT_OWNER,
            TENANT_ADMIN,
            DEVELOPER,
            OPERATOR,
            MEMBER,
            VIEWER
    );

    private RoleCodes() {
    }

    public static boolean isPlatformAdmin(String roleCode) {
        return PLATFORM_ADMIN.equals(roleCode);
    }

    public static boolean isProtectedMemberRole(String roleCode) {
        return roleCode != null && PROTECTED_MEMBER_ROLES.contains(roleCode);
    }
}
