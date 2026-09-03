import axios from 'axios'
import type { ApiResult } from '@/types/dashboard'
import { useAuthStore } from '@/stores/auth'
import { APP_LOGIN_PATH } from '@/config/app'

const AUTH_ERROR_CODES = new Set([40101, 401])

function handleUnauthorized(message?: string) {
  const auth = useAuthStore()
  auth.clear()
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  const loginPath = APP_LOGIN_PATH
  window.location.href = `${loginPath}?redirect=${redirect}${message ? `&reason=${encodeURIComponent(message)}` : ''}`
}

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = auth.token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const result = response.data as ApiResult<unknown>
    if (result.code !== 0) {
      if (AUTH_ERROR_CODES.has(result.code)) {
        handleUnauthorized(result.message)
        return Promise.reject(new Error(result.message || '登录已过期'))
      }
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return response
  },
  (error) => {
    const status = error.response?.status
    const result = error.response?.data as ApiResult<unknown> | undefined
    if (status === 401 || (result && AUTH_ERROR_CODES.has(result.code))) {
      handleUnauthorized(result?.message)
      return Promise.reject(new Error(result?.message || '登录已过期'))
    }
    const message = result?.message || error.message || '请求失败'
    return Promise.reject(new Error(message))
  },
)

export default request
