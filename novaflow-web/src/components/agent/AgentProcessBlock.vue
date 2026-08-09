<template>
  <div class="process-block" :class="{ 'process-block--sub': hideIcon }">
    <button
      type="button"
      class="process-block__toggle"
      :class="{ 'process-block__toggle--sub': hideIcon }"
      :disabled="!hasPanel"
      @click="toggleExpanded"
    >
      <AgentTechLoader v-if="!hideIcon" size="xs" :paused="!loading" aria-hidden="true" />
      <span class="process-block__title">{{ title }}</span>
      <DownOutlined
        v-if="hasPanel"
        class="process-block__chevron"
        :class="{ 'is-collapsed': !expanded }"
      />
    </button>
    <div v-if="hasPanel && expanded" class="process-block__panel">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, useSlots, watch } from 'vue'
import { DownOutlined } from '@ant-design/icons-vue'
import AgentTechLoader from '@/components/agent/AgentTechLoader.vue'

const props = withDefaults(
  defineProps<{
    title: string
    loading?: boolean
    expanded?: boolean
    defaultExpanded?: boolean
    collapsible?: boolean
    hideIcon?: boolean
  }>(),
  {
    loading: false,
    defaultExpanded: true,
    collapsible: true,
    hideIcon: false,
  },
)

const emit = defineEmits<{
  'update:expanded': [value: boolean]
}>()

const slots = useSlots()
const hasPanel = computed(() => props.collapsible && Boolean(slots.default))
const expanded = ref(props.expanded ?? props.defaultExpanded)

watch(
  () => props.expanded,
  (value) => {
    if (value !== undefined) {
      expanded.value = value
    }
  },
)

function toggleExpanded() {
  if (!hasPanel.value) {
    return
  }
  const next = !expanded.value
  expanded.value = next
  emit('update:expanded', next)
}
</script>

<style scoped>
.process-block {
  width: 100%;
  margin-bottom: 8px;
}

.process-block--sub {
  margin-bottom: 0;
}

.process-block--sub .process-block__panel {
  padding-left: 0;
}

.process-block__toggle--sub {
  padding: 2px 0;
}

.process-block__toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 4px 0;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.process-block__toggle:disabled {
  cursor: default;
}

.process-block__toggle:hover:not(:disabled) .process-block__title {
  color: #475569;
}

.process-block__title {
  flex: 1;
  font-size: 13px;
  color: #64748b;
  transition: color 0.15s ease;
}

.process-block__chevron {
  font-size: 10px;
  color: #94a3b8;
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.process-block__chevron.is-collapsed {
  transform: rotate(-90deg);
}

.process-block__panel {
  max-height: 280px;
  overflow-y: auto;
  padding: 4px 0 8px 28px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.process-block__panel::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.process-block__panel :deep(.process-block__text) {
  white-space: pre-wrap;
  line-height: 1.65;
  font-size: 13px;
  color: #94a3b8;
}

.process-block__panel :deep(.process-block__text--muted) {
  color: #cbd5e1;
}
</style>
