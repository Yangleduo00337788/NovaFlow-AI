import type { RouteRecordRaw } from 'vue-router'

export const platformChildRoutes: RouteRecordRaw[] = [
  {
    path: 'dashboard',
    name: 'platform-dashboard',
    component: () => import('@/views/platform/dashboard.vue'),
    meta: { title: '运营概览', permissions: ['platform:manage'] },
  },
  {
    path: 'tenants',
    name: 'platform-tenants',
    component: () => import('@/views/platform/tenants.vue'),
    meta: { title: '租户管理', permissions: ['platform:manage'] },
  },
  {
    path: 'tenants/:id',
    name: 'platform-tenant-detail',
    component: () => import('@/views/platform/tenant-detail.vue'),
    meta: { title: '租户详情', permissions: ['platform:manage'] },
  },
  {
    path: 'users',
    name: 'platform-users',
    component: () => import('@/views/platform/users.vue'),
    meta: { title: '用户管理', permissions: ['platform:manage'] },
  },
  {
    path: 'settings',
    name: 'platform-settings',
    component: () => import('@/views/platform/settings.vue'),
    meta: { title: '系统配置', permissions: ['platform:manage'] },
  },
  {
    path: 'api-monitor',
    name: 'platform-api-monitor',
    component: () => import('@/views/platform/api-monitor.vue'),
    meta: { title: 'API 监控', permissions: ['platform:manage'] },
  },
  {
    path: 'billing',
    name: 'platform-billing',
    component: () => import('@/views/platform/billing.vue'),
    meta: { title: '计费大盘', permissions: ['platform:manage'] },
  },
  {
    path: 'models',
    name: 'platform-models',
    component: () => import('@/views/platform/models.vue'),
    meta: { title: '模型概览', permissions: ['platform:manage'] },
  },
  {
    path: 'security',
    name: 'platform-security',
    component: () => import('@/views/platform/security.vue'),
    meta: { title: 'IP 黑名单', permissions: ['platform:manage'] },
  },
  {
    path: 'login-logs',
    name: 'platform-login-logs',
    component: () => import('@/views/platform/login-logs.vue'),
    meta: { title: '登录日志', permissions: ['platform:manage'] },
  },
  {
    path: 'audit',
    name: 'platform-audit',
    component: () => import('@/views/platform/audit.vue'),
    meta: { title: '审计日志', permissions: ['audit:view'] },
  },
]
