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
        <img :src="welcomeIllustration" alt="" class="welcome-illustration" />
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
          {{ item.change }} {{ trendLabel(item.key) }}
        </div>
        <v-chart
          v-if="(data.sparklines?.[item.key] || []).length"
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
          :key="tile.key"
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
              <a-empty v-if="!displayRecentItems.length" :description="recentTab === 'recent' ? '暂无最近访问' : '暂无收藏'">
                <template #description>
                  <span>{{ recentTab === 'recent' ? '暂无最近访问' : '暂无收藏' }}</span>
                  <p class="dashboard-empty-hint">
                    <template v-if="recentTab === 'recent'">
                      打开 Agent 编辑/调试或知识库详情后，将在此展示最近访问
                    </template>
                    <template v-else>
                      在最近使用列表中点击星标，即可加入收藏
                    </template>
                  </p>
                </template>
              </a-empty>
              <router-link
                v-for="item in displayRecentItems"
                :key="`${item.resourceType || item.type}-${item.resourceId || item.name}`"
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
                <button
                  v-if="item.resourceType && item.resourceId"
                  type="button"
                  class="favorite-btn"
                  :class="{ active: item.favorite }"
                  :title="item.favorite ? '取消收藏' : '加入收藏'"
                  @click.prevent.stop="toggleFavorite(item)"
                >
                  <StarFilled v-if="item.favorite" />
                  <StarOutlined v-else />
                </button>
              </router-link>
            </div>
            <button type="button" class="recent-view-more" @click="openRecentDrawer">查看更多</button>
          </div>

          <div class="page-card workflow-card">
            <div class="section-title workflow-section-title">
              <span class="section-title-left">
                <ApartmentOutlined class="section-icon" /> 工作流运行情况
              </span>
              <router-link to="/workflow" class="view-more">全部工作流 <RightOutlined /></router-link>
            </div>
            <a-empty v-if="!data.workflowRuntime" class="workflow-empty" description="暂无已发布工作流">
              <template #description>
                <span>暂无已发布工作流</span>
                <p class="dashboard-empty-hint">在 <router-link to="/workflow">工作流 Studio</router-link> 创建并发布后将在此展示画布预览</p>
              </template>
            </a-empty>
            <div v-else class="workflow-runtime">
              <DashboardWorkflowCanvas :runtime="data.workflowRuntime!" />
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
              <a-empty v-if="!data.modelUsage?.length" description="暂无模型用量数据">
                <template #description>
                  <span>暂无模型用量数据</span>
                  <p class="dashboard-empty-hint">完成 Agent 真实对话调用后，将统计各模型 Token 消耗</p>
                </template>
              </a-empty>
              <template v-else>
              <div class="donut-wrap">
                <v-chart class="donut-chart" :option="modelChartOption" autoresize />
                <div class="donut-center">
                  <div class="donut-label-top">总消耗</div>
                  <div class="donut-total">{{ data.totalModelTokens || '0' }}</div>
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
              </template>
            </div>
          </div>

          <div class="page-card log-card">
            <div class="section-title log-section-title">
              <span>最近运行日志</span>
              <router-link to="/log" class="view-more">查看更多 <RightOutlined /></router-link>
            </div>
            <div class="log-list">
              <a-empty v-if="!displayLogs.length" description="暂无调用记录">
                <template #description>
                  <span>暂无调用记录</span>
                  <p class="dashboard-empty-hint">可在 <router-link to="/log">调用日志</router-link> 查看完整记录</p>
                </template>
              </a-empty>
              <div v-for="record in displayLogs" :key="record.name + record.time" class="log-row">
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

    <a-drawer
      v-model:open="recentDrawerOpen"
      :title="recentTab === 'recent' ? '最近使用' : '我的收藏'"
      placement="right"
      :width="420"
      destroy-on-close
    >
      <a-spin :spinning="drawerLoading">
        <a-empty v-if="!drawerItems.length" :description="recentTab === 'recent' ? '暂无最近访问' : '暂无收藏'" />
        <div v-else class="drawer-recent-list">
          <router-link
            v-for="item in drawerItems"
            :key="`${item.resourceType || item.type}-${item.resourceId || item.name}`"
            :to="item.path"
            class="recent-item drawer-recent-item"
            @click="recentDrawerOpen = false"
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
            <button
              v-if="item.resourceType && item.resourceId"
              type="button"
              class="favorite-btn"
              :class="{ active: item.favorite }"
              :title="item.favorite ? '取消收藏' : '加入收藏'"
              @click.prevent.stop="toggleFavorite(item, true)"
            >
              <StarFilled v-if="item.favorite" />
              <StarOutlined v-else />
            </button>
          </router-link>
        </div>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
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
  CheckCircleOutlined,
  CloseCircleOutlined,
  RightOutlined,
  StarFilled,
} from '@ant-design/icons-vue'
import { fetchDashboardFavorites, fetchDashboardRecentItems, toggleDashboardFavorite } from '@/api/dashboard'
import { message } from 'ant-design-vue'
import { getMenuIcon } from '@/config/menuIcons'
import DashboardRightPanel from '@/components/dashboard/DashboardRightPanel.vue'
import DashboardWorkflowCanvas from '@/components/dashboard/DashboardWorkflowCanvas.vue'
import type { RecentItem, RecentLog } from '@/types/dashboard'
import { useDashboardOverview } from '@/composables/useDashboardOverview'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { getChartTheme } from '@/utils/chartTheme'
import { storeToRefs } from 'pinia'
import welcomeIllustration from '@/assets/dashboard/welcome-illustration.png'

