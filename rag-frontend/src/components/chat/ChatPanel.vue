<template>
  <main :class="['chat-panel', { 'is-empty': messages.length === 0 }]">
    <div v-if="messages.length" class="trace-view-switch"><div role="tablist" aria-label="问答视图"><button id="qa-conversation-tab" role="tab" aria-controls="qa-conversation" :aria-selected="view === 'conversation'" @click="view = 'conversation'" @keydown="viewTabKey($event, 'conversation')">对话</button><button id="qa-trace-tab" role="tab" aria-controls="qa-execution-trace" :aria-selected="view === 'trace'" @click="view = 'trace'" @keydown="viewTabKey($event, 'trace')">轨迹</button></div><select v-if="view === 'trace' && assistantMessages.length > 1" v-model="selectedTraceId" aria-label="选择本页问答轨迹"><option value="">最新问答</option><option v-for="msg in assistantMessages" :key="msg.id" :value="msg.id">{{ questionFor(msg).slice(0, 45) || '本次问答' }}</option></select></div>
    <div id="qa-conversation" v-show="view === 'conversation'" ref="messageViewport" class="messages-container" role="tabpanel" aria-labelledby="qa-conversation-tab" @scroll="updateScrollPosition" @wheel="pauseUpwardScroll" @touchstart="pauseTouchScroll">
      <p v-if="isHistoryDetail && historyLoadError" class="kb-navigation-status" role="alert">{{ historyLoadError }} <button v-if="parseRouteHistoryId()" @click="retryHistorySession">重试读取历史</button><RouterLink to="/history">返回历史记录</RouterLink></p>
      <div v-if="messages.length === 0" class="welcome-screen">
        <h1 class="welcome-title">你好{{ authStore.userInfo?.username ? '，' + authStore.userInfo.username : '' }}。准备好开始了吗？</h1>
      </div>

      <template v-else>
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['message-wrapper', msg.role]"
        >
          <div v-if="msg.role === 'user'" class="user-message">
            <div class="user-content">{{ msg.content }}</div>
          </div>

          <div v-else class="ai-message">
            <div class="ai-avatar"><BrandMark compact /></div>
            <div class="ai-body">
              <div v-if="msg.loading && !msg.content" class="loading-dots" role="status" aria-label="正在生成回答">
                <span></span>
                <span></span>
                <span></span>
              </div>

              <AnswerContent
                v-else
                :content="msg.content"
                :error="msg.error"
              />

              <span v-if="msg.loading && msg.content" class="typing-cursor" aria-hidden="true"></span>

              <p v-if="msg.responseMode === 'stream'" class="stream-state" role="status">
                {{ streamStateLabel(msg) }}
              </p>

              <div v-if="!msg.loading && msg.content && !msg.error && (msg.responseMode !== 'stream' || msg.streamStatus === 'ANSWER')" class="message-actions">
                <button class="action-btn" title="复制回答" aria-label="复制回答" :disabled="copyingAnswer === msg.id" @click="copyToClipboard(msg)">
                  <Copy :size="14" />
                </button>
                <span class="answer-copy-status" role="status" aria-live="polite">{{ answerCopyStatus[msg.id] }}</span>
              </div>

              <AnswerSources
                v-if="!msg.loading && !msg.error && (msg.responseMode !== 'stream' || msg.streamStatus === 'ANSWER')"
                :citations="msg.citations"
                :hint="msg.sourceHint"
                :reset-key="messages.length"
              />

              <HistoryFeedback
                v-if="isHistoryDetail && historyRecordId && !msg.loading && !msg.error"
                :key="historyRecordId"
                :history-id="historyRecordId"
              />
            </div>
          </div>
        </div>
      </template>

    </div>

    <ExecutionTracePanel v-if="messages.length" id="qa-execution-trace" v-show="view === 'trace'" class="trace-viewport" role="tabpanel" aria-labelledby="qa-trace-tab" :trace="traceMessage?.executionTrace" :question="traceMessage ? questionFor(traceMessage) : ''" :answer="traceMessage?.content" :citations="traceMessage?.citations" :pending="traceMessage?.loading" :invalid="traceMessage?.traceInvalid" :released="traceMessage?.traceReleased" :historical="isHistoryDetail" :business-state="traceMessage?.businessState" :transport="traceMessage?.streamStatus" :answer-available="traceAnswerAvailable" :active="view === 'trace'" />

    <div class="input-wrapper">
      <button v-if="showScrollBottom && messages.length" class="scroll-bottom" aria-label="回到底部" @click="scrollToBottom(true)"><ArrowDown :size="18" /></button>
      <p v-if="!isHistoryDetail && (kbNavigationError || (kbLoading && route.query?.kbId !== undefined))" class="kb-navigation-status" role="status">{{ kbNavigationError || '正在确认指定知识库…' }}<button v-if="kbNavigationRetry" type="button" @click="applyKbNavigation">重试指定知识库</button></p>
      <div
        v-if="selectedKb"
        class="kb-scope-row"
      >
        <div
          class="kb-scope-chip"
        >
          <Database :size="14" aria-hidden="true" />
          <span :title="selectedKb.name">知识库范围：<b>{{ selectedKb.name }}</b></span>
          <button
            type="button"
            title="取消选择"
            aria-label="取消选择知识库"
            :disabled="isHistoryDetail"
            @click="clearSelectedKb"
            class="kb-scope-x"
          >
            <X :size="12" aria-hidden="true" />
          </button>
        </div>
      </div>

      <div ref="composerRoot" class="input-container">
        <div ref="kbDropdownRoot" class="kb-picker" @keydown.esc.stop.prevent="closeKbDropdown(true)" @focusout="handleKbFocusout">
          <button ref="kbDropdownTrigger" class="attach-btn" title="选择知识库" aria-label="选择知识库" aria-controls="composer-kb-menu" :aria-expanded="kbDropdownOpen" :disabled="isHistoryDetail" @click.stop="toggleKbDropdown">
            <Plus :size="18" />
          </button>
          <div
            v-if="kbDropdownOpen"
            @click.stop
            id="composer-kb-menu"
            class="composer-menu kb-dropdown"
            :data-placement="menuPlacement"
            :style="{ maxHeight: `${menuMaxHeight}px` }"
          >
            <div class="menu-label">选择知识库范围</div>
            <div v-if="kbLoading" class="kb-load-status">正在加载知识库…</div>
            <div v-else-if="kbLoadError" class="kb-load-status" role="status">知识库加载失败<button type="button" @click="loadKbListIfNeeded()">重试知识库</button></div>
            <div v-else-if="kbList.length === 0" class="kb-load-status">暂无知识库</div>
            <div v-else class="kb-menu-scroll">
              <button
                v-for="kb in kbList"
                :key="kb.id"
                type="button"
                class="kb-menu-item"
                :class="{ selected: selectedKbId === kb.id }"
                :aria-pressed="selectedKbId === kb.id"
                :title="kb.name"
                @click="selectKbFromDropdown(kb)"
              >
                <Database :size="16" aria-hidden="true" />
                <span>{{ kb.name }}</span>
                <Check v-if="selectedKbId === kb.id" :size="15" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>

        <textarea
          ref="composerInput"
          v-model="inputText"
          rows="1"
          class="pill-input"
          :placeholder="isHistoryDetail ? '历史详情只展示单条问答，请前往新问答后提问' : selectedKb ? `在「${selectedKb.name}」中提问…` : '请选择知识库后输入问题'"
          :disabled="isHistoryDetail"
          @keydown="handleKeydown"
        ></textarea>

        <div class="composer-actions">
          <div v-if="!isHistoryDetail" ref="responseOptionsRoot" class="response-options" @keydown.esc.stop.prevent="closeResponseOptions(true)" @focusout="handleOptionsFocusout">
            <button
              ref="responseOptionsTrigger"
              type="button"
              class="options-btn"
              :class="{ 'is-open': responseOptionsOpen }"
              aria-label="高级选项"
              :title="`高级选项 · 当前${responseMode === 'stream' ? '流式问答' : '同步问答'}`"
              aria-haspopup="dialog"
              aria-controls="response-options-panel"
              :aria-expanded="responseOptionsOpen"
              :disabled="isSubmitting"
              @click="toggleResponseOptions"
            >
              <SlidersHorizontal :size="18" aria-hidden="true" />
              <span v-if="responseMode === 'stream'" class="options-indicator" aria-hidden="true"></span>
            </button>
            <div v-if="responseOptionsOpen" id="response-options-panel" class="composer-menu response-options-panel" :data-placement="menuPlacement" :style="{ maxHeight: `${menuMaxHeight}px` }" role="dialog" aria-label="高级选项">
              <p class="options-heading">高级选项</p>
              <fieldset class="response-options-group" :disabled="isSubmitting">
                <legend>回答方式</legend>
                <label v-for="mode in responseModes" :key="mode.value" class="response-option" :class="{ selected: responseMode === mode.value }">
                  <input v-model="responseMode" type="radio" name="response-mode" :value="mode.value" />
                  <span class="response-option-copy"><span class="response-option-title">{{ mode.label }}</span><span class="response-option-hint">{{ mode.hint }}</span></span>
                  <Check :size="16" class="response-option-check" aria-hidden="true" />
                </label>
              </fieldset>
            </div>
          </div>
          <button v-if="streaming" class="send-btn-pill stop-receiving" type="button" title="停止接收" aria-label="停止接收" @click="stopReceiving"><Square :size="14" fill="currentColor" aria-hidden="true" /></button>
          <button
            v-else
            :class="['send-btn-pill', { active: canSend }]"
            :disabled="!canSend"
            aria-label="发送问题"
            @click="handleSend"
          >
            <ArrowUp :size="17" />
          </button>
        </div>
      </div>

      <p class="disclaimer">
        {{ isHistoryDetail ? '这里展示的是一条历史问答记录，不代表多轮会话。' : responseMode === 'stream' ? '仅完整回答终态展示来源；停止接收不代表服务端已取消。' : 'AI 可能产生不准确内容，请核对引用来源' }}
      </p>

      <div v-if="messages.length === 0" class="example-questions" aria-label="示例问题">
        <button
          v-for="question in exampleQuestions"
          :key="question"
          type="button"
          class="example-card"
          @click="selectExample(question)"
        >
          <span class="example-text">{{ question }}</span>
          <ArrowRight :size="16" aria-hidden="true" />
        </button>
      </div>

    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Copy,
  ArrowUp,
  ArrowDown,
  Plus,
  SlidersHorizontal,
  Check,
  Database,
  X,
  Square,
} from 'lucide-vue-next'
import BrandMark from '@/components/common/BrandMark.vue'
import AnswerContent from './AnswerContent.vue'
import AnswerSources from './AnswerSources.vue'
import ExecutionTracePanel from './ExecutionTracePanel.vue'
import '@/styles/traceHost.css'
import { TraceAccumulator, releaseOldTraces } from '@/utils/executionTrace'
import { copyText, clipboardMessage } from '@/utils/clipboard'
import { parseKbId } from '@/utils/kbPresentation'
import { getHistoryById } from '@/api/history'
import { RouterLink } from 'vue-router'
import HistoryFeedback from '@/components/history/HistoryFeedback.vue'
import { ask } from '@/api/qa'
import { useSSE } from '@/composables/useSSE'
import { normalizeError } from '@/api/errors'
import { useChatStore } from '@/stores/chat'
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase'
import { useAuthStore } from '@/stores/auth'
import type { KnowledgeBaseDTO } from '@/types/knowledgeBase'
import {
  applySyncResponse,
  applyStructuredResponse,
  buildHistoryMessages,
  type ChatPresentationMessage as Message,
} from '@/utils/qaPresentation'

