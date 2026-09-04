package ai.novaflow.common.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 资源 ACL 权限蕴含关系：高权限 grant 可满足低权限 check。
 */
public final class ResourcePermissionHierarchy {

    private static final Map<String, List<String>> IMPLIES = Map.ofEntries(
            Map.entry(PermissionCodes.AGENT_DELETE, List.of(
                    PermissionCodes.AGENT_PUBLISH, PermissionCodes.AGENT_EDIT, PermissionCodes.AGENT_READ)),
            Map.entry(PermissionCodes.AGENT_PUBLISH, List.of(
                    PermissionCodes.AGENT_EDIT, PermissionCodes.AGENT_READ)),
            Map.entry(PermissionCodes.AGENT_EDIT, List.of(PermissionCodes.AGENT_READ)),
            Map.entry(PermissionCodes.WORKFLOW_DELETE, List.of(
                    PermissionCodes.WORKFLOW_PUBLISH, PermissionCodes.WORKFLOW_EDIT,
                    PermissionCodes.WORKFLOW_EXECUTE, PermissionCodes.WORKFLOW_READ)),
            Map.entry(PermissionCodes.WORKFLOW_PUBLISH, List.of(
                    PermissionCodes.WORKFLOW_EDIT, PermissionCodes.WORKFLOW_READ)),
            Map.entry(PermissionCodes.WORKFLOW_EDIT, List.of(PermissionCodes.WORKFLOW_READ)),
            Map.entry(PermissionCodes.WORKFLOW_EXECUTE, List.of(PermissionCodes.WORKFLOW_READ)),
            Map.entry(PermissionCodes.KNOWLEDGE_DELETE, List.of(
                    PermissionCodes.KNOWLEDGE_UPLOAD, PermissionCodes.KNOWLEDGE_CREATE,
                    PermissionCodes.KNOWLEDGE_SEARCH, PermissionCodes.KNOWLEDGE_READ)),
            Map.entry(PermissionCodes.KNOWLEDGE_UPLOAD, List.of(PermissionCodes.KNOWLEDGE_READ)),
            Map.entry(PermissionCodes.KNOWLEDGE_CREATE, List.of(PermissionCodes.KNOWLEDGE_READ)),
            Map.entry(PermissionCodes.KNOWLEDGE_SEARCH, List.of(PermissionCodes.KNOWLEDGE_READ)),
            Map.entry(PermissionCodes.APPLICATION_MANAGE, List.of(
                    PermissionCodes.APPLICATION_PUBLISH, PermissionCodes.APPLICATION_READ)),
            Map.entry(PermissionCodes.APPLICATION_PUBLISH, List.of(PermissionCodes.APPLICATION_READ))
    );

    private ResourcePermissionHierarchy() {
    }

    public static boolean grantSatisfies(String grantCode, String requiredCode) {
        if (grantCode == null || requiredCode == null) {
            return false;
        }
        if (grantCode.equals(requiredCode)) {
            return true;
        }
        List<String> implied = IMPLIES.get(grantCode);
        return implied != null && implied.contains(requiredCode);
    }

    public static Set<String> acceptableGrantCodes(String requiredCode) {
        Set<String> codes = new HashSet<>();
        if (requiredCode == null) {
            return codes;
        }
        codes.add(requiredCode);
        for (Map.Entry<String, List<String>> entry : IMPLIES.entrySet()) {
            if (entry.getValue().contains(requiredCode)) {
                codes.add(entry.getKey());
            }
        }
        return codes;
    }
}
