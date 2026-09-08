import { getRoutePermissions, type RoutePermissionResolver } from '@/config/menu'
import { getPlatformRoutePermissions, isPlatformScopePath } from '@/config/platformMenu'
import { isPlatformAccount } from '@/config/account'
import { defaultPlatformHome, IS_PLATFORM_DEPLOY } from '@/config/deploy'
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

export function isAllowedForPortalOnlyUser(path: string): boolean {
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

/** 有门户权限且不能进 Studio 工作台（含仅门户的自定义角色） */
export function isPortalOnlyUser(ctx: RouteAccessContext): boolean {
  if (isPlatformAccount(ctx.accountType)) {
    return false
  }
  return canAccessRoute(PORTAL_HOME, ctx) && !canAccessRoute('/dashboard', ctx)
}

/** 登录后默认首页 */
export function getDefaultHome(
  accountType?: string | null,
  roleCode = '',
  canAccess?: (path: string) => boolean,
): string {
  if (isPlatformAccount(accountType)) {
    return defaultPlatformHome()
  }
  if (roleCode === RoleCodes.PLATFORM_ADMIN) {
    return defaultPlatformHome()
  }
  if (canAccess) {
    if (canAccess('/dashboard')) {
      return '/dashboard'
    }
    if (canAccess(PORTAL_HOME)) {
      return PORTAL_HOME
    }
  } else if (roleCode === RoleCodes.MEMBER) {
    return PORTAL_HOME
  }
  return '/dashboard'
}

/** @deprecated 使用 getDefaultHome(accountType, roleCode, canAccess) */
export function getDefaultHomeByRole(roleCode: string): string {
  return getDefaultHome(null, roleCode)
}

export function resolvePostLoginPath(
  accountType: string | undefined | null,
  roleCode: string,
  redirect: string | undefined,
  canAccess: (path: string) => boolean,
): string {
  const defaultHome = getDefaultHome(accountType, roleCode, canAccess)
  if (!redirect || !redirect.startsWith('/')) {
    return defaultHome
  }
  if (isPlatformAccount(accountType) && redirect === '/audit') {
    const auditPath = IS_PLATFORM_DEPLOY ? '/audit' : '/platform/audit'
    return canAccess(auditPath) ? auditPath : defaultHome
  }
  if (canAccess(PORTAL_HOME) && !canAccess('/dashboard') && !isAllowedForPortalOnlyUser(redirect)) {
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
