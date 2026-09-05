import { canAccessRoute, type RouteAccessContext } from '@/config/access'
import { platformPath, isPlatformScopePath as isDeployPlatformScopePath } from '@/config/deploy'

export interface PlatformMenuItem {
  key: string
  label: string
  path: string
  icon: string
  permissions?: string[]
}

export interface PlatformMenuGroup {
  title: string
  items: PlatformMenuItem[]
}

/** 平台运营后台菜单（仅 platform 账号可见） */
export const platformMenuGroups: PlatformMenuGroup[] = [
  {
    title: '运营概览',
    items: [
      { key: 'platform-dashboard', label: '运营概览', path: platformPath('/platform/dashboard'), icon: 'dashboard', permissions: ['platform:manage'] },
    ],
  },
  {
    title: '租户与用户',
    items: [
      { key: 'platform-tenants', label: '租户管理', path: platformPath('/platform/tenants'), icon: 'org', permissions: ['platform:manage'] },
      { key: 'platform-users', label: '用户管理', path: platformPath('/platform/users'), icon: 'org', permissions: ['platform:manage'] },
      { key: 'platform-settings', label: '系统配置', path: platformPath('/platform/settings'), icon: 'settings', permissions: ['platform:manage'] },
    ],
  },
  {
    title: '运营大盘',
    items: [
      { key: 'platform-api-monitor', label: 'API 监控', path: platformPath('/platform/api-monitor'), icon: 'monitor', permissions: ['platform:manage'] },
      { key: 'platform-billing', label: '计费大盘', path: platformPath('/platform/billing'), icon: 'billing', permissions: ['platform:manage'] },
      { key: 'platform-models', label: '模型概览', path: platformPath('/platform/models'), icon: 'model', permissions: ['platform:manage'] },
    ],
  },
  {
    title: '安全与审计',
    items: [
      { key: 'platform-security', label: 'IP 黑名单', path: platformPath('/platform/security'), icon: 'settings', permissions: ['platform:manage'] },
      { key: 'platform-login-logs', label: '登录日志', path: platformPath('/platform/login-logs'), icon: 'log', permissions: ['platform:manage'] },
      { key: 'platform-audit', label: '审计日志', path: platformPath('/platform/audit'), icon: 'log', permissions: ['audit:view'] },
    ],
  },
]

const platformRoutePermissionMap: Record<string, string[]> = {
  '/platform/dashboard': ['platform:manage'],
  '/platform/tenants': ['platform:manage'],
  '/platform/users': ['platform:manage'],
  '/platform/settings': ['platform:manage'],
  '/platform/api-monitor': ['platform:manage'],
  '/platform/billing': ['platform:manage'],
  '/platform/models': ['platform:manage'],
  '/platform/security': ['platform:manage'],
  '/platform/login-logs': ['platform:manage'],
  '/platform/audit': ['audit:view'],
}

const platformRouteLabels: Record<string, { title: string; icon: string }> = {
  '/platform/dashboard': { title: '运营概览', icon: 'dashboard' },
  '/platform/tenants': { title: '租户管理', icon: 'org' },
  '/platform/users': { title: '用户管理', icon: 'org' },
  '/platform/settings': { title: '系统配置', icon: 'settings' },
  '/platform/api-monitor': { title: 'API 监控', icon: 'monitor' },
  '/platform/billing': { title: '计费大盘', icon: 'billing' },
  '/platform/models': { title: '模型概览', icon: 'model' },
  '/platform/security': { title: 'IP 黑名单', icon: 'settings' },
  '/platform/login-logs': { title: '登录日志', icon: 'log' },
  '/platform/audit': { title: '审计日志', icon: 'log' },
}

function resolveLogicalPlatformPath(path: string): string {
  if (path === '/dashboard') return '/platform/dashboard'
  if (path === '/tenants') return '/platform/tenants'
  if (path === '/users') return '/platform/users'
  if (path === '/settings') return '/platform/settings'
  if (path === '/api-monitor') return '/platform/api-monitor'
  if (path === '/billing') return '/platform/billing'
  if (path === '/models') return '/platform/models'
  if (path === '/security') return '/platform/security'
  if (path === '/login-logs') return '/platform/login-logs'
  if (path === '/audit') return '/platform/audit'
  if (path.startsWith('/about')) return '/platform' + path
  if (path === '/platform' || path.startsWith('/platform/')) return path
  return path
}

export function getPlatformRoutePermissions(path: string): string[] | undefined {
  const logical = resolveLogicalPlatformPath(path)
  const matched = Object.keys(platformRoutePermissionMap)
    .filter((route) => logical === route || logical.startsWith(`${route}/`))
    .sort((a, b) => b.length - a.length)[0]
  return matched ? platformRoutePermissionMap[matched] : undefined
}

export function filterPlatformMenuGroups(ctx: RouteAccessContext): PlatformMenuGroup[] {
  return platformMenuGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => canAccessRoute(item.path, ctx, getPlatformRoutePermissions)),
    }))
    .filter((group) => group.items.length > 0)
}

export function getPlatformBreadcrumbByPath(path: string) {
  const logical = resolveLogicalPlatformPath(path)
  const matched = Object.keys(platformRouteLabels)
    .filter((route) => logical === route || logical.startsWith(`${route}/`))
    .sort((a, b) => b.length - a.length)[0]
  if (matched) {
    const meta = platformRouteLabels[matched]
    return { title: meta.title, path: platformPath(matched), icon: meta.icon }
  }
  return { title: '运营概览', path: platformPath('/platform/dashboard'), icon: 'dashboard' }
}

export function isPlatformScopePath(path: string): boolean {
  return isDeployPlatformScopePath(path)
}
