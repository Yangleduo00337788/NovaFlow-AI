<template>
  <a-modal
    :open="open"
    title="编辑租户"
    :confirm-loading="saving"
    width="640px"
    @update:open="(v: boolean) => emit('update:open', v)"
    @ok="emit('save')"
  >
    <a-form layout="vertical" class="tenant-form">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="企业名称" required>
            <a-input v-model:value="form.tenantName" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="套餐">
            <a-select v-model:value="form.planType" :options="planOptions" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="状态">
            <a-select v-model:value="form.status">
              <a-select-option :value="1">正常</a-select-option>
              <a-select-option :value="0">停用</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="套餐到期">
            <a-date-picker
              v-model:value="form.expireAt"
              show-time
              format="YYYY-MM-DD HH:mm"
              style="width: 100%"
              placeholder="选择到期时间"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="8"><a-form-item label="联系人"><a-input v-model:value="form.contactName" /></a-form-item></a-col>
        <a-col :span="8"><a-form-item label="邮箱"><a-input v-model:value="form.contactEmail" /></a-form-item></a-col>
        <a-col :span="8"><a-form-item label="电话"><a-input v-model:value="form.contactPhone" /></a-form-item></a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="6"><a-form-item label="成员上限"><a-input-number v-model:value="form.maxMembers" :min="1" style="width:100%" /></a-form-item></a-col>
        <a-col :span="6"><a-form-item label="Agent 上限"><a-input-number v-model:value="form.maxAgents" :min="1" style="width:100%" /></a-form-item></a-col>
        <a-col :span="6"><a-form-item label="知识库上限"><a-input-number v-model:value="form.maxKnowledge" :min="1" style="width:100%" /></a-form-item></a-col>
        <a-col :span="6"><a-form-item label="存储 (MB)"><a-input-number v-model:value="form.maxStorageMb" :min="1" style="width:100%" /></a-form-item></a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="月 Token 配额">
            <a-input-number v-model:value="form.monthlyTokenQuota" :min="0" style="width:100%" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import type { Dayjs } from 'dayjs'
import { PLAN_OPTIONS } from '@/views/platform/shared/utils'

export interface TenantFormState {
  tenantName: string
  planType: string
  contactName: string
  contactEmail: string
  contactPhone: string
  maxMembers: number
  maxAgents: number
  maxKnowledge: number
  maxStorageMb: number
  monthlyTokenQuota: number
  status: number
  expireAt: Dayjs | null
}

defineProps<{
  open: boolean
  saving: boolean
  form: TenantFormState
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  save: []
}>()

const planOptions = PLAN_OPTIONS
</script>
