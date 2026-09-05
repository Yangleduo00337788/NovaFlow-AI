<template>
  <div class="permission-page page-shell" data-testid="permission-page">
    <div class="page-header">
      <div>
        <h1>权限管理</h1>
        <p>查看系统角色权限矩阵、管理自定义角色与成员分布</p>
      </div>
      <a-button
        v-if="canCreateRole"
        type="primary"
        data-testid="create-custom-role"
        @click="openCreateModal"
      >
        新建自定义角色
      </a-button>
    </div>

    <div class="permission-panel page-card">
      <aside class="role-nav">
        <div class="nav-title">系统角色</div>
        <a-spin :spinning="rolesLoading">
          <button
            v-for="role in systemRoles"
            :key="role.id"
            type="button"
            class="role-item"
            :class="{ active: selectedRoleId === role.id }"
            @click="selectRole(role)"
          >
            <div class="role-head">
              <strong>{{ role.roleName }}</strong>
              <a-tag>{{ role.memberCount || 0 }} 人</a-tag>
            </div>
            <p class="role-desc">{{ role.description }}</p>
          </button>
        </a-spin>

        <div class="nav-title custom-title">自定义角色</div>
        <a-spin :spinning="rolesLoading">
          <button
            v-for="role in customRoles"
            :key="role.id"
            type="button"
            class="role-item custom"
            :class="{ active: selectedRoleId === role.id }"
            @click="selectRole(role)"
          >
            <div class="role-head">
              <strong>{{ role.roleName }}</strong>
              <a-tag color="purple">{{ role.memberCount || 0 }} 人</a-tag>
            </div>
            <p class="role-desc">{{ role.description || '企业自定义权限组合' }}</p>
          </button>
          <a-empty v-if="!customRoles.length" :image-style="{ height: '48px' }" description="暂无自定义角色" />
        </a-spin>
      </aside>

      <main class="role-detail">
        <a-empty v-if="!selectedRole" description="请选择角色" />
        <template v-else>
          <div class="detail-header">
            <div>
              <h3>{{ selectedRole.roleName }}</h3>
              <p>{{ selectedRole.description }}</p>
            </div>
            <div class="detail-tags">
              <a-tag v-if="selectedRole.isSystem" color="blue">系统内置角色（只读）</a-tag>
              <a-tag v-else color="purple">自定义角色</a-tag>
              <a-tag v-if="isProtectedMemberRole(selectedRole.roleCode)" color="purple">不可在组织内分配</a-tag>
              <template v-if="!selectedRole.isSystem">
                <a-button v-if="canUpdateRole" size="small" @click="openEditModal">编辑</a-button>
                <a-button
                  v-if="canDeleteRole"
                  size="small"
                  danger
                  :disabled="(selectedRole.memberCount || 0) > 0"
                  @click="handleDeleteRole"
                >
                  删除
                </a-button>
              </template>
            </div>
          </div>

          <a-tabs v-model:activeKey="activeTab" class="detail-tabs">
            <a-tab-pane key="permissions" tab="权限矩阵">
              <a-spin :spinning="permissionsLoading">
                <div v-for="(items, module) in groupedPermissions" :key="module" class="perm-module">
                  <div class="perm-module-title">{{ MODULE_LABELS[module] || module }}</div>
                  <div class="perm-tags">
                    <span
                      v-for="perm in items"
                      :key="perm.id"
                      class="perm-tag"
                      :class="{ granted: hasPermission(perm.permissionCode) }"
                      :title="perm.permissionCode"
                    >
                      <CheckOutlined v-if="hasPermission(perm.permissionCode)" class="perm-check" />
                      {{ perm.permissionName }}
                    </span>
                  </div>
                </div>
              </a-spin>
            </a-tab-pane>

            <a-tab-pane key="members" :tab="`角色成员 (${roleMembers.length})`">
              <a-spin :spinning="membersLoading">
                <a-table
                  :columns="memberColumns"
                  :data-source="roleMembers"
                  row-key="id"
                  :pagination="false"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'member'">
                      <div class="member-cell">
                        <strong>{{ record.nickname || record.username }}</strong>
                        <span class="member-email">{{ record.email }}</span>
                      </div>
                    </template>
                    <template v-else-if="column.key === 'status'">
                      <a-tag :color="record.status === 1 ? 'success' : 'default'">
                        {{ record.status === 1 ? '正常' : '已禁用' }}
                      </a-tag>
                    </template>
                    <template v-else-if="column.key === 'joinedAt'">
                      {{ formatDateTime(record.joinedAt) }}
                    </template>
                  </template>
                </a-table>
              </a-spin>
            </a-tab-pane>
          </a-tabs>
        </template>
      </main>
    </div>

    <a-modal
      v-model:open="roleModalOpen"
      :title="editingRoleId ? '编辑自定义角色' : '新建自定义角色'"
      :confirm-loading="roleSaving"
      width="720px"
      @ok="saveCustomRole"
    >
      <a-form layout="vertical">
        <a-form-item label="角色名称" required>
          <a-input v-model:value="roleForm.roleName" maxlength="64" placeholder="例如：客服专员" />
        </a-form-item>
        <a-form-item label="描述">
          <a-input v-model:value="roleForm.description" maxlength="256" placeholder="可选" />
        </a-form-item>
        <a-form-item label="权限" required>
          <a-spin :spinning="permissionsLoading">
            <div v-for="(items, module) in groupedPermissions" :key="module" class="perm-module modal-perm">
              <div class="perm-module-title">{{ MODULE_LABELS[module] || module }}</div>
              <a-checkbox-group v-model:value="roleForm.permissionCodes" class="perm-checkbox-group">
                <a-checkbox
                  v-for="perm in items"
                  :key="perm.permissionCode"
                  :value="perm.permissionCode"
                >
                  {{ perm.permissionName }}
                </a-checkbox>
              </a-checkbox-group>
            </div>
          </a-spin>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { CheckOutlined } from '@ant-design/icons-vue'
