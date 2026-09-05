<template>
  <a-modal
    :open="open"
    title="新建租户"
    :confirm-loading="saving"
    width="720px"
    @update:open="(v: boolean) => emit('update:open', v)"
    @ok="emit('save')"
  >
    <a-form layout="vertical" class="tenant-form">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="企业名称" required>
            <a-input v-model:value="form.tenantName" placeholder="请输入企业名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="套餐">
            <a-select v-model:value="form.planType" :options="planOptions" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-alert
        v-if="selectedTemplate"
        type="info"
        show-icon
        class="template-preview"
        :message="`套餐模板：${selectedTemplate.planTypeLabel}`"
      >
        <template #description>
          成员 {{ selectedTemplate.maxMembers }} · Agent {{ selectedTemplate.maxAgents }} ·
          知识库 {{ selectedTemplate.maxKnowledge }} · 存储 {{ selectedTemplate.maxStorageMb }} MB ·
          月 Token {{ formatPlatformNumber(selectedTemplate.monthlyTokenQuota) }}
        </template>
      </a-alert>

      <a-divider orientation="left">所有者账号</a-divider>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="所有者邮箱" required>
            <a-input v-model:value="form.ownerEmail" placeholder="owner@company.com" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="所有者昵称">
            <a-input v-model:value="form.ownerNickname" placeholder="可选" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="初始密码">
            <a-input-password
              v-model:value="form.ownerPassword"
              :disabled="form.generatePassword"
              placeholder="至少包含字母和数字"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="开户选项">
            <a-space direction="vertical">
              <a-checkbox v-model:checked="form.generatePassword">自动生成安全密码</a-checkbox>
              <a-checkbox v-model:checked="form.sendInviteEmail">发送邀请邮件（需 SMTP）</a-checkbox>
            </a-space>
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider orientation="left">联系信息（可选）</a-divider>
      <a-row :gutter="16">
        <a-col :span="8"><a-form-item label="联系人"><a-input v-model:value="form.contactName" /></a-form-item></a-col>
        <a-col :span="8"><a-form-item label="邮箱"><a-input v-model:value="form.contactEmail" /></a-form-item></a-col>
        <a-col :span="8"><a-form-item label="电话"><a-input v-model:value="form.contactPhone" /></a-form-item></a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { fetchOnboardingTemplates, type PlatformOnboardingTemplate } from '@/api/platform'
import { PLAN_OPTIONS, formatPlatformNumber } from '@/views/platform/shared/utils'

export interface TenantCreateFormState {
  tenantName: string
  planType: string
  ownerEmail: string
  ownerPassword: string
  generatePassword: boolean
  sendInviteEmail: boolean
  ownerNickname: string
  contactName: string
  contactEmail: string
  contactPhone: string
}

const props = defineProps<{
  open: boolean
  saving: boolean
  form: TenantCreateFormState
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  save: []
}>()

const planOptions = PLAN_OPTIONS
const templates = ref<PlatformOnboardingTemplate[]>([])

const selectedTemplate = computed(() =>
  templates.value.find((item) => item.planType === props.form.planType),
)

watch(
  () => props.form.generatePassword,
  (enabled) => {
    if (enabled) {
      props.form.ownerPassword = ''
    }
  },
)

onMounted(async () => {
  try {
    const res = await fetchOnboardingTemplates()
    templates.value = res.data.data
  } catch {
    templates.value = []
  }
})
</script>

<style scoped>
.template-preview {
  margin-bottom: 16px;
}
</style>
