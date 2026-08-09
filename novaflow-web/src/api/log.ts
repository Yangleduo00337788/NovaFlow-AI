import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface TokenUsageLogItem {
  id: number
  agentId?: number
  agentName: string
  modelName?: string
  displayName?: string
  usageType: string
  inputTokens: number
  outputTokens: number
  totalTokens: number
  cost?: number
  currency?: string
  costLabel?: string
  latencyMs?: number
  success?: boolean
  statusLabel?: string
  errorMessage?: string
  traceId?: string
  userId?: number
  createdAt: string
}

export interface TokenUsageLogPage {
  list: TokenUsageLogItem[]
  total: number
  page: number
  pageSize: number
}

export function fetchTokenUsageLogs(params?: {
  page?: number
  pageSize?: number
  agentId?: number
  keyword?: string
  success?: boolean
  usageType?: string
}) {
  return request.get<ApiResult<TokenUsageLogPage>>('/v1/token-usage/logs', { params })
}

export function exportTokenUsageLogs(params?: {
  agentId?: number
  keyword?: string
  success?: boolean
  usageType?: string
}) {
  return request.get('/v1/token-usage/logs/export', {
    params,
    responseType: 'blob',
  })
}
