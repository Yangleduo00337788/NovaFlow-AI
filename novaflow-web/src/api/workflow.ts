import request from './request'
import type {
  ApiResult,
  WorkflowDetail,
  WorkflowItem,
  WorkflowRunResult,
  WorkflowSaveRequest,
} from '@/types/workflow'

export function fetchWorkflowOptions(applicationId?: number) {
  return request.get<ApiResult<WorkflowItem[]>>('/v1/workflows/options', {
    params: applicationId ? { applicationId } : undefined,
  })
}

export function fetchWorkflows(params?: {
  page?: number
  pageSize?: number
  keyword?: string
  applicationId?: number
}) {
  return request.get<ApiResult<{ list: WorkflowItem[]; total: number; page: number; pageSize: number }>>(
    '/v1/workflows',
    { params },
  )
}

export function fetchWorkflow(id: number) {
  return request.get<ApiResult<WorkflowDetail>>(`/v1/workflows/${id}`)
}

export function createWorkflow(data: WorkflowSaveRequest) {
  return request.post<ApiResult<WorkflowDetail>>('/v1/workflows', data)
}

export function updateWorkflow(id: number, data: WorkflowSaveRequest) {
  return request.put<ApiResult<WorkflowDetail>>(`/v1/workflows/${id}`, data)
}

export function publishWorkflow(id: number) {
  return request.post<ApiResult<WorkflowDetail>>(`/v1/workflows/${id}/publish`)
}

export function runWorkflow(id: number, input?: string) {
  return request.post<ApiResult<WorkflowRunResult>>(`/v1/workflows/${id}/run`, { input })
}

export function deleteWorkflow(id: number) {
  return request.delete<ApiResult<void>>(`/v1/workflows/${id}`)
}

export function buildDefaultCanvas(): WorkflowSaveRequest['canvasData'] {
  return {
    nodes: [
      { id: 'start-1', type: 'start', position: { x: 80, y: 200 }, data: { label: '开始' } },
      {
        id: 'llm-1',
        type: 'llm',
        position: { x: 320, y: 180 },
        data: { label: 'LLM', config: { prompt: '请处理以下输入：{{input}}' } },
      },
      { id: 'end-1', type: 'end', position: { x: 600, y: 200 }, data: { label: '结束' } },
    ],
    edges: [
      { id: 'edge-1', source: 'start-1', target: 'llm-1' },
      { id: 'edge-2', source: 'llm-1', target: 'end-1' },
    ],
  }
}
