<template>
  <div class="tool-page" data-testid="tool-page">
    <div class="page-header">
      <div>
        <h1>工具市场</h1>
        <p>注册可复用的 HTTP 工具，供多个 Agent 共享调用</p>
      </div>
      <a-button type="primary" data-testid="create-tool-btn" @click="openCreate">
        <PlusOutlined />
        注册工具
      </a-button>
    </div>

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
import { formatDateTime } from '@/utils/datetime'

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
</style>
