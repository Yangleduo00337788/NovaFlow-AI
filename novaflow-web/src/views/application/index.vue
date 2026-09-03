<template>
  <div class="application-page page-shell" data-testid="application-page">
    <div class="page-header">
      <div>
        <h1>应用管理</h1>
        <p>聚合 Agent 与知识库，作为统一发布与访问入口</p>
      </div>
      <a-button v-if="canManage" type="primary" data-testid="create-app-btn" @click="openCreate">
        <PlusOutlined />
        创建应用
      </a-button>
    </div>

    <div class="list-panel page-card">
      <div class="list-toolbar">
        <div class="list-toolbar-filters">
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索应用名称"
            style="width: 240px"
            allow-clear
            @search="loadData"
          />
        </div>
        <span class="list-toolbar-meta">共 {{ total }} 个应用</span>
      </div>
      <div class="list-body">
        <a-spin :spinning="loading">
          <div v-if="list.length" class="app-grid">
        <div
          v-for="item in list"
          :key="item.id"
          class="app-card page-card"
          :data-testid="`app-card-${item.id}`"
        >
          <div class="app-card-head">
            <div class="app-icon">
              <AppstoreOutlined />
            </div>
            <div class="app-title-wrap">
              <h3>{{ item.appName }}</h3>
              <div class="app-tags">
                <a-tag>{{ getAppTypeLabel(item.appType) }}</a-tag>
                <a-tag :color="publishStatusColor(item.publishStatus)">
                  {{ getPublishStatusLabel(item.publishStatus) }}
                </a-tag>
              </div>
            </div>
          </div>
          <p class="app-desc">{{ item.description || '暂无描述' }}</p>
          <div class="app-stats">
            <span>{{ item.agentCount || 0 }} 个 Agent</span>
            <span>{{ item.knowledgeBaseCount || 0 }} 个知识库</span>
          </div>
          <div v-if="item.defaultAgentName" class="app-default">
            默认入口：{{ item.defaultAgentName }}
          </div>
          <div class="app-footer">
            <span class="app-time">{{ formatDateTime(item.updatedAt) }}</span>
          </div>
          <div v-if="canManage || canRead" class="app-actions">
            <a-button v-if="canRead" type="link" size="small" @click="openPublish(item)">
              {{ canManage ? '发布信息' : '查看详情' }}
            </a-button>
            <template v-if="canManage">
              <a-button type="link" size="small" @click="openEdit(item)">编辑</a-button>
              <a-popconfirm title="确认删除该应用？" @confirm="onDelete(item.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </template>
          </div>
        </div>
      </div>
      <a-empty v-else :description="canManage ? '暂无应用，点击右上角创建' : '暂无应用'" />
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
      :title="editingId ? '编辑应用' : '创建应用'"
      :width="720"
      @close="resetForm"
    >
      <a-form layout="vertical" :model="form">
        <a-row :gutter="16">
          <a-col :span="14">
            <a-form-item label="应用名称" required>
              <a-input v-model:value="form.appName" placeholder="智能客服" data-testid="app-name-input" :maxlength="128" :show-count="true" />
            </a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item label="应用类型">
              <a-select v-model:value="form.appType" :options="APP_TYPES" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="应用功能说明" />
        </a-form-item>
        <a-form-item label="访问范围">
          <a-select v-model:value="form.accessType" :options="ACCESS_TYPES" />
        </a-form-item>
        <a-form-item label="关联 Agent">
          <a-select
            v-model:value="form.agentIds"
            mode="multiple"
            allow-clear
            placeholder="选择要纳入本应用的 Agent"
            :loading="agentsLoading"
            :options="agentOptions"
            @change="onAgentIdsChange"
          />
        </a-form-item>
        <a-form-item label="默认入口 Agent">
          <a-select
            v-model:value="form.defaultAgentId"
            allow-clear
            placeholder="对外访问时使用的默认 Agent"
            :options="selectedAgentOptions"
          />
        </a-form-item>
        <a-form-item label="关联知识库">
          <a-select
            v-model:value="form.knowledgeBaseIds"
            mode="multiple"
            allow-clear
            placeholder="选择要纳入本应用的知识库"
            :loading="knowledgeLoading"
            :options="knowledgeOptions"
          />
        </a-form-item>
        <a-button v-if="canManage" type="primary" :loading="saving" data-testid="save-app-btn" @click="onSave">保存</a-button>
        <a-button
          v-if="editingId && canManageResource"
          style="margin-top: 12px"
          block
          @click="resourcePermOpen = true"
        >
          资源授权
        </a-button>
      </a-form>
    </a-drawer>

    <ResourcePermissionDrawer
      v-if="canManageResource"
      :open="resourcePermOpen"
      resource-type="APPLICATION"
      :resource-id="editingId"
      :permission-options="applicationPermissionOptions"
      @close="resourcePermOpen = false"
    />

    <a-modal v-model:open="publishOpen" title="应用发布" :footer="null" width="640px">
      <div v-if="publishTarget" class="publish-modal">
        <p class="publish-target">{{ publishTarget.appName }}</p>
        <a-descriptions bordered size="small" :column="1">
          <a-descriptions-item label="发布状态">
            {{ getPublishStatusLabel(publishInfo?.publishStatus ?? publishTarget.publishStatus) }}
          </a-descriptions-item>
          <a-descriptions-item label="默认入口 Agent">
            {{ publishInfo?.defaultAgentName || publishTarget.defaultAgentName || '未设置' }}
          </a-descriptions-item>
          <a-descriptions-item v-if="publishInfo?.chatEndpoint" label="对话接口">
            {{ publishInfo.chatEndpoint }}
          </a-descriptions-item>
          <a-descriptions-item v-if="publishInfo?.embedPath" label="嵌入路径">
            {{ publishInfo.embedPath }}
          </a-descriptions-item>
          <a-descriptions-item v-if="publishInfo?.portalPath" label="应用门户">
            <router-link :to="publishInfo.portalPath">{{ publishInfo.portalPath }}</router-link>
          </a-descriptions-item>
        </a-descriptions>
        <div v-if="canManage" class="publish-actions">
          <a-button
            v-if="(publishInfo?.publishStatus ?? publishTarget.publishStatus) !== 1"
            type="primary"
            :loading="publishLoading"
            @click="onPublish"
          >
            发布应用
          </a-button>
          <a-button
            v-else
            danger
            :loading="publishLoading"
            @click="onUnpublish"
          >
            下线应用
          </a-button>
        </div>
        <p class="publish-hint">发布前请确保默认入口 Agent 已在 Agent Studio 中发布。</p>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { AppstoreOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { fetchAgents, type AgentItem } from '@/api/agent'
import { fetchKnowledgeBases, type KnowledgeBaseItem } from '@/api/knowledge'
import {
  ACCESS_TYPES,
  APP_TYPES,
  createApplication,
  deleteApplication,
  fetchApplication,
  fetchApplicationPublishInfo,
  fetchApplications,
  getPublishStatusLabel,
  publishApplication,
  unpublishApplication,
  updateApplication,
  type ApplicationItem,
  type ApplicationPublishInfo,
  type ApplicationSaveRequest,
} from '@/api/application'
import { formatDateTime } from '@/utils/datetime'
import { useAuthStore } from '@/stores/auth'
import ResourcePermissionDrawer from '@/components/common/ResourcePermissionDrawer.vue'
import { RESOURCE_PERMISSION_OPTIONS, canManageResourcePermission } from '@/config/resourcePermissions'

const auth = useAuthStore()
const canRead = computed(() => auth.hasAnyPermission(['application:read', 'application:manage']))
const canManage = computed(() => auth.hasPermission('application:manage'))
const canManageResource = computed(() => canManageResourcePermission(auth.hasAnyPermission.bind(auth)))
const applicationPermissionOptions = RESOURCE_PERMISSION_OPTIONS.APPLICATION
const resourcePermOpen = ref(false)
const loading = ref(false)
const saving = ref(false)
const list = ref<ApplicationItem[]>([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)

const drawerOpen = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<ApplicationSaveRequest>({
  appName: '',
  description: '',
  appType: 'agent',
  accessType: 'team',
  agentIds: [],
  knowledgeBaseIds: [],
  defaultAgentId: undefined,
})

const agents = ref<AgentItem[]>([])
const knowledgeBases = ref<KnowledgeBaseItem[]>([])
const agentsLoading = ref(false)
const knowledgeLoading = ref(false)

const publishOpen = ref(false)
const publishLoading = ref(false)
const publishTarget = ref<ApplicationItem | null>(null)
const publishInfo = ref<ApplicationPublishInfo | null>(null)

const agentOptions = computed(() =>
  agents.value.map((item) => ({
    value: item.id,
    label: `${item.agentName}（${item.agentType}）`,
  })),
)

const knowledgeOptions = computed(() =>
  knowledgeBases.value.map((item) => ({
    value: item.id,
    label: item.kbName,
  })),
)

const selectedAgentOptions = computed(() =>
  agentOptions.value.filter((item) => form.agentIds?.includes(item.value)),
)

function getAppTypeLabel(type?: string) {
  return APP_TYPES.find((item) => item.value === type)?.label || 'Agent 应用'
}

function publishStatusColor(status?: number) {
  if (status === 1) return 'green'
  if (status === 2) return 'default'
  return 'blue'
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchApplications({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    list.value = res.data.data.list
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

async function loadAgents() {
  agentsLoading.value = true
  try {
    const res = await fetchAgents({ page: 1, pageSize: 200 })
    agents.value = res.data.data.list
  } finally {
    agentsLoading.value = false
  }
}

async function loadKnowledgeBases() {
  knowledgeLoading.value = true
  try {
    const res = await fetchKnowledgeBases({ page: 1, pageSize: 200 })
    knowledgeBases.value = res.data.data.list
  } finally {
    knowledgeLoading.value = false
  }
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    appName: '',
    description: '',
    appType: 'agent',
    accessType: 'team',
    agentIds: [],
    knowledgeBaseIds: [],
    defaultAgentId: undefined,
  })
}

function openCreate() {
  if (!canManage.value) return
  resetForm()
  loadAgents()
  loadKnowledgeBases()
  drawerOpen.value = true
}

async function openEdit(item: ApplicationItem) {
  if (!canManage.value) return
  resetForm()
  loadAgents()
  loadKnowledgeBases()
  const res = await fetchApplication(item.id)
  const data = res.data.data
  editingId.value = item.id
  Object.assign(form, {
    appName: data.appName,
    description: data.description,
    appType: data.appType || 'agent',
    accessType: data.accessType || 'team',
    agentIds: data.agentIds || [],
    knowledgeBaseIds: data.knowledgeBaseIds || [],
    defaultAgentId: data.defaultAgentId,
  })
  drawerOpen.value = true
}

function onAgentIdsChange(values: number[]) {
  if (form.defaultAgentId && !values.includes(form.defaultAgentId)) {
    form.defaultAgentId = values[0]
  }
}

async function onSave() {
  if (!canManage.value) return
  if (!form.appName?.trim()) {
    message.warning('请输入应用名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateApplication(editingId.value, form)
      message.success('更新成功')
    } else {
      await createApplication(form)
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
  if (!canManage.value) return
  try {
    await deleteApplication(id)
    message.success('删除成功')
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function openPublish(item: ApplicationItem) {
  if (!canRead.value) return
  publishTarget.value = item
  publishInfo.value = null
  publishOpen.value = true
  try {
    const res = await fetchApplicationPublishInfo(item.id)
    publishInfo.value = res.data.data
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载发布信息失败')
  }
}

async function onPublish() {
  if (!canManage.value || !publishTarget.value) return
  publishLoading.value = true
  try {
    const res = await publishApplication(publishTarget.value.id)
    publishInfo.value = res.data.data
    message.success('应用已发布')
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '发布失败')
  } finally {
    publishLoading.value = false
  }
}

async function onUnpublish() {
  if (!canManage.value || !publishTarget.value) return
  publishLoading.value = true
  try {
    const res = await unpublishApplication(publishTarget.value.id)
    publishInfo.value = res.data.data
    message.success('应用已下线')
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '下线失败')
  } finally {
    publishLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.application-page {
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

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-meta {
  color: #64748b;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.app-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.app-card-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.app-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #eef2ff;
  color: #4f46e5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.app-title-wrap h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.app-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.app-desc {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
  min-height: 40px;
}

.app-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #475569;
}

.app-default {
  font-size: 12px;
  color: #334155;
}

.app-footer {
  margin-top: auto;
}

.app-time {
  font-size: 12px;
  color: #94a3b8;
}

.app-actions {
  display: flex;
  gap: 4px;
  border-top: 1px solid #f1f5f9;
  padding-top: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
}

.publish-modal {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.publish-target {
  margin: 0;
  font-weight: 600;
}

.publish-actions {
  display: flex;
  gap: 8px;
}

.publish-hint {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
}
</style>
