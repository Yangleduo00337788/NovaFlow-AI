<template>
  <a-modal
    :open="open"
    title="新建租户"
    :confirm-loading="saving"
    width="640px"
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
      <a-divider orientation="left">所有者账号</a-divider>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="所有者邮箱" required>
            <a-input v-model:value="form.ownerEmail" placeholder="owner@company.com" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="初始密码" required>
            <a-input-password v-model:value="form.ownerPassword" placeholder="至少包含字母和数字" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="所有者昵称">
            <a-input v-model:value="form.ownerNickname" placeholder="可选" />
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
import { PLAN_OPTIONS } from '@/views/platform/shared/utils'

export interface TenantCreateFormState {
  tenantName: string
  planType: string
  ownerEmail: string
  ownerPassword: string
  ownerNickname: string
  contactName: string
  contactEmail: string
  contactPhone: string
}

defineProps<{
  open: boolean
  saving: boolean
  form: TenantCreateFormState
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  save: []
}>()

const planOptions = PLAN_OPTIONS
</script>
