<template>
  <div class="workflow-page page-shell" data-testid="workflow-page">
    <div class="page-header">
      <div>
        <h1>工作流 Studio</h1>
        <p>可视化编排 LLM 与条件分支，构建可复用的自动化流程</p>
      </div>
      <a-button v-if="canCreate" type="primary" data-testid="create-workflow-btn" @click="openCreate">
        <PlusOutlined />
        创建工作流
      </a-button>
    </div>

    <div class="list-panel page-card">
      <div class="list-toolbar">
        <div class="list-toolbar-filters">
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索工作流名称"
            style="width: 240px"
            allow-clear
            @search="loadData"
          />
        </div>
        <span class="list-toolbar-meta">共 {{ total }} 个工作流</span>
      </div>
      <div class="list-body">
        <a-spin :spinning="loading">
          <div v-if="list.length" class="workflow-grid">
        <div
          v-for="item in list"
          :key="item.id"
          class="workflow-card page-card"
          :data-testid="`workflow-card-${item.id}`"
        >
          <div class="card-head">
            <div class="card-icon">
              <ApartmentOutlined />
            </div>
            <div>
              <h3>{{ item.workflowName }}</h3>
              <div class="card-tags">
                <a-tag :color="getWorkflowStatusColor(item.status)">
                  {{ getWorkflowStatusLabel(item.status) }}
                </a-tag>
                <a-tag>v{{ item.version || 1 }}</a-tag>
              </div>
            </div>
          </div>
          <p class="card-desc">{{ item.description || '暂无描述' }}</p>
          <div class="card-meta">
            <span>{{ item.applicationName || '未关联应用' }}</span>
            <span>{{ item.nodeCount || 0 }} 个节点</span>
          </div>
          <div class="card-footer">
            <span>{{ formatDateTime(item.updatedAt) }}</span>
          </div>
          <div v-if="canRead || canEdit || canDelete" class="card-actions">
            <a-button v-if="canEdit" type="link" size="small" @click="openEditor(item.id)">编辑</a-button>
            <a-button v-else-if="canRead" type="link" size="small" @click="openEditor(item.id)">查看</a-button>
            <a-popconfirm v-if="canDelete" title="确认删除该工作流？" @confirm="onDelete(item.id)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>
      <a-empty v-else :description="canCreate ? '暂无工作流，点击右上角创建' : '暂无工作流'" />
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

    <a-modal
      v-model:open="createOpen"
      title="创建工作流"
      ok-text="创建并编辑"
      :confirm-loading="creating"
      @ok="onCreate"
    >
      <a-form layout="vertical">
        <a-form-item label="工作流名称" required>
          <a-input
            v-model:value="createForm.workflowName"
            placeholder="客服分流流程"
            :maxlength="128"
            :show-count="true"
          />
        </a-form-item>
        <a-form-item label="所属应用" required>
          <a-select
            v-model:value="createForm.applicationId"
            placeholder="选择应用"
            :options="applicationOptions"
            :loading="appsLoading"
          />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="createForm.description" :rows="2" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ApartmentOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { fetchApplicationOptions } from '@/api/application'
import { buildDefaultCanvas, createWorkflow, deleteWorkflow, fetchWorkflows } from '@/api/workflow'
import type { WorkflowItem } from '@/types/workflow'
import { getWorkflowStatusColor, getWorkflowStatusLabel } from '@/types/workflow'
import { formatDateTime } from '@/utils/datetime'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const canCreate = computed(() => auth.hasPermission('workflow:create'))
const canRead = computed(() => auth.hasPermission('workflow:read'))
const canEdit = computed(() => auth.hasPermission('workflow:edit'))
const canDelete = computed(() => auth.hasPermission('workflow:delete'))
const loading = ref(false)
const creating = ref(false)
const appsLoading = ref(false)
const list = ref<WorkflowItem[]>([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const createOpen = ref(false)
const applicationOptions = ref<Array<{ label: string; value: number }>>([])
const createForm = reactive({
  workflowName: '',
  description: '',
  applicationId: undefined as number | undefined,
})

async function loadApplications() {
  appsLoading.value = true
  try {
    const res = await fetchApplicationOptions()
    applicationOptions.value = res.data.data.map((item) => ({
      label: item.appName,
      value: item.id,
    }))
  } finally {
    appsLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchWorkflows({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    list.value = res.data.data.list
    total.value = res.data.data.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载工作流失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  if (!canCreate.value) return
  createForm.workflowName = ''
  createForm.description = ''
  createForm.applicationId = applicationOptions.value[0]?.value
  createOpen.value = true
}

async function onCreate() {
  if (!canCreate.value) return
  if (!createForm.workflowName.trim()) {
    message.warning('请输入工作流名称')
    return
  }
  if (!createForm.applicationId) {
    message.warning('请选择所属应用')
    return
  }
  creating.value = true
  try {
    const res = await createWorkflow({
      workflowName: createForm.workflowName.trim(),
      description: createForm.description || undefined,
      applicationId: createForm.applicationId,
      canvasData: buildDefaultCanvas(),
    })
    createOpen.value = false
    message.success('工作流已创建')
    openEditor(res.data.data.id)
  } catch (e) {
    message.error(e instanceof Error ? e.message : '创建工作流失败')
  } finally {
    creating.value = false
  }
}

function openEditor(id: number) {
  router.push(`/workflow/${id}`)
}

async function onDelete(id: number) {
  if (!canDelete.value) return
  try {
    await deleteWorkflow(id)
    message.success('已删除')
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

onMounted(async () => {
  await loadApplications()
  await loadData()
  if (route.query.create === '1') {
    openCreate()
  }
})
</script>

<style scoped>
.workflow-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  margin: 0 0 4px;
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
}

.toolbar-meta {
  color: var(--text-muted);
  font-size: 13px;
}

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.workflow-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-head {
  display: flex;
  gap: 12px;
}

.card-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #eef2ff;
  color: #6366f1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.card-head h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.card-tags {
  display: flex;
  gap: 6px;
}

.card-desc {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  min-height: 40px;
}

.card-meta,
.card-footer {
  display: flex;
  justify-content: space-between;
  color: var(--text-muted);
  font-size: 12px;
}

.card-actions {
  display: flex;
  gap: 4px;
  border-top: 1px solid var(--border-color, #f0f0f0);
  padding-top: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
}
</style>
