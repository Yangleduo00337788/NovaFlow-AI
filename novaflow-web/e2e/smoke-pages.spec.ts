import { test, expect } from '@playwright/test'

const tenantPages: Array<{ path: string; selector: string }> = [
  { path: '/dashboard', selector: '.dashboard' },
  { path: '/agent', selector: '[data-testid="agent-page"]' },
  { path: '/workflow', selector: '[data-testid="workflow-page"]' },
  { path: '/knowledge', selector: '[data-testid="knowledge-page"]' },
  { path: '/model', selector: '.model-page' },
  { path: '/tool', selector: '[data-testid="tool-page"]' },
  { path: '/prompt', selector: '[data-testid="prompt-page"]' },
  { path: '/application', selector: '[data-testid="application-page"]' },
  { path: '/monitor', selector: '[data-testid="monitor-page"]' },
  { path: '/log', selector: '.log-page' },
  { path: '/trace', selector: '.trace-page' },
  { path: '/observability', selector: '.observability-page' },
  { path: '/org', selector: '[data-testid="org-page"]' },
  { path: '/permission', selector: '[data-testid="permission-page"]' },
  { path: '/settings', selector: '.settings-page' },
  { path: '/billing', selector: '[data-testid="billing-page"]' },
  { path: '/portal', selector: '.portal-client' },
  { path: '/audit', selector: '.audit-page' },
  { path: '/about', selector: '.about-page' },
]

const platformPages: Array<{ path: string; selector: string }> = [
  { path: '/platform', selector: '.platform-page' },
  { path: '/audit', selector: '.audit-page' },
]

test.describe('全站页面冒烟', () => {
  test('租户侧全部页面可加载', async ({ page }) => {
    for (const { path: pagePath, selector } of tenantPages) {
      await page.goto(pagePath)
      await expect(page.locator(selector), `page ${pagePath} should render`).toBeVisible({ timeout: 15000 })
    }
  })

  test('平台超管页面可加载', async ({ page }) => {
    for (const { path: pagePath, selector } of platformPages) {
      await page.goto(pagePath)
      await expect(page.locator(selector), `page ${pagePath} should render`).toBeVisible({ timeout: 15000 })
    }
  })
})

test.describe('注册页', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('注册页可加载', async ({ page }) => {
    await page.goto('/register')
    await expect(page.getByTestId('register-card')).toBeVisible()
  })
})
