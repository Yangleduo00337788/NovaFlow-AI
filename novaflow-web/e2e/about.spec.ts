import { test, expect } from '@playwright/test'
import { login } from './helpers/auth'

const aboutPages = [
  { path: '/about', heading: '关于 NovaFlow' },
  { path: '/about/terms', heading: '用户协议' },
  { path: '/about/privacy', heading: '安全与隐私' },
  { path: '/about/help', heading: '帮助文档' },
  { path: '/about/contact', heading: '联系我们' },
  { path: '/about/changelog', heading: '更新日志' },
  { path: '/about/report', heading: '报告问题' },
]

test.describe('关于页面', () => {
  test.beforeEach(async ({ page }) => login(page))

  for (const { path, heading } of aboutPages) {
    test(`${path} 可加载`, async ({ page }) => {
      await page.goto(path)
      await expect(page.locator('.about-page')).toBeVisible()
      await expect(page.getByRole('heading', { level: 1 })).toContainText(heading)
      await expect(page.locator('.about-nav')).toBeVisible()
    })
  }
})
