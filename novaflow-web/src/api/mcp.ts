import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface McpDiscoveredTool {
  name: string
  description?: string
  inputSchema?: Record<string, unknown>
}

export interface McpServer {
  id: number
  serverName: string
  description?: string
  transportType: string
  commandSummary: string
  serverConfig?: string
  status: number
  statusLabel: string
  toolCount: number
  lastConnectedAt?: string
  updatedAt: string
  tools?: McpDiscoveredTool[]
}

export interface McpConnectResult {
  id: number
  serverName: string
  status: number
  statusLabel: string
  toolCount: number
  message: string
  lastConnectedAt?: string
  tools: McpDiscoveredTool[]
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
  serverConfig: string
}) {
  return request.post<ApiResult<McpServer>>('/v1/mcp-servers', payload)
}

export function deleteMcpServer(id: number) {
  return request.delete<ApiResult<void>>(`/v1/mcp-servers/${id}`)
}

export function fetchMcpServerDetail(id: number) {
  return request.get<ApiResult<McpServer>>(`/v1/mcp-servers/${id}`)
}

export function connectMcpServer(id: number) {
  return request.post<ApiResult<McpConnectResult>>(`/v1/mcp-servers/${id}/connect`)
}
