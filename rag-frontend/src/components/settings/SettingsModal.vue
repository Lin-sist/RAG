<!--
  SettingsModal.vue - ChatGPT 风格设置弹窗
  左右两栏布局：左侧导航 + 右侧内容区
-->
<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="settings-modal-overlay" @click.self="closeModal">
        <div class="settings-modal" @keydown.esc="closeModal">
          <!-- 关闭按钮 -->
          <button class="close-btn" @click="closeModal" title="关闭">
            <X :size="20" />
          </button>

          <!-- 左侧导航栏 -->
          <nav class="settings-nav">
            <div
              v-for="tab in tabs"
              :key="tab.id"
              :class="['nav-item', { active: activeTab === tab.id }]"
              @click="activeTab = tab.id"
            >
              <component :is="tab.icon" :size="18" />
              <span>{{ tab.label }}</span>
            </div>
          </nav>

          <!-- 右侧内容区 -->
          <div class="settings-content">
            <!-- 个人资料 -->
            <div v-if="activeTab === 'profile'" class="content-section">
              <h2 class="section-title">个人资料</h2>
              
              <div class="avatar-row">
                <div class="avatar-circle">{{ userInitial }}</div>
                <button class="text-btn">更换头像</button>
              </div>

              <div class="form-group">
                <label class="form-label">用户名</label>
                <input v-model="user.name" type="text" class="form-input" />
              </div>

              <div class="form-group">
                <label class="form-label">邮箱</label>
                <div class="input-with-icon">
                  <input v-model="user.email" type="email" class="form-input" readonly />
                  <Lock :size="16" class="input-icon-right" />
                </div>
              </div>

              <div class="form-actions">
                <button class="primary-btn" @click="handleSaveProfile">保存修改</button>
              </div>
            </div>

            <!-- 安全性 -->
            <div v-if="activeTab === 'security'" class="content-section">
              <h2 class="section-title">安全性</h2>

              <div class="form-group">
                <label class="form-label">当前密码</label>
                <div class="input-with-icon">
                  <input
                    v-model="passwords.current"
                    :type="showCurrentPassword ? 'text' : 'password'"
                    class="form-input"
                  />
                  <button class="visibility-btn" @click="showCurrentPassword = !showCurrentPassword">
                    <Eye v-if="showCurrentPassword" :size="16" />
                    <EyeOff v-else :size="16" />
                  </button>
                </div>
              </div>

              <div class="form-group">
                <label class="form-label">新密码</label>
                <div class="input-with-icon">
                  <input
                    v-model="passwords.newPassword"
                    :type="showNewPassword ? 'text' : 'password'"
                    class="form-input"
                  />
                  <button class="visibility-btn" @click="showNewPassword = !showNewPassword">
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
                  <button class="visibility-btn" @click="showConfirmPassword = !showConfirmPassword">
                    <Eye v-if="showConfirmPassword" :size="16" />
                    <EyeOff v-else :size="16" />
                  </button>
                </div>
              </div>

              <div class="form-actions">
                <button class="primary-btn" @click="handleUpdatePassword">更新密码</button>
              </div>
            </div>

            <!-- API 密钥 -->
            <div v-if="activeTab === 'apiKey'" class="content-section">
              <h2 class="section-title">API 密钥</h2>

              <div class="form-group">
                <label class="form-label">当前 API 密钥</label>
                <div class="api-key-row">
                  <input
                    :value="showApiKey ? user.apiKey : maskedApiKey"
                    type="text"
                    class="form-input mono"
                    readonly
                  />
                  <button class="icon-btn" @click="showApiKey = !showApiKey" title="显示/隐藏">
                    <Eye v-if="showApiKey" :size="16" />
                    <EyeOff v-else :size="16" />
                  </button>
                  <button class="icon-btn" @click="handleCopyApiKey" title="复制">
                    <Copy :size="16" />
                  </button>
                </div>
              </div>

              <div class="warning-box">
                <AlertTriangle :size="16" />
                <span>重新生成后旧密钥立即失效，请谨慎操作</span>
              </div>

              <div class="form-actions">
                <button class="outlined-btn" @click="handleRegenerateApiKey">
                  <RefreshCw :size="16" />
                  <span>重新生成密钥</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import {
  X,
  User,
  Shield,
  Key,
  Lock,
  Eye,
  EyeOff,
  Copy,
  RefreshCw,
  AlertTriangle,
} from 'lucide-vue-next'

// Props & Emits
interface Props {
  modelValue: boolean
  initialTab?: 'profile' | 'security' | 'apiKey'
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false,
  initialTab: 'profile'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

// Tabs
const tabs = [
  { id: 'profile', label: '个人资料', icon: User },
  { id: 'security', label: '安全性', icon: Shield },
  { id: 'apiKey', label: 'API 密钥', icon: Key },
]

const activeTab = ref<string>(props.initialTab)

// Watch for initialTab changes
watch(() => props.initialTab, (newTab) => {
  if (newTab) {
    activeTab.value = newTab
  }
})

// Mock user data
const user = ref({
  name: 'Lin',
  email: 'lin@example.com',
  avatar: null as string | null,
  apiKey: 'sk-rag-xxxxxxxxxxxxxxxxxxxx',
})

const userInitial = computed(() => user.value.name.charAt(0).toUpperCase())
const maskedApiKey = computed(() => {
  const key = user.value.apiKey
  return key.slice(0, 3) + '●'.repeat(20)
})

// Password state
const passwords = ref({
  current: '',
  newPassword: '',
  confirm: '',
})
const showCurrentPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

// API key state
const showApiKey = ref(false)

// Methods
function closeModal() {
  emit('update:modelValue', false)
}

function handleSaveProfile() {
  console.log('Save profile:', user.value)
}

function handleUpdatePassword() {
  console.log('Update password:', passwords.value)
  passwords.value = { current: '', newPassword: '', confirm: '' }
}

function handleCopyApiKey() {
  navigator.clipboard.writeText(user.value.apiKey)
  console.log('API key copied')
}

function handleRegenerateApiKey() {
  console.log('Regenerate API key')
}

// ESC key handler
function handleEsc(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.modelValue) {
    closeModal()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleEsc)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEsc)
})
</script>

