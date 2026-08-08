<template>
  <div class="provider-icon" :class="[code, size]" :style="colorStyle">
    <img v-if="iconDef.type === 'url'" :src="iconDef.src" :alt="code" class="provider-icon-img" />
    <span v-else class="provider-icon-svg" v-html="iconDef.svg" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getProviderIconDef } from '@/constants/providerIcons'

const props = withDefaults(defineProps<{
  code: string
  size?: 'md' | 'sm'
}>(), {
  size: 'md',
})

const iconDef = computed(() => getProviderIconDef(props.code))

const colorStyle = computed(() => {
  if (iconDef.value.type === 'raw' && iconDef.value.color) {
    return { color: iconDef.value.color }
  }
  return undefined
})
</script>

<style scoped>
.provider-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  padding: 8px;
  box-sizing: border-box;
}

.provider-icon.sm {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  padding: 4px;
}

.provider-icon-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.provider-icon-svg {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.provider-icon-svg :deep(svg) {
  width: 100%;
  height: 100%;
}
</style>
