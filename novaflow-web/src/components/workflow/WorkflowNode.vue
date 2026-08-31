<template>
  <div class="workflow-node" :class="[`type-${type}`, statusClass]">
    <Handle v-if="type !== 'start'" type="target" :position="Position.Left" />
    <div class="node-icon">{{ icon }}</div>
    <div class="node-body">
      <div class="node-label">{{ data.label || type }}</div>
      <div v-if="data.statusLabel" class="node-status">{{ data.statusLabel }}</div>
    </div>
    <template v-if="type === 'condition'">
      <Handle id="true" type="source" :position="Position.Right" class="handle-true" />
      <Handle id="false" type="source" :position="Position.Right" class="handle-false" />
    </template>
    <Handle v-else-if="type !== 'end'" type="source" :position="Position.Right" />
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
    agent: 'AG',
    condition: '?',
    end: '■',
  }
  return map[String(props.type)] || '•'
})

const statusClass = computed(() => {
  const status = Number(props.data?.status ?? -1)
  if (status === 0) return 'status-running'
  if (status === 1) return 'status-done'
  if (status === 2) return 'status-failed'
  return ''
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

.workflow-node.type-agent {
  border-color: #0ea5e9;
  background: #f0f9ff;
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

.node-body {
  min-width: 0;
}

.node-status {
  margin-top: 2px;
  font-size: 10px;
  color: #64748b;
}

.workflow-node.status-running {
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.25);
}

.workflow-node.status-done {
  border-color: #52c41a !important;
}

.workflow-node.status-failed {
  border-color: #ff4d4f !important;
  background: #fff1f0 !important;
}

.handle-true {
  top: 35%;
}

.handle-false {
  top: 65%;
}
</style>
