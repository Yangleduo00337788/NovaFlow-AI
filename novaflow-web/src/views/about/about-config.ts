import type { Component } from 'vue'
import {
  BookOutlined,
  BugOutlined,
  FileTextOutlined,
  HistoryOutlined,
  MailOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons-vue'

export interface AboutNavItem {
  path: string
  label: string
  title: string
  subtitle: string
  icon: Component
}

export const aboutNavItems: AboutNavItem[] = [
  {
    path: '/about/terms',
    label: '用户协议',
    title: '用户协议',
    subtitle: '使用 NovaFlow AI 平台前，请阅读并理解以下条款',
    icon: FileTextOutlined,
  },
  {
    path: '/about/privacy',
    label: '安全与隐私',
    title: '安全与隐私',
    subtitle: '数据存储、留存周期、第三方观测与集成建议',
    icon: SafetyCertificateOutlined,
  },
  {
    path: '/about/help',
    label: '帮助文档',
    title: '帮助文档',
    subtitle: '快速上手、部署指引与常见问题',
    icon: BookOutlined,
  },
  {
    path: '/about/contact',
    label: '联系我们',
    title: '联系我们',
    subtitle: '技术支持、商务合作与社区交流',
    icon: MailOutlined,
  },
  {
    path: '/about/changelog',
    label: '更新日志',
    title: '更新日志',
    subtitle: 'NovaFlow AI 产品发布历史与功能说明',
    icon: HistoryOutlined,
  },
  {
    path: '/about/report',
    label: '报告问题',
    title: '报告问题',
    subtitle: '提交 Bug、功能建议或使用反馈',
    icon: BugOutlined,
  },
]

export const aboutHomeMeta = {
  title: '关于 NovaFlow AI',
  subtitle: '企业级 AI Agent 开发平台 · 协议、文档与支持',
}

export function getAboutPageMeta(path: string) {
  if (path === '/about' || path === '/about/') {
    return aboutHomeMeta
  }
  const matched = aboutNavItems.find((item) => path === item.path || path.startsWith(`${item.path}/`))
  return matched ?? aboutHomeMeta
}
