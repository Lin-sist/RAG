// Local synthetic preview only. Login/ask return fixtures; unknown API calls fail closed.
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'
import { readFile } from 'node:fs/promises'
const root = fileURLToPath(new URL('../', import.meta.url))
const kb = { id: 901, name: '合成知识库 · 项目文档', description: '仅用于前端验证，不连接真实后端', documentCount: 2, isPublic: false, vectorCollection: 'fixture_collection', vectorModel: 'fixture-model', vectorDimension: 1024, createdAt: '2026-09-20T10:00:00', updatedAt: '2026-09-20T10:00:00' }
const server = await createServer({ root, configFile: false, plugins: [vue(), {
  name: 'synthetic-api', configureServer(server) {
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
      if (req.method !== 'GET' && url !== '/auth/login' && !(req.method === 'POST' && url === '/api/qa/ask')) {
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
      else if (url === '/api/history/901') data = { id: 901, kbId: 901, userId: 901, question: '这是一条合成历史问题', answer: '这是一条合成回答，仅用于前端兼容验证。', citations: [], createdAt: kb.createdAt, traceId: 'fixture-trace', latencyMs: 0 }
      else if (url === '/api/history/901/feedback') data = []
      else if (url === '/api/qa/ask') data = { question: '合成问题', answer: '这是一条合成回答，仅用于前端兼容验证。', citations: [], contexts: [], metadata: { status: 'ANSWER' } }
      else status = 404
      res.statusCode = status
      res.setHeader('Content-Type', 'application/json; charset=utf-8')
      const send = () => res.end(JSON.stringify(status === 200 ? { code: 200, data } : { errorCode: 'FIXTURE_ONLY', message: '合成预览错误，未连接真实服务' }))
      if (scenario === 'loading') setTimeout(send, 4000)
      else send()
    })
  },
}], resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } }, server: { host: '127.0.0.1', port: 5188, strictPort: true } })
await server.listen()
console.log('Synthetic preview: http://127.0.0.1:5188/login')
