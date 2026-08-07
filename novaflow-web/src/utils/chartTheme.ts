import type { ThemeMode } from '@/stores/theme'

export function getChartTheme(mode: ThemeMode) {
  const isDark = mode === 'dark'
  return {
    axisLine: isDark ? '#334155' : '#e2e8f0',
    splitLine: isDark ? '#1e293b' : '#f1f5f9',
    axisLabel: isDark ? '#64748b' : '#94a3b8',
    pieBorder: isDark ? '#1a2332' : '#ffffff',
    tooltipBg: isDark ? '#1e293b' : '#ffffff',
    tooltipText: isDark ? '#e2e8f0' : '#334155',
  }
}
