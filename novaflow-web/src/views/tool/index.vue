<template>
  <div class="tool-page page-shell" data-testid="tool-page">
    <div class="page-header">
      <div>
        <h1>工具市场</h1>
        <p>{{ pageSubtitle }}</p>
      </div>
      <a-button type="primary" data-testid="create-tool-btn" @click="onCreateClick">
        <PlusOutlined />
        {{ createButtonLabel }}
      </a-button>
    </div>

    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="skill" tab="Skill 技能">
        <div class="concept-banner">
          <div class="concept-banner__title">Skill = 流程与知识</div>
          <p>
            上传符合 Agent Skills 标准的 <code>SKILL.md</code> 文件，为 Agent 注入流程与领域知识。
            技能不会作为可调用工具，而是在对话时指导 Agent「怎么做」。
          </p>
        </div>
        <div class="list-panel page-card">
          <div class="list-toolbar">
            <div class="list-toolbar-filters">
              <a-input-search
                v-model:value="keyword"
                placeholder="搜索技能名称或标识"
                style="width: 240px"
                allow-clear
                @search="loadData"
              />
            </div>
            <span class="list-toolbar-meta">共 {{ total }} 个技能</span>
          </div>
          <div class="list-body">
            <a-spin :spinning="loading">
              <div v-if="list.length" class="tool-grid">
        <div
          v-for="item in list"
          :key="item.id"
          class="tool-card page-card"
          :data-testid="`tool-card-${item.id}`"
        >
          <div class="tool-card-head">
            <div class="tool-icon skill">
              <ThunderboltOutlined />
            </div>
            <div class="tool-title-wrap">
              <h3>{{ item.displayName }}</h3>
              <p class="tool-name">{{ item.toolName }}</p>
            </div>
          </div>
          <p class="tool-desc">{{ item.description || '暂无描述' }}</p>
          <div class="tool-meta">
            <a-tag color="purple">Skill</a-tag>
            <span class="tool-url" :title="item.skillFileName">{{ item.skillFileName || 'SKILL.md' }}</span>
          </div>
          <p v-if="item.skillContentPreview" class="skill-preview">{{ item.skillContentPreview }}</p>
          <div class="tool-footer">
            <span class="tool-time">{{ formatDateTime(item.updatedAt) }}</span>
          </div>
          <div class="tool-actions">
            <a-button type="link" size="small" @click="openSkillView(item)">查看</a-button>
            <a-button type="link" size="small" @click="openSkillReupload(item)">重新上传</a-button>
            <a-popconfirm title="确认删除该技能？" @confirm="onDelete(item.id)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>
              <a-empty v-else description="暂无技能，点击右上角上传 SKILL.md" />
            </a-spin>

            <div v-if="total > pageSize" class="pagination-wrap">
              <a-pagination
                v-model:current="page"
                :total="total"
                :page-size="pageSize"
                show-less-items
                @change="loadData"
              />
            </div>
          </div>
        </div>
      </a-tab-pane>

      <a-tab-pane key="mcp" tab="MCP 插件">
        <div class="concept-banner">
          <div class="concept-banner__title">MCP = 插件与连接</div>
          <p>
            插件让 Agent「能连上外部系统」：通过 Model Context Protocol 连接数据库、API、文件系统等，
            由独立进程暴露 tools / resources（对应 MCP Server / Plugin 连接层）。
          </p>
        </div>
        <div class="section-block">
          <div class="section-header">
            <h2>插件服务</h2>
            <p>注册并连接 MCP Server，发现工具后可同步到下方插件市场</p>
          </div>
          <div class="list-panel page-card">
            <div class="list-toolbar">
              <div class="list-toolbar-filters">
                <a-input-search
                  v-model:value="mcpKeyword"
                  placeholder="搜索 MCP 服务名称"
                  style="width: 240px"
                  allow-clear
                  @search="loadMcpData"
                />
              </div>
              <span class="list-toolbar-meta">共 {{ mcpTotal }} 个服务</span>
            </div>
            <div class="list-body">
              <a-spin :spinning="mcpLoading">
                <div v-if="mcpList.length" class="tool-grid">
            <div v-for="item in mcpList" :key="item.id" class="tool-card page-card">
              <div class="tool-card-head">
                <div class="tool-icon mcp">
                  <ApiOutlined />
                </div>
                <div class="tool-title-wrap">
                  <h3>{{ item.serverName }}</h3>
                  <p class="tool-name">{{ item.transportType }}</p>
                </div>
              </div>
              <p class="tool-desc">{{ item.description || '暂无描述' }}</p>
              <div class="tool-meta">
                <a-tag :color="item.status === 1 ? 'success' : item.status === 2 ? 'error' : 'default'">
                  {{ item.statusLabel }}
                </a-tag>
                <span class="tool-url" :title="item.commandSummary">{{ item.commandSummary }}</span>
              </div>
              <div class="tool-footer">
                <span class="tool-time">
                  发现 {{ item.toolCount }} 个
                  <template v-if="item.syncedToolCount"> · 已同步 {{ item.syncedToolCount }} 个</template>
                  · {{ formatDateTime(item.updatedAt) }}
                </span>
              </div>
              <div class="tool-actions">
                <a-button type="link" size="small" :loading="mcpConnectingId === item.id" @click="onMcpConnect(item)">
                  连接测试
                </a-button>
                <a-button type="link" size="small" :disabled="!item.toolCount" @click="openMcpTools(item)">
                  工具列表
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  :loading="mcpSyncingId === item.id"
                  :disabled="item.status !== 1 || !item.toolCount"
                  @click="onMcpSync(item)"
                >
                  同步市场
                </a-button>
                <a-popconfirm title="确认删除该 MCP 服务？" @confirm="onMcpDelete(item.id)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </div>
            </div>
          </div>
                <a-empty v-else description="暂无 MCP 插件，点击右上角配置" />
              </a-spin>
            </div>
          </div>
        </div>

        <div class="section-block">
          <div class="section-header">
            <h2>插件工具</h2>
            <p>从 MCP 插件同步到市场的可调用工具，可在 Agent 中直接选用</p>
          </div>
          <div class="list-panel page-card">
            <div class="list-toolbar">
              <div class="list-toolbar-filters">
                <a-input-search
                  v-model:value="mcpMarketKeyword"
                  placeholder="搜索插件工具名称"
                  style="width: 240px"
                  allow-clear
                  @search="loadMcpMarketData"
                />
              </div>
              <span class="list-toolbar-meta">共 {{ mcpMarketTotal }} 个插件工具</span>
            </div>
            <div class="list-body">
              <a-spin :spinning="mcpMarketLoading">
                <div v-if="mcpMarketList.length" class="tool-grid">
              <div
                v-for="item in mcpMarketList"
                :key="item.id"
                class="tool-card page-card"
                :data-testid="`mcp-tool-card-${item.id}`"
              >
                <div class="tool-card-head">
                  <div class="tool-icon mcp">
                    <ApiOutlined />
                  </div>
                  <div class="tool-title-wrap">
                    <h3>{{ item.displayName }}</h3>
                    <p class="tool-name">{{ item.toolName }}</p>
                  </div>
                </div>
                <p class="tool-desc">{{ item.description || '暂无描述' }}</p>
                <div class="tool-meta">
                  <a-tag color="orange">MCP 插件</a-tag>
                  <span class="tool-url" :title="item.sourceServerName || item.mcpToolName">
                    {{ item.sourceServerName || item.mcpToolName || 'MCP 工具' }}
                  </span>
                </div>
                <div class="tool-footer">
                  <span class="tool-time">{{ formatDateTime(item.updatedAt) }}</span>
                </div>
                <div class="tool-actions">
                  <a-popconfirm title="确认删除该插件工具？" @confirm="onDeleteMcpMarketTool(item.id)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </div>
              </div>
            </div>
                <a-empty v-else description="暂无插件工具，请先在上方插件服务中执行「同步市场」" />
              </a-spin>

              <div v-if="mcpMarketTotal > mcpMarketPageSize" class="pagination-wrap">
                <a-pagination
                  v-model:current="mcpMarketPage"
                  :total="mcpMarketTotal"
                  :page-size="mcpMarketPageSize"
                  show-less-items
                  @change="loadMcpMarketData"
                />
              </div>
            </div>
          </div>
        </div>
      </a-tab-pane>
    </a-tabs>

    <a-drawer
      v-model:open="skillUploadOpen"
      :title="skillReuploadId ? '重新上传技能' : '上传技能'"
      :width="560"
      @close="resetSkillUpload"
    >
      <p class="upload-hint">
        请上传 <code>SKILL.md</code> 文件。支持 YAML frontmatter（<code>name</code>、<code>description</code>）自动解析名称与描述。
      </p>
      <a-upload-dragger
        :file-list="skillFileList"
        accept=".md"
        :max-count="1"
        :before-upload="beforeSkillUpload"
        @remove="onSkillFileRemove"
      >
        <p class="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p class="ant-upload-text">点击或拖拽 SKILL.md 到此处</p>
        <p class="ant-upload-hint">仅支持 .md 格式，最大 512KB</p>
      </a-upload-dragger>
      <a-button
        type="primary"
        block
        :loading="skillUploading"
        style="margin-top: 16px"
        :disabled="!skillUploadFile"
        @click="submitSkillUpload"
      >
        {{ skillReuploadId ? '确认重新上传' : '确认上传' }}
      </a-button>
    </a-drawer>

    <a-drawer
      v-model:open="skillViewOpen"
      :title="skillViewItem ? `${skillViewItem.displayName} · 技能内容` : '技能内容'"
      :width="720"
      @close="resetSkillView"
    >
      <a-spin :spinning="skillViewLoading">
        <div v-if="skillViewItem" class="skill-view-meta">
          <a-tag color="purple">Skill</a-tag>
          <span>{{ skillViewItem.skillFileName || 'SKILL.md' }}</span>
        </div>
        <pre v-if="skillViewContent" class="skill-content">{{ skillViewContent }}</pre>
        <a-empty v-else description="暂无内容" />
      </a-spin>
    </a-drawer>

    <a-drawer v-model:open="mcpDrawerOpen" title="配置 MCP 插件" :width="640" @close="resetMcpForm">
      <a-form layout="vertical" :model="mcpForm">
        <a-form-item label="插件名称" required>
          <a-input v-model:value="mcpForm.serverName" placeholder="example-server" />
          <div class="form-hint">若使用 mcpServers 完整配置，名称需与 JSON 中的 key 一致</div>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="mcpForm.description" :rows="2" placeholder="图像生成 MCP 插件" />
        </a-form-item>
        <a-form-item label="服务配置（JSON）" required>
          <a-textarea
            v-model:value="mcpConfigJson"
            :rows="14"
            placeholder="粘贴 mcpServers 配置"
          />
          <div class="form-hint">
            支持 Cursor 风格配置，例如 command / args / env；也可直接粘贴 mcpServers 包裹格式
          </div>
        </a-form-item>
        <a-button type="primary" block :loading="mcpSaving" @click="onMcpSave">保存</a-button>
      </a-form>
    </a-drawer>

    <a-drawer
      v-model:open="mcpToolsDrawerOpen"
      :title="mcpToolsServer ? `${mcpToolsServer.serverName} · 工具列表` : '工具列表'"
      :width="640"
      @close="resetMcpTools"
    >
      <a-spin :spinning="mcpToolsLoading">
        <a-empty v-if="!mcpTools.length" description="暂无已发现工具，请先执行连接测试" />
        <div v-else class="mcp-tools-list">
          <div v-for="tool in mcpTools" :key="tool.name" class="mcp-tool-item page-card">
            <div class="mcp-tool-head">
              <h4>{{ tool.name }}</h4>
              <p>{{ tool.description || '暂无描述' }}</p>
            </div>
            <pre v-if="tool.inputSchema" class="mcp-tool-schema">{{ formatSchema(tool.inputSchema) }}</pre>
          </div>
        </div>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { UploadFile } from 'ant-design-vue'
