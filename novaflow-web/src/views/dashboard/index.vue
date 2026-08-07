<template>
  <div class="dashboard">
    <!-- 左主栏：欢迎 / KPI / 快速开始 / 下半内容 -->
    <div class="dashboard-left">
      <div class="welcome-banner">
      <div class="welcome-text">
        <h1>欢迎回来，{{ displayName }} 👋</h1>
        <p>今天是 {{ today }}，NovaFlow AI 助力您高效构建智能应用</p>
      </div>
      <div class="welcome-visual" aria-hidden="true">
        <svg class="welcome-cube-svg cube-main" viewBox="0 0 80 72" width="80" height="72">
          <defs>
            <linearGradient id="cubeTop" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#91caff" />
              <stop offset="100%" stop-color="#4096ff" />
            </linearGradient>
            <linearGradient id="cubeLeft" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#1677ff" />
              <stop offset="100%" stop-color="#0958d9" />
            </linearGradient>
            <linearGradient id="cubeRight" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#69b1ff" />
              <stop offset="100%" stop-color="#1677ff" />
            </linearGradient>
          </defs>
          <g transform="translate(8,4)">
            <path d="M32 10 L58 25 L32 40 L6 25 Z" fill="url(#cubeTop)" opacity="0.95" />
            <path d="M6 25 L32 40 L32 62 L6 47 Z" fill="url(#cubeLeft)" />
            <path d="M32 40 L58 25 L58 47 L32 62 Z" fill="url(#cubeRight)" />
          </g>
        </svg>
        <svg class="welcome-cube-svg cube-sub" viewBox="0 0 56 52" width="52" height="48">
          <defs>
            <linearGradient id="cubeTop2" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#d3adf7" />
              <stop offset="100%" stop-color="#9254de" />
            </linearGradient>
            <linearGradient id="cubeLeft2" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#b37feb" />
              <stop offset="100%" stop-color="#722ed1" />
            </linearGradient>
            <linearGradient id="cubeRight2" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#d3adf7" />
              <stop offset="100%" stop-color="#9254de" />
            </linearGradient>
          </defs>
          <g transform="translate(4,2)" opacity="0.75">
            <path d="M24 8 L44 20 L24 32 L4 20 Z" fill="url(#cubeTop2)" />
            <path d="M4 20 L24 32 L24 48 L4 36 Z" fill="url(#cubeLeft2)" />
            <path d="M24 32 L44 20 L44 36 L24 48 Z" fill="url(#cubeRight2)" />
          </g>
        </svg>
        <svg class="welcome-cube-svg cube-mini" viewBox="0 0 40 36" width="36" height="32">
          <g transform="translate(4,2)" opacity="0.55">
            <path d="M16 6 L30 14 L16 22 L2 14 Z" fill="#69b1ff" />
            <path d="M2 14 L16 22 L16 32 L2 24 Z" fill="#4096ff" />
            <path d="M16 22 L30 14 L30 24 L16 32 Z" fill="#91caff" />
          </g>
        </svg>
      </div>
    </div>

    <div class="stats-grid">
      <div v-for="item in data.stats" :key="item.key" class="stat-card">
        <div class="stat-head">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-icon" :class="item.key">
            <component :is="getMenuIcon(item.key)" />
          </div>
        </div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-change" :class="{ up: isPositiveTrend(item), down: !isPositiveTrend(item) }">
          <component :is="item.up ? RiseOutlined : FallOutlined" class="trend-icon" />
          {{ item.change }} 较上周
        </div>
        <v-chart
          v-if="statSparklines[item.key]"
          class="stat-spark"
          :option="sparkOption(item.key)"
          autoresize
        />
      </div>
    </div>

    <div class="page-card quick-start">
      <div class="quick-start-title">快速开始</div>
      <div class="quick-tiles">
        <router-link
          v-for="tile in quickTiles"
          :key="tile.label"
          :to="tile.path"
          class="quick-tile"
          :class="tile.color"
        >
          <component :is="tile.leftIcon" class="tile-mini-icon" />
          <div class="tile-text">
            <span class="tile-label">{{ tile.label }}</span>
            <span class="tile-desc">{{ tile.desc }}</span>
          </div>
          <div class="tile-deco" :class="tile.color">
            <component :is="tile.icon" class="tile-deco-icon" />
          </div>
        </router-link>
      </div>
    </div>

      <div class="main-panel">
        <div class="middle-row">
          <div class="page-card recent-card">
            <div class="recent-header">
              <button
                class="recent-tab-btn"
                :class="{ active: recentTab === 'recent' }"
                @click="recentTab = 'recent'"
              >最近使用</button>
              <button
                class="recent-tab-btn"
                :class="{ active: recentTab === 'favorite' }"
                @click="recentTab = 'favorite'"
              >我的收藏</button>
            </div>
            <div class="recent-list">
              <router-link
                v-for="item in displayRecentItems"
                :key="item.name"
                :to="item.path"
                class="recent-item"
              >
                <div class="recent-icon" :class="typeClass(item.type)">
                  <component :is="recentTypeIcon(item.type)" />
                </div>
                <div class="recent-info">
                  <div class="recent-name-row">
                    <span class="recent-name">{{ item.name }}</span>
                    <span class="recent-type-tag">{{ item.type }}</span>
                  </div>
                  <div class="recent-meta">更新于 {{ item.updatedAt }}</div>
                </div>
              </router-link>
            </div>
            <a class="recent-view-more">查看更多</a>
          </div>

          <div class="page-card workflow-card">
            <div class="section-title">
              <span class="section-title-left">
                <ApartmentOutlined class="section-icon" /> 工作流运行情况
              </span>
              <a-tag color="success" class="run-tag"><SyncOutlined spin /> 运行中</a-tag>
            </div>
            <div class="workflow-canvas">
              <div class="workflow-dag" :style="{ transform: `scale(${dagScale})` }">
                <div class="dag-row">
                  <div class="dag-node start"><span class="dag-dot start" /><component :is="getMenuIcon('start')" class="dag-node-icon" /> 开始</div>
                  <div class="dag-edge" />
                  <div class="dag-node llm"><component :is="getMenuIcon('llm')" class="dag-node-icon" /> 意图识别(LLM)</div>
                  <div class="dag-edge" />
                  <div class="dag-node branch-node dag-anchor"><component :is="getMenuIcon('branch')" class="dag-node-icon" /> 条件分支</div>
                </div>

                <div class="dag-fork-area">
                  <svg class="dag-fork-svg" viewBox="0 0 520 52" preserveAspectRatio="xMidYMid meet">
                    <path
                      d="M 260 0 L 260 14 L 100 14 L 100 28 M 260 14 L 420 14 L 420 28 M 260 14 L 260 52"
                      fill="none"
                      stroke="#94a3b8"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                  <div class="dag-row branch-row">
                    <div class="dag-node branch"><component :is="getMenuIcon('search')" class="dag-node-icon" /> 知识检索(Qdrant)</div>
                    <div class="dag-node branch api"><component :is="getMenuIcon('api')" class="dag-node-icon" /> 调用工具(API)</div>
                  </div>
                </div>

                <div class="dag-row">
                  <div class="dag-node llm"><component :is="getMenuIcon('llm')" class="dag-node-icon" /> 生成回答(LLM)</div>
                  <div class="dag-edge" />
                  <div class="dag-node end"><span class="dag-dot end" /><component :is="getMenuIcon('end')" class="dag-node-icon" /> 结束</div>
                </div>
              </div>
              <div class="dag-zoom-controls">
                <button type="button" class="zoom-btn" title="放大" @click="zoomIn">+</button>
                <button type="button" class="zoom-btn" title="缩小" @click="zoomOut">−</button>
                <button type="button" class="zoom-btn fit" title="适应画布" @click="zoomFit">
                  <ExpandOutlined />
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 底排：模型使用 | 最近运行日志 -->
        <div class="bottom-row">
          <div class="page-card chart-card">
            <div class="section-title chart-section-title">
              <span>模型使用情况</span>
            </div>
            <div class="chart-legend-wrap">
              <div class="donut-wrap">
                <v-chart class="donut-chart" :option="modelChartOption" autoresize />
                <div class="donut-center">
                  <div class="donut-label-top">总消耗</div>
                  <div class="donut-total">348.6M</div>
                  <div class="donut-label-bottom">Tokens</div>
                </div>
              </div>
              <div class="model-legend">
                <div v-for="(item, idx) in data.modelUsage" :key="item.model" class="legend-item">
                  <span class="legend-left">
                    <i class="legend-dot" :style="{ background: modelColors[idx % modelColors.length] }" />
                    <span class="legend-name">{{ item.model }}</span>
                  </span>
                  <span class="legend-percent">{{ item.percent }}%</span>
                  <span class="legend-tokens">{{ item.tokens }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="page-card log-card">
            <div class="section-title log-section-title">
              <span>最近运行日志</span>
              <a class="view-more">查看更多 <RightOutlined /></a>
            </div>
            <div class="log-list">
              <div v-for="record in displayLogs" :key="record.name" class="log-row">
                <span class="log-name" :title="record.name">{{ record.name }}</span>
                <span class="log-status" :class="record.success ? 'success' : 'failed'">
                  <CheckCircleOutlined v-if="record.success" class="log-status-icon" />
                  <CloseCircleOutlined v-else class="log-status-icon" />
                  {{ record.status }}
                </span>
                <span class="log-time">{{ record.time }}</span>
                <span class="log-duration">{{ record.duration }}</span>
                <span class="log-tokens">{{ formatLogTokens(record) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <DashboardRightPanel />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, GraphicComponent, MarkPointComponent, VisualMapComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import {
  RobotOutlined,
  ApartmentOutlined,
  DatabaseOutlined,
  FileAddOutlined,
  StarOutlined,
  RiseOutlined,
  FallOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExpandOutlined,
  RightOutlined,
} from '@ant-design/icons-vue'
import { fetchDashboardOverview } from '@/api/dashboard'
import { dashboardMock, statSparklines } from '@/mocks/dashboard'
import { getMenuIcon } from '@/config/menuIcons'
import DashboardRightPanel from '@/components/dashboard/DashboardRightPanel.vue'
import type { DashboardOverview, RecentLog } from '@/types/dashboard'
import { useAuthStore } from '@/stores/auth'

use([CanvasRenderer, PieChart, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, GraphicComponent, MarkPointComponent, VisualMapComponent])

const auth = useAuthStore()
const displayName = computed(() => auth.user?.nickname || auth.user?.username || '用户')
const data = ref<DashboardOverview>(dashboardMock)
const recentTab = ref<'recent' | 'favorite'>('recent')
const dagScale = ref(0.86)
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })

function zoomIn() {
  dagScale.value = Math.min(1.35, +(dagScale.value + 0.1).toFixed(2))
}
function zoomOut() {
  dagScale.value = Math.max(0.75, +(dagScale.value - 0.1).toFixed(2))
}
function zoomFit() {
  dagScale.value = 0.86
}

const quickTiles = [
  { label: '创建 Agent', desc: '快速创建智能助手', path: '/agent', leftIcon: StarOutlined, icon: RobotOutlined, color: 'blue' },
  { label: '创建工作流', desc: '可视化编排流程', path: '/workflow', leftIcon: ApartmentOutlined, icon: ApartmentOutlined, color: 'teal' },
  { label: '创建知识库', desc: '构建企业知识中心', path: '/knowledge', leftIcon: DatabaseOutlined, icon: DatabaseOutlined, color: 'orange' },
  { label: '导入文档', desc: '支持多种格式', path: '/knowledge', leftIcon: FileAddOutlined, icon: FileAddOutlined, color: 'purple' },
]

function formatLogTokens(record: RecentLog) {
  if (!record.success || record.tokens == null) return '-'
  return `${record.tokens.toLocaleString()} Tokens`
}

const favoriteItems = computed(() => (data.value.recentItems || []).filter((_, i) => i % 2 === 0))

const displayRecentItems = computed(() => {
  const items = recentTab.value === 'recent' ? data.value.recentItems : favoriteItems.value
  return (items || []).slice(0, 5)
})

const displayLogs = computed(() => data.value.recentLogs || [])

const modelColors = ['#2563eb', '#60a5fa', '#94a3b8', '#f59e0b']

const modelChartOption = computed(() => ({
  color: modelColors,
  tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
  series: [{
    type: 'pie',
    radius: ['62%', '82%'],
    center: ['50%', '50%'],
    padAngle: 2,
    label: { show: false },
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
    data: (data.value.modelUsage || []).map((m) => ({ name: m.model, value: m.percent })),
  }],
}))

function sparkOption(key: string) {
  const points = statSparklines[key] || []
  return {
    grid: { left: 0, right: 0, top: 6, bottom: 0 },
    xAxis: { type: 'category', show: false, data: points.map((_, i) => i) },
    yAxis: { type: 'value', show: false, min: 'dataMin' },
    series: [{
      type: 'line',
      data: points,
      smooth: true,
      symbol: 'none',
      lineStyle: { color: '#1677ff', width: 1.5 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(22,119,255,0.18)' },
            { offset: 1, color: 'rgba(22,119,255,0)' },
          ],
        },
      },
    }],
  }
}

