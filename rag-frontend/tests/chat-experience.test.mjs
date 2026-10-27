import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createRenderer, h, nextTick, ssrContextKey } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { createPinia } from 'pinia'
import { routerKey, routeLocationKey } from 'vue-router'
import { fileURLToPath } from 'node:url'

globalThis.localStorage = { getItem() { return null }, setItem() {}, removeItem() {} }
globalThis.document = { addEventListener() {}, removeEventListener() {} }
globalThis.window = { navigator: { userAgent: 'fixture' }, location: { href: 'http://fixture.test/' }, addEventListener() {}, removeEventListener() {}, dispatchEvent() {} }
const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] },
  server: { middlewareMode: true, hmr: { server: createHttpServer() } }, appType: 'custom',
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } }, plugins: [vue(), {
    name: 'chat-presentation-message-fixture', enforce: 'pre',
    transform(code, id) {
      if (id.includes('ChatPanel.vue') || id.endsWith('/stores/knowledgeBase.ts')) {
        return code.replace(/import \{ ElMessage \} from 'element-plus'/g, 'const ElMessage = { warning() {}, error() {}, success() {} }')
      }
    },
    resolveId(id) {
      if (id === 'element-plus') return '\0message-fixture'
      if (id === './request' || id.endsWith('/api/request')) return '\0request-fixture'
    },
    load(id) {
      if (id === '\0message-fixture') return 'export const ElMessage = { warning() {}, error() {}, success() {} }'
      if (id === '\0request-fixture') return 'const fail = () => { throw Error("No requests allowed in presentation tests") }; export default { get: fail, post: fail, put: fail, delete: fail }'
    },
  }] })
