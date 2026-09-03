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
}

export type ResourcePermissionType = keyof typeof RESOURCE_PERMISSION_OPTIONS

export function canManageResourcePermission(hasAnyPermission: (codes?: string[]) => boolean) {
  return hasAnyPermission(['member:manage', 'tenant:manage', 'role:update'])
}
