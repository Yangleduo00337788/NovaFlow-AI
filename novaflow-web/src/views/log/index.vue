<template>
  <div class="log-page">
    <div class="page-header">
      <div>
        <h1>调用日志</h1>
        <p>查看 Agent 与模型的 Token 消耗、耗时与成本明细</p>
      </div>
    </div>

    <div class="page-card toolbar">
      <a-space wrap>
        <a-select
          v-model:value="agentId"
          allow-clear
          placeholder="筛选 Agent"
          style="width: 220px"
          :loading="agentsLoading"
          :options="agentOptions"
          @change="onSearch"
        />
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索 Agent / 模型"
          style="width: 260px"
          allow-clear
          @search="onSearch"
        />
      </a-space>
      <span class="toolbar-meta">共 {{ total }} 条记录</span>
    </div>

    <div class="page-card">
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'agentName'">
            <span class="agent-name">{{ record.agentName }}</span>
          </template>
          <template v-else-if="column.key === 'model'">
            {{ record.displayName || record.modelName || '-' }}
          </template>
          <template v-else-if="column.key === 'usageType'">
            <a-tag>{{ record.usageType || 'chat' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'totalTokens'">
            {{ formatNumber(record.totalTokens) }}
          </template>
          <template v-else-if="column.key === 'latencyMs'">
            {{ formatLatency(record.latencyMs) }}
          </template>
          <template v-else-if="column.key === 'costLabel'">
            {{ record.costLabel || '-' }}
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchAgents } from '@/api/agent'
import { fetchTokenUsageLogs, type TokenUsageLogItem } from '@/api/log'
import { formatDateTime } from '@/utils/datetime'

const loading = ref(false)
const agentsLoading = ref(false)
const list = ref<TokenUsageLogItem[]>([])
const keyword = ref('')
const agentId = ref<number | undefined>()
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const agentOptions = ref<Array<{ label: string; value: number }>>([])

const columns = [
  { title: '时间', key: 'createdAt', width: 180 },
  { title: 'Agent', key: 'agentName', dataIndex: 'agentName' },
  { title: '模型', key: 'model', width: 180 },
  { title: '类型', key: 'usageType', width: 90 },
  { title: 'Tokens', key: 'totalTokens', width: 100 },
  { title: '耗时', key: 'latencyMs', width: 90 },
  { title: '成本', key: 'costLabel', width: 100 },
]

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
})

function formatNumber(value?: number) {
  return value != null ? value.toLocaleString() : '-'
}

function formatLatency(value?: number) {
  if (value == null || value <= 0) return '-'
  if (value < 1000) return `${value}ms`
  return `${(value / 1000).toFixed(1)}s`
}

async function loadAgents() {
  agentsLoading.value = true
  try {
    const res = await fetchAgents({ page: 1, pageSize: 100 })
    agentOptions.value = res.data.data.list.map((item) => ({
      label: item.agentName,
      value: item.id,
    }))
  } finally {
    agentsLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchTokenUsageLogs({
      page: page.value,
      pageSize: pageSize.value,
      agentId: agentId.value,
      keyword: keyword.value || undefined,
    })
    list.value = res.data.data.list
    total.value = res.data.data.total
    pagination.total = res.data.data.total
    pagination.current = res.data.data.page
    pagination.pageSize = res.data.data.pageSize
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载调用日志失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  loadData()
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 20
  loadData()
}

onMounted(() => {
  loadAgents()
  loadData()
})
</script>

<style scoped>
.log-page {
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

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-meta {
  color: var(--text-muted);
  font-size: 13px;
  white-space: nowrap;
}

.agent-name {
  font-weight: 500;
}
</style>
