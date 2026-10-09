import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createRenderer, h, nextTick, reactive, ssrContextKey } from 'vue'
import { createPinia } from 'pinia'
import { routerKey, routeLocationKey } from 'vue-router'
import { fileURLToPath } from 'node:url'

const events = new Map()
globalThis.localStorage = { getItem() { return null }, setItem() {}, removeItem() {} }
globalThis.document = { addEventListener() {}, removeEventListener() {} }
globalThis.window = { navigator: { userAgent: 'fixture' }, location: { href: 'http://fixture.test/' }, addEventListener(name, fn) { if (!events.has(name)) events.set(name, new Set()); events.get(name).add(fn) }, removeEventListener(name, fn) { events.get(name)?.delete(fn) }, dispatchEvent(event) { for (const fn of events.get(event.type) || []) fn() } }
globalThis.__f5 = { calls: [], confirm: async () => {}, handle: async () => ({data:{data:[]}}) }
const server = await createServer({ configFile:false, optimizeDeps:{noDiscovery:true,include:[]}, server:{middlewareMode:true,hmr:{server:createHttpServer()}}, appType:'custom',
  resolve:{alias:{'@':fileURLToPath(new URL('../src',import.meta.url))}}, plugins:[vue(),{
    name:'f5-fixtures',enforce:'pre',transform(code,id) {
      if (/ChatPanel\.vue|ChatHistory\.vue|HistoryFeedback\.vue|knowledgeBase\.ts/.test(id)) return code.replace(/import \{ ElMessage(?:Box)? \} from 'element-plus'/g,
        'const ElMessage = { warning() {}, error() {}, success() {} }; const ElMessageBox = { confirm: (...args) => globalThis.__f5.confirm(...args) }')
    },resolveId(id,importer) { if(id==='./request' && importer?.replaceAll('\\','/').includes('/src/api/')) return '\0f5-request' },load(id) {
      if(id==='\0f5-request') return `const invoke=(method,url,data)=>{globalThis.__f5.calls.push({method,url,data});return globalThis.__f5.handle(method,url,data)}; export default {get:(url,data)=>invoke('GET',url,data),post:(url,data)=>invoke('POST',url,data),delete:url=>invoke('DELETE',url)}`
    },
  }]})
