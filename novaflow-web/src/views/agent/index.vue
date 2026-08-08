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
            <a-tag :color="statusColor(record.status)">
              {{ statusLabel(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'updatedAt'">
            {{ formatDateTime(record.updatedAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" :data-testid="`edit-agent-${record.id}`" @click="openEdit(record.id)">编辑</a-button>
              <a-button type="link" :data-testid="`debug-agent-${record.id}`" @click="openDebug(record.id)">调试</a-button>
              <a-button
                v-if="record.agentType === 'chat' || record.agentType === 'rag' || record.agentType === 'tool'"
                type="link"
                @click="openPublish(record.id)"
              >
                {{ record.status === 1 ? '发布管理' : '发布' }}
              </a-button>
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
            <a-form-item required>
              <template #label>
                <FormLabelTip label="名称" :tip="AGENT_FIELD_TIPS.agentName" />
              </template>
              <a-input v-model:value="form.agentName" data-testid="agent-name-input" />
            </a-form-item>
            <a-form-item>
              <template #label>
                <FormLabelTip label="类型" :tip="AGENT_FIELD_TIPS.agentType" />
              </template>
              <a-select v-model:value="form.agentType">
                <a-select-option value="chat">Chat Agent</a-select-option>
                <a-select-option value="rag">RAG Agent</a-select-option>
                <a-select-option value="tool">Tool Agent</a-select-option>
                <a-select-option value="workflow">Workflow Agent</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item>
              <template #label>
                <FormLabelTip label="描述" :tip="AGENT_FIELD_TIPS.description" />
              </template>
              <a-textarea v-model:value="form.description" :rows="2" />
            </a-form-item>
            <a-form-item>
              <template #label>
                <FormLabelTip label="模型" :tip="AGENT_FIELD_TIPS.modelConfigId" />
              </template>
              <a-select
                v-model:value="form.modelConfigId"
                allow-clear
                placeholder="使用租户默认 Chat 模型"
                :loading="modelsLoading"
                :options="chatModelOptions"
              />
            </a-form-item>
            <a-form-item v-if="form.agentType === 'rag'" required>
              <template #label>
                <FormLabelTip label="关联知识库" :tip="AGENT_FIELD_TIPS.knowledgeBaseIds" />
              </template>
              <a-select
                v-model:value="form.knowledgeBaseIds"
                mode="multiple"
                allow-clear
                placeholder="选择要检索的知识库"
                :loading="knowledgeBasesLoading"
                :options="knowledgeBaseOptions"
              />
            </a-form-item>
            <a-row v-if="form.agentType === 'rag'" :gutter="16">
              <a-col :span="12">
                <a-form-item>
                  <template #label>
                    <FormLabelTip label="检索 Top-K" :tip="AGENT_FIELD_TIPS.retrievalTopK" />
                  </template>
                  <a-input-number v-model:value="form.retrievalTopK" :min="1" :max="20" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item>
                  <template #label>
                    <FormLabelTip label="相似度阈值" :tip="AGENT_FIELD_TIPS.retrievalScoreThreshold" />
                  </template>
                  <a-input-number
                    v-model:value="form.retrievalScoreThreshold"
                    :min="0"
                    :max="1"
                    :step="0.05"
                    style="width: 100%"
                    placeholder="留空不限制"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row v-if="form.agentType === 'rag'" :gutter="16">
              <a-col :span="8">
                <a-form-item>
                  <template #label>
                    <FormLabelTip label="启用 Rerank" :tip="AGENT_FIELD_TIPS.rerankEnabled" />
                  </template>
                  <a-switch v-model:checked="form.rerankEnabled" />
                </a-form-item>
              </a-col>
              <a-col :span="16">
                <a-form-item v-if="form.rerankEnabled" label="Rerank 模型">
                  <a-select
                    v-model:value="form.rerankModel"
                    allow-clear
                    placeholder="选择 Rerank 模型"
                    :loading="rerankModelsLoading"
                    :options="rerankModelOptions"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-alert
              v-if="form.agentType === 'rag' && form.rerankEnabled && !rerankModelsLoading && rerankModelOptions.length === 0"
              type="warning"
              show-icon
              message="未找到可用的 Rerank 模型"
              description="请先到「模型中心」同步提供商，并启用 rerank 类型模型（如 Jina Reranker、BGE-Reranker）。"
              class="rerank-hint"
            />
            <a-alert
              v-else-if="form.agentType === 'rag' && form.rerankEnabled && !form.rerankModel"
              type="info"
              show-icon
              message="请选择 Rerank 模型"
              description="开启后会在向量召回结果上进行重排序，通常能提升 RAG 回答相关性。保存前需选定模型。"
              class="rerank-hint"
            />
            <a-row v-if="form.agentType === 'rag'" :gutter="16">
              <a-col :span="8">
                <a-form-item>
                  <template #label>
                    <FormLabelTip label="混合检索" :tip="AGENT_FIELD_TIPS.hybridEnabled" />
                  </template>
                  <a-switch v-model:checked="form.hybridEnabled" />
                </a-form-item>
              </a-col>
              <a-col :span="16">
                <a-form-item v-if="form.hybridEnabled" label="向量权重">
                  <a-slider
                    v-model:value="form.hybridAlpha"
                    :min="0"
                    :max="1"
                    :step="0.05"
                    :tip-formatter="(value?: number) => `向量 ${((value ?? 0.7) * 100).toFixed(0)}%`"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <template v-if="form.agentType === 'tool'">
              <a-form-item>
                <template #label>
                  <FormLabelTip label="关联工具" :tip="AGENT_FIELD_TIPS.toolIds" />
                </template>
                <a-select
                  v-model:value="form.toolIds"
                  mode="multiple"
                  allow-clear
                  placeholder="从工具市场选择 HTTP 工具"
                  :loading="toolsLoading"
                  :options="toolOptions"
                />
                <div class="tool-market-hint">
                  还没有工具？
                  <router-link to="/tool" target="_blank">前往工具市场注册</router-link>
                </div>
              </a-form-item>
            </template>
            <a-form-item>
              <template #label>
                <FormLabelTip label="System Prompt" :tip="AGENT_FIELD_TIPS.systemPrompt" />
              </template>
              <a-textarea v-model:value="form.systemPrompt" :rows="6" />
            </a-form-item>
            <a-form-item>
              <template #label>
                <FormLabelTip label="欢迎语" :tip="AGENT_FIELD_TIPS.welcomeMessage" />
              </template>
              <a-input v-model:value="form.welcomeMessage" />
            </a-form-item>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item>
                  <template #label>
                    <FormLabelTip label="Temperature" :tip="AGENT_FIELD_TIPS.temperature" />
                  </template>
                  <a-input-number v-model:value="form.temperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item>
                  <template #label>
                    <FormLabelTip label="Max Tokens" :tip="AGENT_FIELD_TIPS.maxTokens" />
                  </template>
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

    <a-drawer
      v-model:open="debugOnlyOpen"
      title="Agent 调试"
      :width="debugDrawerWidth"
      class="debug-only-drawer"
      :body-style="debugDrawerBodyStyle"
      @close="onDebugDrawerClose"
    >
      <AgentDebugPanel
        :agent-id="debugAgentId"
        :wide="debugWideLayout"
        show-layout-toggle
        @toggle-layout="debugWideLayout = $event"
      />
    </a-drawer>

    <a-modal
      v-model:open="publishModalOpen"
      title="Agent 发布与 API"
      :width="720"
      :footer="null"
      @cancel="closePublishModal"
    >
      <div v-if="publishInfo" class="publish-modal">
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="发布状态">
            <a-tag :color="statusColor(publishInfo.status)">{{ statusLabel(publishInfo.status) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="版本">v{{ publishInfo.version }}</a-descriptions-item>
          <a-descriptions-item v-if="publishInfo.publishedAt" label="发布时间">
            {{ formatDateTime(publishInfo.publishedAt) }}
          </a-descriptions-item>
          <a-descriptions-item v-if="publishInfo.apiKeyPrefix" label="API Key 前缀">
            <span class="key-prefix">{{ publishInfo.apiKeyPrefix }}...</span>
            <a-popconfirm
              title="轮换后旧 Key 将立即失效，需同步更新所有调用方"
              ok-text="确认轮换"
              @confirm="onRotateKey"
            >
              <a-button v-if="!revealedApiKey" type="link" size="small">轮换获取新 Key</a-button>
            </a-popconfirm>
          </a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="revealedApiKey"
          type="warning"
          show-icon
          class="key-alert"
          message="新 API Key 已生成"
          description="请立即复制并更新到调用方配置。关闭弹窗后将无法再次查看完整密钥，此前使用的旧 Key 会立即失效。"
        />
        <a-alert
          v-else-if="publishInfo.status === 1 && publishInfo.apiKeyPrefix"
          type="info"
          show-icon
          class="key-alert"
          message="完整 API Key 仅在发布或轮换时显示一次"
          description="若调用返回「API Key 无效」，请点击「轮换 API Key」生成新密钥，并同步更新 Authorization 请求头中的 Bearer Token。"
        />
        <div v-if="revealedApiKey" class="api-key-box">
          <code>{{ revealedApiKey }}</code>
          <a-button type="primary" size="small" @click="copyText(revealedApiKey)">复制 API Key</a-button>
        </div>

        <div v-if="publishInfo.status === 1" class="endpoint-section">
          <div class="section-label">API 端点</div>
          <p class="endpoint-tip">命令行或第三方系统请使用后端地址 <code>{{ apiBaseUrl }}</code></p>
          <div class="endpoint-item">
            <span>对话</span>
            <code>{{ apiBaseUrl }}{{ publishInfo.chatEndpoint }}</code>
            <a-button type="link" size="small" @click="copyText(`${apiBaseUrl}${publishInfo.chatEndpoint}`)">复制</a-button>
          </div>
          <div class="endpoint-item">
            <span>流式</span>
            <code>{{ apiBaseUrl }}{{ publishInfo.streamEndpoint }}</code>
            <a-button type="link" size="small" @click="copyText(`${apiBaseUrl}${publishInfo.streamEndpoint}`)">复制</a-button>
          </div>
          <div class="endpoint-item">
            <span>会话列表</span>
            <code>{{ apiBaseUrl }}/api/v1/open/agents/{{ publishInfo.agentId }}/conversations</code>
          </div>
          <div class="endpoint-item">
            <span>会话消息</span>
            <code>{{ apiBaseUrl }}/api/v1/open/agents/{{ publishInfo.agentId }}/conversations/messages?conversationKey=</code>
          </div>

          <div class="section-label">网页嵌入</div>
          <p class="endpoint-tip">将以下 iframe 嵌入到你的网站。API Key 会暴露在页面中，生产环境建议通过服务端代理调用。</p>
          <pre class="curl-example">{{ embedExample }}</pre>
          <a-button size="small" @click="copyText(embedExample)">复制嵌入代码</a-button>

          <div class="section-label">调用示例</div>
          <pre class="curl-example">{{ curlExample }}</pre>
          <a-space>
            <a-button size="small" type="primary" @click="copyText(curlExample)">复制 cURL</a-button>
            <span v-if="!revealedApiKey" class="curl-hint">请将 YOUR_API_KEY 替换为轮换后的最新密钥</span>
          </a-space>
        </div>

        <div class="publish-actions">
          <a-button
            v-if="publishInfo.status !== 1"
            type="primary"
            :loading="publishLoading"
            @click="onPublish"
          >
            发布并生成 API Key
          </a-button>
          <template v-else>
            <a-popconfirm
              title="轮换后旧 Key 将立即失效，需同步更新所有调用方"
              ok-text="确认轮换"
              @confirm="onRotateKey"
            >
              <a-button :loading="publishLoading">轮换 API Key</a-button>
            </a-popconfirm>
            <a-popconfirm title="确认下线该 Agent？对外 API 将立即停止" @confirm="onUnpublish">
              <a-button danger :loading="publishLoading">下线</a-button>
            </a-popconfirm>
          </template>
        </div>
      </div>
      <a-spin v-else :spinning="publishLoading" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import AgentDebugPanel from '@/components/agent/AgentDebugPanel.vue'
import FormLabelTip from '@/components/common/FormLabelTip.vue'
import {
  createAgent,
  deleteAgent,
  fetchAgent,
  fetchAgentPublishInfo,
  fetchAgents,
  publishAgent,
  rotateAgentApiKey,
  unpublishAgent,
  updateAgent,
  type AgentItem,
  type AgentPublishInfo,
  type AgentSaveRequest,
} from '@/api/agent'
import { fetchKnowledgeBases, type KnowledgeBaseItem } from '@/api/knowledge'
import { fetchModelConfigs, type ModelConfigItem } from '@/api/model'
import { fetchToolOptions, type ToolDefinition } from '@/api/tool'
import { formatDateTime } from '@/utils/datetime'

const AGENT_FIELD_TIPS = {
  agentName: 'Agent 的显示名称，会出现在列表、调试对话和对外 API 的标识中。',
  agentType:
    '决定 Agent 的能力形态。Chat 为纯对话；RAG 会先检索知识库再回答；Tool 可调用 HTTP 工具；Workflow 用于工作流编排（后续扩展）。',
  description: '简要说明 Agent 的用途，便于团队成员理解与管理，不影响模型实际行为。',
  modelConfigId: '对话所使用的大语言模型。留空时将自动使用租户默认的 Chat 模型。',
  knowledgeBaseIds: 'RAG Agent 进行向量检索的知识库，支持多选。每次提问会从中召回与问题最相关的文档分块作为参考。',
  toolIds: '从工具市场选择已注册的 HTTP 工具，支持多选。Tool Agent 将根据用户问题自动决定调用哪些工具。',
  retrievalTopK:
    '每次提问最多召回的文档分块数量。数值越大上下文越丰富，但可能引入无关内容；一般建议 3–10。',
  retrievalScoreThreshold:
    '向量相似度下限（0–1），低于该分数的分块会被过滤。留空表示不做阈值过滤，仅按 Top-K 取结果。',
  rerankEnabled:
    '开启后使用 Rerank 模型对向量召回的候选文档重新打分排序，可提升 RAG 回答质量。需先在模型中心配置 rerank 模型。',
  rerankModel: '选择已启用的 Rerank 模型，例如 Jina Reranker。',
  hybridEnabled: '在向量召回结果上叠加关键词匹配重排，改善专有名词、编号等精确匹配场景。',
  systemPrompt:
    '系统级指令，用于定义 Agent 的角色、语气、回答规范与边界。留空则不向模型发送 System Prompt；填写后才会作为系统消息传给模型。',
  welcomeMessage: '用户打开对话时首条展示的消息，调试面板与对外 API 均可使用。留空时由系统生成默认问候语。',
  temperature:
    '控制回复的随机性与创造性。越低越稳定、严谨；越高越发散、有创意，但也更容易偏离事实或产生幻觉。',
  maxTokens: '单次回复允许生成的最大 Token 数，影响回答长度、响应时间与调用成本。',
} as const

const loading = ref(false)
const saving = ref(false)
const modelsLoading = ref(false)
const rerankModelsLoading = ref(false)
const knowledgeBasesLoading = ref(false)
const chatModels = ref<ModelConfigItem[]>([])
const rerankModels = ref<ModelConfigItem[]>([])
const knowledgeBases = ref<KnowledgeBaseItem[]>([])
const marketplaceTools = ref<ToolDefinition[]>([])
const toolsLoading = ref(false)
const list = ref<AgentItem[]>([])
const keyword = ref('')
const agentType = ref<string>()
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const drawerOpen = ref(false)
const debugOnlyOpen = ref(false)
const debugWideLayout = ref(false)
const editingId = ref<number | null>(null)
const debugAgentId = ref<number | null>(null)
const publishModalOpen = ref(false)
const publishAgentId = ref<number | null>(null)
const publishInfo = ref<AgentPublishInfo | null>(null)
const publishLoading = ref(false)
const revealedApiKey = ref('')

const apiBaseUrl = import.meta.env.DEV ? 'http://localhost:8080' : window.location.origin

const debugDrawerWidth = computed(() => (debugWideLayout.value ? 1080 : 480))
const debugDrawerBodyStyle = {
  padding: '0',
  overflow: 'hidden',
  display: 'flex',
  flexDirection: 'column',
  height: '100%',
}

const curlExample = computed(() => {
  if (!publishInfo.value) return ''
  const key = revealedApiKey.value || 'YOUR_API_KEY'
  return `curl -X POST "${apiBaseUrl}${publishInfo.value.chatEndpoint}" ^
  -H "Authorization: Bearer ${key}" ^
  -H "Content-Type: application/json" ^
  -d "{\\"message\\":\\"你好\\",\\"conversationId\\":\\"conv-001\\"}"`
})

const embedExample = computed(() => {
  if (!publishInfo.value?.embedPath) return ''
  const key = revealedApiKey.value || 'YOUR_API_KEY'
  const origin = typeof window !== 'undefined' ? window.location.origin : 'https://your-domain.com'
  return `<iframe src="${origin}${publishInfo.value.embedPath}?apiKey=${key}" width="400" height="640" style="border:0;border-radius:12px;" allow="clipboard-write"></iframe>`
})

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
  retrievalTopK: 5,
  retrievalScoreThreshold: undefined,
  rerankEnabled: false,
  rerankModel: undefined,
  hybridEnabled: false,
  hybridAlpha: 0.7,
  modelConfigId: undefined,
  knowledgeBaseIds: [],
  toolIds: [],
})

const rerankModelOptions = computed(() =>
  rerankModels.value
    .filter((item) => item.enabled)
    .map((item) => ({
      value: item.modelName,
      label: `${item.displayName} (${item.providerName})`,
    })),
)

const chatModelOptions = computed(() =>
  chatModels.value
    .filter((item) => item.enabled)
    .map((item) => ({
      value: item.id,
      label: `${item.displayName} (${item.providerName})`,
    })),
)

const knowledgeBaseOptions = computed(() =>
  knowledgeBases.value.map((item) => ({
    value: item.id,
    label: `${item.kbName}（${item.chunkCount} 分块）`,
  })),
)

const toolOptions = computed(() =>
  marketplaceTools.value.map((item) => ({
    value: item.id,
    label: `${item.displayName}（${item.toolName}）`,
  })),
)

function statusLabel(status: number) {
  if (status === 1) return '已发布'
  if (status === 2) return '已下线'
  return '草稿'
}

function statusColor(status: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'warning'
  return 'default'
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制')
  } catch {
    message.error('复制失败')
  }
}

const columns = [
  { title: '名称', dataIndex: 'agentName', key: 'agentName' },
  { title: '类型', key: 'agentType' },
  { title: '状态', key: 'status' },
  { title: '版本', dataIndex: 'version', key: 'version' },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt' },
  { title: '操作', key: 'action', width: 280 },
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
    retrievalTopK: 5,
    retrievalScoreThreshold: undefined,
    rerankEnabled: false,
    rerankModel: undefined,
    hybridEnabled: false,
    hybridAlpha: 0.7,
    modelConfigId: undefined,
    knowledgeBaseIds: [],
    toolIds: [],
  })
}

async function loadMarketplaceTools() {
  toolsLoading.value = true
  try {
    const res = await fetchToolOptions()
    marketplaceTools.value = res.data.data
  } finally {
    toolsLoading.value = false
  }
}

async function loadKnowledgeBases() {
  knowledgeBasesLoading.value = true
  try {
    const res = await fetchKnowledgeBases({ page: 1, pageSize: 100 })
    knowledgeBases.value = res.data.data.list
  } finally {
    knowledgeBasesLoading.value = false
  }
}

async function loadRerankModels() {
  rerankModelsLoading.value = true
  try {
    const res = await fetchModelConfigs({ modelType: 'rerank' })
    rerankModels.value = res.data.data
  } finally {
    rerankModelsLoading.value = false
  }
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
  loadRerankModels()
  loadKnowledgeBases()
  loadMarketplaceTools()
  drawerOpen.value = true
}

async function openEdit(id: number) {
  loadChatModels()
  loadRerankModels()
  loadKnowledgeBases()
  loadMarketplaceTools()
  const res = await fetchAgent(id)
  const data = res.data.data
  editingId.value = id
  Object.assign(form, {
    ...data,
    hybridAlpha: data.hybridAlpha ?? 0.7,
    toolIds: data.toolIds || [],
  })
  drawerOpen.value = true
}

function openDebug(id: number) {
  debugWideLayout.value = false
  debugAgentId.value = id
  debugOnlyOpen.value = true
  fetchAgent(id).catch(() => {})
}

function onDebugDrawerClose() {
  debugAgentId.value = null
  debugWideLayout.value = false
}

async function openPublish(id: number) {
  publishAgentId.value = id
  publishModalOpen.value = true
  revealedApiKey.value = ''
  publishLoading.value = true
  try {
    const res = await fetchAgentPublishInfo(id)
    publishInfo.value = res.data.data
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载发布信息失败')
    publishModalOpen.value = false
  } finally {
    publishLoading.value = false
  }
}

function closePublishModal() {
  publishAgentId.value = null
  publishInfo.value = null
  revealedApiKey.value = ''
}

async function onPublish() {
  if (!publishAgentId.value) return
  publishLoading.value = true
  try {
    const res = await publishAgent(publishAgentId.value)
    publishInfo.value = res.data.data
    revealedApiKey.value = res.data.data.apiKey || ''
    message.success('发布成功，请立即复制 API Key')
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '发布失败')
  } finally {
    publishLoading.value = false
  }
}

async function onUnpublish() {
  if (!publishAgentId.value) return
  publishLoading.value = true
  try {
    const res = await unpublishAgent(publishAgentId.value)
    publishInfo.value = res.data.data
    revealedApiKey.value = ''
    message.success('已下线')
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '下线失败')
  } finally {
    publishLoading.value = false
  }
}

async function onRotateKey() {
  if (!publishAgentId.value) return
  publishLoading.value = true
  try {
    const res = await rotateAgentApiKey(publishAgentId.value)
    publishInfo.value = res.data.data
    revealedApiKey.value = res.data.data.apiKey || ''
    message.success('API Key 已轮换，旧 Key 已失效，请复制新 Key 并更新调用方')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '轮换失败')
  } finally {
    publishLoading.value = false
  }
}

