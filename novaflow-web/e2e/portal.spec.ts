import { expect, test } from '@playwright/test'

test.describe('Portal 用户流程', () => {
  test('门户用户登录后可见应用列表', async ({ page }) => {
    await page.goto('/portal')
    await expect(page).toHaveURL(/\/portal/)
    await expect(page.getByText('你的 AI 办公助手')).toBeVisible()
    await expect(page.getByText('应用', { exact: true })).toBeVisible()
    await expect(page.locator('.app-item').first()).toBeVisible({ timeout: 15000 })
  })

  test('选择应用进入对话页', async ({ page }) => {
    await page.goto('/portal')
    await page.locator('.app-item').first().click()
    await expect(page).toHaveURL(/\/portal\/apps\/\d+/)
    await expect(page.getByText('在线')).toBeVisible()
    await expect(page.getByPlaceholder(/输入消息，Enter 发送/)).toBeVisible()
  })
})
