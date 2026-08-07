<template>
  <div class="page-card placeholder">
    <h2>{{ title }}</h2>
    <p>模块开发中，敬请期待。</p>
    <a-button type="primary" @click="$router.push('/dashboard')">返回工作台</a-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => (route.meta.title as string) || '页面')
</script>

<style scoped>
.placeholder {
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 12px;
}
</style>
