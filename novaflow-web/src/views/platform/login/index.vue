<template>
  <div class="auth-page auth-page--platform">
    <div class="auth-theme-toggle">
      <ThemeToggle />
    </div>

    <div class="auth-card auth-card--login" data-testid="platform-login-card">
      <div class="auth-brand">
        <span class="auth-scope-badge">Platform Admin</span>
        <AppLogo variant="auth" />
        <h2>平台治理登录</h2>
        <p class="subtitle">NovaFlow 平台运营后台专用入口</p>
      </div>

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
          进入平台后台
        </a-button>
      </a-form>

      <p v-if="!IS_PLATFORM_DEPLOY" class="auth-switch-link">
        企业用户？
        <router-link :to="tenantLoginLink">前往 Studio 登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LockOutlined, MailOutlined } from '@ant-design/icons-vue'
import { login } from '@/api/auth'
import AppLogo from '@/components/common/AppLogo.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { isPlatformAccount } from '@/config/account'
import { APP_LOGIN_PATH } from '@/config/app'
import { IS_PLATFORM_DEPLOY } from '@/config/deploy'
import { canAccessRoute, createRouteAccessContext, resolvePostLoginPath } from '@/config/access'
import { useAuthStore } from '@/stores/auth'

const REMEMBER_EMAIL_KEY = 'novaflow-login-email-platform'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const rememberMe = ref(false)
const form = reactive({ email: '', password: '' })

const tenantLoginLink = computed(() => {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('/platform')
    ? { path: APP_LOGIN_PATH, query: { redirect } }
    : APP_LOGIN_PATH
})

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
  message.info('请联系 NovaFlow 平台管理员重置密码')
}

async function onSubmit() {
  loading.value = true
  try {
    const res = await login({ ...form })
    auth.setAuth(res.data.data)
    if (!isPlatformAccount(auth.user?.accountType)) {
      auth.clear()
      message.error('该账号不是平台管理员，请使用企业 Studio 登录入口')
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
