<template>
  <div class="portal-shell">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { fetchCurrentUser } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

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
.portal-shell {
  height: 100vh;
  overflow: hidden;
  background: var(--bg);
}
</style>
