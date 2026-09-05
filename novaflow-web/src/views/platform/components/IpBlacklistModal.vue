<template>
  <a-modal
    :open="open"
    :title="editingId ? '编辑 IP 黑名单' : '添加 IP 黑名单'"
    :confirm-loading="saving"
    width="520px"
    @update:open="(v: boolean) => emit('update:open', v)"
    @ok="emit('save')"
  >
    <a-form layout="vertical">
      <a-form-item label="IP 地址" required>
        <a-input v-model:value="form.ipAddress" :disabled="!!editingId" placeholder="如 203.0.113.10" />
      </a-form-item>
      <a-form-item label="封禁原因">
        <a-input v-model:value="form.reason" placeholder="可选" />
      </a-form-item>
      <a-form-item label="过期时间">
        <a-date-picker
          v-model:value="form.expireAt"
          show-time
          format="YYYY-MM-DD HH:mm"
          style="width: 100%"
          placeholder="留空表示永久封禁"
        />
      </a-form-item>
      <a-form-item v-if="editingId" label="状态">
        <a-select v-model:value="form.status">
          <a-select-option :value="1">生效</a-select-option>
          <a-select-option :value="0">停用</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import type { Dayjs } from 'dayjs'

export interface IpBlacklistFormState {
  ipAddress: string
  reason: string
  expireAt: Dayjs | null
  status: number
}

defineProps<{
  open: boolean
  saving: boolean
  editingId: number | null
  form: IpBlacklistFormState
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  save: []
}>()
</script>
