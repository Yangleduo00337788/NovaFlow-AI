<template>
  <div class="platform-admin-page page-shell">
    <div class="page-header">
      <div>
        <h1>安全中心</h1>
        <p>IP 黑名单与风控告警管理</p>
      </div>
      <a-button v-if="activeTab === 'risk'" :loading="overviewLoading" @click="loadOverview">刷新</a-button>
    </div>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="blacklist" tab="IP 黑名单">
        <div class="page-card list-panel">
          <div class="list-toolbar">
            <div class="list-toolbar-filters">
              <a-input-search
                v-model:value="keyword"
                placeholder="搜索 IP、原因"
                allow-clear
                style="width: 260px"
                @search="loadList"
              />
              <a-button type="primary" @click="openCreate">添加 IP</a-button>
            </div>
            <span class="list-toolbar-meta">共 {{ total }} 条记录</span>
          </div>
          <a-table
            :columns="columns"
            :data-source="items"
            :loading="loading"
            row-key="id"
            :pagination="pagination"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-badge :status="record.status === 1 ? 'error' : 'default'" :text="record.status === 1 ? '生效' : '停用'" />
              </template>
              <template v-else-if="column.key === 'expireAt'">
                {{ record.expireAt ? formatDateTime(record.expireAt) : '永久' }}
              </template>
              <template v-else-if="column.key === 'createdAt'">
                {{ formatDateTime(record.createdAt) }}
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-space>
                  <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
                  <a-button v-if="record.status === 1" type="link" size="small" @click="toggleStatus(record, 0)">停用</a-button>
                  <a-button v-else type="link" size="small" @click="toggleStatus(record, 1)">启用</a-button>
                  <a-popconfirm title="确认删除该 IP 黑名单记录？" @confirm="remove(record.id)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>

      <a-tab-pane key="risk" tab="风控告警">
        <div class="page-card list-panel">
          <a-spin :spinning="overviewLoading">
            <div v-if="overview" class="ops-stats-grid">
              <div class="stat-item">
                <span class="label">待处理告警</span>
                <strong>{{ formatPlatformNumber(overview.openAlertCount) }}</strong>
              </div>
              <div class="stat-item">
                <span class="label">异常登录</span>
                <strong>{{ formatPlatformNumber(overview.abnormalLoginOpenCount) }}</strong>
              </div>
              <div class="stat-item">
                <span class="label">批量注册</span>
                <strong>{{ formatPlatformNumber(overview.batchRegisterOpenCount) }}</strong>
              </div>
              <div class="stat-item">
                <span class="label">新设备登录</span>
                <strong>{{ formatPlatformNumber(overview.newUserAgentOpenCount) }}</strong>
              </div>
            </div>
          </a-spin>

          <div class="list-toolbar" style="margin-top: 16px">
            <a-select
              v-model:value="alertStatusFilter"
              allow-clear
              placeholder="状态"
              style="width: 120px"
              @change="loadAlerts"
            >
              <a-select-option value="OPEN">待处理</a-select-option>
              <a-select-option value="ACKED">已确认</a-select-option>
            </a-select>
            <span class="list-toolbar-meta">共 {{ alertTotal }} 条告警</span>
          </div>
          <a-table
            :columns="alertColumns"
            :data-source="alerts"
            :loading="alertsLoading"
            row-key="id"
            :pagination="alertPagination"
            size="small"
            @change="onAlertTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'severity'">
                <a-tag :color="record.severity === 'critical' ? 'red' : 'orange'">
                  {{ record.severity === 'critical' ? '严重' : '警告' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'alertType'">
                {{ alertTypeLabel(record) }}
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === 'OPEN' ? 'orange' : 'default'">
                  {{ record.status === 'OPEN' ? '待处理' : '已确认' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'createdAt'">
                {{ formatDateTime(record.createdAt) }}
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-button
                  v-if="record.status === 'OPEN'"
                  type="link"
                  size="small"
                  @click="ackAlert(record.id)"
                >
                  确认
                </a-button>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>
    </a-tabs>

    <IpBlacklistModal
      v-model:open="modalOpen"
      :saving="saving"
      :editing-id="editingId"
      :form="form"
      @save="save"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  acknowledgePlatformSecurityAlert,
  createIpBlacklist,
  deleteIpBlacklist,
  fetchIpBlacklist,
  fetchPlatformSecurityAlerts,
  fetchPlatformSecurityOverview,
  updateIpBlacklist,
  type IpBlacklistItem,
  type PlatformSecurityAlertEvent,
  type PlatformSecurityOverview,
} from '@/api/platform'
import { formatDateTime } from '@/utils/datetime'
import IpBlacklistModal from '@/views/platform/components/IpBlacklistModal.vue'
import { formatPlatformNumber } from '@/views/platform/shared/utils'
import '@/views/platform/shared/styles.css'

const activeTab = ref('blacklist')

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const items = ref<IpBlacklistItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  ipAddress: '',
  reason: '',
  expireAt: null as Dayjs | null,
  status: 1,
})

const overviewLoading = ref(false)
const alertsLoading = ref(false)
const overview = ref<PlatformSecurityOverview | null>(null)
const alerts = ref<PlatformSecurityAlertEvent[]>([])
const alertTotal = ref(0)
const alertPage = ref(1)
const alertPageSize = ref(10)
const alertStatusFilter = ref<string | undefined>()

const columns = [
  { title: 'IP 地址', dataIndex: 'ipAddress', key: 'ipAddress', width: 160 },
  { title: '原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '过期时间', key: 'expireAt', width: 170 },
  { title: '创建时间', key: 'createdAt', width: 170 },
  { title: '操作', key: 'actions', width: 180 },
]

const alertColumns = [
  { title: '级别', key: 'severity', width: 90 },
  { title: '类型', key: 'alertType', width: 120 },
  { title: '用户', dataIndex: 'userEmail', key: 'userEmail', width: 180, ellipsis: true },
  { title: 'IP', dataIndex: 'clientIp', key: 'clientIp', width: 130 },
  { title: '说明', dataIndex: 'message', key: 'message', ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '时间', key: 'createdAt', width: 170 },
  { title: '操作', key: 'actions', width: 90 },
]

const pagination = computed(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
}))