async function onSave() {
  if (!form.agentName) {
    message.warning('请输入 Agent 名称')
    return
  }
  if (form.agentType === 'rag' && (!form.knowledgeBaseIds || form.knowledgeBaseIds.length === 0)) {
    message.warning('RAG Agent 请至少关联一个知识库')
    return
  }
  if (form.agentType === 'rag' && form.rerankEnabled) {
    if (!rerankModelOptions.value.length) {
      message.warning('未找到可用的 Rerank 模型，请先在模型中心配置并启用')
      return
    }
    if (!form.rerankModel) {
      message.warning('已启用 Rerank，请选择 Rerank 模型')
      return
    }
  }
  if (form.agentType === 'tool' && (!form.toolIds || form.toolIds.length === 0)) {
    message.warning('Tool Agent 请至少选择一个工具市场的 HTTP 工具')
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
  height: 100%;
}

.debug-only-drawer :deep(.ant-drawer-content-wrapper),
.debug-only-drawer :deep(.ant-drawer-content) {
  display: flex;
  flex-direction: column;
}

.debug-only-drawer :deep(.ant-drawer-body) {
  flex: 1;
  min-height: 0;
}

.tool-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tool-item {
  padding: 12px;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
  background: #fafafa;
}

.rerank-hint {
  margin-bottom: 12px;
}

.tool-market-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.tool-market-hint a {
  color: var(--primary);
}

.publish-modal {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.key-alert {
  margin-top: 4px;
}

.key-prefix {
  margin-right: 8px;
}

.api-key-box {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding: 12px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 8px;
  word-break: break-all;
}

.endpoint-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.endpoint-tip {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}

.endpoint-tip code {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
}

.section-label {
  font-weight: 600;
  margin-top: 8px;
}

.endpoint-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}

.endpoint-item > code {
  flex: 1;
  min-width: 0;
}

.curl-hint {
  font-size: 12px;
  color: #94a3b8;
}

.endpoint-item code,
.curl-example {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.publish-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}
</style>
