import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { AppScope } from '@/config/theme-colors'
import { themeTokensForScope } from '@/config/theme-colors'

export type ThemeMode = 'light' | 'dark'

const THEME_KEY = 'novaflow_theme'

function getSystemTheme(): ThemeMode {
  if (typeof window === 'undefined') return 'light'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function readStoredTheme(): ThemeMode {
  const stored = localStorage.getItem(THEME_KEY)
  if (stored === 'light' || stored === 'dark') return stored
  return getSystemTheme()
}

export function applyTheme(mode: ThemeMode) {
  document.documentElement.setAttribute('data-theme', mode)
  document.documentElement.style.colorScheme = mode
}

export function applyScope(scope: AppScope) {
  document.documentElement.setAttribute('data-scope', scope)
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(readStoredTheme())
  const scope = ref<AppScope>('tenant')

  const isDark = computed(() => mode.value === 'dark')
  const siderTheme = computed(() => (isDark.value ? 'dark' : 'light'))
  const tokens = computed(() => themeTokensForScope(scope.value))

  function setTheme(next: ThemeMode) {
    mode.value = next
    localStorage.setItem(THEME_KEY, next)
    applyTheme(next)
  }

  function setScope(next: AppScope) {
    scope.value = next
    applyScope(next)
  }

  function toggle() {
    setTheme(isDark.value ? 'light' : 'dark')
  }

  return { mode, scope, isDark, siderTheme, tokens, setTheme, setScope, toggle }
})
