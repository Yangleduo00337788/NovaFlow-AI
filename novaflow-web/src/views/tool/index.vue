<template>
  <div class="tool-page" data-testid="tool-page">
    <div class="page-header">
      <div>
        <h1>工具市场</h1>
        <p>{{ activeTab === 'http' ? '注册可复用的 HTTP 工具，供多个 Agent 共享调用' : '注册 MCP Server（command/args/env），连接后自动发现工具' }}</p>
      </div>
      <a-button type="primary" data-testid="create-tool-btn" @click="onCreateClick">
        <PlusOutlined />
        {{ activeTab === 'http' ? '注册工具' : '注册 MCP' }}
      </a-button>
    </div>

    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="http" tab="HTTP 工具">
    <div class="toolbar page-card">
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索工具名称或标识"
        style="width: 280px"
        allow-clear
        @search="loadData"
      />
      <span class="toolbar-meta">共 {{ total }} 个工具</span>
    </div>

    <a-spin :spinning="loading">
      <div v-if="list.length" class="tool-grid">
        <div
          v-for="item in list"
          :key="item.id"
          class="tool-card page-card"
          :data-testid="`tool-card-${item.id}`"
        >
          <div class="tool-card-head">
            <div class="tool-icon">
              <ApiOutlined />
            </div>
            <div class="tool-title-wrap">
              <h3>{{ item.displayName }}</h3>
              <p class="tool-name">{{ item.toolName }}</p>
            </div>
          </div>
          <p class="tool-desc">{{ item.description || '暂无描述' }}</p>
          <div class="tool-meta">
            <a-tag>{{ item.method || 'GET' }}</a-tag>
            <span class="tool-url" :title="item.url">{{ item.url }}</span>
          </div>
          <div class="tool-footer">
            <span class="tool-time">{{ formatDateTime(item.updatedAt) }}</span>
          </div>
          <div class="tool-actions">
            <a-button type="link" size="small" @click="openTest(item)">测试</a-button>
            <a-button type="link" size="small" @click="openEdit(item)">编辑</a-button>
            <a-popconfirm title="确认删除该工具？" @confirm="onDelete(item.id)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>
      <a-empty v-else description="暂无工具，点击右上角注册" />
    </a-spin>

    <div v-if="total > pageSize" class="pagination-wrap">
      <a-pagination
        v-model:current="page"
        :total="total"
        :page-size="pageSize"
        show-less-items
        @change="loadData"
      />
    </div>
      </a-tab-pane>

      <a-tab-pane key="mcp" tab="MCP 服务">
        <div class="toolbar page-card">
          <a-input-search
            v-model:value="mcpKeyword"
            placeholder="搜索 MCP 服务名称"
            style="width: 280px"
            allow-clear
            @search="loadMcpData"
          />
          <span class="toolbar-meta">共 {{ mcpTotal }} 个服务</span>
        </div>

        <a-spin :spinning="mcpLoading">
          <div v-if="mcpList.length" class="tool-grid">
            <div v-for="item in mcpList" :key="item.id" class="tool-card page-card">
              <div class="tool-card-head">
                <div class="tool-icon mcp">
                  <ApiOutlined />
                </div>
                <div class="tool-title-wrap">
                  <h3>{{ item.serverName }}</h3>
                  <p class="tool-name">{{ item.transportType }}</p>
                </div>
              </div>
              <p class="tool-desc">{{ item.description || '暂无描述' }}</p>
              <div class="tool-meta">
                <a-tag :color="item.status === 1 ? 'success' : item.status === 2 ? 'error' : 'default'">
                  {{ item.statusLabel }}
                </a-tag>
                <span class="tool-url" :title="item.commandSummary">{{ item.commandSummary }}</span>
              </div>
              <div class="tool-footer">
                <span class="tool-time">工具 {{ item.toolCount }} 个 · {{ formatDateTime(item.updatedAt) }}</span>
              </div>
              <div class="tool-actions">
                <a-button type="link" size="small" :loading="mcpConnectingId === item.id" @click="onMcpConnect(item)">
                  连接测试
                </a-button>
                <a-button type="link" size="small" :disabled="!item.toolCount" @click="openMcpTools(item)">
                  工具列表
                </a-button>
                <a-popconfirm title="确认删除该 MCP 服务？" @confirm="onMcpDelete(item.id)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </div>
            </div>
          </div>
          <a-empty v-else description="暂无 MCP 服务，点击右上角注册" />
        </a-spin>
      </a-tab-pane>
    </a-tabs>

    <a-drawer
      v-model:open="drawerOpen"
      :title="editingId ? '编辑工具' : '注册工具'"
      :width="640"
      @close="resetForm"
    >
      <a-form layout="vertical" :model="form">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="工具标识" required>
              <a-input
                v-model:value="form.toolName"
                placeholder="get_weather"
                :disabled="!!editingId"
                data-testid="tool-name-input"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="显示名称" required>
              <a-input v-model:value="form.displayName" placeholder="天气查询" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="帮助模型判断何时调用此工具" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="请求方法" required>
              <a-select v-model:value="form.method" :options="methodOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="18">
            <a-form-item label="请求 URL" required>
              <a-input v-model:value="form.url" placeholder="https://api.example.com/weather?city={{city}}" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="isBodyMethod(form.method)" label="Body 模板">
          <a-textarea
            v-model:value="form.bodyTemplate"
            :rows="4"
            placeholder='{"city":"{{city}}"}'
          />
        </a-form-item>
        <a-form-item label="参数 Schema（OpenAI JSON Schema）">
          <a-textarea
            v-model:value="inputSchemaJson"
            :rows="6"
            placeholder='{"type":"object","properties":{"city":{"type":"string"}},"required":["city"]}'
          />
        </a-form-item>
        <a-button type="primary" :loading="saving" data-testid="save-tool-btn" @click="onSave">保存</a-button>
      </a-form>
    </a-drawer>

    <a-modal
      v-model:open="testOpen"
      title="测试工具"
      :footer="null"
      width="720px"
      @cancel="resetTest"
    >
      <div v-if="testingTool" class="test-modal">
        <p class="test-target">{{ testingTool.displayName }}（{{ testingTool.toolName }}）</p>
        <a-textarea
          v-model:value="testArgsJson"
          :rows="6"
          placeholder='{"city":"北京"}'
        />
        <a-button type="primary" :loading="testLoading" style="margin-top: 12px" @click="runTest">
          执行测试
        </a-button>
        <div v-if="testResult" class="test-result">
          <a-alert
            :type="testResult.success ? 'success' : 'error'"
            :message="testResult.success ? '调用成功' : '调用失败'"
            show-icon
            style="margin-top: 16px"
          />
          <pre class="result-box">{{ testResult.success ? testResult.result : testResult.error }}</pre>
        </div>
      </div>
    </a-modal>

    <a-drawer v-model:open="mcpDrawerOpen" title="注册 MCP 服务" :width="640" @close="resetMcpForm">
      <a-form layout="vertical" :model="mcpForm">
        <a-form-item label="服务名称" required>
          <a-input v-model:value="mcpForm.serverName" placeholder="example-server" />
          <div class="form-hint">若使用 mcpServers 完整配置，名称需与 JSON 中的 key 一致</div>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="mcpForm.description" :rows="2" placeholder="图像生成 MCP 服务" />
        </a-form-item>
        <a-form-item label="服务配置（JSON）" required>
          <a-textarea
            v-model:value="mcpConfigJson"
            :rows="14"
            placeholder="粘贴 mcpServers 配置"
          />
          <div class="form-hint">
            支持 Cursor 风格配置，例如 command / args / env；也可直接粘贴 mcpServers 包裹格式
          </div>
        </a-form-item>
        <a-button type="primary" block :loading="mcpSaving" @click="onMcpSave">保存</a-button>
      </a-form>
    </a-drawer>

    <a-drawer
      v-model:open="mcpToolsDrawerOpen"
      :title="mcpToolsServer ? `${mcpToolsServer.serverName} · 工具列表` : '工具列表'"
      :width="640"
      @close="resetMcpTools"
    >
      <a-spin :spinning="mcpToolsLoading">
        <a-empty v-if="!mcpTools.length" description="暂无已发现工具，请先执行连接测试" />
        <div v-else class="mcp-tools-list">
          <div v-for="tool in mcpTools" :key="tool.name" class="mcp-tool-item page-card">
            <div class="mcp-tool-head">
              <h4>{{ tool.name }}</h4>
              <p>{{ tool.description || '暂无描述' }}</p>
            </div>
            <pre v-if="tool.inputSchema" class="mcp-tool-schema">{{ formatSchema(tool.inputSchema) }}</pre>
          </div>
        </div>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ApiOutlined, PlusOutlined } from '@ant-design/icons-vue'
