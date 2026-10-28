// Real Chrome UI and file-input checks against local synthetic HTTP fixtures only.
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve, join } from 'node:path'
import { selectSetting } from './settings-browser-helpers.mjs'
const { chromium } = createRequire(import.meta.url)(resolve(process.argv[2]))
const output = resolve(process.argv[3]); await mkdir(output, {recursive:true})
const browser = await chromium.launch({headless:true,executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe'})
const base = 'http://127.0.0.1:5188', records=[], outbound=[], errors=[]
async function fixture({theme='dark',width=1440,height=900,mode='ready'}={}) {
 const context=await browser.newContext({viewport:{width,height}})
 let profile={id:901,username:'合成验收',displayName:null,email:null,hasAvatar:false,revision:0},image=null,writes=0,reads=0
 await context.route('**/*',async route=>{
  const request=route.request(),url=new URL(request.url())
  if(url.origin!==base){outbound.push(url.origin);return route.abort()}
  if(!url.pathname.startsWith('/api/account/profile'))return route.continue()
  assert.equal(request.headers().authorization,'Bearer fixture-access');assert.equal(url.search,'')
  const reply=(status,data,errorCode)=>route.fulfill({status,headers:{'Cache-Control':'no-store'},json:status===200?{code:200,data}:{errorCode,message:'合成资料错误'}})
  if(mode==='legacy')return reply(404)
  if(request.method()==='GET' && url.pathname.endsWith('/avatar'))return mode==='avatar-read-error'?reply(503):image?route.fulfill({contentType:'image/png',body:image}):reply(404)
  if(request.method()==='GET'){reads++;return reply(200,profile)}
  assert.equal(request.method(),'PUT');writes++
  const form=await new Response(request.postDataBuffer(),{headers:{'Content-Type':request.headers()['content-type']}}).formData()
  const input=JSON.parse(await form.get('profile').text())
  assert.deepEqual(Object.keys(input).sort(),['displayName','email','expectedRevision','avatarAction'].sort())
  if(mode==='email')return reply(409,null,'PROFILE_EMAIL_UNAVAILABLE')
  if(mode==='unauthorized')return reply(401,null,'AUTH_001')
  if(mode==='save-error')return reply(500,null,'SYS_500')
  if(mode==='conflict'){profile={...profile,displayName:'服务器新昵称',revision:profile.revision+1};return reply(409,null,'PROFILE_REVISION_CONFLICT')}
  assert.equal(input.expectedRevision,profile.revision)
  if(input.avatarAction==='REPLACE'){image=Buffer.from(await form.get('avatar').arrayBuffer());assert.equal(image.readUInt32BE(16),256);assert.equal(image.readUInt32BE(20),256)}
  if(input.avatarAction==='REMOVE')image=null
  profile={...profile,displayName:input.displayName,email:input.email,hasAvatar:!!image,revision:profile.revision+1}
  if(mode==='unknown')return route.abort('failed')
  if(mode==='slow')await new Promise(resolve=>setTimeout(resolve,800))
  return reply(200,profile)
 })
 await context.addInitScript(theme=>{localStorage.setItem('rag_accessToken','fixture-access');localStorage.setItem('rag_refreshToken','fixture-refresh');localStorage.setItem('theme',theme)},theme)
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message))
 await page.goto(base+'/chat?fixture=workspace-ready')
 return {context,page,get profile(){return profile},get writes(){return writes},get reads(){return reads},setMode(next){mode=next}}
}
async function open(page){
 if(page.viewportSize().width<=768)await page.getByRole('button',{name:'打开侧栏',exact:true}).click()
 await page.locator('.user-trigger').click();await page.getByRole('button',{name:'设置',exact:true}).click()
 await category(page,'account','账户与工作空间')
 await page.getByRole('textbox',{name:'显示昵称',exact:true}).waitFor()
}
async function category(page,value,label){const mobile=page.locator('#settings-category');if(await mobile.isVisible())await selectSetting(mobile,value);else await page.getByRole('button',{name:label,exact:true}).click()}
async function png(page){return Buffer.from(await page.evaluate(()=>{const c=document.createElement('canvas');c.width=400;c.height=200;const g=c.getContext('2d');g.fillStyle='#31a892';g.fillRect(0,0,400,200);return c.toDataURL().split(',')[1]}),'base64')}
async function upload(page,buffer){await page.locator('input[type=file]').setInputFiles({name:'fixture.png',mimeType:'image/png',buffer});await page.getByText('尚未保存',{exact:true}).waitFor()}
async function saved(page){await page.getByText('个人资料已保存。',{exact:true}).waitFor()}
async function close(f){await f.context.close()}
try{
 for(const theme of ['dark','light'])for(const [width,height]of [[1440,900],[1024,768],[390,844],[320,640],[844,390]]){
  const f=await fixture({theme,width,height}),{page}=f;await open(page)
  await page.getByRole('textbox',{name:'显示昵称',exact:true}).fill('林同学 · 合成资料')
  await page.getByRole('textbox',{name:'邮箱',exact:true}).fill('lin@example.test')
  await upload(page,await png(page));await page.screenshot({path:join(output,`${theme}-${width}x${height}-draft.png`)})
  await page.getByRole('button',{name:'保存修改',exact:true}).click();await saved(page)
  assert.equal(f.writes,1);assert.equal(f.profile.hasAvatar,true)
  await page.screenshot({path:join(output,`${theme}-${width}x${height}-saved.png`)})
  const bounds=await page.locator('.settings-modal').evaluate(e=>{const r=e.getBoundingClientRect();return {x:r.x,y:r.y,right:r.right,bottom:r.bottom,w:innerWidth,h:innerHeight,overflow:document.documentElement.scrollWidth>innerWidth}})
  assert.ok(bounds.x>=0 && bounds.y>=0 && bounds.right<=bounds.w+.1 && bounds.bottom<=bounds.h+.1,JSON.stringify(bounds));assert.equal(bounds.overflow,false)
  records.push({case:'layout-and-atomic-save',theme,width,height,status:'PASS'});await close(f)
 }
 {
  const f=await fixture(),{page}=f;const question=page.locator('textarea').first();await question.fill('保留问答草稿')
  await page.evaluate(()=>{window.chatSentinel=document.querySelector('textarea');window.revoked=[];const revoke=URL.revokeObjectURL;URL.revokeObjectURL=value=>{window.revoked.push(value);revoke(value)}})
  await open(page);const image=await png(page);await upload(page,image)
  const localPreview=await page.locator('.profile-picture img').getAttribute('src')
  assert.equal(await page.locator('.user-trigger img').count(),0)
  await page.getByRole('textbox',{name:'显示昵称',exact:true}).fill('新昵称')
  await page.getByRole('button',{name:'取消',exact:true}).click();assert.equal(f.writes,0);assert.equal(await page.getByRole('textbox',{name:'显示昵称',exact:true}).inputValue(),'')
  assert.ok(await page.evaluate(value=>window.revoked.includes(value),localPreview))
  await upload(page,image);await page.getByRole('textbox',{name:'显示昵称',exact:true}).fill('新昵称')
  await page.getByRole('button',{name:'保存修改',exact:true}).click();await saved(page)
  await page.locator('.user-trigger img').waitFor({state:'attached'});assert.match(await page.locator('.user-name').innerText(),/新昵称/)
  assert.equal(await page.evaluate(()=>document.querySelector('textarea')===window.chatSentinel),true)
  await page.keyboard.press('Escape');assert.equal(await question.inputValue(),'保留问答草稿')
  await page.reload();await open(page);assert.equal(await page.getByRole('textbox',{name:'显示昵称',exact:true}).inputValue(),'新昵称')
  await page.getByRole('button',{name:'移除头像',exact:true}).click();await page.getByRole('button',{name:'保存修改',exact:true}).click();await saved(page);assert.equal(f.profile.hasAvatar,false)
  await category(page,'data','数据管理');await page.getByRole('button',{name:'重置本地偏好',exact:true}).click();await page.getByRole('button',{name:'确认重置',exact:true}).click()
  await category(page,'account','账户与工作空间');assert.equal(await page.getByRole('textbox',{name:'显示昵称',exact:true}).inputValue(),'新昵称');assert.equal(f.writes,2)
  records.push({case:'cancel-blob-revoke-shared-avatar-reload-no-chat-remount-reset-isolation',status:'PASS'});await close(f)
 }
 {
  const f=await fixture(),{page}=f;await open(page);await page.getByRole('textbox',{name:'显示昵称',exact:true}).fill('保留草稿')
  for(const intent of ['close','tab','backdrop']){
   if(intent==='close')await page.keyboard.press('Escape');else if(intent==='tab')await category(page,'appearance','外观');else await page.locator('.settings-backdrop').click({position:{x:2,y:2}})
   await page.getByRole('heading',{name:'要放弃尚未保存的修改？',exact:true}).waitFor();await page.getByRole('button',{name:'继续编辑',exact:true}).click();assert.equal(await page.getByRole('textbox',{name:'显示昵称',exact:true}).inputValue(),'保留草稿')
  }
  await page.keyboard.press('Escape');await page.getByRole('button',{name:'放弃修改',exact:true}).click();assert.equal(f.writes,0)
  records.push({case:'dirty-close-tab-backdrop-continue-discard',status:'PASS'});await close(f)
 }
 for(const mode of ['email','unknown','conflict','save-error','unauthorized','slow']){
  const f=await fixture({mode}),{page}=f;await open(page);await page.getByRole('textbox',{name:'显示昵称',exact:true}).fill('草稿昵称');await page.getByRole('textbox',{name:'邮箱',exact:true}).fill('draft@example.test')
  await page.getByRole('button',{name:'保存修改',exact:true}).click()
  if(mode==='email'){await page.getByText('此邮箱暂不可用，请更换邮箱。',{exact:true}).waitFor();assert.equal(await page.getByRole('textbox',{name:'邮箱',exact:true}).inputValue(),'draft@example.test')}
  else if(mode==='unauthorized'){await page.getByText('登录状态已变化，请重新登录后读取资料。',{exact:true}).waitFor()}
  else if(mode==='slow'){await page.getByRole('button',{name:'关闭设置',exact:true}).click();await page.getByText('正在处理个人资料，请稍候。',{exact:true}).waitFor();await saved(page)}
  else {
   const failureText=mode==='unknown'?'网络连接异常，请重试保存。':mode==='conflict'?'资料已在其他位置更新，请重试保存。':'保存失败，请重试。'
   await page.getByText(failureText,{exact:true}).waitFor()
   assert.equal(await page.getByRole('textbox',{name:'显示昵称',exact:true}).inputValue(),'草稿昵称')
   assert.equal(await page.getByRole('button',{name:'保存修改',exact:true}).isDisabled(),false);assert.equal(f.writes,1)
   assert.doesNotMatch(await page.locator('.account-profile').innerText(),/草稿|服务器昵称|保存结果暂未确认/)
   await page.screenshot({path:join(output,mode+'.png')})
   f.setMode('ready');await page.getByRole('button',{name:'保存修改',exact:true}).click();await saved(page)
   assert.equal(f.profile.revision,mode==='save-error'?1:2)
   assert.match(await page.locator('.user-name').innerText(),/草稿昵称/)
  }
  assert.equal(f.writes,['unknown','conflict','save-error'].includes(mode)?2:1);records.push({case:mode+'-simple-feedback-and-explicit-retry',status:'PASS'});await close(f)
 }
 {
  const f=await fixture({mode:'avatar-read-error'}),{page}=f;await open(page)
  await upload(page,await png(page));await page.getByRole('textbox',{name:'显示昵称',exact:true}).fill('即时生效昵称')
  await page.getByRole('button',{name:'保存修改',exact:true}).click();await saved(page)
  await page.locator('.user-trigger img').waitFor({state:'attached'});assert.match(await page.locator('.user-name').innerText(),/即时生效昵称/)
  assert.equal(await page.getByRole('button',{name:'保存修改',exact:true}).isDisabled(),true)
  assert.doesNotMatch(await page.locator('.account-profile').innerText(),/草稿|保存结果暂未确认/)
  await page.screenshot({path:join(output,'immediate-avatar-and-nickname.png')})
  records.push({case:'confirmed-save-publishes-avatar-without-second-download',status:'PASS'});await close(f)
 }
 {
  const f=await fixture(),{page}=f;await open(page);await page.getByRole('textbox',{name:'显示昵称',exact:true}).fill('😀'.repeat(51));await page.getByRole('button',{name:'保存修改',exact:true}).click();await page.getByText('昵称最多 50 个字符，不能含控制字符。',{exact:true}).waitFor();assert.equal(f.writes,0)
  await page.locator('input[type=file]').setInputFiles({name:'x.svg',mimeType:'image/svg+xml',buffer:Buffer.from('<svg/>')});await page.getByText('请选择 JPG 或 PNG 图片。',{exact:true}).waitFor()
  await page.locator('input[type=file]').setInputFiles({name:'x.png',mimeType:'image/png',buffer:Buffer.alloc(2*1024*1024+1)});await page.getByText('头像不能超过 2 MB。',{exact:true}).waitFor()
  records.push({case:'unicode-format-and-file-size-validation',status:'PASS'});await close(f)
 }
 {
  const f=await fixture({mode:'legacy'}),{page}=f;await page.locator('.user-trigger').click();await page.getByRole('button',{name:'设置',exact:true}).click();await category(page,'account','账户与工作空间');await page.getByText('当前服务暂不支持资料编辑，以下仅展示已有登录资料。',{exact:true}).waitFor();assert.equal(await page.getByRole('button',{name:'保存修改',exact:true}).count(),0)
  records.push({case:'legacy-backend-readonly-fallback',status:'PASS'});await close(f)
 }
 {
  const f=await fixture(),{page}=f;await open(page)
  const jpeg=Buffer.from(await page.evaluate(()=>{const c=document.createElement('canvas');c.width=400;c.height=200;const g=c.getContext('2d');g.fillStyle='red';g.fillRect(0,0,200,200);g.fillStyle='blue';g.fillRect(200,0,200,200);return c.toDataURL('image/jpeg',1).split(',')[1]}),'base64')
  const rotated=Buffer.concat([jpeg.subarray(0,2),Buffer.from('ffe1002245786966000049492a0008000000010012010300010000000600000000000000','hex'),jpeg.subarray(2)])
  await page.locator('input[type=file]').setInputFiles({name:'orientation6.jpg',mimeType:'image/jpeg',buffer:rotated});await page.getByText('尚未保存',{exact:true}).waitFor()
  const colors=await page.locator('.profile-picture img').evaluate(async img=>{await img.decode();const c=document.createElement('canvas');c.width=c.height=256;const g=c.getContext('2d');g.drawImage(img,0,0);return [Array.from(g.getImageData(128,20,1,1).data),Array.from(g.getImageData(128,235,1,1).data)]})
  assert.ok(colors[0][0]>200 && colors[0][2]<50);assert.ok(colors[1][2]>200 && colors[1][0]<50)
  await page.getByRole('button',{name:'保存修改',exact:true}).click();await saved(page)
  records.push({case:'jpeg-exif6-oriented-preview-and-upload',colors,status:'PASS'});await close(f)
 }
 {
  const f=await fixture(),{page}=f;await page.goto(base+'/chat?fixture=chat-stop');let streams=0
  page.on('request',request=>{if(request.url().includes('/api/qa/ask/stream'))streams++})
  await page.getByRole('button',{name:'选择知识库',exact:true}).click();await page.locator('.kb-menu-item').filter({hasText:'合成知识库'}).click()
  await page.getByRole('button',{name:'高级选项',exact:true}).click();await page.locator('input[name=response-mode][value=stream]').click();await page.locator('.pill-input').click();await page.locator('.pill-input').fill('保存资料同时接收合成回答');await page.getByRole('button',{name:'发送问题',exact:true}).click();await page.getByRole('button',{name:'停止接收',exact:true}).waitFor()
  await page.evaluate(()=>{window.streamNode=document.querySelector('.chat-panel')||document.querySelector('textarea')})
  await open(page);await page.getByRole('textbox',{name:'显示昵称',exact:true}).fill('流式期间改名');await page.getByRole('button',{name:'保存修改',exact:true}).click();await saved(page)
  await page.keyboard.press('Escape');assert.equal(await page.evaluate(()=>window.streamNode.isConnected),true);assert.equal(streams,1);await page.getByRole('button',{name:'停止接收',exact:true}).click();assert.equal(await page.locator('.source-card').count(),0)
  records.push({case:'profile-save-keeps-single-stream-and-chat-instance',status:'PASS'});await close(f)
 }
 assert.deepEqual(outbound,[]);assert.deepEqual(errors,[])
 await writeFile(join(output,'evidence.json'),JSON.stringify({records,outbound,errors,scope:'Synthetic HTTP and real Chrome file inputs; no real DB/accounts/providers'},null,2));console.log(JSON.stringify({groups:records.length,status:'PASS'}))
}catch(error){await writeFile(join(output,'failure.txt'),String(error.stack));console.error(error);process.exitCode=1}finally{await browser.close()}
