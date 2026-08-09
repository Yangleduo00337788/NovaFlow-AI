<template>
  <WebSearchStatusBar
    :items="items"
    :query="query"
    :loading="loading"
    :hide-icon="hideIcon"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import WebSearchStatusBar from '@/components/agent/WebSearchStatusBar.vue'
import {
  parseSearchToolArgs,
  parseSearchToolResult,
  type SearchResultItem,
} from '@/utils/searchToolResult'

const props = defineProps<{
  name: string
  args?: string
  result?: string
  hideIcon?: boolean
}>()

const parsed = computed(() => parseSearchToolResult(props.result))
const query = computed(() => parsed.value.query || parseSearchToolArgs(props.args))
const items = computed<SearchResultItem[]>(() => parsed.value.items)
const loading = computed(() => !props.result)
</script>