import { ApiOutlined, InboxOutlined, PlusOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import {
  deleteTool,
  fetchTool,
  fetchTools,
  reuploadSkill,
  uploadSkill,
  type ToolDefinition,
} from '@/api/tool'
import {
  connectMcpServer,
  createMcpServer,
  deleteMcpServer,
  fetchMcpServerDetail,
  fetchMcpServers,
  syncMcpServerTools,
  type McpDiscoveredTool,
  type McpServer,
} from '@/api/mcp'
import { formatDateTime } from '@/utils/datetime'

const activeTab = ref('skill')

const pageSubtitle = computed(() =>
  activeTab.value === 'skill'
    ? 'Skill 技能：上传 SKILL.md，为 Agent 注入流程与领域知识'
    : 'MCP 插件：配置外部连接并同步可调用工具',
)

const createButtonLabel = computed(() =>
  activeTab.value === 'skill' ? '上传技能' : '配置 MCP 插件',
)
const loading = ref(false)
const list = ref<ToolDefinition[]>([])
const keyword = ref('')
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)

const skillUploadOpen = ref(false)
const skillReuploadId = ref<number | null>(null)
const skillUploadFile = ref<File | null>(null)
const skillFileList = ref<UploadFile[]>([])
const skillUploading = ref(false)
const skillViewOpen = ref(false)
const skillViewLoading = ref(false)
const skillViewItem = ref<ToolDefinition | null>(null)
const skillViewContent = ref('')

