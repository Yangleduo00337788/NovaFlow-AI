import axios from 'axios'
import type { ApiResult } from '@/types/dashboard'

export interface PublicPlatformStatus {
  maintenanceEnabled: boolean
  maintenanceMessage?: string
  platformAnnouncement?: string
}

const publicClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

export function fetchPublicPlatformStatus() {
  return publicClient.get<ApiResult<PublicPlatformStatus>>('/v1/public/platform-status')
}
