<template>
  <div class="org-page" data-testid="org-page">
    <div class="page-header">
      <div>
        <h1>组织管理</h1>
        <p>管理企业信息、工作空间与成员</p>
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="tenant" tab="企业信息">
        <div class="page-card tenant-card">
          <a-spin :spinning="tenantLoading">
            <a-form layout="vertical" :model="tenantForm" class="tenant-form">
              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="企业名称" required>
                    <a-input v-model:value="tenantForm.tenantName" placeholder="企业名称" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="企业编码">
                    <a-input :value="tenantInfo?.tenantCode" disabled />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-row :gutter="16">
                <a-col :span="8">
                  <a-form-item label="联系人">
                    <a-input v-model:value="tenantForm.contactName" placeholder="联系人姓名" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="联系邮箱">
                    <a-input v-model:value="tenantForm.contactEmail" placeholder="contact@company.com" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="联系电话">
                    <a-input v-model:value="tenantForm.contactPhone" placeholder="联系电话" />
                  </a-form-item>
                </a-col>
              </a-row>

              <div class="quota-grid">
                <div class="quota-item">
                  <span class="quota-label">套餐类型</span>
                  <strong>{{ tenantInfo?.planTypeLabel || '-' }}</strong>
                </div>
                <div class="quota-item">
                  <span class="quota-label">到期时间</span>
                  <strong>{{ formatDate(tenantInfo?.expireAt) }}</strong>
                </div>
                <div class="quota-item">
                  <span class="quota-label">成员席位</span>
                  <strong>{{ tenantInfo?.memberCount || 0 }} / {{ tenantInfo?.maxMembers || 0 }}</strong>
                </div>
                <div class="quota-item">
                  <span class="quota-label">Agent 配额</span>
                  <strong>{{ tenantInfo?.maxAgents || 0 }}</strong>
                </div>
                <div class="quota-item">
                  <span class="quota-label">知识库配额</span>
                  <strong>{{ tenantInfo?.maxKnowledge || 0 }}</strong>
                </div>
                <div class="quota-item">
                  <span class="quota-label">月 Token 配额</span>
                  <strong>{{ formatNumber(tenantInfo?.monthlyTokenQuota) }}</strong>
                </div>
              </div>

              <div class="form-actions">
                <a-button type="primary" :loading="savingTenant" @click="saveTenant">保存企业信息</a-button>
              </div>
            </a-form>
          </a-spin>
        </div>
      </a-tab-pane>

      <a-tab-pane key="workspace" tab="工作空间">
        <div class="page-card">
          <div class="section-toolbar">
            <span class="toolbar-meta">共 {{ workspaces.length }} 个工作空间</span>
            <a-button type="primary" @click="openWorkspaceCreate">
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
                <a-space>
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

      <a-tab-pane key="member" tab="成员管理">
        <div class="page-card">
          <div class="section-toolbar">
            <a-input-search
              v-model:value="memberKeyword"
              placeholder="搜索成员邮箱/昵称"
              style="width: 260px"
              allow-clear
              @search="loadMembers"
            />
            <a-button type="primary" @click="openInvite">
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
                <a-space>
                  <a-button type="link" size="small" @click="openMemberEdit(record)">编辑</a-button>
                  <a-popconfirm title="确认移除该成员？" @confirm="onRemoveMember(record.id)">
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
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, UserAddOutlined } from '@ant-design/icons-vue'
import {
  ROLE_OPTIONS,
  createWorkspace,
  deleteWorkspace,
  fetchMembers,
  fetchTenant,
  fetchWorkspaces,
  inviteMember,
  removeMember,
  updateMember,
  updateTenant,
  updateWorkspace,
  type MemberItem,
  type TenantInfo,
  type WorkspaceItem,
} from '@/api/org'
import { formatDateTime } from '@/utils/datetime'

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
})

const memberModalOpen = ref(false)
const updatingMember = ref(false)
const editingMember = ref<MemberItem | null>(null)
const memberForm = reactive({ roleCode: 'developer', status: 1 })

const workspaceColumns = [
  { title: '名称', key: 'name', dataIndex: 'workspaceName' },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '更新时间', key: 'updatedAt', width: 180 },
  { title: '操作', key: 'actions', width: 140 },
]

const memberColumns = [
  { title: '成员', key: 'member', width: 240 },
  { title: '角色', key: 'role', width: 120 },
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
  editingWorkspaceId.value = null
  workspaceForm.workspaceName = ''
  workspaceForm.description = ''
  workspaceModalOpen.value = true
}

function openWorkspaceEdit(record: WorkspaceItem) {
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

function onMemberTableChange(pagination: { current?: number }) {
  memberPage.value = pagination.current || 1
  loadMembers()
}

function openInvite() {
  inviteForm.email = ''
  inviteForm.nickname = ''
  inviteForm.roleCode = 'developer'
  inviteForm.password = ''
  inviteModalOpen.value = true
}

function resetInviteForm() {
  inviteForm.email = ''
  inviteForm.nickname = ''
  inviteForm.roleCode = 'developer'
  inviteForm.password = ''
}

async function submitInvite() {
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

function openMemberEdit(record: MemberItem) {
  editingMember.value = record
  memberForm.roleCode = record.roleCode || 'developer'
  memberForm.status = record.status ?? 1
  memberModalOpen.value = true
}

async function submitMemberUpdate() {
  if (!editingMember.value) return
  updatingMember.value = true
  try {
    await updateMember(editingMember.value.id, { ...memberForm })
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
  loadTenant()
  loadWorkspaces()
  loadMembers()
})
</script>

<style scoped>
.org-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tenant-card {
  max-width: 960px;
}

.quota-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 8px 0 20px;
}

.quota-item {
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--card-bg);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quota-label {
  color: var(--text-secondary);
  font-size: 12px;
}

.form-actions {
  margin-top: 8px;
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

.field-tip {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}
</style>
