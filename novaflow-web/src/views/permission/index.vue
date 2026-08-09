<template>
  <div class="permission-page" data-testid="permission-page">
    <div class="page-header">
      <div>
        <h1>权限管理</h1>
        <p>查看系统角色权限矩阵与成员分布</p>
      </div>
    </div>

    <div class="permission-layout">
      <div class="page-card role-panel">
        <div class="panel-title">系统角色</div>
        <a-spin :spinning="rolesLoading">
          <div
            v-for="role in roles"
            :key="role.id"
            class="role-item"
            :class="{ active: selectedRoleId === role.id }"
            @click="selectRole(role)"
          >
            <div class="role-head">
              <strong>{{ role.roleName }}</strong>
              <a-tag>{{ role.memberCount || 0 }} 人</a-tag>
            </div>
            <p class="role-desc">{{ role.description }}</p>
          </div>
        </a-spin>
      </div>

      <div class="detail-panel">
        <div class="page-card">
          <div class="panel-title">权限矩阵</div>
          <a-empty v-if="!selectedRole" description="请选择角色" />
          <template v-else>
            <div class="selected-role-bar">
              <div>
                <h3>{{ selectedRole.roleName }}</h3>
                <p>{{ selectedRole.description }}</p>
              </div>
              <a-tag color="blue">系统内置角色（只读）</a-tag>
            </div>
            <a-spin :spinning="permissionsLoading">
              <div v-for="(items, module) in groupedPermissions" :key="module" class="perm-module">
                <div class="perm-module-title">{{ MODULE_LABELS[module] || module }}</div>
                <div class="perm-grid">
                  <div
                    v-for="perm in items"
                    :key="perm.id"
                    class="perm-item"
                    :class="{ granted: hasPermission(perm.permissionCode) }"
                  >
                    <span class="perm-name">{{ perm.permissionName }}</span>
                    <span class="perm-code">{{ perm.permissionCode }}</span>
                  </div>
                </div>
              </div>
            </a-spin>
          </template>
        </div>

        <div class="page-card">
          <div class="panel-title">角色成员</div>
          <a-spin :spinning="membersLoading">
            <a-empty v-if="!selectedRole" description="请选择角色" />
            <a-table
              v-else
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
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { MemberItem } from '@/api/org'
import {
  MODULE_LABELS,
  fetchGroupedPermissions,
  fetchRoleMembers,
  fetchRoles,
  type PermissionItem,
  type RoleItem,
} from '@/api/permission'
import { formatDateTime } from '@/utils/datetime'

const rolesLoading = ref(false)
const permissionsLoading = ref(false)
const membersLoading = ref(false)
const roles = ref<RoleItem[]>([])
const selectedRoleId = ref<number | null>(null)
const groupedPermissions = ref<Record<string, PermissionItem[]>>({})
const roleMembers = ref<MemberItem[]>([])

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
      selectRole(roles.value[0])
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
  loadRoleMembers(role.id)
}

onMounted(() => {
  loadRoles()
  loadPermissions()
})
</script>

<style scoped>
.permission-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.permission-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 12px;
  align-items: start;
}

.panel-title {
  font-weight: 600;
  margin-bottom: 14px;
}

.role-panel {
  position: sticky;
  top: 0;
}

.role-item {
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.role-item:hover,
.role-item.active {
  border-color: #1677ff;
  background: rgba(22, 119, 255, 0.04);
}

.role-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.role-desc {
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.selected-role-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.selected-role-bar h3 {
  margin: 0 0 4px;
}

.selected-role-bar p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.perm-module {
  margin-bottom: 18px;
}

.perm-module-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}

.perm-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.perm-item {
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--card-bg);
  opacity: 0.55;
}

.perm-item.granted {
  opacity: 1;
  border-color: rgba(22, 119, 255, 0.35);
  background: rgba(22, 119, 255, 0.05);
}

.perm-name {
  display: block;
  font-weight: 500;
}

.perm-code {
  display: block;
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
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
</style>
