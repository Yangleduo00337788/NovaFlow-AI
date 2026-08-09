<template>
  <div class="workflow-editor" data-testid="workflow-editor">
    <div class="editor-toolbar page-card">
      <div class="toolbar-left">
        <a-button type="text" @click="goBack">
          <ArrowLeftOutlined />
        </a-button>
        <div>
          <h2>{{ detail.workflowName || '工作流编辑' }}</h2>
          <p>{{ detail.applicationName || '加载中...' }}</p>
        </div>
        <a-tag :color="getWorkflowStatusColor(detail.status)">{{ getWorkflowStatusLabel(detail.status) }}</a-tag>
      </div>
      <a-space>
        <a-button :loading="saving" @click="saveWorkflow">
          <SaveOutlined />
          保存
        </a-button>
        <a-button :loading="publishing" @click="publishWorkflowAction">
          <CloudUploadOutlined />
          发布
        </a-button>
        <a-button type="primary" :loading="running" @click="openRun">
          <PlayCircleOutlined />
          试运行
        </a-button>
      </a-space>
    </div>

    <div class="editor-body">
      <div class="palette page-card">
        <div class="panel-title">节点面板</div>
        <div class="palette-list">
          <button
            v-for="item in palette"
            :key="item.type"
            class="palette-item"
            @click="addNode(item.type, item.label)"
          >
            <span class="palette-icon" :class="item.type">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </div>

      <div class="canvas-wrap page-card">
        <VueFlow
          v-model:nodes="nodes"
          v-model:edges="edges"
          :node-types="nodeTypes"
          fit-view-on-init
          @node-click="onNodeClick"
          @pane-click="selectedNodeId = null"
          @connect="onConnect"
          @nodes-change="onNodesChange"
          @edges-change="onEdgesChange"
        >
          <Background />
          <Controls />
        </VueFlow>
      </div>

      <div class="config-panel page-card">
        <div class="panel-title">节点配置</div>
        <template v-if="selectedNode">
          <a-form layout="vertical">
            <a-form-item label="节点名称">
              <a-input v-model:value="selectedNode.data.label" />
            </a-form-item>
            <template v-if="selectedNode.type === 'llm'">
              <a-form-item label="模型">
                <a-select
                  v-model:value="llmModelConfigId"
                  placeholder="选择模型"
                  :options="modelOptions"
                  :loading="modelsLoading"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="Prompt">
                <a-textarea
                  v-model:value="llmPrompt"
                  :rows="6"
                  placeholder="使用 {{input}} 引用试运行输入"
                />
              </a-form-item>
            </template>
            <template v-else-if="selectedNode.type === 'condition'">
              <a-form-item label="条件类型">
                <a-select v-model:value="conditionExpression" :options="conditionOptions" />
              </a-form-item>
              <a-alert
                type="info"
                show-icon
                message="条件分支说明"
                description="节点输出 true/false。从该节点连出的第一条线为 true 分支，第二条为 false 分支。"
              />
            </template>
            <template v-else-if="selectedNode.type === 'tool'">
              <a-form-item label="工具">
                <a-select
                  v-model:value="toolId"
                  placeholder="选择 HTTP / MCP 插件工具"
                  :options="toolOptions"
                  :loading="toolsLoading"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="固定参数（JSON，可选）">
                <a-textarea
                  v-model:value="toolArgumentsJson"
                  :rows="4"
                  placeholder='{"message":"hello"}；留空则使用上游节点输出作为 input/query'
                />
              </a-form-item>
              <a-alert
                type="info"
                show-icon
                message="MCP 工具说明"
                description="上游输出为 JSON 对象时会自动解析为工具参数；也可在此填写固定参数，与上游输出合并（上游优先）。"
              />
            </template>
            <template v-else-if="selectedNode.type === 'knowledge'">
              <a-form-item label="知识库">
                <a-select
                  v-model:value="knowledgeBaseId"
                  placeholder="选择知识库"
                  :options="knowledgeOptions"
                  :loading="knowledgeLoading"
                  allow-clear
                />
              </a-form-item>
              <a-form-item label="Top-K">
                <a-input-number v-model:value="knowledgeTopK" :min="1" :max="20" style="width: 100%" />
              </a-form-item>
            </template>
            <template v-else>
              <a-empty description="该节点无需额外配置" />
            </template>
          </a-form>
        </template>
        <a-empty v-else description="点击画布中的节点进行配置" />
      </div>
    </div>

    <a-drawer v-model:open="runOpen" title="试运行" :width="520">
      <a-form layout="vertical">
        <a-form-item label="输入内容">
          <a-textarea v-model:value="runInput" :rows="4" placeholder="输入测试内容" />
        </a-form-item>
        <a-button type="primary" :loading="running" block @click="runWorkflowAction">开始运行</a-button>
      </a-form>
      <div v-if="runResult" class="run-result">
        <a-alert
          :type="runResult.status === 1 ? 'success' : 'error'"
          :message="runResult.status === 1 ? '运行成功' : '运行失败'"
          :description="runResult.errorMessage || runResult.output"
          show-icon
        />
        <div class="run-steps">
          <div v-for="step in runResult.steps" :key="step.nodeId" class="run-step">
            <div class="step-head">
              <strong>{{ step.nodeName }}</strong>
              <a-tag :color="step.status === 1 ? 'success' : 'error'">{{ step.nodeType }}</a-tag>
            </div>
            <div v-if="step.output" class="step-output">{{ step.output }}</div>
            <div v-if="step.errorMessage" class="step-error">{{ step.errorMessage }}</div>
          </div>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  CloudUploadOutlined,
  PlayCircleOutlined,
  SaveOutlined,
} from '@ant-design/icons-vue'
import { VueFlow, addEdge, applyEdgeChanges, applyNodeChanges } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import type { Connection, EdgeChange, NodeChange, NodeTypesObject } from '@vue-flow/core'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import WorkflowNode from '@/components/workflow/WorkflowNode.vue'
import { fetchModelConfigs } from '@/api/model'
import { fetchKnowledgeBases } from '@/api/knowledge'
import { fetchToolOptions } from '@/api/tool'
import {
  fetchWorkflow,
  publishWorkflow,
  runWorkflow,
  updateWorkflow,
} from '@/api/workflow'
import type { WorkflowDetail, WorkflowRunResult } from '@/types/workflow'
import { getWorkflowStatusColor, getWorkflowStatusLabel } from '@/types/workflow'

