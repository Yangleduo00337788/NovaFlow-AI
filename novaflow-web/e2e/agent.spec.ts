import { test, expect } from '@playwright/test'

test.describe('Agent Studio', () => {
  test('展示 Agent 列表并打开调试面板', async ({ page }) => {
    await page.goto('/agent')
    await expect(page.getByTestId('agent-page')).toBeVisible()
    await expect(page.getByTestId('agent-table')).toBeVisible()

    const search = page.getByTestId('agent-search')
    await search.fill('DeepSeek')
    await search.press('Enter')
    const row = page.getByRole('row', { name: /DeepSeek/ })
    await expect(row).toBeVisible({ timeout: 10000 })
    await row.getByRole('button', { name: '调试' }).click()

    const panel = page.getByTestId('agent-debug-panel')
    await expect(panel).toBeVisible()
    await expect(panel.getByTestId('debug-input')).toBeVisible()

    const assistant = panel.getByTestId('debug-message-assistant').first()
    const hasWelcome = await assistant.isVisible({ timeout: 15000 }).catch(() => false)
    if (!hasWelcome) {
      // 模型 API Key 未配置或加密密钥不匹配时，欢迎语接口会失败；此时仅验证调试面板 UI
      await expect(panel.getByText('调试对话')).toBeVisible()
      return
    }

    await panel.getByTestId('debug-input').fill('你好，请介绍一下自己')
    await panel.getByTestId('debug-send').click()

    await expect(panel.getByTestId('debug-message-user')).toContainText('你好')
    await expect(panel.getByTestId('debug-message-assistant').nth(1)).toBeVisible({ timeout: 60000 })
  })

  test('编辑 Agent 时显示右侧调试面板', async ({ page }) => {
    await page.goto('/agent')
    const editBtn = page.locator('[data-testid^="edit-agent-"]').first()
    await editBtn.click()

    await expect(page.getByTestId('agent-debug-panel')).toBeVisible()
    await expect(page.getByTestId('agent-name-input')).not.toHaveValue('')
  })
})
