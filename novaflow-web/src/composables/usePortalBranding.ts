import { onMounted, ref } from 'vue'
import { fetchPortalBranding, type PortalBranding } from '@/api/portal'

function applyPortalTheme(color?: string) {
  const root = document.documentElement
  if (!color) {
    root.style.removeProperty('--portal-primary')
    root.style.removeProperty('--primary')
    return
  }
  root.style.setProperty('--portal-primary', color)
  root.style.setProperty('--primary', color)
}

export function usePortalBranding() {
  const branding = ref<PortalBranding | null>(null)
  const loading = ref(false)

  async function loadBranding() {
    loading.value = true
    try {
      const res = await fetchPortalBranding()
      branding.value = res.data.data || null
      applyPortalTheme(branding.value?.portalThemeColor)
    } catch {
      branding.value = null
      applyPortalTheme()
    } finally {
      loading.value = false
    }
  }

  onMounted(loadBranding)

  return {
    branding,
    loading,
    loadBranding,
  }
}
