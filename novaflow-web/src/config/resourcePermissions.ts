export const RESOURCE_PERMISSION_OPTIONS = {
  AGENT: [
    { value: 'agent:read', label: '查看' },
    { value: 'agent:edit', label: '编辑' },
    { value: 'agent:publish', label: '发布' },
    { value: 'agent:delete', label: '删除' },
  ],
  WORKFLOW: [
    { value: 'workflow:read', label: '查看' },
    { value: 'workflow:edit', label: '编辑' },
    { value: 'workflow:publish', label: '发布' },
    { value: 'workflow:delete', label: '删除' },
    { value: 'workflow:execute', label: '执行' },
  ],
  KNOWLEDGE: [
    { value: 'knowledge:read', label: '查看' },
    { value: 'knowledge:create', label: '编辑配置' },
    { value: 'knowledge:upload', label: '上传文档' },
    { value: 'knowledge:delete', label: '删除' },
    { value: 'knowledge:search', label: '检索' },
  ],
  APPLICATION: [
    { value: 'application:read', label: '查看' },
    { value: 'application:publish', label: '发布' },
    { value: 'application:manage', label: '管理' },
  ],
  MODEL: [
    { value: 'model:read', label: '查看' },
    { value: 'model:config', label: '配置' },
  ],
  TOOL: [
    { value: 'tool:read', label: '查看' },
    { value: 'tool:update', label: '编辑' },
    { value: 'tool:delete', label: '删除' },
  ],
  MCP: [
    { value: 'mcp:read', label: '查看' },
    { value: 'mcp:update', label: '编辑' },
    { value: 'mcp:delete', label: '删除' },
  ],
  PROMPT: [
    { value: 'prompt:read', label: '查看' },
    { value: 'prompt:edit', label: '编辑' },
    { value: 'prompt:delete', label: '删除' },
  ],
}

export type ResourcePermissionType = keyof typeof RESOURCE_PERMISSION_OPTIONS

export function canManageResourcePermission(hasAnyPermission: (codes?: string[]) => boolean) {
  return hasAnyPermission(['member:manage', 'tenant:manage', 'role:update'])
}
