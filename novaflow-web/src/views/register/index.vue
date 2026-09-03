<template>
  <div class="auth-page auth-page--scroll">
    <div class="auth-theme-toggle">
      <ThemeToggle />
    </div>

    <div class="auth-card auth-card--register" data-testid="register-card">
      <div class="auth-brand">
        <AppLogo variant="auth" />
        <h2>创建账户</h2>
        <p class="subtitle">注册 NovaFlow AI，开始构建智能应用</p>
      </div>

      <a-form layout="vertical" :model="form" @finish="onSubmit">
        <a-form-item label="选择套餐" name="planType">
          <div class="auth-plan-picker">
            <div
              v-for="plan in planCards"
              :key="plan.value"
              class="auth-plan-card"
              :class="{ active: form.planType === plan.value }"
              data-testid="register-plan-card"
              @click="form.planType = plan.value"
            >
              <div class="auth-plan-head">
                <component :is="plan.icon" class="auth-plan-icon" />
                <strong>{{ plan.title }}</strong>
              </div>
              <span class="auth-plan-desc">{{ plan.desc }}</span>
              <ul class="auth-plan-perks">
                <li v-for="perk in plan.perks" :key="perk">{{ perk }}</li>
              </ul>
            </div>
          </div>
        </a-form-item>

        <a-form-item
          label="企业 / 个人名称"
          name="companyName"
          :rules="[{ required: true, message: '请输入企业或个人名称' }]"
        >
          <a-input
            v-model:value="form.companyName"
            placeholder="您的公司、团队或个人名称"
            data-testid="register-company"
          >
            <template #prefix>
              <BankOutlined class="auth-field-icon" />
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
              <MailOutlined class="auth-field-icon" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item label="昵称（可选）" name="nickname">
          <a-input v-model:value="form.nickname" placeholder="显示名称" data-testid="register-nickname">
            <template #prefix>
              <UserOutlined class="auth-field-icon" />
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
              <LockOutlined class="auth-field-icon" />
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
              <LockOutlined class="auth-field-icon" />
            </template>
          </a-input-password>
        </a-form-item>

        <a-button
          type="primary"
          html-type="submit"
          block
          :loading="loading"
          class="auth-submit-btn"
          data-testid="register-submit"
        >
          注册并登录
        </a-button>
      </a-form>

      <p class="auth-footer-tip">
        已有账号？
        <router-link class="auth-footer-link" to="/login">立即登录</router-link>
      </p>
    </div>
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
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { register } from '@/api/auth'
import AppLogo from '@/components/common/AppLogo.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({
  planType: 'enterprise' as 'personal' | 'enterprise',
  companyName: '',
  email: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const planCards = [
  {
    value: 'personal' as const,
    icon: UserOutlined,
    title: '个人版',
    desc: '个人开发者与轻量使用',
    perks: ['1 名成员', '3 个 Agent', '1 个知识库', '每月 2 万 Token'],
  },
  {
    value: 'enterprise' as const,
    icon: TeamOutlined,
    title: '企业版（免费试用）',
    desc: '团队协作与生产部署',
    perks: ['10 名成员', '5 个 Agent', '3 个知识库', '每月 10 万 Token'],
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
    message.success(form.planType === 'personal' ? '注册成功，欢迎加入 NovaFlow AI（个人版）' : '注册成功，欢迎加入 NovaFlow AI')
    router.push('/dashboard')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-submit-btn {
  margin-top: 8px;
}
</style>