<style scoped>
/* Modal Overlay */
.settings-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* Modal Container */
.settings-modal {
  position: relative;
  width: 740px;
  height: 480px;
  background: #fff;
  border-radius: 16px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.16);
}

/* Close Button */
.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6e6e80;
  transition: all 0.15s ease;
  z-index: 10;
}

.close-btn:hover {
  background: #f4f4f4;
  color: #0d0d0d;
}

/* Left Navigation */
.settings-nav {
  width: 180px;
  background: #f7f7f8;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  color: #0d0d0d;
  cursor: pointer;
  transition: all 0.15s ease;
}

.nav-item:hover {
  background: #efefef;
}

.nav-item.active {
  background: #efefef;
  font-weight: 600;
}

/* Right Content */
.settings-content {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #0d0d0d;
  margin: 0 0 4px 0;
}

/* Avatar Row */
.avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-circle {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #10a37f 0%, #0d8a6a 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  font-weight: 600;
}

.text-btn {
  border: none;
  background: none;
  color: #10a37f;
  font-size: 14px;
  cursor: pointer;
  padding: 0;
}

.text-btn:hover {
  text-decoration: underline;
}

/* Form Elements */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: #6e6e80;
}

.form-input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  color: #0d0d0d;
  background: #fff;
  transition: border-color 0.15s ease;
}

.form-input:focus {
  outline: none;
  border-color: #10a37f;
}

.form-input[readonly] {
  background: #f9f9f9;
  color: #6e6e80;
}

.form-input.mono {
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 13px;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-icon .form-input {
  flex: 1;
  padding-right: 40px;
}

.input-icon-right {
  position: absolute;
  right: 12px;
  color: #b4b4b4;
  pointer-events: none;
}

.visibility-btn {
  position: absolute;
  right: 8px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6e6e80;
}

.visibility-btn:hover {
  background: #f4f4f4;
  color: #0d0d0d;
}

/* API Key Row */
.api-key-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.api-key-row .form-input {
  flex: 1;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: 1px solid #e5e5e5;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6e6e80;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.icon-btn:hover {
  background: #f4f4f4;
  color: #0d0d0d;
  border-color: #d5d5d5;
}

/* Warning Box */
.warning-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
  font-size: 13px;
  color: #92400e;
}

/* Form Actions */
.form-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
}

.primary-btn {
  height: 40px;
  padding: 0 20px;
  background: #10a37f;
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s ease;
}

.primary-btn:hover {
  background: #0d8a6a;
}

.outlined-btn {
  height: 40px;
  padding: 0 16px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  color: #0d0d0d;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.15s ease;
}

.outlined-btn:hover {
  background: #f4f4f4;
  border-color: #d5d5d5;
}

/* Transition */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.15s ease;
}

.modal-enter-active .settings-modal,
.modal-leave-active .settings-modal {
  transition: transform 0.15s ease, opacity 0.15s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .settings-modal,
.modal-leave-to .settings-modal {
  transform: scale(0.97);
  opacity: 0;
}

/* Dark Theme */
:global(html.dark) .settings-modal {
  background: #1e1e1e;
}

:global(html.dark) .close-btn {
  color: #9a9a9a;
}

:global(html.dark) .close-btn:hover {
  background: #2a2a2a;
  color: #fff;
}

:global(html.dark) .settings-nav {
  background: #2a2a2a;
}

:global(html.dark) .nav-item {
  color: #e0e0e0;
}

:global(html.dark) .nav-item:hover {
  background: #3a3a3a;
}

:global(html.dark) .nav-item.active {
  background: #3a3a3a;
}

:global(html.dark) .section-title {
  color: #fff;
}

:global(html.dark) .form-label {
  color: #9a9a9a;
}

:global(html.dark) .form-input {
  background: #2a2a2a;
  border-color: #3a3a3a;
  color: #e0e0e0;
}

:global(html.dark) .form-input:focus {
  border-color: #10a37f;
}

:global(html.dark) .form-input[readonly] {
  background: #252525;
  color: #9a9a9a;
}

:global(html.dark) .visibility-btn {
  color: #9a9a9a;
}

:global(html.dark) .visibility-btn:hover {
  background: #3a3a3a;
  color: #fff;
}

:global(html.dark) .icon-btn {
  background: #2a2a2a;
  border-color: #3a3a3a;
  color: #9a9a9a;
}

:global(html.dark) .icon-btn:hover {
  background: #3a3a3a;
  color: #fff;
  border-color: #4a4a4a;
}

:global(html.dark) .warning-box {
  background: #3d2e0a;
  border-color: #5c4813;
  color: #fcd34d;
}

:global(html.dark) .outlined-btn {
  background: #2a2a2a;
  border-color: #3a3a3a;
  color: #e0e0e0;
}

:global(html.dark) .outlined-btn:hover {
  background: #3a3a3a;
  border-color: #4a4a4a;
}
</style>
