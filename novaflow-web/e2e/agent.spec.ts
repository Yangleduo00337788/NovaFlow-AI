import { test, expect, type APIRequestContext } from '@playwright/test'
import { endpoint, loginApi } from './helpers/api'

/** CI 是全新环境，没有预置的 DeepSeek Agent：用例自行创建与清理所需数据 */
const E2E_AGENT_NAME = 'E2E-DeepSeek 调试助手'

async function createE2EAgent(request: APIRequestContext) {
  const { auth } = await loginApi(request)
  // Agent 必须挂在应用下（后端强校验 applicationId），取演示租户预置应用的第一个
  const appsRes = await request.get(endpoint('/api/v1/applications?page=1&pageSize=10'), { headers: auth })
  expect(appsRes.ok(), `查询应用列表失败: HTTP ${appsRes.status()}`).toBeTruthy()
  const appsBody = await appsRes.json()
  const appId = appsBody?.data?.list?.[0]?.id
  expect(appId, '演示租户没有任何应用，无法挂载 E2E Agent').toBeTruthy()

  const createRes = await request.post(endpoint('/api/v1/agents'), {
    headers: auth,
    data: { agentName: E2E_AGENT_NAME, description: 'E2E 临时 Agent，用例结束即删除', agentType: 'chat', applicationId: appId },
  })
  expect(createRes.ok(), `创建 E2E Agent 失败: HTTP ${createRes.status()} ${await createRes.text()}`).toBeTruthy()
  const createBody = await createRes.json()
  return { auth, agentId: createBody?.data?.id as number }
}

async function deleteE2EAgent(request: APIRequestContext, auth: Record<string, string>, agentId: number | undefined) {
  if (!agentId) return
  await request.delete(endpoint(`/api/v1/agents/${agentId}`), { headers: auth }).catch(() => undefined)
}

test.describe('Agent Studio', () => {
  test('展示 Agent 列表并打开调试面板', async ({ page, request }) => {
    const { auth, agentId } = await createE2EAgent(request)
    try {
      await page.goto('/agent')
      await expect(page.getByTestId('agent-page')).toBeVisible()
      await expect(page.getByTestId('agent-table')).toBeVisible()

      const search = page.getByTestId('agent-search')
      await search.fill('DeepSeek')
      await search.press('Enter')
      const row = page.getByRole('row', { name: /DeepSeek/ })
      await expect(row).toBeVisible({ timeout: 10000 })
      await row.getByRole('button', { name: '调试' }).click()

      const panel = page.getByTestId('agent-debug-panel')
      await expect(panel).toBeVisible()
      await expect(panel.getByTestId('debug-input')).toBeVisible()

      const assistant = panel.getByTestId('debug-message-assistant').first()
      const hasWelcome = await assistant.isVisible({ timeout: 15000 }).catch(() => false)
      if (!hasWelcome) {
        // 模型 API Key 未配置或加密密钥不匹配时，欢迎语接口会失败；此时仅验证调试面板 UI
        await expect(panel.getByText('调试对话')).toBeVisible()
        return
      }

      await panel.getByTestId('debug-input').fill('你好，请介绍一下自己')
      await panel.getByTestId('debug-send').click()

      await expect(panel.getByTestId('debug-message-user')).toContainText('你好')
      await expect(panel.getByTestId('debug-message-assistant').nth(1)).toBeVisible({ timeout: 60000 })
    } finally {
      await deleteE2EAgent(request, auth, agentId)
    }
  })

  test('编辑 Agent 时显示右侧调试面板', async ({ page }) => {
    await page.goto('/agent')
    const editBtn = page.locator('[data-testid^="edit-agent-"]').first()
    await editBtn.click()

    await expect(page.getByTestId('agent-debug-panel')).toBeVisible()
    await expect(page.getByTestId('agent-name-input')).not.toHaveValue('')
  })
})
