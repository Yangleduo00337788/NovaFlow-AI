<template>
  <div class="agent-page" data-testid="agent-page">
    <div class="page-header">
      <div>
        <h1>Agent Studio</h1>
        <p>创建和管理 AI Agent</p>
      </div>
      <a-button type="primary" data-testid="create-agent-btn" @click="openCreate">创建 Agent</a-button>
    </div>

    <div class="page-card">
      <a-space style="margin-bottom: 16px">
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索 Agent"
          style="width: 240px"
          data-testid="agent-search"
          @search="loadData"
        />
        <a-select v-model:value="agentType" allow-clear placeholder="类型" style="width: 140px" @change="loadData">
          <a-select-option value="chat">Chat</a-select-option>
          <a-select-option value="rag">RAG</a-select-option>
          <a-select-option value="tool">Tool</a-select-option>
          <a-select-option value="workflow">Workflow</a-select-option>
        </a-select>
      </a-space>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        data-testid="agent-table"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'agentType'">
            <a-tag>{{ record.agentType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'default'">
              {{ record.status === 1 ? '已发布' : '草稿' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" :data-testid="`edit-agent-${record.id}`" @click="openEdit(record.id)">编辑</a-button>
              <a-button type="link" :data-testid="`debug-agent-${record.id}`" @click="openDebug(record.id)">调试</a-button>
              <a-popconfirm title="确认删除？" @confirm="onDelete(record.id)">
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-drawer
      v-model:open="drawerOpen"
      :title="editingId ? '编辑 Agent' : '创建 Agent'"
      :width="editingId ? 1080 : 720"
      @close="resetForm"
    >
      <div class="drawer-body" :class="{ split: !!editingId }">
        <div class="config-panel">
          <a-form layout="vertical" :model="form">
            <a-form-item label="名称" required>
              <a-input v-model:value="form.agentName" data-testid="agent-name-input" />
            </a-form-item>
            <a-form-item label="类型">
              <a-select v-model:value="form.agentType">
                <a-select-option value="chat">Chat Agent</a-select-option>
                <a-select-option value="rag">RAG Agent</a-select-option>
                <a-select-option value="tool">Tool Agent</a-select-option>
                <a-select-option value="workflow">Workflow Agent</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="描述">
              <a-textarea v-model:value="form.description" :rows="2" />
            </a-form-item>
            <a-form-item label="模型">
              <a-select
                v-model:value="form.modelConfigId"
                allow-clear
                placeholder="使用租户默认 Chat 模型"
                :loading="modelsLoading"
                :options="chatModelOptions"
              />
            </a-form-item>
            <a-form-item label="System Prompt">
              <a-textarea v-model:value="form.systemPrompt" :rows="6" />
            </a-form-item>
            <a-form-item label="欢迎语">
              <a-input v-model:value="form.welcomeMessage" />
            </a-form-item>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="Temperature">
                  <a-input-number v-model:value="form.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="Max Tokens">
                  <a-input-number v-model:value="form.maxTokens" :min="256" :max="8192" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-button type="primary" :loading="saving" data-testid="save-agent-btn" @click="onSave">保存</a-button>
          </a-form>
        </div>
        <AgentDebugPanel v-if="editingId" :agent-id="editingId" class="debug-side" />
      </div>
    </a-drawer>

    <a-drawer v-model:open="debugOnlyOpen" title="Agent 调试" width="480" @close="debugAgentId = null">
      <AgentDebugPanel :agent-id="debugAgentId" />
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import AgentDebugPanel from '@/components/agent/AgentDebugPanel.vue'
import { createAgent, deleteAgent, fetchAgent, fetchAgents, updateAgent, type AgentItem, type AgentSaveRequest } from '@/api/agent'
import { fetchModelConfigs, type ModelConfigItem } from '@/api/model'

const loading = ref(false)
const saving = ref(false)
const modelsLoading = ref(false)
const chatModels = ref<ModelConfigItem[]>([])
const list = ref<AgentItem[]>([])
const keyword = ref('')
const agentType = ref<string>()
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const drawerOpen = ref(false)
const debugOnlyOpen = ref(false)
const editingId = ref<number | null>(null)
const debugAgentId = ref<number | null>(null)

const form = reactive<AgentSaveRequest>({
  agentName: '',
  agentType: 'chat',
  description: '',
  systemPrompt: '',
  welcomeMessage: '',
  temperature: 0.7,
  maxTokens: 2048,
  memoryType: 'window',
  memoryWindow: 10,
  modelConfigId: undefined,
})

const chatModelOptions = computed(() =>
  chatModels.value
    .filter((item) => item.enabled)
    .map((item) => ({
      value: item.id,
      label: `${item.displayName} (${item.providerName})`,
    })),
)

const columns = [
  { title: '名称', dataIndex: 'agentName', key: 'agentName' },
  { title: '类型', key: 'agentType' },
  { title: '状态', key: 'status' },
  { title: '版本', dataIndex: 'version', key: 'version' },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt' },
  { title: '操作', key: 'action', width: 220 },
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (t: number) => `共 ${t} 条`,
})

async function loadData() {
  loading.value = true
  try {
    const res = await fetchAgents({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      agentType: agentType.value,
    })
    list.value = res.data.data.list
    total.value = res.data.data.total
    pagination.total = res.data.data.total
    pagination.current = res.data.data.page
  } finally {
    loading.value = false
  }
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 10
  loadData()
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    agentName: '',
    agentType: 'chat',
    description: '',
    systemPrompt: '',
    welcomeMessage: '',
    temperature: 0.7,
    maxTokens: 2048,
    memoryType: 'window',
    memoryWindow: 10,
    modelConfigId: undefined,
  })
}

async function loadChatModels() {
  modelsLoading.value = true
  try {
    const res = await fetchModelConfigs({ modelType: 'chat' })
    chatModels.value = res.data.data
  } finally {
    modelsLoading.value = false
  }
}

function openCreate() {
  resetForm()
  loadChatModels()
  drawerOpen.value = true
}

async function openEdit(id: number) {
  loadChatModels()
  const res = await fetchAgent(id)
  const data = res.data.data
  editingId.value = id
  Object.assign(form, data)
  drawerOpen.value = true
}

function openDebug(id: number) {
  debugAgentId.value = id
  debugOnlyOpen.value = true
}

async function onSave() {
  if (!form.agentName) {
    message.warning('请输入 Agent 名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateAgent(editingId.value, form)
      message.success('更新成功')
    } else {
      await createAgent(form)
      message.success('创建成功')
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
  await deleteAgent(id)
  message.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.agent-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h1 {
  margin: 0 0 4px;
}

.page-header p {
  margin: 0;
  color: #64748b;
}

.drawer-body {
  height: 100%;
}

.drawer-body.split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  margin: -24px;
  min-height: calc(100vh - 120px);
}

.config-panel {
  padding: 24px;
  overflow-y: auto;
}

.debug-side {
  min-height: 100%;
}
</style>
