<template>
  <div class="knowledge-page page-shell" data-testid="knowledge-page">
    <div class="page-header">
      <div>
        <h1>知识库 Hub</h1>
        <p>管理企业知识文档，为 RAG Agent 提供检索数据源</p>
      </div>
      <a-button type="primary" data-testid="create-kb-btn" @click="openCreate">
        <PlusOutlined />
        创建知识库
      </a-button>
    </div>

    <div class="list-panel page-card">
      <div class="list-toolbar">
        <div class="list-toolbar-filters">
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索知识库"
            style="width: 240px"
            allow-clear
            @search="loadData"
          />
        </div>
        <span class="list-toolbar-meta">共 {{ total }} 个知识库</span>
      </div>
      <div class="list-body">
        <a-spin :spinning="loading">
          <div v-if="list.length" class="kb-grid">
        <div
          v-for="item in list"
          :key="item.id"
          class="kb-card page-card"
          :data-testid="`kb-card-${item.id}`"
          @click="goDetail(item.id)"
        >
          <div class="kb-card-head">
            <div class="kb-icon">
              <BookOutlined />
            </div>
            <div class="kb-title-wrap">
              <h3>{{ item.kbName }}</h3>
              <p>{{ item.description || '暂无描述' }}</p>
            </div>
          </div>
          <div class="kb-stats">
            <div class="kb-stat">
              <FileTextOutlined />
              <span>{{ item.documentCount }} 文档</span>
            </div>
            <div class="kb-stat">
              <DatabaseOutlined />
              <span>{{ formatFileSize(item.totalSizeBytes) }}</span>
            </div>
            <div class="kb-stat">
              <PartitionOutlined />
              <span>{{ item.chunkCount }} 分块</span>
            </div>
          </div>
          <div class="kb-footer">
            <a-tag color="green">{{ item.embeddingModel }}</a-tag>
            <span class="kb-time">{{ formatDateTime(item.updatedAt) }}</span>
          </div>
          <div class="kb-actions" @click.stop>
            <a-button type="link" @click="openEdit(item)">编辑</a-button>
            <a-popconfirm title="确认删除该知识库及全部文档？" @confirm="onDelete(item.id)">
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>
      <a-empty v-else description="暂无知识库，点击右上角创建" />
        </a-spin>

        <div v-if="total > pageSize" class="pagination-wrap">
          <a-pagination
            v-model:current="page"
            :total="total"
            :page-size="pageSize"
            show-less-items
            @change="loadData"
          />
        </div>
      </div>
    </div>
    <a-drawer
      v-model:open="drawerOpen"
      :title="editingId ? '编辑知识库' : '创建知识库'"
      :width="560"
      @close="resetForm"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item label="名称" required>
          <a-input v-model:value="form.kbName" placeholder="产品手册知识库" data-testid="kb-name-input" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="简要说明知识库用途" />
        </a-form-item>
        <a-form-item label="Embedding 模型" required>
          <a-alert
            v-if="embeddingEmptyHint"
            type="warning"
            show-icon
            :message="embeddingEmptyHint"
            style="margin-bottom: 8px"
          />
          <a-select
            v-model:value="form.embeddingModel"
            placeholder="选择 Embedding 模型"
            :loading="modelsLoading"
            :options="embeddingModelOptions"
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="分块策略">
              <a-select v-model:value="form.chunkStrategy">
                <a-select-option value="fixed">固定长度</a-select-option>
                <a-select-option value="paragraph">按段落</a-select-option>
                <a-select-option value="semantic">语义分块</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="可见性">
              <a-select v-model:value="form.visibility">
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
              <a-input-number v-model:value="form.chunkSize" :min="128" :max="4096" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="重叠字符">
              <a-input-number v-model:value="form.chunkOverlap" :min="0" :max="512" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="检索 Top-K">
              <a-input-number v-model:value="form.retrievalTopK" :min="1" :max="20" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="相似度阈值">
              <a-input-number
                v-model:value="form.retrievalScoreThreshold"
                :min="0"
                :max="1"
                :step="0.05"
                style="width: 100%"
                placeholder="留空不限制"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-button type="primary" block :loading="saving" @click="onSave">
          {{ editingId ? '保存修改' : '创建知识库' }}
        </a-button>
      </a-form>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  BookOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  PartitionOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  fetchKnowledgeBases,
  updateKnowledgeBase,
  type KnowledgeBaseItem,
  type KnowledgeBaseSaveRequest,
} from '@/api/knowledge'
import { fetchEmbeddingOptions } from '@/api/model'
import { formatDateTime } from '@/utils/datetime'
import { formatFileSize } from '@/utils/filesize'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const modelsLoading = ref(false)
const drawerOpen = ref(false)
const editingId = ref<number | null>(null)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const list = ref<KnowledgeBaseItem[]>([])
const embeddingModels = ref<Array<{ label: string; value: string }>>([])
const embeddingEmptyHint = ref('')

