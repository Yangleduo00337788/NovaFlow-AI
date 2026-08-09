<template>
  <div class="workflow-node" :class="`type-${type}`">
    <Handle v-if="type !== 'start'" type="target" :position="Position.Left" />
    <div class="node-icon">{{ icon }}</div>
    <div class="node-label">{{ data.label || type }}</div>
    <Handle v-if="type !== 'end'" type="source" :position="Position.Right" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position, type NodeProps } from '@vue-flow/core'

const props = defineProps<NodeProps>()

const icon = computed(() => {
  const map: Record<string, string> = {
    start: '▶',
    llm: 'AI',
    knowledge: 'KB',
    tool: 'T',
    condition: '?',
    end: '■',
  }
  return map[String(props.type)] || '•'
})
</script>

<style scoped>
.workflow-node {
  min-width: 120px;
  padding: 10px 14px;
  border-radius: 10px;
  border: 2px solid #d1d5db;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.workflow-node.type-start {
  border-color: #22c55e;
  background: #f0fdf4;
}

.workflow-node.type-llm {
  border-color: #6366f1;
  background: #eef2ff;
}

.workflow-node.type-knowledge {
  border-color: #22c55e;
  background: #f0fdf4;
}

.workflow-node.type-tool {
  border-color: #ef4444;
  background: #fef2f2;
}

.workflow-node.type-condition {
  border-color: #f59e0b;
  background: #fffbeb;
}

.workflow-node.type-end {
  border-color: #ef4444;
  background: #fef2f2;
}

.node-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: rgba(99, 102, 241, 0.12);
  color: #4f46e5;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
}

.node-label {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}
</style>
