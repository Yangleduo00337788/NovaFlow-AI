<template>
  <a-tooltip :title="text" placement="top" :overlay-style="{ maxWidth: '360px' }">
    <QuestionCircleOutlined class="field-help-icon" @click.stop />
  </a-tooltip>
</template>

<script setup lang="ts">
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

defineProps<{
  text: string
}>()
</script>

<style scoped>
.field-help-icon {
  margin-left: 4px;
  color: #94a3b8;
  cursor: help;
  font-size: 14px;
  vertical-align: -2px;
  transition: color 0.2s;
}

.field-help-icon:hover {
  color: #3b82f6;
}
</style>
