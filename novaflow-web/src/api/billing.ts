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

export interface BillingAlert {
  id: number
  alertName: string
  alertType: string
  thresholdPercent: number
  enabled: boolean
  notifyChannels: string[]
  lastTriggeredAt?: string
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

export function updateBillingQuota(monthlyTokenQuota: number) {
  return request.put<ApiResult<BillingQuota>>('/v1/billing/quota', { monthlyTokenQuota })
}

export function fetchBillingAlerts() {
  return request.get<ApiResult<BillingAlert[]>>('/v1/billing/alerts')
}

export function saveBillingAlert(payload: {
  id?: number
  alertName: string
  thresholdPercent: number
  enabled: boolean
  notifyChannels?: string[]
}) {
  return request.put<ApiResult<BillingAlert>>('/v1/billing/alerts', payload)
}

export interface NotifyChannelConfig {
  emailEnabled: boolean
  emailRecipients?: string
  webhookEnabled: boolean
  webhookUrl?: string
  webhookSecretSet?: boolean
  mailConfigured?: boolean
}

export function fetchNotifyChannels() {
  return request.get<ApiResult<NotifyChannelConfig>>('/v1/billing/notify-channels')
}

export function saveNotifyChannels(payload: {
  emailEnabled: boolean
  emailRecipients?: string
  webhookEnabled: boolean
  webhookUrl?: string
  webhookSecret?: string
}) {
  return request.put<ApiResult<NotifyChannelConfig>>('/v1/billing/notify-channels', payload)
}

export async function downloadBillingExport(month: string, format: 'excel' | 'pdf') {
  const response = await request.get('/v1/billing/export', {
    params: { month, format },
    responseType: 'blob',
  })
  return response.data as Blob
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

export type CostAllocationDimension = 'application' | 'workspace' | 'user'

export interface CostAllocationItem {
  id?: number | null
  name: string
  calls: number
  tokens: number
  tokenPercent: number
  costLabel: string
}

export interface CostAllocation {
  periodLabel: string
  dimension: CostAllocationDimension
  dimensionLabel: string
  totalCalls: number
  totalTokens: number
  totalCostLabel: string
  items: CostAllocationItem[]
}

export function fetchBillingAllocation(month?: string, dimension: CostAllocationDimension = 'application') {
  return request.get<ApiResult<CostAllocation>>('/v1/billing/allocation', { params: { month, dimension } })
}
