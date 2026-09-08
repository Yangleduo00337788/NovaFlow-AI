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

  test('打开当前应用的历史对话', async ({ page }) => {
    await page.goto('/portal')
    await page.locator('.app-item').first().click()
    await expect(page).toHaveURL(/\/portal\/apps\/\d+/)
    await page.getByRole('button', { name: '历史对话' }).click()
    await expect(page.getByText('我的对话')).toBeVisible()
    await expect(page.locator('.history-empty, .history-item').first()).toBeVisible()
  })

  test('发送一条对话消息', async ({ page }) => {
    await page.goto('/portal')
    await page.locator('.app-item').first().click()
    await expect(page).toHaveURL(/\/portal\/apps\/\d+/)
    await expect(page.getByTestId('portal-chat-panel')).toHaveAttribute('data-ready', 'true', { timeout: 30000 })

    const input = page.locator('textarea[data-testid="portal-chat-input"]')
    await expect(input).toBeEditable({ timeout: 5000 })
    await input.fill('你好，请用一句话介绍自己')
    await page.getByTestId('portal-chat-send').click()
    await expect(page.getByTestId('portal-chat-user-message')).toContainText('你好', { timeout: 10000 })

    const error = page.locator('.portal-chat-panel__error')
    const assistant = page.locator('.assistant-content').last()
    await expect(error.or(assistant)).toBeVisible({ timeout: 60000 })
    if (await error.isVisible().catch(() => false)) {
      const text = await error.innerText()
      if (/模型|密钥|API|解密|配额/.test(text)) {
        test.skip(true, `门户对话依赖 LLM：${text}`)
      }
      throw new Error(`门户对话失败：${text}`)
    }
  })
})
