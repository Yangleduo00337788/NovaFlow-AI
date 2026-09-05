<template>
  <div class="platform-admin-page page-shell">
    <div class="page-header">
      <div>
        <h1>IP 黑名单</h1>
        <p>拦截恶意 IP 的登录与 API 访问</p>
      </div>
    </div>

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
import { computed, onMounted, reactive, ref } from 'vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  createIpBlacklist,
  deleteIpBlacklist,
  fetchIpBlacklist,
  updateIpBlacklist,
  type IpBlacklistItem,
} from '@/api/platform'
import { formatDateTime } from '@/utils/datetime'
import IpBlacklistModal from '@/views/platform/components/IpBlacklistModal.vue'
import '@/views/platform/shared/styles.css'

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

const columns = [
  { title: 'IP 地址', dataIndex: 'ipAddress', key: 'ipAddress', width: 160 },
  { title: '原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '过期时间', key: 'expireAt', width: 170 },
  { title: '创建时间', key: 'createdAt', width: 170 },
  { title: '操作', key: 'actions', width: 180 },
]

const pagination = computed(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
}))

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

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 10
  loadList()
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

onMounted(loadList)
</script>

<style scoped>
.list-panel {
  padding: 16px;
}
</style>
