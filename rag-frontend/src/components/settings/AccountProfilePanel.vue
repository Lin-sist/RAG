<template>
  <section class="account-profile" :aria-busy="busy">
    <h2>账户与工作空间</h2>
    <p class="intro">管理头像与个人资料，修改后保存到当前账号。</p>
    <p v-if="loading" role="status">正在读取资料…</p>
    <div v-if="loadError" class="notice" role="status"><p>{{ loadError }}</p><button type="button" @click="load">重新读取资料</button></div>
    <form novalidate @submit.prevent="save">
      <div class="avatar-row">
        <AccountAvatar class="profile-picture" :preview="preview || undefined" :removed="action === 'REMOVE'" :name="displayName || undefined" />
        <div class="avatar-controls"><strong>{{ summaryName }}</strong><div v-if="baseline" class="buttons">
          <button type="button" :disabled="busy" @click="fileInput?.click()">更换头像</button>
          <button v-if="action === 'REPLACE' || (baseline.hasAvatar && action !== 'REMOVE')" type="button" :disabled="busy" @click="removeAvatar">移除头像</button>
        </div><p>JPG / PNG，最多 2 MB；自动居中裁剪。</p></div>
        <input ref="fileInput" type="file" accept="image/jpeg,image/png" aria-label="选择头像文件" hidden @change="selectAvatar" />
      </div>
      <p v-if="avatarError" class="error" role="alert">{{ avatarError }}</p>
      <p v-if="avatar.unavailable" class="hint">已保存的头像暂未加载。<button type="button" @click="avatar.load(true)">重试头像</button></p>
      <div class="read-only"><span>登录用户名</span><div>{{ baseline?.username || auth.userInfo?.username || '资料暂不可用' }}<p>用于登录，暂不支持修改。</p></div></div>
      <template v-if="baseline">
        <div class="field"><label for="profile-display-name">显示昵称</label><input id="profile-display-name" v-model="displayName" autocomplete="nickname" :disabled="busy" :aria-invalid="!!nameError" aria-describedby="profile-name-hint profile-name-error" /><p id="profile-name-hint" class="hint">最多 50 个字符，留空时显示登录用户名。</p><p id="profile-name-error" class="error">{{ nameError }}</p></div>
        <div class="field"><label for="profile-email">邮箱</label><input id="profile-email" v-model="email" type="email" autocomplete="email" inputmode="email" :disabled="busy" :aria-invalid="!!emailError" aria-describedby="profile-email-hint profile-email-error" /><p id="profile-email-hint" class="hint">仅作为资料邮箱，尚未验证；不用于登录或找回密码。</p><p id="profile-email-error" class="error">{{ emailError }}</p></div>
      </template>
      <div v-else class="read-only"><span>邮箱</span><div>{{ auth.userInfo?.email || '未提供' }}</div></div>
      <div class="read-only workspace"><span>所属工作空间</span><WorkspaceContext mode="profile" /></div>
      <p class="hint">资料可见范围以当前工作空间及知识库授权为准。</p>
      <p v-if="message" :class="{ error: failed }" role="status">{{ message }}</p>
      <div v-if="baseline" class="save-row"><span role="status">{{ busy ? '正在处理…' : dirty ? '尚未保存' : '已保存' }}</span><div class="buttons"><button type="button" :disabled="busy || !dirty" @click="discard">取消</button><button type="submit" class="primary" :disabled="busy || !dirty">{{ saving ? '正在保存…' : '保存修改' }}</button></div></div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, nextTick, onMounted, onUnmounted } from 'vue'
import { getProfile, putProfile, type Profile, type AvatarAction } from '@/api/profile'
import { normalizeError } from '@/api/errors'
import { normalizeProfileImage } from '@/utils/profileImage'
import { useAuthStore } from '@/stores/auth'
import { useProfileAvatarStore } from '@/stores/profileAvatar'
import AccountAvatar from '@/components/common/AccountAvatar.vue'
import WorkspaceContext from '@/components/common/WorkspaceContext.vue'

