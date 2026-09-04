package ai.novaflow.common.security;

/**
 * 系统权限码常量（与 {@code permission.permission_code} 及 Flyway 种子数据保持一致）。
 * <p>在 {@code @SaCheckPermission} 中请使用内联数组 {@code {PermissionCodes.AGENT_READ, ...}}，
 * 不要直接引用本类的 {@code String[]} 组合常量。
 */
public final class PermissionCodes {

    // --- Agent ---
    public static final String AGENT_READ = "agent:read";
    public static final String AGENT_CREATE = "agent:create";
    public static final String AGENT_EDIT = "agent:edit";
    public static final String AGENT_DELETE = "agent:delete";
    public static final String AGENT_PUBLISH = "agent:publish";
    public static final String AGENT_CHAT = "agent:chat";
    public static final String AGENT_DEBUG = "agent:debug";

    // --- Workflow ---
    public static final String WORKFLOW_READ = "workflow:read";
    public static final String WORKFLOW_CREATE = "workflow:create";
    public static final String WORKFLOW_EDIT = "workflow:edit";
    public static final String WORKFLOW_DELETE = "workflow:delete";
    public static final String WORKFLOW_PUBLISH = "workflow:publish";
    public static final String WORKFLOW_EXECUTE = "workflow:execute";

    // --- Knowledge ---
    public static final String KNOWLEDGE_READ = "knowledge:read";
    public static final String KNOWLEDGE_CREATE = "knowledge:create";
    public static final String KNOWLEDGE_UPLOAD = "knowledge:upload";
    public static final String KNOWLEDGE_DELETE = "knowledge:delete";
    public static final String KNOWLEDGE_SEARCH = "knowledge:search";

    // --- Application ---
    public static final String APPLICATION_READ = "application:read";
    public static final String APPLICATION_MANAGE = "application:manage";
    public static final String APPLICATION_PUBLISH = "application:publish";

    // --- Model ---
    public static final String MODEL_READ = "model:read";
    public static final String MODEL_CONFIG = "model:config";

    // --- Tool / MCP ---
    public static final String TOOL_READ = "tool:read";
    public static final String TOOL_CREATE = "tool:create";
    public static final String TOOL_UPDATE = "tool:update";
    public static final String TOOL_DELETE = "tool:delete";
    public static final String MCP_READ = "mcp:read";
    public static final String MCP_CREATE = "mcp:create";
    public static final String MCP_UPDATE = "mcp:update";
    public static final String MCP_DELETE = "mcp:delete";

    // --- Prompt ---
    public static final String PROMPT_READ = "prompt:read";
    public static final String PROMPT_CREATE = "prompt:create";
    public static final String PROMPT_EDIT = "prompt:edit";
    public static final String PROMPT_DELETE = "prompt:delete";

    // --- API Key ---
    public static final String API_READ = "api:read";
    public static final String API_CREATE = "api:create";
    public static final String API_UPDATE = "api:update";
    public static final String API_DELETE = "api:delete";

    // --- Organization / Tenant ---
    public static final String USER_READ = "user:read";
    public static final String USER_CREATE = "user:create";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_DELETE = "user:delete";
    public static final String ROLE_READ = "role:read";
    public static final String ROLE_CREATE = "role:create";
    public static final String ROLE_UPDATE = "role:update";
    public static final String ROLE_DELETE = "role:delete";
    public static final String TENANT_MANAGE = "tenant:manage";
    public static final String TENANT_DELETE = "tenant:delete";
    public static final String MEMBER_MANAGE = "member:manage";

    // --- Billing / Monitor ---
    public static final String BILLING_VIEW = "billing:view";
    public static final String BILLING_MANAGE = "billing:manage";
    public static final String MONITOR_VIEW = "monitor:view";
    public static final String TRACE_VIEW = "trace:view";
    public static final String LOG_READ = "log:read";

    // --- Platform / Portal ---
    public static final String DASHBOARD_VIEW = "dashboard:view";
    public static final String PORTAL_ACCESS = "portal:access";
    public static final String PLATFORM_MANAGE = "platform:manage";
    public static final String AUDIT_VIEW = "audit:view";
    public static final String SEARCH_GLOBAL = "search:global";

    /** Agent 列表/详情：读或写权限任一即可 */
    public static final String[] AGENT_STUDIO_ACCESS = {
            AGENT_READ, AGENT_CREATE, AGENT_EDIT
    };

    /** Agent 调试入口 */
    public static final String[] AGENT_DEBUG_ACCESS = {
            AGENT_DEBUG, AGENT_EDIT
    };

    /** 工作流列表/详情 */
    public static final String[] WORKFLOW_STUDIO_ACCESS = {
            WORKFLOW_READ, WORKFLOW_CREATE, WORKFLOW_EDIT
    };

    /** 工作流下拉（Agent 编排页也会用到） */
    public static final String[] WORKFLOW_OPTIONS_ACCESS = {
            WORKFLOW_READ, WORKFLOW_CREATE, WORKFLOW_EDIT, AGENT_EDIT
    };

    /** 知识库列表/详情 */
    public static final String[] KNOWLEDGE_STUDIO_ACCESS = {
            KNOWLEDGE_READ, KNOWLEDGE_CREATE, KNOWLEDGE_UPLOAD
    };

    /** 工作流执行 */
    public static final String[] WORKFLOW_EXECUTE_ACCESS = {
            WORKFLOW_EXECUTE, WORKFLOW_EDIT
    };

    private PermissionCodes() {
    }
}
