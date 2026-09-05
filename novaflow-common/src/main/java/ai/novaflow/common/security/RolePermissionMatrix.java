package ai.novaflow.common.security;

import java.util.Set;

/**
 * 租户内置角色的权限矩阵（与 V29 + V30–V34 对齐，供代码与测试引用）。
 */
public final class RolePermissionMatrix {

    public static final Set<String> DEVELOPER = Set.of(
            PermissionCodes.DASHBOARD_VIEW,
            PermissionCodes.AGENT_READ, PermissionCodes.AGENT_CREATE, PermissionCodes.AGENT_EDIT,
            PermissionCodes.AGENT_DELETE, PermissionCodes.AGENT_PUBLISH, PermissionCodes.AGENT_CHAT,
            PermissionCodes.AGENT_DEBUG,
            PermissionCodes.WORKFLOW_READ, PermissionCodes.WORKFLOW_CREATE, PermissionCodes.WORKFLOW_EDIT,
            PermissionCodes.WORKFLOW_DELETE, PermissionCodes.WORKFLOW_PUBLISH, PermissionCodes.WORKFLOW_EXECUTE,
            PermissionCodes.KNOWLEDGE_READ, PermissionCodes.KNOWLEDGE_CREATE, PermissionCodes.KNOWLEDGE_UPLOAD,
            PermissionCodes.KNOWLEDGE_DELETE, PermissionCodes.KNOWLEDGE_SEARCH,
            PermissionCodes.MODEL_READ,
            PermissionCodes.TOOL_READ, PermissionCodes.TOOL_CREATE, PermissionCodes.TOOL_UPDATE, PermissionCodes.TOOL_DELETE,
            PermissionCodes.MCP_READ, PermissionCodes.MCP_CREATE, PermissionCodes.MCP_UPDATE, PermissionCodes.MCP_DELETE,
            PermissionCodes.PROMPT_READ, PermissionCodes.PROMPT_CREATE, PermissionCodes.PROMPT_EDIT, PermissionCodes.PROMPT_DELETE,
            PermissionCodes.API_READ, PermissionCodes.API_CREATE, PermissionCodes.API_UPDATE, PermissionCodes.API_DELETE,
            PermissionCodes.APPLICATION_READ, PermissionCodes.APPLICATION_PUBLISH, PermissionCodes.APPLICATION_MANAGE,
            PermissionCodes.MONITOR_VIEW, PermissionCodes.TRACE_VIEW,
            PermissionCodes.SEARCH_GLOBAL,
            PermissionCodes.PORTAL_ACCESS
    );

    public static final Set<String> OPERATOR = Set.of(
            PermissionCodes.DASHBOARD_VIEW,
            PermissionCodes.AGENT_READ, PermissionCodes.AGENT_PUBLISH, PermissionCodes.AGENT_CHAT,
            PermissionCodes.WORKFLOW_READ, PermissionCodes.WORKFLOW_PUBLISH, PermissionCodes.WORKFLOW_EXECUTE,
            PermissionCodes.KNOWLEDGE_READ, PermissionCodes.KNOWLEDGE_SEARCH,
            PermissionCodes.MODEL_READ,
            PermissionCodes.APPLICATION_READ, PermissionCodes.APPLICATION_PUBLISH,
            PermissionCodes.MONITOR_VIEW, PermissionCodes.TRACE_VIEW, PermissionCodes.LOG_READ,
            PermissionCodes.SEARCH_GLOBAL,
            PermissionCodes.PORTAL_ACCESS
    );

    public static final Set<String> MEMBER = Set.of(
            PermissionCodes.DASHBOARD_VIEW,
            PermissionCodes.AGENT_READ, PermissionCodes.AGENT_CHAT,
            PermissionCodes.WORKFLOW_READ,
            PermissionCodes.KNOWLEDGE_READ, PermissionCodes.KNOWLEDGE_SEARCH,
            PermissionCodes.APPLICATION_READ,
            PermissionCodes.PORTAL_ACCESS,
            PermissionCodes.SEARCH_GLOBAL
    );

    public static final Set<String> VIEWER = Set.of(
            PermissionCodes.DASHBOARD_VIEW,
            PermissionCodes.AGENT_READ,
            PermissionCodes.WORKFLOW_READ,
            PermissionCodes.KNOWLEDGE_READ, PermissionCodes.KNOWLEDGE_SEARCH,
            PermissionCodes.APPLICATION_READ,
            PermissionCodes.MONITOR_VIEW,
            PermissionCodes.SEARCH_GLOBAL,
            PermissionCodes.PORTAL_ACCESS
    );

    private RolePermissionMatrix() {
    }
}
