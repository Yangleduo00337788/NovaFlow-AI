<template>
  <div class="about-body">
    <div class="about-block">
      <p class="about-block-lead">
        感谢您帮助改进 NovaFlow AI。提交问题时请尽量描述复现步骤、期望行为与实际结果，
        并附上相关截图或日志（注意脱敏）。
      </p>
    </div>

    <div class="about-card-grid">
      <a
        v-for="item in reportChannels"
        :key="item.label"
        :href="item.href"
        target="_blank"
        rel="noopener noreferrer"
        class="about-link-card"
      >
        <component :is="item.icon" class="report-icon" />
        <span class="report-title">{{ item.label }}</span>
        <span class="report-desc">{{ item.desc }}</span>
        <span class="report-action">前往提交 →</span>
      </a>
    </div>

    <div class="about-block">
      <h3 class="about-block-title">提交前建议</h3>
      <ul class="tips-list">
        <li v-for="tip in submitTips" :key="tip">{{ tip }}</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { BugOutlined, GithubOutlined, MessageOutlined } from '@ant-design/icons-vue'

const reportChannels = [
  {
    label: 'GitHub Issues',
    desc: '推荐：面向开源用户与开发者，便于跟踪修复进度',
    href: 'https://github.com/Yangleduo00337788/NovaFlow-AI/issues/new',
    icon: GithubOutlined,
  },
  {
    label: 'Gitee Issues',
    desc: '国内访问更稳定，适合 Gitee 用户提交反馈',
    href: 'https://gitee.com/yangleduo7788/nova-flow-ai/issues',
    icon: MessageOutlined,
  },
  {
    label: '邮件反馈',
    desc: '企业私有化部署或不便公开的问题，可邮件联系',
    href: 'mailto:support@novaflow.ai?subject=NovaFlow%20问题反馈',
    icon: BugOutlined,
  },
]

const submitTips = [
  '说明使用的版本号（可在「更新日志」查看）',
  '描述操作路径：从哪个页面、执行了哪些步骤',
  '附上浏览器控制台或后端日志中的关键报错（隐藏 Token、密钥等敏感信息）',
  '若是功能建议，请说明业务场景与期望效果',
]
</script>

<style scoped>
@import './doc-styles.css';

.report-icon {
  font-size: 22px;
  color: #1677ff;
}

.report-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.report-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  flex: 1;
}

.report-action {
  font-size: 13px;
  color: var(--primary);
  font-weight: 500;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  color: var(--text-secondary);
  line-height: 1.75;
  font-size: 14px;
}
</style>
