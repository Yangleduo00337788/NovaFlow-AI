import request from './request'
import type { ApiResult } from '@/types/dashboard'
import { useAuthStore } from '@/stores/auth'

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
  modelConfigId?: number
  temperature?: number
  maxTokens?: number
  memoryType?: string
  memoryWindow?: number
  retrievalTopK?: number
  retrievalScoreThreshold?: number
  knowledgeBaseIds?: number[]
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
  modelConfigId?: number
  temperature?: number
  maxTokens?: number
  memoryType?: string
  memoryWindow?: number
  retrievalTopK?: number
  retrievalScoreThreshold?: number
  knowledgeBaseIds?: number[]
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

export interface AgentPublishInfo {
  agentId: number
  status: number
  version: number
  publishedAt?: string
  apiKeyPrefix?: string
  apiKey?: string
  chatEndpoint: string
  streamEndpoint: string
}

export function fetchAgentPublishInfo(id: number) {
  return request.get<ApiResult<AgentPublishInfo>>(`/v1/agents/${id}/publish`)
}

export function publishAgent(id: number) {
  return request.post<ApiResult<AgentPublishInfo>>(`/v1/agents/${id}/publish`)
}

export function unpublishAgent(id: number) {
  return request.post<ApiResult<AgentPublishInfo>>(`/v1/agents/${id}/unpublish`)
}

export function rotateAgentApiKey(id: number) {
  return request.post<ApiResult<AgentPublishInfo>>(`/v1/agents/${id}/rotate-api-key`)
}

export interface AgentDebugChatResponse {
  reply: string
  agentName: string
  tokensUsed: number
  latencyMs: number
  debugMode: boolean
  sources?: RetrievalSourceItem[]
}

export interface RetrievalSourceItem {
  knowledgeBaseId?: number
  knowledgeBaseName?: string
  documentId?: number
  docName?: string
  chunkIndex?: number
  text: string
  score?: number
}

export function fetchAgentDebugWelcome(id: number) {
  return request.get<ApiResult<AgentDebugChatResponse>>(`/v1/agents/${id}/debug/welcome`)
}

export function debugAgentChat(id: number, message: string, conversationId?: string) {
  return request.post<ApiResult<AgentDebugChatResponse>>(`/v1/agents/${id}/debug/chat`, {
    message,
    conversationId,
  })
}

export interface AgentDebugStreamEvent {
  type: 'token' | 'done' | 'error'
  content?: string
  reply?: string
  agentName?: string
  tokensUsed?: number
  latencyMs?: number
  debugMode?: boolean
  message?: string
  sources?: RetrievalSourceItem[]
}

export function clearAgentDebugConversation(id: number, conversationId: string) {
  return request.delete<ApiResult<void>>(`/v1/agents/${id}/debug/conversation`, {
    params: { conversationId },
  })
}

export async function streamAgentDebugChat(
  id: number,
  message: string,
  conversationId: string | undefined,
  handlers: {
    onToken: (token: string) => void
    onDone: (data: AgentDebugChatResponse) => void
    onError: (error: Error) => void
  },
  signal?: AbortSignal,
) {
  const auth = useAuthStore()
  const response = await fetch(`/api/v1/agents/${id}/debug/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(auth.token ? { Authorization: auth.token } : {}),
    },
    body: JSON.stringify({ message, conversationId }),
    signal,
  })

  if (response.status === 401) {
    auth.clear()
    window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`
    throw new Error('登录已过期')
  }

  if (!response.ok || !response.body) {
    let errorMessage = '流式请求失败'
    try {
      const result = (await response.json()) as ApiResult<unknown>
      errorMessage = result.message || errorMessage
    } catch {
      // ignore parse error
    }
    throw new Error(errorMessage)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const chunks = buffer.split('\n\n')
    buffer = chunks.pop() || ''

    for (const chunk of chunks) {
      const line = chunk
        .split('\n')
        .find((item) => item.startsWith('data:'))
      if (!line) continue

      const payload = line.slice(5).trim()
      if (!payload) continue

      const event = JSON.parse(payload) as AgentDebugStreamEvent
      if (event.type === 'token' && event.content) {
        handlers.onToken(event.content)
      } else if (event.type === 'done') {
        handlers.onDone({
          reply: event.reply || '',
          agentName: event.agentName || '',
          tokensUsed: event.tokensUsed || 0,
          latencyMs: event.latencyMs || 0,
          debugMode: event.debugMode ?? false,
          sources: event.sources,
        })
      } else if (event.type === 'error') {
        throw new Error(event.message || '流式对话失败')
      }
    }
  }
}
