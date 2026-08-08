import type { ApiResult } from '@/types/dashboard'

export interface WorkflowItem {
  id: number
  applicationId: number
  applicationName?: string
  workflowName: string
  description?: string
  status?: number
  version?: number
  nodeCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface WorkflowCanvasNode {
  id: string
  type: string
  position: { x: number; y: number }
  data: {
    label: string
    config?: Record<string, unknown>
  }
}

export interface WorkflowCanvasEdge {
  id: string
  source: string
  target: string
  sourceHandle?: string
  targetHandle?: string
  label?: string
}

export interface WorkflowCanvasData {
  nodes: WorkflowCanvasNode[]
  edges: WorkflowCanvasEdge[]
  viewport?: Record<string, unknown>
}

export interface WorkflowDetail extends WorkflowItem {
  elExpression?: string
  canvasData?: WorkflowCanvasData
  nodes?: Array<{
    nodeId: string
    nodeType: string
    nodeName: string
    positionX?: number
    positionY?: number
    nodeConfig?: Record<string, unknown>
  }>
  edges?: Array<{
    edgeId: string
    sourceNodeId: string
    targetNodeId: string
    sourceHandle?: string
    targetHandle?: string
    condition?: string
  }>
}

export interface WorkflowSaveRequest {
  workflowName: string
  description?: string
  applicationId: number
  canvasData?: WorkflowCanvasData
}

export interface WorkflowRunStep {
  nodeId: string
  nodeType: string
  nodeName: string
  status: number
  input?: string
  output?: string
  errorMessage?: string
  durationMs?: number
}

export interface WorkflowRunResult {
  executionId: string
  status: number
  output?: string
  errorMessage?: string
  durationMs?: number
  steps: WorkflowRunStep[]
}

export function getWorkflowStatusLabel(status?: number) {
  return status === 1 ? '已发布' : '草稿'
}

export function getWorkflowStatusColor(status?: number) {
  return status === 1 ? 'success' : 'default'
}

export type { ApiResult }