const mcpLoading = ref(false)
const mcpSaving = ref(false)
const mcpList = ref<McpServer[]>([])
const mcpKeyword = ref('')
const mcpPage = ref(1)
const mcpTotal = ref(0)
const mcpDrawerOpen = ref(false)
const mcpConnectingId = ref<number | null>(null)
const mcpSyncingId = ref<number | null>(null)
const mcpToolsDrawerOpen = ref(false)
const mcpToolsLoading = ref(false)
const mcpToolsServer = ref<McpServer | null>(null)
const mcpTools = ref<McpDiscoveredTool[]>([])
const mcpToolsCache = ref<Record<number, McpDiscoveredTool[]>>({})
const mcpMarketLoading = ref(false)
const mcpMarketList = ref<ToolDefinition[]>([])
const mcpMarketKeyword = ref('')
const mcpMarketPage = ref(1)
const mcpMarketPageSize = 12
const mcpMarketTotal = ref(0)
const mcpForm = reactive({
  serverName: '',
  description: '',
})
const mcpConfigJson = ref(`{
  "mcpServers": {
    "example-server": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-server-example"
      ]
    }
  }
}`)

function onCreateClick() {
  if (activeTab.value === 'mcp') {
    resetMcpForm()
    mcpDrawerOpen.value = true
    return
  }
  openSkillUpload()
}