const route = useRoute()
const router = useRouter()
const workflowId = Number(route.params.id)

const nodeTypes: NodeTypesObject = {
  start: markRaw(WorkflowNode),
  llm: markRaw(WorkflowNode),
  knowledge: markRaw(WorkflowNode),
  tool: markRaw(WorkflowNode),
  condition: markRaw(WorkflowNode),
  end: markRaw(WorkflowNode),
}
const palette = [
  { type: 'start', label: '开始', icon: '▶' },
  { type: 'llm', label: 'LLM', icon: 'AI' },
  { type: 'knowledge', label: '知识库', icon: 'KB' },
  { type: 'tool', label: '工具', icon: 'T' },
  { type: 'condition', label: '条件分支', icon: '?' },
  { type: 'end', label: '结束', icon: '■' },
]
const conditionOptions = [
  { value: 'not_empty', label: '输入非空' },
  { value: 'contains:success', label: '包含 success' },
]

const detail = ref<WorkflowDetail>({ id: workflowId, applicationId: 0, workflowName: '' })
interface WorkflowFlowNode {
  id: string
  type: string
  position: { x: number; y: number }
  data: { label: string; config?: Record<string, unknown> }
}

interface WorkflowFlowEdge {
  id: string
  source: string
  target: string
  sourceHandle?: string
  targetHandle?: string
  label?: string
}

const nodes = ref<WorkflowFlowNode[]>([])
const edges = ref<WorkflowFlowEdge[]>([])
const selectedNodeId = ref<string | null>(null)
const saving = ref(false)
const publishing = ref(false)
const running = ref(false)
const modelsLoading = ref(false)
const toolsLoading = ref(false)
const knowledgeLoading = ref(false)
const modelOptions = ref<Array<{ label: string; value: number }>>([])
const toolOptions = ref<Array<{ label: string; value: number }>>([])
const knowledgeOptions = ref<Array<{ label: string; value: number }>>([])
const runOpen = ref(false)
const runInput = ref('你好，请帮我总结这段话。')
const runResult = ref<WorkflowRunResult | null>(null)

const selectedNode = computed(() => nodes.value.find((node) => node.id === selectedNodeId.value))

const llmModelConfigId = computed({
  get: () => (selectedNode.value?.data?.config?.modelConfigId as number | undefined) ?? undefined,
  set: (value) => {
    if (!selectedNode.value) return
    selectedNode.value.data.config = { ...(selectedNode.value.data.config || {}), modelConfigId: value }
  },
})

const llmPrompt = computed({
  get: () => (selectedNode.value?.data?.config?.prompt as string | undefined) ?? '',
  set: (value) => {
    if (!selectedNode.value) return
    selectedNode.value.data.config = { ...(selectedNode.value.data.config || {}), prompt: value }
  },
})

const conditionExpression = computed({
  get: () => (selectedNode.value?.data?.config?.expression as string | undefined) ?? 'not_empty',
  set: (value) => {
    if (!selectedNode.value) return
    selectedNode.value.data.config = { ...(selectedNode.value.data.config || {}), expression: value }
  },
})

