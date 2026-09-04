import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { LoginResponse } from '@/api/auth'

const TOKEN_KEY = 'novaflow_token'
const USER_KEY = 'novaflow_user'
const PERMISSIONS_KEY = 'novaflow_permissions'
const LEGACY_TERMINAL_KEY = 'novaflow_login_terminal'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<LoginResponse['user'] | null>(
    localStorage.getItem(USER_KEY) ? JSON.parse(localStorage.getItem(USER_KEY)!) : null,
  )
  const tenant = ref<LoginResponse['tenant'] | null>(null)
  const permissions = ref<string[]>(
    localStorage.getItem(PERMISSIONS_KEY) ? JSON.parse(localStorage.getItem(PERMISSIONS_KEY)!) : [],
  )

  function setAuth(data: LoginResponse) {
    token.value = data.token
    user.value = data.user
    tenant.value = data.tenant
    permissions.value = data.permissions || []
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(data.user))
    localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(permissions.value))
    localStorage.removeItem(LEGACY_TERMINAL_KEY)
  }

  function clear() {
    token.value = ''
    user.value = null
    tenant.value = null
    permissions.value = []
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(PERMISSIONS_KEY)
    localStorage.removeItem(LEGACY_TERMINAL_KEY)
  }

  const isLoggedIn = () => !!token.value

  const roleCode = computed(() => user.value?.roleCode || '')

  function hasPermission(...codes: string[]) {
    if (codes.length === 0) {
      return true
    }
    return codes.some((code) => permissions.value.includes(code))
  }

  function hasAnyPermission(codes?: string[]) {
    if (!codes || codes.length === 0) {
      return true
    }
    return hasPermission(...codes)
  }

  return {
    token,
    user,
    tenant,
    permissions,
    roleCode,
    setAuth,
    clear,
    isLoggedIn,
    hasPermission,
    hasAnyPermission,
  }
})
