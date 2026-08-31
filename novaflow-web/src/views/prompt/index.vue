<template>
  <div class="prompt-page page-shell" data-testid="prompt-page">
    <div class="page-header">
      <div>
        <h1>Prompt 管理</h1>
        <p>管理可复用的 Prompt 模板，支持版本历史与在线测试</p>
      </div>
      <a-button type="primary" data-testid="create-prompt-btn" @click="openCreate">
        <PlusOutlined />
        创建模板
      </a-button>
    </div>

    <div class="list-panel page-card">
      <div class="list-toolbar">
        <div class="list-toolbar-filters">
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索模板名称"
            style="width: 220px"
            allow-clear
            @search="loadData"
          />
          <a-select
            v-model:value="category"
            allow-clear
            placeholder="全部分类"
            style="width: 130px"
            :options="PROMPT_CATEGORIES"
            @change="loadData"
          />
        </div>
        <span class="list-toolbar-meta">共 {{ total }} 个模板</span>
      </div>

      <div class="list-body">
        <a-spin :spinning="loading">
          <div v-if="list.length" class="prompt-grid">
        <div
          v-for="item in list"
          :key="item.id"
          class="prompt-card page-card"
          :data-testid="`prompt-card-${item.id}`"
        >
          <div class="prompt-card-head">
            <div class="prompt-icon">
              <FileTextOutlined />
            </div>
            <div class="prompt-title-wrap">
              <h3>{{ item.templateName }}</h3>
              <div class="prompt-tags">
                <a-tag>{{ getCategoryLabel(item.category) }}</a-tag>
                <a-tag color="blue">v{{ item.currentVersion }}</a-tag>
              </div>
            </div>
          </div>
          <p class="prompt-desc">{{ item.description || '暂无描述' }}</p>
          <pre class="prompt-preview">{{ previewContent(item.content) }}</pre>
          <div class="prompt-footer">
            <span class="prompt-time">{{ formatDateTime(item.updatedAt) }}</span>
          </div>
          <div class="prompt-actions">
            <a-button type="link" size="small" @click="openVersions(item)">版本</a-button>
            <a-button type="link" size="small" @click="openTest(item)">测试</a-button>
            <a-button type="link" size="small" @click="openEdit(item)">编辑</a-button>
            <a-popconfirm title="确认删除该模板？" @confirm="onDelete(item.id)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>
          <a-empty v-else description="暂无模板，点击右上角创建" />
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
      </div>
    </div>

    <a-drawer
      v-model:open="drawerOpen"
      :title="editingId ? '编辑模板' : '创建模板'"
      :width="720"
      @close="resetForm"
    >
      <a-form layout="vertical" :model="form">
        <a-row :gutter="16">
          <a-col :span="14">
            <a-form-item label="模板名称" required>
              <a-input v-model:value="form.templateName" placeholder="客服话术模板" data-testid="prompt-name-input" />
            </a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item label="分类">
              <a-select v-model:value="form.category" :options="PROMPT_CATEGORIES" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="模板用途说明" />
        </a-form-item>
        <a-form-item label="Prompt 内容" required>
          <a-textarea
            v-model:value="form.content"
            :rows="10"
            placeholder="你是一个专业的客服助手。公司名称：{{company_name}}"
          />
        </a-form-item>
        <a-form-item label="变量定义">
          <div class="variable-list">
            <div v-for="(row, index) in variableRows" :key="index" class="variable-row">
              <a-input v-model:value="row.name" placeholder="变量名，如 company_name" />
              <a-input v-model:value="row.description" placeholder="说明" />
              <a-input v-model:value="row.defaultValue" placeholder="默认值" />
              <a-button type="link" danger @click="removeVariable(index)">删除</a-button>
            </div>
            <a-button type="dashed" block @click="addVariable">添加变量</a-button>
          </div>
        </a-form-item>
        <a-form-item v-if="editingId" label="变更说明">
          <a-input v-model:value="form.changeLog" placeholder="本次修改说明（内容变更时生成新版本）" />
        </a-form-item>
        <a-button type="primary" :loading="saving" data-testid="save-prompt-btn" @click="onSave">保存</a-button>
      </a-form>
    </a-drawer>

    <a-drawer v-model:open="versionOpen" title="版本历史" :width="640">
      <a-list :data-source="versions" :loading="versionsLoading">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="`v${item.version}`" :description="item.changeLog || '无变更说明'" />
            <template #actions>
              <a-button type="link" size="small" @click="previewVersion(item)">查看</a-button>
              <a-popconfirm
                v-if="currentTemplate && item.version !== currentTemplate.currentVersion"
                title="确认回滚到该版本？"
                @confirm="onRollback(item.version)"
              >
                <a-button type="link" size="small">回滚</a-button>
              </a-popconfirm>
            </template>
            <div class="version-time">{{ formatDateTime(item.publishedAt) }}</div>
          </a-list-item>
        </template>
      </a-list>
    </a-drawer>

    <a-modal v-model:open="testOpen" title="在线测试" :footer="null" width="760px" @cancel="resetTest">
      <div v-if="testingPrompt" class="test-modal">
        <p class="test-target">{{ testingPrompt.templateName }}（v{{ testingPrompt.currentVersion }}）</p>
        <a-form layout="vertical">
          <a-form-item label="测试变量（JSON）">
            <a-textarea v-model:value="testVarsJson" :rows="4" placeholder='{"company_name":"NovaFlow"}' />
          </a-form-item>
          <a-form-item label="模型（可选，填写后执行真实对话）">
            <a-select
              v-model:value="testModelId"
              allow-clear
              placeholder="选择 Chat 模型"
              :loading="modelsLoading"
              :options="chatModelOptions"
            />
          </a-form-item>
          <a-form-item label="用户消息">
            <a-textarea v-model:value="testUserMessage" :rows="3" placeholder="你好，请介绍一下你们的产品" />
          </a-form-item>
        </a-form>
        <a-button type="primary" :loading="testLoading" @click="runTest">执行测试</a-button>
        <div v-if="testResult" class="test-result">
          <div class="result-block">
            <div class="result-label">渲染后的 Prompt</div>
            <pre class="result-box">{{ testResult.renderedPrompt }}</pre>
          </div>
          <div v-if="testResult.reply" class="result-block">
            <div class="result-label">模型回复</div>
            <pre class="result-box">{{ testResult.reply }}</pre>
          </div>
        </div>
      </div>
    </a-modal>

    <a-modal v-model:open="previewOpen" title="版本内容" :footer="null" width="720px">
      <pre class="result-box">{{ previewContentText }}</pre>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { FileTextOutlined, PlusOutlined } from '@ant-design/icons-vue'
