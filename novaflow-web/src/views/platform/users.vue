<template>
  <div class="platform-admin-page page-shell">
    <div class="page-header">
      <div>
        <h1>用户管理</h1>
        <p>跨租户用户账号治理：封禁、解封与强制下线</p>
      </div>
    </div>

    <div class="page-card list-panel">
      <div class="list-toolbar">
        <div class="list-toolbar-filters">
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索邮箱、用户名、昵称"
            allow-clear
            style="width: 260px"
            @search="loadUsers"
          />
          <a-select v-model:value="statusFilter" placeholder="状态" allow-clear style="width: 120px" @change="loadUsers">
            <a-select-option :value="1">正常</a-select-option>
            <a-select-option :value="0">封禁</a-select-option>
          </a-select>
          <a-select
            v-model:value="accountTypeFilter"
            placeholder="账号类型"
            allow-clear
            style="width: 130px"
            @change="loadUsers"
          >
            <a-select-option value="tenant">租户账号</a-select-option>
            <a-select-option value="platform">平台账号</a-select-option>
          </a-select>
        </div>
        <span class="list-toolbar-meta">共 {{ total }} 个用户</span>
      </div>
      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'accountType'">
            <a-tag :color="record.accountType === 'platform' ? 'purple' : 'blue'">
              {{ record.accountType === 'platform' ? '平台' : '租户' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'memberships'">
            <span v-if="!record.memberships?.length">-</span>
            <a-tooltip v-else :title="formatMemberships(record.memberships)">
              <span class="membership-cell">{{ formatMemberships(record.memberships) }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'error'" :text="record.status === 1 ? '正常' : '封禁'" />
          </template>
          <template v-else-if="column.key === 'lastLoginAt'">
            {{ formatDateTime(record.lastLoginAt) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-popconfirm
                v-if="record.status === 1"
                title="确认封禁该用户？将强制下线并禁止登录。"
                @confirm="banUser(record.id)"
              >
                <a-button type="link" size="small" danger>封禁</a-button>
              </a-popconfirm>
              <a-button v-else type="link" size="small" @click="unbanUser(record.id)">解封</a-button>
              <a-popconfirm title="确认强制下线该用户？" @confirm="forceLogout(record.id)">
                <a-button type="link" size="small">强制下线</a-button>
              </a-popconfirm>
              <a-popconfirm
                title="确认注销该用户？将软删除账号并强制下线，此操作不可恢复。"
                @confirm="deregisterUser(record.id)"
              >
                <a-button type="link" size="small" danger>注销</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  deletePlatformUser,
  fetchPlatformUsers,
  forceLogoutPlatformUser,
  updatePlatformUser,
  type PlatformUser,
} from '@/api/platform'
import { formatDateTime } from '@/utils/datetime'
import { formatMemberships } from '@/views/platform/shared/utils'
import '@/views/platform/shared/styles.css'

const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<number | undefined>()
const accountTypeFilter = ref<string | undefined>()
const users = ref<PlatformUser[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const columns = [
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '类型', key: 'accountType', width: 90 },
  { title: '所属租户', key: 'memberships', ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '最近登录', key: 'lastLoginAt', width: 170 },
  { title: '登录 IP', dataIndex: 'lastLoginIp', key: 'lastLoginIp', width: 130 },
  { title: '操作', key: 'actions', width: 220 },
]

const pagination = computed(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
}))

async function loadUsers() {
  loading.value = true
  try {
    const res = await fetchPlatformUsers({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value,
      accountType: accountTypeFilter.value,
    })
    users.value = res.data.data.list
    total.value = res.data.data.total
  } catch {
    message.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 10
  loadUsers()
}

async function banUser(id: number) {
  await updatePlatformUser(id, { status: 0 })
  message.success('用户已封禁')
  await loadUsers()
}

async function unbanUser(id: number) {
  await updatePlatformUser(id, { status: 1 })
  message.success('用户已解封')
  await loadUsers()
}

async function forceLogout(id: number) {
  await forceLogoutPlatformUser(id)
  message.success('已强制下线')
}

async function deregisterUser(id: number) {
  await deletePlatformUser(id)
  message.success('用户已注销')
  await loadUsers()
}

onMounted(loadUsers)
</script>

<style scoped>
.list-panel {
  padding: 16px;
}
</style>
