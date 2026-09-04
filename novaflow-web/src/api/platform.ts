import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface PlatformTenant {
  id: number
  tenantCode: string
  tenantName: string
  contactName?: string
  contactEmail?: string
  contactPhone?: string
  planType: string
  planTypeLabel: string
  status?: number
  expireAt?: string
  maxMembers?: number
  maxAgents?: number
  maxKnowledge?: number
  maxStorageMb?: number
  monthlyTokenQuota?: number
  memberCount?: number
  usedTokensThisMonth?: number
  createdAt?: string
  updatedAt?: string
}

export interface PlatformTenantPage {
  list: PlatformTenant[]
  total: number
  page: number
  pageSize: number
}

export interface PlatformGlobalStats {
  tenantCount: number
  activeTenantCount: number
  totalMembers: number
  totalAgents: number
  totalKnowledgeBases: number
  totalWorkflows: number
  tokensUsedThisMonth: number
}

export function fetchPlatformTenants(params: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<ApiResult<PlatformTenantPage>>('/v1/platform/tenants', { params })
}

export function fetchPlatformTenant(id: number) {
  return request.get<ApiResult<PlatformTenant>>(`/v1/platform/tenants/${id}`)
}

export function updatePlatformTenant(id: number, data: Record<string, unknown>) {
  return request.put<ApiResult<PlatformTenant>>(`/v1/platform/tenants/${id}`, data)
}

export function deletePlatformTenant(id: number) {
  return request.delete<ApiResult<void>>(`/v1/platform/tenants/${id}`)
}

export function fetchPlatformStats() {
  return request.get<ApiResult<PlatformGlobalStats>>('/v1/platform/stats')
}