const auth = useAuthStore(), avatar = useProfileAvatarStore()
const baseline = ref<Profile>()
const displayName = ref(''), email = ref(''), action = ref<AvatarAction>('KEEP')
const fileInput = ref<HTMLInputElement>(), preview = ref('')
let file: Blob | undefined, alive = true, generation = 0, imageGeneration = 0
const session = auth.sessionRevision, userId = auth.userInfo?.id
const loading = ref(false), saving = ref(false), processing = ref(false)
const loadError = ref(''), message = ref(''), failed = ref(false)
let refreshBeforeRetry = false
const nameError = ref(''), emailError = ref(''), avatarError = ref('')
const busy = computed(() => loading.value || saving.value || processing.value)
const blocking = computed(() => saving.value || processing.value)
const dirty = computed(() => !!baseline.value && (displayName.value !== (baseline.value.displayName ?? '') || email.value !== (baseline.value.email ?? '') || action.value !== 'KEEP'))
const summaryName = computed(() => baseline.value?.displayName || auth.userInfo?.displayName || baseline.value?.username || auth.userInfo?.username || '已登录用户')
const current = () => alive && auth.sessionRevision === session && auth.isLoggedIn
const belongs = (profile: Profile) => current() && ((userId ?? auth.userInfo?.id) === undefined || profile.id === (userId ?? auth.userInfo?.id))
function restoreFocus() {
  void nextTick(() => { if (current() && document.activeElement === document.body) document.getElementById('profile-display-name')?.focus() })
}
function revoke(value: typeof preview) { if (value.value) URL.revokeObjectURL(value.value); value.value = '' }
function sync(profile: Profile, image?: Blob) {
  if (!belongs(profile)) throw new Error('Identity changed')
  auth.setUserInfo({ id: profile.id, username: profile.username, email: profile.email ?? '', displayName: profile.displayName, hasAvatar: profile.hasAvatar, profileRevision: profile.revision })
  if (image && profile.hasAvatar) avatar.publish(image, profile.revision)
  else void avatar.load()
}
function discard() {
  imageGeneration++; revoke(preview); file = undefined; action.value = 'KEEP'
  displayName.value = baseline.value?.displayName ?? ''; email.value = baseline.value?.email ?? ''
  nameError.value = ''; emailError.value = ''; avatarError.value = ''; message.value = ''; failed.value = false
  restoreFocus()
}
function install(profile: Profile, image?: Blob) { sync(profile, image); baseline.value = profile; refreshBeforeRetry = false; discard() }
async function load() {
  if (busy.value) return
  const request = ++generation; loading.value = true; loadError.value = ''
  try { const profile = await getProfile(); if (request === generation && belongs(profile)) install(profile) }
  catch (error) { if (current() && request === generation) loadError.value = normalizeError(error).status === 404 ? '当前服务暂不支持资料编辑，以下仅展示已有登录资料。' : '资料暂不可用，请稍后重新读取。' }
  finally { if (current() && request === generation) loading.value = false }
}
async function selectAvatar(event: Event) {
  const input = event.target as HTMLInputElement, selected = input.files?.[0]; input.value = ''
  if (!selected || busy.value) return
  const request = ++imageGeneration; processing.value = true; avatarError.value = ''
  try {
    const blob = await normalizeProfileImage(selected)
    if (!current() || request !== imageGeneration) return
    revoke(preview); preview.value = URL.createObjectURL(blob); file = blob; action.value = 'REPLACE'
  } catch (error) { if (current() && request === imageGeneration) avatarError.value = error instanceof Error ? error.message : '图片处理失败。' }
  finally { if (current() && request === imageGeneration) processing.value = false }
}
function removeAvatar() { imageGeneration++; revoke(preview); file = undefined; action.value = baseline.value?.hasAvatar ? 'REMOVE' : 'KEEP'; avatarError.value = '' }
async function save() {
  if (!baseline.value || busy.value || !dirty.value) return
  const name = displayName.value.trim(), address = email.value.trim()
  nameError.value = Array.from(name).length > 50 || /[\u0000-\u001f\u007f-\u009f]/.test(name) ? '昵称最多 50 个字符，不能含控制字符。' : ''
  emailError.value = address && (address.length > 100 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(address)) ? '请输入有效的邮箱地址，最多 100 个字符。' : ''
  if (nameError.value || emailError.value) { document.getElementById(nameError.value ? 'profile-display-name' : 'profile-email')?.focus(); return }
  saving.value = true; message.value = ''; failed.value = false
  try {
    if (refreshBeforeRetry) {
      const latest = await getProfile()
      if (!belongs(latest)) throw new Error('Identity changed')
      baseline.value = latest
    }
    const profile = await putProfile({ displayName: name || null, email: address || null, expectedRevision: baseline.value.revision, avatarAction: action.value }, file)
    if (belongs(profile)) { install(profile, action.value === 'REPLACE' ? file : undefined); message.value = '个人资料已保存。' }
  } catch (error) {
    if (!current()) return
    const failure = normalizeError(error); failed.value = true
    if (failure.errorCode === 'PROFILE_EMAIL_UNAVAILABLE') { emailError.value = '此邮箱暂不可用，请更换邮箱。'; document.getElementById('profile-email')?.focus() }
    else if (!failure.status || failure.status >= 500 || failure.status === 409) {
      refreshBeforeRetry = true
      message.value = failure.status === 409 ? '资料已在其他位置更新，请重试保存。' : !failure.status ? '网络连接异常，请重试保存。' : '保存失败，请重试。'
    } else message.value = failure.status === 401 ? '登录状态已变化，请重新登录后读取资料。' : failure.status === 413 ? '头像过大，请重新选择。' : failure.status === 415 ? '请选择 JPG 或 PNG 图片。' : '未能保存，请检查资料后重试。'
  } finally {
    if (current()) {
      saving.value = false
      if (emailError.value) void nextTick(() => document.getElementById('profile-email')?.focus())
      else restoreFocus()
    }
  }
}
onMounted(load)
onUnmounted(() => { alive = false; generation++; imageGeneration++; revoke(preview) })
defineExpose({ dirty, blocking, discard })
</script>

