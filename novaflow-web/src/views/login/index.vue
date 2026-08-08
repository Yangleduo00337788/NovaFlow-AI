<template>
  <div class="login-page">
    <div class="login-theme">
      <ThemeToggle />
    </div>

    <section class="login-showcase">
      <header class="showcase-header">
        <AppLogo variant="sidebar" />
      </header>

      <div class="showcase-body">
        <div class="showcase-main">
          <h1>
            <span class="title-line">下一代企业级</span>
            <span class="title-line">
              <span class="title-highlight">AI Agent</span>
              开发平台
            </span>
          </h1>
          <p class="showcase-desc">
            <span class="desc-line">可视化构建、编排和部署智能 Agent，快速连接知识、工具与数据，</span>
            <span class="desc-line">让 AI 真正为业务创造价值。</span>
          </p>

          <ul class="feature-list">
            <li v-for="item in features" :key="item.title">
              <span class="feature-icon">
                <component :is="item.icon" />
              </span>
              <div class="feature-copy">
                <strong>{{ item.title }}</strong>
                <span class="feature-desc">{{ item.desc }}</span>
              </div>
            </li>
          </ul>
        </div>

        <div class="showcase-visual" aria-hidden="true">
          <div class="showcase-illustration-wrap">
            <div class="showcase-illustration-glow" />
            <img :src="loginIllustration" alt="" class="showcase-illustration" />
          </div>
        </div>
      </div>

      <footer class="showcase-footer">© 2025 NovaFlow AI. All rights reserved.</footer>
    </section>

    <section class="login-panel">
      <div class="login-card" data-testid="login-card">
        <h2>欢迎回来 👋</h2>
        <p class="subtitle">登录您的 NovaFlow AI 账户</p>

        <div class="login-tabs">
          <button
            type="button"
            class="login-tab"
            :class="{ active: activeTab === 'password' }"
            @click="activeTab = 'password'"
          >
            账号密码登录
          </button>
          <button
            type="button"
            class="login-tab"
            :class="{ active: activeTab === 'sso' }"
            @click="activeTab = 'sso'"
          >
            SSO 登录
          </button>
        </div>

        <template v-if="activeTab === 'password'">
          <a-form layout="vertical" :model="form" @finish="onSubmit">
            <a-form-item
              label="邮箱 / 用户名"
              name="email"
              :rules="[{ required: true, message: '请输入邮箱或用户名' }]"
            >
              <a-input
                v-model:value="form.email"
                placeholder="请输入邮箱或用户名"
                data-testid="login-email"
              >
                <template #prefix>
                  <MailOutlined class="field-icon" />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item
              label="密码"
              name="password"
              :rules="[{ required: true, message: '请输入密码' }]"
            >
              <a-input-password
                v-model:value="form.password"
                placeholder="请输入密码"
                data-testid="login-password"
              >
                <template #prefix>
                  <LockOutlined class="field-icon" />
                </template>
              </a-input-password>
            </a-form-item>

            <div class="forgot-row">
              <a-button type="link" class="forgot-link" @click="onForgotPassword">
                忘记密码？
              </a-button>
            </div>

            <div class="remember-row">
              <a-checkbox v-model:checked="rememberMe">记住我</a-checkbox>
            </div>

            <a-button
              type="primary"
              html-type="submit"
              block
              :loading="loading"
              class="submit-btn"
              data-testid="login-submit"
            >
              登录
            </a-button>
          </a-form>

          <div class="social-divider">
            <span>其他登录方式</span>
          </div>

          <div class="social-buttons">
            <button
              v-for="provider in socialProviders"
              :key="provider.key"
              type="button"
              class="social-btn"
              @click="onSocialLogin(provider.label)"
            >
              <component :is="provider.icon" />
              <span>{{ provider.label }}</span>
            </button>
          </div>
        </template>

        <div v-else class="sso-placeholder">
          <SafetyCertificateOutlined class="sso-placeholder__icon" />
          <p>企业 SSO 单点登录即将上线</p>
          <a-button type="link" @click="activeTab = 'password'">返回账号密码登录</a-button>
        </div>

        <p class="register-tip">
          还没有账号？
          <router-link class="register-link" to="/register">立即注册</router-link>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  GithubOutlined,
  GoogleOutlined,
  LockOutlined,
  MailOutlined,
  SafetyCertificateOutlined,
  WechatOutlined,
} from '@ant-design/icons-vue'
import { login } from '@/api/auth'
import AppLogo from '@/components/common/AppLogo.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { getMenuIcon } from '@/config/menuIcons'
import { useAuthStore } from '@/stores/auth'
import loginIllustration from '@/assets/login/login-illustration.png'

