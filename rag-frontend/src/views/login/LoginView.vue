<template>
  <div class="login-view">
    <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0" size="large">
      <el-form-item prop="username">
        <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="loginForm.password" type="password" placeholder="请输入密码"
          prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
          {{ loading ? '登录中...' : '登 录' }}
        </el-button>
      </el-form-item>
    </el-form>
    <div class="login-tip">默认账号：admin / admin123</div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const route = useRoute()
const { login } = useAuth()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await login({ username: loginForm.username, password: loginForm.password })
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/')
  } catch {
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-view { width: 100%; }
.login-btn { width: 100%; height: 44px; font-size: 16px; }
.login-tip { text-align: center; margin-top: 16px; font-size: 12px; color: #909399; }
</style>
