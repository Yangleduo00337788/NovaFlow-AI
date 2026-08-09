import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface McpServer {
  id: number
  serverName: string
  description?: string
  transportType: string
  endpoint: string
  status: number
  statusLabel: string
  toolCount: number
  lastConnectedAt?: string
  updatedAt: string
}

export interface McpServerPage {
  list: McpServer[]
  total: number
  page: number
  pageSize: number
}

export function fetchMcpServers(params?: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<ApiResult<McpServerPage>>('/v1/mcp-servers', { params })
}

export function createMcpServer(payload: {
  serverName: string
  description?: string
  transportType: string
  endpoint: string
}) {
  return request.post<ApiResult<McpServer>>('/v1/mcp-servers', payload)
}

export function deleteMcpServer(id: number) {
  return request.delete<ApiResult<void>>(`/v1/mcp-servers/${id}`)
}
