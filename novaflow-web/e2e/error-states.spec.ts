import { test, expect } from '@playwright/test'

const EMAIL = 'admin@novaflow.ai'
const PASSWORD = 'Admin123!'

test.describe('API 失败状态', () => {
  test('登录失败显示错误提示', async ({ page }) => {
    await page.goto('/login')
    await page.getByTestId('login-email').fill(EMAIL)
    await page.getByTestId('login-password').fill('WrongPassword123!')
    await page.getByTestId('login-submit').click()

    await expect(page.getByText(/邮箱或密码错误|登录失败|Invalid/i)).toBeVisible({ timeout: 10000 })
    await expect(page).toHaveURL(/\/login/)
  })
})
