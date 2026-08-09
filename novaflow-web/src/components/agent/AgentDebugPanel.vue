<template>
  <div
    class="debug-panel"
    :class="{
      'debug-panel--wide': wide,
      'debug-panel--drawer': showLayoutToggle,
    }"
    data-testid="agent-debug-panel"
  >
    <div class="debug-header">
      <div>
        <div class="title">
          调试对话
          <FieldHelpIcon text="实时测试当前 Agent 配置效果。修改左侧配置并保存后，新的对话将使用最新设置；已存在的会话上下文需清空后才会重置。" />
        </div>
        <div class="subtitle">{{ debugSubtitle }}</div>
      </div>
      <a-space v-if="showLayoutToggle" size="small">
        <a-tooltip title="查看并切换历史调试会话" placement="top">
          <a-button size="small" @click="openHistory">
            <HistoryOutlined />
            历史
          </a-button>
        </a-tooltip>
        <a-tooltip title="在全屏与半屏调试窗口之间切换" placement="top">
          <a-button size="small" @click="toggleLayout">
            <ColumnWidthOutlined />
            {{ wide ? '半屏' : '全屏' }}
          </a-button>
        </a-tooltip>
        <a-tooltip title="清空当前调试会话的消息记录与上下文记忆，相当于开始一段新对话。" placement="top">
          <a-button size="small" @click="resetChat">清空</a-button>
        </a-tooltip>
      </a-space>
      <a-tooltip v-else title="清空当前调试会话的消息记录与上下文记忆，相当于开始一段新对话。" placement="top">
        <a-space size="small">
          <a-button size="small" @click="openHistory">
            <HistoryOutlined />
            历史
          </a-button>
          <a-button size="small" @click="resetChat">清空</a-button>
        </a-space>
      </a-tooltip>
    </div>

    <a-drawer
      v-model:open="historyOpen"
      title="会话历史"
      :width="400"
      placement="right"
      :z-index="1100"
    >
      <div class="history-actions">
        <a-button type="primary" block @click="startNewConversation">新建会话</a-button>
      </div>
      <a-spin :spinning="historyLoading">
        <a-empty v-if="!historyList.length" description="暂无历史会话">
          <template #description>
            <span>暂无历史会话</span>
            <p class="history-empty-hint">在调试面板发送消息后，会话将自动保存到此列表</p>
          </template>
        </a-empty>
        <div v-else class="history-list">
          <div
            v-for="item in historyList"
            :key="item.id"
            class="history-item"
            :class="{ active: item.conversationKey === conversationId }"
            @click="selectConversation(item)"
          >
            <div class="history-preview">{{ item.preview || '（无预览）' }}</div>
            <div class="history-meta">
              <span>{{ item.messageCount || 0 }} 条消息</span>
              <span>{{ formatHistoryTime(item.lastMessageAt || item.createdAt) }}</span>
            </div>
          </div>
        </div>
      </a-spin>
    </a-drawer>

    <div ref="messageListRef" class="message-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message"
        :class="msg.role"
        :data-testid="`debug-message-${msg.role}`"
      >
        <div v-if="msg.role === 'user'" class="bubble user-bubble">
          {{ msg.content }}
        </div>
        <template v-else>
          <div v-if="showInitialLoading(msg)" class="assistant-loading">
            <AgentTechLoader aria-label="思考中" />
            <span class="thinking-label">正在思考......</span>
          </div>
          <div v-else-if="showReplyHead(msg)" class="assistant-reply-head">
            <AgentTechLoader size="xs" :paused="!msg.streaming" aria-hidden="true" />
            <span class="assistant-reply-head__label">
              {{ msg.streaming ? '正在回答......' : '已完成' }}
            </span>
          </div>
          <AgentAssistantProcesses
            :msg="msg"
            :show-thinking="shouldShowThinking(msg)"
            :thinking-title="getThinkingTitle(msg)"
            :thinking-content="msg.thinkingContent"
            :thinking-loading="isThinkingLoading(msg)"
            :thinking-expanded="msg.thinkingExpanded !== false"
            :show-web-search="shouldShowWebSearchBar(msg)"
            :web-search-items="resolveWebSearchItems(msg)"
            :web-search-query="getPreviousUserQuery(msg)"
            @update:thinking-expanded="(value) => patchMessage(msg.id, { thinkingExpanded: value })"
            @open-source="openSource"
          />
          <div v-if="msg.content" class="assistant-content-wrap">
            <div
              class="assistant-content markdown-body"
              v-html="renderAssistantContent(msg.content, msg)"
            />
            <span v-if="msg.streaming" class="cursor">|</span>
          </div>
        </template>
        <a-tooltip
          v-if="msg.meta"
          title="本次回复的耗时与 Token 消耗统计，用于评估响应速度与调用成本。"
          placement="top"
        >
          <div class="meta">{{ msg.meta }}</div>
        </a-tooltip>
      </div>
      <div v-if="loading && !streamingMessageId" class="message assistant">
        <div class="assistant-loading">
          <AgentTechLoader aria-label="加载中" />
        </div>
      </div>
    </div>

    <div class="input-area">
      <div class="input-label">
        测试消息
        <FieldHelpIcon text="输入问题测试 Agent 回复效果。Enter 发送，Shift+Enter 换行。生成过程中可点击停止按钮打断回复。" />
      </div>
      <div class="input-box-shell" :class="{ 'input-box-shell--streaming': loading }">
        <div
          class="input-box"
          :class="{ 'input-box--focused': inputFocused, 'input-box--disabled': loading }"
        >
        <div
          v-if="attachment"
          class="attachment-chip"
        >
          <PaperClipOutlined />
          <span>{{ attachment.fileName }}</span>
          <button type="button" class="attachment-chip__remove" @click="attachment = null">×</button>
        </div>
        <a-textarea
          :value="input"
          :auto-size="{ minRows: 1, maxRows: 6 }"
          :bordered="false"
          placeholder="输入测试消息，Enter 发送，Shift+Enter 换行"
          data-testid="debug-input"
          :disabled="loading"
          @update:value="onInputChange"
          @focus="inputFocused = true"
          @blur="inputFocused = false"
          @keydown.enter.exact.prevent="onSend"
        />
        <div class="input-toolbar">
          <div class="input-toolbar__left">
            <button
              v-if="showDeepThinkingToggle"
              type="button"
              class="capability-btn"
              :class="{ active: deepThinkingEnabled }"
              :disabled="loading"
              @click="deepThinkingEnabled = !deepThinkingEnabled"
            >
              <BulbOutlined />
              深度思考
            </button>
            <a-tooltip
              v-if="showWebSearchToggle"
              title="仅通义千问、Kimi、智谱、百度等支持联网的模型可用。"
              placement="top"
            >
              <button
                type="button"
                class="capability-btn"
                :class="{ active: webSearchEnabled }"
                :disabled="loading"
                @click="webSearchEnabled = !webSearchEnabled"
              >
                <GlobalOutlined />
                全网搜索
              </button>
            </a-tooltip>
          </div>
          <div class="input-toolbar__actions">
            <input
              ref="fileInputRef"
              type="file"
              class="hidden-file-input"
              accept=".txt,.md,.json,.csv,.log"
              @change="onAttachmentSelected"
            />
            <button
              type="button"
              class="input-icon-btn"
              title="上传文本附件"
              :disabled="loading"
              aria-label="附件"
              @click="onPickAttachment"
            >
              <PaperClipOutlined />
            </button>
            <button
              v-if="loading"
              type="button"
              class="send-btn send-btn--stop"
              aria-label="停止生成"
              @click="stopGeneration"
            >
              <StopOutlined />
            </button>
            <button
              v-else
              type="button"
              class="send-btn"
              data-testid="debug-send"
              :disabled="!canSend"
              aria-label="发送"
              @click="onSend"
            >
              <ArrowUpOutlined />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowUpOutlined, BulbOutlined, ColumnWidthOutlined, GlobalOutlined, HistoryOutlined, PaperClipOutlined, StopOutlined } from '@ant-design/icons-vue'
