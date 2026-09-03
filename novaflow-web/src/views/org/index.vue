<template>
  <div class="org-page page-shell" data-testid="org-page">
    <div class="page-header">
      <div>
        <h1>组织管理</h1>
        <p>管理企业信息、工作空间、部门与成员</p>
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab" type="card">
      <a-tab-pane v-if="canManageTenant" key="tenant" tab="企业信息">
        <div class="page-card tenant-card">
          <a-spin :spinning="tenantLoading">
            <a-form layout="vertical" :model="tenantForm" class="tenant-form">
              <div class="section-block">
                <h3 class="section-title">基本信息</h3>
                <a-row :gutter="[16, 0]">
                  <a-col :xs="24" :sm="12" :lg="8">
                    <a-form-item label="企业名称" required>
                      <a-input v-model:value="tenantForm.tenantName" placeholder="企业名称" />
                    </a-form-item>
                  </a-col>
                  <a-col :xs="24" :sm="12" :lg="8">
                    <a-form-item label="企业编码">
                      <a-input :value="tenantInfo?.tenantCode" disabled />
                    </a-form-item>
                  </a-col>
                  <a-col :xs="24" :sm="12" :lg="8">
                    <a-form-item label="联系人">
                      <a-input v-model:value="tenantForm.contactName" placeholder="联系人姓名" />
                    </a-form-item>
                  </a-col>
                  <a-col :xs="24" :sm="12" :lg="8">
                    <a-form-item label="联系邮箱">
                      <a-input v-model:value="tenantForm.contactEmail" placeholder="contact@company.com" />
                    </a-form-item>
                  </a-col>
                  <a-col :xs="24" :sm="12" :lg="8">
                    <a-form-item label="联系电话">
                      <a-input v-model:value="tenantForm.contactPhone" placeholder="联系电话" />
                    </a-form-item>
                  </a-col>
                </a-row>
              </div>

              <div class="section-block">
                <h3 class="section-title">套餐与配额</h3>
                <div class="info-grid">
                  <div class="info-grid-item">
                    <span class="info-grid-label">套餐类型</span>
                    <strong class="info-grid-value">{{ tenantInfo?.planTypeLabel || '-' }}</strong>
                  </div>
                  <div class="info-grid-item">
                    <span class="info-grid-label">到期时间</span>
                    <strong class="info-grid-value">{{ formatDate(tenantInfo?.expireAt) }}</strong>
                  </div>
                  <div class="info-grid-item">
                    <span class="info-grid-label">成员席位</span>
                    <strong class="info-grid-value">{{ tenantInfo?.memberCount || 0 }} / {{ tenantInfo?.maxMembers || 0 }}</strong>
                  </div>
                  <div class="info-grid-item">
                    <span class="info-grid-label">Agent 配额</span>
                    <strong class="info-grid-value">{{ tenantInfo?.maxAgents || 0 }}</strong>
                  </div>
                  <div class="info-grid-item">
                    <span class="info-grid-label">知识库配额</span>
                    <strong class="info-grid-value">{{ tenantInfo?.maxKnowledge || 0 }}</strong>
                  </div>
                  <div class="info-grid-item">
                    <span class="info-grid-label">月 Token 配额</span>
                    <strong class="info-grid-value">{{ formatNumber(tenantInfo?.monthlyTokenQuota) }}</strong>
                  </div>
                </div>
              </div>

              <div class="form-actions">
                <a-button type="primary" :loading="savingTenant" @click="saveTenant">保存企业信息</a-button>
              </div>
            </a-form>
          </a-spin>
        </div>
      </a-tab-pane>

      <a-tab-pane v-if="canManageTenant" key="workspace" tab="工作空间">
        <div class="page-card">
          <div class="section-toolbar">
            <span class="toolbar-meta">共 {{ workspaces.length }} 个工作空间</span>
            <a-button v-if="canManageTenant" type="primary" @click="openWorkspaceCreate">
              <PlusOutlined />
              创建工作空间
            </a-button>
          </div>
          <a-table
            :columns="workspaceColumns"
            :data-source="workspaces"
            :loading="workspaceLoading"
            row-key="id"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <span>{{ record.workspaceName }}</span>
                <a-tag v-if="record.isDefault" color="blue" class="default-tag">默认</a-tag>
              </template>
              <template v-else-if="column.key === 'updatedAt'">
                {{ formatDateTime(record.updatedAt) }}
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-space v-if="canManageTenant">
                  <a-button type="link" size="small" @click="openWorkspaceEdit(record)">编辑</a-button>
                  <a-popconfirm
                    v-if="!record.isDefault"
                    title="确认删除该工作空间？"
                    @confirm="onDeleteWorkspace(record.id)"
                  >
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>

      <a-tab-pane v-if="canReadUsers" key="department" tab="部门">
        <div class="page-card">
          <div class="section-toolbar">
            <span class="toolbar-meta">树形组织，成员可归属到部门</span>
            <a-button v-if="canManageDept" type="primary" @click="openDeptCreate(null)">新建部门</a-button>
          </div>
          <a-spin :spinning="deptLoading">
            <p v-if="!deptLoading && !departments.length" class="empty-dept">暂无部门，点击右上角新建</p>
            <a-tree
              v-else
              block-node
              default-expand-all
              :tree-data="departments"
              :field-names="{ title: 'deptName', key: 'id', children: 'children' }"
            >
              <template #title="node">
                <div class="dept-node">
                  <span>
                    {{ node.deptName || node.title }}
                    <em>{{ node.memberCount || 0 }} 人</em>
                  </span>
                  <a-space v-if="canManageDept">
                    <a-button type="link" size="small" @click.stop="openDeptCreate(node.id || node.key)">子部门</a-button>
                    <a-button type="link" size="small" @click.stop="openDeptEdit(node)">编辑</a-button>
                    <a-popconfirm title="删除后成员将变为未分配，确认删除？" @confirm="onDeleteDept(node.id || node.key)">
                      <a-button type="link" size="small" danger @click.stop>删除</a-button>
                    </a-popconfirm>
                  </a-space>
                </div>
              </template>
            </a-tree>
          </a-spin>
        </div>
      </a-tab-pane>

      <a-tab-pane v-if="canReadUsers" key="member" tab="成员管理">
        <div class="page-card">
          <div class="section-toolbar">
            <a-input-search
              v-model:value="memberKeyword"
              placeholder="搜索成员邮箱/昵称"
              style="width: 260px"
              allow-clear
              @search="loadMembers"
            />
            <a-tree-select
              v-model:value="memberDeptFilter"
              allow-clear
              placeholder="按部门筛选"
              style="width: 220px"
              :tree-data="departments"
              :field-names="{ label: 'deptName', value: 'id', children: 'children' }"
              tree-default-expand-all
              @change="onMemberDeptFilterChange"
            />
            <a-button v-if="canInviteUser" type="primary" @click="openInvite">
              <UserAddOutlined />
              邀请成员
            </a-button>
          </div>
          <a-table
            :columns="memberColumns"
            :data-source="members"
            :loading="memberLoading"
            row-key="id"
            :pagination="memberPagination"
            @change="onMemberTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'member'">
                <div class="member-cell">
                  <strong>{{ record.nickname || record.username }}</strong>
                  <span class="member-email">{{ record.email }}</span>
                </div>
              </template>
              <template v-else-if="column.key === 'role'">
                <a-tag>{{ record.roleName }}</a-tag>
              </template>
              <template v-else-if="column.key === 'department'">
                {{ record.departmentName || '未分配' }}
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'success' : 'default'">
                  {{ record.status === 1 ? '正常' : '已禁用' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'joinedAt'">
                {{ formatDateTime(record.joinedAt) }}
              </template>
              <template v-else-if="column.key === 'lastLoginAt'">
                {{ formatDateTime(record.lastLoginAt) }}
              </template>
              <template v-else-if="column.key === 'actions'">
                <span v-if="isProtectedMember(record)" class="member-locked">受保护角色</span>
                <a-space v-else-if="canUpdateUser || canDeleteUser">
                  <a-button v-if="canUpdateUser" type="link" size="small" @click="openMemberEdit(record)">编辑</a-button>
                  <a-popconfirm v-if="canDeleteUser" title="确认移除该成员？" @confirm="onRemoveMember(record.id)">
                    <a-button type="link" size="small" danger>移除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="workspaceModalOpen"
      :title="editingWorkspaceId ? '编辑工作空间' : '创建工作空间'"
      @ok="saveWorkspace"
      @cancel="resetWorkspaceForm"
    >
      <a-form layout="vertical" :model="workspaceForm">
        <a-form-item label="名称" required>
          <a-input v-model:value="workspaceForm.workspaceName" placeholder="研发部" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="workspaceForm.description" :rows="3" placeholder="工作空间说明" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="deptModalOpen"
      :title="editingDeptId ? '编辑部门' : (deptForm.parentId ? '新建子部门' : '新建部门')"
      :confirm-loading="savingDept"
      @ok="saveDepartment"
      @cancel="resetDeptForm"
    >
      <a-form layout="vertical" :model="deptForm">
        <a-form-item label="部门名称" required>
          <a-input v-model:value="deptForm.deptName" placeholder="例如：研发部" />
        </a-form-item>
        <a-form-item v-if="!editingDeptId" label="上级部门">
          <a-tree-select
            v-model:value="deptForm.parentId"
            allow-clear
            placeholder="无（作为一级部门）"
            :tree-data="departments"
            :field-names="{ label: 'deptName', value: 'id', children: 'children' }"
            tree-default-expand-all
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="inviteModalOpen"
      title="邀请成员"
      :confirm-loading="inviting"
      @ok="submitInvite"
      @cancel="resetInviteForm"
    >
      <a-form layout="vertical" :model="inviteForm">
        <a-form-item label="邮箱" required>
          <a-input v-model:value="inviteForm.email" placeholder="member@company.com" />
        </a-form-item>
        <a-form-item label="昵称">
          <a-input v-model:value="inviteForm.nickname" placeholder="显示名称" />
        </a-form-item>
        <a-form-item label="角色" required>
          <a-select v-model:value="inviteForm.roleCode" :options="ROLE_OPTIONS" />
        </a-form-item>
        <a-form-item label="部门">
          <a-tree-select
            v-model:value="inviteForm.departmentId"
            allow-clear
            placeholder="未分配"
            :tree-data="departments"
            :field-names="{ label: 'deptName', value: 'id', children: 'children' }"
            tree-default-expand-all
          />
        </a-form-item>
        <a-form-item label="初始密码" required>
          <a-input-password v-model:value="inviteForm.password" placeholder="新用户登录密码（至少 8 位）" />
          <div class="field-tip">若邮箱已注册，将直接加入企业，无需密码</div>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="memberModalOpen"
      title="编辑成员"
      :confirm-loading="updatingMember"
      @ok="submitMemberUpdate"
    >
      <a-form layout="vertical" :model="memberForm">
        <a-form-item label="成员">
          <a-input :value="editingMember?.email" disabled />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="memberForm.roleCode" :options="ROLE_OPTIONS" />
        </a-form-item>
        <a-form-item label="部门">
          <a-tree-select
            v-model:value="memberForm.departmentId"
            allow-clear
            placeholder="未分配"
            :tree-data="departments"
            :field-names="{ label: 'deptName', value: 'id', children: 'children' }"
            tree-default-expand-all
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="memberForm.status">
            <a-select-option :value="1">正常</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, UserAddOutlined } from '@ant-design/icons-vue'
import {
  ROLE_OPTIONS,
  createDepartment,
  createWorkspace,
  deleteDepartment,
  deleteWorkspace,
  fetchDepartments,
  fetchMembers,
  fetchTenant,
  fetchWorkspaces,
  inviteMember,
  removeMember,
  updateDepartment,
  updateMember,
  updateTenant,
  updateWorkspace,
  type DepartmentItem,
  type MemberItem,
  type TenantInfo,
  type WorkspaceItem,
} from '@/api/org'
import { isProtectedMemberRole } from '@/config/roles'
import { formatDateTime } from '@/utils/datetime'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const canManageTenant = computed(() => auth.hasPermission('tenant:manage'))
const canReadUsers = computed(() => auth.hasAnyPermission(['user:read', 'member:manage', 'tenant:manage']))
const canInviteUser = computed(() => auth.hasAnyPermission(['user:create', 'member:manage', 'tenant:manage']))
const canUpdateUser = computed(() => auth.hasAnyPermission(['user:update', 'member:manage', 'tenant:manage']))
const canDeleteUser = computed(() => auth.hasAnyPermission(['user:delete', 'member:manage', 'tenant:manage']))
const canManageDept = computed(() => auth.hasAnyPermission(['user:update', 'member:manage', 'tenant:manage']))

const activeTab = ref('tenant')

const tenantLoading = ref(false)
const savingTenant = ref(false)
const tenantInfo = ref<TenantInfo | null>(null)
const tenantForm = reactive({
  tenantName: '',
  contactName: '',
  contactEmail: '',
  contactPhone: '',
})

const workspaceLoading = ref(false)
const workspaces = ref<WorkspaceItem[]>([])
const workspaceModalOpen = ref(false)
const editingWorkspaceId = ref<number | null>(null)
const workspaceForm = reactive({ workspaceName: '', description: '' })

const memberLoading = ref(false)
const members = ref<MemberItem[]>([])
const memberKeyword = ref('')
const memberDeptFilter = ref<number | undefined>(undefined)
const memberPage = ref(1)
const memberPageSize = ref(10)
const memberTotal = ref(0)
const memberPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false,
})