const messages = ref<Message[]>([])
const view = ref<'conversation' | 'trace'>('conversation')
const selectedTraceId = ref('')
let qaRequestSequence = 0
const assistantMessages = computed(() => messages.value.filter(m => m.role === 'assistant'))
const traceMessage = computed(() => assistantMessages.value.find(m => m.id === selectedTraceId.value) ?? assistantMessages.value[assistantMessages.value.length - 1])
const traceAnswerAvailable = computed(() => {
  const msg = traceMessage.value
  return !!msg && !msg.loading && !msg.error && (msg.businessState === 'ANSWER' || !msg.businessState && msg.responseMode !== 'stream')
})
function questionFor(msg: Message) { const i = messages.value.findIndex(m => m.id === msg.id); return i > 0 && messages.value[i - 1]?.role === 'user' ? messages.value[i - 1]!.content : '' }
function viewTabKey(event: KeyboardEvent, current: 'conversation' | 'trace') {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  view.value = event.key === 'Home' ? 'conversation' : event.key === 'End' ? 'trace' : current === 'trace' ? 'conversation' : 'trace'
  document.getElementById(view.value === 'trace' ? 'qa-trace-tab' : 'qa-conversation-tab')?.focus()
}
const inputText = ref('')
const composerInput = ref<HTMLTextAreaElement | null>(null)
const composerRoot = ref<HTMLElement>()
const menuPlacement = ref<'above' | 'below'>('below')
const menuMaxHeight = ref(350)
let composerResizeObserver: ResizeObserver | undefined
const isSubmitting = ref(false)
const responseMode = ref<'sync' | 'stream'>('sync')
const responseModes = [
  { value: 'sync', label: '同步问答', hint: '完整返回回答与来源' },
  { value: 'stream', label: '流式问答', hint: '逐步输出，完整回答后展示来源' },
] as const
const responseOptionsRoot = ref<HTMLElement>()
const responseOptionsTrigger = ref<HTMLButtonElement>()
const responseOptionsOpen = ref(false)
const streaming = ref(false)
const sse = useSSE()
const messageViewport = ref<HTMLElement>()
const followBottom = ref(true)
const showScrollBottom = ref(false)
const answerCopyStatus = ref<Record<string, string>>({})
const copyingAnswer = ref<string | null>(null)
const kbDropdownRoot = ref<HTMLElement>()
const kbDropdownTrigger = ref<HTMLButtonElement>()
const kbDropdownOpen = ref(false)
const selectedKbId = ref<number | null>(null)
const route = useRoute()
const historyLoading = ref(false)
const historyLoadError = ref('')
const historyRecordId = ref<number | null>(null)
let historyLoadSeq = 0
let historyAlive = true
const chatStore = useChatStore()
const kbStore = useKnowledgeBaseStore()
const authStore = useAuthStore()
const kbLoading = ref(false)
const kbLoadError = ref(false)
const kbNavigationError = ref(''), kbNavigationRetry = ref(false)
let kbNavigationSequence = 0
let kbLoadPromise: Promise<void> | null = null

