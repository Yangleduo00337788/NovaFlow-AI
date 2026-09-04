package ai.novaflow.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionMatrixTest {

    @Test
    void developerCanDebugButNotConfigureModels() {
        assertTrue(RolePermissionMatrix.DEVELOPER.contains(PermissionCodes.AGENT_DEBUG));
        assertFalse(RolePermissionMatrix.DEVELOPER.contains(PermissionCodes.MODEL_CONFIG));
    }

    @Test
    void operatorCanExecuteWorkflowsButNotCreateAgents() {
        assertTrue(RolePermissionMatrix.OPERATOR.contains(PermissionCodes.WORKFLOW_EXECUTE));
        assertFalse(RolePermissionMatrix.OPERATOR.contains(PermissionCodes.AGENT_CREATE));
    }

    @Test
    void viewerIsReadOnlyAcrossCoreResources() {
        assertTrue(RolePermissionMatrix.VIEWER.contains(PermissionCodes.AGENT_READ));
        assertFalse(RolePermissionMatrix.VIEWER.contains(PermissionCodes.AGENT_EDIT));
        assertFalse(RolePermissionMatrix.VIEWER.contains(PermissionCodes.WORKFLOW_CREATE));
    }

    @Test
    void memberCanChatButNotPublish() {
        assertTrue(RolePermissionMatrix.MEMBER.contains(PermissionCodes.AGENT_CHAT));
        assertFalse(RolePermissionMatrix.MEMBER.contains(PermissionCodes.AGENT_PUBLISH));
    }
}
