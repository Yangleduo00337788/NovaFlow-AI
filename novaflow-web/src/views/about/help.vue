<template>
  <div class="about-body">
    <section v-for="section in helpSections" :key="section.title" class="help-section">
      <h3 class="about-block-title">{{ section.title }}</h3>
      <p v-if="section.desc" class="about-block-note">{{ section.desc }}</p>
      <div class="help-links">
        <component
          :is="link.external ? 'a' : 'router-link'"
          v-for="link in section.links"
          :key="link.label"
          :href="link.external ? link.href : undefined"
          :to="link.external ? undefined : link.href"
          class="help-link-item"
          :target="link.external ? '_blank' : undefined"
          :rel="link.external ? 'noopener noreferrer' : undefined"
        >
          <span class="help-link-label">{{ link.label }}</span>
          <span class="help-link-hint">{{ link.hint }}</span>
        </component>
      </div>
    </section>

    <section class="help-section">
      <h3 class="about-block-title">常见问题</h3>
      <div class="faq-list">
        <div v-for="item in faqItems" :key="item.q" class="faq-item">
          <div class="faq-q">{{ item.q }}</div>
          <div class="faq-a">{{ item.a }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
const repoBase = 'https://github.com/Yangleduo00337788/NovaFlow-AI'

const helpSections = [
  {
    title: '产品文档',
    desc: '仓库内 Markdown 文档，涵盖产品设计、架构与数据库说明。',
    links: [
      { label: '产品需求文档（PRD）', hint: '功能设计与用户场景', href: `${repoBase}/blob/main/docs/PRD.md`, external: true },
      { label: '系统架构设计', hint: '模块拆分、核心流程与部署', href: `${repoBase}/blob/main/docs/系统架构设计.md`, external: true },
      { label: '数据库设计', hint: '表结构与 ER 关系', href: `${repoBase}/blob/main/docs/数据库设计.md`, external: true },
    ],
  },
  {
    title: '部署与运维',
    desc: '私有化部署、环境变量与安全配置说明。',
    links: [
      { label: 'README · 快速开始', hint: 'Docker Compose 一键启动', href: `${repoBase}#readme`, external: true },
      { label: '安全与隐私', hint: '数据留存与 Open API 建议', href: '/about/privacy', external: false },
      { label: 'Swagger API 文档', hint: '启动服务后访问 /swagger-ui.html', href: '/dashboard', external: false },
    ],
  },
  {
    title: '开发指南',
    desc: 'Agent、工作流与知识库的核心使用路径。',
    links: [
      { label: 'Agent Studio', hint: '创建、调试与发布 Agent', href: '/agent', external: false },
      { label: '工作流 Studio', hint: '可视化编排与执行追踪', href: '/workflow', external: false },
      { label: '知识库 Hub', hint: '文档上传与 RAG 检索', href: '/knowledge', external: false },
    ],
  },
]

const faqItems = [
  {
    q: '忘记密码怎么办？',
    a: '请联系企业管理员在「组织管理」中重置，或由部署方运维处理。',
  },
  {
    q: '如何对外提供 Agent 对话能力？',
    a: '在 Agent Studio 中发布 Agent，获取 API Key 或 Embed Token，并按要求传入 X-Caller-Id 隔离终端用户。',
  },
  {
    q: '模型 API Key 存在哪里？',
    a: '存储在数据库中并使用 AES 加密，生产环境需配置 NOVAFLOW_CRYPTO_KEY 环境变量。',
  },
  {
    q: '对话记录会保留多久？',
    a: '默认 90 天后自动清理，可通过 novaflow.conversation.retention-days 调整。',
  },
]
</script>

<style scoped>
@import './doc-styles.css';

.help-section + .help-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

.help-links {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.help-link-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-subtle);
  text-decoration: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.help-link-item:hover {
  border-color: #91caff;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.06);
}

.help-link-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary);
}

.help-link-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.faq-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 10px;
}

.faq-item {
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-subtle);
}

.faq-q {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
}

.faq-a {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>
