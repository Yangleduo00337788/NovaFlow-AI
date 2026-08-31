<template>
  <div class="billing-page page-shell" data-testid="billing-page">
    <div class="page-header">
      <div>
        <h1>账单与用量</h1>
        <p>查看本月 Token 消耗、预估费用与配额使用情况</p>
      </div>
      <div class="page-header-actions">
        <a-date-picker
          v-model:value="selectedMonth"
          picker="month"
          format="YYYY-MM"
          :allow-clear="false"
          @change="onMonthChange"
        />
        <a-button :loading="loading" @click="loadData">
          <ReloadOutlined />
          刷新
        </a-button>
        <a-button @click="exportCsv" :disabled="!records.length">
          <DownloadOutlined />
          导出 CSV
        </a-button>
        <a-dropdown v-if="canManageBilling">
          <a-button>
            <DownloadOutlined />
            导出账单
            <DownOutlined />
          </a-button>
          <template #overlay>
            <a-menu @click="onExportMenu">
              <a-menu-item key="excel">导出 Excel（全月）</a-menu-item>
              <a-menu-item key="pdf">导出 PDF（全月）</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button @click="openReceipt">
          <PrinterOutlined />
          打印小票
        </a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <div class="billing-body">
      <div class="metrics-grid">
        <div v-for="item in overview.metrics" :key="item.key" class="metric-card page-card">
          <div class="metric-label">{{ item.label }}</div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-hint">{{ item.hint }}</div>
        </div>
      </div>

      <div class="quota-grid">
        <div class="page-card quota-card">
          <div class="section-head">
            <div class="section-title">套餐与配额</div>
            <a-space v-if="canManageBilling" size="small">
              <a-button size="small" @click="openQuotaModal">配额设置</a-button>
              <a-button size="small" @click="openAlertDrawer">预警配置</a-button>
            </a-space>
          </div>
          <div class="quota-head">
            <div>
              <div class="plan-name">{{ overview.quota.planTypeLabel || '免费版' }}</div>
              <div class="plan-expire">到期时间：{{ formatDate(overview.quota.expireAt) }}</div>
            </div>
            <a-tag color="blue">{{ overview.periodLabel }}</a-tag>
          </div>
          <div class="quota-item">
            <div class="quota-row">
              <span>本月 Token</span>
              <span>
                {{ formatNumber(overview.quota.usedTokens) }}
                <template v-if="overview.quota.monthlyTokenQuota">
                  / {{ formatNumber(overview.quota.monthlyTokenQuota) }}
                </template>
              </span>
            </div>
            <a-progress
              v-if="overview.quota.tokenUsedPercent != null"
              :percent="overview.quota.tokenUsedPercent"
              :status="overview.quota.tokenUsedPercent >= 90 ? 'exception' : 'active'"
            />
          </div>
          <div class="quota-item">
            <div class="quota-row">
              <span>成员席位</span>
              <span>{{ overview.quota.memberCount }} / {{ overview.quota.maxMembers }}</span>
            </div>
            <a-progress :percent="overview.quota.memberUsedPercent" />
          </div>
          <div class="quota-meta">
            <span>Agent 配额：{{ overview.quota.maxAgents ?? '-' }}</span>
            <span>知识库配额：{{ overview.quota.maxKnowledge ?? '-' }}</span>
          </div>
        </div>

        <div class="page-card trend-card">
          <div class="section-title">本月 Token 趋势</div>
          <v-chart class="trend-chart" :option="trendOption" autoresize />
        </div>
      </div>

      <div class="content-grid">
        <div class="page-card">
          <div class="section-title">按类型分布</div>
          <a-empty v-if="!overview.usageByType.length" description="暂无用量数据" />
          <div v-else class="usage-type-list">
            <div v-for="item in overview.usageByType" :key="item.usageType" class="usage-type-item">
              <div>
                <strong>{{ item.usageTypeLabel }}</strong>
                <div class="usage-type-meta">{{ formatNumber(item.calls) }} 次调用</div>
              </div>
              <span>{{ formatNumber(item.tokens) }} tokens</span>
            </div>
          </div>
        </div>

        <div class="page-card">
          <div class="section-title">模型消耗 Top 5</div>
          <a-empty v-if="!overview.topModels.length" description="暂无模型用量" />
          <div v-else class="model-list">
            <div v-for="(item, index) in overview.topModels" :key="item.modelName" class="model-item">
              <span class="rank-no" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <div class="model-main">
                <div class="model-name">{{ item.displayName || item.modelName }}</div>
                <div class="model-meta">{{ formatNumber(item.calls) }} 次 · {{ formatNumber(item.tokens) }} tokens</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="page-card records-card list-panel">
        <div class="list-toolbar">
          <div class="list-toolbar-filters">
            <a-select
              v-model:value="agentId"
              allow-clear
              placeholder="筛选 Agent"
              style="width: 200px"
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
          :data-source="records"
          :loading="recordsLoading"
          row-key="id"
          :pagination="pagination"
          @change="onTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'usageType'">
              <a-tag>{{ record.usageType || 'chat' }}</a-tag>
            </template>
            <template v-else-if="column.key === 'totalTokens'">
              {{ formatNumber(record.totalTokens) }}
            </template>
            <template v-else-if="column.key === 'createdAt'">
              {{ formatDateTime(record.createdAt) }}
            </template>
          </template>
        </a-table>
      </div>
      </div>
    </a-spin>

    <BillingReceiptPrinter
      v-model:open="receiptOpen"
      :overview="overview"
      :month="currentMonth()"
      :total-records="total"
    />

    <a-modal
      v-model:open="quotaModalOpen"
      title="月度 Token 配额设置"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="quotaSaving"
      @ok="saveQuota"
    >
      <a-form layout="vertical">
        <a-form-item label="月度 Token 配额">
          <a-input-number
            v-model:value="quotaForm.monthlyTokenQuota"
            :min="1"
            :step="10000"
            style="width: 100%"
            placeholder="请输入月度 Token 上限"
          />
        </a-form-item>
        <a-alert
          type="info"
          show-icon
          message="配额用于控制本月 Token 消耗上限，达到预警阈值时将发送站内通知。"
        />
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="alertDrawerOpen"
      title="配额预警配置"
      width="420"
      :footer-style="{ textAlign: 'right' }"
    >
      <a-spin :spinning="alertsLoading">
        <div v-for="alert in alerts" :key="alert.id" class="alert-item">
          <div class="alert-head">
            <div>
              <div class="alert-name">{{ alert.alertName }}</div>
              <div class="alert-meta">
                阈值 {{ alert.thresholdPercent }}%
                <template v-if="alert.lastTriggeredAt">
                  · 上次触发 {{ formatDateTime(alert.lastTriggeredAt) }}
                </template>
              </div>
            </div>
            <a-switch
              :checked="alert.enabled"
              :loading="alertSavingId === alert.id"
              @change="(checked: boolean) => toggleAlert(alert, checked)"
            />
          </div>
          <div class="alert-channels">
            通知渠道：{{ alert.notifyChannels?.join('、') || '站内信' }}
          </div>
        </div>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { DownloadOutlined, DownOutlined, PrinterOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { fetchAgents } from '@/api/agent'
import {
  downloadBillingExport,
  fetchBillingAlerts,
  fetchBillingOverview,
  fetchBillingRecords,
  saveBillingAlert,
  updateBillingQuota,
  type BillingAlert,
  type BillingOverview,
} from '@/api/billing'
import type { TokenUsageLogItem } from '@/api/log'
import { formatDateTime } from '@/utils/datetime'
import { useAuthStore } from '@/stores/auth'
import BillingReceiptPrinter from './BillingReceiptPrinter.vue'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent])

