<template>
  <div class="portal-client">
    <!-- 左侧：Agent / 应用列表 -->
    <aside class="portal-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-top">
        <div class="brand-row">
          <AppLogo variant="sidebar" :collapsed="sidebarCollapsed" />
          <button
            v-if="!sidebarCollapsed"
            type="button"
            class="icon-btn"
            aria-label="收起侧栏"
            @click="sidebarCollapsed = true"
          >
            <MenuFoldOutlined />
          </button>
        </div>
        <p v-if="!sidebarCollapsed" class="brand-subtitle">你的 AI 办公助手</p>

        <div v-if="!sidebarCollapsed" class="sidebar-search">
          <SearchOutlined />
          <input v-model="keyword" type="search" placeholder="搜索应用" />
        </div>
      </div>

      <div class="sidebar-section">
        <div class="section-head">
          <span v-if="!sidebarCollapsed">应用</span>
          <button
            v-else
            type="button"
            class="icon-btn"
            aria-label="展开侧栏"
            @click="sidebarCollapsed = false"
          >
            <MenuUnfoldOutlined />
          </button>
        </div>

        <a-spin :spinning="loadingApps" class="app-list-spin">
          <div v-if="!sidebarCollapsed && !filteredApps.length && !loadingApps" class="empty-apps">
            暂无已发布应用
          </div>
          <button
            v-for="app in filteredApps"
            :key="app.id"
            type="button"
            class="app-item"
            :class="{ active: app.id === selectedAppId }"
            :title="app.appName"
            @click="selectApp(app.id)"
          >
            <span class="app-avatar">{{ appIcon(app) }}</span>
            <span v-if="!sidebarCollapsed" class="app-copy">
              <strong>{{ app.appName }}</strong>
              <span>{{ app.defaultAgentName || 'AI 助手' }}</span>
            </span>
          </button>
        </a-spin>
      </div>

      <div class="sidebar-footer">
        <div class="user-row">
          <span class="user-avatar">{{ userInitial }}</span>
          <div v-if="!sidebarCollapsed" class="user-copy">
            <strong>{{ userName }}</strong>
            <span>{{ tenantName }}</span>
          </div>
          <a-dropdown v-if="!sidebarCollapsed" :trigger="['click']">
            <button type="button" class="icon-btn" aria-label="用户菜单">
              <SettingOutlined />
            </button>
            <template #overlay>
              <a-menu>
                <a-menu-item v-if="canReturnToStudio" @click="goStudio">返回工作台</a-menu-item>
                <a-menu-item @click="onLogout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>
    </aside>

    <!-- 中间：对话主区域 -->
    <main class="portal-main">
      <header class="portal-main__header">
        <div class="agent-head">
          <h1>{{ currentTitle }}</h1>
          <span v-if="selectedAppId" class="online-badge">
            <i />
            在线
          </span>
        </div>
        <a-button
          v-if="selectedAppId"
          type="text"
          :disabled="chatBusy"
          @click="startNewChat"
        >
          新对话
        </a-button>
      </header>

      <div v-if="!selectedAppId" class="portal-main__placeholder">
        <div class="placeholder-card">
          <RobotOutlined class="placeholder-icon" />
          <h2>欢迎使用 NovaFlow</h2>
          <p v-if="apps.length">从左侧选择一个应用，开始与 AI 助手对话</p>
          <p v-else>管理员尚未发布可用应用，请联系管理员在 Studio 中发布应用</p>
        </div>
      </div>

      <PortalChatPanel
        v-else
        ref="chatPanelRef"
        :application-id="selectedAppId"
        @loaded="onChatLoaded"
        @conversation-changed="onConversationChanged"
      />
    </main>

    <aside v-if="historyOpen" class="portal-history" aria-label="历史对话">
      <div class="history-head">
        <strong>我的对话</strong>
        <button type="button" class="icon-btn" aria-label="关闭历史" @click="historyOpen = false">
          ×
        </button>
      </div>
      <a-spin :spinning="loadingHistory">
        <p v-if="!loadingHistory && !historyItems.length" class="history-empty">暂无历史对话</p>
        <button
          v-for="item in historyItems"
          :key="item.conversationKey"
          type="button"
          class="history-item"
          :class="{ active: item.conversationKey === activeConversationKey }"
          @click="openHistoryItem(item.conversationKey)"
        >
          <strong>{{ item.preview || '对话' }}</strong>
          <span>{{ formatHistoryTime(item.lastMessageAt) }}</span>
        </button>
      </a-spin>
    </aside>

    <!-- 右侧：工具栏 -->
    <aside class="portal-rail" aria-label="快捷工具">
      <button
        type="button"
        class="rail-btn"
        :class="{ active: historyOpen }"
        :disabled="!selectedAppId"
        title="历史对话"
        aria-label="历史对话"
        @click="toggleHistory"
      >
        <FolderOutlined />
      </button>
      <button type="button" class="rail-btn" title="帮助（即将上线）">
        <QuestionCircleOutlined />
      </button>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  FolderOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  QuestionCircleOutlined,
  RobotOutlined,
  SearchOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import AppLogo from '@/components/common/AppLogo.vue'
