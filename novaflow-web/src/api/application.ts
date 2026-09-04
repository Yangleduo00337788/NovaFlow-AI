import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface ApplicationItem {
  id: number
  workspaceId?: number
  appName: string
  description?: string
  icon?: string
  appType?: string
  defaultAgentId?: number
  defaultAgentName?: string
  publishStatus?: number
  accessType?: string
  invokeCount?: number
  publishedAt?: string
  status?: number
  agentCount?: number
  knowledgeBaseCount?: number
  agentIds?: number[]
  knowledgeBaseIds?: number[]
  createdAt?: string
  updatedAt?: string
}

export interface ApplicationSaveRequest {
  appName: string
  description?: string
  icon?: string
  appType?: string
  accessType?: string
  defaultAgentId?: number
  agentIds?: number[]
  knowledgeBaseIds?: number[]
}

export interface ApplicationPublishInfo {
  applicationId: number
  publishStatus?: number
  defaultAgentId?: number
  defaultAgentName?: string
  publishedAt?: string
  chatEndpoint?: string
  streamEndpoint?: string
  embedPath?: string
  portalPath?: string
}

export const APP_TYPES = [
  { value: 'agent', label: 'Agent 应用' },
  { value: 'workflow', label: '工作流应用' },
  { value: 'mixed', label: '混合应用' },
]

export const ACCESS_TYPES = [
  { value: 'team', label: '团队内' },
  { value: 'public', label: '公开' },
  { value: 'private', label: '指定成员' },
]

export function getPublishStatusLabel(status?: number) {
  if (status === 1) return '已发布'
  if (status === 2) return '已下线'
  return '草稿'
}

export function fetchApplications(params?: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<ApiResult<{ list: ApplicationItem[]; total: number }>>('/v1/applications', { params })
}

export function fetchApplicationOptions() {
  return request.get<ApiResult<ApplicationItem[]>>('/v1/applications/options')
}

export function fetchApplication(id: number) {
  return request.get<ApiResult<ApplicationItem>>(`/v1/applications/${id}`)
}

export function createApplication(data: ApplicationSaveRequest) {
  return request.post<ApiResult<ApplicationItem>>('/v1/applications', data)
}

export function updateApplication(id: number, data: ApplicationSaveRequest) {
  return request.put<ApiResult<ApplicationItem>>(`/v1/applications/${id}`, data)
}

export function deleteApplication(id: number) {
  return request.delete<ApiResult<void>>(`/v1/applications/${id}`)
}

export function fetchApplicationPublishInfo(id: number) {
  return request.get<ApiResult<ApplicationPublishInfo>>(`/v1/applications/${id}/publish`)
}

export function publishApplication(id: number) {
  return request.post<ApiResult<ApplicationPublishInfo>>(`/v1/applications/${id}/publish`)
}

export function unpublishApplication(id: number) {
  return request.post<ApiResult<ApplicationPublishInfo>>(`/v1/applications/${id}/unpublish`)
}
