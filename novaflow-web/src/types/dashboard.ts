export interface DashboardOverview {
  stats: StatCard[]
  recentItems: RecentItem[]
  recentLogs: RecentLog[]
  modelUsage: ModelUsage[]
  topApps: TopApp[]
  systemHealth: SystemHealth[]
  trend: TrendPoint[]
  quickActions: QuickAction[]
  planInfo: PlanInfo
}

export interface StatCard {
  key: string
  label: string
  value: string
  change: string
  up: boolean
}

export interface RecentItem {
  name: string
  type: string
  updatedAt: string
  path: string
}

export interface RecentLog {
  name: string
  status: string
  success: boolean
  time: string
  duration: string
  tokens: number | null
}

export interface ModelUsage {
  model: string
  percent: number
  tokens: string
}

export interface TopApp {
  name: string
  value: string
  icon?: string
  color?: string
  iconBg?: string
}

export interface SystemHealth {
  name: string
  status: string
  healthy: boolean
}

export interface TrendPoint {
  time: string
  value: number
}

export interface QuickAction {
  key: string
  label: string
  path: string
}

export interface PlanInfo {
  planType: string
  expireAt: string
  usedPercent: number
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}
