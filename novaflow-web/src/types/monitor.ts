export interface ObservabilityOverview {
  metrics: MonitorMetricCard[]
  services: MonitorServiceHealth[]
  failedTrend: MonitorTrendPoint[]
  latencyTrend: MonitorTrendPoint[]
  errorAgents: MonitorRankingItem[]
  alerts: ObservabilityAlert[]
}

export interface ObservabilityAlert {
  key: string
  level: string
  title: string
  message: string
  active: boolean
}

export interface MonitorOverview {
  metrics: MonitorMetricCard[]
  services: MonitorServiceHealth[]
  topAgents: MonitorRankingItem[]
  topApplications: MonitorRankingItem[]
  hourlyTrend: MonitorTrendPoint[]
}

export interface MonitorMetricCard {
  key: string
  label: string
  value: string
  hint: string
}

export interface MonitorServiceHealth {
  key: string
  name: string
  healthy: boolean
  status: string
  detail: string
}

export interface MonitorRankingItem {
  name: string
  value: number
  valueLabel: string
}

export interface MonitorTrendPoint {
  time: string
  value: number
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}
