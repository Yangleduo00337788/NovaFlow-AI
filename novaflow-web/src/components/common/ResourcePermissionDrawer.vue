<template>
  <a-drawer
    :open="open"
    title="资源授权"
    :width="560"
    @close="emit('close')"
  >
    <p class="hint">
      配置后仅列出的用户可访问该资源（企业 Owner/Admin 始终可访问）。未配置时沿用角色权限。
    </p>
    <a-spin :spinning="loading">
      <div v-for="(row, index) in rows" :key="index" class="grant-row">
        <a-select
          v-model:value="row.userId"
          show-search
          placeholder="选择成员"
          style="width: 200px"
          :options="memberOptions"
          :filter-option="filterMember"
        />
        <a-select
          v-model:value="row.permissionCode"
          placeholder="权限"
          style="width: 180px"
          :options="permissionOptions"
        />
        <a-button type="text" danger @click="removeRow(index)">移除</a-button>
      </div>
      <a-button type="dashed" block @click="addRow">添加授权</a-button>
    </a-spin>
    <template #footer>
      <a-space>
        <a-button @click="emit('close')">取消</a-button>
        <a-button type="primary" :loading="saving" @click="onSave">保存</a-button>
      </a-space>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { fetchMembers, type MemberItem } from '@/api/org'
import {
  fetchResourcePermissions,
  saveResourcePermissions,
  type ResourcePermissionGrant,
} from '@/api/resourcePermission'

const props = defineProps<{
  open: boolean
  resourceType: string
  resourceId: number | null
  permissionOptions: Array<{ value: string; label: string }>
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

const loading = ref(false)
const saving = ref(false)
const members = ref<MemberItem[]>([])
const rows = ref<Array<{ userId?: number; permissionCode?: string }>>([])

const memberOptions = computed(() =>
  members.value.map((m) => ({
    value: m.userId,
    label: `${m.nickname || m.username || m.email} (${m.roleName || m.roleCode})`,
  })),
)

function filterMember(input: string, option: { label?: string }) {
  return (option.label || '').toLowerCase().includes(input.toLowerCase())
}

function addRow() {
  rows.value.push({})
}

function removeRow(index: number) {
  rows.value.splice(index, 1)
}

async function loadMembers() {
  const res = await fetchMembers({ page: 1, pageSize: 200 })
  members.value = res.data.data?.list || []
}

async function loadGrants() {
  if (!props.resourceId) return
  loading.value = true
  try {
    const res = await fetchResourcePermissions(props.resourceType, props.resourceId)
    const items = res.data.data || []
    rows.value = items.map((item) => ({
      userId: item.userId,
      permissionCode: item.permissionCode,
    }))
    if (rows.value.length === 0) {
      addRow()
    }
  } catch {
    message.error('加载资源授权失败')
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!props.resourceId) return
  const grants: ResourcePermissionGrant[] = rows.value
    .filter((row) => row.userId && row.permissionCode)
    .map((row) => ({
      userId: row.userId!,
      permissionCode: row.permissionCode!,
    }))
  saving.value = true
  try {
    await saveResourcePermissions(props.resourceType, props.resourceId, grants)
    message.success('资源授权已保存')
    emit('saved')
    emit('close')
  } catch {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

watch(
  () => [props.open, props.resourceId] as const,
  async ([open, id]) => {
    if (!open || !id) return
    rows.value = []
    await Promise.all([loadMembers(), loadGrants()])
  },
)
</script>

<style scoped>
.hint {
  color: var(--text-secondary, #666);
  margin-bottom: 16px;
  font-size: 13px;
}
.grant-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}
</style>
