<template>
  <div class="portal-chat-panel">
    <div ref="messageListRef" class="portal-chat-panel__messages">
      <a-spin v-if="loading" class="welcome-spin" />
      <div v-else-if="!messages.length" class="portal-chat-panel__empty">
        <p class="watermark">对话由 AI 生成</p>
        <p class="hint">在下方输入消息，开始与助手对话</p>
      </div>

      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-row"
        :class="msg.role"
      >
        <div v-if="msg.role === 'user'" class="bubble user-bubble">{{ msg.content }}</div>
        <template v-else>
          <div v-if="msg.streaming && !msg.content" class="assistant-loading">
            <span class="dot-pulse" />
            正在思考...
          </div>
          <div v-if="msg.content" class="assistant-content markdown-body" v-html="renderMarkdown(msg.content)" />
          <div v-if="msg.meta" class="message-meta">{{ msg.meta }}</div>
        </template>
      </div>
    </div>

    <div class="portal-chat-panel__input-area">
      <div
        class="input-box"
        :class="{ 'input-box--focused': inputFocused, 'input-box--disabled': sending || !ready }"
      >
        <a-textarea
          v-model:value="input"
          :auto-size="{ minRows: 1, maxRows: 6 }"
          :bordered="false"
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          :disabled="sending || !ready"
          @focus="inputFocused = true"
          @blur="inputFocused = false"
          @keydown.enter.exact.prevent="onSend"
        />
        <div class="input-toolbar">
          <div class="input-toolbar__left" />
          <button
            type="button"
            class="send-btn"
            :disabled="sending || !ready || !input.trim()"
            aria-label="发送"
            @click="onSend"
          >
            <ArrowUpOutlined />
          </button>
        </div>
      </div>
    </div>

    <a-alert v-if="errorMessage" type="error" :message="errorMessage" show-icon class="portal-chat-panel__error" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onUnmounted, ref, watch } from 'vue'
import { ArrowUpOutlined } from '@ant-design/icons-vue'
import { fetchAgentDebugWelcome, streamAgentDebugChat } from '@/api/agent'
import { fetchPortalApp } from '@/api/portal'
import { renderMarkdown } from '@/utils/markdown'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
  meta?: string
}

const props = defineProps<{
  applicationId: number | null
}>()

const emit = defineEmits<{
  loaded: [payload: { appName: string; description?: string; agentName?: string }]
  error: [message: string]
}>()

const input = ref('')
const inputFocused = ref(false)
const messages = ref<ChatMessage[]>([])
const sending = ref(false)
const loading = ref(false)
const ready = ref(false)
const errorMessage = ref('')
const agentId = ref<number | null>(null)
const conversationId = ref('')
const messageListRef = ref<HTMLElement>()
let seq = 1
let abortController: AbortController | null = null