const inviteModalOpen = ref(false)
const inviting = ref(false)
const inviteForm = reactive({
  email: '',
  nickname: '',
  roleCode: 'developer',
  password: '',
  departmentId: undefined as number | undefined,
})

const memberModalOpen = ref(false)
const updatingMember = ref(false)
const editingMember = ref<MemberItem | null>(null)
const memberForm = reactive({
  roleCode: 'developer',
  status: 1,
  departmentId: undefined as number | undefined,
})

const deptLoading = ref(false)
const departments = ref<DepartmentItem[]>([])
const deptModalOpen = ref(false)
const savingDept = ref(false)
const editingDeptId = ref<number | null>(null)
const deptForm = reactive({
  deptName: '',
  parentId: undefined as number | undefined,
})

const workspaceColumns = [
  { title: '名称', key: 'name', dataIndex: 'workspaceName' },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '更新时间', key: 'updatedAt', width: 180 },
  { title: '操作', key: 'actions', width: 140 },
]

const memberColumns = [
  { title: '成员', key: 'member', width: 240 },
  { title: '角色', key: 'role', width: 120 },
  { title: '部门', key: 'department', width: 140 },
  { title: '状态', key: 'status', width: 100 },
  { title: '加入时间', key: 'joinedAt', width: 180 },
  { title: '最近登录', key: 'lastLoginAt', width: 180 },
  { title: '操作', key: 'actions', width: 140 },
]