const renderer=createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:text=>({text}),setText(n,t){n.text=t},setElementText(n,t){n.text=t},insert(n,p){n.parent=p;p.children.push(n)},remove(n){n.parent.children=n.parent.children.filter(x=>x!==n)},parentNode:n=>n.parent,nextSibling:()=>null,patchProp(){}})
const flush=async()=>{for(let i=0;i<16;i++){await Promise.resolve();await nextTick()}}
const record=id=>({id,userId:901,kbId:7,question:`问题${id}`,answer:'回答',citations:[],createdAt:'2026-10-03T10:00:00'})
const page=(ids,total=ids.length,totalPages=1)=>({records:ids.map(record),total,totalPages,page:1,size:20})
const saved=id=>({id:50,qaId:id,userId:901,rating:4,comment:'反馈',createdAt:'2026-10-03T10:00:00'})
const response=data=>({data:{data}})
function deferred(){let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return{promise,resolve,reject}}
try {
  const {ApiError}=await server.ssrLoadModule('/src/api/errors.ts')
  const actions=await server.ssrLoadModule('/src/utils/historyActions.ts')
  const {useAuthStore}=await server.ssrLoadModule('/src/stores/auth.ts')
  const History=(await server.ssrLoadModule('/src/views/history/ChatHistory.vue')).default
  const Feedback=(await server.ssrLoadModule('/src/components/history/HistoryFeedback.vue')).default
  const Mine=(await server.ssrLoadModule('/src/components/history/MyFeedbackList.vue')).default
  const Chat=(await server.ssrLoadModule('/src/components/chat/ChatPanel.vue')).default
  const fixture=globalThis.__f5
  function mount(component,props={},route=reactive({name:'History',fullPath:'/history',params:{},query:{}})) {
    const pinia=createPinia(),auth=useAuthStore(pinia);auth.setAuthTokens('fixture','fixture');auth.setUserInfo({id:901,username:'fixture',email:''})
    const app=renderer.createApp({...component,render:()=>h('div')},props);app.use(pinia);app.provide(ssrContextKey,{});app.provide(routerKey,{push(){}});app.provide(routeLocationKey,route);app.mount({children:[]})
    return{app,state:app._instance.setupState,auth,route}
  }
  function reset(handle){fixture.calls=[];fixture.confirm=async()=>{};fixture.handle=handle}
  await test('history reads one page of 20; local find does not issue requests or change server total',async()=>{
    reset(async(method,url,data)=>response(url==='/api/history'?page([data.params.page],61,4):[]))
    const m=mount(History);await flush();assert.equal(fixture.calls.filter(x=>x.url==='/api/history').length,1);assert.equal(fixture.calls[0].data.params.size,20)
    m.state.searchQuery='missing';await flush();assert.equal(m.state.filteredCount,0);assert.equal(m.state.total,61);assert.equal(fixture.calls.length,2)
    m.state.goPage(2);await flush();assert.equal(m.state.records[0].id,2);assert.equal(m.state.searchQuery,'missing')
    m.state.changeScope('7');await flush();assert.equal(m.state.page,1);assert.equal(fixture.calls.at(-1).data.params.kbId,7);m.app.unmount()
  })
  await test('out-of-order page, scope, session and unmount responses cannot refill old history',async()=>{
    const waits=[];reset((method,url)=>url==='/api/history'?(()=>{const d=deferred();waits.push(d);return d.promise})():Promise.resolve(response([])))
    const m=mount(History);m.state.goPage(2);waits[1].resolve(response(page([2],40,2)));await flush();waits[0].resolve(response(page([1])));await flush();assert.equal(m.state.records[0].id,2)
    m.state.changeScope('8');m.auth.clearAuth();waits[2].resolve(response(page([3])));await flush();assert.deepEqual(m.state.records,[])
    const pending=m.state.loadHistoryGroups();m.app.unmount();waits[3].resolve(response(page([4])));await pending;assert.deepEqual(m.state.records,[])
  })
  await test('invalid KB range is not sent, and deleting the last page performs one corrective GET',async()=>{
    reset(async(method,url,data)=>response(url==='/api/history'?(data.params.page===3?page([],21,2):page([21],21,2)):[]))
    const m=mount(History);await flush();const count=fixture.calls.length;m.state.changeScope('1e3');assert.equal(fixture.calls.length,count)
    m.state.page=3;await m.state.loadHistoryGroups();assert.equal(m.state.page,2);assert.equal(m.state.records[0].id,21);assert.equal(fixture.calls.filter(x=>x.url==='/api/history').length,3);m.app.unmount()
  })
  await test('single deletion is locked during confirmation; failed refresh retries only GET',async()=>{
    let deleted=false;const confirmation=deferred();reset(async(method,url)=>{if(method==='DELETE'){deleted=true;return response(null)}if(url==='/api/history'&&deleted)throw new ApiError('读取失败',503);return response(url==='/api/history'?page([1]):[])})
    fixture.confirm=()=>confirmation.promise;const m=mount(History);await flush();fixture.calls=[]
    const first=m.state.handleDelete({id:1,title:'目标'});await m.state.handleDelete({id:1,title:'目标'});confirmation.resolve();await first
    assert.equal(fixture.calls.filter(x=>x.method==='DELETE').length,1);assert.match(m.state.actionMessage,/删除结果已保留/);assert.deepEqual(m.state.records,[])
    fixture.calls=[];await m.state.loadHistoryGroups();assert.ok(fixture.calls.every(x=>x.method==='GET'));m.app.unmount()
  })
  await test('batch snapshot failure and cancelled confirmation never send DELETE',async()=>{
    for(const mode of ['failure','cancel']) {
      reset(async(method,url,data)=>{if(url==='/api/history'&&data.params.size===100){if(mode==='failure')throw new ApiError('failed',503);return response(page([1,2]))}return response(url==='/api/history'?page([1]):[])})
      fixture.confirm=async()=>{throw 'cancel'};const m=mount(History);await flush();await m.state.handleClearAll();assert.ok(fixture.calls.every(x=>x.method!=='DELETE'));m.app.unmount()
    }
  })
  await test('snapshot freezes page count and deduplicates actual IDs independent of total',async()=>{
    const calls=[];const ids=await actions.collectHistorySnapshot(async p=>{calls.push(p);return p===1?page([1,2],999,2):page([2,3],999,9)},()=>true)
    assert.deepEqual(ids,[1,2,3]);assert.deepEqual(calls,[1,2])
  })
  await test('batch partial rejection continues; 429 and uncertain writes stop remaining targets',async()=>{
    for(const cause of [new ApiError('rejected',403),new ApiError('limited',429),new ApiError('lost')]) {
      const calls=[],confirmed=[];const result=await actions.deleteHistorySnapshot([1,1,2,3],async id=>{calls.push(id);if(id===2)throw cause},()=>true,id=>confirmed.push(id))
      if(cause.status===403){assert.deepEqual(calls,[1,2,3]);assert.equal(result.failed,1);assert.equal(result.remaining,0)}
      else {assert.deepEqual(calls,[1,2]);assert.equal(result.remaining,1);assert.equal(result.stopped,true)}
      assert.equal(result.uncertain,cause.status===undefined?1:0)
    }
  })
  await test('changing identity during a deletion stops the next write and suppresses old UI callback',async()=>{
    let current=true;const d=deferred(),calls=[],confirmed=[]
    const pending=actions.deleteHistorySnapshot([1,2],id=>{calls.push(id);return d.promise},()=>current,id=>confirmed.push(id))
    current=false;d.resolve();const result=await pending;assert.deepEqual(calls,[1]);assert.deepEqual(confirmed,[]);assert.equal(result.remaining,1)
  })
  await test('confirmed delete invalidates an in-flight list instead of reviving its row',async()=>{
    const stale=deferred();let reads=0;reset(async(method,url)=>{if(url==='/api/history'){reads++;return reads===1?response(page([1])):reads===2?stale.promise:response(page([]))}return response([])})
    const m=mount(History);await flush();const pending=m.state.loadHistoryGroups();await m.state.handleDelete({id:1,title:'目标'});stale.resolve(response(page([1])));await pending;assert.deepEqual(m.state.records,[]);m.app.unmount()
  })
  await test('feedback rejects foreign qaId/user and preserves validation failure inputs',async()=>{
    reset(async(method)=>{if(method==='POST')throw new ApiError('校验失败',400);return response([saved(2),{...saved(1),userId:8}])})
    const m=mount(Feedback,{historyId:1});await flush();assert.equal(m.state.feedback,null);m.state.rating=3;m.state.comment='输入保留';await m.state.submit();assert.equal(m.state.comment,'输入保留');assert.equal(m.state.rating,3);assert.equal(m.state.submitError,'校验失败');m.app.unmount()
  })
  await test('feedback double submit writes once and known success remains locked if readback fails',async()=>{
    const write=deferred();let reads=0;reset((method)=>{if(method==='POST')return write.promise;if(++reads>1)return Promise.reject(new ApiError('读回失败',503));return Promise.resolve(response([]))})
    const m=mount(Feedback,{historyId:1});await flush();m.state.rating=4;const first=m.state.submit();await m.state.submit();write.resolve(response(saved(1)));await first
    assert.equal(m.state.submissionLocked,true);assert.equal(m.state.feedback.qaId,1);await m.state.loadFeedback();await m.state.submit();assert.equal(fixture.calls.filter(x=>x.method==='POST').length,1);m.app.unmount()
  })
  await test('lost feedback response checks only GET; empty check requires a separate confirmed action',async()=>{
    reset(async method=>{if(method==='POST')throw new ApiError('lost');return response([])})
    const m=mount(Feedback,{historyId:1});await flush();m.state.rating=4;m.state.comment='kept';await m.state.submit();assert.equal(m.state.uncertain,true);assert.equal(m.state.checkedEmpty,true)
    await m.state.submit();assert.equal(fixture.calls.filter(x=>x.method==='POST').length,1);fixture.confirm=async()=>{throw 'cancel'};await m.state.confirmResubmit();assert.equal(fixture.calls.filter(x=>x.method==='POST').length,1);assert.equal(m.state.comment,'kept');m.app.unmount()
  })
  await test('duplicate feedback error locks writing and recovers the existing feedback',async()=>{
    let writes=0;reset(async method=>{if(method==='POST'){writes++;throw new ApiError('duplicate',400,'FEEDBACK_002')}return response(writes?[saved(1)]:[])})
    const m=mount(Feedback,{historyId:1});await flush();m.state.rating=4;await m.state.submit();assert.equal(m.state.feedback.qaId,1);assert.equal(m.state.submissionLocked,true);m.app.unmount()
  })
  await test('feedback props, session and unmount invalidate delayed reads and writes',async()=>{
    const first=deferred();reset(async(method,url)=>url.includes('/1/')?first.promise:response([]))
    const m=mount(Feedback,{historyId:1});m.app._instance.props.historyId=2;await flush();first.resolve(response([saved(1)]));await flush();assert.equal(m.state.feedback,null)
    const write=deferred();fixture.handle=method=>method==='POST'?write.promise:Promise.resolve(response([]));m.state.rating=4;const pending=m.state.submit();m.auth.clearAuth();write.resolve(response(saved(2)));await pending;assert.equal(m.state.feedback,null);m.app.unmount()
    const read=deferred();reset(()=>read.promise);const n=mount(Feedback,{historyId:1});n.app.unmount();read.resolve(response([saved(1)]));await flush();assert.equal(n.state.feedback,null)
  })
  await test('my feedback issues one GET without per-record enrichment and clears foreign/session data',async()=>{
    reset(async()=>response([saved(1),{...saved(2),userId:2}]))
    const m=mount(Mine,{titles:{}});await flush();assert.deepEqual(fixture.calls.map(x=>x.url),['/api/history/feedback/my']);assert.equal(m.state.items.length,1)
    const read=deferred();fixture.handle=()=>read.promise;const pending=m.state.load();m.auth.clearAuth();read.resolve(response([saved(1)]));await pending;assert.deepEqual(m.state.items,[]);m.app.unmount()
  })
  await test('history detail unmount and session changes suppress late answers; a mismatched record is rejected',async()=>{
    const read=deferred();reset(async(method,url)=>url==='/api/history/1'?read.promise:response([]))
    const route=reactive({name:'ChatSession',fullPath:'/chat/1',params:{id:'1'},query:{}})
    const m=mount(Chat,{},route);m.app.unmount();read.resolve(response(record(1)));await flush();assert.equal(m.state.historyRecordId,null)
    reset(async(method,url)=>response(url==='/api/history/1'?record(2):[]));const n=mount(Chat,{},route);await flush();assert.equal(n.state.historyRecordId,null);assert.ok(n.state.historyLoadError);assert.equal(n.state.canSend,false);n.app.unmount()
  })
} finally { await server.close();delete globalThis.__f5 }
