<template>
  <div class="changelog-page">
    <div class="hero page-card">
      <div class="hero-content">
        <span class="version-badge">v1.0</span>
        <h1>NovaFlow 版本记录</h1>
        <p>企业级 AI Agent 开发平台首个正式发布版本，覆盖 Agent 编排、工作流、知识库、私有化部署与平台治理能力。</p>
      </div>
    </div>

    <div class="release page-card">
      <div class="release-header">
        <h2>v1.0.0</h2>
        <span class="release-date">2026-08-31</span>
        <a-tag color="blue">正式发布</a-tag>
      </div>

      <div class="feature-section">
        <h3>P0 · 发布阻塞项</h3>
        <ul>
          <li><strong>Multi-Agent 工作流节点</strong>：工作流编排器新增 Agent 节点，支持在工作流中调用已发布 Agent，实现多 Agent 协作编排。</li>
          <li><strong>私有化部署包</strong>：提供 Server / Web Docker 镜像与 <code>docker-compose.prod.yml</code> 一键启动完整栈（MySQL、Redis、MinIO、Qdrant、后端、前端）。</li>
          <li><strong>OpenTelemetry 基础接入</strong>：Workflow / Agent 执行自动上报 Span；支持 OTLP 与 Langfuse 最小集成（Basic Auth）。</li>
        </ul>
      </div>

      <div class="feature-section">
        <h3>P1 · 企业交付体验</h3>
        <ul>
          <li><strong>平台超管后台</strong>：租户 CRUD、套餐配额配置、全局用量统计（仅 super_admin）。</li>
          <li><strong>审计日志查询</strong>：关键操作留痕，支持按动作、资源类型、时间范围筛选。</li>
          <li><strong>全局搜索</strong>：顶栏搜索联动应用、Agent、知识库、工作流，快速跳转。</li>
        </ul>
      </div>

      <div class="feature-section">
        <h3>核心能力（MVP 延续）</h3>
        <div class="capability-grid">
          <div v-for="item in coreCapabilities" :key="item.title" class="capability-item">
            <div class="cap-title">{{ item.title }}</div>
            <div class="cap-desc">{{ item.desc }}</div>
          </div>
        </div>
      </div>

      <div class="feature-section muted">
        <h3>v1.1 规划（未纳入本版本）</h3>
        <p>SSO 单点登录 · 部门组织架构 · 成本分摊报表 · 外部告警通道（邮件/Webhook）</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const coreCapabilities = [
  { title: 'Agent Studio', desc: 'Chat / RAG / Tool / Workflow 四类 Agent，调试、发布、Embed 与 Open API。' },
  { title: '工作流 Studio', desc: 'LLM、知识库、工具、条件分支、Agent 节点可视化编排与执行追踪。' },
  { title: '知识库 Hub', desc: '文档上传、混合检索、Rerank、向量存储（Qdrant）。' },
  { title: '模型与工具', desc: '多模型接入、Prompt 模板、HTTP/MCP 工具市场。' },
  { title: '运行监控', desc: '调用日志、链路分析、可观测性大盘与配额账单。' },
  { title: '安全合规', desc: 'RBAC、Embed Token、Caller 隔离、审计日志、会话留存策略。' },
]
</script>

<style scoped>
.changelog-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 960px;
}

.hero {
  padding: 28px 32px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08), rgba(14, 165, 233, 0.06));
}

.version-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  background: #4f46e5;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 12px;
}

.hero h1 {
  margin: 0 0 8px;
  font-size: 28px;
}

.hero p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.release {
  padding: 24px 28px;
}

.release-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.release-header h2 {
  margin: 0;
  font-size: 22px;
}

.release-date {
  color: var(--text-secondary);
}

.feature-section {
  margin-bottom: 24px;
}

.feature-section h3 {
  margin: 0 0 10px;
  font-size: 16px;
}

.feature-section ul {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
}

.feature-section.muted {
  color: var(--text-secondary);
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.capability-item {
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 10px;
  padding: 12px 14px;
  background: var(--card-bg, #fff);
}

.cap-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.cap-desc {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .capability-grid {
    grid-template-columns: 1fr;
  }
}
</style>
