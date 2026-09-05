<template>
  <div class="about-page page-shell">
    <div class="page-header">
      <div>
        <h1>{{ pageMeta.title }}</h1>
        <p>{{ pageMeta.subtitle }}</p>
      </div>
      <span v-if="isHome" class="version-badge">v1.1.0</span>
    </div>

    <div class="about-panel page-card">
      <aside class="about-nav">
        <router-link :to="aboutBase" class="about-brand" :class="{ active: isHome }">
          <span class="brand-dot" />
          <span class="brand-text">
            <span class="brand-name">关于 NovaFlow</span>
            <span class="brand-version">v1.1.0</span>
          </span>
        </router-link>
        <nav class="about-nav-list">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="about-nav-item"
            :class="{ active: isActive(item.path) }"
          >
            <component :is="item.icon" class="nav-icon" />
            <span>{{ item.label }}</span>
          </router-link>
        </nav>
      </aside>

      <main class="about-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { aboutNavItems, getAboutPageMeta, resolveAboutBase } from './about-config'

const route = useRoute()

const aboutBase = computed(() => resolveAboutBase(route.path))
const isHome = computed(() => route.path === aboutBase.value || route.path === `${aboutBase.value}/`)
const pageMeta = computed(() => getAboutPageMeta(route.path))
const navItems = computed(() =>
  aboutNavItems.map((item) => ({
    ...item,
    path: item.path.replace('/about', aboutBase.value),
  })),
)

function isActive(path: string) {
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>

<style scoped>
.about-page {
  min-height: auto;
}

.about-panel {
  display: flex;
  align-items: stretch;
  padding: 0;
  overflow: hidden;
}

.about-nav {
  width: 208px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 10px;
  border-right: 1px solid var(--border);
  background: var(--bg-subtle);
}

.about-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 4px;
  border-radius: 10px;
  color: inherit;
  text-decoration: none;
  transition: background 0.15s;
}

.about-brand:hover,
.about-brand.active {
  background: rgba(22, 119, 255, 0.08);
}

.about-brand.active .brand-name {
  color: var(--primary);
}

.brand-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.brand-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.3;
}

.brand-version {
  font-size: 11px;
  color: var(--text-muted);
}

.about-nav-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.about-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13px;
  transition: background 0.15s, color 0.15s;
}

.about-nav-item:hover {
  background: var(--hover-bg);
  color: var(--text-primary);
}

.about-nav-item.active {
  background: var(--card-bg);
  color: var(--primary);
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.nav-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.about-content {
  flex: 1;
  min-width: 0;
  padding: 20px 24px;
  overflow-y: auto;
}

.version-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .about-panel {
    flex-direction: column;
    min-height: auto;
  }

  .about-nav {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid var(--border);
    padding-bottom: 12px;
  }

  .about-nav-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 4px;
  }
}

@media (max-width: 520px) {
  .about-nav-list {
    grid-template-columns: 1fr;
  }

  .about-content {
    padding: 16px;
  }
}
</style>
