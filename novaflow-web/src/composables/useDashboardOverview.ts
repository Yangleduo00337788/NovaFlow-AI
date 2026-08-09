import { ref } from 'vue'
import { fetchDashboardOverview } from '@/api/dashboard'
import type { DashboardOverview } from '@/types/dashboard'

const emptyOverview = (): DashboardOverview => ({
  stats: [],
  recentItems: [],
  favoriteItems: [],
  recentLogs: [],
  modelUsage: [],
  topApps: [],
  systemHealth: [],
  trend: [],
  quickActions: [],
  quickStartTiles: [],
  planInfo: { planType: '—', expireAt: '—', usedPercent: 0 },
  totalModelTokens: '0',
  sparklines: {},
})

const overview = ref<DashboardOverview>(emptyOverview())
const loading = ref(false)
const loaded = ref(false)
const error = ref<string | null>(null)
let inflight: Promise<void> | null = null

export function useDashboardOverview() {
  async function loadOverview(force = false) {
    if (!force && loaded.value) {
      return overview.value
    }
    if (inflight && !force) {
      await inflight
      return overview.value
    }
    loading.value = true
    error.value = null
    inflight = (async () => {
      try {
        const res = await fetchDashboardOverview()
        if (res.data.data) {
          overview.value = res.data.data
          loaded.value = true
        }
      } catch (e) {
        error.value = e instanceof Error ? e.message : '加载工作台数据失败'
        throw e
      } finally {
        loading.value = false
        inflight = null
      }
    })()
    await inflight
    return overview.value
  }

  return {
    overview,
    loading,
    error,
    loadOverview,
  }
}