import {
  PROMPT_CATEGORIES,
  createPrompt,
  deletePrompt,
  fetchPromptVersions,
  fetchPrompts,
  getCategoryLabel,
  rollbackPrompt,
  testPrompt,
  updatePrompt,
  type PromptTemplate,
  type PromptVariable,
  type PromptVersion,
} from '@/api/prompt'
import { fetchModelConfigs, type ModelConfigItem } from '@/api/model'
import { formatDateTime } from '@/utils/datetime'

const loading = ref(false)
const saving = ref(false)
const list = ref<PromptTemplate[]>([])
const keyword = ref('')
const category = ref<string>()
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)

const drawerOpen = ref(false)
const editingId = ref<number | null>(null)
const variableRows = ref<PromptVariable[]>([])
const form = reactive({
  templateName: '',
  description: '',
  category: 'custom',
  content: '',
  changeLog: '',
})

const versionOpen = ref(false)
const versionsLoading = ref(false)
const versions = ref<PromptVersion[]>([])
const currentTemplate = ref<PromptTemplate | null>(null)

const testOpen = ref(false)
const testingPrompt = ref<PromptTemplate | null>(null)
const testVarsJson = ref('{}')
const testUserMessage = ref('')
const testModelId = ref<number>()
const testLoading = ref(false)
const testResult = ref<{ renderedPrompt: string; reply?: string } | null>(null)
const chatModels = ref<ModelConfigItem[]>([])
const modelsLoading = ref(false)

const previewOpen = ref(false)
const previewContentText = ref('')

const chatModelOptions = computed(() =>
  chatModels.value
    .filter((item) => item.enabled)
    .map((item) => ({
      value: item.id,
      label: `${item.displayName} (${item.providerName})`,
    })),
)

