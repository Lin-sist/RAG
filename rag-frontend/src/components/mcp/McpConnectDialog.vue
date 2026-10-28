<template>
  <Teleport to="body">
    <div class="mcp-connect-backdrop" @click.self="close">
      <section ref="dialog" class="mcp-connect-dialog" role="dialog" aria-modal="true" aria-labelledby="mcp-connect-title" aria-describedby="mcp-connect-description" tabindex="-1" @keydown="handleKeys">
        <header class="dialog-header"><div><p class="dialog-eyebrow">连接你的知识库</p><h2 id="mcp-connect-title">接入 AI 助手</h2><p id="mcp-connect-description">选择你使用的客户端，按对应步骤添加知识库能力。</p></div><button class="dialog-close" aria-label="关闭接入引导" @click="close"><X :size="20" aria-hidden="true" /></button></header>
        <div class="dialog-scroll">
          <div class="client-picker" role="group" aria-label="选择 AI 客户端">
            <button v-for="client in clients" :key="client.id" :aria-pressed="selected === client.id" :aria-label="'选择 ' + client.name" @click="selected = client.id">
              <img :src="client.logo" alt="" :class="{ 'cursor-logo': client.id === 'cursor' }" width="36" height="36" />
              <span>{{ client.name }}</span><Check v-if="selected === client.id" class="selection-check" :size="15" aria-hidden="true" />
            </button>
          </div>
          <p v-if="!selected" class="selection-hint">选择上方的客户端，查看专属接入方式。</p>
          <div v-else class="client-setup">
            <p class="service-note" v-if="metadataState === 'loading'">正在读取服务状态；你可以先准备客户端配置。</p>
            <p class="service-note" v-else-if="!metadata">暂未取得服务配置。以下是接入模板，使用前请确认服务地址与开关。</p>
            <p class="service-note" v-else-if="!metadata.enabled">当前服务未启用 MCP。可以先准备配置，启用后再到客户端验证。</p>

            <div class="setup-section"><h3><span>1</span>确认服务地址</h3><label class="address-label" for="mcp-client-endpoint">MCP 服务地址</label><input id="mcp-client-endpoint" v-model="endpoint" type="url" spellcheck="false" autocomplete="off" :aria-invalid="invalid || undefined" aria-describedby="mcp-address-help mcp-address-error" />
              <p id="mcp-address-help" class="setup-help">默认值是本机开发示例。请填写所选客户端能够访问的地址。</p><p id="mcp-address-error" class="field-error" role="status">{{ invalid ? '请输入以 /mcp 结尾的 HTTP(S) 地址，不含凭据、查询参数或特殊字符。' : '' }}</p>
            </div>

            <div class="setup-section"><h3><span>2</span>在客户端本地配置凭据</h3><p>将有效的项目 access token 配置为客户端环境变量 <code>RAG_MCP_ACCESS_TOKEN</code>。</p><p class="setup-help">使用项目已有登录流程取得的访问凭据；模板只引用变量，不会复制你的登录 Token。凭据过期后需在客户端本地更新。</p>
              <details class="setup-details"><summary>如何让客户端读取环境变量</summary><p>变量需存在于运行该客户端的进程环境中。桌面应用不一定继承某个终端里临时设置的变量；配置好环境后重新启动客户端。此页面不读取变量值，也不修改你的客户端文件。</p></details>
            </div>

            <div class="setup-section"><h3><span>3</span>{{ selected === 'codex' ? '添加到 Codex' : selected === 'claude-code' ? '添加到 Claude Code' : '添加到 Cursor' }}</h3>
              <template v-if="selected === 'codex'"><p class="setup-help">配置好环境变量后，在本机终端执行下方命令。</p><pre class="setup-code"><code>{{ setup?.command ?? '请先修正服务地址。' }}</code></pre><button class="setup-action primary" :disabled="!setup" @click="copyPayload('command')"><Copy :size="16" aria-hidden="true" />复制接入命令</button><details class="setup-details"><summary>改用 TOML 配置</summary><p class="setup-help">将此项合并到 <code>~/.codex/config.toml</code>，保留已有配置。</p><pre class="setup-code"><code>{{ setup?.config }}</code></pre><button class="setup-action" :disabled="!setup" @click="copyPayload('config')">复制 TOML 配置</button></details></template>
              <template v-else-if="selected === 'claude-code'"><p class="setup-help">将下方内容合并到当前工作项目的 <code>.mcp.json</code> 中，保留已有的 mcpServers。随后启动 Claude Code，并按客户端提示确认项目 MCP。</p><pre class="setup-code"><code>{{ setup?.config ?? '请先修正服务地址。' }}</code></pre><button class="setup-action primary" :disabled="!setup" @click="copyPayload('config')"><Copy :size="16" aria-hidden="true" />复制 Claude Code 配置</button></template>
              <template v-else><p class="setup-help">打开 Cursor 的添加确认界面；确认后还需确保客户端能读取凭据变量。</p><a v-if="setup?.installUrl" class="setup-action primary" :href="setup.installUrl" rel="noreferrer noopener" @click="noteCursorHandoff"><ExternalLink :size="16" aria-hidden="true" />在 Cursor 中添加</a><button v-else class="setup-action primary" disabled>在 Cursor 中添加</button><details class="setup-details"><summary>无法打开？复制 JSON 手动添加</summary><p class="setup-help">将此项合并到 <code>.cursor/mcp.json</code> 或 <code>~/.cursor/mcp.json</code>，保留已有配置。</p><pre class="setup-code"><code>{{ setup?.config }}</code></pre><button class="setup-action" :disabled="!setup" @click="copyPayload('config')">复制 Cursor 配置</button></details></template>
              <div class="handoff-feedback" role="status" aria-live="polite">{{ feedback }}</div>
              <textarea v-if="manualText" ref="manualField" class="manual-payload" aria-label="手动复制接入内容" readonly :value="manualText" @focus="($event.target as HTMLTextAreaElement).select()"></textarea>
            </div>

            <div class="setup-section verify-section"><h3><span>4</span>到客户端确认工具可见</h3><p>{{ selected === 'claude-code' ? '在 Claude Code 中打开 /mcp，查看 enterprise-rag 的连接状态与工具。' : selected === 'codex' ? '在 Codex 的 MCP 状态或工具列表中查看 enterprise-rag，然后发出下方确认请求。' : '在 Cursor 的 MCP 设置中查看 enterprise-rag，再到 Agent 对话中确认工具可见。' }}</p><p class="setup-help">当前状态：待客户端验证。复制配置、打开安装界面或本页协议检查通过，都不代表客户端已接入。</p><button class="setup-action" @click="copyPayload('discovery')">复制工具发现提示词</button>
              <details class="setup-details"><summary>接入后，试着让助手查一次资料</summary><p class="setup-help">需要服务端开放 search / ask，且可能触发外部 embedding、rerank 或生成。请先选定你有权限使用的知识库；本页不会执行该任务。</p><pre class="setup-code"><code>{{ MCP_QUERY_PROMPT }}</code></pre><button class="setup-action" @click="copyPayload('query')">复制知识库示例任务</button></details>
            </div>
          </div>
        </div>
        <footer class="dialog-footer"><span>{{ selected ? '配置准备与客户端连接分别确认' : '支持三种客户端的 HTTP MCP 配置' }}</span><button class="setup-action" @click="close">关闭</button></footer>
      </section>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { X, Check, Copy, ExternalLink } from 'lucide-vue-next'
