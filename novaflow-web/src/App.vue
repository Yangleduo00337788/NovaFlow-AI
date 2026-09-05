<template>
  <a-config-provider :theme="antdTheme">
    <router-view />
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { theme } from 'ant-design-vue'
import { useAppScopeSync } from '@/composables/useAppScope'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
useAppScopeSync()

const antdTheme = computed(() => ({
  algorithm: themeStore.isDark ? theme.darkAlgorithm : theme.defaultAlgorithm,
  token: {
    colorPrimary: themeStore.tokens.colorPrimary,
    borderRadius: 8,
  },
}))
</script>
