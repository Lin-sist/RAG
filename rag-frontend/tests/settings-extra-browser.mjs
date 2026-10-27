import { selectSetting, settingValue } from './settings-browser-helpers.mjs'
import assert from 'node:assert/strict'
import {createRequire} from 'node:module'
import {mkdir,writeFile} from 'node:fs/promises'
import {resolve,join} from 'node:path'
const {chromium}=createRequire(import.meta.url)(resolve(process.argv[2]))
const output=resolve(process.argv[3]);await mkdir(output,{recursive:true})
const browser=await chromium.launch({headless:true,executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe'})
const base='http://127.0.0.1:5188',records=[],outbound=[],errors=[],colors=[]
async function setup(theme='dark',options={}) {
 const context=await browser.newContext({viewport:{width:1440,height:900},...options})
 await context.route('**/*',route=>{if(new URL(route.request().url()).origin===base)return route.continue();outbound.push('blocked');return route.abort()})
 await context.addInitScript(theme=>{if(location.pathname!=='/login'){localStorage.setItem('rag_accessToken','fixture-access');localStorage.setItem('rag_refreshToken','fixture-refresh')}localStorage.setItem('theme',theme)},theme)
 const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));return {context,page}
}
async function open(page,account=false) {
 const mobile=page.getByRole('button',{name:'打开侧栏',exact:true});if(page.viewportSize().width<=768){await mobile.waitFor();await mobile.click()}
 await page.locator('.user-trigger').click();await page.getByRole('button',{name:account?'个人资料':'设置',exact:true}).click()
 await page.getByRole('dialog',{name:'设置',exact:true}).waitFor()
}
function luminance(rgb){const a=rgb.match(/[\d.]+/g).slice(0,3).map(x=>+x/255).map(x=>x<=.04045?x/12.92:((x+.055)/1.055)**2.4);return a[0]*.2126+a[1]*.7152+a[2]*.0722}
function ratio(a,b){const x=[luminance(a),luminance(b)].sort((a,b)=>a-b);return(x[1]+.05)/(x[0]+.05)}
try {
 for(const theme of ['dark','light']) {
  const {context,page}=await setup(theme);await page.goto(base+'/chat?fixture=workspace-ready');await open(page)
  const palette=await page.locator('.settings-modal').evaluate(e=>{const s=getComputedStyle(e);return {background:s.backgroundColor,text:s.color,secondary:s.getPropertyValue('--rag-text-secondary'),control:getComputedStyle(e.querySelector('.el-select__wrapper')).borderColor}})
  const secondary=await page.locator('.section-description').evaluate(e=>getComputedStyle(e).color)
  const result={theme,text:ratio(palette.text,palette.background),secondary:ratio(secondary,palette.background),control:ratio(palette.control,palette.background)}
  assert.ok(result.text>=4.5&&result.secondary>=4.5&&result.control>=3);colors.push(result)
  // 200% text enlargement plus a 720 CSS-pixel viewport is a reflow check, not desktop browser zoom.
  await page.setViewportSize({width:720,height:450});await page.evaluate(()=>document.documentElement.style.fontSize='32px');await selectSetting(page.getByRole('combobox',{name:'回答字号',exact:true}),'large');await page.locator('.preview-answer').scrollIntoViewIfNeeded()
  assert.equal(await page.locator('.preview-answer').evaluate(e=>getComputedStyle(e).fontSize),'34px');assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false)
  await selectSetting(page.locator('#settings-category'),'about');await page.locator('.version-row').scrollIntoViewIfNeeded();assert.equal(await page.locator('.version-row').isVisible(),true)
  await page.evaluate(()=>document.documentElement.dir='rtl');assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false)
  await page.screenshot({path:join(output,`reflow-rtl-${theme}.png`)});records.push({case:'contrast-text-reflow-rtl',theme,status:'PASS'});await context.close()
 }
 {
  const {context,page}=await setup('dark',{forcedColors:'active'});await page.goto(base+'/chat?fixture=workspace-ready');await open(page);await page.getByRole('radio',{name:'深色',exact:true}).focus();assert.equal(await page.locator('.theme-option.selected').evaluate(e=>getComputedStyle(e).borderTopWidth),'2px');await page.screenshot({path:join(output,'forced-colors-focus.png')});records.push({case:'forced-colors-focus',status:'PASS'});await context.close()
 }
 for(const scene of ['workspace-error','workspace-long']) {
  const {context,page}=await setup();await page.goto(base+'/chat?fixture='+scene);await open(page,true)
  if(scene==='workspace-error'){await page.getByRole('dialog',{name:'设置',exact:true}).getByRole('button',{name:'重试工作空间资料',exact:true}).click();await page.getByText('工作空间A · 合成预览',{exact:true}).last().waitFor()}
  else assert.ok((await page.locator('.settings-modal .workspace-name').innerText()).length>80)
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);await page.screenshot({path:join(output,scene+'-account.png')});records.push({case:scene+'-account',status:'PASS'});await context.close()
 }
 {
  const {context,page}=await setup();await context.addInitScript(()=>{const get=Storage.prototype.getItem;Storage.prototype.getItem=function(key){if(key==='rag.ui.preferences.v1')throw new DOMException('blocked','SecurityError');return get.call(this,key)}})
  await page.goto(base+'/chat?fixture=workspace-ready');await open(page);assert.equal(await settingValue(page.getByRole('combobox',{name:'回答字号',exact:true})),'standard');assert.match(await page.locator('.persistence-status').innerText(),/无法保存/);records.push({case:'storage-read-failure',status:'PASS'});await context.close()
 }
 {
  const {context,page}=await setup();await page.goto(base+'/chat?fixture=workspace-ready');await open(page);await page.getByRole('button',{name:'数据管理',exact:true}).click();await page.getByRole('link',{name:'打开历史记录',exact:true}).click();await page.waitForURL('**/history');assert.equal(await page.getByRole('dialog',{name:'设置',exact:true}).count(),0);records.push({case:'history-navigation',status:'PASS'});await context.close()
 }
 for(const status of [200,503]) {
  const {context,page}=await setup();let logoutCalls=0
  await context.route('**/auth/logout',async route=>{logoutCalls++;await new Promise(resolve=>setTimeout(resolve,300));await route.fulfill({status,json:status===200?{code:200}:{errorCode:'FIXTURE_ONLY',message:'合成退出失败'}})})
  await page.goto(base+'/chat?fixture=workspace-ready');await open(page,true);await page.getByRole('button',{name:'退出登录',exact:true}).click();assert.equal(await page.getByRole('button',{name:'正在退出…',exact:true}).isDisabled(),true);await page.waitForURL('**/login');assert.equal(logoutCalls,1);assert.equal(await page.evaluate(()=>localStorage.getItem('rag_accessToken')),null);if(status===503)await page.getByText('本地登录信息已清除，服务端退出未确认。',{exact:true}).waitFor();records.push({case:'logout-'+status,status:'PASS'});await context.close()
 }
 assert.deepEqual(outbound,[]);assert.deepEqual(errors,[]);await writeFile(join(output,'extra-evidence.json'),JSON.stringify({records,colors,outbound,errors,desktopZoom:'Not verified; used text enlargement and CSS viewport reflow'},null,2));console.log(JSON.stringify({groups:records.length,colors,status:'PASS'}))
}catch(error){await writeFile(join(output,'extra-failure.txt'),String(error.stack));console.error(error);process.exitCode=1}finally{await browser.close()}



