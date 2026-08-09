<template>
  <a-layout-header class="header">
    <div class="left">
      <a-button type="text" class="icon-btn" @click="$emit('toggle')">
        <MenuUnfoldOutlined v-if="collapsed" />
        <MenuFoldOutlined v-else />
      </a-button>
      <router-link :to="breadcrumb.path" class="breadcrumb">
        <component :is="breadcrumbIcon" class="breadcrumb-icon" />
        <span class="page-title">{{ breadcrumb.title }}</span>
      </router-link>
    </div>

    <div class="center">
      <div class="global-search">
        <SearchOutlined class="search-prefix" />
        <input
          ref="searchInputRef"
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="搜索应用、Agent、知识库、工作流..."
        />
      </div>
    </div>

    <div class="right">
      <ThemeToggle />
      <a-button type="text" class="icon-btn" title="帮助"><QuestionCircleOutlined /></a-button>
      <a-dropdown
        v-model:open="notificationOpen"
        :trigger="['click']"
        placement="bottomRight"
        overlay-class-name="notification-dropdown"
        @open-change="onNotificationOpenChange"
      >
        <a-badge :count="displayUnreadCount" :overflow-count="99" :offset="[-2, 2]">
          <a-button type="text" class="icon-btn" title="通知"><BellOutlined /></a-button>
        </a-badge>
        <template #overlay>
          <div class="notification-panel" @click.stop>
            <div class="notification-header">
              <span>通知</span>
              <a-button
                type="link"
                size="small"
                :disabled="!hasUnread"
                :loading="markAllLoading"
                @click.stop="markAllRead"
              >
                全部已读
              </a-button>
            </div>
            <a-spin :spinning="notificationsLoading">
              <a-empty v-if="!notifications.length" description="暂无通知" />
              <div v-else class="notification-list">
                <div
                  v-for="item in notifications"
                  :key="item.id"
                  class="notification-item"
                  :class="{ unread: !item.read }"
                  @click="openNotification(item)"
                >
                  <div class="notification-title">{{ item.title }}</div>
                  <div class="notification-content">{{ item.content }}</div>
                  <div class="notification-time">{{ formatDateTime(item.createdAt) }}</div>
                </div>
              </div>
            </a-spin>
          </div>
        </template>
      </a-dropdown>
      <div class="user-profile">
        <div class="user-meta">
          <span class="user-name">{{ displayName }}</span>
          <span class="user-role">{{ roleName }}</span>
        </div>
        <a-dropdown
          v-model:open="userMenuOpen"
          :trigger="['click']"
          placement="bottomRight"
          overlay-class-name="user-menu-dropdown"
        >
          <span class="user-toggle" :class="{ open: userMenuOpen }">
            <DownOutlined class="user-toggle-icon" />
          </span>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="onLogout">退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BellOutlined,
  QuestionCircleOutlined,
  SearchOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { logout } from '@/api/auth'
import {
  fetchNotifications,
  fetchUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
  type UserNotification,
} from '@/api/notification'
import { getBreadcrumbByPath } from '@/config/menu'
import { getMenuIcon } from '@/config/menuIcons'
import { formatDateTime } from '@/utils/datetime'
import ThemeToggle from '@/components/common/ThemeToggle.vue'

defineProps<{ collapsed: boolean }>()
defineEmits<{ toggle: [] }>()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const breadcrumb = computed(() => getBreadcrumbByPath(route.path))
const breadcrumbIcon = computed(() => getMenuIcon(breadcrumb.value.icon))
const displayName = computed(() => auth.user?.nickname || auth.user?.username || '用户')
const roleName = computed(() => {
  const name = auth.user?.roleName || '超级管理员'
  return name === '企业管理员' ? '超级管理员' : name
})
const searchInputRef = ref<HTMLInputElement | null>(null)
const searchKeyword = ref('')
const userMenuOpen = ref(false)
const notificationOpen = ref(false)
const notificationsLoading = ref(false)
const markAllLoading = ref(false)
const unreadCount = ref(0)
const notifications = ref<UserNotification[]>([])
let notificationTimer: number | undefined

const hasUnread = computed(
  () => unreadCount.value > 0 || notifications.value.some((item) => !item.read),
)

const displayUnreadCount = computed(() => {
  const listUnread = notifications.value.filter((item) => !item.read).length
  return Math.max(unreadCount.value, listUnread)
})

async function loadUnreadCount() {
  try {
    const res = await fetchUnreadNotificationCount()
    unreadCount.value = Number(res.data.data) || 0
  } catch {
    unreadCount.value = 0
  }
}

async function loadNotifications() {
  notificationsLoading.value = true
  try {
    const [listRes] = await Promise.all([
      fetchNotifications({ page: 1, pageSize: 10 }),
      loadUnreadCount(),
    ])
    notifications.value = listRes.data.data.list
  } catch {
    notifications.value = []
  } finally {
    notificationsLoading.value = false
  }
}

