<template>
  <div class="audit-page">
    <div class="page-header">
      <div>
        <h1>审计日志</h1>
        <p>查询平台与企业内的关键操作记录</p>
      </div>
    </div>

    <div class="page-card filters">
      <a-row :gutter="12">
        <a-col :span="6">
          <a-input v-model:value="filters.keyword" placeholder="关键词（动作/详情/IP）" allow-clear />
        </a-col>
        <a-col :span="5">
          <a-input v-model:value="filters.action" placeholder="动作" allow-clear />
        </a-col>
        <a-col :span="5">
          <a-input v-model:value="filters.resourceType" placeholder="资源类型" allow-clear />
        </a-col>
        <a-col :span="8">
          <a-range-picker v-model:value="dateRange" style="width: 100%" />
        </a-col>
      </a-row>
      <div class="filter-actions">
        <a-button type="primary" @click="search">查询</a-button>
        <a-button @click="resetFilters">重置</a-button>
      </div>
    </div>

    <div class="page-card table-card">
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
          <template v-else-if="column.key === 'detail'">
            <span class="detail-cell" :title="record.detail">{{ record.detail || '-' }}</span>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Dayjs } from 'dayjs'
import { fetchAuditLogs, type AuditLogItem } from '@/api/audit'
import { formatDateTime } from '@/utils/datetime'

const loading = ref(false)
const logs = ref<AuditLogItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const filters = reactive({
  keyword: '',
  action: '',
  resourceType: '',
})

const columns = [
  { title: '时间', key: 'createdAt', width: 180 },
  { title: '租户 ID', dataIndex: 'tenantId', key: 'tenantId', width: 90 },
  { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 90 },
  { title: '动作', dataIndex: 'action', key: 'action', width: 180 },
  { title: '资源类型', dataIndex: 'resourceType', key: 'resourceType', width: 120 },
  { title: '资源 ID', dataIndex: 'resourceId', key: 'resourceId', width: 90 },
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
    const res = await fetchAuditLogs({
      page: page.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      action: filters.action || undefined,
      resourceType: filters.resourceType || undefined,
      startDate: dateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: dateRange.value?.[1]?.format('YYYY-MM-DD'),
    })
    logs.value = res.data.data.list
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadLogs()
}

function resetFilters() {
  filters.keyword = ''
  filters.action = ''
  filters.resourceType = ''
  dateRange.value = null
  search()
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 20
  loadLogs()
}

onMounted(loadLogs)
</script>

<style scoped>
.audit-page {
  display: flex;
  flex-direction: column;
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

.filters {
  padding: 16px;
}

.filter-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.table-card {
  padding: 16px;
}

.detail-cell {
  display: inline-block;
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
