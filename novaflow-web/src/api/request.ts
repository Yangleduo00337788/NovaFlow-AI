import axios from 'axios'
import type { ApiResult } from '@/types/dashboard'
import { useAuthStore } from '@/stores/auth'

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
    const result = response.data as ApiResult<unknown>
    if (result.code !== 0) {
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      const auth = useAuthStore()
      auth.clear()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export default request
