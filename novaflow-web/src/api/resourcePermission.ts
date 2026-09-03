import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface ResourcePermissionItem {
  id: number
  tenantId: number
  resourceType: string
  resourceId: number
  userId: number
  permissionCode: string
  createdBy?: number
  createdAt?: string
}

export interface ResourcePermissionGrant {
  userId: number
  permissionCode: string
}

export function fetchResourcePermissions(resourceType: string, resourceId: number) {
  return request.get<ApiResult<ResourcePermissionItem[]>>(
    `/v1/resources/${resourceType}/${resourceId}/permissions`,
  )
}

export function saveResourcePermissions(
  resourceType: string,
  resourceId: number,
  grants: ResourcePermissionGrant[],
) {
  return request.put<ApiResult<ResourcePermissionItem[]>>(
    `/v1/resources/${resourceType}/${resourceId}/permissions`,
    { grants },
  )
}
