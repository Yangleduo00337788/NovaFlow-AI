import { getRoutePermissions } from '@/config/menu'

export interface RouteAccessContext {
  roleCode: string
  hasAnyPermission: (codes?: string[]) => boolean
}

export const PORTAL_HOME = '/portal'

export function isPortalPath(path: string): boolean {
  return path === PORTAL_HOME || path.startsWith(`${PORTAL_HOME}/`)
}

export function portalAppPath(applicationId: number): string {
  return `${PORTAL_HOME}/apps/${applicationId}`
}

export function isEndUser(roleCode: string): boolean {
  return roleCode === 'user'
}

/** 普通用户仅使用门户与关于页；管理/开发角色可进入全部有权限的区域（含预览门户） */
export function isAllowedForEndUser(path: string): boolean {
  return isPortalPath(path) || path === '/about' || path.startsWith('/about/')
}

/** 纯权限模型：路由是否可见只由权限码决定 */
export function canAccessRoute(path: string, ctx: RouteAccessContext): boolean {
  const requiredPermissions = getRoutePermissions(path)
  if (requiredPermissions && requiredPermissions.length > 0) {
    return ctx.hasAnyPermission(requiredPermissions)
  }
  return true
}

/** 登录后按角色进入对应区域：超管 → 总控，普通用户 → 门户，其余 → 工作台 */
export function getDefaultHomeByRole(roleCode: string): string {
  if (roleCode === 'super_admin') {
    return '/platform'
  }
  if (isEndUser(roleCode)) {
    return PORTAL_HOME
  }
  return '/dashboard'
}

export function resolvePostLoginPath(
  roleCode: string,
  redirect: string | undefined,
  canAccess: (path: string) => boolean,
): string {
  const defaultHome = getDefaultHomeByRole(roleCode)
  if (!redirect || !redirect.startsWith('/')) {
    return defaultHome
  }
  if (isEndUser(roleCode) && !isAllowedForEndUser(redirect)) {
    return defaultHome
  }
  if (!canAccess(redirect)) {
    return defaultHome
  }
  return redirect
}
