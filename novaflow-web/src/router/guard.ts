import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getRoutePermissions } from '@/config/menu'
import { getDefaultHomeByRole } from '@/config/access'
import { APP_LOGIN_PATH } from '@/config/app'

export function installRouterGuard(router: Router) {
  router.beforeEach((to) => {
    const auth = useAuthStore()

    if (to.meta.public) {
      if ((to.path === '/register' || to.path === APP_LOGIN_PATH) && auth.isLoggedIn()) {
        return getDefaultHomeByRole(auth.roleCode)
      }
      return true
    }

    if (!auth.isLoggedIn()) {
      if (to.fullPath !== APP_LOGIN_PATH) {
        return { path: APP_LOGIN_PATH, query: { redirect: to.fullPath } }
      }
      return true
    }

    const requiredPermissions =
      (to.meta.permissions as string[] | undefined) ?? getRoutePermissions(to.path)
    if (requiredPermissions && requiredPermissions.length > 0 && !auth.hasAnyPermission(requiredPermissions)) {
      const fallback = getDefaultHomeByRole(auth.roleCode)
      if (to.path !== fallback) {
        return fallback
      }
    }

    return true
  })
}
