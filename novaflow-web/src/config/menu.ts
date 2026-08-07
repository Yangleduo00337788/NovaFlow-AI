export interface MenuItem {
  key: string
  label: string
  path: string
  icon: string
  beta?: boolean
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
      { key: 'agent', label: 'Agent Studio', path: '/agent', icon: 'robot' },
      { key: 'workflow', label: '工作流 Studio', path: '/workflow', icon: 'workflow' },
      { key: 'knowledge', label: '知识库 Hub', path: '/knowledge', icon: 'knowledge' },
      { key: 'model', label: '模型中心', path: '/model', icon: 'model' },
      { key: 'tool', label: '工具市场', path: '/tool', icon: 'tool' },
      { key: 'prompt', label: 'Prompt 管理', path: '/prompt', icon: 'prompt' },
    ],
  },
  {
    title: '运行与监控',
    items: [
      { key: 'application', label: '应用管理', path: '/application', icon: 'application' },
      { key: 'monitor', label: '运行监控', path: '/monitor', icon: 'monitor' },
      { key: 'log', label: '调用日志', path: '/log', icon: 'log' },
      { key: 'trace', label: '链路分析', path: '/trace', icon: 'trace', beta: true },
      { key: 'observability', label: '可观测性', path: '/observability', icon: 'observability' },
    ],
  },
  {
    title: '系统管理',
    items: [
      { key: 'org', label: '组织管理', path: '/org', icon: 'org' },
      { key: 'permission', label: '权限管理', path: '/permission', icon: 'permission' },
      { key: 'settings', label: '系统设置', path: '/settings', icon: 'settings' },
      { key: 'billing', label: '账单与用量', path: '/billing', icon: 'billing' },
    ],
  },
]

export function getBreadcrumbByPath(path: string): BreadcrumbInfo {
  if (path === '/dashboard' || path.startsWith('/dashboard/')) {
    return { title: '工作台', path: '/dashboard', icon: 'dashboard' }
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
