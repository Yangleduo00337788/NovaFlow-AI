<template>
  <div class="process-stack">
    <AgentTechLoader size="xs" class="process-stack__icon" :paused="!loading" aria-hidden="true" />
    <div class="process-stack__body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import AgentTechLoader from '@/components/agent/AgentTechLoader.vue'

withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  {
    loading: false,
  },
)
</script>

<style scoped>
.process-stack {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  margin-bottom: 8px;
}

.process-stack__icon {
  flex-shrink: 0;
  margin-top: 4px;
}

.process-stack__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
</style>
