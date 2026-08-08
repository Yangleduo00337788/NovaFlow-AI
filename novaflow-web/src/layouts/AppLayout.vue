<template>
  <a-layout class="app-layout">
    <AppSidebar :collapsed="collapsed" />
    <a-layout>
      <AppHeader :collapsed="collapsed" @toggle="collapsed = !collapsed" />
      <a-layout-content class="main-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchCurrentUser } from '@/api/auth'
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
import { useAuthStore } from '@/stores/auth'

const collapsed = ref(false)
const auth = useAuthStore()

onMounted(async () => {
  if (!auth.isLoggedIn()) {
    return
  }
  try {
    const res = await fetchCurrentUser()
    auth.setAuth(res.data.data)
  } catch {
    // 401 由 request 拦截器处理
  }
})
</script>

<style scoped>
.app-layout {
  height: 100vh;
  overflow: hidden;
  background: var(--bg);
}

.app-layout :deep(.ant-layout) {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.main-content {
  margin: 10px 12px;
  height: calc(100vh - 56px - 20px);
  max-height: calc(100vh - 56px - 20px);
  overflow-x: hidden;
  overflow-y: auto;
}
</style>
