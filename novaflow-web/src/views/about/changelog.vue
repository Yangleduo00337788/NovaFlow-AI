<template>
  <div class="about-body">
    <div class="about-block intro-block">
      <div class="release-head">
        <h3 class="release-version">NovaFlow v1.1.0</h3>
        <a-tag color="green">正式发布</a-tag>
      </div>
      <p class="about-block-lead">
        应用门户、RBAC 对齐、部门组织、成本分摊与账单外部告警。SSO 仍延后。
      </p>
      <div class="release-meta">发布日期 2026-09-03</div>
    </div>

    <section class="release-section">
      <h3 class="about-block-title">v1.1 已交付</h3>
      <ul class="fix-list">
        <li v-for="item in v11Items" :key="item">{{ item }}</li>
      </ul>
    </section>

    <div class="about-block intro-block">
      <div class="release-head">
        <h3 class="release-version">NovaFlow v1.0.1</h3>
        <a-tag color="green">补丁版本</a-tag>
      </div>
      <p class="about-block-lead">
        修复 v1.0.0 全量测试中发现的问题，提升注册、审计日志、模型中心与 E2E 测试稳定性。
      </p>
      <div class="release-meta">发布日期 2026-08-31</div>
    </div>

    <section class="release-section">
      <h3 class="about-block-title">问题修复</h3>
      <ul class="fix-list">
        <li v-for="item in v101Fixes" :key="item">{{ item }}</li>
      </ul>
    </section>

    <section class="release-section">
      <h3 class="about-block-title">测试与工程</h3>
      <ul class="fix-list">
        <li v-for="item in v101Engineering" :key="item">{{ item }}</li>
      </ul>
    </section>

    <div class="about-block history-block">
      <div class="release-head">
        <h3 class="release-version">NovaFlow v1.0.0</h3>
        <a-tag color="blue">正式发布</a-tag>
      </div>
      <p class="about-block-lead">
        企业管理端（Studio，含 AI 应用开发）首个正式发布版本，覆盖 Agent 编排、工作流、知识库、
        私有化部署与租户内平台治理能力。终端用户通过 Embed / Open API 接入；同站应用门户见 v1.1。
      </p>
      <div class="release-meta">发布日期 2026-08-31</div>
    </div>

    <section class="release-section">
      <h3 class="about-block-title">v1.0.0 产品形态</h3>
      <ul class="fix-list">
        <li v-for="item in productForm" :key="item">{{ item }}</li>
      </ul>
    </section>

    <section class="release-section">
      <h3 class="about-block-title">v1.0.0 本版本新增</h3>
      <div class="about-card-grid">
        <div v-for="item in newFeatures" :key="item.title" class="mini-card">
          <div class="mini-card-title">{{ item.title }}</div>
          <div class="mini-card-desc">{{ item.desc }}</div>
        </div>
      </div>
    </section>

    <section class="release-section">
      <h3 class="about-block-title">v1.0.0 平台能力</h3>
      <div class="about-card-grid">
        <div v-for="item in platformCapabilities" :key="item.title" class="mini-card">
          <div class="mini-card-title">{{ item.title }}</div>
          <div class="mini-card-desc">{{ item.desc }}</div>
        </div>
      </div>
    </section>

    <div class="about-block roadmap-block">
      <span class="roadmap-label">后续规划</span>
      <span class="roadmap-text">v1.2：SSO（OAuth2/OIDC，按需）</span>
    </div>
  </div>
</template>

<script setup lang="ts">
const v11Items = [
  '应用门户：已发布应用列表、对话、当前用户会话历史。',
  'RBAC 对齐：保护超管成员、权限页默认当前角色、Studio 写按钮按权限码隐藏。',
  '部门组织：树形部门与成员归属（不替代角色权限）。',
  '成本分摊：账单页按应用 / 工作空间 / 用户汇总 Token 与费用。',
  '外部告警：账单配额预警可发站内信、邮件与公网 Webhook。',
]

