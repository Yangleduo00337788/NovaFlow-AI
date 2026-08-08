<template>
  <a-popover
    v-model:open="popoverOpen"
    trigger="click"
    placement="bottomLeft"
    overlay-class-name="search-tool-popover"
    :overlay-style="{ maxWidth: '360px' }"
  >
    <template #content>
      <div class="search-tool-popover__title">参考网页</div>
      <div class="search-tool-popover__list">
        <a
          v-for="(item, index) in items"
          :key="`${item.title}-${index}`"
          class="search-tool-popover__item"
          :href="item.url || undefined"
          :target="item.url ? '_blank' : undefined"
          :rel="item.url ? 'noopener noreferrer' : undefined"
          @click="!item.url && $event.preventDefault()"
        >
          <span class="search-tool-popover__index">{{ index + 1 }}</span>
          <img
            v-if="item.url"
            class="search-tool-popover__favicon"
            :src="faviconUrl(item.url)"
            alt=""
            loading="lazy"
            @error="onFaviconError"
          />
          <div class="search-tool-popover__body">
            <div class="search-tool-popover__item-title">{{ item.title }}</div>
            <div v-if="item.snippet" class="search-tool-popover__snippet">{{ item.snippet }}</div>
            <div v-if="item.url" class="search-tool-popover__host">{{ displayHost(item.url) }}</div>
          </div>
        </a>
      </div>
    </template>

    <button
      type="button"
      class="search-status-bar"
      :class="{ 'search-status-bar--loading': loading }"
      :disabled="loading && !items.length"
      @click="onBarClick"
    >
      <SearchOutlined class="search-status-bar__icon" />
      <span class="search-status-bar__text">{{ statusText }}</span>
      <div v-if="faviconHosts.length" class="search-status-bar__favicons">
        <img
          v-for="(source, index) in faviconHosts"
          :key="`${source.host}-${index}`"
          class="search-status-bar__favicon"
          :src="faviconUrl(source.url)"
          :alt="source.host"
          loading="lazy"
          @error="onFaviconError"
        />
        <span v-if="extraCount > 0" class="search-status-bar__more">+{{ extraCount }}</span>
      </div>
      <DownOutlined v-if="!loading && items.length" class="search-status-bar__chevron" />
    </button>
  </a-popover>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { DownOutlined, SearchOutlined } from '@ant-design/icons-vue'
import {
  displayHost,
  faviconUrl,
  uniqueFaviconHosts,
  type SearchResultItem,
} from '@/utils/searchToolResult'

const props = withDefaults(
  defineProps<{
    items: SearchResultItem[]
    query?: string
    loading?: boolean
  }>(),
  {
    items: () => [],
    loading: false,
  },
)

const popoverOpen = ref(false)
const faviconHosts = computed(() => uniqueFaviconHosts(props.items, 6))
const extraCount = computed(() => Math.max(props.items.length - faviconHosts.value.length, 0))

const statusText = computed(() => {
  if (props.loading) {
    return props.query ? `正在阅读「${props.query}」相关网页...` : '正在阅读网页...'
  }
  if (props.items.length > 0) {
    return `已阅读 ${props.items.length} 个网页`
  }
  return '阅读网页完成'
})

function onBarClick() {
  if (!props.loading && props.items.length) {
    popoverOpen.value = !popoverOpen.value
  }
}

function onFaviconError(event: Event) {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}
</script>

<style scoped>
.search-status-bar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  padding: 6px 10px;
  border: 1px solid #e8edf3;
  border-radius: 999px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  line-height: 1.4;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.search-status-bar:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #dbe3ec;
}

.search-status-bar--loading {
  cursor: default;
}

.search-status-bar:disabled {
  cursor: default;
}

.search-status-bar__icon {
  color: #64748b;
  font-size: 14px;
  flex-shrink: 0;
}

.search-status-bar__text {
  color: #475569;
  white-space: nowrap;
}

.search-status-bar__favicons {
  display: inline-flex;
  align-items: center;
  margin-left: 2px;
}

.search-status-bar__favicon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1.5px solid #fff;
  margin-left: -5px;
  background: #fff;
  object-fit: cover;
  box-shadow: 0 0 0 1px #e2e8f0;
}

.search-status-bar__favicon:first-child {
  margin-left: 0;
}

.search-status-bar__more {
  margin-left: 4px;
  font-size: 11px;
  color: #94a3b8;
}

.search-status-bar__chevron {
  font-size: 10px;
  color: #94a3b8;
  flex-shrink: 0;
}
</style>

<style>
.search-tool-popover .ant-popover-inner {
  padding: 0;
  border-radius: 12px;
  overflow: hidden;
}

.search-tool-popover__title {
  padding: 10px 12px 8px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  border-bottom: 1px solid #f1f5f9;
}

.search-tool-popover__list {
  max-height: 320px;
  overflow-y: auto;
}

.search-tool-popover__item {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  text-decoration: none;
  color: inherit;
  border-bottom: 1px solid #f8fafc;
  transition: background 0.15s ease;
}

.search-tool-popover__item:hover {
  background: #f8fafc;
}

.search-tool-popover__item:last-child {
  border-bottom: none;
}

.search-tool-popover__index {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.search-tool-popover__favicon {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  flex-shrink: 0;
  margin-top: 2px;
}

.search-tool-popover__body {
  min-width: 0;
  flex: 1;
}

.search-tool-popover__item-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.45;
}

.search-tool-popover__snippet {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.search-tool-popover__host {
  margin-top: 4px;
  font-size: 11px;
  color: #94a3b8;
}
</style>
