import { expect, test } from '@playwright/test'
import {
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  DEVELOPER_EMAIL,
  DEVELOPER_PASSWORD,
  OPERATOR_EMAIL,
  OPERATOR_PASSWORD,
  PLATFORM_EMAIL,
  PLATFORM_PASSWORD,
  PORTAL_EMAIL,
  PORTAL_PASSWORD,
  VIEWER_EMAIL,
  VIEWER_PASSWORD,
  loginAs,
} from './helpers/auth'

test.describe('六角色权限矩阵', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('企业所有者：可创建 Agent 与访问组织管理', async ({ page }) => {
    await loginAs(page, ADMIN_EMAIL, ADMIN_PASSWORD, /\/dashboard/)
    await page.goto('/agent')
    await expect(page.getByTestId('create-agent-btn')).toBeVisible()
    await page.goto('/org')
    await expect(page.getByTestId('org-page')).toBeVisible()
  })

  test('开发者：可创建 Agent，不可访问组织管理', async ({ page }) => {
    await loginAs(page, DEVELOPER_EMAIL, DEVELOPER_PASSWORD, /\/dashboard/)
    await page.goto('/agent')
    await expect(page.getByTestId('create-agent-btn')).toBeVisible()
    await page.goto('/org')
    await expect(page).toHaveURL(/\/dashboard/)
  })

  test('运维人员：可浏览 Agent 但不可创建', async ({ page }) => {
    await loginAs(page, OPERATOR_EMAIL, OPERATOR_PASSWORD, /\/dashboard/)
    await page.goto('/agent')
    await expect(page.getByTestId('agent-page')).toBeVisible()
    await expect(page.getByTestId('create-agent-btn')).toHaveCount(0)
    await page.goto('/workflow')
    await expect(page.getByTestId('workflow-page')).toBeVisible()
    await expect(page.getByTestId('create-workflow-btn')).toHaveCount(0)
  })

  test('企业成员：可浏览 Agent 但不可创建，可进入应用门户', async ({ page }) => {
    await loginAs(page, PORTAL_EMAIL, PORTAL_PASSWORD, /\/dashboard/)
    await page.goto('/agent')
    await expect(page.getByTestId('agent-page')).toBeVisible()
    await expect(page.getByTestId('create-agent-btn')).toHaveCount(0)
    await page.goto('/portal')
    await expect(page.locator('.portal-client')).toBeVisible()
  })

  test('只读用户：可浏览 Agent/工作流但不可创建', async ({ page }) => {
    await loginAs(page, VIEWER_EMAIL, VIEWER_PASSWORD, /\/dashboard/)
    await page.goto('/agent')
    await expect(page.getByTestId('agent-page')).toBeVisible()
    await expect(page.getByTestId('create-agent-btn')).toHaveCount(0)
    await page.goto('/workflow')
    await expect(page.getByTestId('workflow-page')).toBeVisible()
    await expect(page.getByTestId('create-workflow-btn')).toHaveCount(0)
  })

  test('平台超管：进入总控管理页', async ({ page }) => {
    await loginAs(page, PLATFORM_EMAIL, PLATFORM_PASSWORD, /\/platform/)
    await expect(page.locator('.platform-page')).toBeVisible()
  })
})
