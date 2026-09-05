<template>
  <div class="platform-admin-page page-shell">
    <div class="page-header">
      <div>
        <h1>API 调用监控</h1>
        <p>异常流量租户识别与高频调用告警</p>
      </div>
      <a-button :loading="loading" @click="loadOverview">刷新</a-button>
    </div>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="live" tab="实时监控">
        <div class="page-card list-panel">
          <a-spin :spinning="loading">
            <div v-if="overview" class="ops-stats-grid">
              <div class="stat-item">
                <span class="label">今日总调用</span>
                <strong>{{ formatPlatformNumber(overview.totalCallsToday) }}</strong>
              </div>
              <div class="stat-item">
                <span class="label">近 1 小时调用</span>
                <strong>{{ formatPlatformNumber(overview.totalCallsLastHour) }}</strong>
                <span class="sub-label">阈值 {{ overview.hourlyCallsThreshold }}</span>
              </div>
              <div class="stat-item">
                <span class="label">流量突增倍数</span>
                <strong>{{ overview.trafficSpikeMultiplier }}x</strong>
                <span class="sub-label">相对 7 日均值</span>
              </div>
              <div class="stat-item">
                <span class="label">活跃告警</span>
                <strong>{{ overview.alerts.length }}</strong>
              </div>
            </div>

            <div v-if="overview?.alerts.length" class="ops-panels">
              <div class="ops-panel-title">当前告警</div>
              <a-table
                :columns="alertColumns"
                :data-source="overview.alerts"
                :pagination="false"
                row-key="(record, index) => `${record.type}-${record.tenantId}-${index}`"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'severity'">
                    <a-tag :color="record.severity === 'critical' ? 'red' : 'orange'">
                      {{ record.severity === 'critical' ? '严重' : '警告' }}
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'type'">
                    {{ record.type === 'HIGH_FREQUENCY' ? '高频调用' : '流量突增' }}
                  </template>
                  <template v-else-if="column.key === 'tenantName'">
                    <router-link v-if="record.tenantId" :to="tenantDetailPath(record.tenantId)">
                      {{ record.tenantName }}
                    </router-link>
                    <span v-else>{{ record.tenantName }}</span>
                  </template>
                </template>
              </a-table>
            </div>

            <div v-if="overview" class="ops-panels two-col">
              <div>
                <div class="ops-panel-title">近 1 小时 Top 租户</div>
                <a-table
                  :columns="tenantColumns"
                  :data-source="overview.topTenantsLastHour"
                  :pagination="false"
                  row-key="tenantId"
                  size="small"
                />
              </div>
              <div>
                <div class="ops-panel-title">流量突增租户</div>
                <a-table
                  :columns="spikeColumns"
                  :data-source="overview.trafficSpikes"
                  :pagination="false"
                  row-key="tenantId"
                  size="small"
                />
              </div>
            </div>
          </a-spin>
        </div>
      </a-tab-pane>

      <a-tab-pane key="history" tab="告警历史">
        <div class="page-card list-panel">
          <div class="list-toolbar">
            <a-select v-model:value="statusFilter" allow-clear placeholder="状态" style="width: 120px" @change="loadHistory">
              <a-select-option value="OPEN">待处理</a-select-option>
              <a-select-option value="ACKED">已确认</a-select-option>
            </a-select>
          </div>
          <a-table
            :columns="historyColumns"
            :data-source="history"
            :loading="historyLoading"
            row-key="id"
            :pagination="historyPagination"
            size="small"
            @change="onHistoryTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'severity'">
                <a-tag :color="record.severity === 'critical' ? 'red' : 'orange'">
                  {{ record.severity === 'critical' ? '严重' : '警告' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'alertType'">
                {{ record.alertType === 'HIGH_FREQUENCY' ? '高频调用' : '流量突增' }}
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === 'OPEN' ? 'orange' : 'default'">
                  {{ record.status === 'OPEN' ? '待处理' : '已确认' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-button
                  v-if="record.status === 'OPEN'"
                  type="link"
                  size="small"
                  @click="ackAlert(record.id)"
                >
                  确认
                </a-button>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  acknowledgePlatformApiAlert,
  fetchPlatformApiAlertEvents,
  fetchPlatformApiMonitor,
  type PlatformApiAlertEvent,
  type PlatformApiMonitor,
} from '@/api/platform'
import { platformPath } from '@/config/deploy'
import { formatPlatformNumber } from '@/views/platform/shared/utils'
import '@/views/platform/shared/styles.css'

const loading = ref(false)
const historyLoading = ref(false)
const activeTab = ref('live')
const statusFilter = ref<string | undefined>()
const overview = ref<PlatformApiMonitor | null>(null)
const history = ref<PlatformApiAlertEvent[]>([])
const historyTotal = ref(0)
const historyPage = ref(1)
const historyPageSize = ref(10)

const alertColumns = [
  { title: '级别', key: 'severity', width: 90 },
  { title: '类型', key: 'type', width: 110 },
  { title: '租户', key: 'tenantName' },
  { title: '说明', dataIndex: 'message', key: 'message', ellipsis: true },
]

const historyColumns = [
  { title: '级别', key: 'severity', width: 90 },
  { title: '类型', key: 'alertType', width: 110 },
  { title: '租户', dataIndex: 'tenantName', key: 'tenantName' },
  { title: '说明', dataIndex: 'message', key: 'message', ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'actions', width: 90 },
]

const tenantColumns = [
  { title: '租户', dataIndex: 'tenantName', key: 'tenantName' },
  { title: '调用次数', dataIndex: 'calls', key: 'calls', width: 100 },
  { title: 'Token', dataIndex: 'tokens', key: 'tokens', width: 120 },
]

const spikeColumns = [
  { title: '租户', dataIndex: 'tenantName', key: 'tenantName' },
  { title: '今日', dataIndex: 'todayCalls', key: 'todayCalls', width: 80 },
  { title: '7 日均值', dataIndex: 'avgDailyCalls', key: 'avgDailyCalls', width: 90 },
  {
    title: '倍数',
    key: 'spikeRatio',
    width: 80,
    customRender: ({ record }: { record: { spikeRatio?: number } }) =>
      record.spikeRatio ? `${record.spikeRatio.toFixed(1)}x` : '-',
  },
]

const historyPagination = computed(() => ({
  current: historyPage.value,
  pageSize: historyPageSize.value,
  total: historyTotal.value,
  showSizeChanger: true,
}))

function tenantDetailPath(id: number) {
  return platformPath(`/platform/tenants/${id}`)
}

async function loadOverview() {
  loading.value = true
  try {
    const res = await fetchPlatformApiMonitor()
    overview.value = res.data.data
    if (activeTab.value === 'history') {
      await loadHistory()
    }
  } catch {
    message.error('加载 API 监控数据失败')
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await fetchPlatformApiAlertEvents({
      page: historyPage.value,
      pageSize: historyPageSize.value,
      status: statusFilter.value,
    })
    history.value = res.data.data.list
    historyTotal.value = res.data.data.total
  } catch {
    message.error('加载告警历史失败')
  } finally {
    historyLoading.value = false
  }
}

function onHistoryTableChange(pag: { current?: number; pageSize?: number }) {
  historyPage.value = pag.current || 1
  historyPageSize.value = pag.pageSize || 10
  loadHistory()
}

async function ackAlert(id: number) {
  try {
    await acknowledgePlatformApiAlert(id)
    message.success('告警已确认')
    await Promise.all([loadHistory(), loadOverview()])
  } catch {
    message.error('确认失败')
  }
}

watch(activeTab, (tab) => {
  if (tab === 'history') {
    loadHistory()
  }
})

onMounted(loadOverview)
</script>

<style scoped>
.list-panel {
  padding: 16px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 1100px) {
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