function formatDate(value?: string) {
  if (!value) return '-'
  return value.slice(0, 10)
}

function formatNumber(value?: number) {
  if (value == null) return '-'
  return value.toLocaleString()
}

async function loadTenant() {
  tenantLoading.value = true
  try {
    const res = await fetchTenant()
    tenantInfo.value = res.data.data
    tenantForm.tenantName = tenantInfo.value?.tenantName || ''
    tenantForm.contactName = tenantInfo.value?.contactName || ''
    tenantForm.contactEmail = tenantInfo.value?.contactEmail || ''
    tenantForm.contactPhone = tenantInfo.value?.contactPhone || ''
  } catch {
    message.error('加载企业信息失败')
  } finally {
    tenantLoading.value = false
  }
}

async function saveTenant() {
  if (!canManageTenant.value) return
  if (!tenantForm.tenantName.trim()) {
    message.warning('请填写企业名称')
    return
  }
  savingTenant.value = true
  try {
    const res = await updateTenant({ ...tenantForm })
    tenantInfo.value = res.data.data
    message.success('企业信息已保存')
  } catch {
    message.error('保存失败')
  } finally {
    savingTenant.value = false
  }
}

async function loadWorkspaces() {
  workspaceLoading.value = true
  try {
    const res = await fetchWorkspaces()
    workspaces.value = res.data.data || []
  } catch {
    message.error('加载工作空间失败')
  } finally {
    workspaceLoading.value = false
  }
}

