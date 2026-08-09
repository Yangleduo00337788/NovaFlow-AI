import type { WorkflowDetail } from '@/types/workflow'

export interface WorkflowFlowNode {
  id: string
  type: string
  position: { x: number; y: number }
  data: {
    label: string
    config?: Record<string, unknown>
    status?: number
    statusLabel?: string
  }
}

export interface WorkflowFlowEdge {
  id: string
  source: string
  target: string
  sourceHandle?: string
  targetHandle?: string
  label?: string
}

export function detailToFlow(detail: WorkflowDetail): { nodes: WorkflowFlowNode[]; edges: WorkflowFlowEdge[] } {
  const canvas = detail.canvasData
  if (canvas?.nodes?.length) {
    return {
      nodes: canvas.nodes.map((node) => ({
        id: node.id,
        type: node.type,
        position: { x: node.position.x, y: node.position.y },
        data: {
          label: node.data.label,
          config: node.data.config || {},
        },
      })),
      edges: (canvas.edges || []).map((edge) => ({
        id: edge.id,
        source: edge.source,
        target: edge.target,
        sourceHandle: edge.sourceHandle,
        targetHandle: edge.targetHandle,
        label: edge.label,
      })),
    }
  }

  return {
    nodes: (detail.nodes || []).map((node) => ({
      id: node.nodeId,
      type: node.nodeType,
      position: { x: node.positionX || 0, y: node.positionY || 0 },
      data: {
        label: node.nodeName,
        config: node.nodeConfig || {},
      },
    })),
    edges: (detail.edges || []).map((edge) => ({
      id: edge.edgeId,
      source: edge.sourceNodeId,
      target: edge.targetNodeId,
      sourceHandle: edge.sourceHandle,
      targetHandle: edge.targetHandle,
      label: edge.condition,
    })),
  }
}

export function mergeNodeRuntimeStatus(
  nodes: WorkflowFlowNode[],
  statusMap: Record<string, { status: number; statusLabel: string }>,
): WorkflowFlowNode[] {
  return nodes.map((node) => {
    const runtime = statusMap[node.id]
    if (!runtime) return node
    return {
      ...node,
      data: {
        ...node.data,
        status: runtime.status,
        statusLabel: runtime.statusLabel,
      },
    }
  })
}
