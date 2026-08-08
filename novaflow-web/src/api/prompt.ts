import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface PromptVariable {
  name: string
  description?: string
  defaultValue?: string
  required?: boolean
}

export interface PromptTemplate {
  id: number
  templateName: string
  description?: string
  category: string
  content: string
  variables?: PromptVariable[]
  visibility?: string
  currentVersion: number
  usageCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface PromptVersion {
  id: number
  templateId: number
  version: number
  content: string
  variables?: PromptVariable[]
  changeLog?: string
  publishedAt?: string
}

export interface PromptSaveRequest {
  templateName: string
  description?: string
  category?: string
  content: string
  variables?: PromptVariable[]
  visibility?: string
  changeLog?: string
}

export interface PromptTestRequest {
  modelConfigId?: number
  variables?: Record<string, unknown>
  userMessage?: string
}

export interface PromptTestResult {
  renderedPrompt: string
  reply?: string
  tokensUsed?: number
  latencyMs?: number
}

export const PROMPT_CATEGORIES = [
  { value: 'customer_service', label: '客服' },
  { value: 'qa', label: '问答' },
  { value: 'writing', label: '写作' },
  { value: 'code', label: '代码' },
  { value: 'custom', label: '自定义' },
]

export function fetchPrompts(params?: { page?: number; pageSize?: number; keyword?: string; category?: string }) {
  return request.get<ApiResult<{ list: PromptTemplate[]; total: number }>>('/v1/prompts', { params })
}

export function fetchPromptOptions(keyword?: string) {
  return request.get<ApiResult<PromptTemplate[]>>('/v1/prompts/options', { params: { keyword } })
}

export function fetchPrompt(id: number) {
  return request.get<ApiResult<PromptTemplate>>(`/v1/prompts/${id}`)
}

export function fetchPromptVersions(id: number) {
  return request.get<ApiResult<PromptVersion[]>>(`/v1/prompts/${id}/versions`)
}

export function createPrompt(data: PromptSaveRequest) {
  return request.post<ApiResult<PromptTemplate>>('/v1/prompts', data)
}

export function updatePrompt(id: number, data: PromptSaveRequest) {
  return request.put<ApiResult<PromptTemplate>>(`/v1/prompts/${id}`, data)
}

export function rollbackPrompt(id: number, version: number) {
  return request.post<ApiResult<PromptTemplate>>(`/v1/prompts/${id}/rollback`, null, { params: { version } })
}

export function deletePrompt(id: number) {
  return request.delete<ApiResult<void>>(`/v1/prompts/${id}`)
}

export function testPrompt(id: number, data?: PromptTestRequest) {
  return request.post<ApiResult<PromptTestResult>>(`/v1/prompts/${id}/test`, data || {})
}

export function getCategoryLabel(category?: string) {
  return PROMPT_CATEGORIES.find((item) => item.value === category)?.label || category || '自定义'
}
