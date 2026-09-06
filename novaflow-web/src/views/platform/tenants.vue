<template>
  <div class="platform-admin-page page-shell">
    <div class="page-header">
      <div>
        <h1>租户管理</h1>
        <p>管理所有企业租户、套餐与资源配额</p>
      </div>
      <a-button type="primary" @click="openCreate">新建租户</a-button>
    </div>

    <div class="page-card list-panel">
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
          <template v-if="column.key === 'tenantName'">
            <router-link :to="tenantDetailPath(record.id)" class="tenant-link">
              {{ record.tenantName }}
            </router-link>
          </template>
          <template v-else-if="column.key === 'planType'">
            <a-tag>{{ record.planTypeLabel || record.planType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'expireAt'">
            <span :class="{ 'text-danger': isExpired(record.expireAt) }">
              {{ formatPlatformDate(record.expireAt) }}
            </span>
          </template>
          <template v-else-if="column.key === 'tokenQuota'">
            <div class="quota-cell">
              <span>{{ formatPlatformNumber(record.usedTokensThisMonth) }}</span>
              <a-progress
                v-if="tokenPercent(record) != null"
                :percent="tokenPercent(record)!"
                size="small"
                :show-info="false"
                :status="tokenPercent(record)! >= 90 ? 'exception' : 'normal'"
              />
            </div>
          </template>
          <template v-else-if="column.key === 'storageQuota'">
            <div class="quota-cell">
              <span>{{ formatStorageMb(record.usedStorageBytes) }} / {{ formatPlatformNumber(record.maxStorageMb) }} MB</span>
              <a-progress
                v-if="record.storageUsedPercent != null"
                :percent="record.storageUsedPercent"
                size="small"
                :show-info="false"
                :status="record.storageUsedPercent >= 90 ? 'exception' : 'normal'"
              />
            </div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'default'" :text="record.status === 1 ? '正常' : '停用'" />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <router-link :to="tenantDetailPath(record.id)">
                <a-button type="link" size="small">详情</a-button>
              </router-link>
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确认删除该租户？" @confirm="removeTenant(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <TenantEditModal v-model:open="modalOpen" :saving="saving" :form="form" @save="saveTenant" />
    <TenantCreateModal v-model:open="createModalOpen" :saving="creating" :form="createForm" @save="createTenant" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import {
  createPlatformTenant,
  deletePlatformTenant,
  fetchPlatformTenants,
  updatePlatformTenant,
  type PlatformTenant,
} from '@/api/platform'
import { platformPath } from '@/config/deploy'
import TenantCreateModal from '@/views/platform/components/TenantCreateModal.vue'
import TenantEditModal from '@/views/platform/components/TenantEditModal.vue'
import { formatPlatformDate, formatPlatformNumber, formatStorageMb, quotaPercent } from '@/views/platform/shared/utils'
import '@/views/platform/shared/styles.css'

const loading = ref(false)
const saving = ref(false)
const creating = ref(false)
const keyword = ref('')
const tenants = ref<PlatformTenant[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const modalOpen = ref(false)
const createModalOpen = ref(false)
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
  maxStorageMb: 10240,
  monthlyTokenQuota: 1000000,
  status: 1,
  expireAt: null as Dayjs | null,
})

const createForm = reactive({
  tenantName: '',
  planType: 'free',
  ownerEmail: '',
  ownerPassword: '',
  generatePassword: true,
  sendInviteEmail: false,
  ownerNickname: '',
  contactName: '',
  contactEmail: '',
  contactPhone: '',
})

const columns = [
  { title: '企业名称', dataIndex: 'tenantName', key: 'tenantName' },
  { title: '编码', dataIndex: 'tenantCode', key: 'tenantCode' },
  { title: '套餐', key: 'planType' },
  {
    title: '成员',
    key: 'members',
    customRender: ({ record }: { record: PlatformTenant }) => `${record.memberCount || 0}/${record.maxMembers || 0}`,
  },
  { title: '到期', key: 'expireAt', width: 120 },
  { title: '本月 Token', key: 'tokenQuota', width: 160 },
  { title: '存储', key: 'storageQuota', width: 180 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'actions', width: 180 },
]

const pagination = computed(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
}))

