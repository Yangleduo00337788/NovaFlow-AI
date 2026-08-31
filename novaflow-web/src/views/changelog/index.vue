<template>
  <div class="page-shell changelog-page">
    <div class="page-header">
      <div>
        <h1>版本记录</h1>
        <p>NovaFlow AI 产品发布历史与功能说明</p>
      </div>
      <span class="version-badge">v1.0</span>
    </div>

    <div class="changelog-hero page-card">
      <h2>NovaFlow v1.0.0</h2>
      <p class="hero-desc">
        企业级 AI Agent 开发平台首个正式发布版本，覆盖 Agent 编排、工作流、知识库、私有化部署与平台治理能力。
      </p>
      <div class="hero-meta">
        <span>发布日期 2026-08-31</span>
        <a-tag color="blue">正式发布</a-tag>
      </div>
    </div>

    <div class="page-card release-card">
      <div class="feature-group">
        <div class="group-head">
          <h3>本版本新增</h3>
        </div>
        <div class="feature-grid">
          <div v-for="item in newFeatures" :key="item.title" class="feature-card">
            <div class="feature-title">{{ item.title }}</div>
            <div class="feature-desc">{{ item.desc }}</div>
          </div>
        </div>
      </div>

      <div class="feature-group">
        <div class="group-head">
          <h3>平台能力</h3>
        </div>
        <div class="capability-grid">
          <div v-for="item in platformCapabilities" :key="item.title" class="capability-item">
            <div class="cap-title">{{ item.title }}</div>
            <div class="cap-desc">{{ item.desc }}</div>
          </div>
        </div>
      </div>

      <div class="roadmap-note">
        <span class="roadmap-label">后续规划</span>
        <span>单点登录（SSO）· 部门组织架构 · 成本分摊报表 · 外部告警通道（邮件 / Webhook）</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const newFeatures = [
  {
    title: '工作流 Agent 节点',
    desc: '在工作流中调用已发布的 Agent，支持多 Agent 协作编排与串联执行。',
  },
  {
    title: '私有化部署',
    desc: '提供服务端与前端 Docker 镜像，配合编排文件一键启动完整环境，适配企业内网部署。',
  },
  {
    title: '链路追踪',
    desc: '工作流与 Agent 执行过程自动记录追踪信息，支持对接外部监控与观测平台。',
  },
  {
    title: '平台管理后台',
    desc: '平台管理员可管理租户、配置套餐与配额，并查看全局用量统计。',
  },
  {
    title: '审计日志',
    desc: '关键操作全程留痕，支持按动作、资源类型与时间范围筛选查询。',
  },
  {
    title: '全局搜索',
    desc: '顶栏快速检索应用、Agent、知识库与工作流，一键跳转至目标页面。',
  },
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
.version-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.changelog-hero {
  background: linear-gradient(135deg, rgba(22, 119, 255, 0.06), rgba(14, 165, 233, 0.04));
}

.changelog-hero h2 {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
}

.hero-desc {
  margin: 0 0 12px;
  color: var(--text-secondary);
  line-height: 1.7;
  max-width: 720px;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}

.release-card {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.feature-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.group-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.feature-card {
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-subtle);
}

.feature-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--text-primary);
}

.feature-desc {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.capability-item {
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--card-bg);
}

.cap-title {
  font-weight: 600;
  margin-bottom: 4px;
  font-size: 14px;
}

.cap-desc {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.roadmap-note {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--bg-subtle);
  border: 1px dashed var(--border-strong);
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.roadmap-label {
  flex-shrink: 0;
  font-weight: 600;
  color: var(--text-primary);
}

@media (max-width: 1100px) {
  .feature-grid,
  .capability-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .feature-grid,
  .capability-grid {
    grid-template-columns: 1fr;
  }

  .hero-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