<style scoped>
.account-profile { color: var(--rag-text-primary); font-size: 14px; }
h2 { margin: 0 0 8px; font-size: 21px; font-weight: 600; }
.intro,.hint,.avatar-controls p,.read-only p { color: var(--rag-text-secondary); font-size: 12px; line-height: 1.75; }
.intro { margin: 0 0 26px; font-size: 13px; }
.avatar-row { display: flex; align-items: center; gap: 18px; margin-bottom: 20px; }
.profile-picture { width: 64px; height: 64px; font-size: 24px; background: var(--rag-bg-hover); outline-color: var(--rag-border); color: var(--rag-text-primary); }
.avatar-controls { min-width: 0; } .avatar-controls strong { display: block; overflow-wrap: anywhere; margin-bottom: 10px; }
.avatar-controls p { margin: 8px 0 0; }
button,input { font: inherit; color: inherit; }
button { cursor: pointer; min-height: 36px; padding: 7px 12px; border: 1px solid var(--rag-border-control); border-radius: 8px; background: transparent; }
button:hover:not(:disabled) { background: var(--rag-bg-hover); }
button:disabled { opacity: .55; cursor: default; }
button:focus-visible,input:focus-visible { outline: 2px solid var(--rag-primary); outline-offset: 3px; }
.buttons { display: flex; flex-wrap: wrap; gap: 8px; }
.read-only { display: grid; grid-template-columns: 124px minmax(0,1fr); gap: 16px; padding: 18px 0; border-bottom: 1px solid var(--rag-border); overflow-wrap: anywhere; }
.read-only > span { color: var(--rag-text-secondary); } .read-only p { margin: 5px 0 0; }
.field { padding-top: 20px; } label { display: block; margin-bottom: 9px; }
input:not([type=file]) { box-sizing: border-box; width: 100%; min-height: 42px; padding: 10px 12px; border: 1px solid var(--rag-border-control); border-radius: 9px; background: var(--rag-bg-primary); }
input[aria-invalid=true] { border-color: var(--rag-danger, #db6565); }
.field .hint { margin: 7px 0 0; } .error { color: var(--rag-danger, #db6565); font-size: 12px; line-height: 1.7; overflow-wrap: anywhere; }
.error:empty { display: none; }
.notice { padding: 16px; margin: 14px 0; border: 1px solid var(--rag-border-control); border-radius: 10px; background: var(--rag-bg-secondary); line-height: 1.7; }
.notice p { margin: 8px 0 12px; } .notice button { margin: 4px 8px 4px 0; }
.server-copy { font-size: 13px; } .server-copy div { display: flex; flex-wrap: wrap; gap: 12px; } dd { margin: 0; overflow-wrap: anywhere; }
.server-avatar { width: 48px; height: 48px; border-radius: 50%; }
.save-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--rag-border); }
.save-row > span { color: var(--rag-text-secondary); font-size: 12px; }
.primary { background: var(--rag-text-primary); color: var(--rag-bg-primary); border-color: transparent; }
.primary:hover:not(:disabled) { background: var(--rag-text-secondary); }
@media(max-width:520px) { button { min-height: 44px; } .read-only { grid-template-columns: 1fr; gap: 7px; } .save-row { align-items: flex-start; flex-direction: column; } }
</style>