const REMEMBER_EMAIL_KEY = 'novaflow-login-email'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const activeTab = ref<'password' | 'sso'>('password')
const rememberMe = ref(false)
const form = reactive({
  email: '',
  password: '',
})

const features = [
  {
    title: '可视化开发',
    desc: '拖拽式 Agent / 工作流构建器，所见即所得',
    icon: getMenuIcon('workflow'),
  },
  {
    title: '知识驱动',
    desc: 'RAG 知识库，让 AI 掌握企业专属知识',
    icon: getMenuIcon('knowledge'),
  },
  {
    title: '多模型支持',
    desc: '灵活接入 OpenAI、DeepSeek、Qwen 等主流大模型',
    icon: getMenuIcon('model'),
  },
  {
    title: '企业级安全',
    desc: 'SSO 单点登录、RBAC 权限、操作审计',
    icon: getMenuIcon('permission'),
  },
]

const socialProviders = [
  { key: 'github', label: 'GitHub', icon: GithubOutlined },
  { key: 'wecom', label: '企业微信', icon: WechatOutlined },
  { key: 'google', label: 'Google', icon: GoogleOutlined },
]

onMounted(() => {
  const reason = route.query.reason
  if (typeof reason === 'string' && reason) {
    message.warning(reason)
  }

  const savedEmail = localStorage.getItem(REMEMBER_EMAIL_KEY)
  if (savedEmail) {
    form.email = savedEmail
    rememberMe.value = true
  }
})

function onForgotPassword() {
  message.info('请联系管理员重置密码')
}

function onSocialLogin(label: string) {
  message.info(`${label} 登录即将上线`)
}

async function onSubmit() {
  loading.value = true
  try {
    const res = await login(form)
    auth.setAuth(res.data.data)
    if (rememberMe.value) {
      localStorage.setItem(REMEMBER_EMAIL_KEY, form.email)
    } else {
      localStorage.removeItem(REMEMBER_EMAIL_KEY)
    }
    message.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.push(redirect.startsWith('/') ? redirect : '/dashboard')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(420px, 0.65fr);
  background: linear-gradient(
    90deg,
    #f8faff 0%,
    #f4f7ff 30%,
    #f0f4ff 58%,
    #ecf2ff 82%,
    #e8efff 100%
  );
  position: relative;
}

.login-theme {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 3;
}

.login-showcase {
  --showcase-feature-gap: 40px;
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-height: 100vh;
  overflow: visible;
  padding: 24px 48px 16px;
  position: relative;
  box-sizing: border-box;
}

.showcase-header {
  position: relative;
  z-index: 1;
}

.showcase-header :deep(.brand-mark) {
  margin: 0;
  justify-content: flex-start;
}

.showcase-body {
  display: grid;
  grid-template-columns: minmax(300px, 420px) minmax(0, 1fr);
  align-items: start;
  gap: 0 12px;
  flex: 1;
  min-height: 0;
  margin-top: 100px;
  position: relative;
  z-index: 1;
  overflow: visible;
}

.showcase-main {
  grid-column: 1;
  min-width: 0;
}

.showcase-body h1 {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  margin: 0 0 32px;
  max-width: 620px;
  font-size: clamp(24px, 2.4vw, 36px);
  line-height: 1.28;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.title-line {
  display: block;
}

.title-highlight {
  color: #1677ff;
}

.showcase-desc {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  margin: 0 0 var(--showcase-feature-gap);
  max-width: 560px;
  font-size: 13px;
  line-height: 1.65;
  color: #64748b;
}

.desc-line {
  display: block;
}

.feature-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: var(--showcase-feature-gap);
  max-width: 420px;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #e8f3ff;
  color: #1677ff;
  line-height: 0;
}

.feature-icon :deep(.anticon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #1677ff;
  line-height: 0;
}

.feature-icon :deep(svg) {
  display: block;
}

.feature-list strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
  line-height: 1.4;
  color: #0f172a;
}

