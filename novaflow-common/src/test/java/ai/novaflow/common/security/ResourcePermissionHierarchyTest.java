package ai.novaflow.common.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePermissionHierarchyTest {

    @Test
    void higherGrantSatisfiesLowerRequirement() {
        assertTrue(ResourcePermissionHierarchy.grantSatisfies(
                PermissionCodes.AGENT_EDIT, PermissionCodes.AGENT_READ));
        assertFalse(ResourcePermissionHierarchy.grantSatisfies(
                PermissionCodes.AGENT_READ, PermissionCodes.AGENT_EDIT));
    }

    @Test
    void acceptableGrantCodesIncludeImpliedGrants() {
        Set<String> codes = ResourcePermissionHierarchy.acceptableGrantCodes(PermissionCodes.AGENT_READ);
        assertTrue(codes.contains(PermissionCodes.AGENT_READ));
        assertTrue(codes.contains(PermissionCodes.AGENT_EDIT));
        assertTrue(codes.contains(PermissionCodes.AGENT_DELETE));
    }
}
