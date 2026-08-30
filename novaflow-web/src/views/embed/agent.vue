<template>
  <div class="embed-chat">
    <header class="embed-header">
      <div>
        <h1>{{ agentName || 'AI 助手' }}</h1>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
      <a-button v-if="messages.length" type="text" :disabled="sending" @click="resetConversation">新对话</a-button>
    </header>

    <div ref="messageListRef" class="embed-messages">
      <a-spin v-if="loadingWelcome" class="welcome-spin" />
      <a-empty v-else-if="!messages.length" description="发送消息开始对话" />

      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-row"
        :class="msg.role"
      >
        <div v-if="msg.role === 'user'" class="bubble user-bubble">{{ msg.content }}</div>

        <template v-else>
          <div v-if="msg.streaming && !msg.content" class="assistant-loading">
            <div class="tech-loader" aria-label="思考中">
              <span class="tech-loader__ring tech-loader__ring--outer" />
              <span class="tech-loader__ring tech-loader__ring--inner" />
              <span class="tech-loader__core">
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path
                    d="M12 3L20 8V16L12 21L4 16V8L12 3Z"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linejoin="round"
                  />
                  <circle cx="12" cy="12" r="2.5" fill="currentColor" />
                </svg>
              </span>
            </div>
            <span class="thinking-label">正在思考......</span>
          </div>

          <div v-else-if="msg.content && msg.streaming" class="assistant-reply-head">
            <div class="tech-loader tech-loader--xs" aria-hidden="true">
              <span class="tech-loader__ring tech-loader__ring--outer" />
              <span class="tech-loader__ring tech-loader__ring--inner" />
              <span class="tech-loader__core">
                <svg viewBox="0 0 24 24" fill="none">
                  <path
                    d="M12 3L20 8V16L12 21L4 16V8L12 3Z"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linejoin="round"
                  />
                  <circle cx="12" cy="12" r="2.5" fill="currentColor" />
                </svg>
              </span>
            </div>
            <span class="assistant-reply-head__label">正在回答......</span>
          </div>

          <div v-if="msg.content" class="assistant-content-wrap">
            <div class="assistant-content markdown-body" v-html="renderMarkdown(msg.content)" />
            <span v-if="msg.streaming" class="cursor">|</span>
          </div>

          <div v-if="msg.meta" class="message-meta">{{ msg.meta }}</div>
        </template>
      </div>
    </div>

    <div class="input-area">
      <div
        class="input-box"
        :class="{ 'input-box--focused': inputFocused, 'input-box--disabled': sending }"
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
          <div class="input-toolbar__actions">
            <button
              type="button"
              class="send-btn"
              :disabled="sending || !ready || !input.trim()"
              aria-label="发送"
              @click="onSend"
            >
              <span v-if="sending" class="tech-loader tech-loader--send" aria-hidden="true">
                <span class="tech-loader__ring tech-loader__ring--outer" />
                <span class="tech-loader__ring tech-loader__ring--inner" />
                <span class="tech-loader__core">
                  <svg viewBox="0 0 24 24" fill="none">
                    <path
                      d="M12 3L20 8V16L12 21L4 16V8L12 3Z"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linejoin="round"
                    />
                    <circle cx="12" cy="12" r="2.5" fill="currentColor" />
                  </svg>
                </span>
              </span>
              <ArrowUpOutlined v-else />
            </button>
          </div>
        </div>
      </div>
    </div>

    <a-alert v-if="errorMessage" type="error" :message="errorMessage" show-icon class="embed-error" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { ArrowUpOutlined } from '@ant-design/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchOpenAgentWelcome,
  streamOpenAgentChat,
} from '@/api/agent'
import { renderMarkdown } from '@/utils/markdown'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
  meta?: string
}

const TYPEWRITER_INTERVAL_MS = 18

const route = useRoute()
const router = useRouter()
const agentId = Number(route.params.id)
const storageKey = `novaflow_embed_key_${agentId}`

function resolveApiKey(): string {
  const fromQuery = String(route.query.apiKey || route.query.key || '')
  const fromStorage = sessionStorage.getItem(storageKey) || ''
  const key = fromQuery || fromStorage
  if (fromQuery) {
    sessionStorage.setItem(storageKey, fromQuery)
    const nextQuery = { ...route.query }
    delete nextQuery.apiKey
    delete nextQuery.key
    router.replace({ query: nextQuery })
  }
  return key
}

const apiKey = ref(resolveApiKey())
const subtitle = route.query.subtitle ? String(route.query.subtitle) : ''

const agentName = ref('')
const input = ref('')
const inputFocused = ref(false)
const messages = ref<ChatMessage[]>([])
const sending = ref(false)
const loadingWelcome = ref(false)
const ready = ref(false)
const errorMessage = ref('')
const conversationId = ref(`embed-${agentId}-${Date.now()}`)
const messageListRef = ref<HTMLElement>()
let seq = 1
let abortController: AbortController | null = null
let typewriterTimer: ReturnType<typeof setInterval> | null = null
let tokenBuffer = ''
let activeAssistantMessageId: number | null = null

