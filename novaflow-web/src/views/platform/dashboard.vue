<template>
  <div class="platform-admin-page platform-page page-shell" data-testid="platform-dashboard">
    <div class="page-header">
      <div>
        <h1>运营概览</h1>
        <p>全平台租户、用户与资源用量总览</p>
      </div>
      <a-button :loading="loading" @click="loadOverview">刷新</a-button>
    </div>

    <a-spin :spinning="loading">
      <div v-if="stats" class="stats-grid page-card">
        <div class="stat-item">
          <span class="label">租户总数</span>
          <strong>{{ formatPlatformNumber(stats.tenantCount) }}</strong>
        </div>
        <div class="stat-item">
          <span class="label">活跃租户</span>
          <strong>{{ formatPlatformNumber(stats.activeTenantCount) }}</strong>
        </div>
        <div class="stat-item">
          <span class="label">用户总数</span>
          <strong>{{ formatPlatformNumber(stats.totalUsers) }}</strong>
        </div>
        <div class="stat-item">
          <span class="label">成员总数</span>
          <strong>{{ formatPlatformNumber(stats.totalMembers) }}</strong>
        </div>
        <div class="stat-item">
          <span class="label">Agent 总数</span>
          <strong>{{ formatPlatformNumber(stats.totalAgents) }}</strong>
        </div>
        <div class="stat-item">
          <span class="label">本月 Token</span>
          <strong>{{ formatPlatformNumber(stats.tokensUsedThisMonth) }}</strong>
        </div>
      </div>

      <a-row v-if="overview" :gutter="16" class="chart-row">
        <a-col :span="12" :xs="24">
          <div class="page-card chart-card">
            <h3>近 14 天新增租户</h3>
            <VChart class="chart" :option="tenantGrowthOption" autoresize />
          </div>
        </a-col>
        <a-col :span="12" :xs="24">
          <div class="page-card chart-card">
            <h3>近 14 天 Token 用量</h3>
            <VChart class="chart" :option="tokenTrendOption" autoresize />
          </div>
        </a-col>
      </a-row>

      <div v-if="overview" class="page-card health-panel">
        <h3>租户健康度（需关注）</h3>
        <a-table
          :columns="healthColumns"
          :data-source="overview.tenantHealth"
          :pagination="false"
          row-key="tenantId"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'tenantName'">
              <router-link :to="tenantDetailPath(record.tenantId)">{{ record.tenantName }}</router-link>
            </template>
            <template v-else-if="column.key === 'healthStatus'">
              <a-tag :color="healthColor(record.healthStatus)">{{ healthLabel(record.healthStatus) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'reasons'">
              {{ record.reasons?.join('；') || '-' }}
            </template>
            <template v-else-if="column.key === 'tokenUsedPercent'">
              <span v-if="record.tokenUsedPercent != null">{{ record.tokenUsedPercent }}%</span>
              <span v-else>-</span>
            </template>
          </template>
          <template #emptyText>
            <a-empty description="暂无需要关注的租户" />
          </template>
        </a-table>
      </div>

      <div class="page-card quick-links">
        <h3>快捷入口</h3>
        <a-row :gutter="[12, 12]">
          <a-col v-for="item in quickLinks" :key="item.path" :span="8" :xs="24" :sm="12" :md="8">
            <router-link :to="item.path" class="quick-link-card">
              <strong>{{ item.label }}</strong>
              <span>{{ item.desc }}</span>
            </router-link>
          </a-col>
        </a-row>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import {
  fetchPlatformDashboardOverview,
  type PlatformDashboardOverview,
  type PlatformGlobalStats,
} from '@/api/platform'
import { platformPath } from '@/config/deploy'
import { formatPlatformNumber } from '@/views/platform/shared/utils'
import '@/views/platform/shared/styles.css'

use([CanvasRenderer, BarChart, LineChart, GridComponent, TooltipComponent])

const loading = ref(false)
const overview = ref<PlatformDashboardOverview | null>(null)

const stats = computed<PlatformGlobalStats | null>(() => overview.value?.stats ?? null)

const quickLinks = [
  { path: platformPath('/platform/tenants'), label: '租户管理', desc: '配额、状态与企业信息' },
  { path: platformPath('/platform/users'), label: '用户管理', desc: '封禁、解封与强制下线' },
  { path: platformPath('/platform/billing'), label: '计费大盘', desc: '全平台 Token 与费用' },
  { path: platformPath('/platform/models'), label: '模型概览', desc: '供应商与配置统计' },
  { path: platformPath('/platform/security'), label: 'IP 黑名单', desc: '登录与 API 拦截' },
  { path: platformPath('/platform/audit'), label: '审计日志', desc: '跨租户操作留痕' },
]

const healthColumns = [
  { title: '租户', key: 'tenantName' },
  { title: '健康度', key: 'healthStatus', width: 100 },
  { title: 'Token 使用率', key: 'tokenUsedPercent', width: 110 },
  { title: '原因', key: 'reasons' },
]

const tenantGrowthOption = computed(() => {
  const points = overview.value?.tenantGrowthTrend || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: points.map((item) => item.label) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: points.map((item) => item.tokens),
      itemStyle: { color: '#6366f1' },
    }],
  }
})

const tokenTrendOption = computed(() => {
  const points = overview.value?.tokenUsageTrend || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 16, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: points.map((item) => item.label) },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      smooth: true,
      data: points.map((item) => item.tokens),
      areaStyle: { color: 'rgba(99, 102, 241, 0.12)' },
      lineStyle: { color: '#4f46e5' },
      itemStyle: { color: '#4f46e5' },
    }],
  }
})

function tenantDetailPath(id: number) {
  return platformPath(`/platform/tenants/${id}`)
}

function healthColor(status: string) {
  if (status === 'CRITICAL') return 'red'
  if (status === 'WARNING') return 'orange'
  return 'green'
}

function healthLabel(status: string) {
  if (status === 'CRITICAL') return '严重'
  if (status === 'WARNING') return '警告'
  return '健康'
}

async function loadOverview() {
  loading.value = true
  try {
    const res = await fetchPlatformDashboardOverview()
    overview.value = res.data.data
  } catch {
    message.error('加载运营概览失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadOverview)
</script>

<style scoped>
.chart-row {
  margin-bottom: 16px;
}

.chart-card {
  padding: 16px;
  margin-bottom: 16px;
}

.chart-card h3,
.health-panel h3,
.quick-links h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.chart {
  height: 260px;
}

.health-panel {
  padding: 16px;
  margin-bottom: 16px;
}

.quick-links {
  padding: 16px;
}

.quick-link-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 10px;
  color: inherit;
  transition: border-color 0.15s, background 0.15s;
}

.quick-link-card:hover {
  border-color: var(--platform-accent);
  background: var(--platform-accent-soft);
}

.quick-link-card span {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
