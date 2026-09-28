<template>
  <div class="login-view">
    <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0" size="large" class="login-form">
      <el-form-item prop="username">
        <label class="field-label" for="login-username">用户名</label>
        <el-input id="login-username" v-model="loginForm.username" autocomplete="username"
          placeholder="请输入用户名" @keyup.enter="handleLogin" />
      </el-form-item>
      <el-form-item prop="password">
        <label class="field-label" for="login-password">密码</label>
        <el-input id="login-password" v-model="loginForm.password" type="password"
          autocomplete="current-password" placeholder="请输入密码" show-password @keyup.enter="handleLogin" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
          {{ loading ? '登录中...' : '登 录' }}
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuth } from '@/composables/useAuth'
import { normalizeError } from '@/api/errors'

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
  if (loading.value) return
  loading.value = true
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) { loading.value = false; return }
  try {
    await login({ username: loginForm.username, password: loginForm.password })
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/')
  } catch (error) {
    ElMessage.error(normalizeError(error).message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-view { width: min(400px, 100%); margin-top: 42px; }
.login-form { display: grid; gap: 8px; }
.login-form :deep(.el-form-item) { margin-bottom: 10px; }
.login-form :deep(.el-form-item__content) { display: block; }
.field-label {
  display: block;
  margin-bottom: 8px;
  color: #3e3b33;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: .05em;
}
.login-form :deep(.el-input__wrapper) {
  min-height: 48px;
  padding: 0 16px;
  background: #faf8f2;
  border: 1px solid #d9d3c3;
  border-radius: 10px;
  box-shadow: none;
}
.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #1a1915;
  box-shadow: 0 0 0 3px rgba(217, 119, 87, .16);
}
.login-form :deep(.el-input__inner) { color: #1a1915; font-size: 15px; }
.login-form :deep(.el-input__inner::placeholder) { color: #a49e8d; }
.login-form :deep(.el-input__password) { color: #75705f; }
.login-form :deep(.el-form-item__error) { color: #b23a16; }
.login-btn {
  width: 100%;
  height: 50px;
  margin-top: 2px;
  border: none;
  border-radius: 10px;
  background: #1a1915;
  color: #f6f3ea;
  font-size: 15.5px;
  font-weight: 600;
  letter-spacing: .14em;
}
.login-btn:hover, .login-btn:focus-visible { background: #b4552d; color: #fff; }
@media (max-width: 980px) { .login-view { margin-top: 30px; } }
</style>
