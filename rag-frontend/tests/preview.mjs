// Local synthetic preview only. Login/ask return fixtures; unknown API calls fail closed.
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'
import { readFile } from 'node:fs/promises'
import { randomUUID } from 'node:crypto'
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
const f4Scenarios = new Set(['f4', 'f4-empty', 'f4-list-error', 'f4-stats-error', 'f4-docs-error', 'f4-403', 'f4-404', 'f4-upload-mixed', 'f4-upload-unconfirmed', 'f4-poll-error', 'f4-task-running', 'f4-task-failed', 'f4-task-cancelled', 'f4-task-matrix', 'f4-delete-error', 'f4-refresh-error', 'f4-long'])
const f4Sessions = new Map()
function newF4Session(scenario) {
  const bases = [kb, { ...kb, id: 902, name: '合成知识库 · 阅读资料', isPublic: true }, { ...kb, id: 903, name: '合成知识库 · 空资料', documentCount: 0 }].map(item => ({ ...item }))
  if (scenario === 'f4-long') { bases[0].name = '合成长名称用于核对完整阅读与窄屏布局'.repeat(8); bases[0].description = '这是合成描述，不代表业务数据。'.repeat(35); bases[0].vectorCollection = 'fixture_long_collection_'.repeat(12) }
  const states = ['COMPLETED', 'RECONCILIATION_REQUIRED', 'PROCESSING', 'FAILED', 'PENDING']
  const docs = states.map((state, i) => ({ id: 1 + i, kbId: 901, title: ['合成说明文档', '需要核对的合成文档', '正在处理的合成文档', '失败的合成文档', '等待的合成文档'][i], status: state, fileType: i === 1 ? 'md' : 'txt', chunkCount: state === 'COMPLETED' ? 6 : 0, createdAt: kb.createdAt, updatedAt: kb.updatedAt }))
  bases[0].documentCount = docs.length
  return { scenario, bases: scenario === 'f4-empty' ? [] : bases, docs, tasks: new Map(), nextId: 910, deleted: false }
}
function fixtureReply(res, status, data, message = '合成预览错误，未连接真实服务') {
  res.statusCode = status; res.setHeader('Content-Type', 'application/json; charset=utf-8')
  res.end(JSON.stringify(status < 400 ? { code: status, data } : { errorCode: 'FIXTURE_ONLY', message }))
}
async function fixtureBody(req, max = 262144) {
  let size = 0; const parts = []
  for await (const chunk of req) { size += chunk.length; if (size > max) throw Error('fixture body limit'); parts.push(chunk) }
  return Buffer.concat(parts)
}
async function f4Api(req, res, url, session) {
  const method = req.method, scene = session.scenario
  if (url === '/api/knowledge-bases' && method === 'GET') return fixtureReply(res, scene === 'f4-list-error' || (scene === 'f4-refresh-error' && session.deleted) ? 503 : 200, session.bases)
  if (url === '/api/knowledge-bases' && method === 'POST') {
    const draft = JSON.parse((await fixtureBody(req)).toString())
    if (typeof draft.name !== 'string' || draft.name.trim().length < 2) return fixtureReply(res, 400, null)
    const created = { ...kb, id: session.nextId++, name: draft.name, description: draft.description || '', isPublic: draft.isPublic === true, documentCount: 0 }
    session.bases.unshift(created); return fixtureReply(res, 201, created)
  }
  const taskMatch = url.match(/^\/api\/tasks\/(fixture-[\w-]+)$/)
  if (taskMatch && method === 'GET') {
    const task = session.tasks.get(taskMatch[1]); if (!task) return fixtureReply(res, 404, null)
    task.calls++
    if ((scene === 'f4-poll-error' || (scene === 'f4-task-matrix' && task.doc.title === 'query.txt')) && task.calls === 1) return fixtureReply(res, 503, null, '合成状态查询失败')
    const state = scene === 'f4-task-matrix'
      ? task.doc.title === 'running.txt' ? 'RUNNING' : task.doc.title === 'failed.txt' ? 'FAILED' : task.doc.title === 'cancelled.txt' ? 'CANCELLED' : 'COMPLETED'
      : scene === 'f4-task-running' || task.calls < 3 ? 'RUNNING' : scene === 'f4-task-failed' ? 'FAILED' : scene === 'f4-task-cancelled' ? 'CANCELLED' : 'COMPLETED'
    if (state === 'COMPLETED') { task.doc.status = 'COMPLETED'; task.doc.chunkCount = 6 }
    else if (state === 'FAILED') task.doc.status = 'FAILED'
    return fixtureReply(res, 200, { taskId: taskMatch[1], taskType: 'DOCUMENT_INDEX', state, progress: state === 'COMPLETED' ? 100 : 36, message: '合成任务状态，仅验证客户端', error: state === 'FAILED' ? '合成索引失败，未执行真实索引' : null, result: null, createdAt: kb.createdAt, updatedAt: kb.updatedAt })
  }
  const match = url.match(/^\/api\/knowledge-bases\/(\d+)(?:\/(statistics|documents)(?:\/(\d+))?)?$/)
  if (!match) return fixtureReply(res, method === 'GET' ? 404 : 405, null)
  const id = Number(match[1]), base = session.bases.find(item => item.id === id)
  if (!base || scene === 'f4-404') return fixtureReply(res, 404, null)
  if (scene === 'f4-403') return fixtureReply(res, 403, null)
  if (!match[2]) {
    if (method === 'GET') return fixtureReply(res, 200, base)
    if (method === 'PUT') {
      const draft = JSON.parse((await fixtureBody(req)).toString())
      if (scene === 'f4-delete-error') return fixtureReply(res, 403, null, '合成写权限拒绝，输入应保留')
      for (const key of ['name', 'description', 'isPublic']) if (draft[key] !== undefined) base[key] = draft[key]
      return fixtureReply(res, 200, base)
    }
    if (method === 'DELETE') {
      if (scene === 'f4-delete-error') return fixtureReply(res, 403, null, '合成删除被拒绝')
      session.bases = session.bases.filter(item => item.id !== id); session.docs = session.docs.filter(item => item.kbId !== id)
      return fixtureReply(res, 200, null)
    }
  }
  if (match[2] === 'statistics' && !match[3] && method === 'GET') {
    if (scene === 'f4-stats-error' || (scene === 'f4-refresh-error' && session.deleted)) return fixtureReply(res, 503, null, '合成统计暂不可用')
    const docs = session.docs.filter(item => item.kbId === id)
    return fixtureReply(res, 200, { kbId: id, documentCount: docs.length, vectorCount: docs.filter(item => item.status === 'COMPLETED').reduce((n, item) => n + item.chunkCount, 0), queryCount: 0 })
  }
  if (match[2] === 'documents') {
    if (!match[3] && method === 'GET') return fixtureReply(res, scene === 'f4-docs-error' ? 503 : 200, session.docs.filter(item => item.kbId === id))
    if (!match[3] && method === 'POST') {
      if (!req.headers['content-type']?.startsWith('multipart/form-data;')) return fixtureReply(res, 400, null)
      const body = (await fixtureBody(req)).toString('utf8')
      const names = [...body.matchAll(/name="file"; filename="([^"\r\n]+)"/g)]
      if (names.length !== 1) return fixtureReply(res, 400, null)
      const name = names[0][1]
      if (scene === 'f4-upload-mixed' && name.includes('fail')) return fixtureReply(res, 400, null, '合成文件拒绝，失败项应保留')
      if (scene === 'f4-upload-unconfirmed' || (scene === 'f4-task-matrix' && name === 'unknown.txt')) return fixtureReply(res, 202, { taskId: '', fileName: name })
      const documentId = session.nextId++, taskId = `fixture-task-${documentId}`
      const doc = { id: documentId, kbId: id, title: name, fileType: 'txt', status: 'PROCESSING', chunkCount: 0, createdAt: kb.createdAt, updatedAt: kb.updatedAt }
      session.docs.push(doc); base.documentCount++; session.tasks.set(taskId, { doc, calls: 0 })
      return fixtureReply(res, 202, { documentId, taskId, fileName: name, fileType: 'txt', status: 'PROCESSING' })
    }
    if (match[3] && method === 'DELETE') {
      if (scene === 'f4-delete-error') return fixtureReply(res, 403, null, '合成删除被拒绝')
      const docId = Number(match[3]); if (!session.docs.some(item => item.id === docId && item.kbId === id)) return fixtureReply(res, 404, null)
      session.docs = session.docs.filter(item => !(item.id === docId && item.kbId === id)); base.documentCount--; session.deleted = true
      return fixtureReply(res, 200, null)
    }
  }
  return fixtureReply(res, 405, null)
}
const f5Scenarios = new Set(['f5-ready', 'f5-empty', 'f5-error', 'f5-loading', 'f5-long', 'f5-delete-error', 'f5-partial', 'f5-limited', 'f5-refresh-error', 'f5-feedback-error', 'f5-feedback-readback', 'f5-feedback-unknown', 'f5-feedback-duplicate', 'f5-my-error', 'f5-my-empty', 'f5-404'])
const f5Sessions = new Map()
function newF5Session(scenario) {
  const records = scenario === 'f5-empty' ? [] : Array.from({ length: 43 }, (_, i) => ({ id: 901 + i, userId: 901, kbId: i % 3 === 0 ? 902 : 901,
    question: i === 0 && scenario === 'f5-long' ? '较长的合成问题用于检验窄屏阅读与截断'.repeat(12) : ['如何排查检索结果为空？', '文档上传后什么时候可以提问？', '如何查看回答的来源？', '怎样为知识库选择合适的资料？'][i % 4] + ` · 合成记录 ${i + 1}`,
    answer: '## 合成历史回答\n\n这是一条保存的单轮问答，仅用于客户端验收。\n\n- 查看资料范围\n- 核对原始来源\n\n不会触发真实问答或生成模型。', citations: sourceCitations, createdAt: new Date(Date.UTC(2026, 9, 3, 7) - i * 9000000).toISOString(), traceId: 'fixture-trace', latencyMs: 0 }))
  const feedbacks = scenario === 'f5-my-empty' ? [] : [{ id: 1, qaId: 902, userId: 901, rating: 4, comment: scenario === 'f5-long' ? '这是一条较长的合成评论，用于核对完整阅读与换行。'.repeat(20) : '来源清楚，方便核对。', createdAt: '2026-10-03T10:00:00' }, { id: 2, qaId: 940, userId: 901, rating: 3, comment: '希望有更多说明。', createdAt: '2026-10-02T10:00:00' }]
  return { scenario, records, feedbacks, deleted: false, wrote: false, attempts: 0 }
}
async function f5Api(req, res, pathname, session) {
  const method = req.method, scene = session.scenario, query = new URL(req.url, 'http://127.0.0.1').searchParams
  if (pathname === '/api/knowledge-bases' && method === 'GET') return fixtureReply(res, 200, [kb, { ...kb, id: 902, name: '合成资料库' }])
  if (pathname === '/api/history' && method === 'GET') {
    const page = Number(query.get('page') || 1), size = Number(query.get('size') || 20), kbId = query.get('kbId')
    if (!Number.isSafeInteger(page) || page < 1 || !Number.isSafeInteger(size) || size < 1 || size > 100 || (kbId && !/^[1-9]\d*$/.test(kbId))) return fixtureReply(res, 400, null)
    if (scene === 'f5-loading') await new Promise(resolve => setTimeout(resolve, 4000))
    if (scene === 'f5-error' || (scene === 'f5-refresh-error' && session.deleted)) return fixtureReply(res, 503, null, '合成历史读取失败')
    const all = session.records.filter(record => !kbId || record.kbId === Number(kbId))
    return fixtureReply(res, 200, { records: all.slice((page - 1) * size, page * size), total: all.length, page, size, totalPages: Math.ceil(all.length / size) })
  }
  if (pathname === '/api/history/feedback/my' && method === 'GET') return fixtureReply(res, scene === 'f5-my-error' ? 503 : 200, session.feedbacks)
  const match = pathname.match(/^\/api\/history\/(\d+)(\/feedback)?$/)
  if (!match) return fixtureReply(res, method === 'GET' ? 404 : 405, null)
  const id = Number(match[1]), record = session.records.find(item => item.id === id)
  if (!record || scene === 'f5-404') return fixtureReply(res, 404, null, '合成历史记录不存在')
  if (!match[2]) {
    if (method === 'GET') return fixtureReply(res, 200, record)
    if (method === 'DELETE') {
      session.attempts++
      if (scene === 'f5-delete-error' || (scene === 'f5-partial' && id % 2 === 0)) return fixtureReply(res, 403, null, '合成删除拒绝')
      if (scene === 'f5-limited' && session.attempts >= 3) return fixtureReply(res, 429, null, '合成限流，停止后续删除')
      session.records = session.records.filter(item => item.id !== id); session.deleted = true
      return fixtureReply(res, 200, null)
    }
  }
  if (match[2]) {
    if (method === 'GET') {
      if (scene === 'f5-feedback-error' || (scene === 'f5-feedback-readback' && session.wrote)) return fixtureReply(res, 503, null, '合成反馈读取失败')
      return fixtureReply(res, 200, session.feedbacks.filter(item => item.qaId === id))
    }
    if (method === 'POST') {
      const body = JSON.parse((await fixtureBody(req, 4096)).toString())
      if (!Number.isInteger(body.rating) || body.rating < 1 || body.rating > 5 || (body.comment !== undefined && (typeof body.comment !== 'string' || body.comment.length > 500))) return fixtureReply(res, 400, null)
      if (scene === 'f5-feedback-unknown') return fixtureReply(res, 503, null, '合成写入响应不明，不自动重发')
      if (scene === 'f5-feedback-duplicate' || session.feedbacks.some(item => item.qaId === id)) {
        if (!session.feedbacks.some(item => item.qaId === id)) session.feedbacks.push({ id: 50, qaId: id, userId: 901, rating: 4, comment: '合成已保存反馈', createdAt: kb.createdAt })
        res.statusCode = 400; res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify({ errorCode: 'FEEDBACK_002', message: '该记录已经提交过反馈' })); return
      }
      const saved = { id: session.feedbacks.length + 50, qaId: id, userId: 901, rating: body.rating, comment: body.comment || '', createdAt: '2026-10-03T12:00:00' }
      session.feedbacks.push(saved); session.wrote = true; return fixtureReply(res, 201, saved)
    }
  }
  return fixtureReply(res, 405, null)
}
const server = await createServer({ root, configFile: false, plugins: [vue(), {
  name: 'synthetic-api',
  transformIndexHtml(html, context) {
    const fixture = new URL(context.originalUrl || context.path, 'http://127.0.0.1').searchParams.get('fixture')
    // Screenshot-only fixture: freeze the running icon/stripes, never fabricate progress or files.
    if (fixture === 'f4-task-matrix') return { html, tags: [{ tag: 'style', injectTo: 'head', children: '.doc-progress *,.doc-progress *::before,.doc-progress *::after{animation-play-state:paused!important}' }] }
    if (fixture !== 'clipboard-failure' && fixture !== 'clipboard-unavailable') return html
    const replacement = fixture === 'clipboard-failure' ? '{ writeText: () => Promise.reject(new Error("Synthetic clipboard denial")) }' : 'undefined'
    return { html, tags: [{ tag: 'script', attrs: { type: 'module' }, injectTo: 'head-prepend', children: `Object.defineProperty(navigator, "clipboard", { configurable: true, value: ${replacement} }); document.documentElement.dataset.clipboardFixture = "${fixture}";` }] }
  },
  configureServer(server) {
    server.middlewares.use((req, res, next) => {
      const url = req.url?.split('?')[0]
      const pageFixture = new URL(req.url || '/', 'http://127.0.0.1').searchParams.get('fixture')
      if (!url?.startsWith('/api/') && f5Scenarios.has(pageFixture)) {
        const id = randomUUID(); f5Sessions.set(id, newF5Session(pageFixture))
        res.setHeader('Set-Cookie', `f5-preview=${id}; Path=/; SameSite=Strict`)
      }
      if (!url?.startsWith('/api/') && f4Scenarios.has(pageFixture)) {
        const id = randomUUID(); f4Sessions.set(id, newF4Session(pageFixture))
        res.setHeader('Set-Cookie', `f4-preview=${id}; Path=/; SameSite=Strict`)
      }
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
      const f5Id = req.headers.cookie?.match(/(?:^|;\s*)f5-preview=([\w-]+)/)?.[1], f5Session = f5Sessions.get(f5Id)
      if ((scenario === null || f5Scenarios.has(scenario)) && f5Session && (url.startsWith('/api/history') || url === '/api/knowledge-bases')) {
        void f5Api(req, res, url, f5Session).catch(() => { if (!res.headersSent) fixtureReply(res, 400, null, '合成请求格式或大小不符合白名单'); else res.end() })
        return
      }
      const sessionId = req.headers.cookie?.match(/(?:^|;\s*)f4-preview=([\w-]+)/)?.[1]
      const f4Session = f4Sessions.get(sessionId)
      if ((scenario === null || f4Scenarios.has(scenario)) && f4Session && (url.startsWith('/api/knowledge-bases') || url.startsWith('/api/tasks/'))) {
        void f4Api(req, res, url, f4Session).catch(() => { if (!res.headersSent) fixtureReply(res, 400, null, '合成请求格式或大小不符合白名单'); else res.end() })
        return
      }
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
