<template>
  <div class="auth-page">
    <div class="auth-theme-toggle">
      <ThemeToggle />
    </div>

    <div class="auth-card auth-card--login" data-testid="login-card">
      <div class="auth-tabs">
        <button
          type="button"
          class="auth-tab"
          :class="{ active: activeTab === 'password' }"
          @click="activeTab = 'password'"
        >
          账号密码登录
        </button>
        <button
          type="button"
          class="auth-tab"
          :class="{ active: activeTab === 'sso' }"
          @click="activeTab = 'sso'"
        >
          SSO 登录
        </button>
      </div>

      <div class="auth-brand">
        <AppLogo variant="auth" />
        <h2>欢迎回来 👋</h2>
        <p class="subtitle">登录 NovaFlow AI Studio</p>
      </div>

      <template v-if="activeTab === 'password'">
        <a-form layout="vertical" :model="form" @finish="onSubmit">
          <a-form-item name="email" :rules="[{ required: true, message: '请输入邮箱或用户名' }]">
            <a-input v-model:value="form.email" placeholder="邮箱 / 用户名" data-testid="login-email">
              <template #prefix>
                <MailOutlined class="auth-field-icon" />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="password" :rules="[{ required: true, message: '请输入密码' }]">
            <a-input-password v-model:value="form.password" placeholder="请输入密码" data-testid="login-password">
              <template #prefix>
                <LockOutlined class="auth-field-icon" />
              </template>
            </a-input-password>
          </a-form-item>

          <div class="auth-form-meta">
            <a-checkbox v-model:checked="rememberMe">记住我</a-checkbox>
            <a-button type="link" @click="onForgotPassword">忘记密码？</a-button>
          </div>

          <a-button
            type="primary"
            html-type="submit"
            block
            :loading="loading"
            class="auth-submit-btn"
            data-testid="login-submit"
          >
            登录
          </a-button>
        </a-form>

        <div class="auth-social-divider">其他登录方式</div>

        <div class="auth-social-buttons">
          <button
            v-for="provider in socialProviders"
            :key="provider.key"
            type="button"
            class="auth-social-btn"
            :title="provider.label"
            @click="onSocialLogin(provider.label)"
          >
            <component :is="provider.icon" />
          </button>
        </div>
      </template>

      <div v-else class="auth-sso-placeholder">
        <SafetyCertificateOutlined class="auth-sso-placeholder__icon" />
        <p>企业 SSO（OAuth2/OIDC）计划在后续版本提供，当前请使用邮箱密码登录</p>
        <a-button type="link" @click="activeTab = 'password'">返回账号密码登录</a-button>
      </div>

      <p class="auth-footer-tip">
        还没有账号？
        <router-link class="auth-footer-link" to="/register">立即注册</router-link>
      </p>

      <p class="auth-switch-link">
        平台管理员？
        <router-link :to="platformLoginLink">前往平台治理登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
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
import { isPlatformAccount } from '@/config/account'
import { PLATFORM_LOGIN_PATH } from '@/config/app'
import { canAccessRoute, createRouteAccessContext, resolvePostLoginPath } from '@/config/access'
import { useAuthStore } from '@/stores/auth'

const REMEMBER_EMAIL_KEY = 'novaflow-login-email-tenant'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const activeTab = ref<'password' | 'sso'>('password')
const rememberMe = ref(false)
const form = reactive({ email: '', password: '' })

const platformLoginLink = computed(() => PLATFORM_LOGIN_PATH)

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
    const res = await login({ ...form })
    auth.setAuth(res.data.data)
    if (isPlatformAccount(auth.user?.accountType)) {
      auth.clear()
      message.warning('平台管理员请使用平台治理登录入口')
      router.push(PLATFORM_LOGIN_PATH)
      return
    }
    if (rememberMe.value) {
      localStorage.setItem(REMEMBER_EMAIL_KEY, form.email)
    } else {
      localStorage.removeItem(REMEMBER_EMAIL_KEY)
    }
    message.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : undefined
    router.push(
      resolvePostLoginPath(
        auth.user?.accountType,
        auth.roleCode,
        redirect,
        (path) => canAccessRoute(path, createRouteAccessContext(auth)),
      ),
    )
  } catch (e) {
    message.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>
