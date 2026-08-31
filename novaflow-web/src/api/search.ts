import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface GlobalSearchItem {
  type: 'application' | 'agent' | 'knowledge' | 'workflow' | string
  id: number
  title: string
  subtitle?: string
  path: string
}

export function globalSearch(keyword: string, limit = 20) {
  return request.get<ApiResult<GlobalSearchItem[]>>('/api/v1/search', {
    params: { keyword, limit },
  })
}
