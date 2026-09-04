<template>
  <div class="about-body">
    <div class="about-block intro-block">
      <p class="about-block-lead">
        NovaFlow AI 是企业级 AI Agent 平台，<strong>一套登录</strong>，按账号角色使用总控、Studio 或应用门户。
        v1.1 交付同站应用门户、部门组织与运营能力；Studio 与 Embed / Open API 延续 v1.0。
      </p>
      <div class="intro-meta">
        <span>发布日期 2026-09-03</span>
        <a-tag color="blue">正式发布</a-tag>
      </div>
    </div>

    <div class="about-block info-block">
      <div class="info-row">
        <span class="info-label">当前版本</span>
        <span class="info-value">v1.1.0</span>
      </div>
      <div class="info-row">
        <span class="info-label">产品定位</span>
        <span class="info-value">企业级 AI Agent 平台（单应用 · 按角色开放功能）</span>
      </div>
      <div class="info-row">
        <span class="info-label">产品形态</span>
        <span class="info-value">Studio · 应用门户 · 总控同站</span>
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
