<template>
  <div class="debug-panel" data-testid="agent-debug-panel">
    <div class="debug-header">
      <div>
        <div class="title">调试对话</div>
        <div class="subtitle">实时预览 Agent 回复（调试模式）</div>
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
        <div class="bubble">{{ msg.content }}</div>
        <div v-if="msg.meta" class="meta">{{ msg.meta }}</div>
      </div>
      <div v-if="loading" class="message assistant">
        <div class="bubble typing">正在思考...</div>
      </div>
    </div>

    <div class="input-area">
      <a-textarea
        v-model:value="input"
        :rows="3"
        placeholder="输入测试消息，Enter 发送"
        data-testid="debug-input"
        @press-enter.prevent="onSend"
      />
      <a-button type="primary" :loading="loading" data-testid="debug-send" @click="onSend">
        发送
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { debugAgentChat, fetchAgentDebugWelcome } from '@/api/agent'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  meta?: string
}

const props = defineProps<{
  agentId: number | null
}>()

const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
let seq = 1

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
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载欢迎语失败')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function resetChat() {
  loadWelcome()
}

async function onSend() {
  const text = input.value.trim()
  if (!text || !props.agentId || loading.value) return

  messages.value.push({ id: seq++, role: 'user', content: text })
  input.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await debugAgentChat(props.agentId, text)
    const data = res.data.data
    messages.value.push({
      id: seq++,
      role: 'assistant',
      content: data.reply,
      meta: `${data.tokensUsed} tokens · ${data.latencyMs}ms`,
    })
  } catch (e) {
    message.error(e instanceof Error ? e.message : '发送失败')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

watch(() => props.agentId, (id) => {
  if (id) loadWelcome()
}, { immediate: true })

onMounted(scrollToBottom)
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
}

.bubble {
  padding: 10px 14px;
  border-radius: 12px;
  white-space: pre-wrap;
  line-height: 1.5;
  font-size: 14px;
}

.message.user .bubble {
  background: #1677ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message.assistant .bubble {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 4px;
}

.typing {
  color: #64748b;
}

.meta {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.input-area {
  padding: 12px 16px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