const toolId = computed({
  get: () => (selectedNode.value?.data?.config?.toolId as number | undefined) ?? undefined,
  set: (value) => {
    if (!selectedNode.value) return
    selectedNode.value.data.config = { ...(selectedNode.value.data.config || {}), toolId: value }
  },
})

const toolArgumentsJson = computed({
  get: () => {
    const value = selectedNode.value?.data?.config?.arguments
    if (value == null) return ''
    if (typeof value === 'string') return value
    try {
      return JSON.stringify(value, null, 2)
    } catch {
      return ''
    }
  },
  set: (value: string) => {
    if (!selectedNode.value) return
    const trimmed = value?.trim() ?? ''
    const config = { ...(selectedNode.value.data.config || {}) }
    if (!trimmed) {
      delete config.arguments
    } else {
      config.arguments = trimmed
    }
    selectedNode.value.data.config = config
  },
})

const knowledgeBaseId = computed({
  get: () => (selectedNode.value?.data?.config?.knowledgeBaseId as number | undefined) ?? undefined,
  set: (value) => {
    if (!selectedNode.value) return
    selectedNode.value.data.config = { ...(selectedNode.value.data.config || {}), knowledgeBaseId: value }
  },
})

const knowledgeTopK = computed({
  get: () => (selectedNode.value?.data?.config?.topK as number | undefined) ?? 5,
  set: (value) => {
    if (!selectedNode.value) return
    selectedNode.value.data.config = { ...(selectedNode.value.data.config || {}), topK: value }
  },
})

function goBack() {
  router.push('/workflow')
}

function onNodeClick({ node }: { node: WorkflowFlowNode }) {
  selectedNodeId.value = node.id
}

