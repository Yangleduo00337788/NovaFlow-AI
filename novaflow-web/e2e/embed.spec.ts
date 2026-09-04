import { expect, test } from '@playwright/test'
import { ADMIN_EMAIL, ADMIN_PASSWORD } from './helpers/auth'

async function publishAgentForEmbed(request: import('@playwright/test').APIRequestContext) {
  const login = await request.post('/api/v1/auth/login', {
    data: { email: ADMIN_EMAIL, password: ADMIN_PASSWORD },
  })
  expect(login.ok()).toBeTruthy()
  const loginBody = await login.json()
  const token = loginBody.data.token as string

  const apps = await request.get('/api/v1/applications/options', {
    headers: { Authorization: token },
  })
  const appsBody = await apps.json()
  const appId = appsBody.data[0].id as number

  const suffix = Date.now()
  const created = await request.post('/api/v1/agents', {
    headers: { Authorization: token },
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
    headers: { Authorization: token },
    data: {},
  })
  const publishBody = await published.json()
  return {
    agentId,
    embedToken: publishBody.data.embedToken as string,
  }
}

test.describe('Embed 页面', () => {
  test('带 embedToken 可加载欢迎页', async ({ page, request }) => {
    const { agentId, embedToken } = await publishAgentForEmbed(request)
    await page.goto(`/embed/agents/${agentId}?embedToken=${encodeURIComponent(embedToken)}`)
    await expect(page.locator('.embed-chat')).toBeVisible()
    await expect(page.getByText('发送消息开始对话')).toBeVisible({ timeout: 15000 })
  })

  test('缺少 embedToken 显示错误', async ({ page }) => {
    await page.goto('/embed/agents/1')
    await expect(page.getByText(/缺少 agentId 或 embedToken/)).toBeVisible({ timeout: 10000 })
  })
})
