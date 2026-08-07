<template>
  <div v-if="variant === 'sidebar'" class="brand-mark" :class="{ collapsed }">
    <span class="brand-icon-wrap">
      <img :src="LOGO_ICON_SRC" alt="" class="brand-icon" aria-hidden="true" />
    </span>
    <span v-if="!collapsed" class="brand-text">{{ LOGO_ALT }}</span>
  </div>
  <img
    v-else
    :src="LOGO_SRC"
    :alt="LOGO_ALT"
    class="app-logo"
    :class="`app-logo--${variant}`"
  />
</template>

<script setup lang="ts">
import { LOGO_ALT, LOGO_ICON_SRC, LOGO_SRC } from '@/constants/brand'

withDefaults(defineProps<{
  variant?: 'sidebar' | 'login' | 'icon'
  collapsed?: boolean
}>(), {
  variant: 'sidebar',
  collapsed: false,
})
</script>

<style scoped>
.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 32px;
  max-width: 100%;
  margin: 0 auto;
}

.brand-mark.collapsed {
  justify-content: center;
}

.brand-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  background: transparent;
}

.brand-icon {
  display: block;
  width: 32px;
  height: 32px;
  object-fit: contain;
  object-position: center;
  background: transparent;
}

.brand-text {
  display: flex;
  align-items: center;
  height: 32px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
  white-space: nowrap;
  transform: translateY(3px);
}

.app-logo {
  display: block;
  object-fit: contain;
}

.app-logo--login {
  width: min(280px, 100%);
  height: auto;
  margin-bottom: 32px;
}

.app-logo--icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
}
</style>
