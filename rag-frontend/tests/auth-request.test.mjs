import { createServer as createHttpServer } from 'node:http'
import { test as nodeTest } from 'node:test'
const test = (name, fn) => nodeTest(name, { timeout: 2000 }, fn)
import assert from 'node:assert/strict'
import { createServer } from 'vite'
const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] }, server: { middlewareMode: true, hmr: { server: createHttpServer() } }, appType: 'custom' })
const { createRequestClient } = await server.ssrLoadModule('/src/api/requestClient.ts')
const { ApiError } = await server.ssrLoadModule('/src/api/errors.ts')
const axios = (await import('axios')).default
const auth = { accessToken: 'new', refreshToken: 'rotated', userInfo: { id: 1, username: 'fixture', email: '' } }
function fixture(handler) {
  let tokens = { accessToken: 'old', refreshToken: 'refresh' }, expired = 0, revision = 0
  const session = { revision: () => revision, access: () => tokens.accessToken, refresh: () => tokens.refreshToken,
    save: data => { tokens = data }, clear: () => { revision++; tokens = { accessToken: '', refreshToken: '' } }, expired: () => expired++ }
  const adapter = async config => {
    const [status, data, headers = {}] = await handler(config)
    const response = { status, data, headers, statusText: '', config }
    if (status >= 400) throw new axios.AxiosError('synthetic', 'ERR_BAD_RESPONSE', config, {}, response)
    return response
  }
  return { ...createRequestClient(session, adapter), tokens: () => tokens, session, expired: () => expired }
}
try {
  await test('201/202/204 and success without data', async () => {
    for (const status of [201, 202, 204]) {
      const f = fixture(async () => [status, status === 204 ? '' : { code: status, message: 'ok' }])
      assert.equal((await f.request.post('/resource')).status, status)
    }
  })
  await test('HTTP failure wins; typed envelopes and Retry-After', async () => {
    for (const body of [{ code: 200, message: 'limited' }, { errorCode: 'RATE_LIMITED', message: 'limited', traceId: 'fixture' }]) {
      const f = fixture(async () => [429, body, { 'retry-after': '5' }])
      await assert.rejects(f.request.get('/resource'), e => e instanceof ApiError && e.status === 429 && e.retryAfter === '5' && e.message === 'limited')
    }
    await assert.rejects(fixture(async () => [200, { code: 500, message: 'failed' }]).request.get('/resource'), e => e instanceof ApiError)
  })
  await test('concurrent 401 shares refresh and rotates both tokens', async () => {
    let refreshes = 0
    const f = fixture(async c => {
      if (c.url === '/auth/refresh') { refreshes++; await new Promise(r => setTimeout(r, 20)); return [200, { code: 200, data: auth }] }
      return c.headers.Authorization === 'Bearer new' ? [200, { code: 200 }] : [401, {}]
    })
    await Promise.all([f.request.get('/a'), f.request.get('/b'), f.request.get('/c')])
    assert.equal(refreshes, 1); assert.equal(f.tokens().refreshToken, 'rotated')
  })
  await test('failed refresh rejects every waiter and clears session once', async () => {
    const f = fixture(async c => { if (c.url === '/auth/refresh') await new Promise(r => setTimeout(r, 20)); return [401, {}] })
    const results = await Promise.allSettled([f.request.get('/a'), f.request.get('/b')])
    assert.ok(results.every(r => r.status === 'rejected' && r.reason instanceof ApiError))
    assert.equal(f.tokens().accessToken, ''); assert.equal(f.expired(), 1)
  })
  await test('replayed 401 stops after one refresh', async () => {
    let calls = 0
    const f = fixture(async c => { calls++; return c.url === '/auth/refresh' ? [200, { code: 200, data: auth }] : [401, {}] })
    await assert.rejects(f.request.get('/a')); assert.equal(calls, 3)
  })
  await test('login 401 never refreshes or forces logout', async () => {
    let calls = 0
    const f = fixture(async () => { calls++; return [401, { message: 'bad credentials' }] })
    await assert.rejects(f.request.post('/auth/login'), e => e.status === 401)
    assert.equal(calls, 1); assert.equal(f.expired(), 0)
  })
  await test('late old-token 401 reuses rotated token', async () => {
    let refreshes = 0
    const f = fixture(async c => {
      if (c.url === '/auth/refresh') { refreshes++; return [200, { code: 200, data: auth }] }
      if (c.headers.Authorization === 'Bearer new') return [200, { code: 200 }]
      if (c.url === '/slow') await new Promise(r => setTimeout(r, 40))
      return [401, {}]
    })
    await Promise.all([f.request.get('/fast'), f.request.get('/slow')]); assert.equal(refreshes, 1)
  })
  await test('missing refresh token rejects and permits later login refresh', async () => {
    const f = fixture(async () => [200, { code: 200, data: auth }])
    f.session.clear()
    await assert.rejects(f.refreshSession())
    f.session.save({ accessToken: 'old', refreshToken: 'refresh' })
    assert.equal((await f.refreshSession()).accessToken, 'new')
  })
  await test('logout during refresh cannot resurrect session', async () => {
    let finish
    const f = fixture(async () => { await new Promise(r => { finish = r }); return [200, { code: 200, data: auth }] })
    const pending = f.refreshSession()
    await new Promise(r => setTimeout(r, 10))
    f.session.clear(); finish()
    await assert.rejects(pending)
    assert.equal(f.tokens().accessToken, '')
  })
  await test('malformed refresh clears session; timeout and network remain typed', async () => {
    const f = fixture(async () => [200, { code: 200, data: { accessToken: 'incomplete' } }])
    await assert.rejects(f.refreshSession(), e => e instanceof ApiError)
    assert.equal(f.tokens().accessToken, '')
    for (const code of ['ERR_NETWORK', 'ECONNABORTED']) {
      const g = fixture(async () => { throw new axios.AxiosError('synthetic', code) })
      await assert.rejects(g.request.get('/a'), e => e instanceof ApiError && e.errorCode === code)
    }
  })
  await test('403/404/503 are typed and never retried', async () => {
    for (const status of [403, 404, 503]) {
      let calls = 0
      const f = fixture(async () => { calls++; return [status, { errorCode: 'FIXTURE', traceId: 'trace' }] })
      await assert.rejects(f.request.get('/a'), e => e.status === status && e.errorCode === 'FIXTURE' && e.traceId === 'trace')
      assert.equal(calls, 1)
    }
  })
  await test('logout failure is returned without refresh', async () => {
    let calls = 0
    const f = fixture(async () => { calls++; return [401, {}] })
    await assert.rejects(f.request.post('/auth/logout'))
    assert.equal(calls, 1)
  })
  await test('explicit refresh and interceptor share one flight', async () => {
    let refreshes = 0
    const f = fixture(async c => {
      if (c.url === '/auth/refresh') { refreshes++; await new Promise(r => setTimeout(r, 20)); return [200, { code: 200, data: auth }] }
      return c.headers.Authorization === 'Bearer new' ? [200, { code: 200 }] : [401, {}]
    })
    await Promise.all([f.refreshSession(), f.request.get('/a')])
    assert.equal(refreshes, 1)
  })
  await test('old request cannot replay under a newly logged-in session', async () => {
    let finish, calls = 0
    const f = fixture(async () => { calls++; await new Promise(r => { finish = r }); return [401, {}] })
    const pending = f.request.get('/old-user-resource')
    await new Promise(r => setTimeout(r, 10))
    f.session.clear(); f.session.save(auth); finish()
    await assert.rejects(pending)
    assert.equal(calls, 1); assert.equal(f.tokens().accessToken, 'new')
  })
} finally { await server.close() }
