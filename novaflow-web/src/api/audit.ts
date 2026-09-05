import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface AuditLogItem {
  id: number
  tenantId?: number
  userId?: number
  action: string
  resourceType?: string
  resourceId?: number
  detail?: string
  clientIp?: string
  createdAt: string
}

export interface AuditLogPage {
  list: AuditLogItem[]
  total: number
  page: number
  pageSize: number
}

export function fetchAuditLogs(params: {
  page?: number
  pageSize?: number
  action?: string
  resourceType?: string
  startDate?: string
  endDate?: string
  keyword?: string
}) {
  return request.get<ApiResult<AuditLogPage>>('/v1/audit-logs', { params })
}

export function fetchPlatformAuditLogs(params: {
  page?: number
  pageSize?: number
  action?: string
  resourceType?: string
  startDate?: string
  endDate?: string
  keyword?: string
}) {
  return request.get<ApiResult<AuditLogPage>>('/v1/platform/audit-logs', { params })
}