function openWorkspaceCreate() {
  if (!canManageTenant.value) return
  editingWorkspaceId.value = null
  workspaceForm.workspaceName = ''
  workspaceForm.description = ''
  workspaceModalOpen.value = true
}

function openWorkspaceEdit(record: WorkspaceItem) {
  if (!canManageTenant.value) return
  editingWorkspaceId.value = record.id
  workspaceForm.workspaceName = record.workspaceName
  workspaceForm.description = record.description || ''
  workspaceModalOpen.value = true
}

function resetWorkspaceForm() {
  editingWorkspaceId.value = null
  workspaceForm.workspaceName = ''
  workspaceForm.description = ''
}

async function saveWorkspace() {
  if (!canManageTenant.value) return
  if (!workspaceForm.workspaceName.trim()) {
    message.warning('请填写工作空间名称')
    return
  }
  try {
    if (editingWorkspaceId.value) {
      await updateWorkspace(editingWorkspaceId.value, { ...workspaceForm })
      message.success('工作空间已更新')
    } else {
      await createWorkspace({ ...workspaceForm })
      message.success('工作空间已创建')
    }
    workspaceModalOpen.value = false
    resetWorkspaceForm()
    loadWorkspaces()
  } catch {
    message.error('保存工作空间失败')
  }
}

