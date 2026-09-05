import { test, expect } from '@playwright/test'

test.describe('响应式布局抽样', () => {
  test.use({ viewport: { width: 375, height: 812 } })

  test('移动端 dashboard 与 agent 页可渲染', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page.locator('.dashboard')).toBeVisible({ timeout: 15000 })

    await page.goto('/agent')
    await expect(page.locator('[data-testid="agent-page"]')).toBeVisible({ timeout: 15000 })
  })

  test('平板宽度 portal 页可渲染', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 })
    await page.goto('/portal')
    await expect(page.locator('.portal-client')).toBeVisible({ timeout: 15000 })
  })
})
