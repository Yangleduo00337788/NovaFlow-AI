<template>
  <div class="runtime-page page-shell" data-testid="monitor-page">
    <div class="page-header">
      <div>
        <h1>运行</h1>
        <p>本企业调用健康、日志与链路。排障先看概览，再下钻日志或 Trace。</p>
      </div>
    </div>

    <a-tabs v-model:activeKey="tab" class="runtime-tabs" @change="onTabChange">
      <a-tab-pane v-if="canOverview" key="overview" tab="概览" force-render>
        <MonitorOverview embedded />
        <ObservabilityOverview embedded />
      </a-tab-pane>
      <a-tab-pane v-if="canLogs" key="logs" tab="调用日志" force-render>
        <LogPage embedded />
      </a-tab-pane>
      <a-tab-pane v-if="canTraces" key="traces" tab="链路" force-render>
        <TracePage embedded />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import MonitorOverview from '@/views/monitor/index.vue'
import ObservabilityOverview from '@/views/observability/index.vue'
import LogPage from '@/views/log/index.vue'
import TracePage from '@/views/trace/index.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const canOverview = computed(() => auth.hasPermission('monitor:view'))
const canLogs = computed(() => auth.hasPermission('log:read'))
const canTraces = computed(() => auth.hasPermission('trace:view'))

function tabFromQuery(): string {
  const raw = String(route.query.tab || '')
  if (raw === 'logs' || raw === 'log' || route.query.logId) return 'logs'
  if (raw === 'traces' || raw === 'trace' || route.query.traceId) return 'traces'
  if (raw === 'overview') return 'overview'
  return canOverview.value ? 'overview' : canLogs.value ? 'logs' : 'traces'
}

const tab = ref(tabFromQuery())

watch(
  () => [route.query.tab, route.query.logId, route.query.traceId],
  () => {
    tab.value = tabFromQuery()
  },
)

function onTabChange(key: string | number) {
  const next = String(key)
  const query: Record<string, string> = { tab: next }
  if (next === 'logs' && route.query.logId) {
    query.logId = String(route.query.logId)
  }
  if (next === 'traces' && route.query.traceId) {
    query.traceId = String(route.query.traceId)
  }
  router.replace({ path: '/monitor', query })
}
</script>

<style scoped>
.runtime-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}

.runtime-tabs :deep(.ant-tabs-content-holder) {
  min-height: 320px;
}
</style>