function onTabChange(key: string | number) {
  if (key === 'mcp') {
    if (!mcpList.value.length) {
      loadMcpData()
    }
    loadMcpMarketData()
  }
}

function resetMcpForm() {
  Object.assign(mcpForm, {
    serverName: '',
    description: '',
  })
  mcpConfigJson.value = `{
  "mcpServers": {
    "example-server": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-server-example"
      ]
    }
  }
}`
}

async function loadMcpData() {
  mcpLoading.value = true
  try {
    const res = await fetchMcpServers({
      page: mcpPage.value,
      pageSize: 12,
      keyword: mcpKeyword.value || undefined,
    })
    mcpList.value = res.data.data.list
    mcpTotal.value = res.data.data.total
  } finally {
    mcpLoading.value = false
  }
}

async function onMcpConnect(item: McpServer) {
  mcpConnectingId.value = item.id
  try {
    const res = await connectMcpServer(item.id)
    const result = res.data.data
    if (result.status === 1) {
      if (result.tools?.length) {
        mcpToolsCache.value[item.id] = result.tools
      }
      message.success(result.message || `连接成功，发现 ${result.toolCount} 个工具`)
    } else {
      message.error(result.message || '连接失败')
    }
    loadMcpData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '连接失败')
    loadMcpData()
  } finally {
    mcpConnectingId.value = null
  }
}

async function onMcpSync(item: McpServer) {
  mcpSyncingId.value = item.id
  try {
    const res = await syncMcpServerTools(item.id)
    const result = res.data.data
    message.success(result.message || `已同步 ${result.syncedToolCount} 个插件工具到市场`)
    loadMcpData()
    loadMcpMarketData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '同步失败')
  } finally {
    mcpSyncingId.value = null
  }
}

async function openMcpTools(item: McpServer) {
  mcpToolsServer.value = item
  mcpToolsDrawerOpen.value = true
  mcpTools.value = mcpToolsCache.value[item.id] || []
  mcpToolsLoading.value = true
  try {
    const res = await fetchMcpServerDetail(item.id)
    mcpToolsServer.value = res.data.data
    const tools = res.data.data.tools || []
    mcpTools.value = tools
    if (tools.length) {
      mcpToolsCache.value[item.id] = tools
    }
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载工具列表失败')
    mcpTools.value = []
  } finally {
    mcpToolsLoading.value = false
  }
}

function resetMcpTools() {
  mcpToolsServer.value = null
  mcpTools.value = []
}

function formatSchema(schema: Record<string, unknown>) {
  return JSON.stringify(schema, null, 2)
}

async function onMcpSave() {
  if (!mcpForm.serverName.trim()) {
    message.warning('请填写插件名称')
    return
  }
  if (!mcpConfigJson.value.trim()) {
    message.warning('请填写 MCP 服务配置')
    return
  }
  try {
    JSON.parse(mcpConfigJson.value)
  } catch {
    message.error('服务配置 JSON 格式不正确')
    return
  }
  mcpSaving.value = true
  try {
    await createMcpServer({
      serverName: mcpForm.serverName.trim(),
      description: mcpForm.description.trim() || undefined,
      serverConfig: mcpConfigJson.value.trim(),
    })
    message.success('MCP 插件已注册')
    mcpDrawerOpen.value = false
    loadMcpData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '注册失败')
  } finally {
    mcpSaving.value = false
  }
}

