<template>
  <div class="right-panel">
    <div class="page-card health-card">
      <div class="section-title">
        <span class="section-title-left"><CloudServerOutlined class="section-icon" /> 系统状态</span>
        <a class="view-more">全部正常 <RightOutlined /></a>
      </div>
      <div v-for="item in panelData.systemHealth" :key="item.name" class="health-item">
        <div class="health-icon-wrap">
          <component :is="getMenuIcon(healthServiceIcon(item.name))" />
        </div>
        <span class="health-name">{{ item.name }}</span>
        <span class="status-pill" :class="item.healthy ? 'ok' : 'error'">
          <span v-if="item.healthy" class="status-dot" />
          {{ item.status }}
        </span>
      </div>
    </div>

    <div class="page-card trend-card">
      <div class="section-title">
        <span class="section-title-left"><LineChartOutlined class="section-icon" /> 今日调用趋势</span>
        <router-link to="/log" class="view-more">查看更多</router-link>
      </div>
      <a-empty
        v-if="!hasTrendData"
        class="chart-empty"
        description="暂无调用数据"
      >
        <template #description>
          <span>暂无调用数据</span>
          <p class="chart-empty-hint">在 Agent 调试中完成对话后，将在此展示今日调用趋势</p>
        </template>
      </a-empty>
      <v-chart v-else class="line-chart" :option="trendChartOption" autoresize />
    </div>

    <div class="page-card top-apps-card">
      <div class="section-title">
        <span class="section-title-left"><BarChartOutlined class="section-icon" /> Top 5 应用（调用次数）</span>
        <router-link to="/log" class="view-more">查看更多</router-link>
      </div>
      <a-empty
        v-if="!panelData.topApps.length"
        class="chart-empty"
        description="暂无应用调用数据"
      >
        <template #description>
          <span>暂无应用调用数据</span>
          <p class="chart-empty-hint">Agent 产生真实调用后，将在此展示 Top 5 排行</p>
        </template>
      </a-empty>
      <div v-else class="top-apps-list">
        <div v-for="(item, index) in topAppsWithPercent" :key="item.name" class="top-app-row">
          <div
            class="top-app-icon"
            :style="{ background: item.iconBg || topAppThemes[index % topAppThemes.length].iconBg }"
          >
            <component
              :is="getMenuIcon(item.icon || topAppThemes[index % topAppThemes.length].icon)"
              :style="{ color: item.color || topAppThemes[index % topAppThemes.length].color }"
            />
          </div>
          <span class="top-app-name">{{ item.name }}</span>
          <div class="top-app-bar">
            <div
              class="top-app-bar-fill"
              :style="{
                width: `${item.percent}%`,
                background: item.color || topAppThemes[index % topAppThemes.length].color,
              }"
            />
          </div>
          <span class="top-app-value">{{ item.value }}</span>
        </div>
      </div>
    </div>

    <div class="page-card quick-actions-card">
      <div class="section-title">
        <span class="section-title-left"><AppstoreOutlined class="section-icon" /> 快捷操作</span>
      </div>
      <div class="quick-grid">
        <router-link
          v-for="item in panelData.quickActions"
          :key="item.key"
          :to="item.path"
          class="quick-action"
        >
          <div class="action-icon-wrap" :class="item.key">
            <component :is="getMenuIcon(item.key)" class="action-icon-svg" />
          </div>
          <span class="action-label">{{ item.label }}</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, VisualMapComponent, MarkPointComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import {
  CloudServerOutlined,
  LineChartOutlined,
  BarChartOutlined,
  AppstoreOutlined,
  RightOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { getMenuIcon } from '@/config/menuIcons'
import { useDashboardOverview } from '@/composables/useDashboardOverview'
import { useThemeStore } from '@/stores/theme'
import { getChartTheme } from '@/utils/chartTheme'
import { storeToRefs } from 'pinia'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, VisualMapComponent, MarkPointComponent])

const themeStore = useThemeStore()
const { mode } = storeToRefs(themeStore)

const topAppThemes = [
  { icon: 'robot', color: '#1677ff', iconBg: '#e8f3ff' },
  { icon: 'application', color: '#4f6ef7', iconBg: '#eef2ff' },
  { icon: 'agents', color: '#fa8c16', iconBg: '#fff7e6' },
  { icon: 'knowledge', color: '#9254de', iconBg: '#f9f0ff' },
  { icon: 'workflow', color: '#13c2c2', iconBg: '#e6fffb' },
]

const { overview, loadOverview } = useDashboardOverview()

const panelData = computed(() => ({
  systemHealth: overview.value.systemHealth || [],
  trend: overview.value.trend || [],
  topApps: overview.value.topApps || [],
  quickActions: overview.value.quickActions || [],
}))

function parseAppValue(val: string) {
  if (val.endsWith('K')) return parseFloat(val) * 1000
  if (val.endsWith('M')) return parseFloat(val) * 1_000_000
  return parseFloat(val)
}

const topAppsWithPercent = computed(() => {
  const apps = panelData.value.topApps || []
  const max = Math.max(...apps.map((a) => parseAppValue(a.value)), 1)
  return apps.map((app) => ({
    ...app,
    percent: Math.max((parseAppValue(app.value) / max) * 100, 8),
  }))
})

const hasTrendData = computed(() => {
  const points = panelData.value.trend || []
  return points.some((point) => point.value > 0)
})