const renderer = createRenderer({
  createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ text }),
  setText(node, text) { node.text = text }, setElementText(node, text) { node.text = text },
  insert(node, parent) { node.parent = parent; parent.children.push(node) },
  remove(node) { node.parent.children = node.parent.children.filter(child => child !== node) },
  parentNode: node => node.parent, nextSibling: () => null, patchProp() {},
})
try {
  const { renderAnswerMarkdown } = await server.ssrLoadModule('/src/utils/answerMarkdown.ts')
  const { copyText, clipboardMessage } = await server.ssrLoadModule('/src/utils/clipboard.ts')
  const { default: Sources } = await server.ssrLoadModule('/src/components/chat/AnswerSources.vue')
  const { default: Chat } = await server.ssrLoadModule('/src/components/chat/ChatPanel.vue')
  const { useKnowledgeBaseStore } = await server.ssrLoadModule('/src/stores/knowledgeBase.ts')
  const { useAuthStore } = await server.ssrLoadModule('/src/stores/auth.ts')
  await test('trace view is local and identity changes clear prior traces and selection', async () => {
    const pinia = createPinia()
    useKnowledgeBaseStore(pinia).list = [{ id: 901, name: 'fixture' }]
    const app = renderer.createApp({ ...Chat, render: () => h('div') })
    app.use(pinia); app.provide(ssrContextKey, {})
    app.provide(routerKey, { push() {} }); app.provide(routeLocationKey, { fullPath: '/chat', name: 'Chat', params: {} })
    app.mount({ children: [] }); await nextTick()
    const state = app._instance.setupState
    state.inputText = 'retained-draft'; state.view = 'trace'
    state.messages = [{ id: 'u', role: 'user', content: 'question' }, { id: 'a', role: 'assistant', content: 'answer', executionTrace: { runId: 'old-run' } }]
    state.selectedTraceId = 'a'; state.view = 'conversation'; await nextTick()
    assert.equal(state.inputText, 'retained-draft')
    assert.equal(state.traceMessage.executionTrace.runId, 'old-run')
    useAuthStore(pinia).sessionRevision++
    await nextTick()
    assert.equal(state.messages.length, 0); assert.equal(state.selectedTraceId, '')
    assert.equal(state.view, 'conversation')
    app.unmount()
  })
  await test('untrusted HTML, language labels and code stay text; unsafe link schemes are rejected', () => {
    const html = renderAnswerMarkdown('<img src=x onerror=alert(1)>\n\n[bad](javascript:alert(1))\n\n[file](file:///etc/passwd)\n\n```html"><img/onerror=alert(1)>\n<script>alert("fixture")</script>\n```')
    assert.doesNotMatch(html, /<img|<script|href="(?:javascript|file):|onclick=/i)
    assert.match(html, /&lt;script&gt;/)
    assert.match(html, /&quot;&gt;&lt;img/)
    assert.equal((html.match(/<button/g) || []).length, 1)
    const link = renderAnswerMarkdown('[safe](https://example.test/path)')
    assert.match(link, /target="_blank"/); assert.match(link, /rel="noopener noreferrer"/)
  })
  await test('Markdown preserves whitespace in code and isolates wide tables in a keyboard region', () => {
    const html = renderAnswerMarkdown('```js\n  const x = "<&>";\n\n\treturn x;\n```\n\n|a|b|\n|-|-|\n|1|2|')
    assert.match(html, /<code>  const x = &quot;&lt;&amp;&gt;&quot;;\n\n\treturn x;\n<\/code>/)
    assert.match(html, /aria-label="复制代码"/)
    assert.match(html, /class="answer-table" tabindex="0" role="region"/)
    assert.match(html, /<\/table><\/div>/)
  })
  await test('copy success waits for the actual write; permission errors and missing clipboard never claim success', async () => {
    let resolveWrite, received
    const promise = copyText('  code\n', { writeText(text) { received = text; return new Promise(resolve => { resolveWrite = resolve }) } })
    let settled = false; promise.then(() => { settled = true })
    await Promise.resolve(); assert.equal(settled, false); assert.equal(received, '  code\n')
    resolveWrite(); assert.equal(await promise, 'copied')
    assert.equal(await copyText('text', {}), 'unavailable')
    assert.equal(await copyText('text', { writeText() { throw Error('permission denied') } }), 'failed')
    assert.equal(await copyText('text', { writeText() { return Promise.reject(Error('blocked')) } }), 'failed')
    assert.match(clipboardMessage('failed'), /失败.*手动/)
    assert.match(clipboardMessage('unavailable'), /不可用.*手动/)
  })
  await test('sources show only supplied fields, escape snippets and offer native keyboard details', async () => {
    const app = renderer.createApp(Sources, { resetKey: 1, citations: [
      { source: '', snippet: '<script>alert(1)</script>' },
      { source: 'fallback.txt', documentTitle: '实际标题', score: 0, snippet: '实际片段' },
    ] })
    const html = await renderToString(app)
    assert.match(html, /未知来源/); assert.match(html, /实际标题/)
    assert.doesNotMatch(html, /<script|fallback\.txt/)
    assert.match(html, /&lt;script&gt;/)
    assert.equal((html.match(/检索相关度/g) || []).length, 1)
    assert.match(html, /检索相关度 0%/)
    assert.equal((html.match(/<details/g) || []).length, 2)
    assert.match(html, /<summary\b/)
    const empty = await renderToString(renderer.createApp(Sources, { resetKey: 0, citations: [] }))
    assert.match(empty, /未返回可展示来源/); assert.doesNotMatch(empty, /<details/)
  })
  await test('reader scrolling pauses follow, receives more content without a jump and resumes only near bottom or on request', async () => {
    const pinia = createPinia()
    useKnowledgeBaseStore(pinia).list = [{ id: 901, name: 'fixture' }]
    const app = renderer.createApp({ ...Chat, render: () => h('div') })
    app.use(pinia); app.provide(ssrContextKey, {})
    app.provide(routerKey, { push() {} }); app.provide(routeLocationKey, { fullPath: '/chat', name: 'Chat', params: {} })
    app.mount({ children: [] }); await nextTick()
    const state = app._instance.setupState
    const viewport = { scrollHeight: 1800, clientHeight: 600, scrollTop: 1200 }
    state.messageViewport = viewport
    state.updateScrollPosition(); assert.equal(state.followBottom, true)
    viewport.scrollTop = 300; state.pauseUpwardScroll({ deltaY: -200 }); state.updateScrollPosition()
    assert.equal(state.followBottom, false); assert.equal(state.showScrollBottom, true)
    viewport.scrollHeight = 2400; state.scrollToBottom(); await nextTick()
    assert.equal(viewport.scrollTop, 300)
    state.scrollToBottom(true); await nextTick()
    assert.equal(viewport.scrollTop, 2400); assert.equal(state.showScrollBottom, false)
    viewport.scrollTop = 1790; state.updateScrollPosition(); assert.equal(state.followBottom, true)
    app.unmount()
  })
} finally {
  await server.close()
  delete globalThis.localStorage; delete globalThis.document; delete globalThis.window
}
