<template>
  <div class="trace-waterfall">
    <div class="waterfall-axis">
      <span>0</span>
      <span>{{ formatAxis(totalMs * 0.25) }}</span>
      <span>{{ formatAxis(totalMs * 0.5) }}</span>
      <span>{{ formatAxis(totalMs * 0.75) }}</span>
      <span>{{ formatAxis(totalMs) }}</span>
    </div>
    <div v-for="node in nodes" :key="node.nodeId" class="waterfall-row">
      <div class="row-label">
        <div class="row-name">{{ node.nodeName }}</div>
        <div class="row-type">{{ node.nodeType }}</div>
      </div>
      <div class="row-track">
        <div
          class="row-bar"
          :class="barClass(node.status)"
          :style="barStyle(node)"
          :title="`${node.statusLabel} · ${node.durationLabel || '-'}`"
        />
      </div>
      <div class="row-meta">
        <a-tag size="small" :color="tagColor(node.status)">{{ node.statusLabel }}</a-tag>
        <span>{{ node.durationLabel || '-' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TraceNode } from '@/api/trace'

const props = defineProps<{
  nodes: TraceNode[]
  totalDurationMs?: number
}>()

const totalMs = computed(() => {
  if (props.totalDurationMs && props.totalDurationMs > 0) {
    return props.totalDurationMs
  }
  let max = 1
  for (const node of props.nodes) {
    const end = (node.offsetMs || 0) + (node.durationMs || 0)
    if (end > max) max = end
  }
  return max
})

function barStyle(node: TraceNode) {
  const left = ((node.offsetMs || 0) / totalMs.value) * 100
  const width = Math.max(((node.durationMs || 0) / totalMs.value) * 100, 1.5)
  return {
    left: `${left}%`,
    width: `${width}%`,
  }
}

function barClass(status?: number) {
  if (status === 0) return 'running'
  if (status === 2) return 'failed'
  if (status === 1) return 'success'
  return 'default'
}

function tagColor(status?: number) {
  if (status === 0) return 'processing'
  if (status === 2) return 'error'
  if (status === 1) return 'success'
  return 'default'
}

function formatAxis(value: number) {
  if (value < 1000) return `${Math.round(value)}ms`
  return `${(value / 1000).toFixed(1)}s`
}
</script>

<style scoped>
.trace-waterfall {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.waterfall-axis {
  display: grid;
  grid-template-columns: 140px 1fr 88px;
  gap: 10px;
  padding-left: 150px;
  padding-right: 98px;
  font-size: 11px;
  color: var(--text-muted);
}

.waterfall-axis span {
  text-align: center;
}

.waterfall-axis span:first-child {
  text-align: left;
}

.waterfall-axis span:last-child {
  text-align: right;
}

.waterfall-row {
  display: grid;
  grid-template-columns: 140px 1fr 88px;
  gap: 10px;
  align-items: center;
}

.row-label {
  min-width: 0;
}

.row-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.row-type {
  font-size: 11px;
  color: var(--text-secondary);
}

.row-track {
  position: relative;
  height: 24px;
  border-radius: 6px;
  background: rgba(148, 163, 184, 0.12);
  overflow: hidden;
}

.row-bar {
  position: absolute;
  top: 4px;
  bottom: 4px;
  border-radius: 4px;
  min-width: 4px;
}

.row-bar.success {
  background: linear-gradient(90deg, #22c55e, #4ade80);
}

.row-bar.running {
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
}

.row-bar.failed {
  background: linear-gradient(90deg, #ef4444, #f87171);
}

.row-bar.default {
  background: linear-gradient(90deg, #94a3b8, #cbd5e1);
}

.row-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  font-size: 11px;
  color: var(--text-secondary);
}
</style>
