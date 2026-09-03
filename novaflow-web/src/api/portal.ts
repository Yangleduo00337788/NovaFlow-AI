import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface PortalAppItem {
  id: number
  appName: string
  description?: string
  icon?: string
  appType?: string
  defaultAgentId?: number
  defaultAgentName?: string
  publishedAt?: string
  portalPath?: string
}

export interface PortalAppDetail {
  applicationId: number
  appName: string
  description?: string
  defaultAgentId: number
  defaultAgentName?: string
  portalPath?: string
}

export function fetchPortalApps() {
  return request.get<ApiResult<PortalAppItem[]>>('/v1/portal/apps')
}

export function fetchPortalApp(id: number) {
  return request.get<ApiResult<PortalAppDetail>>(`/v1/portal/apps/${id}`)
}
