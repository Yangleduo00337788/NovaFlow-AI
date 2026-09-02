<template>
  <div class="platform-page page-shell">
    <div class="page-header">
      <div>
        <h1>平台超管</h1>
        <p>跨租户管理、套餐配置与全局用量概览</p>
      </div>
    </div>

    <div class="stats-grid page-card" v-if="stats">
      <div class="stat-item">
        <span class="label">租户总数</span>
        <strong>{{ stats.tenantCount }}</strong>
      </div>
      <div class="stat-item">
        <span class="label">活跃租户</span>
        <strong>{{ stats.activeTenantCount }}</strong>
      </div>
      <div class="stat-item">
        <span class="label">成员总数</span>
        <strong>{{ stats.totalMembers }}</strong>
      </div>
      <div class="stat-item">
        <span class="label">Agent 总数</span>
        <strong>{{ stats.totalAgents }}</strong>
      </div>
      <div class="stat-item">
        <span class="label">知识库总数</span>
        <strong>{{ stats.totalKnowledgeBases }}</strong>
      </div>
      <div class="stat-item">
        <span class="label">本月 Token</span>
        <strong>{{ formatNumber(stats.tokensUsedThisMonth) }}</strong>
      </div>
    </div>

    <div class="list-panel page-card">
      <div class="list-toolbar">
        <div class="list-toolbar-filters">
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索企业名称、编码、邮箱"
            allow-clear
            style="width: 280px"
            @search="loadTenants"
          />
        </div>
        <span class="list-toolbar-meta">共 {{ total }} 个租户</span>
      </div>
      <a-table
        :columns="columns"
        :data-source="tenants"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'planType'">
            <a-tag>{{ record.planTypeLabel || record.planType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'default'" :text="record.status === 1 ? '正常' : '停用'" />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确认删除该租户？" @confirm="removeTenant(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="modalOpen"
      title="编辑租户"
      :confirm-loading="saving"
      width="640px"
      @ok="saveTenant"
    >
      <a-form layout="vertical" class="tenant-form">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="企业名称" required>
              <a-input v-model:value="form.tenantName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="套餐">
              <a-select v-model:value="form.planType" :options="planOptions" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8"><a-form-item label="联系人"><a-input v-model:value="form.contactName" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="邮箱"><a-input v-model:value="form.contactEmail" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="电话"><a-input v-model:value="form.contactPhone" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="6"><a-form-item label="成员上限"><a-input-number v-model:value="form.maxMembers" :min="1" style="width:100%" /></a-form-item></a-col>
          <a-col :span="6"><a-form-item label="Agent 上限"><a-input-number v-model:value="form.maxAgents" :min="1" style="width:100%" /></a-form-item></a-col>
          <a-col :span="6"><a-form-item label="知识库上限"><a-input-number v-model:value="form.maxKnowledge" :min="1" style="width:100%" /></a-form-item></a-col>
          <a-col :span="6"><a-form-item label="月 Token 配额"><a-input-number v-model:value="form.monthlyTokenQuota" :min="0" style="width:100%" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  deletePlatformTenant,
  fetchPlatformStats,
  fetchPlatformTenants,
  updatePlatformTenant,
  type PlatformGlobalStats,
  type PlatformTenant,
} from '@/api/platform'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const tenants = ref<PlatformTenant[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const stats = ref<PlatformGlobalStats | null>(null)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  tenantName: '',
  planType: 'free',
  contactName: '',
  contactEmail: '',
  contactPhone: '',
  maxMembers: 50,
  maxAgents: 20,
  maxKnowledge: 10,
  monthlyTokenQuota: 1000000,
  status: 1,
})

const planOptions = [
  { value: 'personal', label: '个人版' },
  { value: 'free', label: '免费版' },
  { value: 'starter', label: '入门版' },
  { value: 'pro', label: '专业版' },
  { value: 'enterprise', label: '企业版' },
]

const columns = [
  { title: '企业名称', dataIndex: 'tenantName', key: 'tenantName' },
  { title: '编码', dataIndex: 'tenantCode', key: 'tenantCode' },
  { title: '套餐', key: 'planType' },
  { title: '成员', key: 'members', customRender: ({ record }: { record: PlatformTenant }) => `${record.memberCount || 0}/${record.maxMembers || 0}` },
  { title: '本月 Token', dataIndex: 'usedTokensThisMonth', key: 'tokens' },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'actions', width: 140 },
]

const pagination = computed(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
}))

function formatNumber(value?: number) {
  return (value ?? 0).toLocaleString()
}

async function loadStats() {
  const res = await fetchPlatformStats()
  stats.value = res.data.data
}

async function loadTenants() {
  loading.value = true
  try {
    const res = await fetchPlatformTenants({ page: page.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
    tenants.value = res.data.data.list
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 10
  loadTenants()
}

function openEdit(record: PlatformTenant) {
  editingId.value = record.id
  form.tenantName = record.tenantName
  form.planType = record.planType
  form.contactName = record.contactName || ''
  form.contactEmail = record.contactEmail || ''
  form.contactPhone = record.contactPhone || ''
  form.maxMembers = record.maxMembers || 50
  form.maxAgents = record.maxAgents || 20
  form.maxKnowledge = record.maxKnowledge || 10
  form.monthlyTokenQuota = record.monthlyTokenQuota || 1000000
  form.status = record.status ?? 1
  modalOpen.value = true
}

async function saveTenant() {
  if (!form.tenantName.trim()) {
    message.warning('请填写企业名称')
    return
  }
  saving.value = true
  try {
    await updatePlatformTenant(editingId.value!, { ...form })
    message.success('租户已更新')
    modalOpen.value = false
    await Promise.all([loadTenants(), loadStats()])
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function removeTenant(id: number) {
  await deletePlatformTenant(id)
  message.success('租户已删除')
  await Promise.all([loadTenants(), loadStats()])
}

onMounted(async () => {
  await Promise.all([loadStats(), loadTenants()])
})
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  padding: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-item .label {
  color: var(--text-secondary);
  font-size: 12px;
}

.stat-item strong {
  font-size: 20px;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
