<template>
  <div class="login-page">
    <div class="login-left">
      <AppLogo variant="login" />
      <h1>Build Intelligent Agents Faster</h1>
      <p>让企业快速构建下一代 AI 应用</p>
    </div>
    <div class="login-right">
      <div class="login-card" data-testid="login-card">
        <h2>登录</h2>
        <p class="subtitle">欢迎回来，请登录您的账号</p>
        <a-form layout="vertical" :model="form" @finish="onSubmit">
          <a-form-item label="邮箱" name="email" :rules="[{ required: true, message: '请输入邮箱' }]">
            <a-input v-model:value="form.email" placeholder="admin@novaflow.ai" size="large" data-testid="login-email" />
          </a-form-item>
          <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
            <a-input-password v-model:value="form.password" placeholder="Admin123!" size="large" data-testid="login-password" />
          </a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="loading" data-testid="login-submit">
            登录
          </a-button>
        </a-form>
        <div class="hint">演示账号：admin@novaflow.ai / Admin123!</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { login } from '@/api/auth'
import AppLogo from '@/components/common/AppLogo.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({
  email: 'admin@novaflow.ai',
  password: 'Admin123!',
})

async function onSubmit() {
  loading.value = true
  try {
    const res = await login(form)
    auth.setAuth(res.data.data)
    message.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #f8fafc;
}

.login-left {
  background: linear-gradient(135deg, #1677ff, #0f172a);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  padding: 80px;
}

.login-left h1 {
  font-size: 40px;
  margin: 0 0 16px;
}

.login-left p {
  font-size: 18px;
  opacity: 0.9;
}

.login-right {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.08);
}

.login-card h2 {
  margin: 0 0 8px;
}

.subtitle {
  color: #64748b;
  margin-bottom: 24px;
}

.hint {
  margin-top: 16px;
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
}
</style>
