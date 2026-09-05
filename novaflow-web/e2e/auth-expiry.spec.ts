import { test, expect } from '@playwright/test'

const EMAIL = 'admin@novaflow.ai'
const PASSWORD = 'Admin123!'

test.describe('Token 过期跳转', () => {
  test('API 401 后跳转登录页', async ({ page }) => {
    await page.goto('/login')
    await page.getByTestId('login-email').fill(EMAIL)
    await page.getByTestId('login-password').fill(PASSWORD)
    await page.getByTestId('login-submit').click()
    await expect(page).toHaveURL(/\/dashboard/)

    await page.route('**/api/v1/**', async (route) => {
      const url = route.request().url()
      if (url.includes('/api/v1/auth/me') || url.includes('/api/v1/dashboard')) {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ code: 40100, message: '未登录', data: null }),
        })
        return
      }
      await route.continue()
    })

    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login/, { timeout: 15000 })
    await expect(page.getByTestId('login-card')).toBeVisible()
  })
})
