import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer as createHttpServer } from 'node:http'
import { createServer } from 'vite'
import { createPinia } from 'pinia'
const server = await createServer({ configFile: false, optimizeDeps: { noDiscovery: true, include: [] },
  server: { middlewareMode: true, hmr: { server: createHttpServer() } }, appType: 'custom' })
try {
  const { usePreferencesStore, parsePreferences, DEFAULT_PREFERENCES, PREFERENCES_KEY } = await server.ssrLoadModule('/src/stores/preferences.ts')
  const { isSendKey } = await server.ssrLoadModule('/src/utils/sendKey.ts')
  const { useThemeStore } = await server.ssrLoadModule('/src/stores/theme.ts')
  function fixture(raw = null, reduced = false) {
    const stored = new Map([[PREFERENCES_KEY, raw], ['theme', 'dark'], ['rag_accessToken', 'fixture-token'], ['draft-sentinel', 'preserved']])
    const listeners = new Set(), styles = new Map(), root = { dataset: {}, style: { setProperty(k,v) { styles.set(k,v) } } }
    let readFailure = false, writeFailure = false
    const environment = { root, media: { matches: reduced, addEventListener(_,fn) { listeners.add(fn) }, removeEventListener(_,fn) { listeners.delete(fn) } },
      read() { if (readFailure) throw Error('blocked'); return stored.get(PREFERENCES_KEY) },
      write(value) { if (writeFailure) throw Error('blocked'); stored.set(PREFERENCES_KEY, value) } }
    const store = usePreferencesStore(createPinia())
    return { store, stored, environment, listeners, root, styles,
      readFails(value = true) { readFailure = value }, writeFails(value = true) { writeFailure = value },
      change(matches) { environment.media.matches = matches; for (const fn of listeners) fn({ matches }) } }
  }
  await test('missing, damaged and unsupported versions fall back without accepting identities', () => {
    for (const raw of [null, '', '{', 'null', '[]', 'true', '{"version":2,"motion":"reduce"}']) assert.deepEqual(parsePreferences(raw), DEFAULT_PREFERENCES)
    const parsed = parsePreferences(JSON.stringify({ version:1, answerTextSize:'large', motion:'invalid', sendKey:'modifier-enter', sources:'expanded', accessToken:'fixture-forbidden' }))
    assert.deepEqual(parsed, { answerTextSize:'large', motion:'system', sendKey:'modifier-enter', sources:'expanded' })
  })
  await test('valid fields round trip and persistence contains only the fixed schema', () => {
    const f = fixture(); f.store.initialize(f.environment)
    f.store.setPreference('answerTextSize','large'); f.store.setPreference('sendKey','modifier-enter'); f.store.setPreference('sources','expanded')
    const saved = JSON.parse(f.stored.get(PREFERENCES_KEY))
    assert.deepEqual(Object.keys(saved).sort(), ['answerTextSize','motion','sendKey','sources','version'].sort())
    assert.equal(saved.answerTextSize,'large'); assert.equal(f.styles.get('--rag-answer-font-size'),'1.0625rem')
    const reloaded = fixture(f.stored.get(PREFERENCES_KEY)); reloaded.store.initialize(reloaded.environment)
    assert.deepEqual(reloaded.store.values, f.store.values)
    f.store.dispose(); reloaded.store.dispose()
  })
  await test('system reduction wins; local reduction survives OS changes and listeners clean up', () => {
    const f = fixture(null,true); f.store.initialize(f.environment); f.store.initialize(f.environment)
    assert.equal(f.listeners.size,1); assert.equal(f.store.reducedMotion,true); assert.equal(f.root.dataset.motion,'reduce')
    f.store.setPreference('motion','system'); assert.equal(f.store.reducedMotion,true)
    f.change(false); assert.equal(f.store.reducedMotion,false)
    f.store.setPreference('motion','reduce'); f.change(true); f.change(false); assert.equal(f.store.reducedMotion,true)
    f.store.dispose(); assert.equal(f.listeners.size,0)
    f.store.initialize(f.environment); assert.equal(f.listeners.size,1); f.store.dispose()
  })
  await test('read/write failures keep safe memory behavior; later successful writes clear only their own fault', () => {
    const f = fixture(); f.readFails(); f.store.initialize(f.environment); assert.equal(f.store.persistenceAvailable,false)
    f.writeFails(); f.store.setPreference('answerTextSize','small')
    assert.equal(f.store.values.answerTextSize,'small'); assert.equal(f.styles.get('--rag-answer-font-size'),'.875rem'); assert.equal(f.store.persistenceAvailable,false)
    f.writeFails(false); f.store.setPreference('sources','expanded'); assert.equal(f.store.persistenceAvailable,true)
    f.store.dispose()
  })
  await test('reset preserves unrelated storage and reports partial persistence failure', () => {
    const f = fixture(); f.store.initialize(f.environment); f.store.setPreference('motion','reduce'); f.writeFails(); f.store.reset()
    assert.deepEqual(f.store.values,DEFAULT_PREFERENCES); assert.equal(f.store.persistenceAvailable,false)
    assert.equal(f.stored.get('rag_accessToken'),'fixture-token'); assert.equal(f.stored.get('draft-sentinel'),'preserved'); assert.equal(f.stored.get('theme'),'dark')
    f.writeFails(false); f.store.reset(); assert.deepEqual(JSON.parse(f.stored.get(PREFERENCES_KEY)),{version:1,...DEFAULT_PREFERENCES})
    f.store.dispose()
  })
  await test('theme and other preferences have independent storage health', () => {
    const f=fixture(); f.store.initialize(f.environment)
    const theme=useThemeStore(createPinia())
    const env={root:{classList:{toggle(){}},style:{}},media:{matches:false,addEventListener(){},removeEventListener(){}},read:()=>null,write(){throw Error('theme blocked')}}
    theme.initialize(env); theme.setPreference('dark'); f.store.setPreference('sources','expanded')
    assert.equal(theme.persistenceAvailable,false); assert.equal(f.store.persistenceAvailable,true)
    theme.dispose(); f.store.dispose()
  })
  await test('send key matrix respects modifiers, shift, composition boundaries and repeat', () => {
    const enter={key:'Enter',shiftKey:false}
    assert.equal(isSendKey(enter,'enter'),true); assert.equal(isSendKey(enter,'modifier-enter'),false)
    for (const key of ['ctrlKey','metaKey']) { const e={...enter,[key]:true}; assert.equal(isSendKey(e,'modifier-enter'),true); assert.equal(isSendKey(e,'enter'),false) }
    for (const patch of [{shiftKey:true},{altKey:true},{repeat:true},{isComposing:true},{keyCode:229},{key:'a'}]) {
      for (const mode of ['enter','modifier-enter']) assert.equal(isSendKey({...enter,ctrlKey:true,...patch},mode),false)
    }
    assert.equal(isSendKey(enter,'enter',true),false)
  })
} finally { await server.close() }
