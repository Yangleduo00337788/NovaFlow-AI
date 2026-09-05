import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { resolveAppScope } from '@/config/theme-colors'
import { useThemeStore } from '@/stores/theme'

/** 根据当前路由同步租户/平台视觉域（data-scope + Ant Design 主色） */
export function useAppScopeSync() {
  const route = useRoute()
  const themeStore = useThemeStore()

  watch(
    () => route.path,
    (path) => {
      themeStore.setScope(resolveAppScope(path))
    },
    { immediate: true },
  )
}