const auth = useAuthStore()
const canManageBilling = computed(() => auth.hasPermission('billing:manage'))

const loading = ref(false)
const recordsLoading = ref(false)
const selectedMonth = ref<Dayjs>(dayjs())
const overview = ref<BillingOverview>({
  periodLabel: '',
  totalCalls: 0,
  totalTokens: 0,
  totalCostLabel: '¥0.00',
  tokenChangePercent: '0%',
  callChangePercent: '0%',
  metrics: [],
  dailyTrend: [],
  usageByType: [],
  topModels: [],
  quota: {
    planType: 'free',
    planTypeLabel: '免费版',
    usedTokens: 0,
    memberCount: 0,
    maxMembers: 0,
    memberUsedPercent: 0,
  },
})
const records = ref<TokenUsageLogItem[]>([])
const keyword = ref('')
const agentId = ref<number | undefined>()
const usageType = ref<string | undefined>()
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const receiptOpen = ref(false)
const quotaModalOpen = ref(false)
const quotaSaving = ref(false)
const alertDrawerOpen = ref(false)
const alertsLoading = ref(false)
const alertSavingId = ref<number | null>(null)
const alerts = ref<BillingAlert[]>([])
const quotaForm = reactive({
  monthlyTokenQuota: undefined as number | undefined,
})
const agentOptions = ref<Array<{ label: string; value: number }>>([])
const usageTypeOptions = [
  { label: '对话', value: 'chat' },
  { label: '工作流', value: 'workflow' },
]