function onNotificationOpenChange(open: boolean) {
  if (open) {
    loadNotifications()
  } else {
    loadUnreadCount()
  }
}

async function openNotification(item: UserNotification) {
  if (!item.read) {
    await markNotificationRead(item.id)
    item.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  notificationOpen.value = false
  if (item.linkUrl) {
    router.push(item.linkUrl)
  }
}

async function markAllRead() {
  if (!hasUnread.value || markAllLoading.value) return
  markAllLoading.value = true
  try {
    await markAllNotificationsRead()
    notifications.value = notifications.value.map((item) => ({ ...item, read: true }))
    unreadCount.value = 0
  } catch {
    await loadNotifications()
  } finally {
    markAllLoading.value = false
  }
}

function onGlobalKeydown(event: KeyboardEvent) {
  const target = event.target as HTMLElement
  const isTyping = target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable
  if (event.key === '/' && !isTyping && !event.metaKey && !event.ctrlKey && !event.altKey) {
    event.preventDefault()
    searchInputRef.value?.focus()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onGlobalKeydown)
  loadUnreadCount()
  notificationTimer = window.setInterval(loadUnreadCount, 30000)
})
watch(
  () => route.path,
  () => {
    loadUnreadCount()
  },
)
onUnmounted(() => {
  window.removeEventListener('keydown', onGlobalKeydown)
  if (notificationTimer) {
    window.clearInterval(notificationTimer)
  }
})

async function onLogout() {
  try {
    await logout()
  } finally {
    auth.clear()
    router.push('/login')
  }
}
</script>

<style scoped>
.header {
  display: grid;
  grid-template-columns: 200px 1fr 340px;
  align-items: center;
  background: var(--header-bg);
  padding: 0 20px;
  border-bottom: 1px solid var(--border);
  height: 56px;
}

.right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.center {
  display: flex;
  justify-content: center;
}

.global-search {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  max-width: 520px;
  height: 36px;
  padding: 0 12px;
  border-radius: 12px;
  background: var(--input-bg);
  border: 1px solid transparent;
  transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
}

.global-search:focus-within {
  background: var(--input-focus-bg);
  border-color: var(--border-strong);
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.08);
}

.search-prefix {
  flex-shrink: 0;
  font-size: 14px;
  color: var(--text-muted);
}

.search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: var(--text-body);
  line-height: 1;
}

.search-input::placeholder {
  color: var(--text-muted);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--primary);
  transition: color 0.15s;
}

.breadcrumb:hover,
.breadcrumb:hover .breadcrumb-icon,
.breadcrumb:hover .page-title {
  color: var(--primary);
}

.breadcrumb-icon {
  color: var(--primary);
  font-size: 14px;
  transition: color 0.15s;
}

.page-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--primary);
}

.left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  color: var(--icon-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-btn:hover {
  color: var(--icon-hover-color);
  background: var(--hover-bg);
}

.right :deep(.ant-badge-count) {
  z-index: 2;
  box-shadow: 0 0 0 1px var(--header-bg, #fff);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 18px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.25;
}

.user-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.user-role {
  font-size: 12px;
  color: var(--text-muted);
}

.user-toggle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--user-toggle-bg);
  color: #1677ff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
  transition: background 0.2s ease, box-shadow 0.2s ease;
}

.user-toggle-icon {
  font-size: 11px;
  transition: transform 0.2s ease;
}

.user-toggle.open {
  background: var(--user-toggle-open-bg);
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}

.user-toggle.open .user-toggle-icon {
  transform: rotate(180deg);
}

.user-toggle:hover {
  background: var(--user-toggle-hover-bg);
}
</style>

<style>
.notification-dropdown .notification-panel {
  width: 320px;
  background: var(--card-bg, #fff);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

.notification-dropdown .notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border-color, #f0f0f0);
  font-weight: 600;
}

.notification-dropdown .notification-header .ant-btn-link {
  pointer-events: auto;
  padding: 0;
  height: auto;
}

.notification-dropdown .notification-list {
  max-height: 360px;
  overflow-y: auto;
}

.notification-dropdown .notification-item {
  padding: 12px 14px;
  border-bottom: 1px solid var(--border-color, #f0f0f0);
  cursor: pointer;
}

.notification-dropdown .notification-item:hover {
  background: rgba(99, 102, 241, 0.06);
}

.notification-dropdown .notification-item.unread {
  background: rgba(99, 102, 241, 0.04);
}

.notification-dropdown .notification-title {
  font-size: 13px;
  font-weight: 600;
}

.notification-dropdown .notification-content {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary, #64748b);
  line-height: 1.5;
}

.notification-dropdown .notification-time {
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-muted, #94a3b8);
}
</style>
