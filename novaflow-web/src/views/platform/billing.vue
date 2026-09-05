<template>
  <div class="platform-admin-page page-shell">
    <div class="page-header">
      <div>
        <h1>计费大盘</h1>
        <p>全平台 Token 用量、费用与租户/模型 Top 排行</p>
      </div>
      <a-button :loading="exporting" @click="exportBilling">导出 CSV</a-button>
    </div>

    <div class="page-card list-panel">
      <div class="list-toolbar">
        <div class="list-toolbar-filters">
          <a-date-picker
            v-model:value="billingMonth"
            picker="month"
            format="YYYY-MM"
            placeholder="选择月份"
            style="width: 140px"
            @change="loadBillingOverview"
          />
        </div>
        <span v-if="overview" class="list-toolbar-meta">{{ overview.month }} 全平台用量</span>
      </div>

      <a-spin :spinning="loading">
        <div v-if="overview" class="ops-stats-grid">
          <div class="stat-item">
            <span class="label">本月 Token</span>
            <strong>{{ formatPlatformNumber(overview.totalTokens) }}</strong>
            <span class="sub-label">上月 {{ formatPlatformNumber(overview.prevMonthTokens) }}</span>
          </div>
          <div class="stat-item">
            <span class="label">调用次数</span>
            <strong>{{ formatPlatformNumber(overview.totalCalls) }}</strong>
          </div>
          <div class="stat-item">
            <span class="label">费用 (CNY)</span>
            <strong>{{ formatPlatformCost(overview.costCny) }}</strong>
          </div>
          <div class="stat-item">
            <span class="label">费用 (USD)</span>
            <strong>{{ formatPlatformCost(overview.costUsd) }}</strong>
          </div>
        </div>

        <a-row v-if="overview" :gutter="16" class="ops-panels">
          <a-col :span="12">
            <div class="ops-panel-title">租户 Token Top 10</div>
            <a-table :columns="tenantUsageColumns" :data-source="overview.topTenants" :pagination="false" row-key="tenantId" size="small">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'tenantName'">
                  <router-link :to="tenantDetailPath(record.tenantId)">{{ record.tenantName }}</router-link>
                </template>
              </template>
            </a-table>
          </a-col>
          <a-col :span="12">
            <div class="ops-panel-title">模型 Token Top 10</div>
            <a-table
              :columns="modelUsageColumns"
              :data-source="overview.topModels"
              :pagination="false"
              :row-key="(record: PlatformModelUsage) => record.modelName || record.displayName || 'unknown'"
              size="small"
            />
          </a-col>
        </a-row>

        <div v-if="overview?.dailyTrend?.length" class="trend-panel">
          <div class="ops-panel-title">每日 Token 趋势</div>
          <div class="trend-bars">
            <div v-for="point in overview.dailyTrend" :key="point.label" class="trend-bar-item">
              <span class="trend-label">{{ point.label }}</span>
              <div class="trend-bar-track">
                <div class="trend-bar-fill" :style="{ width: trendBarWidth(point.tokens) }" />
              </div>
              <span class="trend-value">{{ formatPlatformNumber(point.tokens) }}</span>
            </div>
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  downloadPlatformBillingExport,
  fetchPlatformBillingOverview,
  type PlatformBillingOverview,
  type PlatformModelUsage,
} from '@/api/platform'
import { platformPath } from '@/config/deploy'
import { formatPlatformCost, formatPlatformNumber } from '@/views/platform/shared/utils'
import '@/views/platform/shared/styles.css'

const loading = ref(false)
const exporting = ref(false)
const billingMonth = ref<Dayjs>(dayjs())
const overview = ref<PlatformBillingOverview | null>(null)

const tenantUsageColumns = [
  { title: '租户', dataIndex: 'tenantName', key: 'tenantName' },
  { title: '调用', dataIndex: 'calls', key: 'calls', width: 100 },
  { title: 'Token', dataIndex: 'tokens', key: 'tokens', width: 120 },
]

function tenantDetailPath(id: number) {
  return platformPath(`/platform/tenants/${id}`)
}

const modelUsageColumns = [
  {
    title: '模型',
    key: 'modelName',
    customRender: ({ record }: { record: PlatformModelUsage }) => record.displayName || record.modelName || '-',
  },
  { title: '调用', dataIndex: 'calls', key: 'calls', width: 100 },
  { title: 'Token', dataIndex: 'tokens', key: 'tokens', width: 120 },
]

function trendBarWidth(tokens: number) {
  const max = overview.value?.dailyTrend?.reduce((acc, item) => Math.max(acc, item.tokens), 0) ?? 0
  if (!max) return '0%'
  return `${Math.max(4, Math.round((tokens / max) * 100))}%`
}

async function loadBillingOverview() {
  loading.value = true
  try {
    const res = await fetchPlatformBillingOverview(billingMonth.value?.format('YYYY-MM'))
    overview.value = res.data.data
  } catch {
    message.error('加载计费大盘失败')
  } finally {
    loading.value = false
  }
}

async function exportBilling() {
  exporting.value = true
  try {
    const month = billingMonth.value?.format('YYYY-MM')
    const blob = await downloadPlatformBillingExport(month)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `platform-billing-${month || 'current'}.csv`
    link.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(loadBillingOverview)
</script>

<style scoped>
.list-panel {
  padding: 16px;
}
</style>