const columns = [
  { title: '时间', key: 'createdAt', width: 180 },
  { title: 'Agent', dataIndex: 'agentName', key: 'agentName' },
  { title: '模型', dataIndex: 'displayName', key: 'model' },
  { title: '类型', key: 'usageType', width: 90 },
  { title: 'Tokens', key: 'totalTokens', width: 100 },
  { title: '成本', dataIndex: 'costLabel', key: 'costLabel', width: 100 },
]

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
})

const trendOption = computed(() => ({
  grid: { left: 40, right: 16, top: 24, bottom: 28 },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: overview.value.dailyTrend.map((item) => item.label),
  },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'line',
      smooth: true,
      data: overview.value.dailyTrend.map((item) => item.tokens),
      areaStyle: { color: 'rgba(99, 102, 241, 0.12)' },
      lineStyle: { color: '#6366f1', width: 2 },
      itemStyle: { color: '#6366f1' },
    },
  ],
}))

function currentMonth() {
  return selectedMonth.value.format('YYYY-MM')
}

function formatNumber(value?: number) {
  return value != null ? value.toLocaleString() : '-'
}

function formatDate(value?: string) {
  if (!value) return '-'
  return value.slice(0, 10)
}

async function loadAgents() {
  const res = await fetchAgents({ page: 1, pageSize: 100 })
  agentOptions.value = res.data.data.list.map((item) => ({
    label: item.agentName,
    value: item.id,
  }))
}

async function loadOverview() {
  const res = await fetchBillingOverview(currentMonth())
  overview.value = res.data.data
}

async function loadRecords() {
  recordsLoading.value = true
  try {
    const res = await fetchBillingRecords({
      page: page.value,
      pageSize: pageSize.value,
      agentId: agentId.value,
      usageType: usageType.value,
      month: currentMonth(),
      keyword: keyword.value || undefined,
    })
    records.value = res.data.data.list
    total.value = res.data.data.total
    pagination.total = res.data.data.total
    pagination.current = res.data.data.page
    pagination.pageSize = res.data.data.pageSize
  } finally {
    recordsLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    await Promise.all([loadOverview(), loadRecords()])
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载账单数据失败')
  } finally {
    loading.value = false
  }
}

function onMonthChange() {
  page.value = 1
  loadData()
}

function onSearch() {
  page.value = 1
  loadRecords()
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 20
  loadRecords()
}

function openReceipt() {
  receiptOpen.value = true
}

function openQuotaModal() {
  quotaForm.monthlyTokenQuota = overview.value.quota.monthlyTokenQuota || overview.value.quota.usedTokens || 100000
  quotaModalOpen.value = true
}

