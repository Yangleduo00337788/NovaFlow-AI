<template>
  <AgentProcessStack v-if="hasSteps" :loading="groupLoading">
    <AgentProcessBlock
      v-if="showThinking"
      hide-icon
      :title="thinkingTitle ?? '深度思考'"
      :loading="thinkingLoading"
      :expanded="thinkingExpanded"
      @update:expanded="(value) => emit('update:thinkingExpanded', value)"
    >
      <div v-if="thinkingContent" class="process-block__text">
        {{ thinkingContent }}<span v-if="thinkingLoading" class="cursor">|</span>
      </div>
      <div v-else-if="thinkingLoading" class="process-block__text process-block__text--muted">
        正在分析问题...
      </div>
    </AgentProcessBlock>

    <WebSearchStatusBar
      v-if="showWebSearch"
      hide-icon
      :items="webSearchItems"
      :query="webSearchQuery"
      :loading="webSearchLoading"
    />

    <template v-for="(tool, toolIndex) in msg.toolCalls || []" :key="toolIndex">
      <SearchToolResult
        v-if="isSearchToolName(tool.name)"
        hide-icon
        :name="tool.name"
        :args="tool.args"
        :result="tool.result"
      />
      <AgentProcessBlock
        v-else
        hide-icon
        :title="getToolCallTitle(tool)"
        :loading="!tool.result"
        :collapsible="false"
      />
    </template>

    <AgentKnowledgeSourceBlock
      v-if="msg.sources?.length"
      hide-icon
      :sources="msg.sources"
      @open="(source) => emit('openSource', source)"
    />
  </AgentProcessStack>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AgentKnowledgeSourceBlock from '@/components/agent/AgentKnowledgeSourceBlock.vue'
import AgentProcessBlock from '@/components/agent/AgentProcessBlock.vue'
import AgentProcessStack from '@/components/agent/AgentProcessStack.vue'
import SearchToolResult from '@/components/agent/SearchToolResult.vue'
import WebSearchStatusBar from '@/components/agent/WebSearchStatusBar.vue'
import type { RetrievalSourceItem } from '@/api/agent'
import { isSearchToolName } from '@/utils/searchToolResult'

interface ToolCallRecord {
  name: string
  args?: string
  result?: string
}

interface ChatMessage {
  toolCalls?: ToolCallRecord[]
  sources?: RetrievalSourceItem[]
  streaming?: boolean
}

const props = defineProps<{
  msg: ChatMessage
  showWebSearch: boolean
  webSearchItems: Array<{ title: string; url?: string; snippet?: string }>
  webSearchQuery?: string
  showThinking?: boolean
  thinkingTitle?: string
  thinkingContent?: string
  thinkingLoading?: boolean
  thinkingExpanded?: boolean
}>()

const emit = defineEmits<{
  openSource: [source: RetrievalSourceItem]
  'update:thinkingExpanded': [value: boolean]
}>()

const webSearchLoading = computed(() => Boolean(props.msg.streaming && !props.webSearchItems.length))

const nonSearchToolCalls = computed(() =>
  (props.msg.toolCalls || []).filter((tool) => !isSearchToolName(tool.name)),
)

const hasSteps = computed(() =>
  Boolean(
    props.showThinking
    || props.showWebSearch
    || nonSearchToolCalls.value.length
    || props.msg.sources?.length,
  ),
)

const groupLoading = computed(() =>
  Boolean(
    props.thinkingLoading
    || webSearchLoading.value
    || nonSearchToolCalls.value.some((tool) => !tool.result),
  ),
)

function formatToolDisplayName(name: string) {
  const colonIndex = name.lastIndexOf(':')
  if (colonIndex >= 0 && colonIndex < name.length - 1) {
    return name.slice(colonIndex + 1).replace(/_/g, ' ')
  }
  const underscoreIndex = name.lastIndexOf('_')
  if (underscoreIndex >= 0 && underscoreIndex < name.length - 1) {
    return name.slice(underscoreIndex + 1).replace(/_/g, ' ')
  }
  return name.replace(/_/g, ' ')
}

function getToolCallTitle(tool: ToolCallRecord) {
  const name = formatToolDisplayName(tool.name)
  return tool.result ? `已调用工具 · ${name}` : `调用工具中......`
}
</script>

<style scoped>
.cursor {
  display: inline-block;
  margin-left: 2px;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>
