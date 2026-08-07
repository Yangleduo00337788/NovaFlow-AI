<template>
  <a-button
    type="text"
    class="icon-btn theme-toggle"
    :title="title"
    :aria-label="title"
    @click="themeStore.toggle()"
  >
    <!-- 暗色模式：月亮（点击切亮色） -->
    <svg
      v-if="themeStore.isDark"
      class="theme-icon"
      viewBox="0 0 24 24"
      width="16"
      height="16"
      aria-hidden="true"
    >
      <path
        d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
    <!-- 亮色模式：太阳（点击切暗色） -->
    <svg
      v-else
      class="theme-icon"
      viewBox="0 0 24 24"
      width="16"
      height="16"
      aria-hidden="true"
    >
      <circle
        cx="12"
        cy="12"
        r="4"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
      />
      <path
        d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
      />
    </svg>
  </a-button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const title = computed(() => (themeStore.isDark ? '切换为亮色模式' : '切换为暗色模式'))
</script>

<style scoped>
.theme-toggle {
  width: 32px;
  height: 32px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--icon-color) !important;
  flex-shrink: 0;
}

.theme-toggle:hover {
  color: var(--icon-hover-color) !important;
  background: var(--hover-bg) !important;
}

.theme-icon {
  display: block;
  flex-shrink: 0;
  color: inherit;
}
</style>