const form = reactive<KnowledgeBaseSaveRequest>({
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

const embeddingModelOptions = computed(() => embeddingModels.value)

async function loadEmbeddingModels() {
  modelsLoading.value = true
  embeddingEmptyHint.value = ''
  try {
    const res = await fetchEmbeddingOptions()
    const models = res.data.data || []
    embeddingModels.value = models.map((item) => ({
      label: `${item.displayName} (${item.providerName})`,
      value: item.modelName,
    }))
    if (!embeddingModels.value.length) {
      embeddingEmptyHint.value = '暂无 Embedding 模型。请先在模型中心配置通义千问、智谱 AI 或 OpenAI 等支持 Embedding 的提供商。'
    } else if (!form.embeddingModel) {
      form.embeddingModel = embeddingModels.value[0].value
    }
  } catch (e) {
    embeddingEmptyHint.value = e instanceof Error ? e.message : '加载 Embedding 模型失败'
    embeddingModels.value = []
  } finally {
    modelsLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchKnowledgeBases({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    list.value = res.data.data.list
    total.value = res.data.data.total
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/knowledge/${id}`)
}

function resetForm() {
  editingId.value = null
  form.kbName = ''
  form.description = ''
  form.chunkStrategy = 'fixed'
  form.chunkSize = 512
  form.chunkOverlap = 50
  form.retrievalTopK = 5
  form.retrievalScoreThreshold = undefined
  form.visibility = 'private'
  form.embeddingModel = embeddingModels.value[0]?.value || ''
}

function openCreate() {
  resetForm()
  drawerOpen.value = true
}

function openEdit(item: KnowledgeBaseItem) {
  editingId.value = item.id
  form.kbName = item.kbName
  form.description = item.description || ''
  form.embeddingModel = item.embeddingModel
  form.chunkStrategy = item.chunkStrategy
  form.chunkSize = item.chunkSize
  form.chunkOverlap = item.chunkOverlap
  form.retrievalTopK = item.retrievalTopK ?? 5
  form.retrievalScoreThreshold = item.retrievalScoreThreshold
  form.visibility = item.visibility
  drawerOpen.value = true
}

async function onSave() {
  if (!form.kbName.trim()) {
    message.warning('请输入知识库名称')
    return
  }
  if (!form.embeddingModel) {
    message.warning('请选择 Embedding 模型')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateKnowledgeBase(editingId.value, form)
      message.success('知识库已更新')
    } else {
      const res = await createKnowledgeBase(form)
      message.success('知识库已创建')
      drawerOpen.value = false
      router.push(`/knowledge/${res.data.data.id}`)
      return
    }
    drawerOpen.value = false
    await loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete(id: number) {
  try {
    await deleteKnowledgeBase(id)
    message.success('已删除')
    await loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

onMounted(async () => {
  await Promise.all([loadEmbeddingModels(), loadData()])
  if (route.query.create === '1') {
    openCreate()
    return
  }
  if (route.query.import === '1') {
    if (list.value.length === 1) {
      router.push(`/knowledge/${list.value[0].id}`)
    } else if (list.value.length > 1) {
      message.info('请选择要导入文档的知识库')
    } else {
      message.info('请先创建知识库，再导入文档')
      openCreate()
    }
  }
})
</script>

<style scoped>
.knowledge-page {
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

.page-header h1 {
  margin: 0 0 4px;
}

.page-header p {
  margin: 0;
  color: #64748b;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
}

.toolbar-meta {
  color: #94a3b8;
  font-size: 13px;
}

.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.kb-card {
  padding: 18px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
  border: 1px solid #e2e8f0;
}

.kb-card:hover {
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.kb-card-head {
  display: flex;
  gap: 14px;
  margin-bottom: 16px;
}

.kb-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #ecfdf5;
  color: #10b981;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.kb-title-wrap h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.kb-title-wrap p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kb-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 14px;
  color: #475569;
  font-size: 13px;
}

.kb-stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.kb-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.kb-time {
  color: #94a3b8;
  font-size: 12px;
}

.kb-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  border-top: 1px solid #f1f5f9;
  padding-top: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding-bottom: 8px;
}
</style>
