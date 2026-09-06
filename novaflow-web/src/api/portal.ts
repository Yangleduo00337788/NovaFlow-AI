import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface PortalBranding {
  tenantName: string
  logoUrl?: string
  portalThemeColor?: string
  portalSubtitle?: string
}

export interface PortalCategory {
  code: string
  label: string
  appCount: number
}

export interface PortalAppItem {
  id: number
  appName: string
  description?: string
  icon?: string
  portalCategory?: string
  appType?: string
  defaultAgentId?: number
  defaultAgentName?: string
  publishedAt?: string
  portalPath?: string
  favorited?: boolean
}

export interface PortalAppDetail {
  applicationId: number
  appName: string
  description?: string
  defaultAgentId: number
  defaultAgentName?: string
  portalPath?: string
}

export function fetchPortalBranding() {
  return request.get<ApiResult<PortalBranding>>('/v1/portal/branding')
}

export function fetchPortalCategories() {
  return request.get<ApiResult<PortalCategory[]>>('/v1/portal/categories')
}

export function fetchPortalApps(params?: { category?: string; favoritesOnly?: boolean }) {
  return request.get<ApiResult<PortalAppItem[]>>('/v1/portal/apps', { params })
}

export function togglePortalFavorite(applicationId: number) {
  return request.post<ApiResult<boolean>>('/v1/portal/favorites/toggle', { applicationId })
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

export function exportPortalConversation(
  applicationId: number,
  conversationKey: string,
  format: 'markdown' | 'json' = 'markdown',
) {
  return request.get<Blob>(
    `/v1/portal/apps/${applicationId}/conversations/export`,
    {
      params: { conversationKey, format },
      responseType: 'blob',
    },
  )
}
