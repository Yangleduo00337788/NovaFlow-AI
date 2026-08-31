<template>
  <div class="monitor-page page-shell" data-testid="monitor-page">
    <div class="page-header">
      <div>
        <h1>运行监控</h1>
        <p>服务健康状态、调用指标与近 24 小时趋势</p>
      </div>
      <a-button :loading="loading" @click="loadData">
        <ReloadOutlined />
        刷新
      </a-button>
    </div>

    <a-spin :spinning="loading">
      <div class="metrics-grid">
        <div v-for="item in overview.metrics" :key="item.key" class="metric-card page-card">
          <div class="metric-head">
            <span class="metric-label">{{ item.label }}</span>
            <component :is="metricIcon(item.key)" class="metric-icon" />
          </div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-hint">{{ item.hint }}</div>
        </div>
      </div>

      <div class="content-grid">
        <div class="page-card services-card">
          <div class="section-title">服务健康</div>
          <div class="service-list">
            <div
              v-for="service in overview.services"
              :key="service.key"
              class="service-item"
              :data-testid="`service-${service.key}`"
            >
              <div class="service-main">
                <span class="service-dot" :class="{ healthy: service.healthy }" />
                <div>
                  <div class="service-name">{{ service.name }}</div>
                  <div class="service-detail">{{ service.detail }}</div>
                </div>
              </div>
              <a-tag :color="service.healthy ? 'success' : 'error'">{{ service.status }}</a-tag>
            </div>
          </div>
        </div>

        <div class="page-card trend-card">
          <div class="section-title">近 24 小时调用趋势</div>
          <v-chart class="trend-chart" :option="trendOption" autoresize />
        </div>
      </div>

      <div class="ranking-grid">
        <div class="page-card">
          <div class="section-title">Agent 调用排行（近 7 天）</div>
          <a-empty v-if="!overview.topAgents.length" description="暂无调用数据" />
          <div v-else class="ranking-list">
            <div v-for="(item, index) in overview.topAgents" :key="item.name" class="ranking-item">
              <span class="rank-no" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <span class="rank-name">{{ item.name }}</span>
              <span class="rank-value">{{ item.valueLabel }}</span>
            </div>
          </div>
        </div>

        <div class="page-card">
          <div class="section-title">应用调用排行（近 7 天）</div>
          <a-empty v-if="!overview.topApplications.length" description="暂无调用数据" />
          <div v-else class="ranking-list">
            <div v-for="(item, index) in overview.topApplications" :key="item.name" class="ranking-item">
              <span class="rank-no" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <span class="rank-name">{{ item.name }}</span>
              <span class="rank-value">{{ item.valueLabel }}</span>
            </div>
          </div>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  ApiOutlined,
  ClockCircleOutlined,
  CloudServerOutlined,
  ReloadOutlined,
  RobotOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { fetchMonitorOverview } from '@/api/monitor'
import type { MonitorOverview } from '@/types/monitor'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent])

const loading = ref(false)
const overview = ref<MonitorOverview>({
  metrics: [],
  services: [],
  topAgents: [],
  topApplications: [],
  hourlyTrend: [],
})

const trendOption = computed(() => ({
  grid: { left: 40, right: 16, top: 24, bottom: 28 },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: overview.value.hourlyTrend.map((item) => item.time),
    axisLine: { lineStyle: { color: '#e5e7eb' } },
    axisLabel: { color: '#6b7280' },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLine: { show: false },
    splitLine: { lineStyle: { color: '#f3f4f6' } },
    axisLabel: { color: '#6b7280' },
  },
  series: [
    {
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: overview.value.hourlyTrend.map((item) => item.value),
      lineStyle: { width: 2, color: '#6366f1' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(99, 102, 241, 0.25)' },
            { offset: 1, color: 'rgba(99, 102, 241, 0.02)' },
          ],
        },
      },
    },
  ],
}))

function metricIcon(key: string) {
  const map: Record<string, typeof ApiOutlined> = {
    calls: ThunderboltOutlined,
    tokens: CloudServerOutlined,
    activeAgents: RobotOutlined,
    latency: ClockCircleOutlined,
    publishedAgents: ApiOutlined,
  }
  return map[key] || ApiOutlined
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchMonitorOverview()
    overview.value = res.data.data
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载监控数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.monitor-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
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

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.metric-card {
  padding: 16px;
}

.metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.metric-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.metric-icon {
  color: #6366f1;
  font-size: 16px;
}

.metric-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
}

.metric-hint {
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 12px;
}

.content-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.section-title {
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
}

.service-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.service-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color, #f0f0f0);
}

.service-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.service-main {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.service-dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: #ef4444;
  flex-shrink: 0;
}

.service-dot.healthy {
  background: #22c55e;
}

.service-name {
  font-weight: 500;
}

.service-detail {
  margin-top: 2px;
  color: var(--text-muted);
  font-size: 12px;
  word-break: break-all;
}

.trend-chart {
  width: 100%;
  height: 280px;
}

.ranking-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ranking-item {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color, #f0f0f0);
}

.ranking-item:last-child {
  border-bottom: none;
}

.rank-no {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #f3f4f6;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.rank-no.top {
  background: #eef2ff;
  color: #6366f1;
}

.rank-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-value {
  color: var(--text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .metrics-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .metrics-grid,
  .ranking-grid {
    grid-template-columns: 1fr;
  }
}
</style>
