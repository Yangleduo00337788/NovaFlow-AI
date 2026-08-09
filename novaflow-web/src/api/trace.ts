import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface TraceSpan {
  traceId: string
  spanType: string
  spanTypeLabel: string
  name: string
  status: number
  statusLabel: string
  durationMs?: number
  durationLabel?: string
  startedAt: string
  errorMessage?: string
}

export interface TraceNode {
  nodeId: string
  nodeName: string
  nodeType: string
  status: number
  statusLabel: string
  durationMs?: number
  durationLabel?: string
  errorMessage?: string
  startedAt?: string
  finishedAt?: string
  offsetMs?: number
}

export interface TraceDetail extends TraceSpan {
  finishedAt?: string
  nodes: TraceNode[]
}

export interface TraceSpanPage {
  list: TraceSpan[]
  total: number
  page: number
  pageSize: number
}

export function fetchTraceSpans(params?: {
  page?: number
  pageSize?: number
  keyword?: string
  type?: string
  status?: number
  timeRange?: string
}) {
  return request.get<ApiResult<TraceSpanPage>>('/v1/trace/spans', { params })
}

export function fetchTraceDetail(traceId: string) {
  return request.get<ApiResult<TraceDetail>>(`/v1/trace/spans/${encodeURIComponent(traceId)}`)
}
