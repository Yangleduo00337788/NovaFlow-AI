import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface AgentItem {
  id: number
  applicationId: number
  agentName: string
  description?: string
  icon?: string
  agentType: string
  status: number
  version: number
  systemPrompt?: string
  welcomeMessage?: string
  temperature?: number
  maxTokens?: number
  memoryType?: string
  memoryWindow?: number
  createdAt?: string
  updatedAt?: string
}

export interface AgentPage {
  list: AgentItem[]
  total: number
  page: number
  pageSize: number
}

export interface AgentSaveRequest {
  agentName: string
  description?: string
  icon?: string
  agentType?: string
  applicationId?: number
  systemPrompt?: string
  welcomeMessage?: string
  temperature?: number
  maxTokens?: number
  memoryType?: string
  memoryWindow?: number
}

export function fetchAgents(params: { page?: number; pageSize?: number; keyword?: string; agentType?: string }) {
  return request.get<ApiResult<AgentPage>>('/v1/agents', { params })
}

export function fetchAgent(id: number) {
  return request.get<ApiResult<AgentItem>>(`/v1/agents/${id}`)
}

export function createAgent(data: AgentSaveRequest) {
  return request.post<ApiResult<AgentItem>>('/v1/agents', data)
}

export function updateAgent(id: number, data: AgentSaveRequest) {
  return request.put<ApiResult<AgentItem>>(`/v1/agents/${id}`, data)
}

export function deleteAgent(id: number) {
  return request.delete<ApiResult<void>>(`/v1/agents/${id}`)
}

export interface AgentDebugChatResponse {
  reply: string
  agentName: string
  tokensUsed: number
  latencyMs: number
  debugMode: boolean
}

export function fetchAgentDebugWelcome(id: number) {
  return request.get<ApiResult<AgentDebugChatResponse>>(`/v1/agents/${id}/debug/welcome`)
}

export function debugAgentChat(id: number, message: string) {
  return request.post<ApiResult<AgentDebugChatResponse>>(`/v1/agents/${id}/debug/chat`, { message })
}
