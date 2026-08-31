import { aboutNavItems, getAboutPageMeta } from '@/views/about/about-config'

export interface MenuItem {  key: string
  label: string
  path: string
  icon: string
  beta?: boolean
  permissions?: string[]
}

export interface MenuGroup {
  title: string
  items: MenuItem[]
}

export interface BreadcrumbInfo {
  title: string
  path: string
  icon: string
}

export const menuGroups: MenuGroup[] = [
  {
    title: 'AI 开发',
    items: [
      { key: 'agent', label: 'Agent Studio', path: '/agent', icon: 'robot', permissions: ['agent:create', 'agent:edit', 'agent:chat'] },
      { key: 'workflow', label: '工作流 Studio', path: '/workflow', icon: 'workflow', permissions: ['workflow:create', 'workflow:edit'] },
      { key: 'knowledge', label: '知识库 Hub', path: '/knowledge', icon: 'knowledge', permissions: ['knowledge:create', 'knowledge:upload'] },
      { key: 'model', label: '模型中心', path: '/model', icon: 'model', permissions: ['model:config'] },
      { key: 'tool', label: '工具市场', path: '/tool', icon: 'tool', permissions: ['agent:edit'] },
      { key: 'prompt', label: 'Prompt 管理', path: '/prompt', icon: 'prompt', permissions: ['prompt:create', 'prompt:edit'] },
    ],
  },
  {
    title: '运行与监控',
    items: [
      { key: 'application', label: '应用管理', path: '/application', icon: 'application', permissions: ['application:manage'] },
      { key: 'monitor', label: '运行监控', path: '/monitor', icon: 'monitor', permissions: ['monitor:view'] },
      { key: 'log', label: '调用日志', path: '/log', icon: 'log', permissions: ['monitor:view', 'billing:view'] },
      { key: 'trace', label: '链路分析', path: '/trace', icon: 'trace', beta: true, permissions: ['trace:view'] },
      { key: 'observability', label: '可观测性', path: '/observability', icon: 'observability', permissions: ['monitor:view'] },
    ],
  },
  {
    title: '系统管理',
    items: [
      { key: 'org', label: '组织管理', path: '/org', icon: 'org', permissions: ['tenant:manage', 'member:manage'] },
      { key: 'permission', label: '权限管理', path: '/permission', icon: 'permission', permissions: ['member:manage'] },
      { key: 'settings', label: '系统设置', path: '/settings', icon: 'settings', permissions: ['tenant:manage'] },
      { key: 'billing', label: '账单与用量', path: '/billing', icon: 'billing', permissions: ['billing:view', 'billing:manage'] },
      { key: 'platform', label: '平台超管', path: '/platform', icon: 'settings', permissions: ['platform:manage'] },
      { key: 'audit', label: '审计日志', path: '/audit', icon: 'log', permissions: ['audit:view', 'platform:manage'] },
    ],
  },
]

const routePermissionMap: Record<string, string[]> = {
  '/dashboard': [],
  '/agent': ['agent:create', 'agent:edit', 'agent:chat'],
  '/workflow': ['workflow:create', 'workflow:edit'],
  '/knowledge': ['knowledge:create', 'knowledge:upload'],
  '/model': ['model:config'],
  '/tool': ['agent:edit'],
  '/prompt': ['prompt:create', 'prompt:edit'],
  '/application': ['application:manage'],
  '/monitor': ['monitor:view'],
  '/log': ['monitor:view', 'billing:view'],
  '/trace': ['trace:view'],
  '/observability': ['monitor:view'],
  '/org': ['tenant:manage', 'member:manage'],
  '/permission': ['member:manage'],
  '/settings': ['tenant:manage'],
  '/billing': ['billing:view', 'billing:manage'],
  '/platform': ['platform:manage'],
  '/audit': ['audit:view', 'platform:manage'],
  '/about': [],
}

export function filterMenuGroups(hasAnyPermission: (codes?: string[]) => boolean): MenuGroup[] {
  return menuGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => hasAnyPermission(item.permissions)),
    }))
    .filter((group) => group.items.length > 0)
}

export function getRoutePermissions(path: string): string[] | undefined {
  const matched = Object.keys(routePermissionMap)
    .filter((route) => path === route || path.startsWith(`${route}/`))
    .sort((a, b) => b.length - a.length)[0]
  return matched ? routePermissionMap[matched] : undefined
}

export function getBreadcrumbByPath(path: string): BreadcrumbInfo {
  if (path === '/dashboard' || path.startsWith('/dashboard/')) {
    return { title: '工作台', path: '/dashboard', icon: 'dashboard' }
  }

  if (path === '/about' || path.startsWith('/about/')) {
    const meta = getAboutPageMeta(path)
    const matched = path === '/about' || path === '/about/'
      ? '/about'
      : aboutNavItems.find((item) => path === item.path || path.startsWith(`${item.path}/`))?.path
    return { title: meta.title, path: matched || '/about', icon: 'settings' }
  }

  for (const group of menuGroups) {
    for (const item of group.items) {
      if (path === item.path || path.startsWith(`${item.path}/`)) {
        return { title: item.label, path: item.path, icon: item.icon }
      }
    }
  }

  return { title: '工作台', path: '/dashboard', icon: 'dashboard' }
}
