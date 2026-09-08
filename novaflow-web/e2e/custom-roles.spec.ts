import { expect, test } from '@playwright/test'
import {
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  PLATFORM_EMAIL,
  PLATFORM_PASSWORD,
  loginAs,
  uniqueName,
} from './helpers/auth'

test.describe('Phase 11 自定义角色与平台账号', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('企业管理员：可创建自定义角色并在组织邀请中分配', async ({ page }) => {
    const roleName = uniqueName('QA角色')

    await loginAs(page, ADMIN_EMAIL, ADMIN_PASSWORD, /\/dashboard/)
    await page.goto('/permission')
    await expect(page.getByTestId('permission-page')).toBeVisible()

    await page.getByTestId('create-custom-role').click()
    await page.locator('.ant-modal').getByPlaceholder('例如：客服专员').fill(roleName)
    await page.locator('.ant-modal').getByText('查看 Agent', { exact: true }).click()
    await page.locator('.ant-modal').getByRole('button', { name: /^(OK|确定)$/ }).click()

    await expect(page.locator('.role-item.custom').filter({ hasText: roleName })).toBeVisible({ timeout: 10000 })

    await page.goto('/org')
    await expect(page.getByTestId('org-page')).toBeVisible()
    await page.waitForResponse(
      (response) => response.url().includes('/roles/assignable') && response.ok(),
      { timeout: 15000 },
    )
    await page.getByRole('tab', { name: '成员管理' }).click()
    const inviteBtn = page.getByRole('button', { name: '邀请成员' })
    await expect(inviteBtn).toBeVisible({ timeout: 15000 })
    await inviteBtn.click()
    await expect(page.locator('.ant-modal').getByText('邀请成员')).toBeVisible()
    const roleSelect = page.locator('.ant-modal').locator('.ant-select').first()
    await roleSelect.click()
    await roleSelect.locator('input').fill(roleName)
    await expect(page.locator('.ant-select-item-option').filter({ hasText: roleName })).toBeVisible({ timeout: 15000 })
  })

  test('平台超管：无法进入企业工作台', async ({ page }) => {
    await loginAs(page, PLATFORM_EMAIL, PLATFORM_PASSWORD, /\/platform/)
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/platform\/dashboard/)
    await page.goto('/agent')
    await expect(page).toHaveURL(/\/platform\/dashboard/)
  })
})
