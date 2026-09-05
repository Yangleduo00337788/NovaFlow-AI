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
    void developerHasNoLogOrBillingViewPerPhase5And8() {
        assertFalse(RolePermissionMatrix.DEVELOPER.contains(PermissionCodes.LOG_READ));
        assertFalse(RolePermissionMatrix.DEVELOPER.contains(PermissionCodes.BILLING_VIEW));
    }

    @Test
    void operatorCanExecuteWorkflowsButNotCreateAgents() {
        assertTrue(RolePermissionMatrix.OPERATOR.contains(PermissionCodes.WORKFLOW_EXECUTE));
        assertFalse(RolePermissionMatrix.OPERATOR.contains(PermissionCodes.AGENT_CREATE));
    }

    @Test
    void operatorCanPublishAppsAndReadModelsButNotManageAppsOrBilling() {
        assertTrue(RolePermissionMatrix.OPERATOR.contains(PermissionCodes.APPLICATION_PUBLISH));
        assertTrue(RolePermissionMatrix.OPERATOR.contains(PermissionCodes.MODEL_READ));
        assertFalse(RolePermissionMatrix.OPERATOR.contains(PermissionCodes.APPLICATION_MANAGE));
        assertFalse(RolePermissionMatrix.OPERATOR.contains(PermissionCodes.BILLING_VIEW));
    }

    @Test
    void viewerIsReadOnlyAcrossCoreResources() {
        assertTrue(RolePermissionMatrix.VIEWER.contains(PermissionCodes.AGENT_READ));
        assertFalse(RolePermissionMatrix.VIEWER.contains(PermissionCodes.AGENT_EDIT));
        assertFalse(RolePermissionMatrix.VIEWER.contains(PermissionCodes.WORKFLOW_CREATE));
    }

    @Test
    void viewerHasNoStudioOrObservabilityExtrasPerPhase5And6() {
        assertTrue(RolePermissionMatrix.VIEWER.contains(PermissionCodes.APPLICATION_READ));
        assertFalse(RolePermissionMatrix.VIEWER.contains(PermissionCodes.APPLICATION_MANAGE));
        assertFalse(RolePermissionMatrix.VIEWER.contains(PermissionCodes.MODEL_READ));
        assertFalse(RolePermissionMatrix.VIEWER.contains(PermissionCodes.TRACE_VIEW));
        assertFalse(RolePermissionMatrix.VIEWER.contains(PermissionCodes.LOG_READ));
        assertFalse(RolePermissionMatrix.VIEWER.contains(PermissionCodes.BILLING_VIEW));
    }

    @Test
    void memberCanChatButNotPublish() {
        assertTrue(RolePermissionMatrix.MEMBER.contains(PermissionCodes.AGENT_CHAT));
        assertFalse(RolePermissionMatrix.MEMBER.contains(PermissionCodes.AGENT_PUBLISH));
    }
}
