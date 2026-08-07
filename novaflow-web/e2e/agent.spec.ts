import { test, expect } from '@playwright/test'

const EMAIL = 'admin@novaflow.ai'
const PASSWORD = 'Admin123!'

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(EMAIL)
  await page.getByTestId('login-password').fill(PASSWORD)
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/)
}

test.describe('Agent Studio', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('展示 Agent 列表并打开调试面板', async ({ page }) => {
    await page.goto('/agent')
    await expect(page.getByTestId('agent-page')).toBeVisible()
    await expect(page.getByTestId('agent-table')).toBeVisible()

    const debugBtn = page.locator('[data-testid^="debug-agent-"]').first()
    await expect(debugBtn).toBeVisible()
    await debugBtn.click()

    const panel = page.getByTestId('agent-debug-panel')
    await expect(panel).toBeVisible()
    await expect(panel.getByTestId('debug-message-assistant').first()).toBeVisible()

    await panel.getByTestId('debug-input').fill('你好，请介绍一下自己')
    await panel.getByTestId('debug-send').click()

    await expect(panel.getByTestId('debug-message-user')).toContainText('你好')
    await expect(panel.getByTestId('debug-message-assistant').nth(1)).toBeVisible()
  })

  test('编辑 Agent 时显示右侧调试面板', async ({ page }) => {
    await page.goto('/agent')
    const editBtn = page.locator('[data-testid^="edit-agent-"]').first()
    await editBtn.click()

    await expect(page.getByTestId('agent-debug-panel')).toBeVisible()
    await expect(page.getByTestId('agent-name-input')).not.toHaveValue('')
  })
})