async function onDeleteWorkspace(id: number) {
  if (!canManageTenant.value) return
  try {
    await deleteWorkspace(id)
    message.success('工作空间已删除')
    loadWorkspaces()
  } catch {
    message.error('删除失败')
  }
}

async function loadMembers() {
  memberLoading.value = true
  try {
    const res = await fetchMembers({
      page: memberPage.value,
      pageSize: memberPageSize.value,
      keyword: memberKeyword.value || undefined,
      departmentId: memberDeptFilter.value,
    })
    members.value = res.data.data?.list || []
    memberTotal.value = res.data.data?.total || 0
    memberPagination.current = memberPage.value
    memberPagination.total = memberTotal.value
  } catch {
    message.error('加载成员失败')
  } finally {
    memberLoading.value = false
  }
}

function onMemberDeptFilterChange() {
  memberPage.value = 1
  loadMembers()
}

async function loadDepartments() {
  deptLoading.value = true
  try {
    const res = await fetchDepartments()
    departments.value = res.data.data || []
  } catch {
    message.error('加载部门失败')
  } finally {
    deptLoading.value = false
  }
}

function openDeptCreate(parentId: number | null) {
  if (!canManageDept.value) return
  editingDeptId.value = null
  deptForm.deptName = ''
  deptForm.parentId = parentId || undefined
  deptModalOpen.value = true
}

function findDept(id: number, nodes: DepartmentItem[] = departments.value): DepartmentItem | undefined {
  for (const node of nodes) {
    if (node.id === id) return node
    const child = findDept(id, node.children || [])
    if (child) return child
  }
  return undefined
}

function openDeptEdit(node: DepartmentItem | { id?: number; key?: number; deptName?: string; title?: string; parentId?: number }) {
  if (!canManageDept.value) return
  const nodeLike = node as { id?: number; key?: number; deptName?: string; title?: string; parentId?: number }
  const id = Number(nodeLike.id || nodeLike.key)
  const found = findDept(id)
  editingDeptId.value = id
  deptForm.deptName = found?.deptName || nodeLike.deptName || String(nodeLike.title || '')
  deptForm.parentId = found?.parentId || undefined
  deptModalOpen.value = true
}

function resetDeptForm() {
  editingDeptId.value = null
  deptForm.deptName = ''
  deptForm.parentId = undefined
}

async function saveDepartment() {
  if (!canManageDept.value) return
  if (!deptForm.deptName.trim()) {
    message.warning('请填写部门名称')
    return
  }
  savingDept.value = true
  try {
    if (editingDeptId.value) {
      await updateDepartment(editingDeptId.value, {
        deptName: deptForm.deptName.trim(),
        parentId: deptForm.parentId,
      })
      message.success('部门已更新')
    } else {
      await createDepartment({
        deptName: deptForm.deptName.trim(),
        parentId: deptForm.parentId,
      })
      message.success('部门已创建')
    }
    deptModalOpen.value = false
    resetDeptForm()
    loadDepartments()
  } catch {
    message.error('保存部门失败')
  } finally {
    savingDept.value = false
  }
}

