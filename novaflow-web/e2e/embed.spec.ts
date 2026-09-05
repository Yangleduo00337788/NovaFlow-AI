import { expect, test } from '@playwright/test'
import { ADMIN_EMAIL, ADMIN_PASSWORD } from './helpers/auth'

async function publishAgentForEmbed(request: import('@playwright/test').APIRequestContext) {
  const login = await request.post('/api/v1/auth/login', {
    data: { email: ADMIN_EMAIL, password: ADMIN_PASSWORD },
  })
  expect(login.ok()).toBeTruthy()
  const loginBody = await login.json()
  const token = loginBody.data.token as string
  const auth = { Authorization: token }

  const suffix = Date.now()
  const appName = `E2E-Embed-App-${suffix}`
  const appRes = await request.post('/api/v1/applications', {
    headers: auth,
    data: { appName, description: 'e2e embed' },
  })
  expect(appRes.ok()).toBeTruthy()
  const appId = (await appRes.json()).data.id as number

  const created = await request.post('/api/v1/agents', {
    headers: auth,
    data: {
      agentName: `E2E-Embed-${suffix}`,
      agentType: 'chat',
      applicationId: appId,
      welcomeMessage: 'Embed welcome',
    },
  })
  const createdBody = await created.json()
  const agentId = createdBody.data.id as number

  const published = await request.post(`/api/v1/agents/${agentId}/publish`, {
    headers: auth,
    data: {},
  })
  const publishBody = await published.json()

  const bindRes = await request.put(`/api/v1/applications/${appId}`, {
    headers: auth,
    data: {
      appName,
      description: 'e2e embed',
      defaultAgentId: agentId,
      agentIds: [agentId],
    },
  })
  expect(bindRes.ok()).toBeTruthy()

  const appPublished = await request.post(`/api/v1/applications/${appId}/publish`, {
    headers: auth,
    data: {},
  })
  expect(appPublished.ok()).toBeTruthy()

  return {
    agentId,
    embedToken: publishBody.data.embedToken as string,
    agentName: `E2E-Embed-${suffix}`,
  }
}

test.describe('Embed 页面', () => {
  test('带 embedToken 可加载欢迎页', async ({ page, request }) => {
    const { agentId, embedToken, agentName } = await publishAgentForEmbed(request)
    await page.goto(`/embed/agents/${agentId}?embedToken=${encodeURIComponent(embedToken)}`)
    await expect(page.locator('.embed-chat')).toBeVisible()
    await expect(page.getByRole('heading', { name: agentName })).toBeVisible({ timeout: 15000 })
    await expect(page.getByText('Embed welcome')).toBeVisible({ timeout: 15000 })
  })

  test('缺少 embedToken 显示错误', async ({ page }) => {
    await page.goto('/embed/agents/1')
    await expect(page.getByText(/缺少 agentId 或 embedToken/)).toBeVisible({ timeout: 10000 })
  })
})