import type { McpMetadata } from '@/types/mcp'
import { buildMcpClientSetup, normalizeMcpEndpoint, MCP_DEFAULT_ENDPOINT, MCP_DISCOVERY_PROMPT, MCP_QUERY_PROMPT, type McpClient } from '@/utils/mcpOnboarding'
import { copyText, clipboardMessage } from '@/utils/clipboard'
import codexLogo from '@/assets/mcp-clients/codex-color.svg'
import claudeLogo from '@/assets/mcp-clients/claudecode-color.svg'
import cursorLogo from '@/assets/mcp-clients/cursor.svg'
defineProps<{ metadata: McpMetadata | null; metadataState: 'loading' | 'ready' | 'error' }>()
const emit = defineEmits<{ (event: 'close'): void }>()
const clients = [{ id: 'codex' as const, name: 'Codex', logo: codexLogo }, { id: 'claude-code' as const, name: 'Claude Code', logo: claudeLogo }, { id: 'cursor' as const, name: 'Cursor', logo: cursorLogo }]
const selected = ref<McpClient | null>(null), endpoint = ref(MCP_DEFAULT_ENDPOINT)
const invalid = computed(() => !normalizeMcpEndpoint(endpoint.value))
const setup = computed(() => selected.value && !invalid.value ? buildMcpClientSetup(selected.value, endpoint.value) : null)
const dialog = ref<HTMLElement>(), manualField = ref<HTMLTextAreaElement>(), feedback = ref(''), manualText = ref('')
let active = true, copyGeneration = 0, background: HTMLElement | null = null, previousInert = false, previousOverflow = ''
let previousFocus: HTMLElement | null = null
function close() { emit('close') }
watch([selected, endpoint], () => { copyGeneration++; feedback.value = ''; manualText.value = '' })
async function copyPayload(kind: 'command' | 'config' | 'discovery' | 'query') {
  const text = kind === 'discovery' ? MCP_DISCOVERY_PROMPT : kind === 'query' ? MCP_QUERY_PROMPT : kind === 'command' ? setup.value?.command : setup.value?.config
  if (!text) return
  const generation = ++copyGeneration
  const result = await copyText(text)
  if (!active || generation !== copyGeneration) return
  feedback.value = result === 'copied' ? kind === 'command' || kind === 'config' ? '配置已准备，待客户端添加与验证。' : '提示词已复制，请到客户端使用。' : clipboardMessage(result)
  manualText.value = result === 'copied' ? '' : text
  if (manualText.value) { await nextTick(); if (active && generation === copyGeneration) manualField.value?.focus() }
}
function noteCursorHandoff() { copyGeneration++; manualText.value = ''; feedback.value = '已发起打开 Cursor 的请求，请在客户端确认添加；连接状态仍待验证。' }
function handleKeys(event: KeyboardEvent) {
  if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); close(); return }
  if (event.key !== 'Tab') return
  const controls = [...dialog.value?.querySelectorAll<HTMLElement>('button:not([disabled]), a[href], input:not([disabled]), textarea, summary') ?? []].filter(e => e.getClientRects().length)
  const first = controls[0], last = controls[controls.length - 1]
  if (!first) { event.preventDefault(); dialog.value?.focus(); return }
  if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog.value)) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
}
onMounted(async () => {
  previousFocus = document.activeElement as HTMLElement | null
  previousOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden'
  background = document.getElementById('app'); if (background) { previousInert = background.inert; background.inert = true }
  await nextTick(); if (active) dialog.value?.querySelector<HTMLButtonElement>('.client-picker button')?.focus()
})
onUnmounted(() => { active = false; copyGeneration++; if (background) background.inert = previousInert; document.body.style.overflow = previousOverflow; if (previousFocus?.isConnected && previousFocus.getClientRects().length) previousFocus.focus() })
</script>

