<template>
  <a-layout-sider
    :collapsed="collapsed"
    :width="220"
    class="sidebar platform-sidebar"
    :theme="themeStore.siderTheme"
  >
    <div class="brand">
      <AppLogo :collapsed="collapsed" />
      <span v-if="!collapsed" class="brand-badge">Platform</span>
    </div>

    <div class="menu-wrap">
      <div v-for="group in visibleMenuGroups" :key="group.title" class="menu-group">
        <div v-if="!collapsed" class="group-title">{{ group.title }}</div>
        <router-link
          v-for="item in group.items"
          :key="item.key"
          :to="item.path"
          class="menu-link"
          :class="{ active: isActive(item.path) }"
        >
          <component :is="getMenuIcon(item.icon)" class="menu-icon" />
          <span v-if="!collapsed" class="menu-text">{{ item.label }}</span>
        </router-link>
      </div>
    </div>

    <div class="sidebar-footer">
      <div v-if="!collapsed" class="scope-tip">NovaFlow 平台治理域</div>
    </div>
  </a-layout-sider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppLogo from '@/components/common/AppLogo.vue'
import { createRouteAccessContext } from '@/config/access'
import { filterPlatformMenuGroups } from '@/config/platformMenu'
import { getMenuIcon } from '@/config/menuIcons'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

defineProps<{ collapsed: boolean }>()

const route = useRoute()
const themeStore = useThemeStore()
const auth = useAuthStore()

const visibleMenuGroups = computed(() =>
  filterPlatformMenuGroups(createRouteAccessContext(auth)),
)

function isActive(path: string) {
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>

<style scoped>
.platform-sidebar {
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--sidebar-bg) !important;
}

.platform-sidebar :deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 10px 18px;
}

.brand-badge {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--platform-accent);
  background: var(--platform-accent-soft);
  border-radius: 999px;
  padding: 2px 10px;
}

.menu-wrap {
  flex: 1;
  overflow: auto;
  padding: 0 10px 16px;
}

.menu-group + .menu-group {
  margin-top: 8px;
}

.group-title {
  font-size: 11px;
  color: var(--text-muted);
  padding: 12px 12px 8px;
  font-weight: 500;
}

.menu-link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--text-secondary);
  margin-bottom: 2px;
  transition: background 0.15s, color 0.15s;
}

.menu-link:hover {
  background: var(--menu-hover-bg);
  color: var(--text-body);
}

.menu-link.active {
  background: var(--menu-active-bg);
  color: var(--primary);
  font-weight: 500;
}

.menu-link.active::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--platform-accent);
}

.menu-icon {
  font-size: 15px;
  width: 16px;
}

.menu-text {
  font-size: 13px;
}

.sidebar-footer {
  flex-shrink: 0;
  padding: 0 12px 12px;
}

.scope-tip {
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
  line-height: 1.5;
}
</style>
