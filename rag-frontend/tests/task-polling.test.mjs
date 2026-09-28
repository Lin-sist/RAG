import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] }, server: { middlewareMode: true, hmr: { server: createHttpServer() } }, appType: 'custom' })
const { createTaskPoller } = await server.ssrLoadModule('/src/utils/taskPoller.ts')
const wait = ms => new Promise(r => setTimeout(r, ms))
try {
  await test('serial polling, deduplicated start and terminal stop', async () => {
    let calls = 0, active = 0, max = 0, terminals = 0
    const p = createTaskPoller(async () => { calls++; active++; max = Math.max(max, active); await wait(15); active--; return { state: calls === 2 ? 'COMPLETED' : 'RUNNING' } }, 1)
    const observer = { update() {}, error: assert.fail, terminal() { terminals++ } }
    p.start('a', observer); p.start('a', observer)
    await wait(100); p.stopAll()
    assert.equal(calls, 2); assert.equal(max, 1); assert.equal(terminals, 1)
  })
  await test('network error stops until explicit retry', async () => {
    let calls = 0, errors = 0
    const p = createTaskPoller(async () => { calls++; throw new Error('synthetic') }, 1)
    const observer = { update: assert.fail, error() { errors++ } }
    p.start('a', observer); await wait(30)
    assert.equal(calls, 1); assert.equal(errors, 1)
    p.start('a', observer); await wait(30); p.stopAll()
    assert.equal(calls, 2)
  })
  await test('cancelled and failed remain distinct terminal states', async () => {
    for (const state of ['CANCELLED', 'FAILED']) {
      let terminal
      const p = createTaskPoller(async () => ({ state }), 1)
      p.start('a', { update() {}, error: assert.fail, terminal(s) { terminal = s.state } })
      await wait(10); p.stopAll(); assert.equal(terminal, state)
    }
  })
  await test('stop discards in-flight responses and restart keeps only new job', async () => {
    const pending = []; const updates = []
    const p = createTaskPoller(() => new Promise(r => pending.push(r)), 1)
    const observer = { update(s) { updates.push(s.state) }, error: assert.fail }
    p.start('a', observer); p.stopAll(); p.start('a', observer)
    pending[0]({ state: 'FAILED' }); pending[1]({ state: 'COMPLETED' })
    await wait(10); p.stopAll(); assert.deepEqual(updates, ['COMPLETED'])
  })
} finally { await server.close() }