import type { MemberItem } from '@/api/org'
import {
  MODULE_LABELS,
  createRole,
  deleteRole,
  fetchGroupedPermissions,
  fetchRoleMembers,
  fetchRoles,
  updateRole,
  type PermissionItem,
  type RoleItem,
} from '@/api/permission'
import { formatDateTime } from '@/utils/datetime'
import { useAuthStore } from '@/stores/auth'
import { isProtectedMemberRole } from '@/config/roles'

const auth = useAuthStore()
const canCreateRole = computed(() => auth.hasAnyPermission(['role:create', 'tenant:manage']))
const canUpdateRole = computed(() => auth.hasAnyPermission(['role:update', 'tenant:manage']))
const canDeleteRole = computed(() => auth.hasAnyPermission(['role:delete', 'tenant:manage']))

const rolesLoading = ref(false)
const permissionsLoading = ref(false)
const membersLoading = ref(false)
const roleSaving = ref(false)
const roles = ref<RoleItem[]>([])
const selectedRoleId = ref<number | null>(null)
const groupedPermissions = ref<Record<string, PermissionItem[]>>({})
const roleMembers = ref<MemberItem[]>([])
const activeTab = ref('permissions')
const roleModalOpen = ref(false)
const editingRoleId = ref<number | null>(null)
const roleForm = reactive({
  roleName: '',
  description: '',
  permissionCodes: [] as string[],
})

const systemRoles = computed(() => roles.value.filter((item) => item.isSystem))
const customRoles = computed(() => roles.value.filter((item) => !item.isSystem))
const selectedRole = computed(() => roles.value.find((item) => item.id === selectedRoleId.value) || null)

const memberColumns = [
  { title: '成员', key: 'member' },
  { title: '状态', key: 'status', width: 100 },
  { title: '加入时间', key: 'joinedAt', width: 180 },
]

function hasPermission(code: string) {
  return selectedRole.value?.permissionCodes?.includes(code)
}

async function loadRoles() {
  rolesLoading.value = true
  try {
    const res = await fetchRoles()
    roles.value = res.data.data || []
    if (roles.value.length && !selectedRoleId.value) {
      const mine = roles.value.find((item) => item.roleCode === auth.roleCode)
      selectRole(mine || roles.value[0])
    }
  } catch {
    message.error('加载角色失败')
  } finally {
    rolesLoading.value = false
  }
}

async function loadPermissions() {
  permissionsLoading.value = true
  try {
    const res = await fetchGroupedPermissions()
    groupedPermissions.value = res.data.data || {}
  } catch {
    message.error('加载权限失败')
  } finally {
    permissionsLoading.value = false
  }
}

