import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface PortalAppItem {
  id: number
  appName: string
  description?: string
  icon?: string
  appType?: string
  defaultAgentId?: number
  defaultAgentName?: string
  publishedAt?: string
  portalPath?: string
}

export interface PortalAppDetail {
  applicationId: number
  appName: string
  description?: string
  defaultAgentId: number
  defaultAgentName?: string
  portalPath?: string
}

export function fetchPortalApps() {
  return request.get<ApiResult<PortalAppItem[]>>('/v1/portal/apps')
}

export function fetchPortalApp(id: number) {
  return request.get<ApiResult<PortalAppDetail>>(`/v1/portal/apps/${id}`)
}

export interface PortalConversationItem {
  id: number
  conversationKey: string
  channel: string
  messageCount: number
  preview?: string
  lastMessageAt?: string
  createdAt?: string
}

export interface PortalConversationMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  tokensUsed?: number
  latencyMs?: number
  createdAt?: string
}

export interface PortalConversationPage {
  list: PortalConversationItem[]
  total: number
  page: number
  pageSize: number
}

export function fetchPortalConversations(
  applicationId: number,
  params?: { page?: number; pageSize?: number },
) {
  return request.get<ApiResult<PortalConversationPage>>(
    `/v1/portal/apps/${applicationId}/conversations`,
    { params },
  )
}

export function fetchPortalConversationMessages(applicationId: number, conversationKey: string) {
  return request.get<ApiResult<PortalConversationMessage[]>>(
    `/v1/portal/apps/${applicationId}/conversations/messages`,
    { params: { conversationKey } },
  )
}
