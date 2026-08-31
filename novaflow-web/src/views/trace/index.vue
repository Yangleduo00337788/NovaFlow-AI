<template>
  <div class="trace-page page-shell">
    <div class="page-header">
      <div>
        <h1>链路分析</h1>
        <p>追踪 Agent 与工作流调用链路，定位性能瓶颈与异常节点</p>
      </div>
    </div>

    <div class="list-toolbar page-card">
      <a-space wrap>
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索 Trace ID / 名称"
          style="width: 260px"
          allow-clear
          @search="onSearch"
        />
        <a-select v-model:value="spanType" allow-clear placeholder="类型" style="width: 120px" @change="onSearch">
          <a-select-option value="workflow">工作流</a-select-option>
          <a-select-option value="agent">Agent</a-select-option>
        </a-select>
        <a-select v-model:value="status" allow-clear placeholder="状态" style="width: 120px" @change="onSearch">
          <a-select-option :value="0">运行中</a-select-option>
          <a-select-option :value="1">成功</a-select-option>
          <a-select-option :value="2">失败</a-select-option>
        </a-select>
        <a-select v-model:value="timeRange" style="width: 140px" @change="onSearch">
          <a-select-option value="1h">近 1 小时</a-select-option>
          <a-select-option value="24h">近 24 小时</a-select-option>
          <a-select-option value="7d">近 7 天</a-select-option>
        </a-select>
      </a-space>
      <span class="list-toolbar-meta">共 {{ total }} 条</span>
    </div>

    <div class="page-card page-table-card trace-card">
      <a-table
          :columns="columns"
          :data-source="list"
          :loading="loading"
          row-key="traceId"
          :pagination="pagination"
          @change="onTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'traceId'">
              <a class="trace-link" @click="openDetail(record.traceId)">{{ record.traceId }}</a>
            </template>
            <template v-else-if="column.key === 'spanType'">
              <a-tag>{{ record.spanTypeLabel }}</a-tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">{{ record.statusLabel }}</a-tag>
            </template>
            <template v-else-if="column.key === 'startedAt'">
              {{ formatDateTime(record.startedAt) }}
            </template>
            <template v-else-if="column.key === 'duration'">
              {{ record.durationLabel || '-' }}
            </template>
            <template v-else-if="column.key === 'errorMessage'">
              <span class="error-text">{{ record.errorMessage || '-' }}</span>
            </template>
          </template>
        </a-table>
    </div>

    <a-drawer v-model:open="detailOpen" title="链路详情" width="720" destroy-on-close>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="2" size="small" bordered>
            <a-descriptions-item label="Trace ID" :span="2">{{ detail.traceId }}</a-descriptions-item>
            <a-descriptions-item label="类型">{{ detail.spanTypeLabel }}</a-descriptions-item>
            <a-descriptions-item label="名称">{{ detail.name }}</a-descriptions-item>
            <a-descriptions-item label="状态">{{ detail.statusLabel }}</a-descriptions-item>
            <a-descriptions-item label="耗时">{{ detail.durationLabel || '-' }}</a-descriptions-item>
            <a-descriptions-item label="开始时间" :span="2">{{ formatDateTime(detail.startedAt) }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.errorMessage" label="错误信息" :span="2">
              {{ detail.errorMessage }}
            </a-descriptions-item>
          </a-descriptions>

          <div class="section-title">执行瀑布图</div>
          <a-empty v-if="!detail.nodes?.length" description="暂无 Span 数据" />
          <TraceWaterfall
            v-else
            :nodes="detail.nodes"
            :total-duration-ms="detail.durationMs"
          />

          <div class="section-title">节点明细</div>
          <a-empty v-if="!detail.nodes?.length" description="暂无节点数据" />
          <div v-else class="node-list">
            <div v-for="node in detail.nodes" :key="node.nodeId" class="node-item">
              <div class="node-head">
                <span class="node-name">{{ node.nodeName }}</span>
                <a-tag size="small">{{ node.statusLabel }}</a-tag>
              </div>
              <div class="node-meta">
                <span>{{ node.nodeType }}</span>
                <span>{{ node.durationLabel || '-' }}</span>
              </div>
              <div v-if="node.errorMessage" class="node-error">{{ node.errorMessage }}</div>
            </div>
          </div>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { fetchTraceDetail, fetchTraceSpans, type TraceDetail, type TraceSpan } from '@/api/trace'
import TraceWaterfall from '@/components/trace/TraceWaterfall.vue'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const loading = ref(false)
const list = ref<TraceSpan[]>([])
const keyword = ref('')
const spanType = ref<string | undefined>()
const status = ref<number | undefined>()
const timeRange = ref('24h')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<TraceDetail | null>(null)

const columns = [
  { title: 'Trace ID', key: 'traceId', dataIndex: 'traceId', width: 220 },
  { title: '类型', key: 'spanType', width: 90 },
  { title: '名称', key: 'name', dataIndex: 'name' },
  { title: '状态', key: 'status', width: 90 },
  { title: '耗时', key: 'duration', width: 90 },
  { title: '开始时间', key: 'startedAt', width: 180 },
  { title: '错误', key: 'errorMessage' },
]

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
})

function statusColor(value?: number) {
  if (value === 0) return 'processing'
  if (value === 1) return 'success'
  if (value === 2 || value === 3) return 'error'
  return 'default'
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchTraceSpans({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      type: spanType.value,
      status: status.value,
      timeRange: timeRange.value,
    })
    list.value = res.data.data.list
    total.value = res.data.data.total
    pagination.total = res.data.data.total
    pagination.current = res.data.data.page
    pagination.pageSize = res.data.data.pageSize
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载链路数据失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(traceId: string) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await fetchTraceDetail(traceId)
    detail.value = res.data.data
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载链路详情失败')
  } finally {
    detailLoading.value = false
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
  loadData()
  const traceId = route.query.traceId
  if (typeof traceId === 'string' && traceId) {
    keyword.value = traceId
    openDetail(traceId)
  }
})
</script>

<style scoped>
.trace-card {
  min-height: 0;
}

.trace-link {
  color: #1677ff;
  cursor: pointer;
}

.error-text {
  color: #ff4d4f;
  font-size: 12px;
}

.section-title {
  margin: 20px 0 12px;
  font-weight: 600;
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.node-item {
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
}

.node-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.node-name {
  font-weight: 500;
}

.node-meta {
  margin-top: 4px;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
}

.node-error {
  margin-top: 6px;
  font-size: 12px;
  color: #ff4d4f;
}
</style>
