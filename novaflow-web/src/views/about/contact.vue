<template>
  <div class="about-body">
    <div class="about-block">
      <p class="about-block-lead">
        如需部署咨询、功能定制或技术支持，可通过以下渠道联系我们。工作日通常在 24 小时内回复。
      </p>
    </div>

    <div class="about-card-grid">
      <div v-for="channel in contactChannels" :key="channel.title" class="contact-card">
        <div class="contact-card-head">
          <component :is="channel.icon" class="contact-icon" />
          <div>
            <div class="contact-title">{{ channel.title }}</div>
            <div class="contact-subtitle">{{ channel.subtitle }}</div>
          </div>
        </div>
        <div v-if="channel.qrLabel" class="qr-placeholder">
          <QrcodeOutlined class="qr-icon" />
          <span>{{ channel.qrLabel }}</span>
        </div>
        <div v-if="channel.value" class="contact-value">
          <a v-if="channel.href" :href="channel.href" target="_blank" rel="noopener noreferrer">
            {{ channel.value }}
          </a>
          <span v-else>{{ channel.value }}</span>
          <a-button
            v-if="channel.copyable"
            type="link"
            size="small"
            @click="copyText(channel.value)"
          >
            复制
          </a-button>
        </div>
        <p v-if="channel.note" class="contact-note">{{ channel.note }}</p>
      </div>
    </div>

    <div class="about-block">
      <h3 class="about-block-title">开源仓库</h3>
      <div class="repo-links">
        <a
          v-for="repo in repoLinks"
          :key="repo.label"
          :href="repo.href"
          target="_blank"
          rel="noopener noreferrer"
          class="repo-link"
        >
          <component :is="repo.icon" />
          <span>{{ repo.label }}</span>
        </a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import {
  GithubOutlined,
  MailOutlined,
  QrcodeOutlined,
  WechatOutlined,
} from '@ant-design/icons-vue'

const contactChannels = [
  {
    title: '微信',
    subtitle: '扫码添加技术支持',
    icon: WechatOutlined,
    qrLabel: '微信二维码（请替换为实际图片）',
    note: '添加时请备注「NovaFlow + 企业名称」，便于快速对接。',
  },
  {
    title: 'QQ',
    subtitle: '技术交流群',
    icon: QrcodeOutlined,
    qrLabel: 'QQ 群二维码（请替换为实际图片）',
    value: '群号：待配置',
    note: '群内可交流部署、集成与最佳实践。',
  },
  {
    title: '邮箱',
    subtitle: '商务与合作',
    icon: MailOutlined,
    value: 'support@novaflow.ai',
    href: 'mailto:support@novaflow.ai',
    copyable: true,
    note: '请附上部署环境、问题描述与复现步骤，便于排查。',
  },
]

const repoLinks = [
  { label: 'GitHub', href: 'https://github.com/Yangleduo00337788/NovaFlow-AI', icon: GithubOutlined },
  { label: 'Gitee', href: 'https://gitee.com/yangleduo7788/nova-flow-ai', icon: GithubOutlined },
]

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动复制')
  }
}
</script>

<style scoped>
@import './doc-styles.css';

.contact-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--card-bg);
}

.contact-card-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.contact-icon {
  font-size: 22px;
  color: #1677ff;
  flex-shrink: 0;
}

.contact-title {
  font-size: 15px;
  font-weight: 600;
}

.contact-subtitle {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.qr-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 120px;
  border-radius: 8px;
  border: 1px dashed var(--border-strong);
  background: var(--bg-subtle);
  color: var(--text-muted);
  font-size: 12px;
  text-align: center;
  padding: 12px;
}

.qr-icon {
  font-size: 32px;
  opacity: 0.5;
}

.contact-value {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 500;
}

.contact-value a {
  color: var(--primary);
}

.contact-note {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.repo-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.repo-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--card-bg);
  color: var(--text-primary);
  text-decoration: none;
  font-size: 13px;
  transition: border-color 0.15s;
}

.repo-link:hover {
  border-color: #91caff;
  color: var(--primary);
}
</style>
