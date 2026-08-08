<template>
  <a-layout-sider
    :collapsed="collapsed"
    :width="220"
    class="sidebar"
    :theme="themeStore.siderTheme"
  >
    <div class="brand">
      <AppLogo :collapsed="collapsed" />
    </div>

    <div class="menu-wrap">
      <router-link to="/dashboard" class="menu-link menu-link-top" :class="{ active: route.path === '/dashboard' }">
        <DashboardOutlined class="menu-icon" />
        <span v-if="!collapsed" class="menu-text">工作台</span>
      </router-link>

      <div v-for="group in visibleMenuGroups" :key="group.title" class="menu-group">
        <div v-if="!collapsed" class="group-title">{{ group.title }}</div>
        <router-link
          v-for="item in group.items"
          :key="item.key"
          :to="item.path"
          class="menu-link"
          :class="{ active: route.path.startsWith(item.path) }"
        >
          <component :is="getMenuIcon(item.icon)" class="menu-icon" />
          <span v-if="!collapsed" class="menu-label">
            <span class="menu-label-text">{{ item.label }}</span>
            <span v-if="item.beta" class="beta-tag">Beta</span>
          </span>
        </router-link>
      </div>
    </div>

    <div class="sidebar-footer">
      <div v-if="!collapsed" class="plan-card">
        <div class="plan-card-top">
          <div class="plan-card-head">
            <CrownOutlined class="plan-crown" />
            <span class="plan-type">{{ planInfo.planType }}</span>
          </div>
          <button type="button" class="plan-close" aria-label="关闭">×</button>
        </div>
        <div class="expire">到期时间：{{ planInfo.expireAt }}</div>
        <div class="usage-row">
          <div class="usage-text">
            已用 <strong class="usage-percent">{{ planInfo.usedPercent }}%</strong> / 100%
          </div>
          <span class="usage-percent-side">{{ planInfo.usedPercent }}%</span>
        </div>
        <a-progress
          :percent="planInfo.usedPercent"
          size="small"
          :show-info="false"
          stroke-color="#1677ff"
          trail-color="rgba(255, 255, 255, 0.65)"
          class="plan-progress"
        />
        <button type="button" class="upgrade-btn">
          <span>升级套餐</span>
          <RightOutlined class="upgrade-arrow" />
        </button>
      </div>
    </div>
  </a-layout-sider>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { DashboardOutlined, CrownOutlined, RightOutlined } from '@ant-design/icons-vue'
import AppLogo from '@/components/common/AppLogo.vue'
import { filterMenuGroups } from '@/config/menu'
import { getMenuIcon } from '@/config/menuIcons'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

defineProps<{ collapsed: boolean }>()

const route = useRoute()
const themeStore = useThemeStore()
const auth = useAuthStore()

const visibleMenuGroups = computed(() => filterMenuGroups(auth.hasAnyPermission))

const planInfo = reactive({
  planType: '企业版',
  expireAt: '2026-12-31',
  usedPercent: 68,
})
</script>

<style scoped>
.sidebar {
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--sidebar-bg) !important;
}

.sidebar :deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.brand {
  display: grid;
  justify-items: center;
  align-items: center;
  width: 100%;
  padding: 10px 10px 18px;
}

.menu-wrap {
  flex: 1;
  overflow: auto;
  padding: 0 10px 16px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.menu-wrap::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

.menu-link-top {
  margin-bottom: 4px;
}

.menu-group {
  margin-bottom: 0;
}

.menu-group + .menu-group {
  margin-top: 0;
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
  line-height: 1.5;
  transition: background 0.15s, color 0.15s;
}

.menu-link:hover {
  background: var(--menu-hover-bg);
  color: var(--text-body);
}

.menu-icon {
  font-size: 15px;
  flex-shrink: 0;
  width: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.menu-text {
  font-size: 13px;
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
  background: var(--primary);
}

.group-title {
  font-size: 11px;
  color: var(--text-muted);
  padding: 20px 12px 8px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.menu-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  min-width: 0;
  gap: 8px;
  font-size: 13px;
  line-height: 1.4;
}

.menu-label-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.beta-tag {
  flex-shrink: 0;
  margin-left: auto;
  font-size: 12px;
  font-weight: 700;
  line-height: 20px;
  padding: 0 10px;
  border-radius: 999px;
  color: var(--beta-tag-color);
  background: var(--beta-tag-bg);
}

.sidebar-footer {
  flex-shrink: 0;
  padding: 0 12px 12px;
}

.plan-card {
  padding: 14px 14px 12px;
  border-radius: 12px;
  background: var(--plan-card-bg);
  border: 1px solid var(--plan-card-border);
}

.plan-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.plan-card-head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.plan-crown {
  color: var(--text-primary);
  font-size: 14px;
}

.plan-type {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.plan-close {
  border: none;
  background: transparent;
  color: #cbd5e1;
  font-size: 14px;
  line-height: 1;
  padding: 0;
  cursor: pointer;
}

.plan-close:hover {
  color: #94a3b8;
}

.expire {
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  line-height: 1.4;
}

.usage-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.usage-text {
  font-size: 11px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.usage-percent {
  color: #1677ff;
  font-weight: 700;
}

.usage-percent-side {
  font-size: 11px;
  color: #cbd5e1;
  font-variant-numeric: tabular-nums;
}

.plan-progress {
  margin-bottom: 10px;
}

.plan-progress :deep(.ant-progress-bg) {
  height: 6px !important;
  border-radius: 999px;
}

.plan-progress :deep(.ant-progress-inner) {
  background: rgba(255, 255, 255, 0.65);
  border-radius: 999px;
}

.upgrade-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  cursor: pointer;
  line-height: 1.4;
}

.upgrade-btn:hover {
  color: var(--text-body);
}

.upgrade-arrow {
  font-size: 12px;
  color: #94a3b8;
}
</style>
