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
}) {
  return request.get<ApiResult<TokenUsageLogPage>>('/v1/token-usage/logs', { params })
}
