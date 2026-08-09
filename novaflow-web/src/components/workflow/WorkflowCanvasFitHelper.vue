<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useVueFlow } from '@vue-flow/core'

const props = defineProps<{
  compact?: boolean
  nodeCount: number
}>()

const { fitView } = useVueFlow()

function applyFitView() {
  if (props.nodeCount <= 0) return
  fitView({
    padding: props.compact
      ? { top: 0.12, right: 0.1, bottom: 0.24, left: 0.14 }
      : 0.2,
    maxZoom: props.compact ? 0.85 : 1.2,
    duration: 200,
  })
}

onMounted(applyFitView)

watch(
  () => [props.compact, props.nodeCount] as const,
  () => {
    applyFitView()
  },
)
</script>

<template />
