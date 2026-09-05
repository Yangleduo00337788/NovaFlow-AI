<template>
  <div class="platform-admin-page page-shell">
    <div class="page-header">
      <div>
        <h1>登录日志</h1>
        <p>全平台用户登录、登出与失败记录</p>
      </div>
    </div>

    <div class="page-card list-panel">
      <div class="list-toolbar">
        <div class="list-toolbar-filters">
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索详情、IP、动作"
            allow-clear
            style="width: 260px"
            @search="loadLogs"
          />
          <a-range-picker v-model:value="dateRange" style="width: 260px" @change="loadLogs" />
        </div>
        <span class="list-toolbar-meta">共 {{ total }} 条记录</span>
      </div>
      <a-table
        :columns="columns"
        :data-source="logs"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-tag :color="record.action.includes('failed') ? 'error' : 'default'">{{ record.action }}</a-tag>
          </template>
          <template v-else-if="column.key === 'detail'">
            <span class="detail-cell" :title="record.detail">{{ record.detail || '-' }}</span>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { fetchPlatformLoginLogs, type PlatformLoginLog } from '@/api/platform'
import { formatDateTime } from '@/utils/datetime'
import '@/views/platform/shared/styles.css'

const loading = ref(false)
const keyword = ref('')
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const logs = ref<PlatformLoginLog[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const columns = [
  { title: '时间', key: 'createdAt', width: 170 },
  { title: '租户 ID', dataIndex: 'tenantId', key: 'tenantId', width: 90 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 90 },
  { title: '动作', key: 'action', width: 160 },
  { title: '详情', key: 'detail' },
  { title: 'IP', dataIndex: 'clientIp', key: 'clientIp', width: 130 },
]

const pagination = computed(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
}))

async function loadLogs() {
  loading.value = true
  try {
    const res = await fetchPlatformLoginLogs({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD'),
    })
    logs.value = res.data.data.list
    total.value = res.data.data.total
  } catch {
    message.error('加载登录日志失败')
  } finally {
    loading.value = false
  }
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 20
  loadLogs()
}

onMounted(loadLogs)
</script>

<style scoped>
.list-panel {
  padding: 16px;
}
</style>
