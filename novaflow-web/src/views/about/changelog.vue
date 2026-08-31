<template>
  <div class="about-body">
    <div class="about-block intro-block">
      <div class="release-head">
        <h3 class="release-version">NovaFlow v1.0.0</h3>
        <a-tag color="blue">正式发布</a-tag>
      </div>
      <p class="about-block-lead">
        企业级 AI Agent 开发平台首个正式发布版本，覆盖 Agent 编排、工作流、知识库、私有化部署与平台治理能力。
      </p>
      <div class="release-meta">发布日期 2026-08-31</div>
    </div>

    <section class="release-section">
      <h3 class="about-block-title">本版本新增</h3>
      <div class="about-card-grid">
        <div v-for="item in newFeatures" :key="item.title" class="mini-card">
          <div class="mini-card-title">{{ item.title }}</div>
          <div class="mini-card-desc">{{ item.desc }}</div>
        </div>
      </div>
    </section>

    <section class="release-section">
      <h3 class="about-block-title">平台能力</h3>
      <div class="about-card-grid">
        <div v-for="item in platformCapabilities" :key="item.title" class="mini-card">
          <div class="mini-card-title">{{ item.title }}</div>
          <div class="mini-card-desc">{{ item.desc }}</div>
        </div>
      </div>
    </section>

    <div class="about-block roadmap-block">
      <span class="roadmap-label">后续规划</span>
      <span class="roadmap-text">单点登录（SSO）· 部门组织架构 · 成本分摊报表 · 外部告警通道（邮件 / Webhook）</span>
    </div>
  </div>
</template>

<script setup lang="ts">
const newFeatures = [
  { title: '工作流 Agent 节点', desc: '在工作流中调用已发布的 Agent，支持多 Agent 协作编排与串联执行。' },
  { title: '私有化部署', desc: '提供服务端与前端 Docker 镜像，配合编排文件一键启动完整环境，适配企业内网部署。' },
  { title: '链路追踪', desc: '工作流与 Agent 执行过程自动记录追踪信息，支持对接外部监控与观测平台。' },
  { title: '平台管理后台', desc: '平台管理员可管理租户、配置套餐与配额，并查看全局用量统计。' },
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
