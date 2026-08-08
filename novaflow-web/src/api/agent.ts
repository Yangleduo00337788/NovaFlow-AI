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
  rerankEnabled?: boolean
  rerankModel?: string
  rerankCandidateK?: number
  knowledgeBaseIds?: number[]
  tools?: AgentToolDefinition[]
  createdAt?: string
  updatedAt?: string
}

export interface AgentToolDefinition {
  name: string
  description?: string
  method?: string
  url?: string
  headers?: Record<string, string>
  inputSchema?: Record<string, unknown>
  /** @deprecated 已移除百度搜索工具，加载旧数据时用于过滤 */
  toolType?: string
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
  rerankEnabled?: boolean
  rerankModel?: string
  rerankCandidateK?: number
  knowledgeBaseIds?: number[]
  tools?: AgentToolDefinition[]
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

export interface ModelCapabilities {
  supportsDeepThinking?: boolean
  supportsWebSearch?: boolean
}

export interface AgentDebugChatResponse {
  reply: string
  agentName: string
  tokensUsed: number
  latencyMs: number
  debugMode: boolean
  thinking?: string
  sources?: RetrievalSourceItem[]
  webSearchSources?: WebSearchSourceItem[]
  modelCapabilities?: ModelCapabilities
  modelName?: string
  providerName?: string
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

export interface WebSearchSourceItem {
  index?: number
  title: string
  url?: string
  snippet?: string
}

export interface AgentDebugStreamEvent {
  type: 'token' | 'thinking_token' | 'tool_call' | 'tool_result' | 'web_search' | 'done' | 'error'
  content?: string
  reply?: string
  thinking?: string
  toolName?: string
  toolArgs?: string
  toolResult?: string
  agentName?: string
  tokensUsed?: number
  latencyMs?: number
  debugMode?: boolean
  message?: string
  sources?: RetrievalSourceItem[]
  webSearchSources?: WebSearchSourceItem[]
  modelName?: string
}

export interface DebugAttachment {
  fileName: string
  content: string
  contentLength?: number
}

export async function uploadDebugAttachment(agentId: number, file: File) {
  const auth = useAuthStore()
  const formData = new FormData()
  formData.append('file', file)
  const response = await fetch(`/api/v1/agents/${agentId}/debug/attachments`, {
    method: 'POST',
    headers: {
      ...(auth.token ? { Authorization: auth.token } : {}),
    },
    body: formData,
  })
  if (!response.ok) {
    const result = (await response.json()) as ApiResult<unknown>
    throw new Error(result.message || '附件上传失败')
  }
  const result = (await response.json()) as ApiResult<DebugAttachment>
  return result.data
}

export function clearAgentDebugConversation(id: number, conversationId: string) {
  return request.delete<ApiResult<void>>(`/v1/agents/${id}/debug/conversation`, {
    params: { conversationId },
  })
}

export interface ConversationItem {
  id: number
  conversationKey: string
  channel: string
  messageCount: number
  preview?: string
  lastMessageAt?: string
  createdAt?: string
}

export interface ConversationMessageItem {
  id: number
  role: 'user' | 'assistant'
  content: string
  tokensUsed?: number
  latencyMs?: number
  sources?: RetrievalSourceItem[]
  createdAt?: string
}

export interface ConversationPage {
  list: ConversationItem[]
  total: number
  page: number
  pageSize: number
}

export function fetchDebugConversations(
  agentId: number,
  params?: { page?: number; pageSize?: number },
) {
  return request.get<ApiResult<ConversationPage>>(`/v1/agents/${agentId}/debug/conversations`, { params })
}

export function fetchDebugConversationMessages(agentId: number, conversationKey: string) {
  return request.get<ApiResult<ConversationMessageItem[]>>(
    `/v1/agents/${agentId}/debug/conversations/messages`,
    { params: { conversationKey } },
  )
}

export async function streamAgentDebugChat(
  id: number,
  message: string,
  conversationId: string | undefined,
  handlers: {
    onToken: (token: string) => void
    onThinkingToken?: (token: string) => void
    onToolCall?: (toolName: string, toolArgs: string) => void
    onToolResult?: (toolName: string, toolResult: string) => void
    onWebSearch?: (sources: WebSearchSourceItem[]) => void
    onDone: (data: AgentDebugChatResponse) => void
    onError: (error: Error) => void
  },
  signal?: AbortSignal,
  options?: {
    enableDeepThinking?: boolean
    enableWebSearch?: boolean
    attachmentName?: string
    attachmentContext?: string
  },
) {
  const auth = useAuthStore()
  const response = await fetch(`/api/v1/agents/${id}/debug/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(auth.token ? { Authorization: auth.token } : {}),
    },
    body: JSON.stringify({
      message,
      conversationId,
      enableDeepThinking: options?.enableDeepThinking,
      enableWebSearch: options?.enableWebSearch,
      attachmentName: options?.attachmentName,
      attachmentContext: options?.attachmentContext,
    }),
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
      if (event.type === 'thinking_token' && event.content) {
        handlers.onThinkingToken?.(event.content)
      } else if (event.type === 'tool_call' && event.toolName) {
        handlers.onToolCall?.(event.toolName, event.toolArgs || '{}')
      } else if (event.type === 'tool_result' && event.toolName) {
        handlers.onToolResult?.(event.toolName, event.toolResult || '')
      } else if (event.type === 'web_search' && event.webSearchSources) {
        handlers.onWebSearch?.(event.webSearchSources)
      } else if (event.type === 'token' && event.content) {
        handlers.onToken(event.content)
      } else if (event.type === 'done') {
        handlers.onDone({
          reply: event.reply || '',
          agentName: event.agentName || '',
          tokensUsed: event.tokensUsed || 0,
          latencyMs: event.latencyMs || 0,
          debugMode: event.debugMode ?? false,
          thinking: event.thinking,
          sources: event.sources,
          webSearchSources: event.webSearchSources,
          modelName: event.modelName,
        })
      } else if (event.type === 'error') {
        throw new Error(event.message || '流式对话失败')
      }
    }
  }
}
