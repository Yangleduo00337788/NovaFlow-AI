/** 与后端 RoleCodes 保持一致：平台管理员 / 企业管理员 / 普通用户 */
export const RoleCodes = {
  PLATFORM_ADMIN: 'super_admin',
  TENANT_ADMIN: 'tenant_admin',
  USER: 'member',
  MEMBER: 'member',
} as const

export type RoleCode = (typeof RoleCodes)[keyof typeof RoleCodes]

export const PROTECTED_MEMBER_ROLES = new Set<string>()

export function isProtectedMemberRole(_roleCode: string): boolean {
  return false
}
