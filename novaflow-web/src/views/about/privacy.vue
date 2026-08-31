<template>
  <div class="about-body">
    <p class="about-block-note">更新日期 2026-08-31 · 供部署方与集成方参考，具体合规义务请结合所在地区法规及企业政策执行。</p>

    <section class="privacy-section">
      <h3 class="about-block-title">数据分类与存储</h3>
      <p class="about-block-lead">
        本文说明 NovaFlow AI 在数据存储、留存周期与第三方观测方面的基本做法。
      </p>
      <div class="data-table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>数据类型</th>
              <th>存储位置</th>
              <th>说明</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in storageRows" :key="row.type">
              <td>{{ row.type }}</td>
              <td>{{ row.location }}</td>
              <td>{{ row.desc }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="section-note">私有化部署时，上述组件均可部署在客户内网，数据不出客户网络（未启用第三方观测的前提下）。</p>
    </section>

    <section class="privacy-section">
      <h3 class="about-block-title">数据留存周期</h3>
      <div class="info-grid">
        <div v-for="item in retentionItems" :key="item.label" class="info-grid-item">
          <span class="info-grid-label">{{ item.label }}</span>
          <span class="info-grid-value">{{ item.value }}</span>
          <span class="info-grid-hint">{{ item.config }}</span>
        </div>
      </div>
      <p class="section-note">
        会话清理任务每日凌晨执行，可通过 <code>novaflow.conversation.retention-enabled=false</code> 关闭自动清理。
      </p>
    </section>

    <section class="privacy-section">
      <h3 class="about-block-title">第三方观测（可选）</h3>
      <p class="about-block-lead">
        平台支持通过 OpenTelemetry 上报工作流与 Agent 执行的 Span，可选对接 Langfuse 或任意兼容 OTLP HTTP 的采集端。
      </p>
      <div class="bullet-list">
        <div v-for="item in observabilityNotes" :key="item.label" class="bullet-item">
          <strong>{{ item.label }}</strong>
          <span>{{ item.desc }}</span>
        </div>
      </div>
    </section>

    <section class="privacy-section">
      <h3 class="about-block-title">Open API 与终端用户数据</h3>
      <div class="about-card-grid">
        <div v-for="item in openApiItems" :key="item.title" class="mini-card">
          <div class="mini-card-title">{{ item.title }}</div>
          <div class="mini-card-desc">{{ item.desc }}</div>
        </div>
      </div>
      <div class="tips-box">
        <div class="tips-title">集成建议</div>
        <ul>
          <li><code>X-Caller-Id</code> 请使用贵方系统内的匿名用户 ID（如内部 UUID），避免传入手机号、邮箱等可直接识别个人的标识。</li>
          <li>Embed Token 应通过服务端下发或短期授权，避免硬编码在前端公开仓库中。</li>
        </ul>
      </div>
    </section>

    <section class="privacy-section">
      <h3 class="about-block-title">审计日志范围</h3>
      <p class="about-block-lead">以下操作会写入审计日志，可在「审计日志」页面查询：</p>
      <ul class="plain-list">
        <li v-for="item in auditScopes" :key="item">{{ item }}</li>
      </ul>
      <p class="section-note">审计记录包含：时间、租户、用户、动作、资源类型、资源 ID、摘要、客户端 IP。</p>
    </section>

    <section class="privacy-section">
      <h3 class="about-block-title">部署方安全建议</h3>
      <ol class="numbered-list">
        <li v-for="item in securityTips" :key="item">{{ item }}</li>
      </ol>
    </section>
  </div>
</template>

<script setup lang="ts">
const storageRows = [
  { type: '用户账号与组织信息', location: 'MySQL', desc: '邮箱、昵称、角色、企业信息等' },
  { type: '登录会话', location: 'Redis（Sa-Token）', desc: 'Token 有效期默认 24 小时' },
  { type: '业务配置', location: 'MySQL', desc: 'Agent、工作流、知识库元数据、模型配置等' },
  { type: '模型提供商 API Key', location: 'MySQL（AES 加密）', desc: '依赖环境变量 NOVAFLOW_CRYPTO_KEY' },
  { type: 'Agent API Key / Embed Token', location: 'MySQL（SHA-256 哈希）', desc: '明文仅在创建/轮换时展示一次' },
  { type: '对话与消息', location: 'MySQL', desc: '含用户输入、模型回复、Token 用量等' },
  { type: '知识库文档', location: 'MinIO + Qdrant', desc: '原始文件与向量索引' },
  { type: '调用日志与链路追踪', location: 'MySQL', desc: 'Token 消耗、耗时、Trace 信息等' },
  { type: '审计日志', location: 'MySQL', desc: '登录、成员变更、资源删除等关键操作' },
]

const retentionItems = [
  { label: '对话与消息', value: '90 天后自动清理', config: 'novaflow.conversation.retention-days' },
  { label: '登录会话', value: 'Token 过期即失效', config: 'sa-token.timeout（默认 86400 秒）' },
  { label: '审计日志', value: '长期保留', config: '由运维制定归档/清理策略' },
  { label: '调用日志 / 链路数据', value: '长期保留', config: '由运维制定归档策略' },
]

const observabilityNotes = [
  { label: '默认状态', desc: '关闭（OTEL_ENABLED=false）' },
  { label: '可能上报内容', desc: '执行耗时、Trace ID、节点名称；具体字段取决于集成配置' },
  { label: '风险提示', desc: '若 Span 属性包含 Prompt/回复片段，数据将离开本系统边界' },
  { label: '建议', desc: '生产环境优先使用自建 OTLP 采集器；对接云服务前签署数据处理协议' },
]

const openApiItems = [
  { title: 'nf_live_ API Key', desc: '服务端集成，可访问会话列表与历史消息。' },
  { title: 'nf_embed_ Embed Token', desc: '仅允许对话与欢迎语，禁止拉取会话列表/消息。' },
  { title: 'X-Caller-Id', desc: '调用方必须传入，用于隔离不同终端用户的会话。' },
]

const auditScopes = [
  '认证：登录成功、登录失败、登出、企业注册',
  '组织：企业信息更新、工作空间增删改、成员邀请/更新/移除',
  '资源删除：应用、Agent、工作流、知识库、文档、Prompt、工具、MCP、模型提供商/配置',
  'Agent 发布：发布、下线、轮换 API Key / Embed Token',
  '平台管理：租户创建/更新/停用（平台管理员）',
]

const securityTips = [
  '生产环境使用 spring.profiles.active=prod，并设置强随机 NOVAFLOW_CRYPTO_KEY（≥ 32 字符）。',
  '修改所有默认密码（MySQL、Redis、MinIO、演示管理员账号）。',
  'CORS_ALLOWED_ORIGIN 仅允许实际前端域名。',
  '对外仅暴露 Web（Nginx）端口，数据库与 Redis 不映射公网。',
  '定期备份 MySQL 与 MinIO，并限制审计日志与数据库的访问权限。',
  '未使用第三方观测时，保持 OTEL_ENABLED=false。',
]
</script>

<style scoped>
@import './doc-styles.css';

.privacy-section + .privacy-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

.section-note {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.data-table-wrap {
  overflow-x: auto;
  margin-top: 10px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th,
.data-table td {
  padding: 10px 12px;
  border: 1px solid var(--border);
  text-align: left;
  vertical-align: top;
}

.data-table th {
  background: var(--bg-subtle);
  font-weight: 600;
}

.info-grid {
  margin-top: 10px;
}

.info-grid-hint {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}

.bullet-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.bullet-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-subtle);
  font-size: 13px;
}

.bullet-item strong {
  color: var(--text-primary);
}

.bullet-item span {
  color: var(--text-secondary);
  line-height: 1.6;
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

.tips-box {
  margin-top: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px dashed var(--border-strong);
  background: var(--bg-subtle);
}

.tips-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.tips-box ul,
.plain-list {
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary);
  line-height: 1.7;
  font-size: 13px;
}

.numbered-list {
  margin: 10px 0 0;
  padding-left: 20px;
  color: var(--text-secondary);
  line-height: 1.8;
  font-size: 13px;
}

code {
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--bg-subtle);
  font-size: 12px;
}
</style>
