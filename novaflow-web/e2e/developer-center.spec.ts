import { expect, test } from '@playwright/test'
import { ADMIN_EMAIL, ADMIN_PASSWORD } from './helpers/auth'

async function createPublishedAgent(request: import('@playwright/test').APIRequestContext) {
  const login = await request.post('/api/v1/auth/login', {
    data: { email: ADMIN_EMAIL, password: ADMIN_PASSWORD },
  })
  expect(login.ok()).toBeTruthy()
  const token = (await login.json()).data.token as string
  const auth = { Authorization: token }
  const suffix = Date.now()
  const appName = `E2E-DevCenter-App-${suffix}`
  const agentName = `E2E-DevCenter-Agent-${suffix}`

  const appRes = await request.post('/api/v1/applications', {
    headers: auth,
    data: { appName, description: 'developer center e2e' },
  })
  expect(appRes.ok()).toBeTruthy()
  const appId = (await appRes.json()).data.id as number

  const created = await request.post('/api/v1/agents', {
    headers: auth,
    data: {
      agentName,
      agentType: 'chat',
      applicationId: appId,
      welcomeMessage: 'Developer center welcome',
    },
  })
  expect(created.ok()).toBeTruthy()
  const agentId = (await created.json()).data.id as number

  const published = await request.post(`/api/v1/agents/${agentId}/publish`, {
    headers: auth,
    data: {},
  })
  expect(published.ok()).toBeTruthy()

  return { agentId, agentName }
}

test.describe('开发者中心发布弹窗', () => {
  test('已发布 Agent 展示 Quick Start / Open API / Embed 三 Tab', async ({ page, request }) => {
    const { agentName } = await createPublishedAgent(request)

    await page.goto('/agent')
    await expect(page.getByTestId('agent-page')).toBeVisible()
    const search = page.getByTestId('agent-search')
    await search.fill(agentName)
    await search.press('Enter')
    const row = page.locator('.ant-table-row', { hasText: agentName })
    await expect(row).toBeVisible({ timeout: 15000 })
    await row.getByRole('button', { name: '发布管理' }).click()

    const dialog = page.getByRole('dialog', { name: '发布与对外接入' })
    await expect(dialog).toBeVisible({ timeout: 15000 })
    await expect(dialog.getByRole('tab', { name: 'Quick Start' })).toBeVisible()
    await expect(dialog.getByRole('tab', { name: 'Open API' })).toBeVisible()
    await expect(dialog.getByRole('tab', { name: 'Embed' })).toBeVisible()
    await expect(dialog.getByText('凭证权限对比')).toBeVisible()

    await dialog.getByRole('tab', { name: 'Open API' }).click()
    await expect(dialog.getByText('同步对话 cURL')).toBeVisible()
    await expect(dialog.getByText('X-Caller-Id 说明')).toBeVisible()

    await dialog.getByRole('tab', { name: 'Embed' }).click()
    await expect(dialog.getByText('iframe 嵌入代码')).toBeVisible()
    await expect(dialog.getByRole('button', { name: '保存 Embed 配置' })).toBeVisible()
    await expect(dialog.getByText('postMessage 事件')).toBeVisible()
  })
})
