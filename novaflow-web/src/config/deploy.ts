export type DeployScope = 'auto' | 'tenant' | 'platform'

const PLATFORM_ROUTE_PREFIXES = [
  '/dashboard',
  '/tenants',
  '/users',
  '/settings',
  '/billing',
  '/models',
  '/security',
  '/login-logs',
  '/audit',
  '/about',
] as const

export function resolveDeployScope(): 'tenant' | 'platform' {
  const configured = import.meta.env.VITE_DEPLOY_SCOPE as DeployScope | undefined
  if (configured === 'platform' || configured === 'tenant') {
    return configured
  }
  if (typeof window !== 'undefined') {
    const host = window.location.hostname.toLowerCase()
    if (host === 'admin' || host.startsWith('admin.')) {
      return 'platform'
    }
  }
  return 'tenant'
}

export const IS_PLATFORM_DEPLOY = resolveDeployScope() === 'platform'

/** 将逻辑平台路径转为当前部署下的实际路由路径 */
export function platformPath(logicalPath: string): string {
  const normalized = logicalPath.startsWith('/') ? logicalPath : `/${logicalPath}`
  if (!IS_PLATFORM_DEPLOY) {
    if (normalized === '/platform' || normalized === '/platform/') {
      return '/platform/dashboard'
    }
    if (normalized.startsWith('/platform/')) {
      return normalized
    }
    return `/platform${normalized}`
  }
  if (normalized === '/platform' || normalized === '/platform/') {
    return '/dashboard'
  }
  if (normalized.startsWith('/platform/')) {
    return normalized.replace('/platform', '') || '/dashboard'
  }
  return normalized
}

export function platformLoginPath(): string {
  return IS_PLATFORM_DEPLOY ? '/login' : '/platform/login'
}

export function isPlatformScopePath(path: string): boolean {
  if (IS_PLATFORM_DEPLOY) {
    if (path === '/login') {
      return true
    }
    return PLATFORM_ROUTE_PREFIXES.some(
      (prefix) => path === prefix || path.startsWith(`${prefix}/`),
    )
  }
  return path === '/platform' || path.startsWith('/platform/')
}

export function defaultPlatformHome(): string {
  return platformPath('/platform/dashboard')
}
