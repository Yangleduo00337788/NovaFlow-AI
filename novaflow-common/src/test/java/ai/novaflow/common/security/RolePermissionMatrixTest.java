package ai.novaflow.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionMatrixTest {

    @Test
    void portalUserCanChatAndOpenPortalOnly() {
        assertEquals(2, RolePermissionMatrix.USER.size());
        assertTrue(RolePermissionMatrix.USER.contains(PermissionCodes.PORTAL_ACCESS));
        assertTrue(RolePermissionMatrix.USER.contains(PermissionCodes.AGENT_CHAT));
        assertFalse(RolePermissionMatrix.USER.contains(PermissionCodes.AGENT_CREATE));
        assertFalse(RolePermissionMatrix.USER.contains(PermissionCodes.DASHBOARD_VIEW));
        assertFalse(RolePermissionMatrix.USER.contains(PermissionCodes.PLATFORM_MANAGE));
    }

    @Test
    void memberAliasMatchesUserMatrix() {
        assertEquals(RolePermissionMatrix.USER, RolePermissionMatrix.MEMBER);
    }

    @Test
    void systemRolesArePlatformTenantAdminAndUser() {
        assertTrue(RoleCodes.ALL_SYSTEM_ROLES.contains(RoleCodes.PLATFORM_ADMIN));
        assertTrue(RoleCodes.ALL_SYSTEM_ROLES.contains(RoleCodes.TENANT_ADMIN));
        assertTrue(RoleCodes.ALL_SYSTEM_ROLES.contains(RoleCodes.USER));
        assertEquals(3, RoleCodes.ALL_SYSTEM_ROLES.size());
        assertFalse(RoleCodes.ASSIGNABLE_TENANT_ROLES.contains(RoleCodes.PLATFORM_ADMIN));
    }
}
