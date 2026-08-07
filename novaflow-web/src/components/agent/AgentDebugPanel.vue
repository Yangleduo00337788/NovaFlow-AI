<template>
  <div class="debug-panel" data-testid="agent-debug-panel">
    <div class="debug-header">
      <div>
        <div class="title">调试对话</div>
        <div class="subtitle">{{ debugMode ? '实时预览 Agent 回复（调试模式）' : '已接入模型引擎，流式返回真实 AI 回复' }}</div>
      </div>
      <a-button size="small" @click="resetChat">清空</a-button>
    </div>

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
          </div>
          <div v-else class="assistant-content">
            {{ msg.content }}<span v-if="msg.streaming" class="cursor">|</span>
          </div>
        </template>
        <div v-if="msg.meta" class="meta">{{ msg.meta }}</div>
      </div>
      <div v-if="loading && !streamingMessageId" class="message assistant">
        <div class="assistant-loading">
          <div class="tech-loader" aria-label="加载中">
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
        </div>
      </div>
    </div>

    <div class="input-area">
      <div
        class="input-box"
        :class="{ 'input-box--focused': inputFocused, 'input-box--disabled': loading }"
      >
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
          <div class="input-toolbar__actions">
            <button
              type="button"
              class="input-icon-btn"
              title="附件（即将支持）"
              disabled
              aria-label="附件"
            >
              <PaperClipOutlined />
            </button>
            <button
              type="button"
              class="send-btn"
              data-testid="debug-send"
              :disabled="loading || !canSend"
              aria-label="发送"
              @click="onSend"
            >
              <span v-if="loading" class="tech-loader tech-loader--send" aria-hidden="true">
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
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ArrowUpOutlined, PaperClipOutlined } from '@ant-design/icons-vue'
import {
  clearAgentDebugConversation,
  fetchAgentDebugWelcome,
  streamAgentDebugChat,
} from '@/api/agent'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  meta?: string
  streaming?: boolean
}

interface DebugSession {
  conversationId: string
  messages: ChatMessage[]
  debugMode: boolean
  seq: number
}

const props = defineProps<{
  agentId: number | null
}>()

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

function saveSession() {
  if (!props.agentId) return
  const payload: DebugSession = {
    conversationId: conversationId.value,
    messages: messages.value.filter((item) => !item.streaming),
    debugMode: debugMode.value,
    seq,
  }
  sessionStorage.setItem(storageKey(props.agentId), JSON.stringify(payload))
}

function restoreSession(agentId: number) {
  const raw = sessionStorage.getItem(storageKey(agentId))
  if (!raw) return false
  try {
    const data = JSON.parse(raw) as DebugSession
    conversationId.value = data.conversationId || createConversationId()
    messages.value = data.messages || []
    debugMode.value = data.debugMode ?? true
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
  if (restoreSession(props.agentId)) {
    await scrollToBottom()
    return
  }
  conversationId.value = createConversationId()
  await loadWelcome()
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

async function onSend() {
  const text = input.value.trim()
  if (!text || !props.agentId || loading.value) return

  input.value = ''
  await nextTick()

  messages.value.push({ id: seq++, role: 'user', content: text })
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
        onDone: async (data) => {
          flushTypewriter(assistantMessageId)
          const target = messages.value.find((item) => item.id === assistantMessageId)
          if (target) {
            target.content = data.reply || target.content
            target.streaming = false
            target.meta = `${data.tokensUsed} tokens · ${data.latencyMs}ms`
          }
          debugMode.value = data.debugMode
          saveSession()
        },
        onError: (error) => {
          message.error(error.message)
        },
      },
      abortController.signal,
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
      target.streaming = false
    }
    saveSession()
    message.error(e instanceof Error ? e.message : '发送失败')
  } finally {
    loading.value = false
    streamingMessageId.value = null
    scrollToBottom()
  }
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
  min-height: 480px;
  border-left: 1px solid #f0f0f0;
  background: #fafafa;
}

.debug-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.title {
  font-weight: 600;
}

.subtitle {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
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

.assistant-content {
  padding: 4px 0;
  white-space: pre-wrap;
  line-height: 1.65;
  font-size: 14px;
  color: #1e293b;
}

.assistant-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 6px 0;
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
  filter: drop-shadow(0 0 6px rgba(22, 119, 255, 0.55));
  animation: tech-pulse 1.4s ease-in-out infinite;
}

.tech-loader__core svg {
  width: 100%;
  height: 100%;
}

@keyframes tech-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes tech-pulse {
  0%,
  100% {
    opacity: 0.75;
    transform: scale(0.92);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
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
  justify-content: flex-end;
  margin-top: 8px;
}

.input-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.input-icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 8px;
  color: #94a3b8;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: not-allowed;
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
</style>
