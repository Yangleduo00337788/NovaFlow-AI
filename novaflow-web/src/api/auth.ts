import request from './request'
import type { ApiResult } from '@/types/dashboard'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  confirmPassword: string
  nickname?: string
  companyName: string
  /** personal（个人版）或 enterprise（企业版，注册即免费试用档），默认 enterprise */
  planType?: 'personal' | 'enterprise'
}

export interface LoginResponse {
  token: string
  user: {
    id: number
    username: string
    nickname: string
    email: string
    roleCode: string
    roleName: string
  }
  tenant: {
    id: number
    tenantName: string
    planType: string
  }
  permissions?: string[]
}

export function register(data: RegisterRequest) {
  return request.post<ApiResult<LoginResponse>>('/v1/auth/register', data)
}

export function login(data: LoginRequest) {
  return request.post<ApiResult<LoginResponse>>('/v1/auth/login', data)
}

export function fetchCurrentUser() {
  return request.get<ApiResult<LoginResponse>>('/v1/auth/me')
}

export function logout() {
  return request.post<ApiResult<void>>('/v1/auth/logout')
}
