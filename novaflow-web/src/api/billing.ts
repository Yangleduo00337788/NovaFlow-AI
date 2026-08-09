import request from './request'
import type { ApiResult } from '@/types/dashboard'
import type { TokenUsageLogItem } from '@/api/log'

export interface BillingMetric {
  key: string
  label: string
  value: string
  hint?: string
}

export interface BillingTrendPoint {
  label: string
  tokens: number
}

export interface BillingUsageType {
  usageType: string
  usageTypeLabel: string
  calls: number
  tokens: number
}

export interface BillingModelUsage {
  modelName: string
  displayName: string
  calls: number
  tokens: number
}

export interface BillingQuota {
  planType: string
  planTypeLabel: string
  expireAt?: string
  monthlyTokenQuota?: number
  usedTokens: number
  tokenUsedPercent?: number
  memberCount: number
  maxMembers: number
  memberUsedPercent: number
  maxAgents?: number
  maxKnowledge?: number
}

export interface BillingOverview {
  periodLabel: string
  totalCalls: number
  totalTokens: number
  totalCostLabel: string
  tokenChangePercent: string
  callChangePercent: string
  metrics: BillingMetric[]
  dailyTrend: BillingTrendPoint[]
  usageByType: BillingUsageType[]
  topModels: BillingModelUsage[]
  quota: BillingQuota
}

export function fetchBillingOverview(month?: string) {
  return request.get<ApiResult<BillingOverview>>('/v1/billing/overview', { params: { month } })
}

export function fetchBillingQuota() {
  return request.get<ApiResult<BillingQuota>>('/v1/billing/quota')
}

export function fetchBillingRecords(params?: {
  page?: number
  pageSize?: number
  agentId?: number
  usageType?: string
  month?: string
  keyword?: string
}) {
  return request.get<ApiResult<{ list: TokenUsageLogItem[]; total: number; page: number; pageSize: number }>>(
    '/v1/billing/records',
    { params },
  )
}
