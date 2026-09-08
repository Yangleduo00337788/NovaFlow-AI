import { expect, test } from '@playwright/test'
import {
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  PLATFORM_EMAIL,
  PLATFORM_PASSWORD,
  PORTAL_EMAIL,
  PORTAL_PASSWORD,
  loginAs,
} from './helpers/auth'

test.describe('三角色权限', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('企业管理员：可创建 Agent 与访问组织管理', async ({ page }) => {
    await loginAs(page, ADMIN_EMAIL, ADMIN_PASSWORD, /\/dashboard/)
    await page.goto('/agent')
    await expect(page.getByTestId('create-agent-btn')).toBeVisible()
    await page.goto('/org')
    await expect(page.getByTestId('org-page')).toBeVisible()
  })

  test('普通用户：登录进入应用门户，不可访问 Studio', async ({ page }) => {
    await loginAs(page, PORTAL_EMAIL, PORTAL_PASSWORD, /\/portal/)
    await expect(page.locator('.portal-client')).toBeVisible()
    await page.goto('/agent')
    await expect(page).toHaveURL(/\/portal/)
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/portal/)
  })

  test('平台管理员：进入总控管理页', async ({ page }) => {
    await loginAs(page, PLATFORM_EMAIL, PLATFORM_PASSWORD, /\/platform/)
    await expect(page.locator('[data-testid="platform-dashboard"]')).toBeVisible()
  })
})
