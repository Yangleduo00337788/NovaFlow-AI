import request from './request'
import type { ApiResult, MonitorOverview } from '@/types/monitor'

export function fetchMonitorOverview() {
  return request.get<ApiResult<MonitorOverview>>('/v1/monitor/overview')
}

export function fetchObservabilityOverview() {
  return request.get<ApiResult<import('@/types/monitor').ObservabilityOverview>>('/v1/monitor/observability')
}