const exampleQuestions = [
  '这个知识库包含哪些主要内容？',
  '请总结一下最重要的要点',
  '帮我解释一下核心概念',
  '有哪些实际应用场景？',
]

const canSend = computed(() => (
  inputText.value.trim().length > 0
  && selectedKbId.value !== null
  && selectedKb.value !== null
  && !kbNavigationError.value
  && !isSubmitting.value
  && !historyLoading.value
  && route.name !== 'ChatSession'
))
const kbList = computed(() => kbStore.list)
const selectedKb = computed(() => kbList.value.find(kb => kb.id === selectedKbId.value) ?? null)
const isHistoryDetail = computed(() => route.name === 'ChatSession')
watch(inputText, async () => {
  await nextTick()
  resizeComposerInput()
})

function resizeComposerInput() {
  if (!composerInput.value) return
  composerInput.value.style.height = 'auto'
  composerInput.value.style.height = Math.min(composerInput.value.scrollHeight, 200) + 'px'
}

function handleComposerResize() {
  resizeComposerInput()
  updateComposerMenuSpace()
}

function updateComposerMenuSpace() {
  if ((!kbDropdownOpen.value && !responseOptionsOpen.value) || !composerRoot.value) return
  const rect = composerRoot.value.getBoundingClientRect()
  const viewport = window.visualViewport
  const top = viewport?.offsetTop ?? 0
  const bottom = top + (viewport?.height ?? window.innerHeight)
  const above = Math.max(0, rect.top - top - 14)
  const below = Math.max(0, bottom - rect.bottom - 14)
  const desired = kbDropdownOpen.value ? 350 : 220
  const preferBelow = messages.value.length === 0
  const preferred = preferBelow ? below : above
  const alternative = preferBelow ? above : below
  const useBelow = preferred < Math.min(desired, 240) && alternative > preferred ? !preferBelow : preferBelow
  menuPlacement.value = useBelow ? 'below' : 'above'
  menuMaxHeight.value = Math.min(desired, useBelow ? below : above)
}

