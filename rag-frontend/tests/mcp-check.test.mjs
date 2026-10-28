import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import { fileURLToPath } from 'node:url'
import { mcpMetadata, mcpReply } from './mcp-fixtures.mjs'
const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] }, server: { middlewareMode: true, hmr: { server: createHttpServer() } }, resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } }, appType: 'custom' })
try {
  const { runMcpCheck, parseMcpMetadata, MCP_MAX_BYTES, mcpCheckMessage } = await server.ssrLoadModule('/src/utils/mcpCheck.ts')
  function fixture({ metadata = mcpMetadata(), change, status, body, headers, delay = false } = {}) {
    const calls = []
    const fetch = async (url, options) => {
      const request = JSON.parse(options.body); calls.push({ url, options, request })
      if (delay) return new Promise(() => {})
      if (status) return new Response(body ?? '', { status, headers })
      if (request.method === 'notifications/initialized') return new Response(null, { status: 202 })
      let data = structuredClone(mcpReply(request, metadata))
      if (change) data = change(data, request)
      return new Response(body ?? JSON.stringify(data), { headers: { 'Content-Type': 'application/json', ...headers } })
    }
    const controller = new AbortController()
    const run = extra => runMcpCheck({ metadata, token: 'synthetic-credential', signal: controller.signal, fetch, ...extra })
    return { calls, run, controller }
  }
  await test('a complete check sends exactly five fixed methods to same origin with explicit auth only', async () => {
    for (const external of [false, true]) {
      const f = fixture({ metadata: mcpMetadata(true, external) }), result = await f.run()
      assert.equal(result.status, 'passed'); assert.equal(result.toolNames.length, external ? 4 : 2)
      assert.deepEqual(f.calls.map(c => c.request.method), ['initialize', 'notifications/initialized', 'ping', 'tools/list', 'resources/templates/list'])
      assert.equal(f.calls[1].request.id, undefined)
      for (const call of f.calls) {
        assert.equal(call.url, '/mcp'); assert.equal(call.options.redirect, 'error'); assert.equal(call.options.credentials, 'omit'); assert.equal(call.options.cache, 'no-store')
        assert.equal(call.options.headers.Authorization, 'Bearer synthetic-credential')
        assert.equal(call.options.headers.Accept, 'application/json, text/event-stream')
      }
      assert.equal(f.calls[0].options.headers['MCP-Protocol-Version'], undefined)
      assert.ok(f.calls.slice(1).every(c => c.options.headers['MCP-Protocol-Version'] === '2025-11-25'))
    }
  })
  await test('disabled metadata and missing credentials produce zero requests', async () => {
    const off = fixture({ metadata: mcpMetadata(false, true) }); assert.equal((await off.run()).category, 'MCP_DISABLED'); assert.equal(off.calls.length, 0)
    const noToken = fixture(); assert.equal((await noToken.run({ token: '' })).category, 'AUTH_REQUIRED'); assert.equal(noToken.calls.length, 0)
    assert.throws(() => parseMcpMetadata({ ...mcpMetadata(), externalToolsEffective: true }))
    assert.throws(() => parseMcpMetadata({ ...mcpMetadata(), endpointPath: 'https://untrusted.invalid' }))
  })
  await test('HTTP rejection never retries and never exposes raw errors', async () => {
    for (const [status, category] of [[401,'AUTH_REQUIRED'],[403,'ACCESS_REJECTED'],[404,'ENDPOINT_UNAVAILABLE'],[405,'METHOD_REJECTED'],[406,'ACCEPT_REJECTED'],[415,'CONTENT_TYPE_REJECTED'],[429,'RATE_LIMITED'],[503,'SERVER_ERROR'],[302,'REDIRECT_REJECTED']]) {
      const f = fixture({ status, body: 'private-upstream-canary' }), result = await f.run()
      assert.equal(result.category, category); assert.equal(f.calls.length, 1); assert.equal(result.steps[1].status, 'skipped')
      assert.ok(!JSON.stringify(result).includes('private-upstream-canary')); assert.ok(mcpCheckMessage(category))
    }
  })
  await test('HTTP 200 with malformed protocol, session, stream or wrong identity is not success', async () => {
    const cases = [
      { body: '<html>proxy page</html>' }, { headers: { 'MCP-Session-Id': 'unexpected' } }, { headers: { 'Content-Type': 'text/event-stream' } },
      { change: data => ({ ...data, id: 999 }) }, { change: data => ({ ...data, error: { code: -1, message: 'private' } }) },
      { change: data => ({ ...data, result: { ...data.result, protocolVersion: 'different' } }) },
      { change: data => ({ ...data, result: { ...data.result, serverInfo: { name: 'other' } } }) },
      { change: data => ({ ...data, result: { ...data.result, capabilities: { tools: {}, resources: {}, prompts: {} } } }) },
    ]
    for (const config of cases) { const f = fixture(config); assert.equal((await f.run()).category, 'PROTOCOL_INCOMPATIBLE'); assert.equal(f.calls.length, 1) }
  })
  await test('discovery differences and pagination retain passed steps without expanding calls', async () => {
    for (const kind of ['cursor','drift','schema','template']) {
      const f = fixture({ change: (data, request) => {
        if (request.method === 'tools/list' && kind === 'cursor') data.result.nextCursor = 'do-not-follow'
        if (request.method === 'tools/list' && kind === 'drift') data.result.tools = []
        if (request.method === 'tools/list' && kind === 'schema') data.result.tools[0].inputSchema = { type: 'object', properties: { unexpected: { type: 'string' } } }
        if (request.method === 'resources/templates/list' && kind === 'template') data.result.resourceTemplates[0].uriTemplate = 'invalid'
        return data
      } })
      const result = await f.run(); assert.equal(result.status, 'failed'); assert.equal(result.steps[0].status, 'passed'); assert.equal(result.steps[1].status, 'passed')
      assert.equal(result.category, kind === 'cursor' ? 'DISCOVERY_INCOMPLETE' : 'CATALOG_CHANGED'); assert.equal(f.calls.length, kind === 'template' ? 5 : 4)
    }
  })
  await test('bounded streamed reads stop at byte ceiling, not only Content-Length', async () => {
    const f = fixture({ body: 'x'.repeat(MCP_MAX_BYTES + 1) }); assert.equal((await f.run()).category, 'RESPONSE_TOO_LARGE'); assert.equal(f.calls.length, 1)
    const declared = fixture({ headers: { 'Content-Length': String(MCP_MAX_BYTES + 1) } }); assert.equal((await declared.run()).category, 'RESPONSE_TOO_LARGE')
  })
  await test('cancellation aborts an ignored fetch without late steps or retry', async () => {
    const f = fixture({ delay: true }), pending = f.run(); f.controller.abort()
    const result = await pending; assert.equal(result.status, 'cancelled'); assert.equal(f.calls.length, 1); assert.ok(result.steps.every(s => s.status === 'skipped'))
    const before = fixture(); before.controller.abort(); assert.equal((await before.run()).status, 'cancelled'); assert.equal(before.calls.length, 0)
  })
  await test('each request and entire sequence respect independent deadlines', async () => {
    const f = fixture({ delay: true }), started = Date.now(); const result = await f.run()
    assert.equal(result.category, 'TIMEOUT'); assert.ok(Date.now() - started < 6500); assert.equal(f.calls.length, 1)
    let time = 0
    const total = fixture(); const checked = await total.run({ now: () => { const value = time; time += 4000; return value } })
    assert.equal(checked.category, 'TIMEOUT'); assert.ok(total.calls.length < 5)
  })
  await test('MCP route stays within authenticated AppShell rather than login layout', async () => {
    const { readFile } = await import('node:fs/promises')
    const router = await readFile(new URL('../src/router/index.ts', import.meta.url), 'utf8')
    assert.ok(router.indexOf("path: 'integrations/mcp'") > router.indexOf("meta: { requiresAuth: true }"))
  })
} finally { await server.close() }
