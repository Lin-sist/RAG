<!--
  UserProfile.vue - 个人资料页面
  复用现有主题系统 (html.dark class toggle)
-->
<template>
  <div class="user-profile-page">
    <!-- Header -->
    <header class="profile-header">
      <div class="header-left">
        <div class="header-avatar">
          {{ userInitial }}
        </div>
        <h1 class="header-title">个人资料</h1>
      </div>
      <div class="header-right">
        <!-- 主题切换按钮 -->
        <div class="theme-toggle-group">
          <button
            :class="['theme-btn', { active: !isDark }]"
            @click="setTheme(false)"
            title="亮色模式"
          >
            <Sun :size="16" />
          </button>
          <button
            :class="['theme-btn', { active: isDark }]"
            @click="setTheme(true)"
            title="暗色模式"
          >
            <Moon :size="16" />
          </button>
        </div>
      </div>
    </header>

    <!-- Content -->
    <div class="profile-content">
      <!-- Basic Info Card -->
      <div class="profile-card">
        <h2 class="card-title">基本信息</h2>
        <div class="card-body basic-info">
          <div class="avatar-section">
            <div class="large-avatar">
              {{ userInitial }}
            </div>
            <button class="change-avatar-btn">更换头像</button>
          </div>
          <div class="info-form">
            <div class="form-group">
              <label class="form-label">用户名</label>
              <input
                v-model="user.name"
                type="text"
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label class="form-label">邮箱</label>
              <div class="input-with-icon">
                <input
                  v-model="user.email"
                  type="email"
                  class="form-input"
                  readonly
                />
                <Lock :size="16" class="input-icon" />
              </div>
            </div>
            <button class="primary-btn" @click="handleSaveBasicInfo">
              保存修改
            </button>
          </div>
        </div>
      </div>

      <!-- Change Password Card -->
      <div class="profile-card">
        <h2 class="card-title">修改密码</h2>
        <div class="card-body password-form">
          <div class="form-group">
            <label class="form-label">当前密码</label>
            <div class="input-with-icon">
              <input
                v-model="passwords.current"
                :type="showCurrentPassword ? 'text' : 'password'"
                class="form-input"
              />
              <button
                class="toggle-visibility-btn"
                @click="showCurrentPassword = !showCurrentPassword"
              >
                <Eye v-if="showCurrentPassword" :size="16" />
                <EyeOff v-else :size="16" />
              </button>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">新密码</label>
            <div class="input-with-icon">
              <input
                v-model="passwords.new"
                :type="showNewPassword ? 'text' : 'password'"
                class="form-input"
              />
              <button
                class="toggle-visibility-btn"
                @click="showNewPassword = !showNewPassword"
              >
                <Eye v-if="showNewPassword" :size="16" />
                <EyeOff v-else :size="16" />
              </button>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">确认新密码</label>
            <div class="input-with-icon">
              <input
                v-model="passwords.confirm"
                :type="showConfirmPassword ? 'text' : 'password'"
                class="form-input"
              />
              <button
                class="toggle-visibility-btn"
                @click="showConfirmPassword = !showConfirmPassword"
              >
                <Eye v-if="showConfirmPassword" :size="16" />
                <EyeOff v-else :size="16" />
              </button>
            </div>
          </div>
          <button class="secondary-btn" @click="handleUpdatePassword">
            更新密码
          </button>
        </div>
      </div>

      <!-- API Key Card -->
      <div class="profile-card">
        <h2 class="card-title">API 密钥</h2>
        <div class="card-body api-key-section">
          <div class="form-group">
            <label class="form-label">当前 API 密钥</label>
            <div class="api-key-input-wrapper">
              <input
                :value="showApiKey ? user.apiKey : maskedApiKey"
                type="text"
                class="form-input api-key-input"
                readonly
              />
              <div class="api-key-actions">
                <button
                  class="icon-btn"
                  @click="showApiKey = !showApiKey"
                  :title="showApiKey ? '隐藏' : '显示'"
                >
                  <Eye v-if="showApiKey" :size="16" />
                  <EyeOff v-else :size="16" />
                </button>
                <button
                  class="icon-btn"
                  @click="handleCopyApiKey"
                  title="复制"
                >
                  <Copy :size="16" />
                </button>
              </div>
            </div>
          </div>
          <div class="api-key-warning">
            <AlertTriangle :size="16" />
            <span>重新生成后旧密钥立即失效，请谨慎操作</span>
          </div>
          <button class="regenerate-btn" @click="handleRegenerateApiKey">
            <RefreshCw :size="16" />
            重新生成密钥
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  Sun,
  Moon,
  Lock,
  Eye,
  EyeOff,
  Copy,
  AlertTriangle,
  RefreshCw
} from 'lucide-vue-next'

// Theme state
const isDark = ref(false)

// Visibility toggles
const showCurrentPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const showApiKey = ref(false)

// Mock user data
const user = ref({
  name: 'AI_User_123',
  email: 'user@example.com',
  avatar: null,
  apiKey: 'sk-rag-xxxxxxxxxxxxxxxxxxxx'
})

// Password form
const passwords = ref({
  current: '',
  new: '',
  confirm: ''
})

// Computed
const userInitial = computed(() => {
  return user.value.name.charAt(0).toUpperCase()
})

const maskedApiKey = computed(() => {
  const key = user.value.apiKey
  if (key.length <= 8) return key
  return key.slice(0, 3) + '-' + '*'.repeat(24)
})

// Theme functions
function applyTheme(dark: boolean) {
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.classList.toggle('light', !dark)
}

