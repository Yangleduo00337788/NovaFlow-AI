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
      <a-button type="text" class="icon-btn" title="切换主题">
        <svg class="theme-icon" viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor">
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" stroke="currentColor" stroke-width="2" stroke-linecap="round" fill="none"/>
        </svg>
      </a-button>
      <a-button type="text" class="icon-btn" title="帮助"><QuestionCircleOutlined /></a-button>
      <a-badge :count="12" :offset="[-2, 2]">
        <a-button type="text" class="icon-btn" title="通知"><BellOutlined /></a-button>
      </a-badge>
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
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
import { getBreadcrumbByPath } from '@/config/menu'
import { getMenuIcon } from '@/config/menuIcons'

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

function onGlobalKeydown(event: KeyboardEvent) {
  const target = event.target as HTMLElement
  const isTyping = target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable
  if (event.key === '/' && !isTyping && !event.metaKey && !event.ctrlKey && !event.altKey) {
    event.preventDefault()
    searchInputRef.value?.focus()
  }
}

onMounted(() => window.addEventListener('keydown', onGlobalKeydown))
onUnmounted(() => window.removeEventListener('keydown', onGlobalKeydown))

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
  background: #fff;
  padding: 0 20px;
  border-bottom: 1px solid #eef2f7;
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
  background: #f8fafc;
  border: 1px solid transparent;
  transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
}

.global-search:focus-within {
  background: #fff;
  border-color: #e2e8f0;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.08);
}

.search-prefix {
  flex-shrink: 0;
  font-size: 14px;
  color: #94a3b8;
}

.search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #334155;
  line-height: 1;
}

.search-input::placeholder {
  color: #94a3b8;
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
  color: #64748b;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-btn:hover {
  color: #334155;
  background: #f8fafc;
}

.theme-icon {
  display: block;
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
  color: #0f172a;
}

.user-role {
  font-size: 12px;
  color: #94a3b8;
}

.user-toggle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #eef2ff;
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
  background: #e0e9ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
}

.user-toggle.open .user-toggle-icon {
  transform: rotate(180deg);
}

.user-toggle:hover {
  background: #e8f0ff;
}
</style>
