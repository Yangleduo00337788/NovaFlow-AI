/** 与后端 RoleCodes 保持一致 */
export const RoleCodes = {
  PLATFORM_ADMIN: 'super_admin',
  PLATFORM_AUDITOR: 'platform_auditor',
  TENANT_OWNER: 'tenant_owner',
  TENANT_ADMIN: 'tenant_admin',
  DEVELOPER: 'developer',
  OPERATOR: 'operator',
  MEMBER: 'member',
  VIEWER: 'viewer',
} as const

export type RoleCode = (typeof RoleCodes)[keyof typeof RoleCodes]

export const PROTECTED_MEMBER_ROLES = new Set<string>([
  RoleCodes.PLATFORM_ADMIN,
  RoleCodes.TENANT_OWNER,
])

export function isProtectedMemberRole(roleCode: string): boolean {
  return PROTECTED_MEMBER_ROLES.has(roleCode)
}