.feature-desc {
  display: block;
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
}

.showcase-visual {
  grid-column: 2;
  grid-row: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  margin: 0 -8px 0 0;
  align-self: center;
  overflow: visible;
}

.showcase-illustration-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: min(100%, 760px);
  transform: translateX(20px) scale(1.32);
  transform-origin: center center;
}

.showcase-illustration-glow {
  display: none;
}

.showcase-illustration {
  position: relative;
  z-index: 1;
  display: block;
  width: 100%;
  max-width: 760px;
  max-height: min(84vh, 820px);
  height: auto;
  object-fit: contain;
  object-position: center center;
  filter: drop-shadow(0 18px 36px rgba(59, 130, 246, 0.14));
  animation: login-illustration-float 5.2s ease-in-out infinite;
  transform-origin: center center;
  will-change: transform;
}

@keyframes login-illustration-float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-12px) scale(1.03);
  }
}

@keyframes login-glow-pulse {
  0%, 100% {
    opacity: 0.55;
    transform: scale(0.98);
  }
  50% {
    opacity: 0.95;
    transform: scale(1.06);
  }
}

@media (prefers-reduced-motion: reduce) {
  .showcase-illustration,
  .showcase-illustration-glow {
    animation: none;
  }
}

.showcase-footer {
  position: absolute;
  left: 48px;
  bottom: 16px;
  margin: 0;
  padding: 0;
  font-size: 12px;
  color: #94a3b8;
  z-index: 1;
}

.login-panel {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  padding: 32px 40px 32px 0;
  background: transparent;
  box-sizing: border-box;
}

.login-card {
  width: 100%;
  max-width: 440px;
  height: calc(100vh - 64px);
  max-height: calc(100vh - 64px);
  overflow-y: auto;
  background: #fff;
  border-radius: 20px;
  padding: 40px 40px 32px;
  box-shadow:
    0 4px 24px rgba(15, 23, 42, 0.06),
    0 12px 48px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(238, 242, 247, 0.9);
  box-sizing: border-box;
}

.login-card h2 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
}

.subtitle {
  margin: 0 0 28px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}

.login-tabs {
  display: flex;
  gap: 32px;
  margin-bottom: 24px;
  border-bottom: 1px solid #eef2f7;
}

.login-tab {
  position: relative;
  padding: 0 0 14px;
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s ease;
}

.login-tab.active {
  color: #1677ff;
  font-weight: 600;
}

.login-tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 3px;
  border-radius: 999px;
  background: #1677ff;
}

.field-icon {
  color: #94a3b8;
}

.login-card :deep(.ant-form-item) {
  margin-bottom: 20px;
}

.login-card :deep(.ant-form-item-label > label) {
  color: #1e293b;
  font-weight: 600;
  font-size: 14px;
  height: auto;
}

.login-card :deep(.ant-form-item-label) {
  padding-bottom: 8px;
}

.login-card :deep(.ant-input-affix-wrapper),
.login-card :deep(.ant-input) {
  border-radius: 10px;
  border-color: #e2e8f0;
}

.login-card :deep(.ant-input-affix-wrapper) {
  min-height: 44px;
  padding: 8px 12px;
}

.login-card :deep(.ant-input-affix-wrapper:hover),
.login-card :deep(.ant-input-affix-wrapper-focused) {
  border-color: #1677ff;
}

.forgot-row {
  display: flex;
  justify-content: flex-end;
  margin: -10px 0 12px;
}

.remember-row {
  margin: 0 0 24px;
}

.login-card :deep(.ant-checkbox-wrapper) {
  color: #64748b;
  font-size: 14px;
}

.forgot-link {
  padding: 0;
  height: auto;
  font-size: 14px;
  color: #1677ff;
}

.register-link {
  color: #1677ff;
  font-weight: 500;
  text-decoration: none;
}

