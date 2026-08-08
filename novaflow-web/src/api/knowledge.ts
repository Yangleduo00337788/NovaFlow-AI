import request from './request'
import axios from 'axios'
import type { ApiResult } from '@/types/dashboard'
import { useAuthStore } from '@/stores/auth'

export interface KnowledgeBaseItem {
  id: number
  applicationId?: number
  kbName: string
  description?: string
  embeddingModel: string
  chunkStrategy: string
  chunkSize: number
  chunkOverlap: number
  retrievalTopK?: number
  retrievalScoreThreshold?: number
  documentCount: number
  chunkCount: number
  totalSizeBytes: number
  visibility: string
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface KnowledgeBaseSaveRequest {
  kbName: string
  description?: string
  embeddingModel: string
  chunkStrategy?: string
  chunkSize?: number
  chunkOverlap?: number
  retrievalTopK?: number
  retrievalScoreThreshold?: number
  visibility?: string
  applicationId?: number
}

export interface DocumentItem {
  id: number
  knowledgeBaseId: number
  docName: string
  docType: string
  fileSize: number
  fileHash?: string
  sourceType: string
  processStatus: number
  processStatusLabel: string
  processError?: string
  chunkCount: number
  charCount: number
  processedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export function fetchKnowledgeBases(params?: { page?: number; pageSize?: number; keyword?: string }) {
  return request.get<ApiResult<PageResult<KnowledgeBaseItem>>>('/v1/knowledge-bases', { params })
}

export function fetchKnowledgeBase(id: number) {
  return request.get<ApiResult<KnowledgeBaseItem>>(`/v1/knowledge-bases/${id}`)
}

export function createKnowledgeBase(data: KnowledgeBaseSaveRequest) {
  return request.post<ApiResult<KnowledgeBaseItem>>('/v1/knowledge-bases', data)
}

export function updateKnowledgeBase(id: number, data: KnowledgeBaseSaveRequest) {
  return request.put<ApiResult<KnowledgeBaseItem>>(`/v1/knowledge-bases/${id}`, data)
}

export function deleteKnowledgeBase(id: number) {
  return request.delete<ApiResult<void>>(`/v1/knowledge-bases/${id}`)
}

export function fetchDocuments(
  knowledgeBaseId: number,
  params?: { page?: number; pageSize?: number; keyword?: string },
) {
  return request.get<ApiResult<PageResult<DocumentItem>>>(`/v1/knowledge-bases/${knowledgeBaseId}/documents`, { params })
}

export function deleteDocument(knowledgeBaseId: number, documentId: number) {
  return request.delete<ApiResult<void>>(`/v1/knowledge-bases/${knowledgeBaseId}/documents/${documentId}`)
}

export function reprocessDocument(knowledgeBaseId: number, documentId: number) {
  return request.post<ApiResult<void>>(`/v1/knowledge-bases/${knowledgeBaseId}/documents/${documentId}/reprocess`)
}

export interface RetrievedChunkItem {
  knowledgeBaseId: number
  knowledgeBaseName?: string
  documentId?: number
  docName?: string
  chunkIndex?: number
  text: string
  score?: number
}

export interface RetrievalTestResult {
  query: string
  topK: number
  latencyMs: number
  chunks: RetrievedChunkItem[]
}

export interface RetrievalTestRequest {
  query: string
  topK?: number
  scoreThreshold?: number
  rerankEnabled?: boolean
  rerankModel?: string
  rerankCandidateK?: number
}

export function retrieveKnowledge(knowledgeBaseId: number, data: RetrievalTestRequest) {
  return request.post<ApiResult<RetrievalTestResult>>(`/v1/knowledge-bases/${knowledgeBaseId}/retrieve`, data)
}

export async function uploadDocument(knowledgeBaseId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const auth = useAuthStore()
  const response = await axios.post<ApiResult<DocumentItem>>(
    `/api/v1/knowledge-bases/${knowledgeBaseId}/documents/upload`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
        ...(auth.token ? { Authorization: auth.token } : {}),
      },
      timeout: 120000,
    },
  )
  const result = response.data
  if (result.code !== 0) {
    throw new Error(result.message || '上传失败')
  }
  return response
}
