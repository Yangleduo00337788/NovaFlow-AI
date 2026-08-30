<template>
  <Panel class="zoom-panel" position="bottom-left">
    <div class="zoom-controls">
      <button type="button" class="zoom-btn" title="放大" @click="() => zoomIn()">+</button>
      <button type="button" class="zoom-btn" title="缩小" @click="() => zoomOut()">−</button>
    </div>
  </Panel>
</template>

<script setup lang="ts">
import { Panel, useVueFlow } from '@vue-flow/core'

const { zoomIn, zoomOut } = useVueFlow()
</script>

<style scoped>
.zoom-panel {
  margin: 0;
}

.zoom-controls {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: var(--card-bg, #fff);
  border: 1px solid var(--border-strong, #e2e8f0);
  border-radius: 8px;
  padding: 3px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
}

.zoom-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary, #64748b);
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.zoom-btn:hover {
  background: var(--bg-muted, #f1f5f9);
  color: #1677ff;
}
</style>