async function onMcpDelete(id: number) {
  try {
    await deleteMcpServer(id)
    message.success('已删除')
    loadMcpData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

function resetSkillUpload() {
  skillReuploadId.value = null
  skillUploadFile.value = null
  skillFileList.value = []
}

function openSkillUpload() {
  resetSkillUpload()
  skillUploadOpen.value = true
}

function openSkillReupload(item: ToolDefinition) {
  skillReuploadId.value = item.id
  skillUploadFile.value = null
  skillFileList.value = []
  skillUploadOpen.value = true
}

function beforeSkillUpload(file: File) {
  if (!file.name.toLowerCase().endsWith('.md')) {
    message.warning('仅支持 .md 格式的 Skill 文件')
    return false
  }
  skillUploadFile.value = file
  skillFileList.value = [{ uid: String(Date.now()), name: file.name, status: 'done' }]
  return false
}

function onSkillFileRemove() {
  skillUploadFile.value = null
  skillFileList.value = []
}

async function submitSkillUpload() {
  if (!skillUploadFile.value) {
    message.warning('请先选择 SKILL.md 文件')
    return
  }
  skillUploading.value = true
  try {
    if (skillReuploadId.value) {
      await reuploadSkill(skillReuploadId.value, skillUploadFile.value)
      message.success('技能已更新')
    } else {
      await uploadSkill(skillUploadFile.value)
      message.success('技能上传成功')
    }
    skillUploadOpen.value = false
    resetSkillUpload()
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '上传失败')
  } finally {
    skillUploading.value = false
  }
}

async function openSkillView(item: ToolDefinition) {
  skillViewItem.value = item
  skillViewContent.value = ''
  skillViewOpen.value = true
  skillViewLoading.value = true
  try {
    const res = await fetchTool(item.id)
    skillViewItem.value = res.data.data
    skillViewContent.value = res.data.data.skillContent || res.data.data.skillContentPreview || ''
  } catch (e) {
    message.error(e instanceof Error ? e.message : '加载技能内容失败')
  } finally {
    skillViewLoading.value = false
  }
}

function resetSkillView() {
  skillViewItem.value = null
  skillViewContent.value = ''
}

async function loadMcpMarketData() {
  mcpMarketLoading.value = true
  try {
    const res = await fetchTools({
      page: mcpMarketPage.value,
      pageSize: mcpMarketPageSize,
      keyword: mcpMarketKeyword.value || undefined,
      toolType: 'mcp',
    })
    mcpMarketList.value = res.data.data.list
    mcpMarketTotal.value = res.data.data.total
  } finally {
    mcpMarketLoading.value = false
  }
}

async function onDeleteMcpMarketTool(id: number) {
  try {
    await deleteTool(id)
    message.success('删除成功')
    loadMcpMarketData()
    loadMcpData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchTools({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      toolType: 'skill',
    })
    list.value = res.data.data.list
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

async function onDelete(id: number) {
  try {
    await deleteTool(id)
    message.success('删除成功')
    loadData()
  } catch (e) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.tool-page {
  min-height: auto;
}

.concept-banner {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-subtle);
}

.concept-banner__title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
}

.concept-banner p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.section-block + .section-block {
  margin-top: 20px;
}

.section-header {
  margin-bottom: 12px;
}

.section-header h2 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
}

.section-header p {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.tool-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.tool-card-head {
  display: flex;
  gap: 12px;
}

.tool-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(22, 119, 255, 0.1);
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.tool-icon.mcp {
  background: #fff7ed;
  color: #ea580c;
}

.tool-icon.skill {
  background: #f5f3ff;
  color: #7c3aed;
}

.tool-title-wrap h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.tool-name {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
  font-family: monospace;
}

.tool-desc {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
  min-height: 40px;
}

.tool-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-url {
  flex: 1;
  font-size: 12px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-footer {
  margin-top: auto;
}

.tool-time {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tool-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 4px;
  border-top: 1px solid var(--border, #f1f5f9);
  padding-top: 8px;
}

.tool-actions :deep(.ant-btn-link) {
  padding: 0 6px;
  height: auto;
  line-height: 1.5;
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.upload-hint {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.skill-preview {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.skill-view-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}

.skill-content {
  margin: 0;
  padding: 14px;
  background: var(--bg-muted, #f8fafc);
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: calc(100vh - 180px);
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.mcp-tools-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mcp-tool-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mcp-tool-head h4 {
  margin: 0 0 4px;
  font-size: 14px;
  font-family: monospace;
}

.mcp-tool-head p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.mcp-tool-schema {
  margin: 0;
  padding: 10px;
  background: var(--bg-muted, #f8fafc);
  border-radius: 8px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 200px;
  overflow: auto;
}
</style>
