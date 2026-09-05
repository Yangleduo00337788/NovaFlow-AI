export type AppScope = 'tenant' | 'platform'

export const TENANT_THEME = {
  colorPrimary: '#1677ff',
  colorPrimaryHover: '#4096ff',
  colorPrimaryActive: '#0958d9',
} as const

export const PLATFORM_THEME = {
  colorPrimary: '#7c3aed',
  colorPrimaryHover: '#8b5cf6',
  colorPrimaryActive: '#6d28d9',
} as const

export function resolveAppScope(path: string): AppScope {
  return path.startsWith('/platform') ? 'platform' : 'tenant'
}

export function themeTokensForScope(scope: AppScope) {
  return scope === 'platform' ? PLATFORM_THEME : TENANT_THEME
}
