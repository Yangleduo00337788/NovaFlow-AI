import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePlatformStatusStore } from '@/stores/platformStatus'
import { getRoutePermissions } from '@/config/menu'
import { getDefaultHome, isAllowedForPortalOnlyRole, isPortalOnlyRole } from '@/config/access'
import { IS_PLATFORM_DEPLOY } from '@/config/deploy'
import { getPlatformRoutePermissions, isPlatformScopePath } from '@/config/platformMenu'
import { isPlatformAccount } from '@/config/account'
import {
  PUBLIC_LOGIN_PATHS,
  resolveLoginPath,
} from '@/config/app'

function resolveRequiredPermissions(path: string): string[] | undefined {
  if (isPlatformScopePath(path)) {
    return getPlatformRoutePermissions(path)
  }
  return getRoutePermissions(path)
}

function isPublicLoginPath(path: string): boolean {
  return PUBLIC_LOGIN_PATHS.includes(path as typeof PUBLIC_LOGIN_PATHS[number])
}

function shouldBypassMaintenance(path: string, accountType?: string | null): boolean {
  if (path === '/maintenance') return true
  if (isPlatformScopePath(path)) return true
  if (isPlatformAccount(accountType)) return true
  return false
}

function shouldEnforceMaintenance(path: string, accountType?: string | null): boolean {
  return !shouldBypassMaintenance(path, accountType)
}

export function installRouterGuard(router: Router) {
  router.beforeEach(async (to) => {
    const auth = useAuthStore()
    const accountType = auth.user?.accountType
    const platformStatus = usePlatformStatusStore()

    if (to.path !== '/maintenance') {
      await platformStatus.refresh()
      if (platformStatus.maintenanceEnabled && shouldEnforceMaintenance(to.path, accountType)) {
        return '/maintenance'
      }
    }

    if (to.meta.public) {
      if (isPublicLoginPath(to.path) && auth.isLoggedIn()) {
        return getDefaultHome(accountType, auth.roleCode)
      }
      return true
    }

    if (!auth.isLoggedIn()) {
      const loginPath = resolveLoginPath(to.path)
      if (to.fullPath !== loginPath) {
        return { path: loginPath, query: { redirect: to.fullPath } }
      }
      return true
    }

    if (isPlatformAccount(accountType)) {
      if (to.path === '/audit') {
        return IS_PLATFORM_DEPLOY ? '/audit' : '/platform/audit'
      }
      if (!isPlatformScopePath(to.path)) {
        return getDefaultHome(accountType, auth.roleCode)
      }
    } else if (isPlatformScopePath(to.path)) {
      return getDefaultHome(accountType, auth.roleCode)
    }

    if (!isPlatformAccount(accountType) && isPortalOnlyRole(auth.roleCode)) {
      if (!isAllowedForPortalOnlyRole(to.path)) {
        const portalHome = getDefaultHome(accountType, auth.roleCode)
        if (to.path !== portalHome && !to.path.startsWith(`${portalHome}/`)) {
          return portalHome
        }
      }
    }

    const requiredPermissions =
      (to.meta.permissions as string[] | undefined) ?? resolveRequiredPermissions(to.path)
    if (requiredPermissions && requiredPermissions.length > 0 && !auth.hasAnyPermission(requiredPermissions)) {
      const fallback = getDefaultHome(accountType, auth.roleCode)
      if (to.path !== fallback) {
        return fallback
      }
    }

    return true
  })
}
