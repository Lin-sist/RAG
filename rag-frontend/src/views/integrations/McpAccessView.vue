<template>
  <main class="mcp-page">
    <header class="mcp-header"><p class="eyebrow">MCP 接入</p><h1>让你的 AI 助手用上知识库</h1><p>在你熟悉的客户端里查资料、找依据，并引用这里的知识。</p></header>
    <section class="onboarding-entry" aria-labelledby="connect-entry-title"><div><h2 id="connect-entry-title">从你正在使用的助手开始</h2><p>选择 Codex、Claude Code 或 Cursor，获取对应的接入方式。</p></div><button ref="connectTrigger" class="action primary connect-cta" @click="connectOpen = true"><Plug :size="18" aria-hidden="true" />接入 AI 助手<ArrowRight :size="16" aria-hidden="true" /></button></section>
    <ol class="onboarding-overview"><li><span>1</span><div><strong>选择助手</strong><p>找到你常用的客户端</p></div></li><li><span>2</span><div><strong>添加配置</strong><p>按专属步骤准备接入</p></div></li><li><span>3</span><div><strong>在客户端验证</strong><p>确认工具可见，再开始使用</p></div></li></ol>
    <div class="onboarding-context"><h2>接入之后，你可以这样开始</h2><p>“在我指定的知识库中查找这个问题的资料，回答时给出来源。”</p><span>检索与问答需要服务端开放对应工具；本页只帮助接入，不会执行任务。</span></div>
    <details class="advanced-diagnostics"><summary>服务状态与高级诊断<span>连接遇到问题时查看</span></summary>
    <div class="mcp-tabs" role="group" aria-label="接入内容">
      <button v-for="item in sections" :key="item.key" :aria-pressed="section === item.key" :aria-controls="'mcp-' + item.key" @click="section = item.key">{{ item.label }}</button>
    </div>
    <div class="metadata-state" role="status" aria-live="polite">
      <span v-if="metadataState === 'loading'">正在读取接入信息…</span>
      <template v-else-if="metadataState === 'error'"><span>{{ metadataError }}</span><button class="text-action" @click="loadMetadata">重试接入信息</button></template>
      <template v-else-if="metadata"><span class="state-label"><span class="state-dot" :class="{ enabled: metadata.enabled }" aria-hidden="true"></span>{{ metadata.enabled ? '已配置启用' : '服务端未启用 MCP' }}</span><span>配置不代表连接已通过</span><button class="text-action" :disabled="running" @click="loadMetadata">刷新接入信息</button></template>
    </div>

    <section v-show="section === 'connect'" id="mcp-connect" aria-label="接入配置">
      <div class="connection-box"><div class="box-heading"><h2>连接信息</h2><span class="quiet-label">Streamable HTTP · 无会话</span></div><dl class="connection-facts">
        <div><dt>服务协议路径</dt><dd><code>/mcp</code></dd></div><div><dt>认证方式</dt><dd>项目 access JWT</dd></div>
        <div><dt>外部调用工具</dt><dd>{{ metadata ? metadata.externalToolsEffective ? '配置允许，执行可能产生费用' : metadata.enabled ? '未开放 search / ask' : '服务关闭，工具均未开放' : '尚未读取配置' }}</dd></div>
        <div><dt>本机访问限制</dt><dd>{{ metadata ? metadata.localOnly ? '已配置限制' : '未配置本机限制' : '尚未读取配置' }}</dd></div>
      </dl><p class="helper">配置来自当前服务进程；页面不会修改开关或导出登录凭据。当前未实现 MCP OAuth 授权流程。</p></div>
      <div class="section-heading"><div><h2>通用连接模板</h2><p class="helper">本机开发地址示例，复制的是占位模板。</p></div><button class="action" @click="copyTemplate"><Copy :size="16" aria-hidden="true" />复制模板</button></div>
      <pre class="code-block"><code>{{ connectionTemplate }}</code></pre>
      <p v-if="copyStatus" class="copy-status" role="status" aria-live="polite">{{ copyStatus }}</p>
      <textarea v-if="manualCopy" ref="copyField" class="manual-copy" aria-label="手动复制连接模板" readonly :value="connectionTemplate" @focus="($event.target as HTMLTextAreaElement).select()"></textarea>
      <p class="helper">地址示例为 <code>http://127.0.0.1:8080/mcp</code>，只适用于对应本机环境。它不一定是当前站点或外部客户端可达的后端地址；通用参数也不等于所有客户端的配置文件格式。</p>
      <h2 class="next-heading">开始接入</h2><ol class="setup-steps"><li><strong>在服务端显式启用 MCP</strong><span>默认关闭；启用和允许 Origin 都由服务配置控制。</span></li><li><strong>在客户端安全配置连接与凭据</strong><span>客户端需支持 HTTP 和自定义请求头，凭据不必粘回此页面。</span></li><li><strong>先验证协议，再按需使用工具</strong><span>页面检查只发现能力，不读取知识库或调用模型。</span></li></ol>
      <button class="action primary" @click="section = 'check'">查看连接检查<ArrowRight :size="16" aria-hidden="true" /></button>
    </section>

    <section v-show="section === 'tools'" id="mcp-tools" aria-label="工具与资源">
      <div class="section-heading"><h2>工具目录</h2><span class="quiet-label">代码实现说明，与协议发现分开</span></div>
      <p v-if="!metadata" class="helper">读取接入信息后展示工具定义；不会用静态数据猜测当前开放状态。</p>
      <div v-else class="tool-grid"><article v-for="tool in metadata.implementedTools" :key="tool.name" class="tool-card"><div class="tool-heading"><code>{{ tool.name }}</code><span class="quiet-label">{{ metadata.configuredToolNames.includes(tool.name) ? '配置允许' : '未开放' }}</span></div><h3>{{ tool.title }}</h3><p>{{ toolDescription(tool.name) }}</p><span class="tool-cost">{{ tool.externalCallPossible ? '可能触发外部服务，具有调用成本' : '只读来源，无模型调用' }}</span><details><summary>参数与结果结构</summary><h4>输入 JSON Schema</h4><pre class="code-block"><code>{{ JSON.stringify(tool.inputSchema, null, 2) }}</code></pre><h4>输出 JSON Schema</h4><pre class="code-block"><code>{{ JSON.stringify(tool.outputSchema, null, 2) }}</code></pre></details></article></div>
      <h2 class="next-heading">资源模板</h2><p class="helper">仅展示 URI 结构，不列出实际知识库、文档或片段。</p><div v-if="metadata" class="resource-list"><details v-for="resource in metadata.implementedResourceTemplates" :key="resource.name"><summary>{{ resourceTitle(resource.name) }}</summary><code>{{ resource.uriTemplate }}</code><p class="helper">{{ resource.mimeType }} · {{ metadata.configuredResourceTemplateNames.includes(resource.name) ? '配置允许' : '服务未开放' }}</p></details></div>
    </section>

    <section v-show="section === 'check'" id="mcp-check" aria-label="连接检查">
      <div class="section-heading"><div><h2>检查当前浏览器路径</h2><p class="helper">固定请求同源 <code>/mcp</code>，不会探测其他服务器。</p></div><button v-if="running" class="action" @click="cancelCheck">取消检查</button><button v-else class="action primary" :disabled="!metadata?.enabled || metadataState !== 'ready'" @click="checkConnection"><Plug :size="16" aria-hidden="true" />{{ result ? '重新检查连接' : '检查连接' }}</button></div>
      <p class="check-budget">每次最多 5 个协议请求，最长 15 秒；不执行工具、不读取资源内容、不调用模型。</p>
      <p v-if="metadata && !metadata.enabled" class="helper">服务端未启用 MCP，请先按服务端使用说明显式配置，再刷新接入信息。</p>
      <ol class="check-steps"><li v-for="step in steps" :key="step.key"><span class="step-name"><CircleCheck v-if="step.status === 'passed'" :size="19" aria-hidden="true" /><Circle v-else :size="19" aria-hidden="true" />{{ step.label }}</span><span :class="['step-status', step.status]">{{ stepStatus(step.status) }}</span></li></ol>
      <div class="check-result" role="status" aria-live="polite"><template v-if="running">正在检查连接，请稍候…</template><template v-else-if="result"><strong>{{ mcpCheckMessage(result.category) }}</strong><span>完成时间：{{ new Date(result.completedAt).toLocaleString() }} · 仅适用于当前登录会话</span></template><template v-else>尚未执行协议检查。</template></div>
      <div v-if="result?.toolNames.length" class="discovered"><h3>本次发现的工具</h3><ul><li v-for="name in result.toolNames" :key="name"><code>{{ name }}</code></li></ul></div>
      <div v-if="result?.resourceTemplateNames.length" class="discovered"><h3>本次发现的资源模板</h3><ul><li v-for="name in result.resourceTemplateNames" :key="name"><code>{{ name }}</code></li></ul></div>
      <p class="boundary-note">检查通过只证明本次握手、Ping 与能力发现，不代表知识库权限、工具执行或生成质量已经验证，也不代表其他客户端一定可以接入。</p>
      <details class="troubleshooting"><summary>连接失败时如何恢复</summary><ul><li>默认 Origin 允许列表为空，浏览器请求可能被拒绝。即使同源也需要服务端精确允许；页面不会自动放宽配置。</li><li>核对当前站点是否把 <code>/mcp</code> 代理到后端。404 可能是代理或入口问题，不能仅据此判断服务关闭。</li><li>本机限制检查的是后端看到的网络对端。代理可达不证明远程客户端符合访问条件。</li><li>登录失效后使用现有登录流程重新登录，再手动检查；本流程不刷新凭据或自动重试。</li></ul></details>
    </section>
    </details>
    <McpConnectDialog v-if="connectOpen" :metadata="metadata" :metadata-state="metadataState" @close="connectOpen = false" />
  </main>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { ArrowRight, Circle, CircleCheck, Copy, Plug } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { getMcpMetadata } from '@/api/mcp'
