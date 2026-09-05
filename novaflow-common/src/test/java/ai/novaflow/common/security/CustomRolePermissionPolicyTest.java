package ai.novaflow.common.security;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomRolePermissionPolicyTest {

    @Test
    void forbidsPlatformAndOwnerPermissions() {
        assertFalse(CustomRolePermissionPolicy.isAllowed(PermissionCodes.PLATFORM_MANAGE));
        assertFalse(CustomRolePermissionPolicy.isAllowed(PermissionCodes.TENANT_DELETE));
        assertFalse(CustomRolePermissionPolicy.isAllowed(PermissionCodes.TENANT_TRANSFER));
    }

    @Test
    void allowsRegularPermissions() {
        assertTrue(CustomRolePermissionPolicy.isAllowed(PermissionCodes.AGENT_READ));
        assertTrue(CustomRolePermissionPolicy.isAllowed(PermissionCodes.ROLE_CREATE));
    }

    @Test
    void validateAllRejectsForbidden() {
        assertThrows(IllegalArgumentException.class, () -> CustomRolePermissionPolicy.validateAll(
                List.of(PermissionCodes.AGENT_READ, PermissionCodes.TENANT_DELETE)));
    }
}
