<template>
  <div class="platform-admin-page page-shell" data-testid="platform-tenant-detail">
    <div class="page-header">
      <div>
        <a-button type="link" class="back-link" @click="goBack">← 返回租户列表</a-button>
        <h1>{{ detail?.tenant.tenantName || '租户详情' }}</h1>
        <p v-if="detail">
          {{ detail.tenant.tenantCode }}
          <a-tag class="header-tag">{{ detail.tenant.planTypeLabel }}</a-tag>
          <a-badge
            :status="detail.tenant.status === 1 ? 'success' : 'default'"
            :text="detail.tenant.status === 1 ? '正常' : '停用'"
          />
          <a-tag v-if="detail.expired" color="red">已到期</a-tag>
          <a-tag v-else-if="detail.daysUntilExpiry != null && detail.daysUntilExpiry <= 30" color="orange">
            {{ detail.daysUntilExpiry }} 天后到期
          </a-tag>
        </p>
      </div>
      <a-space>
        <a-button type="primary" :disabled="!detail" @click="openEdit">编辑租户</a-button>
        <a-button :disabled="!detail" @click="openResetOwner">重置 Owner 密码</a-button>
      </a-space>
    </div>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <div class="page-card quota-panel">
          <h3>套餐与配额</h3>
          <div class="info-grid">
            <div class="info-item">
              <span>到期时间</span>
              <strong>{{ formatPlatformDateTime(detail.tenant.expireAt) }}</strong>
            </div>
            <div class="info-item">
              <span>成员席位</span>
              <strong>{{ detail.tenant.memberCount || 0 }} / {{ detail.tenant.maxMembers || 0 }}</strong>
              <a-progress
                v-if="detail.memberUsedPercent != null"
                :percent="detail.memberUsedPercent"
                size="small"
                :status="detail.memberUsedPercent >= 90 ? 'exception' : 'normal'"
              />
            </div>
            <div class="info-item">
              <span>Agent</span>
              <strong>{{ detail.agentCount }} / {{ detail.tenant.maxAgents || 0 }}</strong>
              <a-progress
                v-if="agentPercent != null"
                :percent="agentPercent"
                size="small"
                :status="agentPercent >= 90 ? 'exception' : 'normal'"
              />
            </div>
            <div class="info-item">
              <span>知识库</span>
              <strong>{{ detail.knowledgeCount }} / {{ detail.tenant.maxKnowledge || 0 }}</strong>
              <a-progress
                v-if="knowledgePercent != null"
                :percent="knowledgePercent"
                size="small"
                :status="knowledgePercent >= 90 ? 'exception' : 'normal'"
              />
            </div>
            <div class="info-item">
              <span>存储配额</span>
              <strong>
                {{ formatStorageMb(detail.tenant.usedStorageBytes) }}
                / {{ formatPlatformNumber(detail.tenant.maxStorageMb) }} MB
              </strong>
              <a-progress
                v-if="detail.storageUsedPercent != null"
                :percent="detail.storageUsedPercent"
                size="small"
                :status="detail.storageUsedPercent >= 90 ? 'exception' : 'normal'"
              />
            </div>
            <div class="info-item">
              <span>月 Token 配额</span>
              <strong>
                {{ formatPlatformNumber(detail.tenant.usedTokensThisMonth) }}
                / {{ formatPlatformNumber(detail.tenant.monthlyTokenQuota) }}
              </strong>
              <a-progress
                v-if="detail.tokenUsedPercent != null"
                :percent="detail.tokenUsedPercent"
                size="small"
                :status="detail.tokenUsedPercent >= 90 ? 'exception' : 'normal'"
              />
            </div>
          </div>
        </div>

        <div class="ops-stats-grid page-card">
          <div class="stat-item">
            <span class="label">应用数</span>
            <strong>{{ formatPlatformNumber(detail.applicationCount) }}</strong>
          </div>
          <div class="stat-item">
            <span class="label">工作流数</span>
            <strong>{{ formatPlatformNumber(detail.workflowCount) }}</strong>
          </div>
          <div class="stat-item">
            <span class="label">本月调用</span>
            <strong>{{ formatPlatformNumber(detail.callsThisMonth) }}</strong>
          </div>
          <div class="stat-item">
            <span class="label">本月费用 (CNY)</span>
            <strong>{{ formatPlatformCost(detail.costCnyThisMonth) }}</strong>
          </div>
        </div>

        <a-row :gutter="16">
          <a-col :span="12">
            <div class="page-card list-panel">
              <div class="ops-panel-title">本月 Token 趋势</div>
              <div v-if="detail.dailyTokenTrend.length" class="trend-bars">
                <div v-for="point in detail.dailyTokenTrend" :key="point.label" class="trend-bar-item">
                  <span class="trend-label">{{ point.label }}</span>
                  <div class="trend-bar-track">
                    <div class="trend-bar-fill" :style="{ width: trendBarWidth(point.tokens) }" />
                  </div>
                  <span class="trend-value">{{ formatPlatformNumber(point.tokens) }}</span>
                </div>
              </div>
              <a-empty v-else description="暂无用量数据" />
            </div>
          </a-col>
          <a-col :span="12">
            <div class="page-card list-panel">
              <div class="ops-panel-title">本月模型 Top</div>
              <a-table
                v-if="detail.topModelsThisMonth.length"
                :columns="modelColumns"
                :data-source="detail.topModelsThisMonth"
                :pagination="false"
                size="small"
                :row-key="(record: PlatformModelUsage) => record.modelName || record.displayName || 'unknown'"
              />
              <a-empty v-else description="暂无模型用量" />
            </div>
          </a-col>
        </a-row>

        <div class="page-card list-panel contact-panel">
          <h3>联系信息</h3>
          <a-descriptions :column="3" size="small">
            <a-descriptions-item label="联系人">{{ detail.tenant.contactName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ detail.tenant.contactEmail || '-' }}</a-descriptions-item>
            <a-descriptions-item label="电话">{{ detail.tenant.contactPhone || '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatPlatformDateTime(detail.tenant.createdAt) }}</a-descriptions-item>
            <a-descriptions-item label="更新时间">{{ formatPlatformDateTime(detail.tenant.updatedAt) }}</a-descriptions-item>
          </a-descriptions>
        </div>
      </template>
    </a-spin>

    <TenantEditModal v-model:open="modalOpen" :saving="saving" :form="form" @save="saveTenant" />

    <a-modal
      v-model:open="resetModalOpen"
      title="重置 Owner 密码"
      :confirm-loading="resetting"
      @ok="submitResetOwner"
    >
      <a-form layout="vertical">
        <a-form-item label="新密码">
          <a-input-password v-model:value="resetForm.newPassword" :disabled="resetForm.generatePassword" />
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="resetForm.generatePassword">自动生成新密码</a-checkbox>
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="resetForm.sendInviteEmail">发送通知邮件</a-checkbox>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import {
  fetchPlatformTenantDetail,
  resetTenantOwnerPassword,
  updatePlatformTenant,
  type PlatformModelUsage,
  type PlatformTenantDetail,
} from '@/api/platform'
import { platformPath } from '@/config/deploy'
import TenantEditModal from '@/views/platform/components/TenantEditModal.vue'
import {
  formatPlatformCost,
  formatPlatformDateTime,
  formatPlatformNumber,
  formatStorageMb,
  quotaPercent,
} from '@/views/platform/shared/utils'
import '@/views/platform/shared/styles.css'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const resetModalOpen = ref(false)
const resetting = ref(false)
const detail = ref<PlatformTenantDetail | null>(null)

const resetForm = reactive({
  newPassword: '',
  generatePassword: true,
  sendInviteEmail: false,
})

const form = reactive({
  tenantName: '',
  planType: 'free',
  contactName: '',
  contactEmail: '',
  contactPhone: '',
  maxMembers: 50,
  maxAgents: 20,
  maxKnowledge: 10,
  maxStorageMb: 10240,
  monthlyTokenQuota: 1000000,
  status: 1,
  expireAt: null as Dayjs | null,
})

const modelColumns = [
  { title: '模型', dataIndex: 'displayName', key: 'displayName' },
  { title: 'Token', dataIndex: 'tokens', key: 'tokens' },
  { title: '调用', dataIndex: 'calls', key: 'calls' },
]

const tenantId = computed(() => {
  const raw = route.params.id
  const parsed = Number(Array.isArray(raw) ? raw[0] : raw)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
})

const agentPercent = computed(() =>
  quotaPercent(detail.value?.agentCount, detail.value?.tenant.maxAgents),
)
const knowledgePercent = computed(() =>
  quotaPercent(detail.value?.knowledgeCount, detail.value?.tenant.maxKnowledge),
)

const maxTrendTokens = computed(() =>
  Math.max(1, ...(detail.value?.dailyTokenTrend.map((item) => item.tokens) || [1])),
)

function trendBarWidth(tokens: number) {
  return `${Math.max(4, Math.round((tokens / maxTrendTokens.value) * 100))}%`
}

function goBack() {
  router.push(platformPath('/platform/tenants'))
}

async function loadDetail() {
  if (!tenantId.value) {
    message.error('无效的租户 ID')
    goBack()
    return
  }
  loading.value = true
  try {
    const res = await fetchPlatformTenantDetail(tenantId.value)
    detail.value = res.data.data
  } catch {
    message.error('加载租户详情失败')
  } finally {
    loading.value = false
  }
}

function openResetOwner() {
  resetForm.newPassword = ''
  resetForm.generatePassword = true
  resetForm.sendInviteEmail = false
  resetModalOpen.value = true
}

async function submitResetOwner() {
  if (!tenantId.value) return
  if (!resetForm.generatePassword && !resetForm.newPassword.trim()) {
    message.warning('请填写新密码或勾选自动生成')
    return
  }
  resetting.value = true
  try {
    const res = await resetTenantOwnerPassword(tenantId.value, {
      newPassword: resetForm.generatePassword ? undefined : resetForm.newPassword,
      generatePassword: resetForm.generatePassword,
      sendInviteEmail: resetForm.sendInviteEmail,
    })
    resetModalOpen.value = false
    const result = res.data.data
    if (result.generatedPassword) {
      Modal.success({
        title: 'Owner 密码已重置',
        content: `账号：${result.ownerEmail}\n新密码：${result.generatedPassword}${
          result.inviteEmailSent ? '\n（通知邮件已发送）' : ''
        }`,
      })
    } else {
      message.success(result.inviteEmailSent ? '密码已重置，通知邮件已发送' : 'Owner 密码已重置')
    }
  } catch {
    message.error('重置失败')
  } finally {
    resetting.value = false
  }
}

function openEdit() {
  const tenant = detail.value?.tenant
  if (!tenant) return
  form.tenantName = tenant.tenantName
  form.planType = tenant.planType
  form.contactName = tenant.contactName || ''
  form.contactEmail = tenant.contactEmail || ''
  form.contactPhone = tenant.contactPhone || ''
  form.maxMembers = tenant.maxMembers || 50
  form.maxAgents = tenant.maxAgents || 20
  form.maxKnowledge = tenant.maxKnowledge || 10
  form.maxStorageMb = tenant.maxStorageMb || 10240
  form.monthlyTokenQuota = tenant.monthlyTokenQuota || 1000000
  form.status = tenant.status ?? 1
  form.expireAt = tenant.expireAt ? dayjs(tenant.expireAt) : null
  modalOpen.value = true
}

async function saveTenant() {
  if (!tenantId.value || !form.tenantName.trim()) {
    message.warning('请填写企业名称')
    return
  }
  saving.value = true
  try {
    await updatePlatformTenant(tenantId.value, {
      tenantName: form.tenantName,
      planType: form.planType,
      contactName: form.contactName,
      contactEmail: form.contactEmail,
      contactPhone: form.contactPhone,
      maxMembers: form.maxMembers,
      maxAgents: form.maxAgents,
      maxKnowledge: form.maxKnowledge,
      maxStorageMb: form.maxStorageMb,
      monthlyTokenQuota: form.monthlyTokenQuota,
      status: form.status,
      expireAt: form.expireAt ? form.expireAt.format('YYYY-MM-DDTHH:mm:ss') : undefined,
    })
    message.success('租户已更新')
    modalOpen.value = false
    await loadDetail()
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

watch(tenantId, loadDetail, { immediate: true })
onMounted(loadDetail)
</script>

<style scoped>
.back-link {
  padding-left: 0;
  margin-bottom: 4px;
}

.header-tag {
  margin-left: 8px;
}

.quota-panel,
.contact-panel {
  padding: 16px;
  margin-bottom: 16px;
}

.quota-panel h3,
.contact-panel h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-item span {
  color: var(--text-secondary);
  font-size: 12px;
}

.list-panel {
  padding: 16px;
  margin-bottom: 16px;
  min-height: 220px;
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
