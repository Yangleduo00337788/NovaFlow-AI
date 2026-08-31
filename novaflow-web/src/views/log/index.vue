<template>
  <div class="log-page page-shell">
    <div class="page-header">
      <div>
        <h1>调用日志</h1>
        <p>查看 Agent 与模型的 Token 消耗、耗时与成本明细</p>
      </div>
      <a-button :loading="exporting" @click="onExport">导出 CSV</a-button>
    </div>

    <div class="list-panel page-card">
      <div class="list-toolbar">
        <div class="list-toolbar-filters">
          <a-select
            v-model:value="agentId"
            allow-clear
            placeholder="筛选 Agent"
            style="width: 200px"
            :loading="agentsLoading"
            :options="agentOptions"
            @change="onSearch"
          />
          <a-select
            v-model:value="usageType"
            allow-clear
            placeholder="调用类型"
            style="width: 120px"
            :options="usageTypeOptions"
            @change="onSearch"
          />
          <a-select
            v-model:value="successFilter"
            allow-clear
            placeholder="调用状态"
            style="width: 120px"
            :options="successOptions"
            @change="onSearch"
          />
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索 Agent / 模型"
            style="width: 220px"
            allow-clear
            @search="onSearch"
          />
        </div>
        <span class="list-toolbar-meta">共 {{ total }} 条记录</span>
      </div>
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <span class="log-status" :class="record.success ? 'success' : 'failed'">
              <CheckCircleOutlined v-if="record.success" class="log-status-icon" />
              <CloseCircleOutlined v-else class="log-status-icon" />
              {{ record.statusLabel || (record.success ? '成功' : '失败') }}
            </span>
          </template>
          <template v-else-if="column.key === 'agentName'">
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
          <template v-else-if="column.key === 'errorMessage'">
            <span class="error-text">{{ record.errorMessage || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a @click="openDetail(record)">详情</a>
              <router-link v-if="record.traceId" :to="`/trace?traceId=${encodeURIComponent(record.traceId)}`">
                链路
              </router-link>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-drawer v-model:open="detailOpen" title="调用详情" width="480" destroy-on-close>
      <a-descriptions v-if="detailRecord" :column="1" size="small" bordered>
        <a-descriptions-item label="Agent">{{ detailRecord.agentName }}</a-descriptions-item>
        <a-descriptions-item label="模型">{{ detailRecord.displayName || detailRecord.modelName }}</a-descriptions-item>
        <a-descriptions-item label="类型">{{ detailRecord.usageType }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ detailRecord.statusLabel }}</a-descriptions-item>
        <a-descriptions-item label="输入 Tokens">{{ formatNumber(detailRecord.inputTokens) }}</a-descriptions-item>
        <a-descriptions-item label="输出 Tokens">{{ formatNumber(detailRecord.outputTokens) }}</a-descriptions-item>
        <a-descriptions-item label="总 Tokens">{{ formatNumber(detailRecord.totalTokens) }}</a-descriptions-item>
        <a-descriptions-item label="耗时">{{ formatLatency(detailRecord.latencyMs) }}</a-descriptions-item>
        <a-descriptions-item label="成本">{{ detailRecord.costLabel || '-' }}</a-descriptions-item>
        <a-descriptions-item label="时间">{{ formatDateTime(detailRecord.createdAt) }}</a-descriptions-item>
        <a-descriptions-item v-if="detailRecord.traceId" label="Trace ID">
          <router-link :to="`/trace?traceId=${encodeURIComponent(detailRecord.traceId)}`">
            {{ detailRecord.traceId }}
          </router-link>
        </a-descriptions-item>
        <a-descriptions-item v-if="detailRecord.errorMessage" label="错误信息">
          {{ detailRecord.errorMessage }}
        </a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import { fetchAgents } from '@/api/agent'
import { fetchTokenUsageLogs, exportTokenUsageLogs, type TokenUsageLogItem } from '@/api/log'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const loading = ref(false)
const exporting = ref(false)
const agentsLoading = ref(false)
const list = ref<TokenUsageLogItem[]>([])
const keyword = ref('')
const agentId = ref<number | undefined>()
const usageType = ref<string | undefined>()
const successFilter = ref<boolean | undefined>()
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const agentOptions = ref<Array<{ label: string; value: number }>>([])
const detailOpen = ref(false)
const detailRecord = ref<TokenUsageLogItem | null>(null)

const successOptions = [
  { label: '成功', value: true },
  { label: '失败', value: false },
]

const usageTypeOptions = [
  { label: 'chat', value: 'chat' },
  { label: 'embedding', value: 'embedding' },
  { label: 'rerank', value: 'rerank' },
]

const columns = [
  { title: '时间', key: 'createdAt', width: 180 },
  { title: '状态', key: 'status', width: 100 },
  { title: 'Agent', key: 'agentName', dataIndex: 'agentName' },
  { title: '模型', key: 'model', width: 160 },
  { title: '类型', key: 'usageType', width: 90 },
  { title: 'Tokens', key: 'totalTokens', width: 100 },
  { title: '耗时', key: 'latencyMs', width: 90 },
  { title: '成本', key: 'costLabel', width: 100 },
  { title: '错误', key: 'errorMessage', ellipsis: true },
  { title: '操作', key: 'actions', width: 120 },
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

function openDetail(record: TokenUsageLogItem) {
  detailRecord.value = record
  detailOpen.value = true
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
      success: successFilter.value,
      usageType: usageType.value,
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

async function onExport() {
  exporting.value = true
  try {
    const res = await exportTokenUsageLogs({
      agentId: agentId.value,
      keyword: keyword.value || undefined,
      success: successFilter.value,
      usageType: usageType.value,
    })
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `token-usage-logs-${Date.now()}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '导出失败')
  } finally {
    exporting.value = false
  }
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 20
  loadData()
}

onMounted(async () => {
  await loadAgents()
  await loadData()
  const traceId = route.query.traceId
  if (typeof traceId === 'string' && traceId) {
    keyword.value = traceId
    onSearch()
  }
  const logId = route.query.logId
  if (typeof logId === 'string' && logId) {
    const record = list.value.find((item) => String(item.id) === logId)
    if (record) openDetail(record)
  }
})
</script>

<style scoped>
.log-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: auto;
}

.agent-name {
  font-weight: 500;
}

.log-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.log-status.success {
  color: #52c41a;
}

.log-status.failed {
  color: #ff4d4f;
}

.log-status-icon {
  font-size: 12px;
}

.error-text {
  color: #ff4d4f;
  font-size: 12px;
}
</style>