import {
  createTool,
  deleteTool,
  fetchTools,
  testTool,
  updateTool,
  type ToolDefinition,
  type ToolSaveRequest,
} from '@/api/tool'
import {
  connectMcpServer,
  createMcpServer,
  deleteMcpServer,
  fetchMcpServerDetail,
  fetchMcpServers,
  type McpDiscoveredTool,
  type McpServer,
} from '@/api/mcp'
import { formatDateTime } from '@/utils/datetime'

const activeTab = ref('http')
const loading = ref(false)
const saving = ref(false)
const list = ref<ToolDefinition[]>([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)

const drawerOpen = ref(false)
const editingId = ref<number | null>(null)
const inputSchemaJson = ref('')
const form = reactive<ToolSaveRequest>({
  toolName: '',
  displayName: '',
  description: '',
  toolType: 'http',
  method: 'GET',
  url: '',
  bodyTemplate: '',
})

const methodOptions = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'PATCH', label: 'PATCH' },
]

const testOpen = ref(false)
const testingTool = ref<ToolDefinition | null>(null)
const testArgsJson = ref('{}')
const testLoading = ref(false)
const testResult = ref<{ success: boolean; result?: string; error?: string } | null>(null)

const mcpLoading = ref(false)
const mcpSaving = ref(false)
const mcpList = ref<McpServer[]>([])
const mcpKeyword = ref('')
const mcpPage = ref(1)
const mcpTotal = ref(0)
const mcpDrawerOpen = ref(false)
const mcpConnectingId = ref<number | null>(null)
const mcpToolsDrawerOpen = ref(false)
const mcpToolsLoading = ref(false)
const mcpToolsServer = ref<McpServer | null>(null)
const mcpTools = ref<McpDiscoveredTool[]>([])
const mcpToolsCache = ref<Record<number, McpDiscoveredTool[]>>({})
const mcpForm = reactive({
  serverName: '',
  description: '',
})
const mcpConfigJson = ref(`{
  "mcpServers": {
    "example-server": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-server-example"
      ]
    }
  }
}`)

