<template>
  <div class="platform-admin-page page-shell">
    <div class="page-header">
      <div>
        <h1>系统配置</h1>
        <p>平台级运行参数、API 监控阈值与模型供应商白名单</p>
      </div>
    </div>

    <div class="page-card settings-panel">
      <a-spin :spinning="loading">
        <a-form layout="vertical" style="max-width: 720px">
          <a-divider orientation="left">账号与注册</a-divider>
          <a-form-item label="开放自助注册">
            <a-switch
              v-model:checked="registrationEnabled"
              checked-children="开启"
              un-checked-children="关闭"
            />
            <div class="field-hint">关闭后，租户端注册页将拒绝新用户自助注册，仅支持平台代开户或邀请。</div>
          </a-form-item>

          <a-divider orientation="left">API 监控告警</a-divider>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="近 1 小时调用阈值">
                <a-input-number v-model:value="hourlyCallsThreshold" :min="1" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="流量突增倍数（相对 7 日均值）">
                <a-input-number v-model:value="trafficSpikeMultiplier" :min="1" :step="0.5" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-divider orientation="left">模型供应商白名单</a-divider>
          <a-form-item label="启用白名单">
            <a-switch
              v-model:checked="providerWhitelistEnabled"
              checked-children="启用"
              un-checked-children="全部允许"
            />
            <div class="field-hint">启用后，租户仅能配置下方勾选的模型供应商。</div>
          </a-form-item>
          <a-form-item v-if="providerWhitelistEnabled" label="允许的供应商">
            <a-select
              v-model:value="allowedProviderCodes"
              mode="multiple"
              placeholder="选择允许的模型供应商"
              :options="providerOptions"
              style="width: 100%"
            />
          </a-form-item>

          <a-divider orientation="left">平台运营</a-divider>
          <a-form-item label="维护模式">
            <a-switch v-model:checked="maintenanceEnabled" checked-children="开启" un-checked-children="关闭" />
            <div class="field-hint">开启后可在租户端展示维护提示（需前端接入）。</div>
          </a-form-item>
          <a-form-item label="维护提示文案">
            <a-textarea v-model:value="maintenanceMessage" :rows="3" placeholder="维护期间展示给用户的说明" />
          </a-form-item>
          <a-form-item label="平台公告">
            <a-textarea v-model:value="platformAnnouncement" :rows="3" placeholder="展示在平台运营后台的公告" />
          </a-form-item>

          <a-form-item>
            <a-button type="primary" :loading="saving" @click="saveSettings">保存配置</a-button>
          </a-form-item>
        </a-form>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { fetchPlatformSettings, updatePlatformSettings } from '@/api/platform'
import { MODEL_PROVIDER_PRESETS } from '@/constants/modelProviders'
import '@/views/platform/shared/styles.css'

const loading = ref(false)
const saving = ref(false)
const registrationEnabled = ref(true)
const hourlyCallsThreshold = ref(500)
const trafficSpikeMultiplier = ref(3)
const providerWhitelistEnabled = ref(false)
const allowedProviderCodes = ref<string[]>([])
const maintenanceEnabled = ref(false)
const maintenanceMessage = ref('')
const platformAnnouncement = ref('')

const providerOptions = computed(() =>
  MODEL_PROVIDER_PRESETS.map((item) => ({
    label: item.providerName,
    value: item.providerCode,
  })),
)

async function loadSettings() {
  loading.value = true
  try {
    const res = await fetchPlatformSettings()
    const data = res.data.data
    registrationEnabled.value = data.registrationEnabled
    hourlyCallsThreshold.value = data.hourlyCallsThreshold
    trafficSpikeMultiplier.value = data.trafficSpikeMultiplier
    providerWhitelistEnabled.value = data.providerWhitelistEnabled
    allowedProviderCodes.value = data.allowedProviderCodes || []
    maintenanceEnabled.value = data.maintenanceEnabled ?? false
    maintenanceMessage.value = data.maintenanceMessage || ''
    platformAnnouncement.value = data.platformAnnouncement || ''
  } catch {
    message.error('加载系统配置失败')
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  saving.value = true
  try {
    await updatePlatformSettings({
      registrationEnabled: registrationEnabled.value,
      hourlyCallsThreshold: hourlyCallsThreshold.value,
      trafficSpikeMultiplier: trafficSpikeMultiplier.value,
      allowedProviderCodes: providerWhitelistEnabled.value ? allowedProviderCodes.value : [],
      maintenanceEnabled: maintenanceEnabled.value,
      maintenanceMessage: maintenanceMessage.value,
      platformAnnouncement: platformAnnouncement.value,
    })
    message.success('配置已保存')
    await loadSettings()
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<style scoped>
.settings-panel {
  padding: 24px;
}

.field-hint {
  margin-top: 8px;
  color: var(--text-secondary, #8c8c8c);
  font-size: 13px;
  line-height: 1.5;
}
</style>
