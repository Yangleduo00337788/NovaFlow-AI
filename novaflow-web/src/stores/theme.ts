import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

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

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(readStoredTheme())

  const isDark = computed(() => mode.value === 'dark')
  const siderTheme = computed(() => (isDark.value ? 'dark' : 'light'))

  function setTheme(next: ThemeMode) {
    mode.value = next
    localStorage.setItem(THEME_KEY, next)
    applyTheme(next)
  }

  function toggle() {
    setTheme(isDark.value ? 'light' : 'dark')
  }

  return { mode, isDark, siderTheme, setTheme, toggle }
})
