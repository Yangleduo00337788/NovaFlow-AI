import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginResponse } from '@/api/auth'

const TOKEN_KEY = 'novaflow_token'
const USER_KEY = 'novaflow_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<LoginResponse['user'] | null>(
    localStorage.getItem(USER_KEY) ? JSON.parse(localStorage.getItem(USER_KEY)!) : null,
  )
  const tenant = ref<LoginResponse['tenant'] | null>(null)

  function setAuth(data: LoginResponse) {
    token.value = data.token
    user.value = data.user
    tenant.value = data.tenant
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(data.user))
  }

  function clear() {
    token.value = ''
    user.value = null
    tenant.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  const isLoggedIn = () => !!token.value

  return { token, user, tenant, setAuth, clear, isLoggedIn }
})
