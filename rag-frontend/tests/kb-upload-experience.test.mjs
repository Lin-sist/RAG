import { createServer as createHttpServer } from 'node:http'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createRenderer, h, nextTick, reactive, ssrContextKey } from 'vue'
import { createPinia } from 'pinia'
import { routerKey, routeLocationKey } from 'vue-router'
import { fileURLToPath } from 'node:url'

globalThis.localStorage = { getItem() { return null }, setItem() {}, removeItem() {} }
globalThis.document = { addEventListener() {}, removeEventListener() {} }
globalThis.window = { navigator: { userAgent: 'fixture' }, location: { href: 'http://fixture.test/' }, addEventListener() {}, removeEventListener() {}, dispatchEvent() {} }
globalThis.__f4 = { calls: [], confirm: async () => {}, handle: async () => ({data:{data:[]}}) }
const server = await createServer({ configFile:false, optimizeDeps:{noDiscovery:true,include:[]},
  server:{middlewareMode:true,hmr:{server:createHttpServer()}}, appType:'custom',
  resolve:{alias:{'@':fileURLToPath(new URL('../src',import.meta.url))}}, plugins:[vue(),{
    name:'f4-fixtures',enforce:'pre',transform(code,id) {
      if (/ChatPanel\.vue|KnowledgeBaseDetail\.vue|DocList\.vue|knowledgeBase\.ts/.test(id)) {
        return code.replace(/import \{ ElMessage(?:Box, ElMessage)? \} from 'element-plus'/g,
          'const ElMessage = { warning() {}, error() {}, success() {} }; const ElMessageBox = { confirm: (...args) => globalThis.__f4.confirm(...args) }')
      }
    },resolveId(id,importer) {
      if (id === './request' && importer?.replaceAll('\\','/').includes('/src/api/')) return '\0f4-request'
    },load(id) {
      if (id === '\0f4-request') return `const invoke=(method,url,data)=>{globalThis.__f4.calls.push({method,url,data});return globalThis.__f4.handle(method,url,data)}; export default {get:(url,data)=>invoke('GET',url,data),post:(url,data)=>invoke('POST',url,data),put:(url,data)=>invoke('PUT',url,data),delete:url=>invoke('DELETE',url)}`
    },
  }]})
