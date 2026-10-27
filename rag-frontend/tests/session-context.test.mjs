import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createPinia } from 'pinia'
import { createSSRApp } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { fileURLToPath } from 'node:url'

const stored = new Map()
globalThis.localStorage = { getItem: key => stored.get(key) ?? null, setItem: (key,value) => stored.set(key,value), removeItem: key => stored.delete(key) }
const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] },
  server: { middlewareMode: true, hmr: { server: createHttpServer() } }, appType: 'custom', plugins: [vue()],
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } } })
const deferred = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b }); return {promise,resolve,reject} }
const context = (user=41, workspace='901', name='组织A · 合成') => ({ userInfo: {id:user,username:'fixture-user',email:null}, workspace: {id:workspace,displayName:name} })
try {
  const { useAuthStore } = await server.ssrLoadModule('/src/stores/auth.ts')
  const { createSessionContextLoader } = await server.ssrLoadModule('/src/utils/sessionContext.ts')
  const { default: Workspace } = await server.ssrLoadModule('/src/components/common/WorkspaceContext.vue')
  function fixture(read) {
    const pinia = createPinia(), auth = useAuthStore(pinia)
    auth.setAuthTokens('fixture-access','fixture-refresh')
    let changes=0
    const loader = createSessionContextLoader(auth, read, () => { changes++; auth.clearAuth() })
    return {auth,pinia,loader,changes:()=>changes}
  }
  await test('page refresh restores identity; concurrent callers share one flight and navigation reuses it', async () => {
    const pending=deferred(); let calls=0
    const f=fixture(() => { calls++; return pending.promise })
    assert.equal(f.auth.userInfo,null)
    const first=f.loader.load(), second=f.loader.load()
    assert.equal(first,second); assert.equal(f.auth.contextStatus,'loading'); assert.equal(f.auth.workspace,null)
    pending.resolve(context()); await first
    assert.equal(calls,1); assert.equal(f.auth.contextStatus,'ready'); assert.equal(f.auth.userInfo.email,'')
    assert.equal(f.auth.workspace.id,'901'); await f.loader.load(); assert.equal(calls,1)
    assert.deepEqual([...stored.keys()].sort(),['rag_accessToken','rag_refreshToken'])
  })
  await test('network or malformed context gives unavailable and explicit retry can recover', async () => {
    for (const outcome of [() => Promise.reject(Error('private upstream error')), () => Promise.resolve(context(41,'901',' ')),
      () => Promise.resolve(context(41,901)), () => Promise.resolve({}), () => { throw Error('sync failure') }]) {
      let calls=0
      const f=fixture(() => ++calls===1 ? outcome() : Promise.resolve(context()))
      await f.loader.load(); assert.equal(f.auth.contextStatus,'error'); assert.equal(f.auth.workspace,null)
      await f.loader.load(); assert.equal(f.auth.contextStatus,'ready'); assert.equal(f.changes(),0)
    }
  })
  await test('logout/new login blocks late success and error without clearing the new generation', async () => {
    for (const fail of [false,true]) {
      const old=deferred(), newer=deferred(); let calls=0
      const f=fixture(() => ++calls===1 ? old.promise : newer.promise)
      const a=f.loader.load(); await Promise.resolve()
      f.auth.clearAuth(); f.auth.setAuthTokens('b-access','b-refresh'); f.auth.setUserInfo({id:42,username:'b',email:''})
      const b=f.loader.load(); await Promise.resolve()
      if (fail) old.reject(Error('old error')); else old.resolve(context())
      await a; assert.equal(f.auth.contextStatus,'loading'); assert.equal(f.auth.workspace,null)
      newer.resolve(context(42,'902')); await b
      assert.equal(f.auth.workspace.id,'902'); assert.equal(f.changes(),0)
    }
  })
  await test('token rotation revalidates without changing login revision and rejects a changed organization', async () => {
    let response=context()
    const f=fixture(async () => response)
    await f.loader.load(); const revision=f.auth.sessionRevision
    f.auth.setAuthTokens('rotated','rotated-refresh',true)
    assert.equal(f.auth.workspace,null); assert.equal(f.auth.sessionRevision,revision)
    await f.loader.load(); assert.equal(f.auth.workspace.id,'901')
    response=context(41,'902'); f.auth.setAuthTokens('another','another-refresh',true)
    await f.loader.load(); assert.equal(f.changes(),1); assert.equal(f.auth.isLoggedIn,false); assert.equal(f.auth.workspace,null)
  })
  await test('login identity mismatch fails closed before accepting the first context', async () => {
    const f=fixture(async () => context(42,'902'))
    f.auth.setUserInfo({id:41,username:'a',email:''}); await f.loader.load()
    assert.equal(f.changes(),1); assert.equal(f.auth.isLoggedIn,false)
  })
  await test('context-triggered token rotation permits a newer generation without deadlock or stale overwrite', async () => {
    let calls=0, newer
    const f=fixture(async () => {
      calls++
      if (calls===1) {
        f.auth.setAuthTokens('rotated','rotation',true)
        newer=f.loader.load()
        return context(41,'901','过时名称')
      }
      return context(41,'901','当前名称')
    })
    await f.loader.load(); await newer
    assert.equal(calls,2); assert.equal(f.auth.workspace.displayName,'当前名称')
  })
  await test('disposing the shell invalidates unfinished context', async () => {
    const pending=deferred(), f=fixture(() => pending.promise)
    const read=f.loader.load(); f.loader.dispose(); pending.resolve(context()); await read
    assert.equal(f.auth.workspace,null)
  })
  await test('workspace UI renders loading, error, long names and an accessible collapsed entry', async () => {
    const f=fixture(async () => context())
    const html = async props => { const app=createSSRApp(Workspace,props); app.use(f.pinia); return renderToString(app) }
    f.auth.beginContextLoad(); assert.match(await html({}),/正在读取工作空间/)
    f.auth.failContextLoad(); assert.match(await html({}),/工作空间资料暂不可用/); assert.match(await html({}),/重试工作空间资料/)
    const fullName='长组织名'.repeat(24); f.auth.acceptContext(context(41,'901',fullName))
    assert.ok((await html({mode:'profile'})).includes(fullName))
    assert.ok((await html({collapsed:true})).includes('查看工作空间归属：'+fullName))
    assert.doesNotMatch(await html({}),/切换组织|已验证|认证通过/)
  })
} finally { await server.close() }