function scrollToBottom() {
  nextTick(() => {
    const el = messageListRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
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

function enqueueToken(token: string, assistantMessageId: number) {
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

function formatMeta(tokens?: number, latencyMs?: number) {
  const parts: string[] = []
  if (tokens) parts.push(`${tokens} tokens`)
  if (latencyMs) parts.push(`${latencyMs}ms`)
  return parts.length ? parts.join(' · ') : undefined
}

async function loadWelcome() {
  if (!agentId || !apiKey.value) {
    errorMessage.value = '缺少 agentId 或 apiKey 参数'
    return
  }
  loadingWelcome.value = true
  errorMessage.value = ''
  try {
    const res = await fetchOpenAgentWelcome(agentId, apiKey.value)
    agentName.value = res.data.data.agentName
    if (res.data.data.reply) {
      messages.value.push({ id: seq++, role: 'assistant', content: res.data.data.reply })
    }
    ready.value = true
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loadingWelcome.value = false
    scrollToBottom()
  }
}

function resetConversation() {
  abortController?.abort()
  clearTypewriter()
  messages.value = []
  conversationId.value = `embed-${agentId}-${Date.now()}`
  loadWelcome()
}

async function onSend() {
  const text = input.value.trim()
  if (!text || sending.value || !ready.value) return
  input.value = ''
  errorMessage.value = ''
  messages.value.push({ id: seq++, role: 'user', content: text })
  sending.value = true
  scrollToBottom()

  const assistantId = seq++
  messages.value.push({ id: assistantId, role: 'assistant', content: '', streaming: true })
  abortController?.abort()
  abortController = new AbortController()
  clearTypewriter()

  try {
    await streamOpenAgentChat(
      agentId,
      apiKey.value,
      text,
      conversationId.value,
      {
        onToken: (token) => {
          enqueueToken(token, assistantId)
        },
        onDone: (data) => {
          flushTypewriter(assistantId)
          const target = messages.value.find((item) => item.id === assistantId)
          if (target) {
            target.content = data.reply || target.content
            target.streaming = false
            target.meta = formatMeta(data.tokensUsed, data.latencyMs)
          }
          if (data.agentName) {
            agentName.value = data.agentName
          }
        },
        onError: (error) => {
          errorMessage.value = error.message
        },
      },
      abortController.signal,
    )
  } catch (e) {
    if (e instanceof Error && e.name === 'AbortError') return
    errorMessage.value = e instanceof Error ? e.message : '发送失败'
    messages.value = messages.value.filter((item) => item.id !== assistantId)
  } finally {
    const target = messages.value.find((item) => item.id === assistantId)
    if (target) {
      flushTypewriter(assistantId)
      target.streaming = false
    }
    sending.value = false
    scrollToBottom()
  }
}

onMounted(loadWelcome)
onUnmounted(() => {
  abortController?.abort()
  clearTypewriter()
})
</script>

<style scoped>
.embed-chat {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f8fafc;
}

.embed-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.embed-header h1 {
  margin: 0;
  font-size: 18px;
}

.embed-header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.embed-messages {
  flex: 1;
  overflow: auto;
  padding: 16px 20px;
}

.welcome-spin {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}

.message-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
  max-width: 100%;
}

.message-row.user {
  align-items: flex-end;
}

.message-row.assistant {
  align-items: flex-start;
  width: 100%;
}

.user-bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  background: #1677ff;
  color: #fff;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
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

.assistant-reply-head__label,
.thinking-label {
  font-size: 13px;
  color: #64748b;
}

.assistant-content-wrap {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  max-width: 100%;
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

.assistant-content.markdown-body :deep(pre) {
  background: #f1f5f9;
  border-radius: 8px;
  padding: 12px;
  overflow-x: auto;
}

.assistant-content.markdown-body :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.9em;
}

.message-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.input-area {
  padding: 12px 16px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.input-box {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  padding: 12px 14px 10px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
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

.input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}

.input-toolbar__left {
  flex: 1;
}

.input-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 4px;
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

.send-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

.tech-loader--send {
  width: 18px;
  height: 18px;
}

.tech-loader--send .tech-loader__ring--inner {
  inset: 3px;
}

.tech-loader--send .tech-loader__core {
  width: 10px;
  height: 10px;
  color: #fff;
  filter: drop-shadow(0 0 4px rgba(255, 255, 255, 0.6));
}

.embed-error {
  margin: 0 20px 16px;
}

.tech-loader {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tech-loader--xs {
  width: 20px;
  height: 20px;
}

.tech-loader--xs .tech-loader__ring--inner {
  inset: 4px;
}

.tech-loader--xs .tech-loader__core {
  width: 10px;
  height: 10px;
}

.tech-loader__ring {
  position: absolute;
  border-radius: 50%;
  border: 2px solid transparent;
}

.tech-loader__ring--outer {
  inset: 0;
  border-top-color: #69b1ff;
  border-right-color: rgba(54, 207, 201, 0.85);
  box-shadow: 0 0 14px rgba(22, 119, 255, 0.25);
  animation: tech-spin 1.1s linear infinite;
}

.tech-loader__ring--inner {
  inset: 7px;
  border-bottom-color: #1677ff;
  border-left-color: rgba(105, 177, 255, 0.7);
  animation: tech-spin 0.75s linear infinite reverse;
}

.tech-loader__core {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  color: #1677ff;
  animation: tech-pulse 1.4s ease-in-out infinite;
}

.tech-loader__core svg {
  width: 100%;
  height: 100%;
}

.cursor {
  display: inline-block;
  color: #1677ff;
  font-weight: 300;
  animation: blink 1s step-end infinite;
  margin-bottom: 2px;
}

@keyframes tech-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes tech-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(0.92);
  }
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>
