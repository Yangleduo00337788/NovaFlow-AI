<template>
  <div class="register-page">
    <div class="register-theme">
      <ThemeToggle />
    </div>

    <section class="register-showcase">
      <header class="showcase-header">
        <AppLogo variant="sidebar" />
      </header>

      <div class="showcase-body">
        <div class="showcase-main">
          <h1>
            <span class="title-line">开启您的</span>
            <span class="title-line">
              <span class="title-highlight">AI 应用</span>
              之旅
            </span>
          </h1>
          <p class="showcase-desc">
            <span class="desc-line">注册 NovaFlow AI 账户，快速创建企业专属 AI Agent，</span>
            <span class="desc-line">连接知识库与工具，让智能应用即刻上线。</span>
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
      </div>

      <footer class="showcase-footer">© 2025 NovaFlow AI. All rights reserved.</footer>
    </section>

    <section class="register-panel">
      <div class="register-card" data-testid="register-card">
        <h2>创建账户</h2>
        <p class="subtitle">注册 NovaFlow AI，开始构建智能应用</p>

        <a-form layout="vertical" :model="form" @finish="onSubmit">
          <a-form-item
            label="企业名称"
            name="companyName"
            :rules="[{ required: true, message: '请输入企业名称' }]"
          >
            <a-input
              v-model:value="form.companyName"
              placeholder="您的公司或团队名称"
              data-testid="register-company"
            >
              <template #prefix>
                <BankOutlined class="field-icon" />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item
            label="邮箱"
            name="email"
            :rules="[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '邮箱格式不正确' },
            ]"
          >
            <a-input
              v-model:value="form.email"
              placeholder="name@company.com"
              data-testid="register-email"
            >
              <template #prefix>
                <MailOutlined class="field-icon" />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item label="昵称（可选）" name="nickname">
            <a-input v-model:value="form.nickname" placeholder="显示名称" data-testid="register-nickname">
              <template #prefix>
                <UserOutlined class="field-icon" />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item
            label="密码"
            name="password"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码至少 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="form.password"
              placeholder="至少 8 位，包含字母和数字"
              data-testid="register-password"
            >
              <template #prefix>
                <LockOutlined class="field-icon" />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item
            label="确认密码"
            name="confirmPassword"
            :rules="[
              { required: true, message: '请确认密码' },
              { validator: validateConfirmPassword },
            ]"
          >
            <a-input-password
              v-model:value="form.confirmPassword"
              placeholder="再次输入密码"
              data-testid="register-confirm-password"
            >
              <template #prefix>
                <LockOutlined class="field-icon" />
              </template>
            </a-input-password>
          </a-form-item>

          <a-button
            type="primary"
            html-type="submit"
            block
            :loading="loading"
            class="submit-btn"
            data-testid="register-submit"
          >
            注册并登录
          </a-button>
        </a-form>

        <p class="login-tip">
          已有账号？
          <router-link class="login-link" to="/login">立即登录</router-link>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import {
  BankOutlined,
  LockOutlined,
  MailOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { register } from '@/api/auth'
import AppLogo from '@/components/common/AppLogo.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { getMenuIcon } from '@/config/menuIcons'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({
  companyName: '',
  email: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const features = [
  {
    title: '免费试用',
    desc: '注册即享免费版套餐，快速体验核心能力',
    icon: getMenuIcon('dashboard'),
  },
  {
    title: '独立租户',
    desc: '自动创建企业与工作空间，数据完全隔离',
    icon: getMenuIcon('org'),
  },
  {
    title: '开箱即用',
    desc: '注册后可直接创建 Agent、知识库与模型配置',
    icon: getMenuIcon('agent'),
  },
]

const validateConfirmPassword = async (_rule: Rule, value: string) => {
  if (!value) {
    return Promise.reject('请确认密码')
  }
  if (value !== form.password) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

async function onSubmit() {
  loading.value = true
  try {
    const res = await register(form)
    auth.setAuth(res.data.data)
    message.success('注册成功，欢迎加入 NovaFlow AI')
    router.push('/dashboard')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(420px, 480px);
  min-height: 100vh;
  background: var(--bg);
}

.register-theme {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10;
}

.register-showcase {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: 40px 56px;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  color: #fff;
}

.showcase-header {
  margin-bottom: 48px;
}

.showcase-body {
  flex: 1;
  display: flex;
  align-items: center;
}

.showcase-main h1 {
  font-size: 42px;
  font-weight: 700;
  line-height: 1.25;
  margin: 0 0 20px;
}

.title-line {
  display: block;
}

.title-highlight {
  color: #60a5fa;
}

.showcase-desc {
  font-size: 16px;
  line-height: 1.7;
  color: #94a3b8;
  margin: 0 0 40px;
  max-width: 480px;
}

.desc-line {
  display: block;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-list li {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.feature-copy strong {
  display: block;
  font-size: 15px;
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 13px;
  color: #94a3b8;
}

.showcase-footer {
  font-size: 12px;
  color: #64748b;
  margin-top: 40px;
}

.register-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 32px;
  background: var(--bg);
}

.register-card {
  width: 100%;
  max-width: 400px;
}

.register-card h2 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--text-primary);
}

.subtitle {
  color: var(--text-secondary);
  margin: 0 0 28px;
  font-size: 14px;
}

.field-icon {
  color: var(--text-muted);
}

.submit-btn {
  height: 44px;
  margin-top: 8px;
  font-weight: 600;
}

.login-tip {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-secondary);
}

.login-link {
  color: var(--primary);
  font-weight: 500;
}

@media (max-width: 960px) {
  .register-page {
    grid-template-columns: 1fr;
  }

  .register-showcase {
    display: none;
  }
}
</style>
