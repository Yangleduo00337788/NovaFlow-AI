<template>
  <div class="maintenance-page page-shell" data-testid="maintenance-page">
    <div class="maintenance-card page-card">
      <a-result status="warning" title="系统维护中">
        <template #subTitle>
          <p class="maintenance-message">{{ message }}</p>
          <p v-if="announcement" class="maintenance-announcement">{{ announcement }}</p>
        </template>
        <template #extra>
          <a-button type="primary" @click="retry">刷新状态</a-button>
          <a-button @click="goLogin">返回登录</a-button>
        </template>
      </a-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePlatformStatusStore } from '@/stores/platformStatus'
import { resolveLoginPath } from '@/config/app'

const router = useRouter()
const platformStatus = usePlatformStatusStore()

const message = computed(() => platformStatus.maintenanceMessage)
const announcement = computed(() => platformStatus.platformAnnouncement)

async function retry() {
  await platformStatus.refresh(true)
  if (!platformStatus.maintenanceEnabled) {
    router.replace('/dashboard')
  }
}

function goLogin() {
  router.push(resolveLoginPath())
}

onMounted(async () => {
  await platformStatus.refresh(true)
  if (!platformStatus.maintenanceEnabled) {
    router.replace('/dashboard')
  }
})
</script>

<style scoped>
.maintenance-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.maintenance-card {
  width: min(560px, 100%);
  padding: 24px;
}

.maintenance-message {
  margin: 0;
  color: var(--text-secondary);
}

.maintenance-announcement {
  margin: 12px 0 0;
  color: var(--text-primary);
}
</style>