watch([kbDropdownOpen, responseOptionsOpen], async ([kbOpen, optionsOpen]) => {
  if (!kbOpen && !optionsOpen) return
  await nextTick()
  updateComposerMenuSpace()
})

async function loadKbListIfNeeded(force = false) {
  if (kbLoadPromise) return kbLoadPromise
  if (!force && kbStore.list.length > 0 && !kbLoadError.value) return
  kbLoading.value = true
  kbLoadError.value = false
  kbLoadPromise = (async () => { try {
    await kbStore.fetchList()
  } catch {
    kbLoadError.value = true
  } finally {
    kbLoading.value = false
    kbLoadPromise = null
  } })()
  return kbLoadPromise
}

async function applyKbNavigation() {
  const seq = ++kbNavigationSequence
  kbNavigationError.value = ''; kbNavigationRetry.value = false
  if (isHistoryDetail.value || route.query?.kbId === undefined) return
  selectedKbId.value = null
  const id = parseKbId(route.query.kbId)
  if (!id) { kbNavigationError.value = '指定知识库参数无效，请重新选择知识库'; return }
  await loadKbListIfNeeded(true)
  if (seq !== kbNavigationSequence || isHistoryDetail.value) return
  if (kbLoadError.value) { kbNavigationError.value = '指定知识库加载失败'; kbNavigationRetry.value = true; return }
  if (!kbList.value.some(kb => kb.id === id)) { kbNavigationError.value = '指定知识库不可访问或已不存在，请重新选择知识库'; return }
  selectedKbId.value = id
}

function parseRouteHistoryId(): number | null {
  const raw = route.params.id

  if (Array.isArray(raw)) {
    return null
  }

  const id = Number(raw)
  return typeof raw === 'string' && /^[1-9]\d*$/.test(raw) && Number.isSafeInteger(id) ? id : null
}

async function loadHistorySession(id: number) {
  const seq = ++historyLoadSeq
  const revision = authStore.sessionRevision
  const current = () => historyAlive && seq === historyLoadSeq && revision === authStore.sessionRevision

  historyLoading.value = true
  historyLoadError.value = ''
  historyRecordId.value = null

  messages.value = [
    {
      id: `history_${id}_loading`,
      role: 'assistant',
      content: '',
      loading: true,
    },
  ]

  try {
    const res = await getHistoryById(id)
    const record = res.data.data

    if (!current()) return
    if (record.id !== id || (authStore.userInfo?.id && record.userId !== authStore.userInfo.id)) throw new Error('HISTORY_IDENTITY_MISMATCH')

    messages.value = buildHistoryMessages(record)
    historyRecordId.value = record.id
    selectedKbId.value = record.kbId ?? null

    await loadKbListIfNeeded()
    if (current()) scrollToBottom()
  } catch (error) {
    if (!current()) return

    historyLoadError.value = normalizeError(error).message
    historyRecordId.value = null
    messages.value = [
      {
        id: `history_${id}_error`,
        role: 'assistant',
        content: '历史记录加载失败，请返回历史记录页重试。',
        loading: false,
        error: true,
      },
    ]
    ElMessage.error('历史记录加载失败')
  } finally {
    if (current()) {
      historyLoading.value = false
    }
  }
}

function retryHistorySession() { const id = parseRouteHistoryId(); if (id) void loadHistorySession(id) }

function resetNewChat() {
  closeResponseOptions()
  closeKbDropdown()
  qaRequestSequence++
  view.value = 'conversation'; selectedTraceId.value = ''
  isSubmitting.value = false; streaming.value = false
  kbNavigationSequence++
  kbNavigationError.value = ''; kbNavigationRetry.value = false
  sse.disconnect()
  historyLoadSeq += 1
  historyLoading.value = false
  historyLoadError.value = ''
  historyRecordId.value = null
  messages.value = []
  selectedKbId.value = null
  followBottom.value = true
  showScrollBottom.value = false
  answerCopyStatus.value = {}
}

async function toggleKbDropdown() {
  closeResponseOptions()
  kbDropdownOpen.value = !kbDropdownOpen.value
  if (kbDropdownOpen.value) {
    await nextTick()
    const root = kbDropdownRoot.value
    const option = root?.querySelector<HTMLButtonElement>('.kb-menu-item.selected') ?? root?.querySelector<HTMLButtonElement>('.kb-menu-item')
    option?.focus()
  }
}

function closeKbDropdown(restoreFocus = false) {
  kbDropdownOpen.value = false
  if (restoreFocus) kbDropdownTrigger.value?.focus()
}

function handleKbFocusout(event: FocusEvent) {
  if (!kbDropdownRoot.value?.contains(event.relatedTarget as Node | null)) closeKbDropdown()
}

function closeResponseOptions(restoreFocus = false) {
  responseOptionsOpen.value = false
  if (restoreFocus) responseOptionsTrigger.value?.focus()
}