import { copyText, clipboardMessage } from '@/utils/clipboard'
import { initialMcpSteps, runMcpCheck, mcpCheckMessage, McpCheckError } from '@/utils/mcpCheck'
import McpConnectDialog from '@/components/mcp/McpConnectDialog.vue'
import type { McpMetadata, McpCheckResult, CheckStepStatus } from '@/types/mcp'
const auth = useAuthStore()
const connectOpen = ref(false), connectTrigger = ref<HTMLButtonElement>()
const sections = [{ key: 'connect', label: '接入配置' }, { key: 'tools', label: '工具与资源' }, { key: 'check', label: '连接检查' }] as const
const section = ref<'connect' | 'tools' | 'check'>('connect')
const metadata = ref<McpMetadata | null>(null), metadataState = ref<'loading' | 'ready' | 'error'>('loading'), metadataError = ref('')
const running = ref(false), steps = ref(initialMcpSteps()), result = ref<McpCheckResult | null>(null)
const copyStatus = ref(''), manualCopy = ref(false), copyField = ref<HTMLTextAreaElement>()
const connectionTemplate = JSON.stringify({ url: 'http://127.0.0.1:8080/mcp', headers: { Authorization: 'Bearer <access-token>' } }, null, 2)
let active = true, generation = 0, metadataGeneration = 0, checkController: AbortController | null = null, metadataController: AbortController | null = null
function clearCheck() { generation++; checkController?.abort(); checkController = null; running.value = false; result.value = null; steps.value = initialMcpSteps() }
async function loadMetadata() {
  if (running.value) return
  const id = ++metadataGeneration, revision = auth.sessionRevision
  metadataController?.abort(); metadataController = new AbortController()
  clearCheck(); metadata.value = null; metadataState.value = 'loading'; metadataError.value = ''
  try {
    const data = await getMcpMetadata(metadataController.signal)
    if (!active || id !== metadataGeneration || revision !== auth.sessionRevision) return
    metadata.value = data; metadataState.value = 'ready'
  } catch (error) {
    if (!active || id !== metadataGeneration || revision !== auth.sessionRevision) return
    metadataState.value = 'error'
    metadataError.value = error instanceof McpCheckError ? mcpCheckMessage(error.category)
      : (error as { status?: number })?.status === 404 ? '当前后端尚未提供接入信息。' : '接入信息暂不可用，请重试或核对登录状态。'
  }
}
async function checkConnection() {
  if (running.value || metadataState.value !== 'ready' || !metadata.value?.enabled) return
  const id = ++generation, revision = auth.sessionRevision, tokenRevision = auth.tokenRevision
  const controller = new AbortController(); checkController = controller
  running.value = true; result.value = null; steps.value = initialMcpSteps()
  const current = () => active && id === generation && revision === auth.sessionRevision && tokenRevision === auth.tokenRevision
  try {
    const checked = await runMcpCheck({ metadata: metadata.value, token: auth.accessToken, signal: controller.signal,
      onStep: value => { if (current()) steps.value = value } })
    if (current()) result.value = checked
  } catch { if (current()) { result.value = { status: 'failed', category: 'METADATA_INVALID', steps: initialMcpSteps(), completedAt: new Date().toISOString(), toolNames: [], resourceTemplateNames: [] } } }
  finally { if (current()) { running.value = false; checkController = null } }
}
function cancelCheck() { checkController?.abort() }
async function copyTemplate() {
  const message = await copyText(connectionTemplate)
  if (!active) return
  copyStatus.value = clipboardMessage(message); manualCopy.value = message !== 'copied'
  if (manualCopy.value) { await nextTick(); copyField.value?.focus() }
}
function stepStatus(status: CheckStepStatus) { return ({ idle: '尚未检查', running: '检查中', passed: '通过', failed: '失败', skipped: '未执行' })[status] }
function toolDescription(name: string) { return ({ 'rag.search': '检索当前身份有权访问的知识库证据。', 'rag.ask': '基于知识库回答，不写入问答历史或查询次数。', 'rag.get-citation': '按知识库、文档和片段标识读取原文引用。', 'rag.compare-sources': '并排返回两份来源及身份对比，不执行 AI 语义比较。' } as Record<string, string>)[name] }
function resourceTitle(name: string) { return ({ 'knowledge-base': '知识库元数据', 'knowledge-base-document': '文档元数据', 'knowledge-base-document-chunk': '文档片段' } as Record<string, string>)[name] }
watch(() => [auth.sessionRevision, auth.tokenRevision], () => { connectOpen.value = false; clearCheck(); if (auth.isLoggedIn) void loadMetadata(); else { metadataGeneration++; metadataController?.abort(); metadata.value = null; metadataState.value = 'error' } }, { flush: 'sync' })
onMounted(loadMetadata)
onUnmounted(() => { active = false; metadataGeneration++; metadataController?.abort(); clearCheck() })
</script>