<style scoped>
.mcp-connect-backdrop { position: fixed; inset: 0; z-index: 110; background: rgb(0 0 0 / .5); display: grid; place-items: center; padding: 24px; font-family: var(--rag-font-family); color: var(--rag-text-primary); }
.mcp-connect-dialog { width: min(640px,100%); max-height: calc(100dvh - 48px); display: flex; flex-direction: column; background: var(--rag-bg-overlay); border: 1px solid var(--rag-border); border-radius: 18px; box-shadow: var(--rag-shadow-lg); overflow: hidden; outline-offset: 3px; }
.dialog-header { padding: 26px 28px 20px; display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; } .dialog-eyebrow { margin: 0 0 6px; font-size: 12px; color: var(--rag-text-secondary); } h2 { margin: 0 0 8px; font-size: 24px; line-height: 1.35; font-weight: 600; } .dialog-header p:last-child { margin: 0; font-size: 13px; line-height: 1.7; color: var(--rag-text-secondary); }
button, a, input, textarea { font: inherit; } button, summary, a { cursor: pointer; } button:disabled { cursor: not-allowed; opacity: .55; } button:focus-visible, a:focus-visible, summary:focus-visible, input:focus-visible, textarea:focus-visible { outline: 2px solid var(--rag-text-primary); outline-offset: 3px; }
.dialog-close { display: grid; place-items: center; width: 40px; height: 40px; flex: none; border: 0; border-radius: 9px; background: transparent; color: var(--rag-text-secondary); }
.dialog-scroll { padding: 0 28px 24px; overflow-y: auto; overscroll-behavior: contain; min-height: 0; }
.client-picker { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 12px; padding: 4px; margin: -4px; }
.client-picker button { position: relative; display: flex; flex-direction: column; justify-content: center; align-items: center; gap: 12px; min-height: 112px; padding: 18px 8px; border: 1px solid var(--rag-border-control); border-radius: 12px; background: var(--rag-bg-overlay); color: var(--rag-text-primary); font-size: 13px; text-align: center; }
.client-picker button[aria-pressed=true] { border-color: var(--rag-text-primary); background: var(--rag-bg-hover); box-shadow: inset 0 0 0 1px var(--rag-text-primary); } .selection-check { position: absolute; top: 9px; inset-inline-end: 9px; } .client-picker img { flex: none; object-fit: contain; } :global(html.dark .mcp-connect-dialog .cursor-logo) { filter: invert(1); }
.selection-hint { text-align: center; font-size: 13px; line-height: 1.8; color: var(--rag-text-secondary); margin: 24px 0 8px; }
.service-note { margin: 20px 0 0; padding-inline-start: 12px; border-inline-start: 2px solid var(--rag-border-control); color: var(--rag-text-secondary); font-size: 13px; line-height: 1.75; }
.setup-section { margin-top: 26px; } h3 { display: flex; align-items: center; gap: 10px; font-size: 14px; font-weight: 600; margin: 0 0 14px; } h3 > span { display: grid; place-items: center; width: 23px; height: 23px; flex: none; border-radius: 50%; background: var(--rag-bg-hover); font-size: 12px; font-weight: 500; }
.setup-section > p, .setup-details > p { font-size: 13px; line-height: 1.8; margin: 0 0 10px; } .setup-help { color: var(--rag-text-secondary); font-size: 12px!important; } .address-label { display: block; font-size: 12px; margin-bottom: 7px; } input { width: 100%; min-height: 42px; border: 1px solid var(--rag-border-control); border-radius: 8px; color: var(--rag-text-primary); background: var(--rag-bg-input); padding: 9px 12px; font-size: 13px; } .field-error { min-height: 0; color: var(--rag-text-primary); font-size: 12px; } input[aria-invalid=true] { border-width: 2px; }
.setup-code { background: var(--rag-bg-sidebar); border: 1px solid var(--rag-border); border-radius: 8px; padding: 14px; white-space: pre-wrap; overflow-wrap: anywhere; font: 12px/1.8 Consolas,monospace; margin: 12px 0; max-width: 100%; } code { font-family: Consolas,monospace; overflow-wrap: anywhere; }
.setup-action { display: inline-flex; align-items: center; justify-content: center; gap: 8px; min-height: 40px; padding: 9px 13px; border: 1px solid var(--rag-border-control); border-radius: 8px; color: var(--rag-text-primary); background: var(--rag-bg-overlay); text-decoration: none; font-size: 13px; line-height: 1.5; } .setup-action.primary { color: var(--rag-bg-page); background: var(--rag-text-primary); border-color: var(--rag-text-primary); }
.setup-details { margin-top: 14px; font-size: 12px; } summary { min-height: 40px; padding-block: 10px; color: var(--rag-text-secondary); } .handoff-feedback { font-size: 12px; line-height: 1.8; margin-top: 10px; color: var(--rag-text-secondary); } .manual-payload { width: 100%; min-height: 140px; resize: vertical; margin-top: 10px; padding: 10px; border: 1px solid var(--rag-border-control); border-radius: 8px; background: var(--rag-bg-input); color: var(--rag-text-primary); font-size: 12px; }
.verify-section { padding-top: 22px; border-top: 1px solid var(--rag-border); } .dialog-footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 16px 28px; border-top: 1px solid var(--rag-border); } .dialog-footer > span { color: var(--rag-text-secondary); font-size: 12px; line-height: 1.6; }
@media(hover:hover) { .dialog-close:hover,.client-picker button:hover { background: var(--rag-bg-hover); } .setup-action:hover { opacity: .85; } }
@media(max-width:520px) { .mcp-connect-backdrop { padding: 12px; } .mcp-connect-dialog { max-height: calc(100dvh - 24px); border-radius: 16px; } .dialog-header { padding: 20px 18px 18px; } .dialog-scroll { padding-inline: 18px; } .dialog-footer { padding: 14px 18px; } .client-picker { gap: 8px; } .client-picker button { min-height: 104px; padding-inline: 5px; font-size: 12px; } .client-picker img { width: 32px; height: 32px; } input { font-size: 16px; } .setup-action,summary,.dialog-close { min-height: 44px; } }
</style>