const renderer = createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:text=>({text}),setText(node,text){node.text=text},setElementText(node,text){node.text=text},insert(node,parent){node.parent=parent;parent.children.push(node)},remove(node){node.parent.children=node.parent.children.filter(x=>x!==node)},parentNode:node=>node.parent,nextSibling:()=>null,patchProp(){}})
const flush = async () => { for(let i=0;i<12;i++) {await Promise.resolve(); await nextTick()} }
function mount(component,props={},route=reactive({name:'Chat',fullPath:'/chat',params:{},query:{}})) {
  const pinia=createPinia(),app=renderer.createApp({...component,render:()=>h('div')},props)
  app.use(pinia);app.provide(ssrContextKey,{});app.provide(routerKey,{push(){}});app.provide(routeLocationKey,route)
  app.mount({children:[]}); return {app,state:app._instance.setupState,pinia,route}
}
try {
  const { ApiError }=await server.ssrLoadModule('/src/api/errors.ts')
  const { submitUploadBatch }=await server.ssrLoadModule('/src/utils/uploadQueue.ts')
  const { parseKbId }=await server.ssrLoadModule('/src/utils/kbPresentation.ts')
  const { default: Form }=await server.ssrLoadModule('/src/components/knowledge-base/KBCreateDialog.vue')
  const { default: Chat }=await server.ssrLoadModule('/src/components/chat/ChatPanel.vue')
  const { default: Detail }=await server.ssrLoadModule('/src/views/knowledge-base/KnowledgeBaseDetail.vue')
  const { default: Docs }=await server.ssrLoadModule('/src/components/document/DocList.vue')
  const fixture=globalThis.__f4
  await test('mixed uploads retain failed files and never resend accepted files on the next user action',async()=>{
    const a=new File(['a'],'a.txt'),b=new File(['b'],'b.txt'),calls=[],outcomes=[]
    const remaining=await submitUploadBatch([a,b],async file=>{calls.push(file.name);if(file===b)throw new ApiError('unsupported',400);return{status:202,data:{data:{taskId:'a'}}}},r=>outcomes.push(r))
    assert.deepEqual(remaining,[b]);assert.deepEqual(outcomes.map(x=>x.state),['accepted','failed'])
    await submitUploadBatch(remaining,async file=>{calls.push(file.name);return{status:202,data:{data:{taskId:'b'}}}},()=>{})
    assert.deepEqual(calls,['a.txt','b.txt','b.txt'])
  })
  await test('lost response and missing task identity remain unconfirmed without automatic retry',async()=>{
    for(const response of [()=>Promise.reject(new ApiError('network')),()=>Promise.resolve({status:202,data:{data:{taskId:''}}})]){
      const file=new File(['x'],'x.txt'),outcomes=[];let calls=0
      const remaining=await submitUploadBatch([file],()=>{calls++;return response()},r=>outcomes.push(r))
      assert.equal(calls,1);assert.deepEqual(remaining,[file]);assert.equal(outcomes[0].state,'unconfirmed');assert.match(outcomes[0].message,/先核查/)
    }
  })
  await test('leaving a page suppresses upload results and does not send the next file',async()=>{
    let active=true,resolveUpload,calls=0;const outcomes=[]
    const files=[new File(['a'],'a.txt'),new File(['b'],'b.txt')]
    const pending=submitUploadBatch(files,()=>{calls++;return new Promise(r=>resolveUpload=r)},r=>outcomes.push(r),()=>active)
    active=false;resolveUpload({status:202,data:{data:{taskId:'accepted'}}});await pending
    assert.equal(calls,1);assert.deepEqual(outcomes,[])
  })
  await test('the same edit object reopens as update; double submit sends one captured payload with an empty description',async()=>{
    fixture.calls=[];let resolveValidation,resolveWrite
    fixture.handle=(method,url,data)=>new Promise(r=>resolveWrite=()=>r({data:{data:{id:4,...data}}}))
    const edit={id:4,name:'旧名称',description:'old',isPublic:false},m=mount(Form,{visible:true,editData:edit})
    m.app._instance.props.visible=false;await flush();m.state.resetForm();m.app._instance.props.visible=true;await flush()
    assert.equal(m.state.isEdit,true);assert.equal(m.state.editId,4);assert.equal(m.state.form.name,'旧名称')
    m.state.form.description='';m.state.formRef={validate:()=>new Promise(r=>resolveValidation=r),clearValidate(){}}
    const first=m.state.handleSubmit(),second=m.state.handleSubmit();resolveValidation(true);await flush()
    assert.equal(fixture.calls.length,1);assert.equal(fixture.calls[0].method,'PUT');assert.equal(fixture.calls[0].url,'/api/knowledge-bases/4');assert.equal(fixture.calls[0].data.description,'')
    resolveWrite();await Promise.all([first,second]);m.app.unmount()
  })
  await test('edit rejection preserves input and normalized error',async()=>{
    fixture.handle=async()=>{throw new ApiError('没有写权限',403)}
    const m=mount(Form,{visible:true,editData:{id:4,name:'原名称',description:'old',isPublic:false}})
    m.state.form.name='保留的输入';m.state.formRef={validate:async()=>true,clearValidate(){}}
    await m.state.handleSubmit();assert.equal(m.state.form.name,'保留的输入');assert.equal(m.state.submitError,'没有写权限');m.app.unmount()
  })
  await test('KB query rejects ambiguous IDs; validated navigation does not ask or select a fallback',async()=>{
    for(const value of ['0','-1','1e3','1.0',' 1','9007199254740992',['1','2'],null])assert.equal(parseKbId(value),null)
    assert.equal(parseKbId('901'),901)
    fixture.calls=[];fixture.handle=async()=>({data:{data:[{id:901,name:'accessible'}]}})
    const m=mount(Chat,{},reactive({name:'Chat',fullPath:'/chat?kbId=901',params:{},query:{kbId:'901'}}));await flush()
    assert.equal(m.state.selectedKbId,901);assert.ok(fixture.calls.every(x=>x.method==='GET'&&x.url==='/api/knowledge-bases'))
    m.route.query.kbId='999';await m.state.applyKbNavigation();assert.equal(m.state.selectedKbId,null);assert.match(m.state.kbNavigationError,/不可访问/)
    m.state.selectKbFromDropdown({id:901});assert.equal(m.state.kbNavigationError,'');m.app.unmount()
  })
  await test('manual selection wins over delayed navigation; loading failure retries only the accessible list',async()=>{
    fixture.calls=[];let resolveList
    fixture.handle=()=>new Promise(r=>resolveList=r)
    const m=mount(Chat,{},reactive({name:'Chat',fullPath:'/chat?kbId=901',params:{},query:{kbId:'901'}}))
    m.state.selectKbFromDropdown({id:902});resolveList({data:{data:[{id:901},{id:902}]}});await flush();assert.equal(m.state.selectedKbId,902)
    fixture.handle=async()=>{throw new ApiError('暂不可用',503)};await m.state.applyKbNavigation();assert.equal(m.state.selectedKbId,null);assert.equal(m.state.kbNavigationRetry,true)
    fixture.handle=async()=>({data:{data:[{id:901}]}});await m.state.applyKbNavigation();assert.equal(m.state.selectedKbId,901)
    assert.ok(fixture.calls.every(x=>x.method==='GET'));m.app.unmount()
  })
  await test('history ignores kbId preselection and remains read-only',async()=>{
    fixture.calls=[];fixture.handle=async(method,url)=>({data:{data:url==='/api/history/1'?{id:1,kbId:7,question:'q',answer:'a',citations:[]}:[{id:7,name:'historical'}]}})
    const m=mount(Chat,{},reactive({name:'ChatSession',fullPath:'/chat/1?kbId=901',params:{id:'1'},query:{kbId:'901'}}));await flush()
    assert.equal(m.state.selectedKbId,7);assert.equal(m.state.canSend,false);assert.equal(m.state.kbNavigationError,'');assert.ok(fixture.calls.every(x=>x.method==='GET'));m.app.unmount()
  })
  await test('confirmed document deletion refreshes three read paths; a failed read retry never deletes again',async()=>{
    fixture.calls=[];fixture.handle=async(method,url)=>{
      if(url.endsWith('/statistics'))throw new ApiError('统计暂不可用',503)
      return{data:{data:url.endsWith('/901')?{id:901,name:'kb'}:[]}}
    }
    const m=mount(Detail,{},reactive({name:'KnowledgeBaseDetail',fullPath:'/kb/901',params:{id:'901'},query:{}}));await flush();fixture.calls=[]
    m.state.handleDocumentDeleted();await flush()
    assert.deepEqual(fixture.calls.map(x=>x.url).sort(),['/api/knowledge-bases','/api/knowledge-bases/901/documents','/api/knowledge-bases/901/statistics'])
    assert.equal(m.state.confirmedAction,'删除已完成；');assert.equal(m.state.statsError,'统计暂不可用')
    fixture.calls=[];await m.state.loadStatistics();assert.equal(fixture.calls.length,1);assert.equal(fixture.calls[0].method,'GET');m.app.unmount()
  })
  await test('document delete cancel/denial and repeated clicks cannot hide or duplicate a target',async()=>{
    fixture.calls=[];let confirm
    fixture.confirm=()=>new Promise((resolve,reject)=>confirm={resolve,reject})
    const doc={id:3,title:'target',fileType:'txt',status:'COMPLETED'},m=mount(Docs,{kbId:901,documents:[doc],loading:false})
    const cancelled=m.state.handleDelete(doc);await m.state.handleDelete(doc);confirm.reject(new Error('cancel'));await cancelled;assert.deepEqual(fixture.calls,[])
    fixture.confirm=async()=>{};fixture.handle=async()=>{throw new ApiError('删除拒绝',403)};await m.state.handleDelete(doc)
    assert.equal(fixture.calls.length,1);assert.equal(m.state.deleteError,'删除拒绝');assert.equal(m.app._instance.props.documents.length,1);m.app.unmount()
  })
} finally {await server.close();delete globalThis.__f4;delete globalThis.localStorage;delete globalThis.document;delete globalThis.window}
