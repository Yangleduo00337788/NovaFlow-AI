import { getRoutePermissions } from '@/config/menu'
import { RoleCodes } from '@/config/roles'

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

/** 仅门户入口角色（当前无；Member/Viewer 可进工作台） */
export function isPortalOnlyRole(roleCode: string): boolean {
  return false
}

export function isAllowedForPortalOnlyRole(path: string): boolean {
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

/** 登录后按角色进入对应区域 */
export function getDefaultHomeByRole(roleCode: string): string {
  if (roleCode === RoleCodes.PLATFORM_ADMIN) {
    return '/platform'
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
  if (isPortalOnlyRole(roleCode) && !isAllowedForPortalOnlyRole(redirect)) {
    return defaultHome
  }
  if (!canAccess(redirect)) {
    return defaultHome
  }
  return redirect
}