import FieldHelpIcon from '@/components/common/FieldHelpIcon.vue'
import AgentAssistantProcesses from '@/components/agent/AgentAssistantProcesses.vue'
import AgentTechLoader from '@/components/agent/AgentTechLoader.vue'
import {
  clearAgentDebugConversation,
  fetchAgentDebugWelcome,
  fetchDebugConversationMessages,
  fetchDebugConversations,
  streamAgentDebugChat,
  uploadDebugAttachment,
  type ConversationItem,
  type DebugAttachment,
  type RetrievalSourceItem,
  type WebSearchSourceItem,
} from '@/api/agent'
import { formatDateTime } from '@/utils/datetime'
import { renderMarkdown } from '@/utils/markdown'
import { injectSearchCitations } from '@/utils/searchCitations'
import {
  extractSearchItemsFromContent,
  isSearchToolName,
  parseSearchToolResult,
  toSearchResultItems,
  type SearchResultItem,
} from '@/utils/searchToolResult'

interface ToolCallRecord {
  name: string
  args?: string
  result?: string
}

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  thinkingContent?: string
  deepThinkingUsed?: boolean
  deepThinkingRequested?: boolean
  thinkingExpanded?: boolean
  thinkingStartedAt?: number
  thinkingFinishedAt?: number
  thinkingComplete?: boolean
  meta?: string
  streaming?: boolean
  sources?: RetrievalSourceItem[]
  toolCalls?: ToolCallRecord[]
  webSearchUsed?: boolean
  webSearchSources?: WebSearchSourceItem[]
}