function isPositiveTrend(item: { key: string; up: boolean }) {
  if (item.key === 'cost') return !item.up
  return item.up
}

function typeClass(type: string) {
  if (type.includes('Agent')) return 'agent'
  if (type.includes('工作流')) return 'workflow'
  return 'knowledge'
}

function recentTypeIcon(type: string) {
  if (type.includes('Agent')) return getMenuIcon('robot')
  if (type.includes('工作流')) return getMenuIcon('workflow')
  return getMenuIcon('knowledge')
}

onMounted(async () => {
  try {
    const res = await fetchDashboardOverview()
    if (res.data.data) {
      data.value = mergeOverview(res.data.data)
    }
  } catch {
    data.value = dashboardMock
  }
})

/** 后端缺字段或空数组时回退 mock，避免底排图表空白 */
function mergeOverview(remote: DashboardOverview): DashboardOverview {
  const pick = <K extends keyof DashboardOverview>(key: K): DashboardOverview[K] => {
    const val = remote[key]
    if (Array.isArray(val) && val.length === 0) return dashboardMock[key]
    if (val == null) return dashboardMock[key]
    return val
  }
  return {
    stats: dashboardMock.stats,
    recentItems: pick('recentItems'),
    recentLogs: pick('recentLogs'),
    modelUsage: pick('modelUsage'),
    topApps: pick('topApps'),
    systemHealth: pick('systemHealth'),
    trend: pick('trend'),
    quickActions: pick('quickActions'),
    planInfo: pick('planInfo'),
  }
}
</script>