use([CanvasRenderer, PieChart, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, GraphicComponent, MarkPointComponent, VisualMapComponent])

const auth = useAuthStore()
const themeStore = useThemeStore()
const { mode } = storeToRefs(themeStore)
const displayName = computed(() => auth.user?.nickname || auth.user?.username || '用户')
const { overview: data, loadOverview } = useDashboardOverview()
const recentTab = ref<'recent' | 'favorite'>('recent')
const recentDrawerOpen = ref(false)
const drawerItems = ref<RecentItem[]>([])
const drawerLoading = ref(false)
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })

const quickStartIconMap: Record<string, { leftIcon: Component; icon: Component }> = {
  agent: { leftIcon: StarOutlined, icon: RobotOutlined },
  workflow: { leftIcon: ApartmentOutlined, icon: ApartmentOutlined },
  knowledge: { leftIcon: DatabaseOutlined, icon: DatabaseOutlined },
  import: { leftIcon: FileAddOutlined, icon: FileAddOutlined },
}

const quickTiles = computed(() =>
  (data.value.quickStartTiles || []).map((tile) => {
    const icons = quickStartIconMap[tile.key] || { leftIcon: StarOutlined, icon: RobotOutlined }
    return { ...tile, leftIcon: icons.leftIcon, icon: icons.icon }
  }),
)

function formatLogTokens(record: RecentLog) {
  if (!record.success || record.tokens == null) return '-'
  return `${record.tokens.toLocaleString()} Tokens`
}

const displayRecentItems = computed(() => {
  const items = recentTab.value === 'recent' ? data.value.recentItems : (data.value.favoriteItems || [])
  return (items || []).slice(0, 5)
})

const displayLogs = computed(() => data.value.recentLogs || [])

const modelColors = ['#2563eb', '#60a5fa', '#94a3b8', '#f59e0b']

const modelChartOption = computed(() => {
  const chartTheme = getChartTheme(mode.value)
  return {
  color: modelColors,
  tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
  series: [{
    type: 'pie',
    radius: ['62%', '82%'],
    center: ['50%', '50%'],
    padAngle: 2,
    label: { show: false },
    itemStyle: { borderRadius: 4, borderColor: chartTheme.pieBorder, borderWidth: 2 },
    data: (data.value.modelUsage || []).map((m) => ({ name: m.model, value: m.percent })),
  }],
}
})

