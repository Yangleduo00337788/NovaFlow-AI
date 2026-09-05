import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import PlatformLayout from '@/layouts/PlatformLayout.vue'
import PortalLayout from '@/layouts/PortalLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { getDefaultHome } from '@/config/access'
import { IS_PLATFORM_DEPLOY } from '@/config/deploy'
import { installRouterGuard } from '@/router/guard'
import { platformChildRoutes } from '@/router/platformRoutes'

const aboutChildren = [
  { path: '', name: 'about', component: () => import('@/views/about/index.vue'), meta: { title: '关于' } },
  { path: 'terms', name: 'about-terms', component: () => import('@/views/about/terms.vue'), meta: { title: '用户协议' } },
  { path: 'privacy', name: 'about-privacy', component: () => import('@/views/about/privacy.vue'), meta: { title: '安全与隐私' } },
  { path: 'help', name: 'about-help', component: () => import('@/views/about/help.vue'), meta: { title: '帮助文档' } },
  { path: 'contact', name: 'about-contact', component: () => import('@/views/about/contact.vue'), meta: { title: '联系我们' } },
  { path: 'changelog', name: 'about-changelog', component: () => import('@/views/about/changelog.vue'), meta: { title: '更新日志' } },
  { path: 'report', name: 'about-report', component: () => import('@/views/about/report.vue'), meta: { title: '报告问题' } },
]

function buildPlatformAboutChildren(): RouteRecordRaw[] {
  return aboutChildren.map((item) => ({
    ...item,
    name: item.name ? `platform-${String(item.name)}` : undefined,
  }))
}

const platformOnlyRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'platform-login',
    component: () => import('@/views/platform/login/index.vue'),
    meta: { public: true, loginScope: 'platform' },
  },
  {
    path: '/',
    component: PlatformLayout,
    meta: { scope: 'platform' },
    redirect: '/dashboard',
    children: [
      ...platformChildRoutes,
      {
        path: 'about',
        component: () => import('@/views/about/AboutLayout.vue'),
        meta: { title: '关于' },
        children: buildPlatformAboutChildren(),
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const fullAppRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, loginScope: 'tenant' },
  },
  {
    path: '/platform/login',
    name: 'platform-login',
    component: () => import('@/views/platform/login/index.vue'),
    meta: { public: true, loginScope: 'platform' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/register/index.vue'),
    meta: { public: true },
  },
  {
    path: '/maintenance',
    name: 'maintenance',
    component: () => import('@/views/maintenance/index.vue'),
    meta: { public: true, title: '系统维护' },
  },
  {
    path: '/embed/agents/:id',
    name: 'embed-agent',
    component: () => import('@/views/embed/agent.vue'),
    meta: { public: true, title: 'Agent 对话' },
  },
  {
    path: '/',
    component: AppLayout,
    meta: { scope: 'tenant' },
    redirect: () => {
      const auth = useAuthStore()
      return getDefaultHome(auth.user?.accountType, auth.roleCode)
    },
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '工作台' } },
      { path: 'agent', name: 'agent', component: () => import('@/views/agent/index.vue'), meta: { title: 'Agent Studio' } },
      { path: 'workflow', name: 'workflow', component: () => import('@/views/workflow/index.vue'), meta: { title: '工作流 Studio' } },
      { path: 'workflow/:id', name: 'workflow-editor', component: () => import('@/views/workflow/editor.vue'), meta: { title: '工作流编辑' } },
      { path: 'knowledge', name: 'knowledge', component: () => import('@/views/knowledge/index.vue'), meta: { title: '知识库 Hub' } },
      { path: 'knowledge/:id', name: 'knowledge-detail', component: () => import('@/views/knowledge/detail.vue'), meta: { title: '知识库详情' } },
      { path: 'model', name: 'model', component: () => import('@/views/model/index.vue'), meta: { title: '模型中心' } },
      { path: 'tool', name: 'tool', component: () => import('@/views/tool/index.vue'), meta: { title: '工具市场' } },
      { path: 'prompt', name: 'prompt', component: () => import('@/views/prompt/index.vue'), meta: { title: 'Prompt 管理' } },
      { path: 'application', name: 'application', component: () => import('@/views/application/index.vue'), meta: { title: '应用管理' } },
      { path: 'monitor', name: 'monitor', component: () => import('@/views/monitor/index.vue'), meta: { title: '运行监控' } },
      { path: 'log', name: 'log', component: () => import('@/views/log/index.vue'), meta: { title: '调用日志' } },
      { path: 'trace', name: 'trace', component: () => import('@/views/trace/index.vue'), meta: { title: '链路分析' } },
      { path: 'observability', name: 'observability', component: () => import('@/views/observability/index.vue'), meta: { title: '可观测性' } },
      { path: 'org', name: 'org', component: () => import('@/views/org/index.vue'), meta: { title: '组织管理' } },
      { path: 'permission', name: 'permission', component: () => import('@/views/permission/index.vue'), meta: { title: '权限管理' } },
      { path: 'settings', name: 'settings', component: () => import('@/views/settings/index.vue'), meta: { title: '系统设置' } },
      { path: 'billing', name: 'billing', component: () => import('@/views/billing/index.vue'), meta: { title: '账单与用量' } },
      {
        path: 'audit',
        name: 'audit',
        component: () => import('@/views/audit/index.vue'),
        meta: { title: '审计日志', permissions: ['audit:view'] },
      },
      {
        path: 'about',
        component: () => import('@/views/about/AboutLayout.vue'),
        meta: { title: '关于' },
        children: aboutChildren,
      },
      { path: 'changelog', redirect: '/about/changelog' },
      { path: 'privacy', redirect: '/about/privacy' },
    ],
  },
  {
    path: '/platform',
    component: PlatformLayout,
    meta: { scope: 'platform' },
    redirect: '/platform/dashboard',
    children: [
      ...platformChildRoutes,
      {
        path: 'about',
        component: () => import('@/views/about/AboutLayout.vue'),
        meta: { title: '关于' },
        children: buildPlatformAboutChildren(),
      },
    ],
  },
  {
    path: '/portal',
    component: PortalLayout,
    meta: { permissions: ['portal:access'] },
    children: [
      {
        path: '',
        name: 'portal',
        component: () => import('@/views/portal/index.vue'),
        meta: { title: 'AI 助手', permissions: ['portal:access'] },
      },
      {
        path: 'apps/:id',
        name: 'portal-chat',
        component: () => import('@/views/portal/index.vue'),
        meta: { title: 'AI 助手', permissions: ['portal:access'] },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes: IS_PLATFORM_DEPLOY ? platformOnlyRoutes : fullAppRoutes,
})

installRouterGuard(router)

export default router