function healthServiceIcon(name: string) {
  if (name.includes('API')) return 'api'
  if (name.includes('向量')) return 'vector'
  if (name.includes('消息')) return 'queue'
  if (name.includes('存储')) return 'storage'
  return 'storage'
}

const trendChartOption = computed(() => {
  const points = panelData.value.trend || []
  const maxVal = Math.max(...points.map((p) => p.value), 1)
  const chartTheme = getChartTheme(mode.value)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 32, right: 8, top: 8, bottom: 16 },
    xAxis: {
      type: 'category',
      data: points.map((p) => p.time),
      axisLine: { lineStyle: { color: chartTheme.axisLine } },
      axisLabel: { fontSize: 10, color: chartTheme.axisLabel },
    },
    yAxis: {
      type: 'value',
      min: 0,
      splitLine: { lineStyle: { color: chartTheme.splitLine } },
      axisLabel: { fontSize: 10, color: chartTheme.axisLabel },
    },
    series: [{
      type: 'line',
      smooth: true,
      data: points.map((p) => p.value),
      areaStyle: { color: 'rgba(22, 119, 255, 0.12)' },
      lineStyle: { color: '#1677ff', width: 1.5 },
      itemStyle: { color: '#1677ff' },
      symbol: 'none',
      markPoint: {
        symbolSize: 28,
        data: [{ type: 'max', name: '峰值' }],
        label: { fontSize: 9, formatter: (p: { data: { value: number } }) => p.data.value.toLocaleString() },
      },
    }],
    visualMap: { show: false, min: 0, max: maxVal },
  }
})

onMounted(async () => {
  try {
    await loadOverview()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载工作台数据失败')
  }
})
</script>

<style scoped>
.right-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1.15fr) minmax(0, 1.05fr) minmax(0, 1fr);
  gap: 12px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.right-panel :deep(.page-card) {
  padding: 10px 12px;
  min-height: 0;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
}

.section-title-left {
  display: flex;
  align-items: center;
}

.section-icon {
  margin-right: 4px;
  color: #1677ff;
  font-size: 13px;
}

.view-more {
  font-size: 12px;
  font-weight: 400;
  color: #1677ff;
  cursor: pointer;
}

.view-more:hover {
  color: #4096ff;
}

.health-card {
  flex-shrink: 0;
}

.trend-card,
.top-apps-card,
.quick-actions-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.trend-card .line-chart {
  flex: 1;
  min-height: 0;
  height: 100%;
}

.top-apps-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 2px;
  padding-top: 2px;
}

.top-app-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) minmax(72px, 1.15fr) auto;
  align-items: center;
  gap: 8px;
  min-height: 0;
}

.top-app-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
}

.top-app-name {
  font-size: 11px;
  color: var(--text-body);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.top-app-bar {
  height: 8px;
  border-radius: 999px;
  background: var(--bg-muted);
  overflow: hidden;
}

.top-app-bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}

.top-app-value {
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.trend-card .section-title,
.top-apps-card .section-title {
  margin-bottom: 4px;
  flex-shrink: 0;
}

.quick-actions-card .section-title {
  flex-shrink: 0;
  margin-bottom: 6px;
}

.quick-actions-card .quick-grid {
  flex: 1;
  align-content: start;
  min-height: 0;
  padding-top: 2px;
}

.health-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
  font-size: 12px;
  color: var(--text-body);
  border-bottom: 1px solid var(--border);
}

.health-item:last-child {
  border-bottom: none;
}

.health-icon-wrap {
  width: 24px;
  height: 24px;
  border-radius: 5px;
  background: var(--bg-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  font-size: 12px;
  flex-shrink: 0;
}

.health-name {
  flex: 1;
}

.status-pill {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 10px;
  line-height: 16px;
  padding: 0 6px;
  border-radius: 4px;
}

.status-pill.ok {
  color: #16a34a;
  background: transparent;
  border: none;
  padding: 0;
  gap: 4px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  flex-shrink: 0;
}

.status-pill.error {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.14);
  border: 1px solid rgba(248, 113, 113, 0.35);
}

.line-chart {
  margin-bottom: 0;
}

.chart-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
}

.chart-empty-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 400;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.quick-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 72px;
  padding: 10px 6px;
  border-radius: 10px;
  background: var(--bg-subtle);
  border: 1px solid var(--card-border);
  text-align: center;
  transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;
}

.quick-action:hover {
  background: var(--card-bg);
  border-color: var(--border-strong);
  box-shadow: var(--card-shadow);
}

.action-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-icon-svg {
  font-size: 18px;
}

.action-label {
  font-size: 11px;
  line-height: 1.35;
  color: var(--text-secondary);
  word-break: keep-all;
}

.action-icon-wrap.api-key {
  background: rgba(250, 173, 20, 0.16);
  color: #fa8c16;
}

.action-icon-wrap.prompt {
  background: rgba(235, 47, 150, 0.12);
  color: #eb2f96;
}

.action-icon-wrap.dataset {
  background: rgba(19, 194, 194, 0.14);
  color: #13c2c2;
}

.action-icon-wrap.mcp {
  background: rgba(54, 207, 201, 0.14);
  color: #36cfc9;
}

.action-icon-wrap.settings {
  background: rgba(71, 85, 105, 0.1);
  color: #475569;
}

.action-icon-wrap.users {
  background: rgba(250, 140, 22, 0.14);
  color: #fa8c16;
}

.action-icon-wrap.log {
  background: rgba(22, 119, 255, 0.12);
  color: #1677ff;
}
</style>
