<template>
  <div class="trace-page">
    <div class="trace-main">
      <div class="page-header">
        <div>
          <h1>链路分析</h1>
          <p>追踪 Agent 与工作流调用链路，定位性能瓶颈与异常节点</p>
        </div>
        <a-space>
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索 Trace ID / 应用名称"
            style="width: 260px"
            allow-clear
          />
          <a-select v-model:value="timeRange" style="width: 140px">
            <a-select-option value="1h">近 1 小时</a-select-option>
            <a-select-option value="24h">近 24 小时</a-select-option>
            <a-select-option value="7d">近 7 天</a-select-option>
          </a-select>
        </a-space>
      </div>

      <div class="page-card trace-card">
        <a-empty description="暂无链路数据，请稍后重试" />
      </div>
    </div>

    <DashboardRightPanel />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import DashboardRightPanel from '@/components/dashboard/DashboardRightPanel.vue'

const keyword = ref('')
const timeRange = ref('24h')
</script>

<style scoped>
.trace-page {
  display: grid;
  grid-template-columns: 1fr var(--rightbar-width);
  gap: 12px;
  height: 100%;
  max-height: 100%;
  min-width: 0;
  overflow: hidden;
}

.trace-main {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
}

.page-header h1 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.page-header p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.trace-card {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
