package ai.novaflow.common.security;

import java.util.Set;

/**
 * 普通用户（门户）权限；企业管理员由库中授予除 platform:* 外的全部权限。
 */
public final class RolePermissionMatrix {

    public static final Set<String> USER = Set.of(
            PermissionCodes.PORTAL_ACCESS,
            PermissionCodes.AGENT_CHAT
    );

    /** @deprecated 使用 {@link #USER} */
    public static final Set<String> MEMBER = USER;

    private RolePermissionMatrix() {
    }
}
