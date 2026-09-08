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
    token,
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

  test('域名白名单拦截非授权来源', async ({ page, request }) => {
    const { agentId, embedToken, token } = await publishAgentForEmbed(request)
    const auth = { Authorization: token }
    const blocked = await request.put(`/api/v1/agents/${agentId}/embed-config`, {
      headers: auth,
      data: {
        themeColor: '#6366f1',
        allowedDomains: ['partner.example'],
        postMessageTargetOrigin: '*',
      },
    })
    expect(blocked.ok()).toBeTruthy()

    await page.goto(`/embed/agents/${agentId}?embedToken=${encodeURIComponent(embedToken)}`)
    await expect(page.getByTestId('embed-error')).toBeVisible({ timeout: 15000 })
    await expect(page.getByTestId('embed-error')).toContainText(/白名单|来源域名/)

    const allowed = await request.put(`/api/v1/agents/${agentId}/embed-config`, {
      headers: auth,
      data: {
        themeColor: '#6366f1',
        allowedDomains: ['localhost'],
        postMessageTargetOrigin: '*',
      },
    })
    expect(allowed.ok()).toBeTruthy()
    await page.reload()
    await expect(page.getByText('Embed welcome')).toBeVisible({ timeout: 15000 })
  })

  test('缺少 embedToken 显示错误', async ({ page }) => {
    await page.goto('/embed/agents/1')
    await expect(page.getByText(/缺少 agentId 或 embedToken/)).toBeVisible({ timeout: 10000 })
  })

  test('welcome 应用主题色 CSS 变量', async ({ page, request }) => {
    const { agentId, embedToken, token } = await publishAgentForEmbed(request)
    const auth = { Authorization: token }
    const themeColor = '#ff5500'
    const saved = await request.put(`/api/v1/agents/${agentId}/embed-config`, {
      headers: auth,
      data: {
        themeColor,
        allowedDomains: [],
        postMessageTargetOrigin: '*',
      },
    })
    expect(saved.ok()).toBeTruthy()

    const welcomeCheck = await request.get(`/api/v1/open/agents/${agentId}/welcome`, {
      headers: { 'X-Embed-Token': embedToken },
    })
    const welcomeBody = await welcomeCheck.json()
    expect(welcomeBody.code).toBe(0)
    expect(String(welcomeBody.data.embedThemeColor).toLowerCase()).toBe(themeColor)

    await page.goto(`/embed/agents/${agentId}?embedToken=${encodeURIComponent(embedToken)}`)
    await expect(page.getByText('Embed welcome')).toBeVisible({ timeout: 15000 })
    await page.waitForFunction(
      (expected) => {
        const inline = document.documentElement.style.getPropertyValue('--embed-primary').trim().toLowerCase()
        const computed = getComputedStyle(document.documentElement).getPropertyValue('--embed-primary').trim().toLowerCase()
        return inline === expected || computed === expected
      },
      themeColor,
      { timeout: 15000 },
    )
  })

  test('向宿主页 postMessage ready 事件', async ({ page, request, baseURL }) => {
    const { agentId, embedToken, token, agentName } = await publishAgentForEmbed(request)
    const auth = { Authorization: token }
    const saved = await request.put(`/api/v1/agents/${agentId}/embed-config`, {
      headers: auth,
      data: {
        themeColor: '#6366f1',
        allowedDomains: [],
        postMessageTargetOrigin: '*',
      },
    })
    expect(saved.ok()).toBeTruthy()

    const embedUrl = `${baseURL}/embed/agents/${agentId}?embedToken=${encodeURIComponent(embedToken)}`
    await page.goto('/')
    await page.evaluate((url) => {
      document.body.innerHTML = ''
      ;(window as { embedEvents?: Array<Record<string, unknown>> }).embedEvents = []
      window.addEventListener('message', (event) => {
        if (event.data?.source === 'novaflow-embed') {
          ;(window as { embedEvents?: Array<Record<string, unknown>> }).embedEvents?.push(event.data)
        }
      })
      const iframe = document.createElement('iframe')
      iframe.src = url
      iframe.style.width = '480px'
      iframe.style.height = '720px'
      document.body.appendChild(iframe)
    }, embedUrl)

    await page.waitForFunction(
      () => {
        const events = (window as { embedEvents?: Array<{ type?: string }> }).embedEvents ?? []
        return events.some((event) => event.type === 'ready')
      },
      { timeout: 30000 },
    )

    const readyEvent = await page.evaluate(() => {
      const events = (window as { embedEvents?: Array<Record<string, unknown>> }).embedEvents ?? []
      return events.find((event) => event.type === 'ready')
    })
    expect(readyEvent?.source).toBe('novaflow-embed')
    expect(readyEvent?.type).toBe('ready')
    expect(readyEvent?.agentId).toBe(agentId)
    expect((readyEvent?.payload as { agentName?: string })?.agentName).toBe(agentName)
  })
})