function scrollToBottom() {
  nextTick(() => {
    const el = messageListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function formatMeta(tokens?: number, latencyMs?: number) {
  const parts: string[] = []
  if (tokens) parts.push(`${tokens} tokens`)
  if (latencyMs) parts.push(`${latencyMs}ms`)
  return parts.length ? parts.join(' · ') : undefined
}

function resetState() {
  abortController?.abort()
  messages.value = []
  agentId.value = null
  ready.value = false
  errorMessage.value = ''
  input.value = ''
}

async function loadApp(applicationId: number) {
  resetState()
  loading.value = true
  conversationId.value = `portal-${applicationId}-${Date.now()}`
  try {
    const res = await fetchPortalApp(applicationId)
    const detail = res.data.data
    agentId.value = detail.defaultAgentId
    emit('loaded', {
      appName: detail.appName,
      description: detail.description,
      agentName: detail.defaultAgentName,
    })

    const welcomeRes = await fetchAgentDebugWelcome(detail.defaultAgentId)
    if (welcomeRes.data.data.reply) {
      messages.value.push({ id: seq++, role: 'assistant', content: welcomeRes.data.data.reply })
    }
    ready.value = true
  } catch (e) {
    const message = e instanceof Error ? e.message : '加载应用失败'
    errorMessage.value = message
    emit('error', message)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function startNewConversation() {
  if (!props.applicationId) return
  loadApp(props.applicationId)
}

async function onSend() {
  const text = input.value.trim()
  if (!text || sending.value || !ready.value || !agentId.value) return
  input.value = ''
  errorMessage.value = ''
  messages.value.push({ id: seq++, role: 'user', content: text })
  sending.value = true
  scrollToBottom()

  const assistantId = seq++
  messages.value.push({ id: assistantId, role: 'assistant', content: '', streaming: true })
  abortController?.abort()
  abortController = new AbortController()

  try {
    await streamAgentDebugChat(
      agentId.value,
      text,
      conversationId.value,
      {
        onToken: (token) => {
          const target = messages.value.find((item) => item.id === assistantId)
          if (target) target.content += token
          scrollToBottom()
        },
        onDone: (data) => {
          const target = messages.value.find((item) => item.id === assistantId)
          if (target) {
            target.streaming = false
            target.meta = formatMeta(data.tokensUsed, data.latencyMs)
          }
        },
        onError: (error) => {
          errorMessage.value = error.message
        },
      },
      abortController.signal,
    )
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '发送失败'
    messages.value = messages.value.filter((item) => item.id !== assistantId)
  } finally {
    const target = messages.value.find((item) => item.id === assistantId)
    if (target) target.streaming = false
    sending.value = false
    scrollToBottom()
  }
}

watch(
  () => props.applicationId,
  (id) => {
    if (id) {
      loadApp(id)
    } else {
      resetState()
      loading.value = false
    }
  },
  { immediate: true },
)

onUnmounted(() => abortController?.abort())

defineExpose({ startNewConversation })
</script>

<style scoped>
.portal-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: var(--bg-subtle);
}

.portal-chat-panel__messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px 16px;
}

.portal-chat-panel__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  text-align: center;
}

.watermark {
  margin: 0;
  font-size: 28px;
  font-weight: 500;
  color: rgba(15, 23, 42, 0.08);
  letter-spacing: 0.04em;
}

.hint {
  margin: 12px 0 0;
  font-size: 14px;
  color: var(--text-muted);
}

.welcome-spin {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}

.message-row {
  margin-bottom: 20px;
}

.message-row.user {
  display: flex;
  justify-content: flex-end;
}

.user-bubble {
  max-width: min(720px, 85%);
  padding: 12px 16px;
  border-radius: 16px 16px 4px 16px;
  background: var(--primary);
  color: #fff;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.assistant-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.dot-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary);
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.35; transform: scale(0.9); }
  50% { opacity: 1; transform: scale(1); }
}

.assistant-content {
  max-width: min(820px, 100%);
  line-height: 1.65;
  font-size: 15px;
  color: var(--text-primary);
}

.message-meta {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.portal-chat-panel__input-area {
  padding: 12px 32px 24px;
  background: linear-gradient(180deg, transparent 0%, var(--bg-subtle) 24%);
}

.input-box {
  max-width: 820px;
  margin: 0 auto;
  background: var(--card-bg);
  border: 1px solid var(--border-strong);
  border-radius: 20px;
  padding: 14px 16px 10px;
  box-shadow: var(--card-shadow);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-box--focused {
  border-color: var(--primary);
  box-shadow: 0 4px 24px var(--auth-focus-ring);
}

.input-box :deep(.ant-input) {
  padding: 0;
  resize: none;
  border: none;
  box-shadow: none !important;
  background: transparent;
  font-size: 15px;
  line-height: 1.6;
}

.input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.send-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.15s;
}

.send-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.send-btn:not(:disabled):hover {
  transform: scale(1.04);
}

.portal-chat-panel__error {
  margin: 0 32px 12px;
  max-width: 820px;
  margin-left: auto;
  margin-right: auto;
}
</style>
