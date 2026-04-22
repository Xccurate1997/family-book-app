<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">📒 家庭记账本</h2>
      <p class="auth-subtitle">登录你的账号</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        还没有账号？
        <router-link to="/register" class="auth-link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login } from '../api/index.js'
import { useAuth } from '../composables/useAuth.js'

const router = useRouter()
const { setAuth } = useAuth()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form.username, form.password)
    setAuth(res.data)
    ElMessage.success(`欢迎回来，${res.data.nickname}！`)
    const target = res.data.role === 'ADMIN' ? '/admin' : '/app'
    router.replace(target)
  } catch (error) {
    const message = error.response?.data?.error || '登录失败，请检查用户名和密码'
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
