<template>
  <AgentProcessBlock
    :title="statusText"
    :loading="loading"
    :default-expanded="items.length > 0"
    :collapsible="items.length > 0"
    :hide-icon="hideIcon"
  >
    <div class="web-panel">
      <a
        v-for="(item, index) in items"
        :key="`${item.title}-${index}`"
        class="web-panel__item"
        :href="item.url || undefined"
        :target="item.url ? '_blank' : undefined"
        :rel="item.url ? 'noopener noreferrer' : undefined"
        @click="!item.url && $event.preventDefault()"
      >
        <span class="web-panel__index">{{ index + 1 }}</span>
        <img
          v-if="item.url"
          class="web-panel__favicon"
          :src="faviconUrl(item.url)"
          alt=""
          loading="lazy"
          @error="onFaviconError"
        />
        <div class="web-panel__body">
          <div class="web-panel__title">{{ item.title }}</div>
          <div v-if="item.snippet" class="web-panel__snippet">{{ item.snippet }}</div>
          <div v-if="item.url" class="web-panel__host">{{ displayHost(item.url) }}</div>
        </div>
      </a>
    </div>
  </AgentProcessBlock>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AgentProcessBlock from '@/components/agent/AgentProcessBlock.vue'
import {
  displayHost,
  faviconUrl,
  type SearchResultItem,
} from '@/utils/searchToolResult'

const props = withDefaults(
  defineProps<{
    items: SearchResultItem[]
    query?: string
    loading?: boolean
    hideIcon?: boolean
  }>(),
  {
    items: () => [],
    loading: false,
    hideIcon: false,
  },
)

const statusText = computed(() => {
  if (props.loading) {
    return props.query ? `正在阅读「${props.query}」相关网页...` : '正在阅读网页...'
  }
  if (props.items.length > 0) {
    return `已阅读 ${props.items.length} 个网页`
  }
  return '阅读网页完成'
})

function onFaviconError(event: Event) {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}
</script>

<style scoped>
.web-panel {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.web-panel__item {
  display: flex;
  gap: 8px;
  padding: 8px 6px;
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
  transition: background 0.15s ease;
}

.web-panel__item:hover {
  background: #f8fafc;
}

.web-panel__index {
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

.web-panel__favicon {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  flex-shrink: 0;
  margin-top: 2px;
}

.web-panel__body {
  min-width: 0;
  flex: 1;
}

.web-panel__title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  line-height: 1.45;
}

.web-panel__snippet {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.web-panel__host {
  margin-top: 4px;
  font-size: 11px;
  color: #94a3b8;
}
</style>