function setTheme(dark: boolean) {
  isDark.value = dark
  applyTheme(dark)
  localStorage.setItem('theme', dark ? 'dark' : 'light')
}

// Action handlers
function handleSaveBasicInfo() {
  console.log('save basic info', {
    name: user.value.name,
    email: user.value.email
  })
}

function handleUpdatePassword() {
  console.log('update password', {
    current: passwords.value.current,
    new: passwords.value.new,
    confirm: passwords.value.confirm
  })
}

async function handleCopyApiKey() {
  try {
    await navigator.clipboard.writeText(user.value.apiKey)
    console.log('API key copied')
  } catch (err) {
    console.error('Failed to copy API key:', err)
  }
}

function handleRegenerateApiKey() {
  console.log('regenerate API key')
  // Generate a mock new key
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789'
  let newKey = 'sk-rag-'
  for (let i = 0; i < 20; i++) {
    newKey += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  user.value.apiKey = newKey
}

onMounted(() => {
  // Restore theme from localStorage
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDark.value = true
  } else if (savedTheme === 'light') {
    isDark.value = false
  } else {
    isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  applyTheme(isDark.value)
})
</script>

<style scoped>
.user-profile-page {
  min-height: 100vh;
  background: var(--rag-bg-page);
  padding: 24px 48px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* ========== Header ========== */
.profile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: var(--rag-bg-surface);
  border-radius: 12px;
  margin-bottom: 32px;
  box-shadow: var(--rag-shadow-sm);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-avatar {
  width: 40px;
  height: 40px;
  background: var(--rag-bg-hover);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: var(--rag-text-primary);
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--rag-text-primary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* Theme Toggle Group */
.theme-toggle-group {
  display: flex;
  align-items: center;
  background: var(--rag-bg-hover);
  border-radius: 8px;
  padding: 4px;
}

.theme-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rag-text-secondary);
  transition: all 0.2s ease;
}

.theme-btn:hover {
  color: var(--rag-text-primary);
}

.theme-btn.active {
  background: var(--rag-bg-surface);
  color: var(--rag-text-primary);
  box-shadow: var(--rag-shadow-sm);
}

/* ========== Content ========== */
.profile-content {
  max-width: 560px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ========== Card ========== */
.profile-card {
  background: var(--rag-bg-surface);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--rag-shadow-sm);
  border: 1px solid var(--rag-border);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--rag-text-primary);
  margin: 0 0 20px;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ========== Basic Info ========== */
.basic-info {
  flex-direction: row;
  gap: 32px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.large-avatar {
  width: 80px;
  height: 80px;
  background: var(--rag-bg-hover);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 600;
  color: var(--rag-text-primary);
}

.change-avatar-btn {
  padding: 6px 12px;
  background: var(--rag-bg-hover);
  border: 1px solid var(--rag-border);
  border-radius: 6px;
  font-size: 12px;
  color: var(--rag-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.change-avatar-btn:hover {
  background: var(--rag-bg-surface);
  border-color: var(--rag-text-secondary);
}

.info-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ========== Form Elements ========== */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--rag-text-secondary);
}

.form-input {
  padding: 10px 12px;
  background: var(--rag-bg-page);
  border: 1px solid var(--rag-border);
  border-radius: 8px;
  font-size: 14px;
  color: var(--rag-text-primary);
  outline: none;
  transition: border-color 0.2s ease;
}

.form-input:focus {
  border-color: var(--rag-primary);
}

.form-input[readonly] {
  cursor: default;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-icon .form-input {
  width: 100%;
  padding-right: 40px;
}

.input-icon {
  position: absolute;
  right: 12px;
  color: var(--rag-text-placeholder);
}

.toggle-visibility-btn {
  position: absolute;
  right: 8px;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rag-text-secondary);
  transition: color 0.2s ease;
}

.toggle-visibility-btn:hover {
  color: var(--rag-text-primary);
}

/* ========== Buttons ========== */
.primary-btn {
  align-self: flex-end;
  padding: 8px 16px;
  background: var(--rag-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.primary-btn:hover {
  background: var(--rag-primary-hover);
}

.secondary-btn {
  align-self: flex-end;
  padding: 8px 16px;
  background: var(--rag-bg-hover);
  color: var(--rag-text-primary);
  border: 1px solid var(--rag-border);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.secondary-btn:hover {
  background: var(--rag-bg-surface);
  border-color: var(--rag-text-secondary);
}

/* ========== API Key Section ========== */
.api-key-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.api-key-input {
  width: 100%;
  padding-right: 80px;
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 13px;
}

.api-key-actions {
  position: absolute;
  right: 8px;
  display: flex;
  gap: 4px;
}

.icon-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--rag-text-secondary);
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background: var(--rag-bg-hover);
  color: var(--rag-text-primary);
}

.api-key-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: rgba(245, 158, 11, 0.1);
  border-radius: 8px;
  font-size: 13px;
  color: #B45309;
}

html.dark .api-key-warning {
  background: rgba(245, 158, 11, 0.15);
  color: #FBBF24;
}

.regenerate-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  background: var(--rag-bg-hover);
  border: 1px solid var(--rag-border);
  border-radius: 8px;
  font-size: 14px;
  color: var(--rag-text-primary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.regenerate-btn:hover {
  background: var(--rag-bg-surface);
  border-color: var(--rag-text-secondary);
}

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .user-profile-page {
    padding: 16px;
  }

  .basic-info {
    flex-direction: column;
  }

  .avatar-section {
    flex-direction: row;
    justify-content: flex-start;
  }

  .profile-content {
    max-width: 100%;
  }
}
</style>
