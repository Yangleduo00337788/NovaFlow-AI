<template>
  <div class="permission-page page-shell" data-testid="permission-page">
    <div class="page-header">
      <div>
        <h1>权限管理</h1>
        <p>查看系统角色权限矩阵与成员分布</p>
      </div>
    </div>

    <div class="permission-panel page-card">
      <aside class="role-nav">
        <div class="nav-title">系统角色</div>
        <a-spin :spinning="rolesLoading">
          <button
            v-for="role in roles"
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
      </aside>

      <main class="role-detail">
        <a-empty v-if="!selectedRole" description="请选择角色" />
        <template v-else>
          <div class="detail-header">
            <div>
              <h3>{{ selectedRole.roleName }}</h3>
              <p>{{ selectedRole.description }}</p>
            </div>
            <a-tag color="blue">系统内置角色（只读）</a-tag>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { CheckOutlined } from '@ant-design/icons-vue'
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
const activeTab = ref('permissions')

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
  activeTab.value = 'permissions'
  loadRoleMembers(role.id)
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

.permission-panel {
  display: flex;
  align-items: stretch;
  padding: 0;
  overflow: hidden;
}

.role-nav {
  width: 240px;
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
  color: #1677ff;
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
