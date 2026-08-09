import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface TenantInfo {
  id: number
  tenantCode: string
  tenantName: string
  logoUrl?: string
  contactName?: string
  contactEmail?: string
  contactPhone?: string
  planType: string
  planTypeLabel: string
  status?: number
  expireAt?: string
  maxMembers?: number
  memberCount?: number
  maxAgents?: number
  maxKnowledge?: number
  maxStorageMb?: number
  monthlyTokenQuota?: number
  createdAt?: string
}

export interface TenantPlanSummary {
  planType: string
  planTypeLabel: string
  expireAt?: string
  memberCount: number
  maxMembers: number
  usedPercent: number
  monthlyTokenQuota?: number
  usedTokens?: number
  tokenUsedPercent?: number
}

export interface WorkspaceItem {
  id: number
  workspaceName: string
  description?: string
  isDefault?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface MemberItem {
  id: number
  userId: number
  username?: string
  nickname?: string
  email?: string
  roleCode?: string
  roleName?: string
  status?: number
  joinedAt?: string
  lastLoginAt?: string
}

export interface TenantUpdateRequest {
  tenantName: string
  logoUrl?: string
  contactName?: string
  contactEmail?: string
  contactPhone?: string
}

export interface WorkspaceSaveRequest {
  workspaceName: string
  description?: string
}

export interface MemberInviteRequest {
  email: string
  nickname?: string
  roleCode: string
  password?: string
}

export interface MemberUpdateRequest {
  roleCode?: string
  status?: number
}

export const ROLE_OPTIONS = [
  { value: 'tenant_admin', label: '企业管理员' },
  { value: 'developer', label: '开发者' },
  { value: 'user', label: '普通用户' },
]

export function fetchTenant() {
  return request.get<ApiResult<TenantInfo>>('/v1/org/tenant')
}

export function updateTenant(data: TenantUpdateRequest) {
  return request.put<ApiResult<TenantInfo>>('/v1/org/tenant', data)
}

export function fetchPlanSummary() {
  return request.get<ApiResult<TenantPlanSummary>>('/v1/org/plan-summary')
}

export function fetchWorkspaces() {
  return request.get<ApiResult<WorkspaceItem[]>>('/v1/org/workspaces')
}

export function createWorkspace(data: WorkspaceSaveRequest) {
  return request.post<ApiResult<WorkspaceItem>>('/v1/org/workspaces', data)
}

export function updateWorkspace(id: number, data: WorkspaceSaveRequest) {
  return request.put<ApiResult<WorkspaceItem>>(`/v1/org/workspaces/${id}`, data)
}

export function deleteWorkspace(id: number) {
  return request.delete<ApiResult<void>>(`/v1/org/workspaces/${id}`)
}

export function fetchMembers(params?: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<ApiResult<{ list: MemberItem[]; total: number }>>('/v1/org/members', { params })
}

export function inviteMember(data: MemberInviteRequest) {
  return request.post<ApiResult<MemberItem>>('/v1/org/members/invite', data)
}

export function updateMember(id: number, data: MemberUpdateRequest) {
  return request.put<ApiResult<MemberItem>>(`/v1/org/members/${id}`, data)
}

export function removeMember(id: number) {
  return request.delete<ApiResult<void>>(`/v1/org/members/${id}`)
}
