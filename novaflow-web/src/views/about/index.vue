<template>
  <div class="about-body">
    <div class="about-block intro-block">
      <p class="about-block-lead">
        NovaFlow AI v1.0 是企业级 AI Agent 开发中台（Studio），提供 Agent 编排、工作流、
        知识库 RAG、模型接入与私有化部署能力。终端用户可通过 Embed 嵌入页或 Open API 使用已发布应用；
        独立用户前台（Portal）与独立超管端规划于 v1.2。
      </p>
      <div class="intro-meta">
        <span>发布日期 2026-08-31</span>
        <a-tag color="blue">正式发布</a-tag>
      </div>
    </div>

    <div class="about-block info-block">
      <div class="info-row">
        <span class="info-label">当前版本</span>
        <span class="info-value">v1.0.1</span>
      </div>
      <div class="info-row">
        <span class="info-label">产品定位</span>
        <span class="info-value">企业级 AI Agent 开发中台（Studio）</span>
      </div>
      <div class="info-row">
        <span class="info-label">v1.0 形态</span>
        <span class="info-value">Studio 已交付 · Portal 未纳入 · 超管同站部分交付</span>
      </div>
      <div class="info-row">
        <span class="info-label">终端接入</span>
        <span class="info-value">Embed 嵌入页 · Open API · Agent Studio 调试（开发者）</span>
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
  width: 80px;
  color: var(--text-muted);
}

.info-value {
  color: var(--text-secondary);
  line-height: 1.5;
}
</style>
