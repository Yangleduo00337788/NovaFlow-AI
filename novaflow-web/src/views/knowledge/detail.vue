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
        <a-button @click="openEdit">编辑配置</a-button>
        <a-popconfirm title="确认删除该知识库及全部文档？" @confirm="onDeleteKb">
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

    <div class="page-card upload-section">
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
                v-if="record.processStatus === 0 || record.processStatus === 3"
                type="link"
                @click="onReprocess(record.id)"
              >
                重新处理
              </a-button>
              <a-popconfirm title="确认删除该文档？" @confirm="onDeleteDocument(record.id)">
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
          <a-input v-model:value="editForm.kbName" />
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
        <a-button type="primary" block :loading="saving" @click="onSaveEdit">保存修改</a-button>
      </a-form>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
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
  updateKnowledgeBase,
  uploadDocument,
  type DocumentItem,
  type KnowledgeBaseItem,
  type KnowledgeBaseSaveRequest,
} from '@/api/knowledge'
import { fetchEmbeddingOptions } from '@/api/model'
import { formatDateTime } from '@/utils/datetime'
import { formatFileSize } from '@/utils/filesize'

const route = useRoute()
const router = useRouter()
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

const editForm = reactive<KnowledgeBaseSaveRequest>({
  kbName: '',
  description: '',
  embeddingModel: '',
  chunkStrategy: 'fixed',
  chunkSize: 512,
  chunkOverlap: 50,
  visibility: 'private',
})

const columns = [
  { title: '文档名称', key: 'docName', dataIndex: 'docName' },
  { title: '类型', dataIndex: 'docType', key: 'docType', width: 90 },
  { title: '大小', key: 'fileSize', width: 100 },
  { title: '分块数', dataIndex: 'chunkCount', key: 'chunkCount', width: 90 },
  { title: '状态', key: 'processStatus', width: 110 },
  { title: '上传时间', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 150 },
]

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

async function loadDetail() {
  const res = await fetchKnowledgeBase(kbId.value)
  detail.value = res.data.data
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
  if (!detail.value) return
  editForm.kbName = detail.value.kbName
  editForm.description = detail.value.description || ''
  editForm.embeddingModel = detail.value.embeddingModel
  editForm.chunkStrategy = detail.value.chunkStrategy
  editForm.chunkSize = detail.value.chunkSize
  editForm.chunkOverlap = detail.value.chunkOverlap
  editForm.visibility = detail.value.visibility
  drawerOpen.value = true
}

function resetEditForm() {
  drawerOpen.value = false
}

async function onSaveEdit() {
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
  try {
    await deleteDocument(kbId.value, documentId)
    message.success('文档已删除')
    await reloadAll()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function onReprocess(documentId: number) {
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
    await reloadAll()
  },
)

onMounted(async () => {
  await loadEmbeddingModels()
  await reloadAll()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
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

@media (max-width: 960px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