function onCreateClick() {
  if (activeTab.value === 'mcp') {
    resetMcpForm()
    mcpDrawerOpen.value = true
    return
  }
  openCreate()
}

function onTabChange(key: string | number) {
  if (key === 'mcp' && !mcpList.value.length) {
    loadMcpData()
  }
}

function resetMcpForm() {
  Object.assign(mcpForm, {
    serverName: '',
    description: '',
  })
  mcpConfigJson.value = `{
  "mcpServers": {
    "example-server": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-server-example"
      ]
    }
  }
}`
}

async function loadMcpData() {
  mcpLoading.value = true
  try {
    const res = await fetchMcpServers({
      page: mcpPage.value,
      pageSize: 12,
      keyword: mcpKeyword.value || undefined,
    })
    mcpList.value = res.data.data.list
    mcpTotal.value = res.data.data.total
  } finally {
    mcpLoading.value = false
  }
}

async function onMcpConnect(item: McpServer) {
  mcpConnectingId.value = item.id
  try {
    const res = await connectMcpServer(item.id)
    const result = res.data.data
    if (result.status === 1) {
      if (result.tools?.length) {
        mcpToolsCache.value[item.id] = result.tools
      }
      message.success(result.message || `连接成功，发现 ${result.toolCount} 个工具`)
    } else {
      message.error(result.message || '连接失败')
    }
    loadMcpData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '连接失败')
    loadMcpData()
  } finally {
    mcpConnectingId.value = null
  }
}

async function openMcpTools(item: McpServer) {
  mcpToolsServer.value = item
  mcpToolsDrawerOpen.value = true
  mcpTools.value = mcpToolsCache.value[item.id] || []
  mcpToolsLoading.value = true
  try {
    const res = await fetchMcpServerDetail(item.id)
    mcpToolsServer.value = res.data.data
    const tools = res.data.data.tools || []
    mcpTools.value = tools
    if (tools.length) {
      mcpToolsCache.value[item.id] = tools
    }
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载工具列表失败')
    mcpTools.value = []
  } finally {
    mcpToolsLoading.value = false
  }
}

function resetMcpTools() {
  mcpToolsServer.value = null
  mcpTools.value = []
}

function formatSchema(schema: Record<string, unknown>) {
  return JSON.stringify(schema, null, 2)
}

async function onMcpSave() {
  if (!mcpForm.serverName.trim()) {
    message.warning('请填写服务名称')
    return
  }
  if (!mcpConfigJson.value.trim()) {
    message.warning('请填写 MCP 服务配置')
    return
  }
  try {
    JSON.parse(mcpConfigJson.value)
  } catch {
    message.error('服务配置 JSON 格式不正确')
    return
  }
  mcpSaving.value = true
  try {
    await createMcpServer({
      serverName: mcpForm.serverName.trim(),
      description: mcpForm.description.trim() || undefined,
      serverConfig: mcpConfigJson.value.trim(),
    })
    message.success('MCP 服务已注册')
    mcpDrawerOpen.value = false
    loadMcpData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '注册失败')
  } finally {
    mcpSaving.value = false
  }
}

