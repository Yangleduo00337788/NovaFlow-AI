<template>
  <div class="dashboard-workflow-canvas">
    <div class="canvas-header">
      <router-link :to="runtime.path" class="workflow-name">{{ runtime.workflowName }}</router-link>
      <a-tag v-if="runtime.statusLabel" color="success" class="runtime-tag">
        {{ runtime.statusLabel }}
      </a-tag>
    </div>
    <WorkflowCanvasViewer :workflow-id="runtime.workflowId" compact class="canvas-body" />
  </div>
</template>

<script setup lang="ts">
import WorkflowCanvasViewer from '@/components/workflow/WorkflowCanvasViewer.vue'
import type { WorkflowRuntime } from '@/types/dashboard'

defineProps<{
  runtime: WorkflowRuntime
}>()
</script>

<style scoped>
.dashboard-workflow-canvas {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1 1 0;
  min-height: 0;
}

.canvas-body {
  flex: 1 1 0;
  min-height: 0;
  max-height: 100%;
}

.canvas-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-shrink: 0;
}

.workflow-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.workflow-name:hover {
  color: #1677ff;
}

.runtime-tag {
  margin: 0;
  font-size: 11px;
}
</style>
