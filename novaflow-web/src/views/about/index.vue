<template>
  <div class="about-body">
    <div class="about-block intro-block">
      <p class="about-block-lead">
        NovaFlow AI 提供 Agent 编排、工作流、知识库 RAG、模型接入与私有化部署能力，
        帮助企业快速构建可治理、可观测的 AI 应用。
      </p>
      <div class="intro-meta">
        <span>发布日期 2026-08-31</span>
        <a-tag color="blue">正式发布</a-tag>
      </div>
    </div>

    <div class="about-block info-block">
      <div class="info-row">
        <span class="info-label">当前版本</span>
        <span class="info-value">v1.0.0</span>
      </div>
      <div class="info-row">
        <span class="info-label">产品定位</span>
        <span class="info-value">企业级 AI Agent 开发平台</span>
      </div>
      <div class="info-row">
        <span class="info-label">核心能力</span>
        <span class="info-value">Agent 编排 · 工作流 · 知识库 RAG · 私有化部署</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import './doc-styles.css';

.intro-block {
  background: linear-gradient(135deg, rgba(22, 119, 255, 0.06), rgba(14, 165, 233, 0.04));
  border-color: rgba(22, 119, 255, 0.12);
}

.intro-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}

.info-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
  font-size: 13px;
}

.info-label {
  flex-shrink: 0;
  width: 72px;
  color: var(--text-muted);
}

.info-value {
  color: var(--text-secondary);
  line-height: 1.5;
}
</style>