interface DebugSession {
  conversationId: string
  messages: ChatMessage[]
  debugMode: boolean
  seq: number
  deepThinkingEnabled?: boolean
  webSearchEnabled?: boolean
}

interface ModelCapabilities {
  supportsDeepThinking?: boolean
  supportsWebSearch?: boolean
}

const props = withDefaults(
  defineProps<{
    agentId: number | null
    wide?: boolean
    showLayoutToggle?: boolean
  }>(),
  {
    wide: false,
    showLayoutToggle: false,
  },
)

const emit = defineEmits<{
  toggleLayout: [wide: boolean]
}>()

const router = useRouter()

const TYPEWRITER_INTERVAL_MS = 18

const messages = ref<ChatMessage[]>([])
const input = ref('')
const inputFocused = ref(false)
const loading = ref(false)
const canSend = computed(() => input.value.trim().length > 0)
const debugMode = ref(true)
const conversationId = ref(createConversationId())
const streamingMessageId = ref<number | null>(null)
const messageListRef = ref<HTMLElement | null>(null)
const historyOpen = ref(false)
const historyLoading = ref(false)
const historyList = ref<ConversationItem[]>([])
const modelCapabilities = ref<ModelCapabilities | null>(null)
const activeModelLabel = ref('')
const deepThinkingEnabled = ref(false)
const webSearchEnabled = ref(false)
const attachment = ref<DebugAttachment | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const showDeepThinkingToggle = computed(() => Boolean(modelCapabilities.value?.supportsDeepThinking))
const showWebSearchToggle = computed(() => Boolean(modelCapabilities.value?.supportsWebSearch))
const debugSubtitle = computed(() => {
  const modelPart = activeModelLabel.value ? `当前模型：${activeModelLabel.value} · ` : ''
  const modePart = debugMode.value ? '实时预览 Agent 回复（调试模式）' : '已接入模型引擎，流式返回真实 AI 回复'
  return `${modelPart}${modePart}`
})
let seq = 1
let abortController: AbortController | null = null
let tokenBuffer = ''
let typewriterTimer: ReturnType<typeof setInterval> | null = null
let activeAssistantMessageId: number | null = null

function createConversationId() {
  return `debug-${crypto.randomUUID()}`
}

function storageKey(agentId: number) {
  return `novaflow:debug:${agentId}`
}

function onInputChange(value: string) {
  input.value = value
}

function formatModelLabel(modelName?: string, providerName?: string) {
  if (!modelName?.trim()) {
    return ''
  }
  return providerName?.trim() ? `${providerName.trim()} · ${modelName.trim()}` : modelName.trim()
}

function formatReplyMeta(tokens: number, latency: number, modelName?: string) {
  const parts: string[] = []
  if (modelName?.trim()) {
    parts.push(modelName.trim())
  }
  parts.push(`${tokens} tokens`, `${latency}ms`)
  return parts.join(' · ')
}

function hasSearchToolCalls(msg: ChatMessage) {
  return msg.toolCalls?.some((tool) => isSearchToolName(tool.name)) ?? false
}

function shouldHideReplyHead(msg: ChatMessage) {
  return hasSearchToolCalls(msg) || shouldShowWebSearchBar(msg)
}

function shouldShowWebSearchBar(msg: ChatMessage) {
  if (!msg.webSearchUsed || hasSearchToolCalls(msg)) {
    return false
  }
  const items = resolveWebSearchItems(msg)
  return Boolean(msg.streaming) || items.length > 0
}

function syncCapabilityToggles() {
  if (!showDeepThinkingToggle.value) {
    deepThinkingEnabled.value = false
  }
  if (!showWebSearchToggle.value) {
    webSearchEnabled.value = false
  }
}

