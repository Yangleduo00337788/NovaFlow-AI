import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { getRoutePermissions } from '@/config/menu'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/index.vue'),
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/register/index.vue'),
      meta: { public: true },
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
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '工作台' } },
        { path: 'agent', name: 'agent', component: () => import('@/views/agent/index.vue'), meta: { title: 'Agent Studio' } },
        { path: 'workflow', name: 'workflow', component: () => import('@/views/placeholder/index.vue'), meta: { title: '工作流 Studio' } },
        { path: 'knowledge', name: 'knowledge', component: () => import('@/views/knowledge/index.vue'), meta: { title: '知识库 Hub' } },
        { path: 'knowledge/:id', name: 'knowledge-detail', component: () => import('@/views/knowledge/detail.vue'), meta: { title: '知识库详情' } },
        { path: 'model', name: 'model', component: () => import('@/views/model/index.vue'), meta: { title: '模型中心' } },
        { path: 'tool', name: 'tool', component: () => import('@/views/tool/index.vue'), meta: { title: '工具市场' } },
        { path: 'prompt', name: 'prompt', component: () => import('@/views/placeholder/index.vue'), meta: { title: 'Prompt 管理' } },
        { path: 'application', name: 'application', component: () => import('@/views/placeholder/index.vue'), meta: { title: '应用管理' } },
        { path: 'monitor', name: 'monitor', component: () => import('@/views/placeholder/index.vue'), meta: { title: '运行监控' } },
        { path: 'log', name: 'log', component: () => import('@/views/log/index.vue'), meta: { title: '调用日志' } },
        { path: 'trace', name: 'trace', component: () => import('@/views/trace/index.vue'), meta: { title: '链路分析' } },
        { path: 'observability', name: 'observability', component: () => import('@/views/placeholder/index.vue'), meta: { title: '可观测性' } },
        { path: 'org', name: 'org', component: () => import('@/views/placeholder/index.vue'), meta: { title: '组织管理' } },
        { path: 'permission', name: 'permission', component: () => import('@/views/placeholder/index.vue'), meta: { title: '权限管理' } },
        { path: 'settings', name: 'settings', component: () => import('@/views/placeholder/index.vue'), meta: { title: '系统设置' } },
        { path: 'billing', name: 'billing', component: () => import('@/views/placeholder/index.vue'), meta: { title: '账单与用量' } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn()) {
    return '/login'
  }
  if ((to.path === '/login' || to.path === '/register') && auth.isLoggedIn()) {
    return '/dashboard'
  }

  if (!to.meta.public) {
    const requiredPermissions = getRoutePermissions(to.path)
    if (requiredPermissions && !auth.hasAnyPermission(requiredPermissions)) {
      return '/dashboard'
    }
  }
})

export default router