function previewContent(content?: string) {
  if (!content) return ''
  return content.length > 120 ? `${content.slice(0, 120)}...` : content
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchPrompts({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      category: category.value || undefined,
    })
    list.value = res.data.data.list
    total.value = res.data.data.total
  } finally {
    loading.value = false
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

function resetForm() {
  editingId.value = null
  variableRows.value = []
  Object.assign(form, {
    templateName: '',
    description: '',
    category: 'custom',
    content: '',
    changeLog: '',
  })
}

function openCreate() {
  resetForm()
  drawerOpen.value = true
}

function openEdit(item: PromptTemplate) {
  editingId.value = item.id
  Object.assign(form, {
    templateName: item.templateName,
    description: item.description || '',
    category: item.category || 'custom',
    content: item.content,
    changeLog: '',
  })
  variableRows.value = (item.variables || []).map((v) => ({ ...v }))
  drawerOpen.value = true
}

async function openVersions(item: PromptTemplate) {
  currentTemplate.value = item
  versionOpen.value = true
  versionsLoading.value = true
  try {
    const res = await fetchPromptVersions(item.id)
    versions.value = res.data.data
  } finally {
    versionsLoading.value = false
  }
}

function previewVersion(item: PromptVersion) {
  previewContentText.value = item.content
  previewOpen.value = true
}

function openTest(item: PromptTemplate) {
  testingPrompt.value = item
  testVarsJson.value = '{}'
  testUserMessage.value = ''
  testModelId.value = undefined
  testResult.value = null
  testOpen.value = true
  loadChatModels()
}

function resetTest() {
  testingPrompt.value = null
  testResult.value = null
}

function addVariable() {
  variableRows.value.push({ name: '', description: '', defaultValue: '' })
}

function removeVariable(index: number) {
  variableRows.value.splice(index, 1)
}

async function onSave() {
  if (!form.templateName?.trim() || !form.content?.trim()) {
    message.warning('请填写模板名称和 Prompt 内容')
    return
  }
  saving.value = true
  try {
    const payload = {
      templateName: form.templateName.trim(),
      description: form.description?.trim(),
      category: form.category,
      content: form.content.trim(),
      variables: variableRows.value.filter((item) => item.name?.trim()),
      changeLog: form.changeLog?.trim(),
    }
    if (editingId.value) {
      await updatePrompt(editingId.value, payload)
      message.success('更新成功')
    } else {
      await createPrompt(payload)
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
  try {
    await deletePrompt(id)
    message.success('删除成功')
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function onRollback(version: number) {
  if (!currentTemplate.value) return
  try {
    await rollbackPrompt(currentTemplate.value.id, version)
    message.success(`已回滚到 v${version}`)
    versionOpen.value = false
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '回滚失败')
  }
}

async function runTest() {
  if (!testingPrompt.value) return
  let variables: Record<string, unknown> = {}
  if (testVarsJson.value.trim()) {
    try {
      variables = JSON.parse(testVarsJson.value) as Record<string, unknown>
    } catch {
      message.error('测试变量 JSON 格式不正确')
      return
    }
  }
  testLoading.value = true
  try {
    const res = await testPrompt(testingPrompt.value.id, {
      variables,
      modelConfigId: testModelId.value,
      userMessage: testUserMessage.value?.trim() || undefined,
    })
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
.prompt-page {
  min-height: auto;
}

.prompt-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.prompt-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.prompt-card-head {
  display: flex;
  gap: 12px;
}

.prompt-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(114, 46, 209, 0.1);
  color: #722ed1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.prompt-title-wrap h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.prompt-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.prompt-desc {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.prompt-preview {
  margin: 0;
  padding: 10px 12px;
  background: var(--bg-muted, #f8fafc);
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 72px;
}

.prompt-footer {
  margin-top: auto;
}

.prompt-time {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.prompt-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 4px;
  border-top: 1px solid var(--border, #f1f5f9);
  padding-top: 8px;
}

.prompt-actions :deep(.ant-btn-link) {
  padding: 0 6px;
  height: auto;
  line-height: 1.5;
  white-space: nowrap;
}

.variable-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.variable-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr auto;
  gap: 8px;
  align-items: center;
}

.version-time {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.test-target {
  margin-bottom: 12px;
  color: var(--text-secondary);
}

.test-result {
  margin-top: 16px;
}

.result-block + .result-block {
  margin-top: 12px;
}

.result-label {
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.result-box {
  margin: 0;
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
