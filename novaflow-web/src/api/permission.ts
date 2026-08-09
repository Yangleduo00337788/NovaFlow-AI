import request from './request'
import type { ApiResult } from '@/types/dashboard'
import type { MemberItem } from './org'

export interface PermissionItem {
  id: number
  permissionCode: string
  permissionName: string
  module: string
}

export interface RoleItem {
  id: number
  roleCode: string
  roleName: string
  description?: string
  isSystem?: boolean
  memberCount?: number
  permissionCodes?: string[]
}

export const MODULE_LABELS: Record<string, string> = {
  agent: 'Agent',
  workflow: '工作流',
  knowledge: '知识库',
  model: '模型',
  prompt: 'Prompt',
  application: '应用',
  billing: '账单',
  monitor: '监控',
  tenant: '组织',
}

export function fetchRoles() {
  return request.get<ApiResult<RoleItem[]>>('/v1/roles')
}

export function fetchRole(id: number) {
  return request.get<ApiResult<RoleItem>>(`/v1/roles/${id}`)
}

export function fetchRoleMembers(id: number) {
  return request.get<ApiResult<MemberItem[]>>(`/v1/roles/${id}/members`)
}

export function fetchPermissions() {
  return request.get<ApiResult<PermissionItem[]>>('/v1/permissions')
}

export function fetchGroupedPermissions() {
  return request.get<ApiResult<Record<string, PermissionItem[]>>>('/v1/permissions/grouped')
}
