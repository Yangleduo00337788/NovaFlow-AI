<template>
  <span class="document-file-icon" :style="{ color: meta.color, background: meta.background }" :title="meta.label">
    <component :is="meta.icon" />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getDocumentIconMeta } from '@/utils/documentIcon'

const props = defineProps<{
  docType?: string
  fileName?: string
}>()

const meta = computed(() => getDocumentIconMeta(props.docType, props.fileName))
</script>

<style scoped>
.document-file-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  font-size: 16px;
  flex-shrink: 0;
}
</style>