const alertPagination = computed(() => ({
  current: alertPage.value,
  pageSize: alertPageSize.value,
  total: alertTotal.value,
  showSizeChanger: true,
}))

function alertTypeLabel(record: PlatformSecurityAlertEvent) {
  if (record.alertTypeLabel) return record.alertTypeLabel
  const map: Record<string, string> = {
    ABNORMAL_LOGIN: '异常登录',
    BATCH_REGISTER: '批量注册',
    NEW_USER_AGENT: '新设备登录',
  }
  return map[record.alertType] || record.alertType
}

async function loadList() {
  loading.value = true
  try {
    const res = await fetchIpBlacklist({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    items.value = res.data.data.list
    total.value = res.data.data.total
  } catch {
    message.error('加载 IP 黑名单失败')
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  overviewLoading.value = true
  try {
    const res = await fetchPlatformSecurityOverview()
    overview.value = res.data.data
  } catch {
    message.error('加载风控概览失败')
  } finally {
    overviewLoading.value = false
  }
}

async function loadAlerts() {
  alertsLoading.value = true
  try {
    const res = await fetchPlatformSecurityAlerts({
      page: alertPage.value,
      pageSize: alertPageSize.value,
      status: alertStatusFilter.value,
    })
    alerts.value = res.data.data.list
    alertTotal.value = res.data.data.total
  } catch {
    message.error('加载风控告警失败')
  } finally {
    alertsLoading.value = false
  }
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 10
  loadList()
}

function onAlertTableChange(pag: { current?: number; pageSize?: number }) {
  alertPage.value = pag.current || 1
  alertPageSize.value = pag.pageSize || 10
  loadAlerts()
}

async function ackAlert(id: number) {
  try {
    await acknowledgePlatformSecurityAlert(id)
    message.success('已确认')
    await Promise.all([loadOverview(), loadAlerts()])
  } catch {
    message.error('确认失败')
  }
}

function openCreate() {
  editingId.value = null
  form.ipAddress = ''
  form.reason = ''
  form.expireAt = null
  form.status = 1
  modalOpen.value = true
}

function openEdit(record: IpBlacklistItem) {
  editingId.value = record.id
  form.ipAddress = record.ipAddress
  form.reason = record.reason || ''
  form.expireAt = record.expireAt ? dayjs(record.expireAt) : null
  form.status = record.status ?? 1
  modalOpen.value = true
}

async function save() {
  if (!editingId.value && !form.ipAddress.trim()) {
    message.warning('请填写 IP 地址')
    return
  }
  saving.value = true
  try {
    const expireAt = form.expireAt?.format('YYYY-MM-DDTHH:mm:ss')
    if (editingId.value) {
      await updateIpBlacklist(editingId.value, {
        reason: form.reason || undefined,
        status: form.status,
        expireAt,
      })
      message.success('已更新')
    } else {
      await createIpBlacklist({
        ipAddress: form.ipAddress.trim(),
        reason: form.reason || undefined,
        expireAt,
      })
      message.success('已添加')
    }
    modalOpen.value = false
    await loadList()
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(record: IpBlacklistItem, status: number) {
  await updateIpBlacklist(record.id, {
    reason: record.reason,
    status,
    expireAt: record.expireAt,
  })
  message.success(status === 1 ? '已启用' : '已停用')
  await loadList()
}

async function remove(id: number) {
  await deleteIpBlacklist(id)
  message.success('已删除')
  await loadList()
}

watch(activeTab, (tab) => {
  if (tab === 'risk') {
    loadOverview()
    loadAlerts()
  }
})

onMounted(loadList)
</script>

<style scoped>
.list-panel {
  padding: 16px;
}
</style>