async function loadRoleMembers(roleId: number) {
  membersLoading.value = true
  try {
    const res = await fetchRoleMembers(roleId)
    roleMembers.value = res.data.data || []
  } catch {
    message.error('加载角色成员失败')
  } finally {
    membersLoading.value = false
  }
}

function selectRole(role: RoleItem) {
  selectedRoleId.value = role.id
  activeTab.value = 'permissions'
  loadRoleMembers(role.id)
}

function openCreateModal() {
  editingRoleId.value = null
  roleForm.roleName = ''
  roleForm.description = ''
  roleForm.permissionCodes = []
  roleModalOpen.value = true
}

function openEditModal() {
  if (!selectedRole.value || selectedRole.value.isSystem) return
  editingRoleId.value = selectedRole.value.id
  roleForm.roleName = selectedRole.value.roleName
  roleForm.description = selectedRole.value.description || ''
  roleForm.permissionCodes = [...(selectedRole.value.permissionCodes || [])]
  roleModalOpen.value = true
}

async function saveCustomRole() {
  if (!roleForm.roleName.trim()) {
    message.warning('请填写角色名称')
    return
  }
  if (!roleForm.permissionCodes.length) {
    message.warning('请至少选择一个权限')
    return
  }
  roleSaving.value = true
  try {
    const payload = {
      roleName: roleForm.roleName.trim(),
      description: roleForm.description.trim() || undefined,
      permissionCodes: roleForm.permissionCodes,
    }
    if (editingRoleId.value) {
      await updateRole(editingRoleId.value, payload)
      message.success('角色已更新')
    } else {
      const res = await createRole(payload)
      message.success('角色已创建')
      if (res.data.data?.id) {
        selectedRoleId.value = res.data.data.id
      }
    }
    roleModalOpen.value = false
    await loadRoles()
    if (selectedRoleId.value) {
      await loadRoleMembers(selectedRoleId.value)
    }
  } catch {
    message.error('保存角色失败')
  } finally {
    roleSaving.value = false
  }
}

function handleDeleteRole() {
  if (!selectedRole.value || selectedRole.value.isSystem) return
  Modal.confirm({
    title: '删除自定义角色',
    content: `确定删除「${selectedRole.value.roleName}」？`,
    okType: 'danger',
    async onOk() {
      await deleteRole(selectedRole.value!.id)
      message.success('角色已删除')
      selectedRoleId.value = null
      roleMembers.value = []
      await loadRoles()
      if (roles.value.length) {
        selectRole(roles.value[0])
      }
    },
  })
}

onMounted(() => {
  loadRoles()
  loadPermissions()
})
</script>

<style scoped>
.permission-page {
  min-height: auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.permission-panel {
  display: flex;
  align-items: stretch;
  padding: 0;
  overflow: hidden;
}

.role-nav {
  width: 260px;
  flex-shrink: 0;
  padding: 16px 12px;
  border-right: 1px solid var(--border);
  background: var(--bg-subtle);
}

.nav-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  padding: 0 8px 10px;
}

.custom-title {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.role-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  margin-bottom: 6px;
  cursor: pointer;
  background: transparent;
  transition: border-color 0.15s, background 0.15s;
}

.role-item:hover {
  background: var(--hover-bg);
}

.role-item.active {
  border-color: rgba(22, 119, 255, 0.35);
  background: var(--card-bg);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
}

.role-item.custom.active {
  border-color: rgba(114, 46, 209, 0.35);
}

.role-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.role-desc {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.role-detail {
  flex: 1;
  min-width: 0;
  padding: 16px 20px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.detail-header h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.detail-header p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.detail-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
}

.perm-module {
  margin-bottom: 16px;
}

.modal-perm {
  margin-bottom: 12px;
}

.perm-module-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.perm-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.perm-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--bg-subtle);
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.4;
}

.perm-tag.granted {
  border-color: rgba(22, 119, 255, 0.3);
  background: rgba(22, 119, 255, 0.06);
  color: var(--text-primary);
}

.perm-check {
  font-size: 11px;
  color: var(--primary);
}

.perm-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.member-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.member-email {
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 900px) {
  .permission-panel {
    flex-direction: column;
  }

  .role-nav {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid var(--border);
  }
}
</style>
