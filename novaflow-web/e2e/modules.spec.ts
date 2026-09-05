import { test, expect } from '@playwright/test'
import { uniqueName, confirmPopconfirm } from './helpers/auth'

test.describe('工作台', () => {
  test('展示欢迎横幅与统计卡片', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page.locator('.dashboard')).toBeVisible()
    await expect(page.getByText('欢迎回来')).toBeVisible()
    await expect(page.locator('.stat-card').first()).toBeVisible()
    await expect(page.getByText('快速开始')).toBeVisible()
  })
})

test.describe('应用管理', () => {
  test('创建并删除应用', async ({ page }) => {
    const appName = uniqueName('E2E-App')
    await page.goto('/application')
    await expect(page.getByTestId('application-page')).toBeVisible()

    await page.getByTestId('create-app-btn').click()
    await expect(page.getByTestId('app-name-input')).toBeVisible()
    await page.getByTestId('app-name-input').fill(appName)
    await page.getByTestId('save-app-btn').click()

    await expect(page.getByText(appName)).toBeVisible({ timeout: 10000 })

    const card = page.locator('.app-card', { hasText: appName })
    await card.getByRole('button', { name: '删除' }).click()
    await confirmPopconfirm(page)
    await expect(page.getByText(appName)).not.toBeVisible({ timeout: 10000 })
  })
})

test.describe('Prompt 管理', () => {
  test('创建并删除 Prompt 模板', async ({ page }) => {
    const templateName = uniqueName('E2E-Prompt')
    await page.goto('/prompt')
    await expect(page.getByTestId('prompt-page')).toBeVisible()

    await page.getByTestId('create-prompt-btn').click()
    await page.getByTestId('prompt-name-input').fill(templateName)
    await page.getByPlaceholder('你是一个专业的客服助手').fill('Hello {{name}}, welcome to NovaFlow.')
    await page.getByTestId('save-prompt-btn').click()

    await expect(page.getByText(templateName)).toBeVisible({ timeout: 10000 })

    const card = page.locator('.prompt-card', { hasText: templateName })
    await card.getByRole('button', { name: '删除' }).click()
    await confirmPopconfirm(page)
    await expect(page.getByText(templateName)).not.toBeVisible({ timeout: 10000 })
  })
})

test.describe('工作流 Studio', () => {
  test('创建工作流并进入编辑器', async ({ page }) => {
    const workflowName = uniqueName('E2E-WF')
    await page.goto('/workflow')
    await expect(page.getByTestId('workflow-page')).toBeVisible()

    await page.getByTestId('create-workflow-btn').click()
    await page.getByRole('dialog').getByPlaceholder('客服分流流程').fill(workflowName)
    const appSelect = page.getByRole('dialog').locator('.ant-select').first()
    await appSelect.click()
    await page.locator('.ant-select-item-option').first().click()
    await page.getByRole('button', { name: '创建并编辑' }).click()

    await expect(page).toHaveURL(/\/workflow\/\d+/, { timeout: 15000 })
    await expect(page.getByTestId('workflow-editor')).toBeVisible({ timeout: 15000 })
    await page.goto('/workflow')
    await expect(page.getByText(workflowName)).toBeVisible({ timeout: 10000 })

    const card = page.locator('.workflow-card', { hasText: workflowName })
    await card.getByRole('button', { name: '删除' }).click()
    await confirmPopconfirm(page)
    await expect(page.getByText(workflowName)).not.toBeVisible({ timeout: 10000 })
  })
})

test.describe('知识库 Hub', () => {
  test('创建知识库并进入详情', async ({ page }) => {
    const kbName = uniqueName('E2E-KB')
    await page.goto('/knowledge')
    await expect(page.getByTestId('knowledge-page')).toBeVisible()

    await page.getByTestId('create-kb-btn').click()
    await page.getByTestId('kb-name-input').fill(kbName)

    const embeddingSelect = page.locator('.ant-drawer .ant-select').first()
    await embeddingSelect.click()
    const firstOption = page.locator('.ant-select-item-option').first()
    const hasEmbedding = await firstOption.isVisible({ timeout: 5000 }).catch(() => false)
    if (!hasEmbedding) {
      test.skip(true, '无可用 Embedding 模型，跳过知识库创建')
      return
    }
    await firstOption.click()
    await page.locator('.ant-drawer').getByRole('button', { name: '创建知识库', exact: true }).click()

    await expect(page.getByText(kbName)).toBeVisible({ timeout: 15000 })
    await page.getByText(kbName).click()
    await expect(page.getByTestId('knowledge-detail-page')).toBeVisible({ timeout: 10000 })

    await page.goto('/knowledge')
    const card = page.locator('.kb-card', { hasText: kbName })
    await card.getByRole('button', { name: '删除' }).click()
    await confirmPopconfirm(page)
    await expect(page.getByText(kbName)).not.toBeVisible({ timeout: 10000 })
  })
})

