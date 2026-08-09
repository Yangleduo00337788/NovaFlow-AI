export interface DashboardOverview {
  stats: StatCard[]
  recentItems: RecentItem[]
  favoriteItems: RecentItem[]
  recentLogs: RecentLog[]
  modelUsage: ModelUsage[]
  topApps: TopApp[]
  systemHealth: SystemHealth[]
  trend: TrendPoint[]
  quickActions: QuickAction[]
  quickStartTiles: QuickStartTile[]
  planInfo: PlanInfo
  workflowRuntime?: WorkflowRuntime | null
  totalModelTokens?: string
  sparklines?: Record<string, number[]>
}

export interface StatCard {
  key: string
  label: string
  value: string
  change: string
  up: boolean
}

export interface RecentItem {
  name: string
  type: string
  updatedAt: string
  path: string
  resourceType?: string
  resourceId?: number
  favorite?: boolean
}

export interface RecentLog {
  logId?: number
  traceId?: string
  name: string
  status: string
  success: boolean
  time: string
  duration: string
  tokens: number | null
}

export interface ModelUsage {
  model: string
  percent: number
  tokens: string
}

export interface TopApp {
  name: string
  value: string
  icon?: string
  color?: string
  iconBg?: string
}

export interface SystemHealth {
  name: string
  status: string
  healthy: boolean
}

export interface TrendPoint {
  time: string
  value: number
}

export interface QuickAction {
  key: string
  label: string
  path: string
}

export interface QuickStartTile {
  key: string
  label: string
  desc: string
  path: string
  color: string
}

export interface PlanInfo {
  planType: string
  expireAt: string
  usedPercent: number
}

export interface WorkflowRuntimeNode {
  nodeId: string
  nodeName: string
  nodeType: string
  status: number
  statusLabel: string
}

export interface WorkflowRuntimeCanvasNode {
  id: string
  type: string
  position: { x: number; y: number }
  data: { label: string }
  status: number
  statusLabel: string
}

export interface WorkflowRuntimeCanvasEdge {
  id: string
  source: string
  target: string
  sourceHandle?: string
  targetHandle?: string
  label?: string
}

export interface WorkflowRuntimeCanvas {
  nodes: WorkflowRuntimeCanvasNode[]
  edges: WorkflowRuntimeCanvasEdge[]
}

export interface WorkflowRuntime {
  workflowId: number
  workflowName: string
  executionId?: string
  status: number
  statusLabel: string
  running: boolean
  path: string
  nodes: WorkflowRuntimeNode[]
  canvas?: WorkflowRuntimeCanvas
}

export interface PublishedWorkflow {
  workflowId: number
  workflowName: string
  applicationName: string
  status: number
  statusLabel: string
  path: string
  updatedAt: string
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}