<style scoped>
.dashboard {
  --bottom-card-height-cut: 60px;
  display: grid;
  grid-template-columns: 1fr var(--rightbar-width);
  gap: 12px;
  height: 100%;
  max-height: 100%;
  min-width: 0;
  overflow: hidden;
}

.dashboard-left {
  display: grid;
  grid-template-rows: 112px 108px auto minmax(0, 1fr);
  gap: 12px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

/* 右侧栏：顶栏下方起，与左主栏并排 */
.dashboard :deep(.page-card) {
  padding: 10px 12px;
  min-height: 0;
}

.dashboard :deep(.section-title) {
  margin-bottom: 6px;
  font-size: 12px;
}

/* 主体布局 */
.main-panel {
  display: grid;
  grid-template-rows: minmax(0, 1fr) minmax(0, calc(1.1fr - var(--bottom-card-height-cut)));
  gap: 12px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

/* 欢迎横幅 */
.welcome-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 112px;
  min-height: 112px;
  max-height: 112px;
  padding: 0 32px;
  background:
    radial-gradient(ellipse 72% 140% at 88% 42%, rgba(167, 192, 255, 0.28) 0%, transparent 62%),
    linear-gradient(92deg, #f8faff 0%, #f4f7ff 28%, #eef2ff 58%, #e8ecff 100%);
  border-radius: 12px;
  border: 1px solid #e6ebf5;
  overflow: hidden;
  position: relative;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.welcome-text h1 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
  color: #0f172a;
}

.welcome-text p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.35;
}

.welcome-visual {
  position: relative;
  flex-shrink: 0;
  width: 140px;
  height: 90px;
}

.welcome-cube-svg {
  position: absolute;
  display: block;
  filter: drop-shadow(0 10px 20px rgba(74, 128, 255, 0.18));
}

.welcome-cube-svg.cube-main {
  right: 12px;
  top: 8px;
  animation: cube-float 3.6s ease-in-out infinite;
}

.welcome-cube-svg.cube-sub {
  right: 68px;
  top: 20px;
  animation: cube-float 4.2s ease-in-out infinite 0.4s;
}

.welcome-cube-svg.cube-mini {
  right: 44px;
  top: 0;
  animation: cube-float 3s ease-in-out infinite 0.8s;
}

@keyframes cube-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

/* 指标卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.stat-card {
  position: relative;
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px 12px;
  min-height: 0;
  height: 108px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 4px 12px rgba(15, 23, 42, 0.04);
  border: 1px solid #eef2f6;
  overflow: hidden;
}

.stat-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0;
}

.stat-spark {
  position: absolute;
  right: 0;
  bottom: -2px;
  width: 92px;
  height: 42px;
  flex-shrink: 0;
  pointer-events: none;
}

.stat-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.stat-icon.apps { background: #e8f3ff; color: #1677ff; }
.stat-icon.agents { background: #f3ebff; color: #722ed1; }
.stat-icon.invocations { background: #e6fffb; color: #13c2c2; }
.stat-icon.tokens { background: #fff7e6; color: #fa8c16; }
.stat-icon.cost { background: #fff1f0; color: #f5222d; }

.trend-icon {
  font-size: 11px;
  margin-right: 2px;
}

.stat-change {
  position: absolute;
  left: 16px;
  bottom: 12px;
  display: flex;
  align-items: center;
  font-size: 11px;
  white-space: nowrap;
  z-index: 1;
}

.stat-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.3;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  margin: 8px 0 0;
  line-height: 1.1;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.stat-change.up { color: #16a34a; }
.stat-change.down { color: #dc2626; }

.section-icon {
  margin-right: 4px;
  color: #1677ff;
  font-size: 13px;
}

.section-title-left {
  display: flex;
  align-items: center;
}

.quick-start {
  width: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 14px 16px 16px;
}

.quick-start-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.3;
}

.quick-tiles {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.quick-tile {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  padding: 14px 12px 14px 14px;
  min-height: 72px;
  border-radius: 12px;
  border: 1px solid #eef2f6;
  background: #f8fafc;
  transition: all 0.2s;
  text-align: left;
}

.quick-tile.blue {
  background: linear-gradient(145deg, #f7faff 0%, #f2f7ff 100%);
  border-color: #e8f0ff;
}

.quick-tile.teal {
  background: linear-gradient(145deg, #f4fdfb 0%, #edfbf8 100%);
  border-color: #d9f7f0;
}

.quick-tile.orange {
  background: linear-gradient(145deg, #fffaf5 0%, #fff5eb 100%);
  border-color: #ffe8cc;
}

.quick-tile.purple {
  background: linear-gradient(145deg, #faf6ff 0%, #f5efff 100%);
  border-color: #ead9ff;
}

.tile-mini-icon {
  flex-shrink: 0;
  font-size: 16px;
}

.quick-tile.blue .tile-mini-icon { color: #1677ff; }
.quick-tile.teal .tile-mini-icon { color: #13c2c2; }
.quick-tile.orange .tile-mini-icon { color: #fa8c16; }
.quick-tile.purple .tile-mini-icon { color: #722ed1; }

.tile-text {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.tile-label {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.3;
  color: #0f172a;
}

.tile-desc {
  font-size: 11px;
  font-weight: 400;
  line-height: 1.35;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tile-deco {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.tile-deco.blue {
  color: #1677ff;
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.22);
}

.tile-deco.teal {
  color: #13c2c2;
  box-shadow: 0 6px 16px rgba(19, 194, 194, 0.22);
}

.tile-deco.orange {
  color: #fa8c16;
  box-shadow: 0 6px 16px rgba(250, 140, 22, 0.22);
}

.tile-deco.purple {
  color: #722ed1;
  box-shadow: 0 6px 16px rgba(114, 46, 209, 0.22);
}

.tile-deco-icon {
  font-size: 22px;
}

.quick-tile:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
}

/* 主体布局 */
.main-panel {
  display: grid;
  grid-template-rows: minmax(0, 1fr) minmax(0, calc(1.1fr - var(--bottom-card-height-cut)));
  gap: 12px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
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

.trend-card .line-chart,
.top-apps-card .bar-chart {
  flex: 1;
  min-height: 0;
  height: 100%;
}

.quick-actions-card .section-title {
  flex-shrink: 0;
  margin-bottom: 6px;
}

.quick-actions-card .quick-grid {
  flex: 1;
  align-content: center;
  row-gap: 8px;
  min-height: 0;
}


.chart-legend-wrap {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 4px 0;
}

.donut-wrap {
  position: relative;
  flex: 0 0 148px;
  width: 148px;
  height: 148px;
}

.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  text-align: center;
}

.donut-label-top,
.donut-label-bottom {
  font-size: 10px;
  color: #94a3b8;
  line-height: 1.2;
}

.donut-total {
  margin: 2px 0;
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.15;
}

.chart-card,
.log-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
}