function tenantDetailPath(id: number) {
  return platformPath(`/platform/tenants/${id}`)
}

function isExpired(expireAt?: string) {
  if (!expireAt) return false
  return new Date(expireAt).getTime() < Date.now()
}

function tokenPercent(record: PlatformTenant) {
  return quotaPercent(record.usedTokensThisMonth, record.monthlyTokenQuota)
}

async function loadTenants() {
  loading.value = true
  try {
    const res = await fetchPlatformTenants({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    tenants.value = res.data.data.list
    total.value = res.data.data.total
  } catch {
    message.error('加载租户列表失败')
  } finally {
    loading.value = false
  }
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 10
  loadTenants()
}

function openCreate() {
  createForm.tenantName = ''
  createForm.planType = 'free'
  createForm.ownerEmail = ''
  createForm.ownerPassword = ''
  createForm.generatePassword = true
  createForm.sendInviteEmail = false
  createForm.ownerNickname = ''
  createForm.contactName = ''
  createForm.contactEmail = ''
  createForm.contactPhone = ''
  createModalOpen.value = true
}

async function createTenant() {
  if (!createForm.tenantName.trim()) {
    message.warning('请填写企业名称')
    return
  }
  if (!createForm.ownerEmail.trim()) {
    message.warning('请填写所有者邮箱')
    return
  }
  if (!createForm.generatePassword && !createForm.ownerPassword.trim()) {
    message.warning('请填写初始密码或勾选自动生成')
    return
  }
  creating.value = true
  try {
    const payload = {
      tenantName: createForm.tenantName,
      planType: createForm.planType,
      ownerEmail: createForm.ownerEmail,
      ownerNickname: createForm.ownerNickname,
      contactName: createForm.contactName,
      contactEmail: createForm.contactEmail,
      contactPhone: createForm.contactPhone,
      generatePassword: createForm.generatePassword,
      sendInviteEmail: createForm.sendInviteEmail,
      ownerPassword: createForm.generatePassword ? undefined : createForm.ownerPassword,
    }
    const res = await createPlatformTenant(payload)
    const result = res.data.data
    createModalOpen.value = false
    await loadTenants()
    if (result.generatedPassword) {
      Modal.success({
        title: '租户已创建',
        content: `Owner：${result.ownerEmail}\n初始密码：${result.generatedPassword}${
          result.inviteEmailSent ? '\n（邀请邮件已发送）' : '\n（请通过安全渠道告知客户）'
        }`,
      })
    } else {
      message.success(result.inviteEmailSent ? '租户已创建，邀请邮件已发送' : '租户已创建')
    }
  } catch {
    message.error('创建失败')
  } finally {
    creating.value = false
  }
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
  form.maxStorageMb = record.maxStorageMb || 10240
  form.monthlyTokenQuota = record.monthlyTokenQuota || 1000000
  form.status = record.status ?? 1
  form.expireAt = record.expireAt ? dayjs(record.expireAt) : null
  modalOpen.value = true
}

async function saveTenant() {
  if (!form.tenantName.trim()) {
    message.warning('请填写企业名称')
    return
  }
  saving.value = true
  try {
    await updatePlatformTenant(editingId.value!, {
      tenantName: form.tenantName,
      planType: form.planType,
      contactName: form.contactName,
      contactEmail: form.contactEmail,
      contactPhone: form.contactPhone,
      maxMembers: form.maxMembers,
      maxAgents: form.maxAgents,
      maxKnowledge: form.maxKnowledge,
      maxStorageMb: form.maxStorageMb,
      monthlyTokenQuota: form.monthlyTokenQuota,
      status: form.status,
      expireAt: form.expireAt ? form.expireAt.format('YYYY-MM-DDTHH:mm:ss') : undefined,
    })
    message.success('租户已更新')
    modalOpen.value = false
    await loadTenants()
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function removeTenant(id: number) {
  await deletePlatformTenant(id)
  message.success('租户已删除')
  await loadTenants()
}

onMounted(loadTenants)
</script>

<style scoped>
.list-panel {
  padding: 16px;
}

.tenant-link {
  font-weight: 600;
}

.quota-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 120px;
}

.text-danger {
  color: #ef4444;
}
</style>
