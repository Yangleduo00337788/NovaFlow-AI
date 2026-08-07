import { test, expect } from '@playwright/test'

const EMAIL = 'admin@novaflow.ai'
const PASSWORD = 'Admin123!'

test.describe('登录流程', () => {
  test('未登录访问受保护页面会跳转登录', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByTestId('login-card')).toBeVisible()
  })

  test('使用演示账号登录进入工作台', async ({ page }) => {
    await page.goto('/login')
    await page.getByTestId('login-email').fill(EMAIL)
    await page.getByTestId('login-password').fill(PASSWORD)
    await page.getByTestId('login-submit').click()

    await expect(page).toHaveURL(/\/dashboard/)
    await expect(page.getByText('欢迎回来')).toBeVisible()
  })
})