async function toggleResponseOptions() {
  if (isSubmitting.value) return
  responseOptionsOpen.value = !responseOptionsOpen.value
  kbDropdownOpen.value = false
  if (responseOptionsOpen.value) {
    await nextTick()
    responseOptionsRoot.value?.querySelector<HTMLInputElement>('input:checked')?.focus()
  }
}

function handleOptionsFocusout(event: FocusEvent) {
  if (!responseOptionsRoot.value?.contains(event.relatedTarget as Node | null)) closeResponseOptions()
}

watch(isSubmitting, busy => { if (busy) closeResponseOptions() })

function selectKbFromDropdown(kb: KnowledgeBaseDTO) {
  kbNavigationSequence++
  kbNavigationError.value = ''; kbNavigationRetry.value = false
  selectedKbId.value = kb.id
  kbDropdownOpen.value = false
  nextTick(() => composerInput.value?.focus())
}

function clearSelectedKb() {
  kbNavigationSequence++
  kbNavigationError.value = ''; kbNavigationRetry.value = false
  selectedKbId.value = null
}

function handleOutsideClick(event: MouseEvent) {
  const target = event.target as Node | null
  if (!target) return
  if (!responseOptionsRoot.value?.contains(target)) closeResponseOptions()
  if (kbDropdownRoot.value?.contains(target)) return
  kbDropdownOpen.value = false
}

function updateScrollPosition() {
  const viewport = messageViewport.value
  if (!viewport) return
  const nearBottom = viewport.scrollHeight - viewport.clientHeight - viewport.scrollTop < 80
  followBottom.value = nearBottom
  showScrollBottom.value = !nearBottom
}

function pauseUpwardScroll(event: WheelEvent) {
  if (event.deltaY < 0) followBottom.value = false
}

function pauseTouchScroll() {
  followBottom.value = false
}

