import { selectSetting, settingValue } from './settings-browser-helpers.mjs'
// Synthetic browser regression; no real providers or external network.
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve, join } from 'node:path'
const { chromium }=createRequire(import.meta.url)(resolve(process.argv[2]))
const output=resolve(process.argv[3]);await mkdir(output,{recursive:true})
const browser=await chromium.launch({headless:true,executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe'})
const base='http://127.0.0.1:5188',key='rag.ui.preferences.v1',records=[],outbound=[],errors=[],calls=[]
const categories=[['appearance','外观'],['conversation','问答偏好'],['account','账户与工作空间'],['data','数据管理'],['about','关于与帮助']]
async function contextFor(theme='dark',width=1440,height=900,extra={}) {
 const context=await browser.newContext({viewport:{width,height},...extra})
 await context.route('**/*',route=>{if(new URL(route.request().url()).origin===base)return route.continue();outbound.push(route.request().url());return route.abort()})
 await context.addInitScript(({theme})=>{if(location.pathname!=='/login'){localStorage.setItem('rag_accessToken','fixture-access');localStorage.setItem('rag_refreshToken','fixture-refresh')}if(!localStorage.getItem('theme'))localStorage.setItem('theme',theme);localStorage.setItem('settings-sentinel','preserved')},{theme})
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));page.on('request',r=>{if(r.url().includes('/api/qa/ask'))calls.push({path:new URL(r.url()).pathname,method:r.method()})})
 return {context,page}
}
async function openSettings(page) {
 const mobile=page.getByRole('button',{name:'打开侧栏',exact:true});if(page.viewportSize().width<=768){await mobile.waitFor({state:'visible'});await mobile.click()}
 await page.getByRole('button',{name:'合成验收，打开用户菜单',exact:true}).click();await page.getByRole('button',{name:'设置',exact:true}).click();await page.getByRole('dialog',{name:'设置',exact:true}).waitFor()
}
async function category(page,id,label) {
 const mobile=page.locator('#settings-category');if(await mobile.isVisible())await selectSetting(mobile,id);else await page.getByRole('button',{name:label,exact:true}).click()
 await page.getByRole('heading',{name:label,exact:true,level:2}).waitFor()
}
async function bounds(page) {
 const r=await page.getByRole('dialog',{name:'设置',exact:true}).evaluate(e=>{const r=e.getBoundingClientRect();return {x:r.x,y:r.y,right:r.right,bottom:r.bottom,w:innerWidth,h:innerHeight,overflow:document.documentElement.scrollWidth>innerWidth,inert:document.getElementById('app').inert}})
 assert.ok(r.x>=0&&r.y>=0&&r.right<=r.w+.1&&r.bottom<=r.h+.1,JSON.stringify(r));assert.equal(r.overflow,false);assert.equal(r.inert,true)
}
async function chooseKb(page){await page.getByRole('button',{name:'选择知识库',exact:true}).click();await page.locator('.kb-menu-item').filter({hasText:'合成知识库'}).click()}
try {
 for(const theme of ['dark','light'])for(const [width,height]of [[1440,900],[1024,768],[390,844],[320,640],[844,390]]) {
  const {context,page}=await contextFor(theme,width,height);await page.goto(base+'/chat?fixture=workspace-ready');await openSettings(page)
  for(const [id,label]of categories){await category(page,id,label);await bounds(page);await page.screenshot({path:join(output,`${theme}-${width}-${height}-${id}.png`)});if(id==='appearance'){await page.getByRole('combobox',{name:'减少动态效果',exact:true}).scrollIntoViewIfNeeded();const r=await page.getByRole('combobox',{name:'减少动态效果',exact:true}).boundingBox();assert.ok(r.y>=0&&r.y+r.height<=height)}}
  await category(page,'appearance','外观');await selectSetting(page.getByRole('combobox',{name:'回答字号',exact:true}),'large');assert.equal(await page.locator('.preview-answer').evaluate(e=>getComputedStyle(e).fontSize),'17px')
  await page.getByRole('button',{name:'关闭设置',exact:true}).focus();for(let i=0;i<12;i++){await page.keyboard.press('Tab');assert.equal(await page.evaluate(()=>!!document.activeElement?.closest('.settings-modal')),true)}
  await category(page,'data','数据管理');await page.getByRole('button',{name:'重置本地偏好',exact:true}).click();assert.equal(await page.getByRole('button',{name:'取消',exact:true}).evaluate(e=>e===document.activeElement),true)
  await page.keyboard.press('Escape');assert.equal(await page.getByRole('dialog',{name:'设置',exact:true}).count(),1);assert.equal(await page.locator('#settings-reset-confirm').count(),0)
  await page.getByRole('button',{name:'重置本地偏好',exact:true}).click();await page.getByRole('button',{name:'确认重置',exact:true}).click();assert.equal(await page.evaluate(key=>JSON.parse(localStorage.getItem(key)).answerTextSize,key),'standard');assert.equal(await page.evaluate(()=>localStorage.getItem('settings-sentinel')),'preserved');assert.equal(await page.evaluate(()=>localStorage.getItem('rag_accessToken')),'fixture-access')
  await page.keyboard.press('Escape');assert.equal(await page.getByRole('dialog',{name:'设置',exact:true}).count(),0);assert.equal(await page.evaluate(()=>document.getElementById('app').inert),false);assert.equal(await page.evaluate(()=>document.activeElement!==document.body),true)
  records.push({case:'layout-navigation-reset-focus',theme,width,height,status:'PASS'});await context.close()
 }
 {
  const {context,page}=await contextFor();await page.goto(base+'/chat?fixture=chat-sources');await openSettings(page);await page.locator('.theme-option').filter({hasText:'浅色'}).click();assert.equal(await page.locator('html').getAttribute('class'),'light');await selectSetting(page.getByRole('combobox',{name:'回答字号',exact:true}),'large');await selectSetting(page.getByRole('combobox',{name:'减少动态效果',exact:true}),'reduce');await category(page,'conversation','问答偏好');await selectSetting(page.getByRole('combobox',{name:'发送快捷键',exact:true}),'modifier-enter');await selectSetting(page.getByRole('combobox',{name:'来源片段',exact:true}),'expanded');await page.keyboard.press('Escape');await page.reload();await openSettings(page);assert.equal(await settingValue(page.getByRole('combobox',{name:'回答字号',exact:true})),'large');await page.keyboard.press('Escape');await chooseKb(page)
  const input=page.locator('.pill-input');await input.fill('合成草稿');const before=calls.length;await input.press('Enter');assert.equal(calls.length,before);assert.ok((await input.inputValue()).includes('\n'));await input.fill('输入法合成问题');await input.dispatchEvent('compositionstart');await input.dispatchEvent('keydown',{key:'Enter',ctrlKey:true,isComposing:true,keyCode:229});await input.dispatchEvent('compositionend');await input.dispatchEvent('keydown',{key:'Enter',ctrlKey:true});assert.equal(calls.length,before)
  await page.getByRole('button',{name:'发送问题',exact:true}).click();await page.locator('.source-card').first().waitFor();assert.equal(calls.length,before+1);assert.equal(calls.at(-1).path,'/api/qa/ask');assert.equal(await page.locator('.answer-content').evaluate(e=>getComputedStyle(e).fontSize),'17px');assert.equal(await page.locator('.source-card').first().evaluate(e=>e.open),true)
  await page.locator('.source-card summary').first().click();assert.equal(await page.locator('.source-card').first().evaluate(e=>e.open),false);await input.fill('切换设置时保留的草稿');await openSettings(page);await category(page,'conversation','问答偏好');await selectSetting(page.getByRole('combobox',{name:'来源片段',exact:true}),'collapsed');await page.keyboard.press('Escape');assert.equal(await input.inputValue(),'切换设置时保留的草稿');assert.equal(await page.locator('.source-card').first().evaluate(e=>e.open),false);assert.equal(calls.length,before+1)
  await Promise.all([page.waitForResponse(r=>r.url().endsWith('/api/qa/ask')&&r.request().method()==='POST'),input.press('Control+Enter')]);assert.equal(calls.length,before+2)
  records.push({case:'persistence-ime-send-source-draft',status:'PASS'});await context.close()
 }
 {
  const {context,page}=await contextFor();await page.goto(base+'/chat?fixture=workspace-ready');await openSettings(page);await page.evaluate(()=>{const set=Storage.prototype.setItem;Storage.prototype.setItem=function(key,value){if(key==='theme'||key==='rag.ui.preferences.v1')throw new DOMException('blocked','SecurityError');return set.call(this,key,value)}})
  await selectSetting(page.getByRole('combobox',{name:'回答字号',exact:true}),'large');assert.match(await page.locator('.persistence-status').innerText(),/无法保存/);assert.equal(await page.locator('.preview-answer').evaluate(e=>getComputedStyle(e).fontSize),'17px');await category(page,'data','数据管理');await page.getByRole('button',{name:'重置本地偏好',exact:true}).click();await page.getByRole('button',{name:'确认重置',exact:true}).click();assert.match(await page.locator('.reset-result').innerText(),/刷新后可能恢复旧设置/);assert.equal(await page.evaluate(()=>localStorage.getItem('settings-sentinel')),'preserved');records.push({case:'storage-write-failure',status:'PASS'});await context.close()
 }
 for(const reducedMotion of ['reduce','no-preference']) {
  const {context,page}=await contextFor('dark',1440,900,{reducedMotion});await page.goto(base+'/chat?fixture=workspace-ready');await openSettings(page);await selectSetting(page.getByRole('combobox',{name:'减少动态效果',exact:true}),'system');assert.equal(await page.locator('html').getAttribute('data-motion'),reducedMotion==='reduce'?'reduce':'no-preference');await selectSetting(page.getByRole('combobox',{name:'减少动态效果',exact:true}),'reduce');await page.evaluate(()=>{localStorage.removeItem('rag_accessToken');localStorage.removeItem('rag_refreshToken')});await page.goto(base+'/login');await page.locator('.particle-pause').waitFor();assert.equal(await page.locator('.particle-pause').isDisabled(),true);records.push({case:'system-local-reduce-login-'+reducedMotion,status:'PASS'});await context.close()
 }
 for(const scenario of ['chat-stop','chat-incomplete','chat-no-answer']) {
  const {context,page}=await contextFor();await page.goto(base+'/chat?fixture='+scenario);await chooseKb(page);await page.getByRole('button',{name:'高级选项',exact:true}).click();await page.locator('input[name=response-mode][value=stream]').click();await page.locator('.pill-input').click();await page.locator('.pill-input').fill('流式合成问题');const before=calls.length;await page.getByRole('button',{name:'发送问题',exact:true}).click();await page.getByRole('button',{name:'停止接收',exact:true}).waitFor();await openSettings(page);await selectSetting(page.getByRole('combobox',{name:'回答字号',exact:true}),'large');await category(page,'conversation','问答偏好');await selectSetting(page.getByRole('combobox',{name:'来源片段',exact:true}),'expanded');await page.keyboard.press('Escape');if(scenario==='chat-stop')await page.getByRole('button',{name:'停止接收',exact:true}).click();else await page.getByRole('button',{name:'停止接收',exact:true}).waitFor({state:'detached'});assert.equal(calls.length,before+1);assert.equal(calls.at(-1).path,'/api/qa/ask/stream');assert.equal(await page.locator('.source-card').count(),0);records.push({case:scenario+'-preserves-terminal',status:'PASS'});await context.close()
 }
 assert.equal(outbound.length,0);assert.deepEqual(errors,[]);await writeFile(join(output,'browser-evidence.json'),JSON.stringify({records,screenshots:50,outbound,errors,syntheticAskCalls:calls},null,2));console.log(JSON.stringify({groups:records.length,screenshots:50,askCalls:calls.length,outbound:outbound.length,pageErrors:errors.length,status:'PASS'}))
} catch(error){await writeFile(join(output,'failure.txt'),String(error.stack));console.error(error);process.exitCode=1}finally{await browser.close()}



