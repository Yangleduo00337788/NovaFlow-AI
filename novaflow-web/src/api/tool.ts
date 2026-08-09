import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface ToolDefinition {
  id: number
  toolName: string
  displayName: string
  description?: string
  toolType: string
  method?: string
  url?: string
  mcpToolName?: string
  sourceServerName?: string
  bodyTemplate?: string
  headers?: Record<string, string>
  inputSchema?: Record<string, unknown>
  enabled?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface ToolSaveRequest {
  toolName: string
  displayName: string
  description?: string
  toolType?: string
  method?: string
  url: string
  bodyTemplate?: string
  headers?: Record<string, string>
  inputSchema?: Record<string, unknown>
}

export interface ToolTestRequest {
  arguments?: Record<string, unknown>
}

export interface ToolTestResult {
  success: boolean
  result?: string
  error?: string
}

export function fetchTools(params?: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<ApiResult<{ list: ToolDefinition[]; total: number }>>('/v1/tools', { params })
}

export function fetchToolOptions(keyword?: string) {
  return request.get<ApiResult<ToolDefinition[]>>('/v1/tools/options', { params: { keyword } })
}

export function fetchTool(id: number) {
  return request.get<ApiResult<ToolDefinition>>(`/v1/tools/${id}`)
}

export function createTool(data: ToolSaveRequest) {
  return request.post<ApiResult<ToolDefinition>>('/v1/tools', data)
}

export function updateTool(id: number, data: ToolSaveRequest) {
  return request.put<ApiResult<ToolDefinition>>(`/v1/tools/${id}`, data)
}

export function deleteTool(id: number) {
  return request.delete<ApiResult<void>>(`/v1/tools/${id}`)
}

export function testTool(id: number, data?: ToolTestRequest) {
  return request.post<ApiResult<ToolTestResult>>(`/v1/tools/${id}/test`, data || {})
}
