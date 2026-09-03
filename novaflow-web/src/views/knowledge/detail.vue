<template>
  <div class="knowledge-detail-page" data-testid="knowledge-detail-page">
    <div class="page-header">
      <div class="header-left">
        <a-button type="text" class="back-btn" @click="router.push('/knowledge')">
          <ArrowLeftOutlined />
          返回列表
        </a-button>
        <div>
          <h1>{{ detail?.kbName || '知识库详情' }}</h1>
          <p>{{ detail?.description || '管理文档上传与处理状态' }}</p>
        </div>
      </div>
      <a-space>
        <a-button v-if="canCreate" @click="openEdit">编辑配置</a-button>
        <a-popconfirm v-if="canCreate" title="确认删除该知识库及全部文档？" @confirm="onDeleteKb">
          <a-button danger>删除知识库</a-button>
        </a-popconfirm>
      </a-space>
    </div>

    <div v-if="detail" class="overview-grid">
      <div class="overview-card page-card">
        <span class="overview-label">文档数量</span>
        <strong>{{ detail.documentCount }}</strong>
      </div>
      <div class="overview-card page-card">
        <span class="overview-label">存储大小</span>
        <strong>{{ formatFileSize(detail.totalSizeBytes) }}</strong>
      </div>
      <div class="overview-card page-card">
        <span class="overview-label">分块总数</span>
        <strong>{{ detail.chunkCount }}</strong>
      </div>
      <div class="overview-card page-card">
        <span class="overview-label">Embedding 模型</span>
        <strong class="model-name">{{ detail.embeddingModel }}</strong>
      </div>
    </div>

    <div v-if="canUpload" class="page-card upload-section">
      <div class="section-title">上传文档</div>
      <p class="section-desc">支持 PDF、Word、Excel、PPT、TXT、Markdown、HTML，单文件最大 50MB</p>
      <a-upload-dragger
        :multiple="true"
        :show-upload-list="false"
        :before-upload="beforeUpload"
        :disabled="uploading"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.html,.htm"
      >
        <p class="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p class="ant-upload-text">点击或拖拽文件到此处上传</p>
        <p class="ant-upload-hint">上传后文档将进入待处理队列，后续由 RAG 流水线自动解析分块</p>
      </a-upload-dragger>
      <div v-if="uploading" class="uploading-tip">
        <a-spin size="small" />
        <span>正在上传 {{ uploadingCount }} 个文件...</span>
      </div>
    </div>

    <div class="page-card retrieval-section">
      <div class="section-title">检索测试</div>
      <p class="section-desc">输入问题测试向量检索效果，查看召回的分块内容与相似度分数</p>
      <div class="retrieval-form">
        <a-textarea
          v-model:value="retrievalQuery"
          :rows="3"
          placeholder="输入测试问题，例如：产品的退货政策是什么？"
          :disabled="retrieving"
        />
        <div class="retrieval-controls">
          <div class="retrieval-topk">
            <span>Top-K</span>
            <a-input-number v-model:value="retrievalTopK" :min="1" :max="20" :disabled="retrieving" />
          </div>
          <div class="retrieval-topk">
            <span>相似度阈值</span>
            <a-input-number
              v-model:value="retrievalScoreThreshold"
              :min="0"
              :max="1"
              :step="0.05"
              :disabled="retrieving"
              placeholder="默认"
            />
          </div>
          <div class="retrieval-topk">
            <span>Rerank</span>
            <a-switch v-model:checked="retrievalRerankEnabled" :disabled="retrieving" />
          </div>
          <div v-if="retrievalRerankEnabled" class="retrieval-topk retrieval-topk--wide">
            <span>Rerank 模型</span>
            <a-select
              v-model:value="retrievalRerankModel"
              allow-clear
              placeholder="选择模型"
              :disabled="retrieving"
              :options="rerankModelOptions"
              style="min-width: 220px"
            />
          </div>
          <div class="retrieval-topk">
            <span>混合检索</span>
            <a-switch v-model:checked="retrievalHybridEnabled" :disabled="retrieving" />
          </div>
          <div v-if="retrievalHybridEnabled" class="retrieval-topk retrieval-topk--wide">
            <span>向量权重</span>
            <a-slider
              v-model:value="retrievalHybridAlpha"
              :min="0"
              :max="1"
              :step="0.05"
              :disabled="retrieving"
              style="min-width: 160px"
            />
          </div>
          <a-button type="primary" :loading="retrieving" :disabled="!retrievalQuery.trim()" @click="onRetrieve">
            开始检索
          </a-button>
        </div>
        <a-alert
          v-if="retrievalRerankEnabled && !rerankModelOptions.length"
          type="warning"
          show-icon
          message="未找到可用的 Rerank 模型"
          description="请先在模型中心同步并启用 rerank 类型模型。"
          class="retrieval-hint"
        />
        <a-alert
          v-else-if="retrievalRerankEnabled && !retrievalRerankModel"
          type="info"
          show-icon
          message="请选择 Rerank 模型后再检索"
          class="retrieval-hint"
        />
      </div>

      <div v-if="retrievalResult" class="retrieval-result">
        <div class="retrieval-meta">
          耗时 {{ retrievalResult.latencyMs }}ms，召回 {{ retrievalResult.chunks.length }} 条
        </div>
        <a-empty v-if="retrievalResult.chunks.length === 0" description="未检索到相关内容，请确认文档已处理完成" />
        <div v-else class="chunk-list">
          <div v-for="(chunk, index) in retrievalResult.chunks" :key="index" class="chunk-card">
            <div class="chunk-head">
              <span class="chunk-rank">#{{ index + 1 }}</span>
              <span class="chunk-doc">{{ chunk.docName || '未知文档' }}</span>
              <a-tag v-if="chunk.score != null" color="blue">相似度 {{ formatScore(chunk.score) }}</a-tag>
            </div>
            <div class="chunk-text">{{ chunk.text }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="page-card documents-section">
      <div class="section-head">
        <div class="section-title">文档列表</div>
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索文档"
          style="width: 240px"
          allow-clear
          @search="loadDocuments"
        />
      </div>

      <a-table
        :columns="columns"
        :data-source="documents"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        :row-class-name="rowClassName"
        :custom-row="customRow"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'docName'">
            <div class="doc-name-cell">
              <DocumentFileIcon :doc-type="record.docType" :file-name="record.docName" />
              <span>{{ record.docName }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'fileSize'">
            {{ formatFileSize(record.fileSize) }}
          </template>
          <template v-else-if="column.key === 'processStatus'">
            <a-tooltip v-if="record.processStatus === 3 && record.processError" :title="record.processError">
              <a-tag :color="statusColor(record.processStatus)">{{ record.processStatusLabel }}</a-tag>
            </a-tooltip>
            <a-tag v-else :color="statusColor(record.processStatus)">{{ record.processStatusLabel }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-if="canUpload && (record.processStatus === 0 || record.processStatus === 3)"
                type="link"
                @click="onReprocess(record.id)"
              >
                重新处理
              </a-button>
              <a-popconfirm v-if="canCreate" title="确认删除该文档？" @confirm="onDeleteDocument(record.id)">
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-drawer v-model:open="drawerOpen" title="编辑知识库" :width="560" @close="resetEditForm">
      <a-form layout="vertical" :model="editForm">
        <a-form-item label="名称" required>
          <a-input v-model:value="editForm.kbName" :show-count="true" :maxlength="128" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="editForm.description" :rows="3" />
        </a-form-item>
        <a-form-item label="Embedding 模型" required>
          <a-select v-model:value="editForm.embeddingModel" :options="embeddingModelOptions" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="分块策略">
              <a-select v-model:value="editForm.chunkStrategy">
                <a-select-option value="fixed">固定长度</a-select-option>
                <a-select-option value="paragraph">按段落</a-select-option>
                <a-select-option value="semantic">语义分块</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="可见性">
              <a-select v-model:value="editForm.visibility">
                <a-select-option value="private">私有</a-select-option>
                <a-select-option value="team">团队</a-select-option>
                <a-select-option value="public">公开</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="分块大小">
              <a-input-number v-model:value="editForm.chunkSize" :min="128" :max="4096" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="重叠字符">
              <a-input-number v-model:value="editForm.chunkOverlap" :min="0" :max="512" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="检索 Top-K">
              <a-input-number v-model:value="editForm.retrievalTopK" :min="1" :max="20" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="相似度阈值">
              <a-input-number
                v-model:value="editForm.retrievalScoreThreshold"
                :min="0"
                :max="1"
                :step="0.05"
                style="width: 100%"
                placeholder="留空不限制"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-button type="primary" block :loading="saving" @click="onSaveEdit">保存修改</a-button>
      </a-form>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { UploadProps } from 'ant-design-vue'
import { ArrowLeftOutlined, InboxOutlined } from '@ant-design/icons-vue'
import DocumentFileIcon from '@/components/common/DocumentFileIcon.vue'
import {
  deleteDocument,
  deleteKnowledgeBase,
  fetchDocuments,
  fetchKnowledgeBase,
  reprocessDocument,
  retrieveKnowledge,
  updateKnowledgeBase,
  uploadDocument,
  type DocumentItem,
  type KnowledgeBaseItem,
  type KnowledgeBaseSaveRequest,
  type RetrievalTestResult,
} from '@/api/knowledge'
import { fetchEmbeddingOptions, fetchModelConfigs } from '@/api/model'
import { formatDateTime } from '@/utils/datetime'
import { formatFileSize } from '@/utils/filesize'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const canCreate = computed(() => auth.hasPermission('knowledge:create'))
const canUpload = computed(() => auth.hasPermission('knowledge:upload'))
const kbId = computed(() => Number(route.params.id))

const detail = ref<KnowledgeBaseItem | null>(null)
const documents = ref<DocumentItem[]>([])
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const uploadingCount = ref(0)
const drawerOpen = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const embeddingModels = ref<Array<{ label: string; value: string }>>([])
const retrievalQuery = ref('')
const retrievalTopK = ref(5)
const retrievalScoreThreshold = ref<number | undefined>(undefined)
const retrievalRerankEnabled = ref(false)
const retrievalRerankModel = ref<string | undefined>(undefined)
const retrievalHybridEnabled = ref(false)
const retrievalHybridAlpha = ref(0.7)
const rerankModelOptions = ref<Array<{ label: string; value: string }>>([])
const retrieving = ref(false)
const retrievalResult = ref<RetrievalTestResult | null>(null)
const highlightedDocId = ref<number | null>(null)

const HIGHLIGHT_DURATION_MS = 10_000

const editForm = reactive<KnowledgeBaseSaveRequest>({
  kbName: '',
  description: '',
  embeddingModel: '',
  chunkStrategy: 'fixed',
  chunkSize: 512,
  chunkOverlap: 50,
  retrievalTopK: 5,
  retrievalScoreThreshold: undefined,
  visibility: 'private',
})

const columns = computed(() => {
  const base = [
    { title: '文档名称', key: 'docName', dataIndex: 'docName' },
    { title: '类型', dataIndex: 'docType', key: 'docType', width: 90 },
    { title: '大小', key: 'fileSize', width: 100 },
    { title: '分块数', dataIndex: 'chunkCount', key: 'chunkCount', width: 90 },
    { title: '状态', key: 'processStatus', width: 110 },
    { title: '上传时间', key: 'createdAt', width: 170 },
  ]
  if (canCreate.value || canUpload.value) {
    base.push({ title: '操作', key: 'action', width: 150 })
  }
  return base
})

const pagination = computed(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showTotal: (t: number) => `共 ${t} 条`,
}))

const embeddingModelOptions = computed(() => embeddingModels.value)
const hasProcessingDocuments = computed(() =>
  documents.value.some((item) => item.processStatus === 0 || item.processStatus === 1),
)
let pollTimer: number | undefined
let highlightTimer: number | undefined

function customRow(record: DocumentItem) {
  return {
    id: `doc-row-${record.id}`,
  }
}

function rowClassName(record: DocumentItem) {
  return record.id === highlightedDocId.value ? 'doc-row-highlight' : ''
}

function clearHighlightTimer() {
  if (highlightTimer) {
    window.clearTimeout(highlightTimer)
    highlightTimer = undefined
  }
}

function clearHighlightQuery() {
  if (!route.query.highlightDoc) return
  const { highlightDoc: _highlightDoc, ...rest } = route.query
  router.replace({ query: rest })
}

async function scrollToHighlightedDocument(documentId: number) {
  await nextTick()
  document.getElementById(`doc-row-${documentId}`)?.scrollIntoView({
    behavior: 'smooth',
    block: 'center',
  })
}

function highlightDocument(documentId: number) {
  clearHighlightTimer()
  highlightedDocId.value = documentId
  clearHighlightQuery()
  scrollToHighlightedDocument(documentId)
  highlightTimer = window.setTimeout(() => {
    highlightedDocId.value = null
    highlightTimer = undefined
  }, HIGHLIGHT_DURATION_MS)
}

async function ensureDocumentVisible(documentId: number) {
  const current = documents.value.find((item) => item.id === documentId)
  if (current) return true

  keyword.value = ''
  const probe = await fetchDocuments(kbId.value, { page: 1, pageSize: 1 })
  const totalCount = probe.data.data.total
  if (totalCount === 0) return false

  const fetchSize = Math.min(totalCount, 200)
  const res = await fetchDocuments(kbId.value, { page: 1, pageSize: fetchSize })
  const index = res.data.data.list.findIndex((item) => item.id === documentId)
  if (index < 0) return false

  page.value = Math.floor(index / pageSize.value) + 1
  await loadDocuments()
  return documents.value.some((item) => item.id === documentId)
}

async function applyHighlightFromQuery() {
  const raw = route.query.highlightDoc
  if (!raw) return
  const documentId = Number(raw)
  if (Number.isNaN(documentId)) return

  const visible = await ensureDocumentVisible(documentId)
  if (!visible) {
    message.warning('未找到对应文档，可能已被删除')
    clearHighlightQuery()
    return
  }
  highlightDocument(documentId)
}

function statusColor(status: number) {
  switch (status) {
    case 2:
      return 'success'
    case 1:
      return 'processing'
    case 3:
      return 'error'
    default:
      return 'default'
  }
}

function formatScore(score: number) {
  return score.toFixed(3)
}

async function onRetrieve() {
  if (!retrievalQuery.value.trim()) return
  if (retrievalRerankEnabled.value) {
    if (!rerankModelOptions.value.length) {
      message.warning('未找到可用的 Rerank 模型，请先在模型中心配置并启用')
      return
    }
    if (!retrievalRerankModel.value) {
      message.warning('已开启 Rerank，请选择 Rerank 模型')
      return
    }
  }
  retrieving.value = true
  try {
    const res = await retrieveKnowledge(kbId.value, {
      query: retrievalQuery.value.trim(),
      topK: retrievalTopK.value,
      scoreThreshold: retrievalScoreThreshold.value,
      rerankEnabled: retrievalRerankEnabled.value,
      rerankModel: retrievalRerankModel.value,
      hybridEnabled: retrievalHybridEnabled.value,
      hybridAlpha: retrievalHybridAlpha.value,
    })
    retrievalResult.value = res.data.data
  } catch (e) {
    message.error(e instanceof Error ? e.message : '检索失败')
  } finally {
    retrieving.value = false
  }
}

async function loadDetail() {
  const res = await fetchKnowledgeBase(kbId.value)
  detail.value = res.data.data
  retrievalTopK.value = detail.value?.retrievalTopK ?? 5
  retrievalScoreThreshold.value = detail.value?.retrievalScoreThreshold
}

async function loadDocuments() {
  loading.value = true
  try {
    const res = await fetchDocuments(kbId.value, {
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    documents.value = res.data.data.list
    total.value = res.data.data.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载文档失败')
  } finally {
    loading.value = false
  }
}

async function loadRerankModels() {
  try {
    const res = await fetchModelConfigs({ modelType: 'rerank' })
    rerankModelOptions.value = (res.data.data || [])
      .filter((item) => item.enabled)
      .map((item) => ({
        label: `${item.displayName} (${item.providerName})`,
        value: item.modelName,
      }))
  } catch {
    rerankModelOptions.value = []
  }
}

async function loadEmbeddingModels() {
  try {
    const res = await fetchEmbeddingOptions()
    embeddingModels.value = (res.data.data || []).map((item) => ({
      label: `${item.displayName} (${item.providerName})`,
      value: item.modelName,
    }))
  } catch {
    embeddingModels.value = []
  }
}

async function reloadAll() {
  await Promise.all([loadDetail(), loadDocuments()])
}

const beforeUpload: UploadProps['beforeUpload'] = async (file) => {
  if (!canUpload.value) {
    return false
  }
  uploading.value = true
  uploadingCount.value += 1
  try {
    await uploadDocument(kbId.value, file as File)
    message.success(`${file.name} 上传成功`)
    await reloadAll()
  } catch (e) {
    message.error(e instanceof Error ? e.message : `${file.name} 上传失败`)
  } finally {
    uploadingCount.value = Math.max(0, uploadingCount.value - 1)
    uploading.value = uploadingCount.value > 0
  }
  return false
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  page.value = pag.current || 1
  pageSize.value = pag.pageSize || 20
  loadDocuments()
}

function openEdit() {
  if (!canCreate.value || !detail.value) return
  editForm.kbName = detail.value.kbName
  editForm.description = detail.value.description || ''
  editForm.embeddingModel = detail.value.embeddingModel
  editForm.chunkStrategy = detail.value.chunkStrategy
  editForm.chunkSize = detail.value.chunkSize
  editForm.chunkOverlap = detail.value.chunkOverlap
  editForm.retrievalTopK = detail.value.retrievalTopK ?? 5
  editForm.retrievalScoreThreshold = detail.value.retrievalScoreThreshold
  editForm.visibility = detail.value.visibility
  drawerOpen.value = true
}

function resetEditForm() {
  drawerOpen.value = false
}

async function onSaveEdit() {
  if (!canCreate.value) return
  saving.value = true
  try {
    await updateKnowledgeBase(kbId.value, editForm)
    message.success('配置已保存')
    drawerOpen.value = false
    await loadDetail()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDeleteDocument(documentId: number) {
  if (!canCreate.value) return
  try {
    await deleteDocument(kbId.value, documentId)
    message.success('文档已删除')
    await reloadAll()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function onReprocess(documentId: number) {
  if (!canUpload.value) return
  try {
    await reprocessDocument(kbId.value, documentId)
    message.success('已提交重新处理')
    await reloadAll()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '提交失败')
  }
}

function startPolling() {
  stopPolling()
  if (!hasProcessingDocuments.value) return
  pollTimer = window.setInterval(async () => {
    await Promise.all([loadDetail(), loadDocuments()])
    if (!hasProcessingDocuments.value) {
      stopPolling()
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = undefined
  }
}

async function onDeleteKb() {
  if (!canCreate.value) return
  try {
    await deleteKnowledgeBase(kbId.value)
    message.success('知识库已删除')
    router.push('/knowledge')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

watch(hasProcessingDocuments, (processing) => {
  if (processing) {
    startPolling()
  } else {
    stopPolling()
  }
})

watch(
  () => route.params.id,
  async (id) => {
    if (!id) return
    page.value = 1
    clearHighlightTimer()
    highlightedDocId.value = null
    await reloadAll()
    await applyHighlightFromQuery()
  },
)

watch(
  () => route.query.highlightDoc,
  async (docId, prev) => {
    if (!docId || docId === prev) return
    await applyHighlightFromQuery()
  },
)

onMounted(async () => {
  await loadEmbeddingModels()
  await loadRerankModels()
  await reloadAll()
  await applyHighlightFromQuery()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
  clearHighlightTimer()
})
</script>

<style scoped>
.knowledge-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.back-btn {
  padding-left: 0;
  color: #64748b;
}

.page-header h1 {
  margin: 0 0 4px;
}

.page-header p {
  margin: 0;
  color: #64748b;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.overview-card {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-label {
  color: #64748b;
  font-size: 13px;
}

.overview-card strong {
  font-size: 24px;
  color: #0f172a;
}

.model-name {
  font-size: 16px !important;
}

.upload-section,
.retrieval-section,
.documents-section {
  padding: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.section-desc {
  margin: 0 0 16px;
  color: #64748b;
  font-size: 13px;
}

.uploading-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  color: #64748b;
  font-size: 13px;
}

.retrieval-hint {
  margin-top: 12px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.doc-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.doc-name-cell span {
  word-break: break-all;
}

.retrieval-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.retrieval-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.retrieval-topk {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.retrieval-result {
  margin-top: 20px;
}

.retrieval-meta {
  margin-bottom: 12px;
  color: #64748b;
  font-size: 13px;
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chunk-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px 16px;
  background: #f8fafc;
}

.chunk-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.chunk-rank {
  font-weight: 600;
  color: #0f172a;
}

.chunk-doc {
  color: #334155;
  font-size: 13px;
}

.chunk-text {
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

:deep(.doc-row-highlight > td) {
  background-color: #fff7e6 !important;
  box-shadow: inset 3px 0 0 #faad14;
  animation: doc-row-highlight-fade 10s ease-out forwards;
}

@keyframes doc-row-highlight-fade {
  0% {
    background-color: #ffe58f !important;
  }
  70% {
    background-color: #fff7e6 !important;
  }
  100% {
    background-color: transparent !important;
    box-shadow: none;
  }
}

@media (max-width: 960px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
