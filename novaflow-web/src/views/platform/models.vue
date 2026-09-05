<template>
  <div class="platform-admin-page page-shell">
    <div class="page-header">
      <div>
        <h1>模型概览</h1>
        <p>全平台模型供应商分布与跨租户治理</p>
      </div>
    </div>

    <div class="page-card list-panel">
      <a-spin :spinning="overviewLoading">
        <div v-if="overview" class="ops-stats-grid">
          <div class="stat-item">
            <span class="label">模型供应商</span>
            <strong>{{ formatPlatformNumber(overview.totalProviders) }}</strong>
            <span class="sub-label">启用 {{ overview.enabledProviders }}</span>
          </div>
          <div class="stat-item">
            <span class="label">模型配置</span>
            <strong>{{ formatPlatformNumber(overview.totalModelConfigs) }}</strong>
            <span class="sub-label">启用 {{ overview.enabledModelConfigs }}</span>
          </div>
        </div>

        <div v-if="overview" class="ops-panels">
          <div class="ops-panel-title">供应商分布</div>
          <a-table
            :columns="providerColumns"
            :data-source="overview.providersByCode"
            :pagination="false"
            row-key="providerCode"
            size="small"
          />
        </div>
      </a-spin>
    </div>

    <div class="page-card list-panel catalog-panel">
      <div class="ops-panel-title">平台模型目录</div>
      <div class="list-toolbar">
        <a-input-search
          v-model:value="catalogKeyword"
          placeholder="搜索模型"
          allow-clear
          style="width: 240px"
          @search="loadCatalog"
        />
        <a-button type="primary" @click="openCatalogModal()">新增模型</a-button>
      </div>
      <a-table
        :columns="catalogColumns"
        :data-source="catalogItems"
        :loading="catalogLoading"
        row-key="id"
        :pagination="catalogPagination"
        size="small"
        @change="onCatalogTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'enabled'">
            <a-badge :status="record.enabled ? 'success' : 'default'" :text="record.enabled ? '开放' : '停用'" />
          </template>
          <template v-else-if="column.key === 'price'">
            {{ record.inputPricePer1k ?? '-' }} / {{ record.outputPricePer1k ?? '-' }} {{ record.currency }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="openCatalogModal(record)">编辑</a-button>
              <a-popconfirm title="确认删除？" @confirm="removeCatalog(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <div class="page-card list-panel governance-panel">
      <div class="ops-panel-title">供应商治理</div>
      <div class="list-toolbar">
        <div class="list-toolbar-filters">
          <a-input-search
            v-model:value="keyword"
            placeholder="搜索供应商、Base URL"
            allow-clear
            style="width: 240px"
            @search="loadProviders"
          />
          <a-select
            v-model:value="tenantFilter"
            placeholder="所属租户"
            allow-clear
            show-search
            option-filter-prop="label"
            style="width: 200px"
            :options="tenantOptions"
            @change="loadProviders"
          />
          <a-select
            v-model:value="providerCodeFilter"
            placeholder="供应商类型"
            allow-clear
            style="width: 160px"
            @change="loadProviders"
          >
            <a-select-option v-for="preset in providerPresets" :key="preset.providerCode" :value="preset.providerCode">
              {{ preset.providerName }}
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="enabledFilter"
            placeholder="状态"
            allow-clear
            style="width: 120px"
            @change="loadProviders"
          >
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">停用</a-select-option>
          </a-select>
        </div>
        <span class="list-toolbar-meta">共 {{ providerTotal }} 条配置</span>
      </div>

      <a-table
        :columns="governanceColumns"
        :data-source="providers"
        :loading="providersLoading"
        row-key="id"
        :pagination="providerPagination"
        @change="onProviderTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'provider'">
            <div class="provider-cell">
              <strong>{{ record.providerName || record.providerCode }}</strong>
              <span class="provider-code">{{ record.providerCode }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'models'">
            {{ record.enabledModelCount || 0 }}/{{ record.modelCount || 0 }}
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-badge :status="record.enabled ? 'success' : 'default'" :text="record.enabled ? '启用' : '停用'" />
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button
                v-if="record.enabled"
                type="link"
                size="small"
                danger
                @click="toggleProvider(record, 0)"
              >
                停用
              </a-button>
              <a-button v-else type="link" size="small" @click="toggleProvider(record, 1)">启用</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="catalogModalOpen"
      :title="catalogEditingId ? '编辑模型目录' : '新增模型目录'"
      :confirm-loading="catalogSaving"
      @ok="saveCatalog"
    >
      <a-form layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="供应商" required>
              <a-input v-model:value="catalogForm.providerCode" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="模型名" required>
              <a-input v-model:value="catalogForm.modelName" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="显示名称">
          <a-input v-model:value="catalogForm.displayName" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="8">
            <a-form-item label="输入价/1K">
              <a-input-number v-model:value="catalogForm.inputPricePer1k" :min="0" :step="0.0001" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="输出价/1K">
              <a-input-number v-model:value="catalogForm.outputPricePer1k" :min="0" :step="0.0001" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="币种">
              <a-input v-model:value="catalogForm.currency" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="启用">
          <a-switch v-model:checked="catalogForm.enabled" />
        </a-form-item>
        <a-form-item label="说明">
          <a-textarea v-model:value="catalogForm.description" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  createPlatformModelCatalog,
  deletePlatformModelCatalog,
  fetchPlatformModelCatalog,
  fetchPlatformModelOverview,
  fetchPlatformModelProviders,
  fetchPlatformTenants,
  updatePlatformModelCatalog,
  updatePlatformModelProvider,
  type PlatformModelCatalogItem,
  type PlatformModelOverview,
  type PlatformModelProvider,
  type PlatformTenant,
} from '@/api/platform'
import { MODEL_PROVIDER_PRESETS } from '@/constants/modelProviders'
import { formatPlatformNumber } from '@/views/platform/shared/utils'
import '@/views/platform/shared/styles.css'

const overviewLoading = ref(false)
const providersLoading = ref(false)
const overview = ref<PlatformModelOverview | null>(null)
const providers = ref<PlatformModelProvider[]>([])
const providerTotal = ref(0)
const providerPage = ref(1)
const providerPageSize = ref(10)
const keyword = ref('')
const tenantFilter = ref<number | undefined>()
const providerCodeFilter = ref<string | undefined>()
const enabledFilter = ref<number | undefined>()
const tenantOptions = ref<{ label: string; value: number }[]>([])
const providerPresets = MODEL_PROVIDER_PRESETS
const catalogLoading = ref(false)
const catalogSaving = ref(false)
const catalogModalOpen = ref(false)
const catalogEditingId = ref<number | null>(null)
const catalogKeyword = ref('')
const catalogItems = ref<PlatformModelCatalogItem[]>([])
const catalogTotal = ref(0)
const catalogPage = ref(1)
const catalogPageSize = ref(10)
const catalogForm = reactive({
  providerCode: '',
  modelName: '',
  displayName: '',
  inputPricePer1k: 0,
  outputPricePer1k: 0,
  currency: 'CNY',
  enabled: true,
  description: '',
})

const catalogColumns = [
  { title: '供应商', dataIndex: 'providerCode', key: 'providerCode', width: 100 },
  { title: '模型', dataIndex: 'modelName', key: 'modelName' },
  { title: '显示名', dataIndex: 'displayName', key: 'displayName' },
  { title: '定价(入/出)', key: 'price', width: 180 },
  { title: '状态', key: 'enabled', width: 90 },
  { title: '操作', key: 'actions', width: 120 },
]

const catalogPagination = computed(() => ({
  current: catalogPage.value,
  pageSize: catalogPageSize.value,
  total: catalogTotal.value,
  showSizeChanger: true,
}))

const providerColumns = [
  { title: '供应商', dataIndex: 'providerCode', key: 'providerCode' },
  { title: '配置数', dataIndex: 'count', key: 'count', width: 120 },
]

const governanceColumns = [
  { title: '租户', dataIndex: 'tenantName', key: 'tenantName', width: 160, ellipsis: true },
  { title: '供应商', key: 'provider', width: 180 },
  { title: 'Base URL', dataIndex: 'baseUrl', key: 'baseUrl', ellipsis: true },
  { title: 'API Key', dataIndex: 'apiKeyMasked', key: 'apiKeyMasked', width: 140, ellipsis: true },
  { title: '模型(启用/总数)', key: 'models', width: 130 },
  { title: '状态', key: 'enabled', width: 90 },
  { title: '操作', key: 'actions', width: 100 },
]

const providerPagination = computed(() => ({
  current: providerPage.value,
  pageSize: providerPageSize.value,
  total: providerTotal.value,
  showSizeChanger: true,
}))

async function loadModelOverview() {
  overviewLoading.value = true
  try {
    const res = await fetchPlatformModelOverview()
    overview.value = res.data.data
  } catch {
    message.error('加载模型概览失败')
  } finally {
    overviewLoading.value = false
  }
}

async function loadTenantOptions() {
  try {
    const res = await fetchPlatformTenants({ page: 1, pageSize: 100 })
    tenantOptions.value = res.data.data.list.map((tenant: PlatformTenant) => ({
      label: tenant.tenantName,
      value: tenant.id,
    }))
  } catch {
    tenantOptions.value = []
  }
}

async function loadProviders() {
  providersLoading.value = true
  try {
    const res = await fetchPlatformModelProviders({
      page: providerPage.value,
      pageSize: providerPageSize.value,
      keyword: keyword.value || undefined,
      tenantId: tenantFilter.value,
      providerCode: providerCodeFilter.value,
      enabled: enabledFilter.value,
    })
    providers.value = res.data.data.list
    providerTotal.value = res.data.data.total
  } catch {
    message.error('加载供应商列表失败')
  } finally {
    providersLoading.value = false
  }
}

function onProviderTableChange(pag: { current?: number; pageSize?: number }) {
  providerPage.value = pag.current || 1
  providerPageSize.value = pag.pageSize || 10
  loadProviders()
}

async function toggleProvider(record: PlatformModelProvider, enabled: number) {
  try {
    await updatePlatformModelProvider(record.id, { enabled })
    message.success(enabled === 1 ? '供应商已启用' : '供应商已停用')
    await Promise.all([loadProviders(), loadModelOverview()])
  } catch {
    message.error('操作失败')
  }
}

async function loadCatalog() {
  catalogLoading.value = true
  try {
    const res = await fetchPlatformModelCatalog({
      page: catalogPage.value,
      pageSize: catalogPageSize.value,
      keyword: catalogKeyword.value || undefined,
    })
    catalogItems.value = res.data.data.list
    catalogTotal.value = res.data.data.total
  } catch {
    message.error('加载模型目录失败')
  } finally {
    catalogLoading.value = false
  }
}

function onCatalogTableChange(pag: { current?: number; pageSize?: number }) {
  catalogPage.value = pag.current || 1
  catalogPageSize.value = pag.pageSize || 10
  loadCatalog()
}

function openCatalogModal(record?: PlatformModelCatalogItem) {
  if (record) {
    catalogEditingId.value = record.id
    catalogForm.providerCode = record.providerCode
    catalogForm.modelName = record.modelName
    catalogForm.displayName = record.displayName || ''
    catalogForm.inputPricePer1k = record.inputPricePer1k || 0
    catalogForm.outputPricePer1k = record.outputPricePer1k || 0
    catalogForm.currency = record.currency || 'CNY'
    catalogForm.enabled = record.enabled ?? true
    catalogForm.description = record.description || ''
  } else {
    catalogEditingId.value = null
    catalogForm.providerCode = ''
    catalogForm.modelName = ''
    catalogForm.displayName = ''
    catalogForm.inputPricePer1k = 0
    catalogForm.outputPricePer1k = 0
    catalogForm.currency = 'CNY'
    catalogForm.enabled = true
    catalogForm.description = ''
  }
  catalogModalOpen.value = true
}

async function saveCatalog() {
  if (!catalogForm.providerCode.trim() || !catalogForm.modelName.trim()) {
    message.warning('请填写供应商与模型名')
    return
  }
  catalogSaving.value = true
  try {
    const payload = {
      providerCode: catalogForm.providerCode,
      modelName: catalogForm.modelName,
      displayName: catalogForm.displayName,
      inputPricePer1k: catalogForm.inputPricePer1k,
      outputPricePer1k: catalogForm.outputPricePer1k,
      currency: catalogForm.currency,
      enabled: catalogForm.enabled ? 1 : 0,
      description: catalogForm.description,
    }
    if (catalogEditingId.value) {
      await updatePlatformModelCatalog(catalogEditingId.value, payload)
    } else {
      await createPlatformModelCatalog(payload)
    }
    message.success('已保存')
    catalogModalOpen.value = false
    await loadCatalog()
  } catch {
    message.error('保存失败')
  } finally {
    catalogSaving.value = false
  }
}

async function removeCatalog(id: number) {
  await deletePlatformModelCatalog(id)
  message.success('已删除')
  await loadCatalog()
}

onMounted(async () => {
  await Promise.all([loadModelOverview(), loadTenantOptions(), loadProviders(), loadCatalog()])
})
</script>

<style scoped>
.list-panel {
  padding: 16px;
}

.governance-panel {
  margin-top: 16px;
}

.catalog-panel {
  margin-top: 16px;
}

.provider-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.provider-code {
  color: var(--text-secondary, #8c8c8c);
  font-size: 12px;
}
</style>
