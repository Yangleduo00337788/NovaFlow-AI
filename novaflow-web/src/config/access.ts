import { getRoutePermissions, type RoutePermissionResolver } from '@/config/menu'
import { getPlatformRoutePermissions, isPlatformScopePath } from '@/config/platformMenu'
import { isPlatformAccount } from '@/config/account'
import { defaultPlatformHome, IS_PLATFORM_DEPLOY, platformPath } from '@/config/deploy'
import { RoleCodes } from '@/config/roles'

export interface RouteAccessContext {
  roleCode: string
  accountType?: string | null
  hasAnyPermission: (codes?: string[]) => boolean
}

export const PORTAL_HOME = '/portal'

export function isPortalPath(path: string): boolean {
  return path === PORTAL_HOME || path.startsWith(`${PORTAL_HOME}/`)
}

export function portalAppPath(applicationId: number): string {
  return `${PORTAL_HOME}/apps/${applicationId}`
}

/** 仅门户入口角色（企业成员：默认且主要使用应用门户） */
export function isPortalOnlyRole(roleCode: string): boolean {
  return roleCode === RoleCodes.MEMBER
}

export function isAllowedForPortalOnlyRole(path: string): boolean {
  return isPortalPath(path) || path === '/about' || path.startsWith('/about/')
}

function resolveRoutePermissions(path: string): string[] | undefined {
  if (isPlatformScopePath(path)) {
    return getPlatformRoutePermissions(path)
  }
  return getRoutePermissions(path)
}

/** 路由是否可访问（按账号域 + 权限码） */
export function canAccessRoute(
  path: string,
  ctx: RouteAccessContext,
  permissionResolver: RoutePermissionResolver = resolveRoutePermissions,
): boolean {
  if (isPlatformScopePath(path) && !isPlatformAccount(ctx.accountType)) {
    return false
  }
  if (!isPlatformScopePath(path) && isPlatformAccount(ctx.accountType)) {
    return false
  }

  const requiredPermissions = permissionResolver(path)
  if (requiredPermissions && requiredPermissions.length > 0) {
    return ctx.hasAnyPermission(requiredPermissions)
  }
  return true
}

/** 登录后默认首页 */
export function getDefaultHome(accountType?: string | null, roleCode = ''): string {
  if (isPlatformAccount(accountType)) {
    if (roleCode === RoleCodes.PLATFORM_AUDITOR) {
      return platformPath('/platform/audit')
    }
    return defaultPlatformHome()
  }
  if (roleCode === RoleCodes.PLATFORM_ADMIN) {
    return defaultPlatformHome()
  }
  if (isPortalOnlyRole(roleCode)) {
    return PORTAL_HOME
  }
  return '/dashboard'
}

/** @deprecated 使用 getDefaultHome(accountType, roleCode) */
export function getDefaultHomeByRole(roleCode: string): string {
  return getDefaultHome(null, roleCode)
}

export function resolvePostLoginPath(
  accountType: string | undefined | null,
  roleCode: string,
  redirect: string | undefined,
  canAccess: (path: string) => boolean,
): string {
  const defaultHome = getDefaultHome(accountType, roleCode)
  if (!redirect || !redirect.startsWith('/')) {
    return defaultHome
  }
  if (isPlatformAccount(accountType) && redirect === '/audit') {
    const auditPath = IS_PLATFORM_DEPLOY ? '/audit' : '/platform/audit'
    return canAccess(auditPath) ? auditPath : defaultHome
  }
  if (isPortalOnlyRole(roleCode) && !isAllowedForPortalOnlyRole(redirect)) {
    return defaultHome
  }
  if (!canAccess(redirect)) {
    return defaultHome
  }
  return redirect
}

export function createRouteAccessContext(auth: {
  roleCode: string
  user?: { accountType?: string | null } | null
  hasAnyPermission: (codes?: string[]) => boolean
}): RouteAccessContext {
  return {
    roleCode: auth.roleCode,
    accountType: auth.user?.accountType,
    hasAnyPermission: auth.hasAnyPermission.bind(auth),
  }
}
