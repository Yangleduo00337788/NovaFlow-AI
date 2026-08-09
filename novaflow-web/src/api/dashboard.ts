import request from './request'
import type { ApiResult, DashboardOverview, RecentItem } from '@/types/dashboard'

export function fetchDashboardOverview() {
  return request.get<ApiResult<DashboardOverview>>('/v1/dashboard/overview')
}

export function toggleDashboardFavorite(payload: {
  resourceType: string
  resourceId: number
  resourceName: string
}) {
  return request.post<ApiResult<boolean>>('/v1/dashboard/favorites/toggle', payload)
}

export function fetchDashboardRecentItems(limit = 20) {
  return request.get<ApiResult<RecentItem[]>>('/v1/dashboard/recent-items', { params: { limit } })
}

export function fetchDashboardFavorites(limit = 20) {
  return request.get<ApiResult<RecentItem[]>>('/v1/dashboard/favorites', { params: { limit } })
}
