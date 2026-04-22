<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">📒 家庭记账本</h2>
      <p class="auth-subtitle">注册新账号</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="2-20 个字符"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="可选，不填则使用用户名"
            :prefix-icon="UserFilled"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="至少 6 个字符"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item label="邀请码" prop="inviteCode">
          <el-input
            v-model="form.inviteCode"
            placeholder="请输入邀请码"
            :prefix-icon="Key"
            size="large"
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        已有账号？
        <router-link to="/login" class="auth-link">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, UserFilled, Lock, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { register } from '../api/index.js'
import { useAuth } from '../composables/useAuth.js'

const router = useRouter()
const { setAuth } = useAuth()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  inviteCode: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度需在 2-20 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  inviteCode: [
    { required: true, message: '请输入邀请码', trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      inviteCode: form.inviteCode
    })
    setAuth(res.data)
    ElMessage.success('注册成功，欢迎使用！')
    router.replace('/app')
  } catch (error) {
    const message = error.response?.data?.error || '注册失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.auth-card {
  background: white;
  border-radius: 16px;
  padding: 40px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.auth-title {
  text-align: center;
  font-size: 28px;
  margin: 0 0 8px;
  color: #333;
}

.auth-subtitle {
  text-align: center;
  color: #999;
  margin: 0 0 30px;
  font-size: 14px;
}

.auth-footer {
  text-align: center;
  margin-top: 16px;
  color: #999;
  font-size: 14px;
}

.auth-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.auth-link:hover {
  text-decoration: underline;
}
</style>