function scrollToBottom(force = false) {
  if (force) followBottom.value = true
  nextTick(() => {
    const viewport = messageViewport.value
    if (!viewport) return
    if (followBottom.value) {
      viewport.scrollTop = viewport.scrollHeight
      showScrollBottom.value = false
    } else {
      showScrollBottom.value = viewport.scrollHeight - viewport.clientHeight - viewport.scrollTop >= 80
    }
  })
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleSend() {
  if (historyLoading.value) return
  if (historyLoadError.value) {
    historyLoadError.value = ''
  }
  if (!canSend.value) return
  sendMessage(inputText.value.trim())
  inputText.value = ''
}

function selectExample(question: string) {
  if (isHistoryDetail.value || isSubmitting.value) return
  inputText.value = question
  nextTick(() => composerInput.value?.focus())
}

function streamStateLabel(message: Message): string {
  const reason = message.streamReason && message.streamReason !== 'NONE'
    ? `（原因：${message.streamReason}）`
    : ''
  switch (message.streamStatus) {
    case 'CONNECTING': return '流式问答 · 正在连接'
    case 'STREAMING_TEXT': return '流式问答 · 正在接收'
    case 'ANSWER': return '完整回答 · 来源来自本次流式问答'
    case 'NO_ANSWER': return `未获得可用答案${reason}`
    case 'UNSUPPORTED': return `当前问题不受支持${reason}`
    case 'INVALID': return `输入无效${reason}`
    case 'ERROR': return `本次问答失败${reason}；已收到的文本不构成完整回答。`
    case 'CANCELLED': return `服务端报告本次问答已取消${reason}`
    case 'INCOMPLETE': return '流式连接未交付有效终态；已收到的文本不构成完整回答。'
    case 'CLIENT_ABORTED': return '已停止接收；服务端是否取消及是否保存历史无法由此确认。'
    default: return ''
  }
}

function stopReceiving() {
  sse.disconnect()
}

async function sendMessage(text: string) {
  if (!text || isSubmitting.value || historyLoading.value || isHistoryDetail.value) return
  const effectiveKbId = selectedKbId.value ?? chatStore.currentKbId
  if (!effectiveKbId) {
    ElMessage.warning('请先选择一个知识库')
    return
  }

  const userMsg: Message = {
    id: `msg_${Date.now()}_user`,
    role: 'user',
    content: text,
  }
  messages.value.push(userMsg)

  const aiMsg: Message = reactive({
    id: `msg_${Date.now()}_ai`,
    role: 'assistant',
    content: '',
    loading: true,
    responseMode: responseMode.value,
    ...(responseMode.value === 'stream' ? { streamStatus: 'CONNECTING' as const } : {}),
  })
  messages.value.push(aiMsg)
  const requestSequence = ++qaRequestSequence
  const revision = authStore.sessionRevision
  const current = () => historyAlive && requestSequence === qaRequestSequence && revision === authStore.sessionRevision && messages.value.some(m => m.id === aiMsg.id)
  const trace = new TraceAccumulator()

  scrollToBottom(true)
  isSubmitting.value = true

  try {
    if (aiMsg.responseMode === 'stream') {
      streaming.value = true
      const result = await sse.connectStructured('/api/qa/ask/stream', {
        kbId: effectiveKbId,
        question: text,
        topK: chatStore.topK,
      }, (chunk) => {
        if (!current()) return
        aiMsg.content += chunk
        aiMsg.streamStatus = 'STREAMING_TEXT'
        scrollToBottom()
      }, payload => {
        if (!current()) return
        trace.ingest(payload)
        aiMsg.executionTrace = trace.value
        aiMsg.traceInvalid = trace.invalid
      })
      if (!current()) return
      if (applyStructuredResponse(aiMsg, result, sse.error.value)) {
        window.dispatchEvent(new Event('rag-history-updated'))
      }
      return
    }
    const response = await ask({
      kbId: effectiveKbId,
      question: text,
      topK: chatStore.topK,
    }, true)
    if (!current()) return
    applySyncResponse(aiMsg, response.data.data)
    window.dispatchEvent(new Event('rag-history-updated'))
  } catch (error) {
    if (!current()) return
    const apiError = normalizeError(error)
    aiMsg.content = apiError.message
    aiMsg.loading = false
    aiMsg.error = true
    aiMsg.businessState = 'ERROR'
    if (aiMsg.responseMode === 'stream') aiMsg.streamStatus = 'INCOMPLETE'
    aiMsg.citations = []
    aiMsg.sourceHint = undefined
  } finally {
    if (current()) {
      if (aiMsg.executionTrace && (aiMsg.streamStatus === 'CLIENT_ABORTED' || aiMsg.streamStatus === 'INCOMPLETE')) {
        aiMsg.executionTrace = { ...aiMsg.executionTrace, captureStatus: 'PARTIAL', reasonCode: 'INCOMPLETE' }
      }
      releaseOldTraces(assistantMessages.value)
      isSubmitting.value = false
      streaming.value = false
      scrollToBottom()
    }
  }
}

async function copyToClipboard(message: Message) {
  if (copyingAnswer.value) return
  const id = message.id
  copyingAnswer.value = id
  answerCopyStatus.value[id] = '正在复制…'
  const result = await copyText(message.content)
  if (messages.value.some(item => item.id === id)) answerCopyStatus.value[id] = clipboardMessage(result)
  copyingAnswer.value = null
}

watch(
  () => route.fullPath,
  async () => {
    closeResponseOptions()
    closeKbDropdown()
    const historyId = parseRouteHistoryId()

    if (route.name === 'ChatSession' && historyId) {
      await loadHistorySession(historyId)
      return
    }

    if (route.name === 'ChatSession') {
      resetNewChat()
      historyLoadError.value = '历史记录 ID 无效，请返回历史记录页。'
      return
    }

    if (route.name === 'Chat' || route.name === 'ChatV2') {
      resetNewChat()
      await applyKbNavigation()
    }
  },
  { immediate: true },
)

watch(() => authStore.sessionRevision, () => {
  const historical = isHistoryDetail.value
  resetNewChat()
  if (!historical) return
  const revision = authStore.sessionRevision
  queueMicrotask(() => { if (historyAlive && revision === authStore.sessionRevision && authStore.isLoggedIn) retryHistorySession() })
}, { flush: 'sync' })

onMounted(() => {
  void loadKbListIfNeeded()
  document.addEventListener('click', handleOutsideClick)
  window.addEventListener('resize', handleComposerResize)
  window.visualViewport?.addEventListener('resize', handleComposerResize)
  window.visualViewport?.addEventListener('scroll', updateComposerMenuSpace)
  if (typeof ResizeObserver !== 'undefined' && composerRoot.value) {
    composerResizeObserver = new ResizeObserver(handleComposerResize)
    composerResizeObserver.observe(composerRoot.value)
  }
})

onUnmounted(() => {
  historyAlive = false
  historyLoadSeq++
  kbNavigationSequence++
  sse.disconnect()
  document.removeEventListener('click', handleOutsideClick)
  window.removeEventListener('resize', handleComposerResize)
  window.visualViewport?.removeEventListener('resize', handleComposerResize)
  window.visualViewport?.removeEventListener('scroll', updateComposerMenuSpace)
  composerResizeObserver?.disconnect()
})
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  min-height: 0;
  background: var(--rag-bg-page);
}
.kb-navigation-status { max-width: 760px; margin: 8px auto; font-size: 13px; color: var(--rag-text-secondary); overflow-wrap: anywhere; }
.kb-navigation-status button { margin-left: 8px; color: var(--rag-text-primary); background: var(--rag-bg-hover); border: 1px solid var(--rag-border); border-radius: 6px; padding: 5px 8px; cursor: pointer; }

.chat-panel.is-empty {
  justify-content: center;
  padding-bottom: 9vh;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  overflow-x: hidden;
}

.is-empty .messages-container {
  flex: 0 0 auto;
  overflow: visible;
}

.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 0 24px;
  text-align: center;
}

.welcome-title {
  font-size: 30px;
  font-weight: 500;
  letter-spacing: .3px;
  color: var(--rag-text-primary);
  margin: 0 0 30px;
  overflow-wrap: anywhere;
}

.welcome-subtitle {
  font-size: 14px;
  color: var(--rag-text-secondary);
  margin: 0 0 26px;
  max-width: 400px;
}

.example-questions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
  max-width: 820px;
  margin-top: 26px;
}

.example-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 11px;
  padding: 13px 15px;
  background: var(--rag-bg-surface);
  border: 1px solid var(--composer-border);
  border-radius: 14px;
  color: var(--rag-text-secondary);
  text-align: left;
  cursor: pointer;
  transition: background-color 150ms, border-color 150ms, color 150ms;
}

.example-card:hover {
  background: var(--rag-bg-hover);
  border-color: var(--rag-text-placeholder);
}

.example-card:focus-visible {
  outline: 2px solid var(--rag-primary);
  outline-offset: 2px;
}

.example-text {
  font-size: 13.5px;
  line-height: 1.5;
  color: inherit;
}

.example-card svg {
  color: var(--rag-text-secondary);
  flex: none;
  transition: color 150ms;
}

