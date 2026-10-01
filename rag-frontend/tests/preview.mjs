// Local synthetic preview only. Login/ask return fixtures; unknown API calls fail closed.
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'
import { readFile } from 'node:fs/promises'
const root = fileURLToPath(new URL('../', import.meta.url))
const kb = { id: 901, name: '合成知识库 · 项目文档', description: '仅用于前端验证，不连接真实后端', documentCount: 2, isPublic: false, vectorCollection: 'fixture_collection', vectorModel: 'fixture-model', vectorDimension: 1024, createdAt: '2026-09-20T10:00:00', updatedAt: '2026-09-20T10:00:00' }
const longAnswer = [
  '## 从资料到回答',
  '这是一条合成回答，用于检查长内容排版。检索、生成和来源展示是不同的环节，请根据返回片段核对回答。',
  '### 一个小例子',
  '```javascript\nconst documents = ["合成说明文档", "较长的资料标题"];\nconst result = documents.map((title, index) => ({ title, index, note: "这是一条很长的代码行，用于检验代码区域自身横向滚动，页面不应横向溢出" }));\nconsole.log(result);\n```',
  '| 环节 | 作用 | 本轮验证 |\n| --- | --- | --- |\n| 检索 | 寻找相关片段 | 仅合成展示 |\n| 生成 | 组织文本 | 仅合成展示 |\n| 来源 | 帮助核对 | 独立展开 |',
  '> 这里的内容仅用于前端展示验证，不代表真实 provider 或质量结果。',
  ...Array.from({ length: 10 }, (_, i) => `### 阅读要点 ${i + 1}\n\n先观察资料中的具体事实，再检查回答是否越过来源边界。长内容应允许向上阅读；新的文本到达时，页面不应强制回到底部。\n\n- 核对标题与片段\n- 区分相关度与正确率\n- 保留未验证项`),
].join('\n\n')
const sourceSnippet = '这是一段合成来源片段，展示返回字段中的实际文本。\n' + '片段较长时，用户可以展开完整内容、向上阅读，再主动回到底部。检索相关度不代表答案正确率，也不保证逐句对应。'.repeat(8)
const sourceCitations = [
  { source: '合成说明文档', documentTitle: '项目检索与回答说明 · 合成来源', score: 0.87, snippet: sourceSnippet, startIndex: 0, endIndex: sourceSnippet.length },
  { source: '', snippet: '<script>alert("合成片段不执行")</script>\n此来源缺失标题和分数，保留中性降级。', startIndex: 0, endIndex: 0 },
]
function chatFixture(scenario) {
  const answer = scenario === 'chat-long' || scenario === 'chat-scroll' ? longAnswer
    : scenario === 'chat-xss' ? '<img src=x onerror=alert(1)>\n\n[危险链接](javascript:alert(1))\n\n```html\n<script>alert("仅文本")</script>\n```'
    : '资料可以帮助你核对回答中的事实。下面的来源片段仅用于合成展示验证。'
  const citations = scenario === 'chat-long-title' ? [{ ...sourceCitations[0], documentTitle: '较长来源标题用于核对自动换行与完整阅读。'.repeat(15) }]
    : ['chat-sources', 'chat-long', 'chat-scroll'].includes(scenario) ? sourceCitations
    : scenario === 'chat-missing' ? [sourceCitations[1]] : []
  return { question: '合成问题', answer, citations, contexts: [], metadata: { status: 'ANSWER' } }
}
function streamFixture(req, res, scenario) {
  if (req.headers['x-rag-stream-contract'] !== 'structured-v1') { res.statusCode = 400; res.end('Fixture requires structured-v1'); return }
  res.setHeader('Content-Type', 'text/event-stream; charset=utf-8')
  res.setHeader('Cache-Control', 'no-cache')
  const frame = (name, payload) => `event:${name}\n${(name === 'text' ? payload : JSON.stringify(payload)).split('\n').map(line => 'data:' + line).join('\n')}\n\n`
  const terminal = { schemaVersion: 'structured-v1', finalState: 'ANSWER', reason: 'NONE', citations: sourceCitations, metadata: {}, classifierVersion: 'legacy', effectiveStrategy: 'legacy', policyVersion: 'legacy', routeReason: 'LEGACY', budgetOutcome: null, usage: null }
  const finalState = { 'chat-no-answer': 'NO_ANSWER', 'chat-unsupported': 'UNSUPPORTED', 'chat-invalid': 'INVALID', 'chat-error': 'ERROR', 'chat-cancelled': 'CANCELLED' }[scenario]
  if (finalState) Object.assign(terminal, { finalState, reason: 'FIXTURE_REASON', citations: [] })
  const text = scenario === 'chat-scroll' || scenario === 'chat-stop' ? longAnswer : '这是一段已收到的合成文本。'
  const chunks = text.match(/[\s\S]{1,100}/g) || []
  let index = 0
  const timer = setInterval(() => {
    if (res.destroyed) { clearInterval(timer); return }
    if (index < chunks.length) { res.write(frame('text', chunks[index++])); return }
    clearInterval(timer)
    if (scenario === 'chat-read-failure') { res.destroy(); return }
    if (scenario !== 'chat-incomplete') {
      res.write(frame('terminal', scenario === 'chat-invalid-terminal' ? { ...terminal, schemaVersion: 'invalid' } : terminal))
      if (scenario === 'chat-duplicate-terminal') res.write(frame('terminal', terminal))
      if (scenario === 'chat-post-terminal') res.write(frame('text', '终态之后的文本'))
      if (scenario === 'chat-unknown-event') res.write(frame('unexpected', '未知事件'))
    }
    res.end()
  }, scenario === 'chat-scroll' || scenario === 'chat-stop' ? 300 : 80)
  res.on('close', () => clearInterval(timer))
}
const streamScenarios = new Set(['chat-stream', 'chat-scroll', 'chat-stop', 'chat-incomplete', 'chat-read-failure', 'chat-no-answer', 'chat-unsupported', 'chat-invalid', 'chat-error', 'chat-cancelled', 'chat-invalid-terminal', 'chat-duplicate-terminal', 'chat-post-terminal', 'chat-unknown-event'])
const server = await createServer({ root, configFile: false, plugins: [vue(), {
  name: 'synthetic-api',
  transformIndexHtml(html, context) {
    const fixture = new URL(context.originalUrl || context.path, 'http://127.0.0.1').searchParams.get('fixture')
    if (fixture !== 'clipboard-failure' && fixture !== 'clipboard-unavailable') return html
    const replacement = fixture === 'clipboard-failure' ? '{ writeText: () => Promise.reject(new Error("Synthetic clipboard denial")) }' : 'undefined'
    return { html, tags: [{ tag: 'script', attrs: { type: 'module' }, injectTo: 'head-prepend', children: `Object.defineProperty(navigator, "clipboard", { configurable: true, value: ${replacement} }); document.documentElement.dataset.clipboardFixture = "${fixture}";` }] }
  },
  configureServer(server) {
    server.middlewares.use((req, res, next) => {
      const url = req.url?.split('?')[0]
      if (url?.startsWith('/__demo/')) {
        const name = url.slice('/__demo/'.length) || 'index.html'
        const allowed = { 'index.html': 'text/html; charset=utf-8', 'css/app.css': 'text/css', 'js/app.js': 'text/javascript', 'favicon.svg': 'image/svg+xml' }
        if (!allowed[name]) { res.statusCode = 404; res.end(); return }
        readFile(new URL('../../prototype/chatgpt-ui-demo/' + name, import.meta.url)).then(content => {
          res.setHeader('Content-Type', allowed[name]); res.end(content)
        }).catch(() => { res.statusCode = 404; res.end() })
        return
      }
      if (!url?.startsWith('/api/') && !url?.startsWith('/auth/')) return next()
      let data, status = 200
      const scenario = new URL(req.headers.referer || 'http://127.0.0.1').searchParams.get('fixture')
      if ((url === '/auth/login' || url === '/api/qa/ask') && req.method !== 'POST') { res.statusCode = 405; res.end('Fixture requires POST'); return }
      if (url === '/api/qa/ask/stream' && req.method === 'POST' && streamScenarios.has(scenario)) { streamFixture(req, res, scenario); return }
      if (req.method !== 'GET' && !(req.method === 'POST' && (url === '/auth/login' || url === '/api/qa/ask'))) {
        res.statusCode = 405; res.end('Synthetic preview is read-only'); return
      }
      if (url === '/auth/login') data = { accessToken: 'fixture-access', refreshToken: 'fixture-refresh', userInfo: { id: 901, username: '合成验收', email: '' } }
      else if (url === '/api/knowledge-bases') {
        data = scenario === 'kb-empty' ? [] : [kb]
        if (scenario === 'kb-error') status = 503
      }
      else if (url === '/api/knowledge-bases/901') data = kb
      else if (url === '/api/knowledge-bases/901/statistics') data = { kbId: 901, documentCount: 2, vectorCount: 12, queryCount: 0 }
      else if (url === '/api/knowledge-bases/901/documents') data = ['COMPLETED', 'RECONCILIATION_REQUIRED'].map((state, i) => ({ id: i + 1, kbId: 901, title: ['合成说明文档', '需要核对的合成文档'][i], status: state, fileType: 'txt', chunkCount: 6, createdAt: kb.createdAt }))
      else if (url === '/api/history') {
        const records = scenario === 'history-long' ? Array.from({ length: 20 }, (_, i) => ({ id: 901 + i, kbId: 901, question: '合成问答 ' + (i + 1) + ' · 较长标题用于检验侧栏截断与滚动，不代表真实业务记录', answer: '合成历史回答', citations: [], createdAt: kb.createdAt })) : []
        data = { records, total: records.length, page: 1, size: 20, totalPages: records.length ? 1 : 0 }
        if (scenario === 'history-error') status = 503
      }
      else if (url === '/api/history/901') data = { id: 901, kbId: 901, userId: 901, question: '这是一条合成历史问题', answer: chatFixture(scenario).answer, citations: chatFixture(scenario).citations, createdAt: kb.createdAt, traceId: 'fixture-trace', latencyMs: 0 }
      else if (url === '/api/history/901/feedback') data = []
      else if (url === '/api/qa/ask') data = chatFixture(scenario)
      else status = 404
      res.statusCode = status
      res.setHeader('Content-Type', 'application/json; charset=utf-8')
      const send = () => res.end(JSON.stringify(status === 200 ? { code: 200, data } : { errorCode: 'FIXTURE_ONLY', message: '合成预览错误，未连接真实服务' }))
      if (scenario === 'loading' || (scenario === 'chat-loading' && url === '/api/qa/ask')) setTimeout(send, 4000)
      else send()
    })
  },
}], resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } }, server: { host: '127.0.0.1', port: 5188, strictPort: true } })
await server.listen()
console.log('Synthetic preview: http://127.0.0.1:5188/login')
