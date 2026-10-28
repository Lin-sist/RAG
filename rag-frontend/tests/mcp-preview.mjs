// Isolated synthetic site. No backend proxy, external provider or real account is used.
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'
import { mcpMetadata, mcpReply } from './mcp-fixtures.mjs'
let mode = 'ready', calls = []
const server = await createServer({ root: fileURLToPath(new URL('../', import.meta.url)), configFile: false,
  define: { __RAG_FRONTEND_VERSION__: JSON.stringify('1.0.0') }, plugins: [vue(), { name: 'mcp-synthetic-preview', configureServer(server) {
    server.middlewares.use(async (req, res, next) => {
      const url = new URL(req.url, 'http://127.0.0.1:5194')
      const json = (status, data) => { res.statusCode = status; res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify(data)) }
      if (url.pathname === '/__mcp-fixture') { if (url.searchParams.has('mode')) { mode = url.searchParams.get('mode'); calls = [] } return json(200, { mode, calls }) }
      const metadata = mcpMetadata(mode !== 'disabled', mode === 'external')
      if (url.pathname === '/api/integrations/mcp') return json(mode === 'legacy' ? 404 : 200, { code: 200, data: metadata })
      if (url.pathname === '/api/session/context') return json(200, { code: 200, data: { userInfo: { id: 901, username: 'synthetic', email: '', displayName: '合成验收', hasAvatar: false, profileRevision: 1 }, workspace: { id: '901', displayName: '合成工作空间' } } })
      if (url.pathname === '/api/history') return json(200, { code: 200, data: { records: [], total: 0, page: 1, size: 20 } })
      if (url.pathname === '/api/knowledge-bases') return json(200, { code: 200, data: [] })
      if (url.pathname === '/mcp') {
        let text = ''; for await (const chunk of req) { text += chunk; if (text.length > 8192) return json(413, {}) }
        const body = JSON.parse(text); calls.push({ method: body.method, credentialsPresent: !!req.headers.authorization, origin: req.headers.origin })
        if (mode === 'rejected') return json(403, { errorCode: 'MCP_FORBIDDEN' })
        if (mode === 'unauthorized') return json(401, { errorCode: 'AUTH_001' })
        if (mode === 'slow') { await new Promise(resolve => setTimeout(resolve, 3000)); if (res.destroyed) return }
        if (body.method === 'notifications/initialized') { res.statusCode = 202; return res.end() }
        const reply = mcpReply(body, metadata)
        if (mode === 'drift' && body.method === 'tools/list') reply.result.tools = []
        return json(200, reply)
      }
      if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/auth/')) return json(404, { errorCode: 'SYNTHETIC_ONLY' })
      next()
    })
  } }], resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } }, server: { host: '127.0.0.1', port: 5194, strictPort: true } })
await server.listen()
console.log('Synthetic MCP preview at http://127.0.0.1:5194')