<style scoped>
.mcp-page.route-page { height: auto; } .mcp-page { max-width: 1120px; height: auto; min-height: 100%; margin-inline: auto; padding: 40px 40px 64px; color: var(--rag-text-primary); }
.onboarding-entry { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 30px; background: var(--rag-bg-sidebar); border: 1px solid var(--rag-border); border-radius: 16px; margin-top: 32px; } .onboarding-entry p { color: var(--rag-text-secondary); font-size: 13px; margin-top: 8px; } .connect-cta { flex: none; min-height: 44px!important; }
.onboarding-overview { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 20px; list-style: none; margin: 30px 0 40px; padding: 0; } .onboarding-overview li { display: flex; gap: 12px; align-items: flex-start; min-width: 0; } .onboarding-overview li > span { display: grid; place-items: center; flex: none; width: 26px; height: 26px; background: var(--rag-bg-hover); border-radius: 50%; font-size: 12px; } .onboarding-overview strong { font-size: 14px; font-weight: 500; } .onboarding-overview p { margin-top: 5px; font-size: 12px; color: var(--rag-text-secondary); }
.onboarding-context { padding-bottom: 34px; } .onboarding-context h2 { font-size: 15px; margin-bottom: 12px; } .onboarding-context p { font-size: 14px; } .onboarding-context > span { display: block; font-size: 12px; color: var(--rag-text-secondary); line-height: 1.75; margin-top: 10px; }
.advanced-diagnostics { padding-top: 16px; border-top: 1px solid var(--rag-border); } .advanced-diagnostics > summary { font-size: 14px; color: var(--rag-text-primary); } .advanced-diagnostics > summary > span { margin-inline-start: 12px; font-size: 12px; color: var(--rag-text-secondary); } .advanced-diagnostics[open] > summary { margin-bottom: 22px; }
@media(max-width:940px) { .onboarding-entry { align-items: flex-start; flex-direction: column; gap: 18px; padding: 24px; } .onboarding-overview { grid-template-columns: minmax(0,1fr); gap: 20px; } }
@media(max-width:500px) { .onboarding-entry { padding: 20px; } .advanced-diagnostics > summary > span { display: block; margin: 4px 0 0 16px; } }
.mcp-header { padding-inline-end: 24px; margin-bottom: 28px; }
.eyebrow { font-size: 13px; color: var(--rag-text-secondary); margin: 0 0 8px; }
h1 { font-size: 28px; font-weight: 600; margin: 0 0 10px; line-height: 1.35; } h2 { font-size: 18px; font-weight: 600; line-height: 1.5; margin: 0; } h3 { font-size: 15px; margin: 12px 0 8px; } h4 { font-size: 13px; font-weight: 500; margin: 16px 0 8px; }
p { line-height: 1.7; margin: 0; } .mcp-header > p:last-child { color: var(--rag-text-secondary); font-size: 14px; }
button { font: inherit; cursor: pointer; } button:disabled { cursor: not-allowed; opacity: .6; }
button:focus-visible, summary:focus-visible, textarea:focus-visible { outline: 2px solid var(--rag-text-primary); outline-offset: 3px; }
.mcp-tabs { display: flex; flex-wrap: wrap; gap: 24px; border-bottom: 1px solid var(--rag-border); }
.mcp-tabs button { padding: 12px 0; min-height: 44px; border: 0; border-bottom: 2px solid transparent; color: var(--rag-text-secondary); background: transparent; font-size: 14px; }
.mcp-tabs button[aria-pressed=true] { color: var(--rag-text-primary); border-bottom-color: var(--rag-primary); font-weight: 600; }
.metadata-state { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin: 20px 0 28px; color: var(--rag-text-secondary); font-size: 13px; min-height: 24px; }
.state-label { display: flex; align-items: center; gap: 7px; color: var(--rag-text-primary); } .state-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--rag-text-secondary); } .state-dot.enabled { background: var(--rag-primary); }
.text-action { border: 0; background: transparent; color: var(--rag-text-primary); text-decoration: underline; text-underline-offset: 3px; padding: 6px 0; }
.connection-box { border: 1px solid var(--rag-border); border-radius: var(--rag-radius-md); padding: 24px; margin-bottom: 32px; }
.box-heading, .section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; } .section-heading { margin-bottom: 14px; }
.quiet-label { color: var(--rag-text-secondary); font-size: 12px; } .connection-facts { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 24px; margin: 24px 0; }
dt { font-size: 12px; color: var(--rag-text-secondary); margin-bottom: 6px; } dd { margin: 0; font-size: 14px; overflow-wrap: anywhere; }
.helper { color: var(--rag-text-secondary); font-size: 13px; line-height: 1.75; } .section-heading .helper { margin-top: 6px; }
code, pre { font-family: Consolas, 'SFMono-Regular', monospace; font-size: 12px; overflow-wrap: anywhere; }
.code-block { white-space: pre-wrap; overflow-wrap: anywhere; background: var(--rag-bg-sidebar); border: 1px solid var(--rag-border); border-radius: 8px; padding: 18px; margin: 0 0 14px; line-height: 1.75; max-width: 100%; }
.action { display: inline-flex; justify-content: center; align-items: center; gap: 8px; min-height: 40px; border: 1px solid var(--rag-border-control); border-radius: 8px; padding: 8px 14px; background: var(--rag-bg-page); color: var(--rag-text-primary); font-size: 13px; }
.action:hover:not(:disabled) { background: var(--rag-bg-hover); } .action.primary { background: var(--rag-text-primary); color: var(--rag-bg-page); border-color: var(--rag-text-primary); } .action.primary:hover:not(:disabled) { opacity: .86; background: var(--rag-text-primary); }
.copy-status { margin-block: 8px 12px; font-size: 13px; } .manual-copy { display: block; width: 100%; min-height: 160px; resize: vertical; margin-bottom: 12px; padding: 12px; border: 1px solid var(--rag-border-control); border-radius: 8px; background: var(--rag-bg-page); color: var(--rag-text-primary); }
.next-heading { margin-top: 32px; } .setup-steps { padding-inline-start: 22px; margin: 18px 0 24px; } .setup-steps li { padding-inline-start: 6px; margin-block: 16px; font-size: 14px; } .setup-steps strong { font-weight: 500; } .setup-steps span { display: block; color: var(--rag-text-secondary); font-size: 13px; margin-top: 4px; }
.tool-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 16px; } .tool-card { border: 1px solid var(--rag-border); border-radius: 12px; padding: 20px; min-width: 0; }
.tool-heading { display: flex; gap: 8px; flex-wrap: wrap; justify-content: space-between; } .tool-heading code { font-size: 13px; } .tool-card > p { color: var(--rag-text-secondary); font-size: 13px; } .tool-cost { display: block; color: var(--rag-text-secondary); font-size: 12px; margin-block: 12px 16px; }
details { min-width: 0; } summary { cursor: pointer; min-height: 40px; padding-block: 8px; font-size: 13px; } .resource-list { margin-top: 16px; display: grid; gap: 10px; } .resource-list details { background: var(--rag-bg-sidebar); border-radius: 8px; padding: 8px 16px; } .resource-list code { display: block; margin-bottom: 8px; }
.check-budget { color: var(--rag-text-secondary); font-size: 13px; margin-bottom: 20px; } .check-steps { list-style: none; padding: 0; margin: 20px 0; } .check-steps li { display: flex; gap: 16px; justify-content: space-between; align-items: center; padding-block: 17px; border-bottom: 1px solid var(--rag-border); font-size: 14px; } .step-name { display: flex; align-items: center; gap: 12px; } .step-status { font-size: 13px; color: var(--rag-text-secondary); } .step-status.passed { color: var(--rag-tag-match); } .step-status.failed { font-weight: 600; color: var(--rag-text-primary); }
.check-result { padding: 18px 20px; background: var(--rag-bg-sidebar); border-radius: 10px; font-size: 14px; } .check-result strong { font-weight: 500; } .check-result span { display: block; color: var(--rag-text-secondary); font-size: 12px; margin-top: 8px; }
.discovered { margin-top: 20px; } .discovered ul { list-style: none; padding: 0; display: flex; gap: 8px 16px; flex-wrap: wrap; }
.boundary-note { margin-top: 24px; border-inline-start: 3px solid var(--rag-border-control); padding-inline-start: 14px; font-size: 13px; color: var(--rag-text-secondary); }
.troubleshooting { margin-top: 20px; } .troubleshooting li { margin-block: 10px; font-size: 13px; color: var(--rag-text-secondary); line-height: 1.75; } .troubleshooting ul { padding-inline-start: 22px; }
@media(max-width:1050px) { .mcp-page { padding: 32px 24px 48px; } .tool-grid { grid-template-columns: minmax(0,1fr); } }
@media(max-width:768px) { .mcp-page { padding-top: 68px; } }
@media(max-width:500px) { .mcp-page { padding-inline: 16px; } .connection-facts { grid-template-columns: minmax(0,1fr); gap: 18px; } .connection-box { padding: 18px; } .mcp-tabs { gap: 18px; } .action { min-height: 44px; } .mcp-header { padding-inline-end: 0; } .tool-card { padding: 16px; } .text-action { min-height: 44px; } }
</style>
