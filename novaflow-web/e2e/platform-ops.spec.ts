import { expect, test } from '@playwright/test'
import { uniqueName, confirmPopconfirm } from './helpers/auth'

test.describe('平台运营写操作', () => {
  test('创建租户并删除', async ({ page }) => {
    const tenantName = uniqueName('E2E-Tenant')
    const ownerEmail = `e2e-tenant-${Date.now()}@novaflow.test`

    await page.goto('/platform/tenants')
    await expect(page.locator('.platform-admin-page')).toBeVisible()
    await page.getByTestId('create-tenant-btn').click()
    await page.getByTestId('tenant-name-input').fill(tenantName)
    await page.getByTestId('tenant-owner-email-input').fill(ownerEmail)
    await page.getByRole('dialog', { name: '新建租户' }).getByRole('button', { name: /^(OK|确定)$/ }).click()

    const createdDialog = page.getByRole('dialog').filter({ hasText: '租户已创建' })
    await expect(createdDialog).toBeVisible({ timeout: 15000 })
    await createdDialog.getByRole('button', { name: /^(OK|确定)$/ }).click()
    await expect(createdDialog).toBeHidden({ timeout: 10000 })

    const search = page.getByPlaceholder('搜索企业名称、编码、邮箱')
    await search.fill(tenantName)
    await search.press('Enter')
    const row = page.locator('.ant-table-row', { hasText: tenantName })
    await expect(row.getByRole('link', { name: tenantName })).toBeVisible({ timeout: 15000 })

    await row.getByRole('button', { name: '删除' }).click()
    await page.locator('.ant-popconfirm-buttons').getByRole('button', { name: /^(OK|确定)$/ }).click()
    await expect(row).toHaveCount(0, { timeout: 15000 })
  })

  test('添加并删除 IP 黑名单', async ({ page }) => {
    const ip = '203.0.113.' + (Date.now() % 200)
    await page.goto('/platform/security')
    await expect(page.locator('.platform-admin-page')).toBeVisible()
    await page.getByRole('tab', { name: 'IP 黑名单' }).click()
    await page.getByRole('button', { name: '添加 IP' }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    await dialog.getByPlaceholder('如 203.0.113.10').fill(ip)
    await dialog.getByRole('button', { name: /^(OK|确定)$/ }).click()

    const search = page.getByPlaceholder('搜索 IP、原因')
    await search.fill(ip)
    await search.press('Enter')
    await expect(page.getByText(ip)).toBeVisible({ timeout: 10000 })

    const row = page.locator('.ant-table-row', { hasText: ip })
    await row.getByRole('button', { name: '删除' }).click()
    await confirmPopconfirm(page)
    await expect(page.getByText(ip)).not.toBeVisible({ timeout: 15000 })
  })
})