async function onDeleteDept(id: number) {
  if (!canManageDept.value) return
  try {
    await deleteDepartment(id)
    message.success('部门已删除')
    if (memberDeptFilter.value === id) {
      memberDeptFilter.value = undefined
    }
    loadDepartments()
    loadMembers()
  } catch {
    message.error('删除失败')
  }
}

function onMemberTableChange(pagination: { current?: number }) {
  memberPage.value = pagination.current || 1
  loadMembers()
}

function openInvite() {
  if (!canInviteUser.value) return
  inviteForm.email = ''
  inviteForm.nickname = ''
  inviteForm.roleCode = 'developer'
  inviteForm.password = ''
  inviteForm.departmentId = undefined
  inviteModalOpen.value = true
}

function resetInviteForm() {
  inviteForm.email = ''
  inviteForm.nickname = ''
  inviteForm.roleCode = 'developer'
  inviteForm.password = ''
  inviteForm.departmentId = undefined
}

async function submitInvite() {
  if (!canInviteUser.value) return
  if (!inviteForm.email.trim()) {
    message.warning('请填写邮箱')
    return
  }
  inviting.value = true
  try {
    await inviteMember({ ...inviteForm })
    message.success('成员邀请成功')
    inviteModalOpen.value = false
    resetInviteForm()
    loadMembers()
    loadTenant()
  } catch {
    message.error('邀请失败')
  } finally {
    inviting.value = false
  }
}

function isProtectedMember(record: MemberItem) {
  return isProtectedMemberRole(record.roleCode || '')
}

function openMemberEdit(record: MemberItem) {
  if (!canUpdateUser.value) return
  if (isProtectedMember(record)) {
    message.warning('不能对企业内的受保护角色进行该操作')
    return
  }
  editingMember.value = record
  memberForm.roleCode = record.roleCode || 'developer'
  memberForm.status = record.status ?? 1
  memberForm.departmentId = record.departmentId || undefined
  memberModalOpen.value = true
}

async function submitMemberUpdate() {
  if (!canUpdateUser.value) return
  if (!editingMember.value) return
  if (isProtectedMember(editingMember.value)) {
    message.warning('不能对企业内的受保护角色进行该操作')
    return
  }
  updatingMember.value = true
  try {
    await updateMember(editingMember.value.id, {
      roleCode: memberForm.roleCode,
      status: memberForm.status,
      departmentId: memberForm.departmentId ?? 0,
    })
    message.success('成员信息已更新')
    memberModalOpen.value = false
    loadMembers()
  } catch {
    message.error('更新失败')
  } finally {
    updatingMember.value = false
  }
}

async function onRemoveMember(id: number) {
  if (!canDeleteUser.value) return
  try {
    await removeMember(id)
    message.success('成员已移除')
    loadMembers()
    loadTenant()
  } catch {
    message.error('移除失败')
  }
}

onMounted(() => {
  if (canManageTenant.value) {
    activeTab.value = 'tenant'
    loadTenant()
    loadWorkspaces()
  } else if (canReadUsers.value) {
    activeTab.value = 'member'
  }
  if (canReadUsers.value) {
    loadDepartments()
    loadMembers()
  }
})
</script>

<style scoped>
.org-page :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
}

.tenant-form {
  max-width: none;
}

.form-actions {
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-meta {
  color: var(--text-secondary);
  font-size: 13px;
}

.default-tag {
  margin-left: 8px;
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

.member-locked {
  color: var(--text-muted);
  font-size: 12px;
}

.empty-dept {
  margin: 24px 0;
  color: var(--text-muted);
}

.dept-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding-right: 8px;
}

.dept-node em {
  margin-left: 8px;
  font-style: normal;
  font-size: 12px;
  color: var(--text-muted);
}

.field-tip {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}
</style>