function sparkOption(key: string) {
  const points = data.value.sparklines?.[key] || []
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

function trendLabel(key: string) {
  if (key === 'invocations' || key === 'tokens' || key === 'cost') {
    return '较上月'
  }
  if (key === 'apps' || key === 'agents' || key === 'knowledge') {
    return '较上周'
  }
  return ''
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

async function toggleFavorite(item: RecentItem, reloadDrawer = false) {
  if (!item.resourceType || !item.resourceId) return
  try {
    await toggleDashboardFavorite({
      resourceType: item.resourceType,
      resourceId: item.resourceId,
      resourceName: item.name,
    })
    await loadOverview(true)
    if (reloadDrawer && recentDrawerOpen.value) {
      await loadDrawerItems()
    }
  } catch (e) {
    message.error(e instanceof Error ? e.message : '收藏操作失败')
  }
}

async function loadDrawerItems() {
  drawerLoading.value = true
  try {
    const res = recentTab.value === 'recent'
      ? await fetchDashboardRecentItems(20)
      : await fetchDashboardFavorites(20)
    drawerItems.value = res.data.data || []
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载列表失败')
  } finally {
    drawerLoading.value = false
  }
}

async function openRecentDrawer() {
  recentDrawerOpen.value = true
  await loadDrawerItems()
}

onMounted(async () => {
  try {
    await loadOverview()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载工作台数据失败')
  }
})
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
  padding: 0 24px 0 32px;
  background: var(--welcome-banner-bg);
  border-radius: 12px;
  border: 1px solid var(--welcome-banner-border);
  overflow: hidden;
  position: relative;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.welcome-text {
  position: relative;
  z-index: 1;
}

.welcome-text h1 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--text-primary);
}

.welcome-text p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.35;
}

.welcome-visual {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 196px;
  height: 100%;
  margin-right: -12px;
}

