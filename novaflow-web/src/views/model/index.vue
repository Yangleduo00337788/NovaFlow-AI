<template>
  <div class="model-page">
    <div class="page-header">
      <div>
        <h1>模型中心</h1>
        <p>管理模型提供商、API Key 与可用模型配置</p>
      </div>
      <a-radio-group v-model:value="activeTab" button-style="solid">
        <a-radio-button value="providers">提供商</a-radio-button>
        <a-radio-button value="models">模型列表</a-radio-button>
        <a-radio-button value="stats">调用统计</a-radio-button>
      </a-radio-group>
    </div>

    <div v-if="activeTab === 'providers'" class="providers-section">
      <div class="overview-grid">
        <div class="overview-card">
          <div class="overview-head">
            <CloudServerOutlined class="overview-icon" />
            <span class="overview-label">已配置提供商</span>
          </div>
          <strong class="overview-value">{{ overview?.configuredProviders ?? 0 }}</strong>
        </div>
        <div class="overview-card">
          <div class="overview-head">
            <ExperimentOutlined class="overview-icon" />
            <span class="overview-label">可用模型</span>
          </div>
          <strong class="overview-value">{{ overview?.enabledModels ?? 0 }}</strong>
        </div>
        <div class="overview-card">
          <div class="overview-head">
            <ApiOutlined class="overview-icon" />
            <span class="overview-label">累计调用</span>
          </div>
          <strong class="overview-value">{{ formatNumber(overview?.totalCalls ?? 0) }}</strong>
        </div>
        <div class="overview-card">
          <div class="overview-head">
            <ThunderboltOutlined class="overview-icon" />
            <span class="overview-label">累计 Token</span>
          </div>
          <strong class="overview-value">{{ formatNumber(overview?.totalTokens ?? 0) }}</strong>
        </div>
      </div>

      <a-spin :spinning="providersLoading">
        <div class="provider-grid">
          <div
            v-for="provider in providers"
            :key="provider.providerCode"
            class="provider-card page-card"
            :class="{ configured: provider.configured, disabled: provider.configured && !provider.enabled }"
          >
            <div class="provider-head">
              <div class="provider-icon" :class="provider.providerCode">
                <component :is="getProviderIcon(provider.providerCode)" />
              </div>
              <div class="provider-meta">
                <div class="provider-title-row">
                  <h3>{{ provider.providerName }}</h3>
                  <a-tag v-if="provider.configured && provider.enabled" color="success">已启用</a-tag>
                  <a-tag v-else-if="provider.configured" color="default">已配置</a-tag>
                  <a-tag v-else color="warning">未配置</a-tag>
                </div>
                <p>{{ provider.description }}</p>
              </div>
            </div>

            <div class="provider-info">
              <div class="info-row">
                <span><LinkOutlined /> Base URL</span>
                <code>{{ provider.baseUrl || provider.defaultBaseUrl }}</code>
              </div>
              <div v-if="provider.configured" class="info-row">
                <span><KeyOutlined /> API Key</span>
                <code>{{ provider.apiKeyMasked || '****' }}</code>
              </div>
              <div v-if="provider.configured" class="info-row">
                <span><AppstoreOutlined /> 模型数量</span>
                <strong>{{ provider.modelCount }}</strong>
              </div>
              <div v-else class="provider-empty-hint">
                <InfoCircleOutlined />
                <span>尚未配置 API Key，点击「立即配置」完成接入</span>
              </div>
            </div>

            <div class="provider-actions">
              <a-button type="primary" @click="openProviderDrawer(provider)">
                <template #icon><SettingOutlined /></template>
                {{ provider.configured ? '编辑配置' : '立即配置' }}
              </a-button>
              <a-button
                v-if="provider.configured && provider.id"
                :loading="syncingProviderId === provider.id"
                @click="onSyncProvider(provider)"
              >
                <template #icon><SyncOutlined /></template>
                同步模型
              </a-button>
              <a-button
                v-if="provider.configured && provider.id"
                :loading="testingProviderId === provider.id"
                @click="onTestProvider(provider)"
              >
                <template #icon><ApiOutlined /></template>
                连通性测试
              </a-button>
              <a-popconfirm
                v-if="provider.configured && provider.id"
                title="确认删除该提供商配置？关联模型将一并删除"
                @confirm="onDeleteProvider(provider.id!)"
              >
                <a-button danger type="text">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </div>
          </div>
        </div>
      </a-spin>
    </div>

    <div v-else-if="activeTab === 'models'" class="page-card models-section">
      <div class="models-toolbar">
        <a-space>
          <a-select
            v-model:value="filterProviderId"
            allow-clear
            placeholder="按提供商筛选"
            style="width: 180px"
            @change="loadConfigs"
          >
            <a-select-option
              v-for="item in configuredProviders"
              :key="item.id"
              :value="item.id"
            >
              {{ item.providerName }}
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="filterModelType"
            allow-clear
            placeholder="按类型筛选"
            style="width: 140px"
            @change="loadConfigs"
          >
            <a-select-option value="chat">Chat</a-select-option>
            <a-select-option value="embedding">Embedding</a-select-option>
            <a-select-option value="rerank">Rerank</a-select-option>
          </a-select>
        </a-space>
        <a-button type="primary" :disabled="configuredProviders.length === 0" @click="openModelDrawer()">
          添加模型
        </a-button>
      </div>

      <a-table
        :columns="modelColumns"
        :data-source="configs"
        :loading="configsLoading"
        row-key="id"
        :pagination="false"
      >
        <template #emptyText>
          <a-empty description="暂无模型，请先配置提供商并同步上游模型">
            <template #image>
              <InboxOutlined class="table-empty-icon" />
            </template>
            <a-button type="primary" :disabled="configuredProviders.length === 0" @click="openModelDrawer()">
              <template #icon><PlusOutlined /></template>
              添加模型
            </a-button>
          </a-empty>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'modelType'">
            <a-tag>{{ record.modelType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-tag :color="record.enabled ? 'success' : 'default'">{{ record.enabled ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'isDefault'">
            <a-tag v-if="record.isDefault" color="blue">默认</a-tag>
            <span v-else class="muted">-</span>
          </template>
          <template v-else-if="column.key === 'price'">
            <span class="price-text">
              {{ formatPrice(record.inputPrice) }} / {{ formatPrice(record.outputPrice) }}
            </span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="openModelDrawer(record)">编辑</a-button>
              <a-button v-if="!record.isDefault" type="link" @click="onSetDefault(record.id)">设为默认</a-button>
              <a-popconfirm title="确认删除该模型？" @confirm="onDeleteConfig(record.id)">
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <div v-else class="page-card stats-section">
      <div v-if="overview && overview.totalCalls > 0" class="stats-content">
        <div class="stats-summary">
          <div class="stats-item">
            <span>累计调用</span>
            <strong>{{ formatNumber(overview.totalCalls) }}</strong>
          </div>
          <div class="stats-item">
            <span>累计 Token</span>
            <strong>{{ formatNumber(overview.totalTokens) }}</strong>
          </div>
          <div class="stats-item">
            <span>累计成本</span>
            <strong>${{ overview.totalCost }}</strong>
          </div>
        </div>
        <a-table
          :columns="statsColumns"
          :data-source="overview.topModels"
          row-key="modelName"
          :pagination="false"
        />
      </div>
      <a-empty v-else description="暂无调用记录，完成 Agent 调试对话后将在此展示统计">
        <template #image>
          <LineChartOutlined class="stats-empty-icon" />
        </template>
      </a-empty>
    </div>

    <a-drawer
      v-model:open="providerDrawerOpen"
      :title="`${editingProvider?.providerName || ''} 配置`"
      width="520"
      @close="resetProviderForm"
    >
      <a-form layout="vertical" :model="providerForm">
        <a-form-item label="Base URL" required>
          <a-input v-model:value="providerForm.baseUrl" placeholder="https://api.openai.com/v1" />
        </a-form-item>
        <a-form-item label="API Key" required>
          <a-input-password
            v-model:value="providerForm.apiKey"
            :placeholder="editingProvider?.apiKeyMasked || '请输入 API Key'"
          />
        </a-form-item>
        <a-form-item label="启用状态">
          <a-switch v-model:checked="providerForm.enabled" />
        </a-form-item>
        <a-space>
          <a-button type="primary" :loading="providerSaving" @click="onSaveProvider">保存</a-button>
          <a-button
            v-if="editingProvider?.configured && editingProvider.id"
            :loading="testingProviderId === editingProvider.id"
            @click="onTestProvider(editingProvider, true)"
          >
            测试连接
          </a-button>
        </a-space>
      </a-form>
    </a-drawer>

    <a-drawer
      v-model:open="modelDrawerOpen"
      :title="editingConfigId ? '编辑模型' : '添加模型'"
      width="560"
      @close="resetModelForm"
    >
      <a-form layout="vertical" :model="modelForm">
        <a-form-item label="提供商" required>
          <a-select v-model:value="modelForm.providerId" :disabled="!!editingConfigId">
            <a-select-option
              v-for="item in configuredProviders"
              :key="item.id"
              :value="item.id"
            >
              {{ item.providerName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="模型名称" required>
          <a-input v-model:value="modelForm.modelName" placeholder="gpt-4o / deepseek-chat" />
        </a-form-item>
        <a-form-item label="显示名称" required>
          <a-input v-model:value="modelForm.displayName" placeholder="GPT-4o" />
        </a-form-item>
        <a-form-item label="模型类型" required>
          <a-select v-model:value="modelForm.modelType">
            <a-select-option value="chat">Chat</a-select-option>
            <a-select-option value="embedding">Embedding</a-select-option>
            <a-select-option value="rerank">Rerank</a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="上下文窗口">
              <a-input-number v-model:value="modelForm.contextWindow" :min="1024" :step="1024" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="最大输出 Token">
              <a-input-number v-model:value="modelForm.maxOutputTokens" :min="256" :step="256" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="输入单价（/1K Token）">
              <a-input-number v-model:value="modelForm.inputPrice" :min="0" :step="0.0001" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="输出单价（/1K Token）">
              <a-input-number v-model:value="modelForm.outputPrice" :min="0" :step="0.0001" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="默认 Temperature">
          <a-input-number v-model:value="modelForm.defaultTemperature" :min="0" :max="2" :step="0.1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="启用">
          <a-switch v-model:checked="modelForm.enabled" />
        </a-form-item>
        <a-form-item label="设为默认模型">
          <a-switch v-model:checked="modelForm.isDefault" />
        </a-form-item>
        <a-button type="primary" :loading="modelSaving" @click="onSaveModel">保存</a-button>
      </a-form>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { Component } from 'vue'
import {
  ApiOutlined,
  AppstoreOutlined,
  CloudServerOutlined,
  DeleteOutlined,
  ExperimentOutlined,
  InboxOutlined,
  InfoCircleOutlined,
  KeyOutlined,
  LineChartOutlined,
  LinkOutlined,
  PlusOutlined,
  SettingOutlined,
  SyncOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import {
  createModelConfig,
  deleteModelConfig,
  deleteModelProvider,
  fetchModelConfigs,
  fetchModelOverview,
  fetchModelProviders,
  saveModelProvider,
  setDefaultModelConfig,
  syncModelProvider,
  testModelProvider,
  updateModelConfig,
  updateModelProvider,
  type ModelConfigItem,
  type ModelConfigSaveRequest,
  type ModelOverview,
  type ModelProviderItem,
} from '@/api/model'
import { mergeModelProviders, MODEL_PROVIDER_PRESETS } from '@/constants/modelProviders'

const activeTab = ref('providers')
const overview = ref<ModelOverview | null>(null)
const providers = ref<ModelProviderItem[]>([...MODEL_PROVIDER_PRESETS])
const configs = ref<ModelConfigItem[]>([])
const configsLoading = ref(false)
const providersLoading = ref(false)
const providerDrawerOpen = ref(false)
const modelDrawerOpen = ref(false)
const providerSaving = ref(false)
const modelSaving = ref(false)
const testingProviderId = ref<number | null>(null)
const syncingProviderId = ref<number | null>(null)
const editingProvider = ref<ModelProviderItem | null>(null)
const editingConfigId = ref<number | null>(null)
const filterProviderId = ref<number>()
const filterModelType = ref<string>()

const providerForm = reactive({
  baseUrl: '',
  apiKey: '',
  enabled: true,
})

const modelForm = reactive<ModelConfigSaveRequest & { enabled: boolean; isDefault: boolean }>({
  providerId: 0,
  modelName: '',
  modelType: 'chat',
  displayName: '',
  contextWindow: 4096,
  maxOutputTokens: 2048,
  inputPrice: 0,
  outputPrice: 0,
  defaultTemperature: 0.7,
  enabled: true,
  isDefault: false,
})

const configuredProviders = computed(() => providers.value.filter((item) => item.configured && item.id))

const modelColumns = [
  { title: '显示名称', dataIndex: 'displayName', key: 'displayName' },
  { title: '模型名称', dataIndex: 'modelName', key: 'modelName' },
  { title: '提供商', dataIndex: 'providerName', key: 'providerName' },
  { title: '类型', key: 'modelType' },
  { title: '上下文', dataIndex: 'contextWindow', key: 'contextWindow' },
  { title: '单价(入/出)', key: 'price', width: 160 },
  { title: '状态', key: 'enabled', width: 90 },
  { title: '默认', key: 'isDefault', width: 80 },
  { title: '操作', key: 'action', width: 220 },
]

const statsColumns = [
  { title: '模型', dataIndex: 'displayName', key: 'displayName' },
  { title: '模型名称', dataIndex: 'modelName', key: 'modelName' },
  { title: '调用次数', dataIndex: 'calls', key: 'calls' },
  { title: 'Token 消耗', dataIndex: 'tokens', key: 'tokens' },
]

const providerIconMap: Record<string, Component> = {
  openai: ExperimentOutlined,
  deepseek: ThunderboltOutlined,
}

function getProviderIcon(code: string) {
  return providerIconMap[code] || ApiOutlined
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value)
}

function formatPrice(value?: number) {
  if (value == null) return '-'
  return `$${value}`
}

async function loadOverview() {
  try {
    const res = await fetchModelOverview()
    overview.value = res.data.data
  } catch {
    overview.value = {
      totalCalls: 0,
      totalTokens: 0,
      totalCost: '0.00',
      configuredProviders: providers.value.filter((item) => item.configured).length,
      enabledModels: 0,
      topModels: [],
    }
  }
}

async function loadProviders() {
  providersLoading.value = true
  try {
    const res = await fetchModelProviders()
    providers.value = mergeModelProviders(res.data.data || [])
  } catch {
    providers.value = [...MODEL_PROVIDER_PRESETS]
  } finally {
    providersLoading.value = false
  }
}

async function loadConfigs() {
  configsLoading.value = true
  try {
    const res = await fetchModelConfigs({
      providerId: filterProviderId.value,
      modelType: filterModelType.value,
    })
    configs.value = res.data.data
  } catch {
    configs.value = []
  } finally {
    configsLoading.value = false
  }
}

async function loadAll() {
  await loadProviders()
  await Promise.all([loadOverview(), loadConfigs()])
}

function openProviderDrawer(provider: ModelProviderItem) {
  editingProvider.value = provider
  providerForm.baseUrl = provider.baseUrl || provider.defaultBaseUrl || ''
  providerForm.apiKey = provider.apiKeyMasked || ''
  providerForm.enabled = provider.configured ? provider.enabled : true
  providerDrawerOpen.value = true
}

function resetProviderForm() {
  editingProvider.value = null
  providerForm.baseUrl = ''
  providerForm.apiKey = ''
  providerForm.enabled = true
}

async function onSaveProvider() {
  if (!editingProvider.value) return
  if (!providerForm.baseUrl.trim()) {
    message.warning('请填写 Base URL')
    return
  }
  const isMaskedKey = providerForm.apiKey.includes('****')
  if (!providerForm.apiKey.trim() || (!editingProvider.value.configured && isMaskedKey)) {
    message.warning('请填写 API Key')
    return
  }

  providerSaving.value = true
  try {
    const payload = {
      providerCode: editingProvider.value.providerCode,
      baseUrl: providerForm.baseUrl.trim(),
      apiKey: providerForm.apiKey.trim(),
      enabled: providerForm.enabled,
    }
    if (editingProvider.value.configured && editingProvider.value.id) {
      await updateModelProvider(editingProvider.value.id, payload)
    } else {
      await saveModelProvider(payload)
    }
    const synced = !editingProvider.value.configured || !providerForm.apiKey.includes('****')
    message.success(synced ? '保存成功，已自动同步上游模型' : '保存成功')
    providerDrawerOpen.value = false
    await loadAll()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    providerSaving.value = false
  }
}

async function onSyncProvider(provider: ModelProviderItem) {
  if (!provider.id) return
  syncingProviderId.value = provider.id
  try {
    const res = await syncModelProvider(provider.id)
    message.success(res.data.data.message)
    await loadAll()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '同步失败')
  } finally {
    syncingProviderId.value = null
  }
}

async function onTestProvider(provider: ModelProviderItem, fromDrawer = false) {
  if (!provider.id) return
  testingProviderId.value = provider.id
  try {
    const res = await testModelProvider(provider.id, fromDrawer ? {
      apiKey: providerForm.apiKey,
      baseUrl: providerForm.baseUrl,
    } : undefined)
    const result = res.data.data
    if (result.success) {
      message.success(`${result.message}（${result.latencyMs}ms）`)
    } else {
      message.error(result.message)
    }
  } catch (e) {
    message.error(e instanceof Error ? e.message : '测试失败')
  } finally {
    testingProviderId.value = null
  }
}

async function onDeleteProvider(id: number) {
  await deleteModelProvider(id)
  message.success('删除成功')
  await loadAll()
}

function openModelDrawer(record?: ModelConfigItem) {
  if (record) {
    editingConfigId.value = record.id
    Object.assign(modelForm, {
      providerId: record.providerId,
      modelName: record.modelName,
      modelType: record.modelType,
      displayName: record.displayName,
      contextWindow: record.contextWindow,
      maxOutputTokens: record.maxOutputTokens,
      inputPrice: record.inputPrice,
      outputPrice: record.outputPrice,
      defaultTemperature: record.defaultTemperature,
      enabled: record.enabled,
      isDefault: record.isDefault,
    })
  } else {
    editingConfigId.value = null
    const firstProvider = configuredProviders.value[0]
    Object.assign(modelForm, {
      providerId: firstProvider?.id || 0,
      modelName: '',
      modelType: 'chat',
      displayName: '',
      contextWindow: 4096,
      maxOutputTokens: 2048,
      inputPrice: 0,
      outputPrice: 0,
      defaultTemperature: 0.7,
      enabled: true,
      isDefault: false,
    })
  }
  modelDrawerOpen.value = true
}

function resetModelForm() {
  editingConfigId.value = null
}

async function onSaveModel() {
  if (!modelForm.providerId) {
    message.warning('请选择提供商')
    return
  }
  if (!modelForm.modelName.trim() || !modelForm.displayName.trim()) {
    message.warning('请填写模型名称与显示名称')
    return
  }

  modelSaving.value = true
  try {
    const payload: ModelConfigSaveRequest = {
      providerId: modelForm.providerId,
      modelName: modelForm.modelName.trim(),
      modelType: modelForm.modelType,
      displayName: modelForm.displayName.trim(),
      contextWindow: modelForm.contextWindow,
      maxOutputTokens: modelForm.maxOutputTokens,
      inputPrice: modelForm.inputPrice,
      outputPrice: modelForm.outputPrice,
      defaultTemperature: modelForm.defaultTemperature,
      enabled: modelForm.enabled,
      isDefault: modelForm.isDefault,
    }
    if (editingConfigId.value) {
      await updateModelConfig(editingConfigId.value, payload)
    } else {
      await createModelConfig(payload)
    }
    message.success('保存成功')
    modelDrawerOpen.value = false
    await loadAll()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    modelSaving.value = false
  }
}

async function onSetDefault(id: number) {
  await setDefaultModelConfig(id)
  message.success('已设为默认模型')
  await loadConfigs()
}

async function onDeleteConfig(id: number) {
  await deleteModelConfig(id)
  message.success('删除成功')
  await loadAll()
}

onMounted(loadAll)

watch(activeTab, (tab) => {
  if (tab === 'providers' || tab === 'stats') {
    loadOverview()
  }
})
</script>

<style scoped>
.model-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.page-header h1 {
  margin: 0 0 4px;
  color: var(--text-primary);
}

.page-header p {
  margin: 0;
  color: var(--text-secondary);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.overview-card {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: var(--card-shadow);
}

.overview-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.overview-icon {
  font-size: 14px;
  color: #1677ff;
}

.overview-label {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
}

.overview-value {
  font-size: 24px;
  color: var(--text-primary);
}

.provider-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.provider-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 240px;
}

.provider-card.configured {
  border-color: rgba(22, 119, 255, 0.25);
}

.provider-card.disabled {
  opacity: 0.88;
}

.provider-head {
  display: flex;
  gap: 14px;
}

.provider-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
  flex-shrink: 0;
}

.provider-icon.openai {
  background: linear-gradient(135deg, #10a37f, #1a7f64);
}

.provider-icon.deepseek {
  background: linear-gradient(135deg, #4f6ef7, #1677ff);
}

.provider-meta h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.provider-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.provider-meta p {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.provider-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}

.info-row span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.provider-empty-hint {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--bg-subtle);
  border: 1px dashed var(--border);
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.provider-empty-hint .anticon {
  color: #1677ff;
  margin-top: 2px;
  flex-shrink: 0;
}

.info-row code {
  font-size: 12px;
  color: var(--text-body);
  word-break: break-all;
  text-align: right;
}

.provider-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
}

.models-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.price-text,
.muted {
  color: var(--text-muted);
  font-size: 12px;
}

.stats-section {
  min-height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.stats-section .stats-content {
  width: 100%;
  align-self: stretch;
}

.stats-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stats-item {
  padding: 16px 20px;
  background: var(--bg-secondary, #fafafa);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stats-item span {
  color: var(--text-muted);
  font-size: 13px;
}

.stats-item strong {
  font-size: 24px;
}

.stats-empty-icon,
.table-empty-icon {
  font-size: 48px;
  color: var(--text-muted);
}

.stats-hint {
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.stats-hint p {
  margin: 0;
}

@media (max-width: 1200px) {
  .overview-grid,
  .provider-grid {
    grid-template-columns: 1fr;
  }
}
</style>