import PortalChatPanel from '@/components/portal/PortalChatPanel.vue'
import { fetchPortalApps, fetchPortalConversationMessages, fetchPortalConversations, type PortalAppItem, type PortalConversationItem } from '@/api/portal'
import { getDefaultHomeByRole, isEndUser, portalAppPath } from '@/config/access'
import { APP_LOGIN_PATH } from '@/config/app'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const apps = ref<PortalAppItem[]>([])
const loadingApps = ref(false)
const keyword = ref('')
const sidebarCollapsed = ref(false)
const selectedAppId = ref<number | null>(null)
const currentTitle = ref('NovaFlow')
const chatPanelRef = ref<InstanceType<typeof PortalChatPanel> | null>(null)
const chatBusy = ref(false)
const historyOpen = ref(false)
const loadingHistory = ref(false)
const historyItems = ref<PortalConversationItem[]>([])
const activeConversationKey = ref('')

const userName = computed(() => auth.user?.nickname || auth.user?.username || '用户')
const tenantName = computed(() => auth.tenant?.tenantName || '')
const userInitial = computed(() => (userName.value[0] || 'U').toUpperCase())
const canReturnToStudio = computed(() => !isEndUser(auth.roleCode))

const filteredApps = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return apps.value
  return apps.value.filter((app) =>
    app.appName.toLowerCase().includes(q)
    || (app.defaultAgentName || '').toLowerCase().includes(q),
  )
})

function appIcon(app: PortalAppItem) {
  if (app.icon) return app.icon
  return app.appType === 'workflow' ? '⚡' : '🤖'
}

function resolveRouteAppId(): number | null {
  const raw = route.params.id
  const id = Number(Array.isArray(raw) ? raw[0] : raw)
  return Number.isFinite(id) && id > 0 ? id : null
}

function selectApp(id: number) {
  if (selectedAppId.value === id && route.name === 'portal-chat') {
    return
  }
  router.push(portalAppPath(id))
}

function onChatLoaded(payload: { appName: string; agentName?: string }) {
  currentTitle.value = payload.agentName || payload.appName
  chatBusy.value = false
}

function startNewChat() {
  chatPanelRef.value?.startNewConversation()
}

function formatHistoryTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

async function refreshHistory() {
  if (!selectedAppId.value) {
    historyItems.value = []
    return
  }
  loadingHistory.value = true
  try {
    const res = await fetchPortalConversations(selectedAppId.value, { page: 1, pageSize: 50 })
    historyItems.value = res.data.data?.list || []
  } catch {
    historyItems.value = []
  } finally {
    loadingHistory.value = false
  }
}

async function toggleHistory() {
  if (!selectedAppId.value) return
  historyOpen.value = !historyOpen.value
  if (historyOpen.value) {
    await refreshHistory()
  }
}

async function openHistoryItem(conversationKey: string) {
  if (!selectedAppId.value) return
  const res = await fetchPortalConversationMessages(selectedAppId.value, conversationKey)
  chatPanelRef.value?.loadHistory(conversationKey, res.data.data || [])
  activeConversationKey.value = conversationKey
}

function onConversationChanged(conversationKey?: string) {
  if (conversationKey) {
    activeConversationKey.value = conversationKey
  }
  if (historyOpen.value) {
    refreshHistory()
  }
}

