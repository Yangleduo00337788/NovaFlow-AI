import { test, expect } from '@playwright/test'

test.describe('防重复提交', () => {
  test('登录按钮连点只发起一次请求', async ({ page }) => {
    let loginRequests = 0

    await page.route('**/api/v1/auth/login', async (route) => {
      loginRequests += 1
      await new Promise((resolve) => setTimeout(resolve, 800))
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: {
            token: 'nf_test_token',
            userId: 1,
            email: 'admin@novaflow.ai',
            nickname: 'Admin',
            roleCode: 'tenant_admin',
            tenantId: 1,
            permissions: ['agent:read'],
          },
        }),
      })
    })

    await page.goto('/login')
    await page.getByTestId('login-email').fill('admin@novaflow.ai')
    await page.getByTestId('login-password').fill('Admin123!')

    const submit = page.getByTestId('login-submit')
    await submit.click({ clickCount: 2, delay: 50 })

    await page.waitForTimeout(1500)
    expect(loginRequests).toBeLessThanOrEqual(1)
  })
})
