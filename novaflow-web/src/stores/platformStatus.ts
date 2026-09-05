import { defineStore } from 'pinia'
import { fetchPublicPlatformStatus, type PublicPlatformStatus } from '@/api/public'

export const usePlatformStatusStore = defineStore('platformStatus', {
  state: () => ({
    loaded: false,
    loading: false,
    status: {
      maintenanceEnabled: false,
      maintenanceMessage: '',
      platformAnnouncement: '',
    } as PublicPlatformStatus,
  }),
  getters: {
    maintenanceEnabled: (state) => state.status.maintenanceEnabled,
    maintenanceMessage: (state) =>
      state.status.maintenanceMessage?.trim() || '系统维护中，请稍后再试',
    platformAnnouncement: (state) => state.status.platformAnnouncement?.trim() || '',
    hasAnnouncement: (state) => Boolean(state.status.platformAnnouncement?.trim()),
  },
  actions: {
    async refresh(force = false) {
      if (this.loading) return this.status
      if (this.loaded && !force) return this.status
      this.loading = true
      try {
        const res = await fetchPublicPlatformStatus()
        this.status = res.data.data
        this.loaded = true
        return this.status
      } finally {
        this.loading = false
      }
    },
  },
})
