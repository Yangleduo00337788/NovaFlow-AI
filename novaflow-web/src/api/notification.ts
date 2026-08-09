import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface UserNotification {
  id: number
  category: string
  title: string
  content: string
  linkUrl?: string
  read: boolean
  createdAt: string
}

export function fetchNotifications(params?: { page?: number; pageSize?: number }) {
  return request.get<ApiResult<{ list: UserNotification[]; total: number; page: number; pageSize: number }>>(
    '/v1/notifications',
    { params },
  )
}

export function fetchUnreadNotificationCount() {
  return request.get<ApiResult<number>>('/v1/notifications/unread-count')
}

export function markNotificationRead(id: number) {
  return request.post<ApiResult<void>>(`/v1/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return request.post<ApiResult<void>>('/v1/notifications/read-all')
}