function getPreviousUserQuery(msg: ChatMessage) {
  const index = messages.value.findIndex((item) => item.id === msg.id)
  for (let cursor = index - 1; cursor >= 0; cursor -= 1) {
    const candidate = messages.value[cursor]
    if (candidate.role === 'user') {
      return candidate.content.replace(/\n\n\[附件:.*$/, '').trim()
    }
  }
  return ''
}

function resolveWebSearchItems(msg: ChatMessage): SearchResultItem[] {
  if (msg.webSearchSources?.length) {
    return toSearchResultItems(msg.webSearchSources)
  }
  const toolSources = collectSearchSources(msg.toolCalls)
  if (toolSources.length) {
    return toolSources
  }
  if (msg.webSearchUsed && msg.content) {
    return extractSearchItemsFromContent(msg.content)
  }
  return []
}

function collectSearchSources(toolCalls?: ToolCallRecord[]): SearchResultItem[] {
  if (!toolCalls?.length) {
    return []
  }
  const sources: SearchResultItem[] = []
  for (const tool of toolCalls) {
    if (!isSearchToolName(tool.name) || !tool.result) {
      continue
    }
    sources.push(...parseSearchToolResult(tool.result).items)
  }
  return sources
}

function renderAssistantContent(content: string, msg: ChatMessage) {
  const html = renderMarkdown(content)
  return injectSearchCitations(html, resolveWebSearchItems(msg))
}

function updateWebSearchSources(assistantMessageId: number, sources: WebSearchSourceItem[]) {
  if (!sources.length) {
    return
  }
  patchMessage(assistantMessageId, { webSearchSources: sources })
  scrollToBottom()
}

function patchMessage(id: number, patch: Partial<ChatMessage>) {
  const index = messages.value.findIndex((item) => item.id === id)
  if (index < 0) return
  messages.value[index] = { ...messages.value[index], ...patch }
}

function isThinkingPaused(msg: ChatMessage) {
  return Boolean(msg.thinkingComplete)
}

function shouldShowThinking(msg: ChatMessage) {
  return Boolean(
    msg.deepThinkingRequested
    || msg.deepThinkingUsed
    || msg.thinkingContent?.trim(),
  )
}

function isThinkingLoading(msg: ChatMessage) {
  if (!shouldShowThinking(msg)) {
    return false
  }
  return !isThinkingPaused(msg)
}

function showInitialLoading(msg: ChatMessage) {
  return Boolean(
    msg.streaming
    && !msg.content
    && !shouldShowThinking(msg)
    && !(msg.toolCalls?.length),
  )
}

function showReplyHead(msg: ChatMessage) {
  return Boolean(
    msg.content
    && (msg.streaming || hasReplyMeta(msg))
    && !shouldHideReplyHead(msg)
    && !shouldShowThinking(msg),
  )
}

function getThinkingTitle(msg: ChatMessage) {
  if (!msg.thinkingComplete) {
    return '思考中......'
  }
  if (!msg.thinkingContent?.trim()) {
    return '已思考'
  }
  const start = msg.thinkingStartedAt || Date.now()
  const end = msg.thinkingFinishedAt || Date.now()
  const seconds = Math.max(1, Math.round((end - start) / 1000))
  return `已思考 (用时 ${seconds} 秒)`
}

function markThinkingComplete(msg: ChatMessage | undefined) {
  if (!msg || msg.thinkingComplete) {
    return
  }
  if (!shouldShowThinking(msg)) {
    return
  }
  patchMessage(msg.id, {
    thinkingComplete: true,
    thinkingFinishedAt: Date.now(),
    deepThinkingUsed: true,
  })
}

function finalizeAssistantMessage(assistantMessageId: number) {
  const target = messages.value.find((item) => item.id === assistantMessageId)
  if (!target || target.role !== 'assistant') {
    return
  }
  markThinkingComplete(target)
  if (target.streaming) {
    patchMessage(assistantMessageId, { streaming: false })
  }
}

function hasReplyMeta(msg: ChatMessage) {
  return Boolean(msg.meta?.includes('tokens'))
}

function formatHistoryTime(value?: string) {
  if (!value) return '—'
  return formatDateTime(value)
}

async function openHistory() {
  historyOpen.value = true
  await loadHistory()
}

function mergeCurrentSession(remote: ConversationItem[]): ConversationItem[] {
  const list = [...remote]
  const userMessages = messages.value.filter((item) => item.role === 'user' && item.content && !item.streaming)
  if (!userMessages.length) {
    return list
  }
  const exists = list.some((item) => item.conversationKey === conversationId.value)
  if (exists) {
    return list
  }
  const lastUser = userMessages[userMessages.length - 1]
  const preview = lastUser.content.length > 80 ? `${lastUser.content.slice(0, 80)}...` : lastUser.content
  list.unshift({
    id: 0,
    conversationKey: conversationId.value,
    channel: 'debug',
    messageCount: messages.value.filter((item) => !item.streaming).length,
    preview: `${preview}（当前会话）`,
    lastMessageAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
  })
  return list
}

async function loadHistory() {
  if (!props.agentId) return
  historyLoading.value = true
  try {
    const res = await fetchDebugConversations(props.agentId, { page: 1, pageSize: 50 })
    historyList.value = mergeCurrentSession(res.data.data?.list ?? [])
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载会话历史失败')
  } finally {
    historyLoading.value = false
  }
}

async function selectConversation(item: ConversationItem) {
  if (!props.agentId) return
  historyLoading.value = true
  try {
    const res = await fetchDebugConversationMessages(props.agentId, item.conversationKey)
    conversationId.value = item.conversationKey
    messages.value = res.data.data.map((msg, index) => ({
      id: index + 1,
      role: msg.role,
      content: msg.content,
      sources: msg.sources,
      meta: msg.role === 'assistant' && msg.tokensUsed
        ? `${msg.tokensUsed} tokens · ${msg.latencyMs || 0}ms`
        : undefined,
    }))
    seq = messages.value.length + 1
    debugMode.value = false
    saveSession()
    historyOpen.value = false
    await scrollToBottom()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载会话消息失败')
  } finally {
    historyLoading.value = false
  }
}

async function startNewConversation() {
  historyOpen.value = false
  await resetChat()
}

function toggleLayout() {
  emit('toggleLayout', !props.wide)
}

function openSource(source: RetrievalSourceItem) {
  if (!source.knowledgeBaseId || !source.documentId) {
    message.warning('来源文档信息不完整')
    return
  }
  router.push({
    path: `/knowledge/${source.knowledgeBaseId}`,
    query: { highlightDoc: String(source.documentId) },
  })
}

function saveSession() {
  if (!props.agentId) return
  const payload: DebugSession = {
    conversationId: conversationId.value,
    messages: messages.value.filter((item) => !item.streaming),
    debugMode: debugMode.value,
    seq,
    deepThinkingEnabled: deepThinkingEnabled.value,
    webSearchEnabled: webSearchEnabled.value,
  }
  sessionStorage.setItem(storageKey(props.agentId), JSON.stringify(payload))
}

function restoreSession(agentId: number) {
  const raw = sessionStorage.getItem(storageKey(agentId))
  if (!raw) return false
  try {
    const data = JSON.parse(raw) as DebugSession
    conversationId.value = data.conversationId || createConversationId()
    messages.value = (data.messages || []).map((msg) => {
      if (msg.role === 'assistant' && !msg.streaming && (msg.thinkingContent || msg.deepThinkingUsed)) {
        return {
          ...msg,
          thinkingComplete: msg.thinkingComplete ?? true,
          deepThinkingUsed: msg.deepThinkingUsed ?? Boolean(msg.thinkingContent),
        }
      }
      return msg
    })
    debugMode.value = data.debugMode ?? true
    deepThinkingEnabled.value = data.deepThinkingEnabled ?? false
    webSearchEnabled.value = data.webSearchEnabled ?? false
    seq = data.seq ?? messages.value.length + 1
    return messages.value.length > 0
  } catch {
    return false
  }
}

function clearTypewriter() {
  if (typewriterTimer !== null) {
    clearInterval(typewriterTimer)
    typewriterTimer = null
  }
  tokenBuffer = ''
  activeAssistantMessageId = null
}

function startTypewriter(assistantMessageId: number) {
  activeAssistantMessageId = assistantMessageId
  if (typewriterTimer !== null) return

  typewriterTimer = setInterval(() => {
    if (!tokenBuffer.length || activeAssistantMessageId === null) {
      if (!tokenBuffer.length && typewriterTimer !== null) {
        clearInterval(typewriterTimer)
        typewriterTimer = null
      }
      return
    }

    const chunkSize = tokenBuffer.length > 24 ? 2 : 1
    const chunk = tokenBuffer.slice(0, chunkSize)
    tokenBuffer = tokenBuffer.slice(chunkSize)

    const target = messages.value.find((item) => item.id === activeAssistantMessageId)
    if (target) {
      target.content += chunk
      scrollToBottom()
    }
  }, TYPEWRITER_INTERVAL_MS)
}

function appendThinkingToken(token: string, assistantMessageId: number) {
  const target = messages.value.find((item) => item.id === assistantMessageId)
  if (!target?.deepThinkingRequested) {
    return
  }
  patchMessage(assistantMessageId, {
    deepThinkingUsed: true,
    thinkingStartedAt: target.thinkingStartedAt || Date.now(),
    thinkingContent: (target.thinkingContent || '') + token,
  })
  scrollToBottom()
}

function enqueueToken(token: string, assistantMessageId: number) {
  const target = messages.value.find((item) => item.id === assistantMessageId)
  markThinkingComplete(target)
  tokenBuffer += token
  startTypewriter(assistantMessageId)
}

function flushTypewriter(assistantMessageId: number) {
  if (tokenBuffer.length) {
    const target = messages.value.find((item) => item.id === assistantMessageId)
    if (target) {
      target.content += tokenBuffer
    }
    tokenBuffer = ''
  }
  clearTypewriter()
}

async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

async function loadWelcome() {
  if (!props.agentId) return
  messages.value = []
  loading.value = true
  try {
    const res = await fetchAgentDebugWelcome(props.agentId)
    const data = res.data.data
    messages.value.push({
      id: seq++,
      role: 'assistant',
      content: data.reply,
      meta: data.debugMode ? '调试模式' : undefined,
    })
    debugMode.value = data.debugMode
    modelCapabilities.value = data.modelCapabilities || null
    activeModelLabel.value = formatModelLabel(data.modelName, data.providerName)
    syncCapabilityToggles()
    saveSession()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载欢迎语失败')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

async function initPanel() {
  if (!props.agentId) return
  clearTypewriter()
  input.value = ''
  modelCapabilities.value = null
  activeModelLabel.value = ''
  deepThinkingEnabled.value = false
  webSearchEnabled.value = false
  const restored = restoreSession(props.agentId)
  if (!restored) {
    conversationId.value = createConversationId()
    await loadWelcome()
  } else {
    await scrollToBottom()
    fetchAgentDebugWelcome(props.agentId)
      .then((res) => {
        modelCapabilities.value = res.data.data.modelCapabilities || null
        activeModelLabel.value = formatModelLabel(res.data.data.modelName, res.data.data.providerName)
        syncCapabilityToggles()
      })
      .catch(() => {})
  }
}

async function resetChat() {
  if (props.agentId && conversationId.value) {
    try {
      await clearAgentDebugConversation(props.agentId, conversationId.value)
    } catch {
      // ignore cleanup failure
    }
    sessionStorage.removeItem(storageKey(props.agentId))
  }
  abortController?.abort()
  clearTypewriter()
  conversationId.value = createConversationId()
  input.value = ''
  await loadWelcome()
}

function appendToolCall(assistantMessageId: number, toolName: string, toolArgs: string) {
  const target = messages.value.find((item) => item.id === assistantMessageId)
  if (!target) return
  const toolCalls = [...(target.toolCalls || []), { name: toolName, args: toolArgs }]
  patchMessage(assistantMessageId, {
    toolCalls,
    deepThinkingUsed: target.deepThinkingUsed || target.deepThinkingRequested,
  })
  if (target.thinkingContent?.trim()) {
    markThinkingComplete(target)
  }
  scrollToBottom()
}

function completeToolCall(assistantMessageId: number, toolName: string, toolResult: string) {
  const target = messages.value.find((item) => item.id === assistantMessageId)
  if (!target?.toolCalls?.length) return
  const toolCalls = target.toolCalls.map((tool) =>
    tool.name === toolName && !tool.result ? { ...tool, result: toolResult } : tool,
  )
  patchMessage(assistantMessageId, { toolCalls })
  scrollToBottom()
}

function onPickAttachment() {
  fileInputRef.value?.click()
}

async function onAttachmentSelected(event: Event) {
  const inputEl = event.target as HTMLInputElement
  const file = inputEl.files?.[0]
  if (!file || !props.agentId) return
  try {
    attachment.value = await uploadDebugAttachment(props.agentId, file)
    message.success(`已添加附件：${attachment.value.fileName}`)
  } catch (e) {
    message.error(e instanceof Error ? e.message : '附件上传失败')
  } finally {
    inputEl.value = ''
  }
}

async function onSend() {
  const text = input.value.trim()
  if (!text || !props.agentId || loading.value) return

  input.value = ''
  const currentAttachment = attachment.value
  attachment.value = null
  await nextTick()

  const displayText = currentAttachment
    ? `${text}\n\n[附件: ${currentAttachment.fileName}]`
    : text
  messages.value.push({ id: seq++, role: 'user', content: displayText })
  saveSession()
  loading.value = true
  scrollToBottom()

  const assistantMessageId = seq++
  streamingMessageId.value = assistantMessageId
  messages.value.push({
    id: assistantMessageId,
    role: 'assistant',
    content: '',
    streaming: true,
    deepThinkingUsed: deepThinkingEnabled.value,
    deepThinkingRequested: deepThinkingEnabled.value,
    thinkingExpanded: true,
    thinkingStartedAt: deepThinkingEnabled.value ? Date.now() : undefined,
    thinkingComplete: false,
    webSearchUsed: webSearchEnabled.value && showWebSearchToggle.value,
    webSearchSources: [],
  })

  abortController?.abort()
  abortController = new AbortController()
  clearTypewriter()

  try {
    await streamAgentDebugChat(
      props.agentId,
      text,
      conversationId.value,
      {
        onToken: (token) => {
          enqueueToken(token, assistantMessageId)
        },
        onThinkingToken: (token) => {
          appendThinkingToken(token, assistantMessageId)
        },
        onToolCall: (toolName, toolArgs) => {
          appendToolCall(assistantMessageId, toolName, toolArgs)
        },
        onToolResult: (toolName, toolResult) => {
          completeToolCall(assistantMessageId, toolName, toolResult)
        },
        onWebSearch: (sources) => {
          updateWebSearchSources(assistantMessageId, sources)
        },
        onDone: async (data) => {
          flushTypewriter(assistantMessageId)
          const target = messages.value.find((item) => item.id === assistantMessageId)
          if (target) {
            const webSearchSources = data.webSearchSources?.length
              ? data.webSearchSources
              : target.webSearchSources
            patchMessage(assistantMessageId, {
              content: data.reply || target.content,
              thinkingContent: data.thinking || target.thinkingContent,
              streaming: false,
              meta: formatReplyMeta(data.tokensUsed || 0, data.latencyMs || 0, data.modelName),
              sources: data.sources,
              webSearchSources,
            })
            markThinkingComplete(messages.value.find((item) => item.id === assistantMessageId))
          }
          debugMode.value = data.debugMode
          saveSession()
          if (historyOpen.value) {
            await loadHistory()
          }
        },
        onError: (error) => {
          message.error(error.message)
        },
      },
      abortController.signal,
      {
        enableDeepThinking: deepThinkingEnabled.value,
        enableWebSearch: webSearchEnabled.value,
        attachmentName: currentAttachment?.fileName,
        attachmentContext: currentAttachment?.content,
      },
    )
  } catch (e) {
    if (e instanceof Error && e.name === 'AbortError') {
      return
    }
    clearTypewriter()
    const target = messages.value.find((item) => item.id === assistantMessageId)
    if (target && !target.content) {
      messages.value = messages.value.filter((item) => item.id !== assistantMessageId)
    } else if (target) {
      finalizeAssistantMessage(assistantMessageId)
    }
    saveSession()
    message.error(e instanceof Error ? e.message : '发送失败')
  } finally {
    if (loading.value) {
      finalizeAssistantMessage(assistantMessageId)
      loading.value = false
      streamingMessageId.value = null
      scrollToBottom()
    }
  }
}

function stopGeneration() {
  if (!loading.value) return
  const assistantId = streamingMessageId.value
  abortController?.abort()
  abortController = null
  clearTypewriter()
  if (assistantId != null) {
    flushTypewriter(assistantId)
    const target = messages.value.find((item) => item.id === assistantId)
    if (target) {
      const hasContent = Boolean(
        target.content?.trim()
          || target.thinkingContent?.trim()
          || target.toolCalls?.length,
      )
      if (!hasContent) {
        messages.value = messages.value.filter((item) => item.id !== assistantId)
      } else {
        finalizeAssistantMessage(assistantId)
      }
    }
  }
  loading.value = false
  streamingMessageId.value = null
  saveSession()
  scrollToBottom()
}

watch(() => props.agentId, (id) => {
  if (id) {
    abortController?.abort()
    initPanel()
  }
}, { immediate: true })

onMounted(scrollToBottom)
onUnmounted(() => {
  abortController?.abort()
  clearTypewriter()
})
</script>

<style scoped>
.debug-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  border-left: 1px solid #f0f0f0;
  background: #fafafa;
  --debug-inline-padding: 16px;
}

.debug-panel--drawer {
  min-height: 480px;
  --debug-inline-padding: 60px;
}

.debug-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px var(--debug-inline-padding);
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.title {
  font-weight: 600;
  display: inline-flex;
  align-items: center;
}

.subtitle {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px var(--debug-inline-padding);
  display: flex;
  flex-direction: column;
  gap: 12px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.message-list::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.message {
  display: flex;
  flex-direction: column;
  max-width: 90%;
}

.message.user {
  align-self: flex-end;
  align-items: flex-end;
}

.message.assistant {
  align-self: flex-start;
  align-items: flex-start;
  max-width: 100%;
}

.bubble {
  padding: 10px 14px;
  white-space: pre-wrap;
  line-height: 1.5;
  font-size: 14px;
}

.user-bubble {
  background: #1677ff;
  color: #fff;
  border-radius: 12px;
}

.assistant-content-wrap {
  display: flex;
  align-items: flex-end;
  gap: 2px;
}

.assistant-content {
  padding: 4px 0;
  line-height: 1.65;
  font-size: 14px;
  color: #1e293b;
}

.assistant-content.markdown-body :deep(p) {
  margin: 0 0 0.75em;
}

.assistant-content.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.assistant-content.markdown-body :deep(ul),
.assistant-content.markdown-body :deep(ol) {
  margin: 0 0 0.75em;
  padding-left: 1.25em;
}

.assistant-content.markdown-body :deep(li) {
  margin: 0.25em 0;
}

.assistant-content.markdown-body :deep(strong) {
  font-weight: 600;
  color: #0f172a;
}

.assistant-content.markdown-body :deep(a) {
  color: #1677ff;
  text-decoration: none;
}

.assistant-content.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.assistant-content.markdown-body :deep(h1),
.assistant-content.markdown-body :deep(h2),
.assistant-content.markdown-body :deep(h3) {
  margin: 0.5em 0 0.35em;
  font-weight: 600;
  color: #0f172a;
}

.assistant-content.markdown-body :deep(code) {
  padding: 0.1em 0.35em;
  border-radius: 4px;
  background: #f1f5f9;
  font-size: 0.92em;
}

.assistant-content.markdown-body :deep(.search-citation) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  margin: 0 1px;
  padding: 0 2px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1;
  text-decoration: none;
  vertical-align: super;
  transition: background 0.15s ease, color 0.15s ease;
}

.assistant-content.markdown-body :deep(a.search-citation:hover) {
  background: #e2e8f0;
  color: #64748b;
  text-decoration: none;
}

.assistant-loading {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  min-height: 48px;
}

.assistant-reply-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0 6px;
}

.assistant-reply-head__label {
  font-size: 13px;
  color: #64748b;
}

.thinking-label {
  font-size: 14px;
  color: #64748b;
  letter-spacing: 0.02em;
}

.input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  padding: 0;
}

