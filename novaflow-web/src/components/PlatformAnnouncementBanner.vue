<template>
  <a-alert
    v-if="announcement"
    class="platform-announcement"
    type="info"
    show-icon
    banner
    :message="announcement"
  />
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { usePlatformStatusStore } from '@/stores/platformStatus'

const platformStatus = usePlatformStatusStore()
const announcement = computed(() => platformStatus.platformAnnouncement)

onMounted(() => {
  platformStatus.refresh()
})
</script>

<style scoped>
.platform-announcement {
  border-radius: 0;
  margin: 0;
}
</style>
