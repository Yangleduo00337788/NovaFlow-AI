<template>
  <div class="workflow-canvas-viewer" :class="{ compact }">
    <a-spin v-if="loading" class="canvas-loading" />
    <a-empty v-else-if="!nodes.length" class="canvas-empty" description="工作流暂无画布数据" />
    <VueFlow
      v-else
      :nodes="nodes"
      :edges="edges"
      :node-types="nodeTypes"
      :nodes-draggable="false"
      :nodes-connectable="false"
      :elements-selectable="false"
      :pan-on-drag="true"
      :zoom-on-scroll="true"
      :min-zoom="compact ? 0.15 : 0.35"
      :max-zoom="compact ? 1 : 1.5"
      fit-view-on-init
      :fit-view-on-init-options="fitViewOptions"
    >
      <Background :gap="16" />
      <WorkflowCanvasZoomControls v-if="compact" />
      <Controls v-else />
      <WorkflowCanvasFitHelper :compact="compact" :node-count="nodes.length" />
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { markRaw, ref, watch } from 'vue'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import type { NodeTypesObject } from '@vue-flow/core'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import WorkflowNode from '@/components/workflow/WorkflowNode.vue'
import WorkflowCanvasFitHelper from '@/components/workflow/WorkflowCanvasFitHelper.vue'
import WorkflowCanvasZoomControls from '@/components/workflow/WorkflowCanvasZoomControls.vue'
import { fetchWorkflow } from '@/api/workflow'
import { detailToFlow, mergeNodeRuntimeStatus, type WorkflowFlowEdge, type WorkflowFlowNode } from '@/utils/workflowCanvas'

const props = withDefaults(
  defineProps<{
    workflowId: number
    compact?: boolean
    nodeStatusMap?: Record<string, { status: number; statusLabel: string }>
  }>(),
  {
    compact: false,
    nodeStatusMap: () => ({}),
  },
)

const fitViewOptions = {
  padding: props.compact
    ? { top: 0.12, right: 0.1, bottom: 0.24, left: 0.14 }
    : 0.2,
  maxZoom: props.compact ? 0.85 : 1.2,
}

const loading = ref(false)
const nodes = ref<WorkflowFlowNode[]>([])
const edges = ref<WorkflowFlowEdge[]>([])

async function loadCanvas() {
  if (!props.workflowId) {
    nodes.value = []
    edges.value = []
    return
  }
  loading.value = true
  try {
    const res = await fetchWorkflow(props.workflowId)
    const flow = detailToFlow(res.data.data)
    nodes.value = mergeNodeRuntimeStatus(flow.nodes, props.nodeStatusMap || {})
    edges.value = flow.edges
  } catch {
    nodes.value = []
    edges.value = []
  } finally {
    loading.value = false
  }
}

const nodeTypes: NodeTypesObject = {
  start: markRaw(WorkflowNode),
  llm: markRaw(WorkflowNode),
  knowledge: markRaw(WorkflowNode),
  tool: markRaw(WorkflowNode),
  condition: markRaw(WorkflowNode),
  end: markRaw(WorkflowNode),
}

watch(
  () => props.workflowId,
  () => {
    loadCanvas()
  },
  { immediate: true },
)

watch(
  () => props.nodeStatusMap,
  () => {
    if (!nodes.value.length) return
    const flow = { nodes: nodes.value, edges: edges.value }
    nodes.value = mergeNodeRuntimeStatus(
      flow.nodes.map((node) => ({
        ...node,
        data: { ...node.data, status: undefined, statusLabel: undefined },
      })),
      props.nodeStatusMap || {},
    )
  },
  { deep: true },
)
</script>

<style scoped>
.workflow-canvas-viewer {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1 1 0;
  min-height: 0;
  max-height: 100%;
  box-sizing: border-box;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--border-color, var(--border));
  background: var(--workflow-grid, radial-gradient(circle at 1px 1px, #e2e8f0 1px, transparent 0)) 0 0 / 16px 16px;
}

.workflow-canvas-viewer :deep(.vue-flow) {
  width: 100%;
  height: 100%;
  min-height: 0;
  background: transparent;
  border-radius: inherit;
}

.workflow-canvas-viewer :deep(.vue-flow__viewport) {
  border-radius: inherit;
}

.workflow-canvas-viewer :deep(.vue-flow__handle) {
  width: 8px;
  height: 8px;
  background: #94a3b8;
  border: 2px solid #fff;
}

.workflow-canvas-viewer :deep(.vue-flow__edge-path) {
  stroke: #94a3b8;
  stroke-width: 2;
}

.workflow-canvas-viewer.compact :deep(.workflow-node) {
  min-width: 96px;
  padding: 6px 10px;
  font-size: 11px;
}

.workflow-canvas-viewer.compact :deep(.node-label) {
  font-size: 11px;
}

.workflow-canvas-viewer.compact :deep(.node-icon) {
  width: 20px;
  height: 20px;
  font-size: 10px;
}

.canvas-loading,
.canvas-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: inherit;
}
</style>