function onConnect(connection: Connection) {
  edges.value = addEdge(
    {
      ...connection,
      id: `e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    },
    edges.value as Parameters<typeof addEdge>[1],
  ) as WorkflowFlowEdge[]
}

function onNodesChange(changes: NodeChange[]) {
  nodes.value = applyNodeChanges(changes, nodes.value as Parameters<typeof applyNodeChanges>[1]) as WorkflowFlowNode[]
}

function onEdgesChange(changes: EdgeChange[]) {
  edges.value = applyEdgeChanges(changes, edges.value as Parameters<typeof applyEdgeChanges>[1]) as WorkflowFlowEdge[]
}

function addNode(type: string, label: string) {
  const id = `${type}-${Date.now()}`
  nodes.value.push({
    id,
    type,
    position: { x: 120 + nodes.value.length * 40, y: 120 + nodes.value.length * 20 },
    data: {
      label,
      config: type === 'llm' ? { prompt: '请处理以下输入：{{input}}' } : type === 'condition' ? { expression: 'not_empty' } : {},
    },
  })
}

function buildCanvasData() {
  return {
    nodes: nodes.value.map((node) => ({
      id: node.id,
      type: String(node.type),
      position: { x: node.position.x, y: node.position.y },
      data: {
        label: node.data?.label || String(node.type),
        config: node.data?.config || {},
      },
    })),
    edges: edges.value.map((edge) => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: edge.sourceHandle || undefined,
      targetHandle: edge.targetHandle || undefined,
      label: typeof edge.label === 'string' ? edge.label : undefined,
    })),
  }
}

function applyDetail(data: WorkflowDetail) {
  detail.value = data
  const canvas = data.canvasData
  if (canvas?.nodes?.length) {
    nodes.value = canvas.nodes.map((node) => ({
      id: node.id,
      type: node.type,
      position: { x: node.position.x, y: node.position.y },
      data: {
        label: node.data.label,
        config: node.data.config || {},
      },
    }))
    edges.value = (canvas.edges || []).map((edge) => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: edge.sourceHandle,
      targetHandle: edge.targetHandle,
      label: edge.label,
    }))
    return
  }
  nodes.value = (data.nodes || []).map((node) => ({
    id: node.nodeId,
    type: node.nodeType,
    position: { x: node.positionX || 0, y: node.positionY || 0 },
    data: {
      label: node.nodeName,
      config: node.nodeConfig || {},
    },
  }))
  edges.value = (data.edges || []).map((edge) => ({
    id: edge.edgeId,
    source: edge.sourceNodeId,
    target: edge.targetNodeId,
    sourceHandle: edge.sourceHandle,
    targetHandle: edge.targetHandle,
    label: edge.condition,
  }))
}

async function loadDetail() {
  const res = await fetchWorkflow(workflowId)
  applyDetail(res.data.data)
}

async function loadTools() {
  toolsLoading.value = true
  try {
    const res = await fetchToolOptions()
    toolOptions.value = res.data.data.map((item) => ({
      label: item.toolType === 'mcp'
        ? `${item.displayName}（MCP · ${item.mcpToolName || item.toolName}）`
        : `${item.displayName}（${item.toolName}）`,
      value: item.id,
    }))
  } finally {
    toolsLoading.value = false
  }
}

async function loadKnowledgeBases() {
  knowledgeLoading.value = true
  try {
    const res = await fetchKnowledgeBases({ page: 1, pageSize: 100 })
    knowledgeOptions.value = res.data.data.list.map((item) => ({
      label: item.kbName,
      value: item.id,
    }))
  } finally {
    knowledgeLoading.value = false
  }
}

async function loadModels() {
  modelsLoading.value = true
  try {
    const res = await fetchModelConfigs({ modelType: 'chat' })
    modelOptions.value = res.data.data
      .filter((item) => item.enabled)
      .map((item) => ({ label: item.displayName || item.modelName, value: item.id }))
  } finally {
    modelsLoading.value = false
  }
}

async function saveWorkflow() {
  saving.value = true
  try {
    const res = await updateWorkflow(workflowId, {
      workflowName: detail.value.workflowName,
      description: detail.value.description,
      applicationId: detail.value.applicationId,
      canvasData: buildCanvasData(),
    })
    applyDetail(res.data.data)
    message.success('已保存')
    return res.data.data
  } catch (e) {
    message.error(e instanceof Error ? e.message : '保存失败')
    throw e
  } finally {
    saving.value = false
  }
}

async function publishWorkflowAction() {
  publishing.value = true
  try {
    await saveWorkflow()
    const res = await publishWorkflow(workflowId)
    applyDetail(res.data.data)
    message.success('发布成功')
  } catch {
    // saveWorkflow 已提示错误
  } finally {
    publishing.value = false
  }
}

function openRun() {
  runOpen.value = true
  runResult.value = null
}

async function runWorkflowAction() {
  running.value = true
  try {
    await saveWorkflow()
    const res = await runWorkflow(workflowId, runInput.value)
    runResult.value = res.data.data
  } catch (e) {
    message.error(e instanceof Error ? e.message : '运行失败')
  } finally {
    running.value = false
  }
}

onMounted(() => {
  loadDetail().catch((e) => message.error(e instanceof Error ? e.message : '加载失败'))
  loadModels()
  loadTools()
  loadKnowledgeBases()
})

watch(
  () => route.params.id,
  (id) => {
    if (id) {
      loadDetail().catch((e) => message.error(e instanceof Error ? e.message : '加载失败'))
    }
  },
)
</script>

<style scoped>
.workflow-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100vh - 120px);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-left h2 {
  margin: 0;
  font-size: 18px;
}

.toolbar-left p {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
}

.editor-body {
  display: grid;
  grid-template-columns: 220px 1fr 300px;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.panel-title {
  font-weight: 600;
  margin-bottom: 12px;
}

.palette,
.config-panel {
  padding: 14px;
  overflow: auto;
}

.palette-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--border-color, #e5e7eb);
  background: #fff;
  border-radius: 8px;
  padding: 10px 12px;
  cursor: pointer;
}

.palette-item:hover {
  border-color: #6366f1;
  background: #f8fafc;
}

.palette-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.palette-icon.start { background: #dcfce7; color: #16a34a; }
.palette-icon.llm { background: #eef2ff; color: #4f46e5; }
.palette-icon.knowledge { background: #dcfce7; color: #15803d; }
.palette-icon.tool { background: #fee2e2; color: #dc2626; }
.palette-icon.condition { background: #fef3c7; color: #d97706; }
.palette-icon.end { background: #fee2e2; color: #dc2626; }

.canvas-wrap {
  min-height: 0;
  overflow: hidden;
}

.canvas-wrap :deep(.vue-flow) {
  width: 100%;
  height: 100%;
  min-height: 520px;
}

.canvas-wrap :deep(.vue-flow__handle) {
  width: 10px;
  height: 10px;
  background: #94a3b8;
  border: 2px solid #fff;
  z-index: 2;
}

.canvas-wrap :deep(.vue-flow__handle:hover) {
  background: #6366f1;
}

.canvas-wrap :deep(.vue-flow__edge-path) {
  stroke: #94a3b8;
  stroke-width: 2;
}

.canvas-wrap :deep(.vue-flow__edge.selected .vue-flow__edge-path) {
  stroke: #6366f1;
}

.run-result {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.run-steps {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.run-step {
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 8px;
  padding: 10px;
}

.step-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.step-output {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: pre-wrap;
}

.step-error {
  font-size: 13px;
  color: #dc2626;
}

@media (max-width: 1200px) {
  .editor-body {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr auto;
  }
}
</style>