.chart-card .section-title,
.log-card .section-title {
  margin-bottom: 10px;
  flex-shrink: 0;
}

.chart-section-title span,
.log-section-title span {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.log-section-title .view-more {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
}

.chart-card .model-legend {
  flex: 1;
  margin-top: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
  padding-right: 4px;
}

.chart-card .donut-chart {
  width: 148px;
  height: 148px;
}

.log-card .log-list {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

/* 底部两栏：模型使用 1/3 | 运行日志 2/3 */
.bottom-row {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 12px;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
}

/* 中间两栏：最近使用窄列 + 工作流宽列 */
.middle-row {
  display: grid;
  grid-template-columns: 328px minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
  min-height: 0;
  overflow: hidden;
}

.recent-card,
.workflow-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.recent-header {
  display: flex;
  gap: 16px;
  margin-bottom: 6px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 4px;
  flex-shrink: 0;
}

.recent-tab-btn {
  border: none;
  background: transparent;
  font-size: 13px;
  color: #94a3b8;
  padding: 0;
  cursor: pointer;
  font-weight: 500;
}

.recent-tab-btn.active {
  color: #0f172a;
  font-weight: 600;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-height: 0;
  justify-content: flex-start;
  overflow: hidden;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 6px;
  border-radius: 8px;
  transition: background 0.15s;
}

.recent-item:hover {
  background: #f1f5f9;
}

.recent-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.recent-icon.agent { background: #e6f4ff; color: #1677ff; }
.recent-icon.workflow { background: #f9f0ff; color: #722ed1; }
.recent-icon.knowledge { background: #e6fffb; color: #13c2c2; }

.recent-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.recent-name {
  font-size: 12px;
  font-weight: 500;
  line-height: 1.25;
  color: #0f172a;
}

.recent-type-tag {
  flex-shrink: 0;
  font-size: 10px;
  line-height: 18px;
  padding: 0 6px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;
}

.recent-meta {
  font-size: 10px;
  color: #94a3b8;
  margin-top: 1px;
}

.recent-view-more {
  display: block;
  margin-top: 6px;
  text-align: center;
  font-size: 12px;
  color: #1677ff;
  cursor: pointer;
  flex-shrink: 0;
}

.recent-view-more:hover {
  color: #4096ff;
}

/* 工作流 DAG */
.workflow-card .section-title {
  margin-bottom: 4px;
  flex-shrink: 0;
}

.workflow-canvas {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 1px 1px, #e2e8f0 1px, transparent 0) 0 0 / 16px 16px;
  border: 1px solid #eef2f6;
  border-radius: 10px;
  padding: 6px 8px 28px;
  overflow: hidden;
}

.workflow-dag {
  display: flex;
  flex-direction: column;
  gap: 0;
  width: 100%;
  max-width: 500px;
  padding: 0;
  transform-origin: center center;
  transition: transform 0.15s ease;
}

.dag-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  flex-wrap: nowrap;
}

.dag-edge {
  width: 22px;
  height: 2px;
  background: linear-gradient(90deg, #cbd5e1, #94a3b8);
  flex-shrink: 0;
  position: relative;
}

.dag-edge::after {
  content: '';
  position: absolute;
  right: -1px;
  top: 50%;
  width: 0;
  height: 0;
  border: 4px solid transparent;
  border-left-color: #94a3b8;
  transform: translateY(-50%);
}

.dag-fork-area {
  position: relative;
  margin: 0 auto;
  width: 100%;
  max-width: 480px;
}

.dag-fork-svg {
  display: block;
  width: 100%;
  height: 30px;
}

.branch-row {
  justify-content: space-between;
  padding: 0 12px;
  margin-top: -6px;
}

.dag-anchor {
  position: relative;
}

.dag-node {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 4px 8px;
  font-size: 10px;
  color: #334155;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
}

.dag-node-icon {
  font-size: 10px;
  color: #64748b;
}

.dag-node.start,
.dag-node.end {
  background: #f8fafc;
}

.dag-node.llm {
  border-color: #91caff;
  background: #f0f7ff;
  color: #1677ff;
}

.dag-node.llm .dag-node-icon {
  color: #1677ff;
}

.dag-node.branch-node {
  border-color: #d3adf7;
  background: #f9f0ff;
  color: #722ed1;
}

.dag-node.branch-node .dag-node-icon {
  color: #722ed1;
}

.dag-node.branch {
  background: #e6f4ff;
  border-color: #91caff;
  color: #1677ff;
}

.dag-node.branch .dag-node-icon {
  color: #1677ff;
}

.dag-node.branch.api {
  background: #fff7e6;
  border-color: #ffd591;
  color: #d46b08;
}

.dag-node.branch.api .dag-node-icon {
  color: #fa8c16;
}

.dag-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dag-dot.start { background: #52c41a; }
.dag-dot.end { background: #94a3b8; }

.dag-zoom-controls {
  position: absolute;
  left: 10px;
  bottom: 10px;
  display: flex;
  gap: 4px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 3px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
}

.zoom-btn {
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #475569;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.zoom-btn:hover {
  background: #f1f5f9;
  color: #1677ff;
}

.zoom-btn.fit {
  font-size: 12px;
}

.health-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
  font-size: 12px;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
}

.health-item:last-child {
  border-bottom: none;
}

.health-icon-wrap {
  width: 24px;
  height: 24px;
  border-radius: 5px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
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
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.run-tag {
  font-size: 10px;
  line-height: 18px;
  padding: 0 6px;
  margin: 0;
}

/* 右侧图表 */
.trend-card .section-title {
  margin-bottom: 4px;
  flex-shrink: 0;
}

.top-apps-card .section-title {
  margin-bottom: 4px;
  flex-shrink: 0;
}

.line-chart {
  margin-bottom: 0;
}

.bar-chart {
  margin-bottom: 0;
}

.donut-chart {
  width: 148px;
  height: 148px;
}

.model-legend {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 40px 56px;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  line-height: 1.4;
}

.legend-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-name {
  color: #334155;
  font-weight: 500;
}

.legend-percent,
.legend-tokens {
  text-align: right;
  color: #94a3b8;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

/* 运行日志列表 */
.log-list {
  flex: 1;
  min-height: 0;
}

.log-row {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) 68px 84px 44px minmax(88px, 1fr);
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px solid #f8fafc;
  font-size: 12px;
}

.log-row:last-child {
  border-bottom: none;
}

.log-name {
  color: #0f172a;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.log-time,
.log-duration,
.log-tokens {
  color: #94a3b8;
  white-space: nowrap;
}

.log-duration,
.log-tokens {
  text-align: right;
}

.log-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.log-status.success {
  color: #16a34a;
}

.log-status.failed {
  color: #dc2626;
}

.log-status-icon {
  font-size: 13px;
}

.quick-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 4px 2px;
  border-radius: 8px;
  font-size: 10px;
  color: #475569;
  transition: background 0.15s;
}

.quick-action:hover {
  background: #f8fafc;
}

/* 快捷操作 */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.action-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #fff;
}

.action-icon.api-key { background: #1677ff; }
.action-icon.prompt { background: #9254de; }
.action-icon.dataset { background: #13c2c2; }
.action-icon.mcp { background: #fa8c16; }
.action-icon.settings { background: #597ef7; }
.action-icon.users { background: #52c41a; }

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
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
</style>