async function saveQuota() {
  if (!quotaForm.monthlyTokenQuota || quotaForm.monthlyTokenQuota <= 0) {
    message.warning('请输入有效的月度 Token 配额')
    return
  }
  quotaSaving.value = true
  try {
    const res = await updateBillingQuota(quotaForm.monthlyTokenQuota)
    overview.value.quota = res.data.data
    quotaModalOpen.value = false
    message.success('配额已更新')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    quotaSaving.value = false
  }
}

async function openAlertDrawer() {
  alertDrawerOpen.value = true
  alertsLoading.value = true
  try {
    const res = await fetchBillingAlerts()
    alerts.value = res.data.data
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载预警配置失败')
  } finally {
    alertsLoading.value = false
  }
}

async function toggleAlert(alert: BillingAlert, enabled: boolean) {
  alertSavingId.value = alert.id
  try {
    const res = await saveBillingAlert({
      id: alert.id,
      alertName: alert.alertName,
      thresholdPercent: alert.thresholdPercent,
      enabled,
      notifyChannels: alert.notifyChannels,
    })
    const index = alerts.value.findIndex((item) => item.id === alert.id)
    if (index >= 0) {
      alerts.value[index] = res.data.data
    }
    message.success(enabled ? '预警已启用' : '预警已关闭')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    alertSavingId.value = null
  }
}

async function onExportMenu({ key }: { key: string }) {
  if (key !== 'excel' && key !== 'pdf') return
  try {
    const blob = await downloadBillingExport(currentMonth(), key)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `billing-${currentMonth()}.${key === 'pdf' ? 'pdf' : 'xlsx'}`
    link.click()
    URL.revokeObjectURL(url)
    message.success(key === 'pdf' ? 'PDF 已下载' : 'Excel 已下载')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '导出失败')
  }
}

function exportCsv() {
  const header = ['时间', 'Agent', '模型', '类型', 'Tokens', '成本']
  const rows = records.value.map((item) => [
    formatDateTime(item.createdAt),
    item.agentName,
    item.displayName || item.modelName || '',
    item.usageType || 'chat',
    String(item.totalTokens ?? 0),
    item.costLabel || '',
  ])
  const csv = [header, ...rows]
    .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `billing-${currentMonth()}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(async () => {
  try {
    await loadAgents()
    await loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  }
})
</script>

<style scoped>
.billing-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.page-header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.page-header h1 {
  margin: 0 0 4px;
  font-size: 22px;
}

.page-header p {
  margin: 0;
  color: var(--text-secondary);
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.billing-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-card {
  padding: 14px 16px;
}

.metric-label {
  color: var(--text-muted);
  font-size: 12px;
}

.metric-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
}

.metric-hint {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;
}

.quota-grid,
.content-grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 12px;
}

.quota-card,
.trend-card {
  padding: 14px 16px;
}

.records-card {
  padding: 0;
}

.section-title {
  font-weight: 600;
  margin-bottom: 14px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-head .section-title {
  margin-bottom: 0;
}

.alert-item {
  padding: 14px 0;
  border-bottom: 1px solid var(--border-color, #f0f0f0);
}

.alert-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.alert-name {
  font-weight: 600;
}

.alert-meta,
.alert-channels {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}

.quota-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.plan-name {
  font-size: 18px;
  font-weight: 700;
}

.plan-expire {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 13px;
}

.quota-item + .quota-item {
  margin-top: 14px;
}

.quota-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
}

.quota-meta {
  display: flex;
  gap: 16px;
  margin-top: 16px;
  color: var(--text-secondary);
  font-size: 13px;
}

.trend-chart {
  height: 220px;
}

.usage-type-list,
.model-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.usage-type-item,
.model-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.usage-type-meta,
.model-meta {
  color: var(--text-secondary);
  font-size: 12px;
  margin-top: 2px;
}

.rank-no {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: #f1f5f9;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.rank-no.top {
  background: #eef2ff;
  color: #4f46e5;
}

.model-item {
  justify-content: flex-start;
}

.model-main {
  flex: 1;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-meta {
  color: var(--text-muted);
  font-size: 13px;
}

@media (max-width: 1200px) {
  .metrics-grid,
  .quota-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
