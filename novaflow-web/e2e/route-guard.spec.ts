import { expect, test, type APIRequestContext } from '@playwright/test'
import {
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  PLATFORM_EMAIL,
  PLATFORM_PASSWORD,
  PORTAL_EMAIL,
  PORTAL_PASSWORD,
  loginAs,
} from './helpers/auth'

const API_BASE = process.env.NOVAFLOW_API_URL ?? 'http://localhost:8088'

async function fetchApiToken(request: APIRequestContext, email: string, password: string) {
  const resp = await request.post(`${API_BASE}/api/v1/auth/login`, {
    data: { email, password },
  })
  expect(resp.ok()).toBeTruthy()
  const json = await resp.json()
  expect(json.code).toBe(0)
  return json.data.token as string
}

async function expectApiDenied(request: APIRequestContext, token: string, path: string) {
  const resp = await request.get(`${API_BASE}${path}`, {
    headers: { Authorization: token },
  })
  const json = await resp.json()
  expect(resp.status()).toBeGreaterThanOrEqual(400)
  expect(json.code).not.toBe(0)
}

test.describe('Z-08 前端路由守卫 vs 后端权限', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('普通用户：不可进组织/平台，门户可访问', async ({ page, request }) => {
    await loginAs(page, PORTAL_EMAIL, PORTAL_PASSWORD, /\/portal/)

    for (const path of ['/org', '/platform', '/audit', '/permission', '/dashboard', '/agent']) {
      await page.goto(path)
      await expect(page).toHaveURL(/\/portal/)
    }

    await expect(page.locator('.portal-client')).toBeVisible()

    const token = await fetchApiToken(request, PORTAL_EMAIL, PORTAL_PASSWORD)
    await expectApiDenied(request, token, '/api/v1/org/members?page=1&pageSize=5')
    await expectApiDenied(request, token, '/api/v1/agents?page=1&pageSize=5')
    await expectApiDenied(request, token, '/api/v1/dashboard/overview')
    const portalResp = await request.get(`${API_BASE}/api/v1/portal/apps`, {
      headers: { Authorization: token },
    })
    const portalJson = await portalResp.json()
    expect(portalJson.code).toBe(0)
  })

  test('企业管理员：可进组织管理，平台 API 被拒', async ({ page, request }) => {
    await loginAs(page, ADMIN_EMAIL, ADMIN_PASSWORD, /\/dashboard/)
    await page.goto('/org')
    await expect(page.getByTestId('org-page')).toBeVisible()
    await page.goto('/platform/dashboard')
    await expect(page).toHaveURL(/\/dashboard/)

    const token = await fetchApiToken(request, ADMIN_EMAIL, ADMIN_PASSWORD)
    await expectApiDenied(request, token, '/api/v1/platform/stats')
  })

  test('平台管理员：默认进入运营概览，平台 API 可用且租户 API 被拒', async ({ page, request }) => {
    await loginAs(page, PLATFORM_EMAIL, PLATFORM_PASSWORD, /\/platform/)
    await expect(page.locator('[data-testid="platform-dashboard"]')).toBeVisible()

    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/platform\/dashboard/)

    const token = await fetchApiToken(request, PLATFORM_EMAIL, PLATFORM_PASSWORD)
    const resp = await request.get(`${API_BASE}/api/v1/platform/tenants?page=1&pageSize=5`, {
      headers: { Authorization: token },
    })
    const json = await resp.json()
    expect(json.code).toBe(0)

    const tenantResp = await request.get(`${API_BASE}/api/v1/agents?page=1&pageSize=5`, {
      headers: { Authorization: token },
    })
    expect(tenantResp.status()).toBe(403)
  })
})