.register-link:hover {
  color: #4096ff;
}

.submit-btn {
  height: 44px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(90deg, #3b82f6 0%, #6366f1 55%, #8b5cf6 100%) !important;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.28);
}

.submit-btn:hover,
.submit-btn:focus {
  background: linear-gradient(90deg, #2563eb 0%, #4f46e5 55%, #7c3aed 100%) !important;
  border: none;
}

.social-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 44px 0 16px;
  color: #94a3b8;
  font-size: 13px;
}

.social-divider::before,
.social-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #eef2f7;
}

.social-buttons {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.social-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 40px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.social-btn :deep(.anticon) {
  font-size: 16px;
}

.social-btn:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.sso-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 280px;
  text-align: center;
  color: #64748b;
}

.sso-placeholder__icon {
  font-size: 40px;
  color: #1677ff;
}

.register-tip {
  margin: 24px 0 0;
  text-align: center;
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}

:global([data-theme='dark']) .login-page {
  background: linear-gradient(
    90deg,
    #0b1220 0%,
    #101a2e 30%,
    #141f33 60%,
    #172554 100%
  );
}

:global([data-theme='dark']) .login-panel {
  background: transparent;
}

:global([data-theme='dark']) .showcase-illustration {
  filter: drop-shadow(0 16px 32px rgba(59, 130, 246, 0.2));
  opacity: 0.96;
}

:global([data-theme='dark']) .login-card {
  background: #1a2332;
  border-color: #243044;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.28);
}

:global([data-theme='dark']) .login-card h2,
:global([data-theme='dark']) .showcase-body h1,
:global([data-theme='dark']) .feature-list strong {
  color: #f1f5f9;
}

:global([data-theme='dark']) .title-highlight {
  color: #69b1ff;
}

:global([data-theme='dark']) .showcase-desc,
:global([data-theme='dark']) .subtitle,
:global([data-theme='dark']) .feature-desc,
:global([data-theme='dark']) .register-tip,
:global([data-theme='dark']) .sso-placeholder {
  color: #94a3b8;
}

:global([data-theme='dark']) .login-tabs,
:global([data-theme='dark']) .social-divider::before,
:global([data-theme='dark']) .social-divider::after {
  border-color: #243044;
  background-color: #243044;
}

:global([data-theme='dark']) .social-btn {
  background: #141c2b;
  border-color: #334155;
  color: #cbd5e1;
}

@media (max-height: 820px) {
  .showcase-body {
    margin-top: 72px;
  }

  .showcase-illustration {
    max-height: min(58vh, 520px);
  }

  .showcase-illustration-wrap {
    transform: translateX(0) scale(1.08);
    transform-origin: center center;
  }

  .showcase-visual {
    margin-top: 0;
  }

  .login-card {
    padding: 22px 24px 18px;
  }

  .login-card h2 {
    font-size: 22px;
  }
}

@media (max-width: 1080px) {
  .login-page {
    grid-template-columns: 1fr;
    height: auto;
    max-height: none;
    overflow: auto;
  }

  .login-showcase {
    height: auto;
    max-height: none;
    overflow: visible;
  }

  .login-panel {
    height: auto;
    max-height: none;
    overflow: visible;
    justify-content: center;
  }

  .login-card {
    max-height: none;
    overflow: visible;
  }

  .showcase-footer {
    position: static;
    margin-top: 24px;
  }

  .showcase-body {
    margin-top: 80px;
    grid-template-columns: 1fr;
  }

  .showcase-main {
    grid-column: 1;
  }

  .showcase-visual {
    grid-column: 1;
    margin: 0;
  }

  .showcase-illustration-wrap {
    transform: none;
  }

  .showcase-illustration {
    max-height: 520px;
  }

  .login-panel {
    padding: 24px 20px 32px;
  }
}

@media (max-width: 640px) {
  .login-showcase {
    padding: 24px 20px 16px;
  }

  .showcase-body {
    margin-top: 24px;
  }

  .login-card {
    max-width: none;
    padding: 24px 20px 20px;
  }

  .social-buttons {
    grid-template-columns: 1fr;
  }
}
</style>