const v101Fixes = [
  '修复企业注册时租户配额字段未写入导致注册失败的问题。',
  '修复审计日志在部分场景下 tenant_id 为空导致写入/查询异常的问题。',
  '修复模型提供商 API Key 因加密密钥不一致导致列表接口 500 的问题，改为友好提示。',
  '修复应用发布返回的 Embed 路径与前端路由不一致（/embed/agent → /embed/agents）导致嵌入 404 的问题。',
  '将登录/注册频率限制默认值从 20 次/分钟提升至 120 次/分钟，避免 E2E 与联调误触发限流。',
]

const v101Engineering = [
  '新增全模块 API 集成冒烟测试与平台超管专项测试。',
  '新增 Playwright 全站页面冒烟 E2E（19 个业务页面 + 注册页）。',
  '本地集成测试默认使用开发环境加密密钥，与 .env 配置保持一致。',
  '补充 v1.0 产品形态说明：Studio / 应用门户 / 总控同站；同步更新 README、PRD、系统架构设计、关于页与更新日志。',
]

const productForm = [
  '企业管理端（Studio）：控制台主体，含 AI 应用开发与企业治理，面向企业管理员与开发者。',
  '应用门户：v1.1 已交付同站应用中心、对话与会话历史；v1.0 另可通过 Embed / Open API 使用已发布应用。',
  '总控：独立平台治理域（`/platform/login` + `/platform/*`），与 Studio 分登录入口与主题色。',
]

const newFeatures = [
  { title: '工作流 Agent 节点', desc: '在工作流中调用已发布的 Agent，支持多 Agent 协作编排与串联执行。' },
  { title: '私有化部署', desc: '提供服务端与前端 Docker 镜像，配合编排文件一键启动完整环境，适配企业内网部署。' },
  { title: '链路追踪', desc: '工作流与 Agent 执行过程自动记录追踪信息，支持对接外部监控与观测平台。' },
  { title: '总控（平台域）', desc: '平台管理员通过 /platform/login 进入治理后台，管理租户、用户、计费与审计。' },
  { title: '审计日志', desc: '关键操作全程留痕，支持按动作、资源类型与时间范围筛选查询。' },
  { title: '全局搜索', desc: '顶栏快速检索应用、Agent、知识库与工作流，一键跳转至目标页面。' },
]

const platformCapabilities = [
  { title: 'Agent 编排', desc: 'Chat / RAG / Tool / Workflow 四类 Agent，支持调试、发布、嵌入与开放 API。' },
  { title: '工作流编排', desc: 'LLM、知识库、工具、条件分支与 Agent 节点可视化编排，配套执行追踪。' },
  { title: '知识库', desc: '文档上传、混合检索、重排序与向量存储，支撑 RAG 场景。' },
  { title: '模型与工具', desc: '多模型接入、Prompt 模板管理，以及 HTTP / MCP 工具市场。' },
  { title: '运行监控', desc: '调用日志、链路分析、可观测性大盘与配额账单一览。' },
  { title: '安全合规', desc: '角色权限、嵌入令牌、调用方隔离、审计日志与会话留存策略。' },
]
</script>

<style scoped>
@import './doc-styles.css';

.intro-block {
  background: linear-gradient(135deg, rgba(82, 196, 26, 0.08), rgba(14, 165, 233, 0.04));
  border-color: rgba(82, 196, 26, 0.18);
}

.history-block {
  margin-top: 28px;
  background: linear-gradient(135deg, rgba(22, 119, 255, 0.06), rgba(14, 165, 233, 0.04));
  border-color: rgba(22, 119, 255, 0.12);
}

.release-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.release-version {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.release-meta {
  margin-top: 10px;
  font-size: 13px;
  color: var(--text-muted);
}

.release-section + .release-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

.fix-list {
  margin: 0;
  padding-left: 20px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

.mini-card {
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-subtle);
}

.mini-card-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
}

.mini-card-desc {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.roadmap-block {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  border-style: dashed;
}

.roadmap-label {
  flex-shrink: 0;
  font-weight: 600;
  font-size: 14px;
}

.roadmap-text {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>