.example-card:hover svg {
  color: var(--rag-text-primary);
}

.message-wrapper {
  padding: 18px 24px;
}

.message-wrapper.user {
  background: transparent;
}

.message-wrapper.assistant {
  background: transparent;
}

.user-message {
  max-width: 760px;
  margin: 0 auto;
  padding: 0;
  display: flex;
  justify-content: flex-end;
}

.user-content {
  background: var(--rag-bg-user-msg);
  border-radius: 24px;
  padding: 11px 18px;
  font-size: 15px;
  line-height: 1.7;
  max-width: 76%;
  word-break: break-word;
  white-space: pre-wrap;
}

.ai-message {
  max-width: 760px;
  margin: 0 auto;
  padding: 0;
  display: flex;
  gap: 16px;
}

.ai-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-avatar :deep(svg) { width: 32px; height: 32px; }

.ai-body {
  flex: 1;
  min-width: 0;
  position: relative;
}


.loading-dots {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: var(--rag-text-secondary);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: var(--rag-primary);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 0.8s step-end infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.message-actions {
  display: flex;
  gap: 4px;
  margin-top: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.message-wrapper:hover .message-actions {
  opacity: 1;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 0;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: var(--rag-text-secondary);
  transition: all 0.2s ease;
}

.action-btn:focus-visible, .scroll-bottom:focus-visible { outline: 2px solid var(--rag-primary); outline-offset: 3px; }
.answer-copy-status { font-size: 12px; color: var(--rag-text-secondary); }
.scroll-bottom { position: absolute; top: -42px; left: calc(50% - 18px); width: 36px; height: 36px; display: grid; place-items: center; border-radius: 50%; border: 1px solid var(--rag-border); background: var(--rag-bg-surface); color: var(--rag-text-primary); cursor: pointer; box-shadow: 0 3px 12px #0002; }

.action-btn:hover {
  border-color: var(--rag-primary);
  color: var(--rag-primary);
}


.input-wrapper {
  --composer-border: rgba(0, 0, 0, .1);
  --composer-border-focus: rgba(0, 0, 0, .18);
  --composer-hover: rgba(0, 0, 0, .05);
  --composer-muted: #8f8f8f;
  --composer-chip: rgba(0, 0, 0, .045);
  --composer-shadow: 0 2px 14px rgba(0, 0, 0, .07);
  --composer-menu-shadow: 0 14px 44px rgba(0, 0, 0, .16), 0 2px 8px rgba(0, 0, 0, .08);
  position: sticky;
  bottom: 0;
  flex: none;
  padding: 6px 24px env(safe-area-inset-bottom, 0px);
  background: linear-gradient(to top, var(--rag-bg-page) 80%, transparent);
  display: flex;
  flex-direction: column;
  align-items: center;
}

html.dark .input-wrapper {
  --composer-border: rgba(255, 255, 255, .075);
  --composer-border-focus: rgba(255, 255, 255, .14);
  --composer-hover: rgba(255, 255, 255, .055);
  --composer-muted: #8d8d8d;
  --composer-chip: rgba(255, 255, 255, .055);
  --composer-shadow: 0 2px 14px rgba(0, 0, 0, .12);
  --composer-menu-shadow: 0 18px 50px rgba(0, 0, 0, .55), 0 2px 10px rgba(0, 0, 0, .4);
}

.is-empty .input-wrapper {
  position: static;
  padding-top: 0;
  background: none;
}

.input-container {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 6px;
  max-width: 820px;
  width: 100%;
  background: var(--rag-bg-input);
  border: 1px solid var(--composer-border);
  border-radius: 28px;
  padding: 8px 10px 8px 14px;
  box-shadow: var(--composer-shadow);
  transition: border-color 150ms, box-shadow 150ms;
}

.input-container:focus-within {
  border-color: var(--composer-border-focus);
}

.kb-picker { display: flex; flex: none; }
.composer-actions { display: flex; align-items: center; gap: 4px; flex: none; padding-bottom: 1px; }

.attach-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--rag-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background-color 150ms, color 150ms;
}

.attach-btn:hover {
  background: var(--composer-hover);
  color: var(--rag-text-regular);
}

.pill-input {
  flex: 1;
  min-width: 0;
  min-height: 38px;
  max-height: 200px;
  border: none;
  outline: none;
  resize: none;
  overflow-y: auto;
  padding: 7px 2px;
  font-size: 16px;
  line-height: 1.5;
  font-family: inherit;
  color: var(--rag-text-primary);
  background: transparent;
}

.pill-input::placeholder {
  color: var(--composer-muted);
  white-space: nowrap;
}

.send-btn-pill {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: var(--rag-border);
  color: var(--rag-text-placeholder);
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background-color 150ms, color 150ms, scale 150ms;
}

.send-btn-pill.active, .send-btn-pill.stop-receiving {
  background: var(--rag-text-primary);
  color: var(--rag-bg-primary);
  cursor: pointer;
}

.send-btn-pill.active:hover {
  scale: 1.04;
}

.send-btn-pill.active:active {
  scale: .96;
}

.disclaimer {
  width: 100%;
  max-width: 820px;
  text-align: center;
  font-size: 12px;
  color: var(--composer-muted);
  padding: 10px 0 2px;
  overflow-wrap: anywhere;
}

.response-options { display: flex; flex: none; }
.options-btn { position: relative; display: grid; place-items: center; width: 34px; height: 34px; padding: 0; border: 0; border-radius: 50%; background: transparent; color: var(--rag-text-secondary); cursor: pointer; transition: background-color 150ms, color 150ms; }
.options-btn:hover, .options-btn.is-open { background: var(--composer-hover); color: var(--rag-text-primary); }
.options-btn:focus-visible { outline: 2px solid var(--rag-primary); outline-offset: 2px; }
.options-btn:disabled { opacity: .5; cursor: not-allowed; }
.options-indicator { position: absolute; top: 4px; right: 4px; width: 5px; height: 5px; border-radius: 50%; background: currentColor; }
.composer-menu { position: absolute; z-index: 30; padding: 7px; border: 1px solid var(--composer-border); border-radius: 16px; background: var(--rag-bg-overlay); color: var(--rag-text-primary); box-shadow: var(--composer-menu-shadow); }
.composer-menu[data-placement="above"] { bottom: calc(100% + 6px); top: auto; }
.composer-menu[data-placement="below"] { top: calc(100% + 6px); bottom: auto; }
.response-options-panel { right: 10px; width: min(280px, calc(100vw - 48px)); overflow-y: auto; }
.options-heading { margin: 4px 10px 12px; font-size: 12px; color: var(--rag-text-secondary); }
.response-options-group { min-width: 0; margin: 0; padding: 0; border: 0; }
.response-options-group legend { padding: 0 10px 6px; font-size: 12px; color: var(--rag-text-secondary); }
.response-option { position: relative; display: flex; align-items: center; gap: 12px; padding: 10px; border-radius: 8px; cursor: pointer; transition: background-color 150ms; }
.response-option:hover { background: var(--composer-hover); }
.response-option.selected { background: var(--rag-success-light); box-shadow: inset 0 0 0 2px var(--rag-primary); }
.response-option:has(input:focus-visible) { outline: 2px solid var(--rag-text-secondary); outline-offset: 2px; }
/* The native radio covers the full row so pointer focus stays inside the panel
   before label activation; a 1px radio lets focusout dismiss it too early. */
.response-option input { position: absolute; inset: 0; z-index: 1; width: 100%; height: 100%; margin: 0; opacity: 0; cursor: inherit; }
.response-option-copy { display: grid; gap: 3px; flex: 1; min-width: 0; }
.response-option-title { font-size: 14px; }
.response-option-hint { font-size: 12px; line-height: 1.5; color: var(--rag-text-secondary); }
.response-option-check { flex: none; visibility: hidden; color: var(--rag-primary); }
.response-option.selected .response-option-title { font-weight: 600; }
.response-option.selected .response-option-check { visibility: visible; }
.stream-state { margin: 10px 0 0; font-size: 12px; color: var(--rag-text-secondary); }
.kb-scope-row { width: 100%; max-width: 820px; display: flex; flex-wrap: wrap; gap: 6px; padding: 0 4px 8px; }
.kb-scope-chip { display: inline-flex; align-items: center; gap: 7px; height: 30px; padding: 0 6px 0 10px; border: 1px solid var(--composer-border); border-radius: 999px; background: var(--composer-chip); color: var(--rag-text-primary); font-size: 13px; max-width: 100%; }
.kb-scope-chip svg { flex: none; color: var(--rag-text-secondary); }
.kb-scope-chip b { font-weight: 500; }
.kb-scope-chip span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kb-scope-x { width: 20px; height: 20px; flex: none; border: 0; border-radius: 50%; background: transparent; display: grid; place-items: center; cursor: pointer; }
.kb-scope-x:hover { background: var(--composer-hover); }
.kb-dropdown { left: 14px; width: min(260px, calc(100vw - 48px)); display: flex; flex-direction: column; }
.menu-label { flex: none; font-size: 12px; color: var(--composer-muted); padding: 8px 10px 4px; }
.kb-menu-scroll { min-height: 0; max-height: 300px; overflow-y: auto; margin: 0 -4px; padding: 0 4px; overscroll-behavior: contain; }
.kb-menu-item { display: flex; align-items: center; gap: 11px; width: 100%; height: 38px; padding: 0 10px; border: 0; border-radius: 10px; background: transparent; font-size: 14px; color: var(--rag-text-primary); text-align: left; cursor: pointer; }
.kb-menu-item:hover, .kb-menu-item.selected { background: var(--composer-hover); }
.kb-menu-item svg { flex: none; color: var(--rag-text-secondary); }
.kb-menu-item span { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kb-menu-item:focus-visible { outline-offset: -2px; }
.kb-load-status { padding: 10px 12px; color: var(--rag-text-secondary); font-size: 13px; }
.kb-load-status button { display: block; margin-top: 8px; color: inherit; background: transparent; border: none; cursor: pointer; text-decoration: underline; }

@media (max-width: 768px) {
  .messages-container > .kb-navigation-status { margin-top: 60px; }
  .chat-panel.is-empty { padding-bottom: 0; }
  .welcome-title { font-size: 24px; }
  .welcome-screen { padding: 0 16px; }
  .input-wrapper { padding: 6px 16px env(safe-area-inset-bottom, 0px); }
  .example-questions { grid-template-columns: 1fr; }
  .example-card { min-height: 46px; }
  .user-content {
    max-width: 85%;
  }

  .message-wrapper { padding: 16px; }
  .ai-message { gap: 10px; }
  .ai-avatar { width: 26px; height: 28px; }
  .ai-avatar :deep(svg) { width: 26px; height: 26px; }
}
</style>