.welcome-illustration {
  height: 108px;
  width: auto;
  max-width: 196px;
  object-fit: contain;
  object-position: right center;
  animation: welcome-illustration-float 3.6s ease-in-out infinite;
  -webkit-mask-image: linear-gradient(90deg, transparent 0%, rgba(0, 0, 0, 0.35) 12%, #000 28%);
  mask-image: linear-gradient(90deg, transparent 0%, rgba(0, 0, 0, 0.35) 12%, #000 28%);
}

@keyframes welcome-illustration-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

:global([data-theme='dark']) .welcome-illustration {
  -webkit-mask-image: none;
  mask-image: none;
  opacity: 0.88;
}

/* 指标卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  position: relative;
  background: var(--card-bg);
  border-radius: 12px;
  padding: 14px 16px 12px;
  min-height: 0;
  height: 108px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
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

.stat-icon.apps { background: var(--stat-apps-bg); color: #1677ff; }
.stat-icon.agents { background: var(--stat-agents-bg); color: #722ed1; }
.stat-icon.invocations { background: var(--stat-invocations-bg); color: #13c2c2; }
.stat-icon.tokens { background: var(--stat-tokens-bg); color: #fa8c16; }
.stat-icon.cost { background: var(--stat-cost-bg); color: #f5222d; }
.stat-icon.knowledge { background: rgba(19, 194, 194, 0.12); color: #13c2c2; }

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
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 400;
  line-height: 1.3;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  margin: 8px 0 0;
  line-height: 1.1;
  color: var(--text-primary);
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
  color: var(--text-primary);
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
  border: 1px solid var(--card-border);
  background: var(--bg-subtle);
  transition: all 0.2s;
  text-align: left;
}

.quick-tile.blue {
  background: var(--quick-blue-bg);
  border-color: var(--quick-blue-border);
}

.quick-tile.teal {
  background: var(--quick-teal-bg);
  border-color: var(--quick-teal-border);
}

.quick-tile.orange {
  background: var(--quick-orange-bg);
  border-color: var(--quick-orange-border);
}

.quick-tile.purple {
  background: var(--quick-purple-bg);
  border-color: var(--quick-purple-border);
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
  color: var(--text-primary);
}

.tile-desc {
  font-size: 11px;
  font-weight: 400;
  line-height: 1.35;
  color: var(--text-muted);
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
  background: var(--card-bg);
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
  color: var(--text-muted);
  line-height: 1.2;
}

.donut-total {
  margin: 2px 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
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
  color: var(--text-primary);
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
  border-bottom: 1px solid var(--border);
  padding-bottom: 4px;
  flex-shrink: 0;
}

.recent-tab-btn {
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--text-muted);
  padding: 0;
  cursor: pointer;
  font-weight: 500;
}

.recent-tab-btn.active {
  color: var(--text-primary);
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

.dashboard-empty-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 400;
  line-height: 1.5;
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
  background: var(--bg-muted);
}

.favorite-btn {
  margin-left: auto;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  flex-shrink: 0;
}

.favorite-btn:hover,
.favorite-btn.active {
  color: #faad14;
}

.workflow-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.workflow-runtime {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  flex: 1 1 0;
  padding-bottom: 6px;
}

.workflow-runtime-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  flex-shrink: 0;
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

.recent-icon.agent { background: var(--recent-agent-bg); color: #1677ff; }
.recent-icon.workflow { background: var(--recent-workflow-bg); color: #722ed1; }
.recent-icon.knowledge { background: var(--recent-knowledge-bg); color: #13c2c2; }

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
  color: var(--text-primary);
}

.recent-type-tag {
  flex-shrink: 0;
  font-size: 10px;
  line-height: 18px;
  padding: 0 6px;
  border-radius: 4px;
  background: var(--bg-muted);
  color: var(--text-secondary);
}

.recent-meta {
  font-size: 10px;
  color: var(--text-muted);
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
  border: none;
  background: transparent;
  width: 100%;
  padding: 0;
}

.recent-view-more:hover {
  color: #4096ff;
}

.drawer-recent-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.drawer-recent-item {
  text-decoration: none;
  color: inherit;
}

/* 工作流 DAG */
.workflow-card .section-title {
  margin-bottom: 4px;
  flex-shrink: 0;
}

.workflow-section-title .view-more {
  font-size: 12px;
}

.workflow-canvas {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--workflow-grid) 0 0 / 16px 16px;
  border: 1px solid var(--card-border);
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
  background: var(--dag-edge);
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
  border-left-color: var(--dag-edge-arrow);
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
  color: var(--dag-edge-arrow);
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
  background: var(--card-bg);
  border: 1px solid var(--dag-node-border);
  border-radius: 8px;
  padding: 4px 8px;
  font-size: 10px;
  color: var(--text-body);
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  box-shadow: var(--card-shadow);
}

.dag-node-icon {
  font-size: 10px;
  color: var(--text-secondary);
}

.dag-node.start,
.dag-node.end {
  background: var(--bg-subtle);
}

.dag-node.llm {
  border-color: var(--dag-llm-border);
  background: var(--dag-llm-bg);
  color: var(--dag-llm-color);
}

.dag-node.llm .dag-node-icon {
  color: var(--dag-llm-color);
}

.dag-node.branch-node {
  border-color: var(--dag-branch-border);
  background: var(--dag-branch-bg);
  color: var(--dag-branch-color);
}

.dag-node.branch-node .dag-node-icon {
  color: var(--dag-branch-color);
}

.dag-node.branch {
  background: var(--dag-branch-alt-bg);
  border-color: var(--dag-branch-alt-border);
  color: var(--dag-branch-alt-color);
}

.dag-node.branch .dag-node-icon {
  color: var(--dag-branch-alt-color);
}

.dag-node.branch.api {
  background: var(--dag-api-bg);
  border-color: var(--dag-api-border);
  color: var(--dag-api-color);
}

.dag-node.branch.api .dag-node-icon {
  color: var(--dag-api-color);
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
  background: var(--card-bg);
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  padding: 3px;
  box-shadow: var(--card-shadow);
}

.zoom-btn {
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.zoom-btn:hover {
  background: var(--bg-muted);
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
  color: var(--text-body);
  font-weight: 500;
}

.legend-percent,
.legend-tokens {
  text-align: right;
  color: var(--text-muted);
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
  border-bottom: 1px solid var(--border);
  font-size: 12px;
}

.log-row:last-child {
  border-bottom: none;
}

.log-name {
  color: var(--text-primary);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.log-time,
.log-duration,
.log-tokens {
  color: var(--text-muted);
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
  color: var(--text-secondary);
  transition: background 0.15s;
}

.quick-action:hover {
  background: var(--bg-subtle);
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