async function onMcpDelete(id: number) {
  try {
    await deleteMcpServer(id)
    message.success('已删除')
    loadMcpData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

function isBodyMethod(method?: string) {
  const normalized = (method || 'GET').toUpperCase()
  return normalized === 'POST' || normalized === 'PUT' || normalized === 'PATCH'
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchTools({ page: page.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
    list.value = res.data.data.list
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  inputSchemaJson.value = ''
  Object.assign(form, {
    toolName: '',
    displayName: '',
    description: '',
    toolType: 'http',
    method: 'GET',
    url: '',
    bodyTemplate: '',
  })
}

function openCreate() {
  resetForm()
  drawerOpen.value = true
}

function openEdit(item: ToolDefinition) {
  editingId.value = item.id
  Object.assign(form, {
    toolName: item.toolName,
    displayName: item.displayName,
    description: item.description || '',
    toolType: item.toolType || 'http',
    method: item.method || 'GET',
    url: item.url || '',
    bodyTemplate: item.bodyTemplate || '',
  })
  inputSchemaJson.value = item.inputSchema ? JSON.stringify(item.inputSchema, null, 2) : ''
  drawerOpen.value = true
}

function openTest(item: ToolDefinition) {
  testingTool.value = item
  testArgsJson.value = '{}'
  testResult.value = null
  testOpen.value = true
}

function resetTest() {
  testingTool.value = null
  testResult.value = null
}

async function onSave() {
  if (!form.toolName?.trim() || !form.displayName?.trim() || !form.url?.trim()) {
    message.warning('请填写工具标识、显示名称和 URL')
    return
  }
  let inputSchema: Record<string, unknown> | undefined
  if (inputSchemaJson.value.trim()) {
    try {
      inputSchema = JSON.parse(inputSchemaJson.value) as Record<string, unknown>
    } catch {
      message.error('参数 Schema JSON 格式不正确')
      return
    }
  }
  saving.value = true
  try {
    const payload: ToolSaveRequest = {
      ...form,
      toolName: form.toolName.trim(),
      displayName: form.displayName.trim(),
      inputSchema,
    }
    if (editingId.value) {
      await updateTool(editingId.value, payload)
      message.success('更新成功')
    } else {
      await createTool(payload)
      message.success('注册成功')
    }
    drawerOpen.value = false
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete(id: number) {
  try {
    await deleteTool(id)
    message.success('删除成功')
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function runTest() {
  if (!testingTool.value) return
  let args: Record<string, unknown> = {}
  if (testArgsJson.value.trim()) {
    try {
      args = JSON.parse(testArgsJson.value) as Record<string, unknown>
    } catch {
      message.error('测试参数 JSON 格式不正确')
      return
    }
  }
  testLoading.value = true
  try {
    const res = await testTool(testingTool.value.id, { arguments: args })
    testResult.value = res.data.data
  } catch (e) {
    message.error(e instanceof Error ? e.message : '测试失败')
  } finally {
    testLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.tool-page {
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header h1 {
  margin: 0 0 6px;
  font-size: 24px;
}

.page-header p {
  margin: 0;
  color: var(--text-secondary);
}

.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.toolbar-meta {
  color: var(--text-secondary);
  font-size: 13px;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.tool-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.tool-card-head {
  display: flex;
  gap: 12px;
}

.tool-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(22, 119, 255, 0.1);
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.tool-icon.mcp {
  background: rgba(250, 140, 22, 0.12);
  color: #fa8c16;
}

.tool-title-wrap h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.tool-name {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
  font-family: monospace;
}

.tool-desc {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
  min-height: 40px;
}

.tool-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-url {
  flex: 1;
  font-size: 12px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-footer {
  margin-top: auto;
}

.tool-time {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tool-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 4px;
  border-top: 1px solid var(--border, #f1f5f9);
  padding-top: 8px;
}

.tool-actions :deep(.ant-btn-link) {
  padding: 0 6px;
  height: auto;
  line-height: 1.5;
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.test-target {
  margin-bottom: 8px;
  color: var(--text-secondary);
}

.result-box {
  margin-top: 12px;
  padding: 12px;
  background: var(--bg-muted, #f8fafc);
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 320px;
  overflow: auto;
  font-size: 12px;
}

.mcp-tools-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mcp-tool-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mcp-tool-head h4 {
  margin: 0 0 4px;
  font-size: 14px;
  font-family: monospace;
}

.mcp-tool-head p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.mcp-tool-schema {
  margin: 0;
  padding: 10px;
  background: var(--bg-muted, #f8fafc);
  border-radius: 8px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 200px;
  overflow: auto;
}
</style>