.input-toolbar__left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.capability-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #fff;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.capability-btn:hover:not(:disabled) {
  border-color: #91caff;
  color: #1677ff;
}

.capability-btn.active {
  border-color: #69b1ff;
  background: #e6f4ff;
  color: #1677ff;
  box-shadow: inset 0 0 0 1px rgba(22, 119, 255, 0.12);
}

.capability-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.input-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

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

.meta {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
  cursor: help;
}

.debug-panel--wide .assistant-content {
  max-width: none;
}

.debug-panel--wide .message.assistant {
  max-width: 100%;
}

.input-area {
  padding: 12px var(--debug-inline-padding) 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.input-label {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.history-actions {
  margin-bottom: 12px;
}

.history-empty-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.history-item:hover,
.history-item.active {
  border-color: #91caff;
  background: #f0f7ff;
}

.history-preview {
  font-size: 13px;
  color: #1f2937;
  line-height: 1.4;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: #94a3b8;
}

.input-box-shell {
  position: relative;
  border-radius: 26px;
  padding: 2px;
  background: transparent;
  transition: background 0.25s ease;
}

.input-box-shell--streaming {
  background: linear-gradient(
    90deg,
    #ff6b6b,
    #feca57,
    #48dbfb,
    #a29bfe,
    #ff9ff3,
    #ff6b6b
  );
  background-size: 300% 100%;
  animation: rainbow-marquee 2.2s linear infinite;
  box-shadow: 0 0 18px rgba(99, 102, 241, 0.18);
}

@keyframes rainbow-marquee {
  0% {
    background-position: 0% 50%;
  }

  100% {
    background-position: 300% 50%;
  }
}

.input-box {
  position: relative;
  z-index: 1;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  padding: 12px 14px 10px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  overflow: visible;
}

.input-box-shell--streaming .input-box {
  border-color: transparent;
  box-shadow: none;
}

.input-box--focused {
  border-color: #c7d2fe;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
}

.input-box--disabled {
  opacity: 0.92;
}

.input-box :deep(.ant-input) {
  padding: 0;
  resize: none;
  border: none;
  box-shadow: none !important;
  background: transparent;
  font-size: 14px;
  line-height: 1.6;
  color: #1e293b;
}

.input-box :deep(.ant-input::placeholder) {
  color: #94a3b8;
}

.input-box :deep(.ant-input:disabled) {
  color: #94a3b8;
  cursor: not-allowed;
}

.input-icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 8px;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
}

.input-icon-btn:disabled {
  color: #94a3b8;
  cursor: not-allowed;
}

.hidden-file-input {
  display: none;
}

.attachment-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
}

.attachment-chip__remove {
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
}

.send-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: #b8c5ff;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.15s ease;
}

.send-btn:not(:disabled):hover {
  background: #a5b4fc;
}

.send-btn:not(:disabled):active {
  transform: scale(0.96);
}

.send-btn--stop {
  background: #ef4444;
}

.send-btn--stop:hover {
  background: #dc2626;
}

.send-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}
</style>
