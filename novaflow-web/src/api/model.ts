import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface ModelProviderItem {
  id?: number
  providerCode: string
  providerName: string
  description?: string
  baseUrl?: string
  defaultBaseUrl?: string
  apiKeyMasked?: string
  configured: boolean
  enabled: boolean
  modelCount: number
  updatedAt?: string
}

export interface ModelConfigItem {
  id: number
  providerId: number
  providerCode: string
  providerName: string
  modelName: string
  modelType: string
  displayName: string
  contextWindow: number
  maxOutputTokens: number
  inputPrice?: number
  outputPrice?: number
  defaultTemperature: number
  enabled: boolean
  isDefault: boolean
  updatedAt?: string
}

export interface ModelOverview {
  totalCalls: number
  totalTokens: number
  totalCost: string
  configuredProviders: number
  enabledModels: number
  topModels: Array<{ modelName: string; displayName: string; calls: number; tokens: number }>
}

export interface ModelProviderSaveRequest {
  providerCode: string
  baseUrl?: string
  apiKey?: string
  enabled?: boolean
}

export interface ModelConfigSaveRequest {
  providerId: number
  modelName: string
  modelType: string
  displayName: string
  contextWindow?: number
  maxOutputTokens?: number
  inputPrice?: number
  outputPrice?: number
  defaultTemperature?: number
  enabled?: boolean
  isDefault?: boolean
}

export interface ModelConnectivityTestResult {
  success: boolean
  message: string
  latencyMs: number
  modelName?: string
}

export function fetchModelOverview() {
  return request.get<ApiResult<ModelOverview>>('/v1/models/overview')
}

export function fetchModelProviders() {
  return request.get<ApiResult<ModelProviderItem[]>>('/v1/models/providers')
}

export function saveModelProvider(data: ModelProviderSaveRequest) {
  return request.post<ApiResult<ModelProviderItem>>('/v1/models/providers', data)
}

export function updateModelProvider(id: number, data: ModelProviderSaveRequest) {
  return request.put<ApiResult<ModelProviderItem>>(`/v1/models/providers/${id}`, data)
}

export function deleteModelProvider(id: number) {
  return request.delete<ApiResult<void>>(`/v1/models/providers/${id}`)
}

export interface ModelSyncResult {
  added: number
  updated: number
  disabled: number
  total: number
  message: string
}

export function testModelProvider(id: number, data?: { apiKey?: string; baseUrl?: string; modelName?: string }) {
  return request.post<ApiResult<ModelConnectivityTestResult>>(`/v1/models/providers/${id}/test`, data || {})
}

export function syncModelProvider(id: number) {
  return request.post<ApiResult<ModelSyncResult>>(`/v1/models/providers/${id}/sync`)
}

export function fetchModelConfigs(params?: { providerId?: number; modelType?: string }) {
  return request.get<ApiResult<ModelConfigItem[]>>('/v1/models/configs', { params })
}

export function createModelConfig(data: ModelConfigSaveRequest) {
  return request.post<ApiResult<ModelConfigItem>>('/v1/models/configs', data)
}

export function updateModelConfig(id: number, data: ModelConfigSaveRequest) {
  return request.put<ApiResult<ModelConfigItem>>(`/v1/models/configs/${id}`, data)
}

export function deleteModelConfig(id: number) {
  return request.delete<ApiResult<void>>(`/v1/models/configs/${id}`)
}

export function setDefaultModelConfig(id: number) {
  return request.put<ApiResult<ModelConfigItem>>(`/v1/models/configs/${id}/default`)
}