test.describe('工具市场', () => {
  test('Skill 与 MCP 两个 Tab 可切换', async ({ page }) => {
    await page.goto('/tool')
    await expect(page.getByTestId('tool-page')).toBeVisible()
    await expect(page.getByText('Skill = 流程与知识')).toBeVisible()

    await page.getByRole('tab', { name: 'MCP 插件' }).click()
    await expect(page.getByText('MCP = 插件与连接')).toBeVisible()
    await page.getByTestId('create-tool-btn').click()
    await expect(page.locator('.ant-drawer')).toBeVisible()
  })
})

test.describe('模型中心', () => {
  test('三个 Tab 可切换', async ({ page }) => {
    await page.goto('/model')
    await expect(page.locator('.model-page')).toBeVisible()
    await expect(page.locator('.overview-grid')).toBeVisible()

    await page.locator('.model-page .ant-radio-button-wrapper').filter({ hasText: '模型列表' }).click()
    await expect(page.locator('.model-page')).toBeVisible()

    await page.locator('.model-page .ant-radio-button-wrapper').filter({ hasText: '调用统计' }).click()
    await expect(page.locator('.model-page')).toBeVisible()
  })
})

test.describe('运行监控与日志', () => {
  test('监控页展示服务状态', async ({ page }) => {
    await page.goto('/monitor')
    await expect(page.getByTestId('monitor-page')).toBeVisible()
    await expect(page.locator('[data-testid^="service-"]').first()).toBeVisible({ timeout: 15000 })
  })

  test('调用日志页可加载', async ({ page }) => {
    await page.goto('/log')
    await expect(page.locator('.log-page')).toBeVisible()
  })

  test('链路分析页可加载', async ({ page }) => {
    await page.goto('/trace')
    await expect(page.locator('.trace-page')).toBeVisible()
  })

  test('可观测性页可加载', async ({ page }) => {
    await page.goto('/observability')
    await expect(page.locator('.observability-page')).toBeVisible()
  })
})

test.describe('组织与权限', () => {
  test('组织管理页展示租户信息', async ({ page }) => {
    await page.goto('/org')
    await expect(page.getByTestId('org-page')).toBeVisible()
    await expect(page.getByRole('tab', { name: '工作空间' })).toBeVisible()
    await expect(page.getByRole('tab', { name: '部门' })).toBeVisible()
    await page.getByRole('tab', { name: '部门' }).click()
    await expect(page.getByRole('button', { name: '新建部门' })).toBeVisible()
  })

  test('权限管理页展示角色列表', async ({ page }) => {
    await page.goto('/permission')
    const permissionPage = page.getByTestId('permission-page')
    await expect(permissionPage).toBeVisible()
    await expect(permissionPage.getByText('系统角色', { exact: true })).toBeVisible()
    const roleList = permissionPage.locator('.role-item')
    await expect(roleList.filter({ hasText: '超级管理员' })).toBeVisible()
    await expect(roleList.filter({ hasText: '企业所有者' })).toBeVisible()
  })

  test('账单页可加载并打开成本分摊', async ({ page }) => {
    await page.goto('/billing')
    await expect(page.getByTestId('billing-page')).toBeVisible()
    await expect(page.getByRole('tab', { name: '成本分摊' })).toBeVisible()
    await page.getByRole('tab', { name: '成本分摊' }).click()
    await expect(page.locator('[data-testid="billing-page"] .ant-radio-button-wrapper').filter({ hasText: '应用' })).toBeVisible()
    await expect(page.locator('[data-testid="billing-page"] .ant-radio-button-wrapper').filter({ hasText: '工作空间' })).toBeVisible()
  })

  test('账单预警可配置邮件与 Webhook', async ({ page }) => {
    await page.goto('/billing')
    await page.getByRole('button', { name: '预警配置' }).click()
    await expect(page.getByText('外部通道')).toBeVisible()
    await expect(page.getByText('Webhook', { exact: true }).first()).toBeVisible()
  })
})