function goStudio() {
  router.push(getDefaultHomeByRole(auth.roleCode))
}

function onLogout() {
  auth.clear()
  router.push(APP_LOGIN_PATH)
}

async function loadApps() {
  loadingApps.value = true
  try {
    const res = await fetchPortalApps()
    apps.value = res.data.data || []
    const routeId = resolveRouteAppId()
    if (routeId && apps.value.some((item) => item.id === routeId)) {
      selectedAppId.value = routeId
    } else if (!selectedAppId.value && apps.value.length) {
      router.replace(portalAppPath(apps.value[0].id))
    }
  } finally {
    loadingApps.value = false
  }
}

watch(
  () => route.params.id,
  (id) => {
    const parsed = Number(Array.isArray(id) ? id[0] : id)
    selectedAppId.value = Number.isFinite(parsed) && parsed > 0 ? parsed : null
    if (selectedAppId.value) {
      const app = apps.value.find((item) => item.id === selectedAppId.value)
      currentTitle.value = app?.defaultAgentName || app?.appName || 'AI 助手'
      if (historyOpen.value) {
        refreshHistory()
      }
    } else {
      currentTitle.value = 'NovaFlow'
      historyItems.value = []
      activeConversationKey.value = ''
    }
  },
  { immediate: true },
)

onMounted(loadApps)
</script>

<style scoped>
.portal-client {
  display: flex;
  height: 100vh;
  min-height: 0;
  background: var(--bg);
}

.portal-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--card-bg);
  border-right: 1px solid var(--border);
  transition: width 0.2s ease;
}

.portal-sidebar.collapsed {
  width: 72px;
}

.sidebar-top {
  padding: 18px 16px 12px;
}

.brand-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.brand-subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.sidebar-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding: 8px 12px;
  border-radius: 10px;
  background: var(--bg-subtle);
  color: var(--text-muted);
}

.sidebar-search input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: var(--text-primary);
}

.sidebar-section {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 0 10px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 8px 10px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--text-muted);
}

.app-list-spin {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.empty-apps {
  padding: 16px 8px;
  font-size: 13px;
  color: var(--text-muted);
}

.app-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  margin-bottom: 4px;
  border: none;
  border-radius: 12px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s;
}

.app-item:hover,
.app-item.active {
  background: var(--hover-bg);
}

.app-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #eef2ff, #fae8ff);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.app-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.app-copy strong {
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-copy span {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-footer {
  padding: 12px 14px 16px;
  border-top: 1px solid var(--border);
}

.user-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.user-copy {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.user-copy strong {
  font-size: 13px;
  color: var(--text-primary);
}

.user-copy span {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-btn:hover {
  background: var(--hover-bg);
}

.portal-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.portal-main__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px 8px;
  background: var(--bg-subtle);
}

.agent-head {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.agent-head h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.online-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #16a34a;
}

.online-badge i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
}

.portal-main__placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-subtle);
}

.placeholder-card {
  text-align: center;
  padding: 32px;
}

.placeholder-icon {
  font-size: 48px;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.placeholder-card h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.placeholder-card p {
  margin: 0;
  color: var(--text-secondary);
}

.portal-history {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--card-bg);
  border-left: 1px solid var(--border);
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 14px 10px;
}

.history-head strong {
  font-size: 14px;
}

.portal-history :deep(.ant-spin-nested-loading),
.portal-history :deep(.ant-spin-container) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.history-empty {
  margin: 24px 16px;
  font-size: 13px;
  color: var(--text-muted);
}

.history-item {
  width: calc(100% - 16px);
  margin: 0 8px 6px;
  padding: 10px 12px;
  border: none;
  border-radius: 10px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-item:hover,
.history-item.active {
  background: var(--hover-bg);
}

.history-item strong {
  font-size: 13px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-item span {
  font-size: 12px;
  color: var(--text-muted);
}

.portal-rail {
  width: 52px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  background: var(--card-bg);
  border-left: 1px solid var(--border);
}

.rail-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.rail-btn:hover:not(:disabled) {
  background: var(--hover-bg);
  color: var(--primary);
}

.rail-btn.active {
  background: var(--hover-bg);
  color: var(--primary);
}

.rail-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
