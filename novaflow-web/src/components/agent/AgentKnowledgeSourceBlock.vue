<template>
  <AgentProcessBlock
    :title="title"
    :loading="loading"
    :default-expanded="items.length > 0"
    :collapsible="items.length > 0"
    :hide-icon="hideIcon"
  >
    <div class="source-panel">
      <button
        v-for="(source, index) in items"
        :key="sourceKey(source, index)"
        type="button"
        class="source-panel__item"
        :title="sourceTitle(source)"
        @click="emit('open', source)"
      >
        <span class="source-panel__index">{{ index + 1 }}</span>
        <span class="source-panel__name">{{ sourceLabel(source) }}</span>
        <span v-if="source.knowledgeBaseName" class="source-panel__meta">{{ source.knowledgeBaseName }}</span>
      </button>
    </div>
  </AgentProcessBlock>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AgentProcessBlock from '@/components/agent/AgentProcessBlock.vue'
import type { RetrievalSourceItem } from '@/api/agent'

const props = withDefaults(
  defineProps<{
    sources: RetrievalSourceItem[]
    loading?: boolean
    hideIcon?: boolean
  }>(),
  {
    loading: false,
    hideIcon: false,
  },
)

const emit = defineEmits<{
  open: [source: RetrievalSourceItem]
}>()

const items = computed(() => uniqueSources(props.sources))

const title = computed(() => {
  if (props.loading && !items.value.length) {
    return '检索知识库中......'
  }
  return `已引用 ${items.value.length} 个知识库文档`
})

function uniqueSources(sources: RetrievalSourceItem[]) {
  const seen = new Set<string>()
  const result: RetrievalSourceItem[] = []
  for (const source of sources) {
    const key = source.documentId != null ? `doc:${source.documentId}` : `name:${source.docName}`
    if (seen.has(key)) continue
    seen.add(key)
    result.push(source)
  }
  return result
}

function getDocumentBaseName(fileName?: string) {
  if (!fileName) return '未知文档'
  const dot = fileName.lastIndexOf('.')
  return dot > 0 ? fileName.slice(0, dot) : fileName
}

function sourceLabel(source: RetrievalSourceItem) {
  return getDocumentBaseName(source.docName)
}

function sourceTitle(source: RetrievalSourceItem) {
  const kb = source.knowledgeBaseName ? `${source.knowledgeBaseName} / ` : ''
  const score = source.score != null ? ` · 相关度 ${source.score.toFixed(3)}` : ''
  return `${kb}${getDocumentBaseName(source.docName)}${score}`
}

function sourceKey(source: RetrievalSourceItem, index: number) {
  return source.documentId != null ? `doc:${source.documentId}` : `name:${source.docName}:${index}`
}
</script>

<style scoped>
.source-panel {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.source-panel__item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 6px 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  line-height: 1.45;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.source-panel__item:hover {
  background: #f8fafc;
  color: #475569;
}

.source-panel__index {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.source-panel__name {
  min-width: 0;
  color: #334155;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-panel__meta {
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
  flex-shrink: 0;
}
</style>
