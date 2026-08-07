import request from './request'
import type { ApiResult, DashboardOverview } from '@/types/dashboard'

export function fetchDashboardOverview() {
  return request.get<ApiResult<DashboardOverview>>('/v1/dashboard/overview')
}
