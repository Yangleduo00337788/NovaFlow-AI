import { IS_PLATFORM_DEPLOY, platformLoginPath } from '@/config/deploy'

export const APP_LOGIN_PATH = '/login'
export const PLATFORM_LOGIN_PATH = platformLoginPath()

export const PUBLIC_LOGIN_PATHS = IS_PLATFORM_DEPLOY
  ? [PLATFORM_LOGIN_PATH]
  : [APP_LOGIN_PATH, PLATFORM_LOGIN_PATH]

export type LoginScope = 'tenant' | 'platform'

export function isPlatformScopePath(path: string): boolean {
  if (IS_PLATFORM_DEPLOY) {
    return path === PLATFORM_LOGIN_PATH
      || path === '/dashboard'
      || path === '/tenants'
      || path === '/users'
      || path === '/settings'
      || path === '/api-monitor'
      || path === '/billing'
      || path === '/models'
      || path === '/security'
      || path === '/login-logs'
      || path === '/audit'
      || path.startsWith('/about')
  }
  return path === '/platform' || path.startsWith('/platform/')
}

export function resolveLoginPath(path = window.location.pathname): string {
  if (IS_PLATFORM_DEPLOY) {
    return PLATFORM_LOGIN_PATH
  }
  return isPlatformScopePath(path) ? PLATFORM_LOGIN_PATH : APP_LOGIN_PATH
}

export function resolveLoginScope(path: string): LoginScope {
  if (IS_PLATFORM_DEPLOY) {
    return 'platform'
  }
  return path === PLATFORM_LOGIN_PATH || (isPlatformScopePath(path) && path !== APP_LOGIN_PATH)
    ? 'platform'
    : 'tenant'
}
