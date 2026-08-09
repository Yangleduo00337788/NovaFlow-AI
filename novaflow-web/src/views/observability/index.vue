<template>
  <div class="observability-page">
    <div class="page-header">
      <div>
        <h1>可观测性</h1>
        <p>错误率、延迟趋势与基础设施健康概览</p>
      </div>
      <a-button :loading="loading" @click="loadData">刷新</a-button>
    </div>

    <a-spin :spinning="loading">
      <div class="metrics-grid">
        <div v-for="item in overview.metrics" :key="item.key" class="metric-card page-card">
          <div class="metric-label">{{ item.label }}</div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-hint">{{ item.hint }}</div>
        </div>
      </div>

      <div class="content-grid">
        <div class="page-card">
          <div class="section-title">近 24 小时失败趋势</div>
          <a-empty v-if="!overview.failedTrend.length" description="暂无失败数据" />
          <v-chart v-else class="trend-chart" :option="failedTrendOption" autoresize />
        </div>
        <div class="page-card">
          <div class="section-title">近 24 小时延迟趋势</div>
          <a-empty v-if="!overview.latencyTrend.length" description="暂无延迟数据" />
          <v-chart v-else class="trend-chart" :option="latencyTrendOption" autoresize />
        </div>
      </div>

      <div class="page-card services-card">
        <div class="section-title">基础设施健康</div>
        <div class="service-list">
          <div v-for="service in overview.services" :key="service.key" class="service-item">
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
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { fetchObservabilityOverview } from '@/api/monitor'
import type { ObservabilityOverview } from '@/types/monitor'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const loading = ref(false)
const overview = ref<ObservabilityOverview>({
  metrics: [],
  services: [],
  failedTrend: [],
  latencyTrend: [],
})

const failedTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 16, top: 24, bottom: 28 },
  xAxis: { type: 'category', data: overview.value.failedTrend.map((item) => item.time) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    type: 'line',
    smooth: true,
    data: overview.value.failedTrend.map((item) => item.value),
    areaStyle: { opacity: 0.12 },
    lineStyle: { color: '#ef4444' },
    itemStyle: { color: '#ef4444' },
  }],
}))

const latencyTrendOption = computed(() => ({
  tooltip: { trigger: 'axis', valueFormatter: (v: number) => `${v}ms` },
  grid: { left: 48, right: 16, top: 24, bottom: 28 },
  xAxis: { type: 'category', data: overview.value.latencyTrend.map((item) => item.time) },
  yAxis: { type: 'value' },
  series: [{
    type: 'line',
    smooth: true,
    data: overview.value.latencyTrend.map((item) => item.value),
    areaStyle: { opacity: 0.12 },
    lineStyle: { color: '#2563eb' },
    itemStyle: { color: '#2563eb' },
  }],
}))

async function loadData() {
  loading.value = true
  try {
    const res = await fetchObservabilityOverview()
    overview.value = res.data.data
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载可观测性数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.observability-page {
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.metric-card {
  padding: 16px;
}

.metric-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.metric-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
}

.metric-hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.section-title {
  margin-bottom: 12px;
  font-weight: 600;
}

.trend-chart {
  height: 220px;
}

.service-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.service-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}

.service-item:last-child {
  border-bottom: none;
}

.service-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.service-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
}

.service-dot.healthy {
  background: #22c55e;
}

.service-name {
  font-weight: 500;
}

.service-detail {
  font-size: 12px;
  color: var(--text-secondary);
}

@media (max-width: 1200px) {
  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
