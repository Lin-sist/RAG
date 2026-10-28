// Headless Chrome regression for the client picker. Cursor intents are intercepted, never launched.
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve, join } from 'node:path'
const { chromium } = createRequire(import.meta.url)(resolve(process.argv[2]))
const output = resolve(process.argv[3]); await mkdir(output,{recursive:true})
const base='http://127.0.0.1:5194', records=[], errors=[], outbound=[]
const browser=await chromium.launch({headless:true,executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe'})
async function fixture({theme='light',width=1440,height=900,mode='ready'}={}){
  const context=await browser.newContext({viewport:{width,height}})
  await context.route('**/*',route=>{if(new URL(route.request().url()).origin===base)return route.continue();outbound.push(route.request().url());return route.abort()})
  await context.addInitScript(theme=>{
    localStorage.setItem('rag_accessToken','synthetic-access');localStorage.setItem('rag_refreshToken','synthetic-refresh');localStorage.setItem('theme',theme)
    Object.defineProperty(navigator,'clipboard',{configurable:true,value:{writeText:text=>{window.copied=text;if(window.copyFault)return Promise.reject(Error('fixture denied'));if(window.copyHold)return new Promise(resolve=>{window.releaseCopy=resolve});return Promise.resolve()}}})
    document.addEventListener('click',event=>{const a=event.target.closest?.('a[href^="cursor:"]');if(a){event.preventDefault();window.cursorIntents=(window.cursorIntents||[]).concat(a.href)}},true)
  },theme)
  const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message))
  await page.request.get(base+'/__mcp-fixture?mode='+mode)
  await page.goto(base+'/integrations/mcp');await page.locator('.workspace-name').waitFor({state:'attached'})
  await page.waitForFunction(()=>{const state=document.querySelector('.metadata-state');return !!state&&!state.textContent.includes('正在读取')})
  return {context,page}
}
async function open(page){await page.getByRole('button',{name:'接入 AI 助手',exact:true}).click();await page.getByRole('dialog',{name:'接入 AI 助手',exact:true}).waitFor()}
async function choose(page,name){await page.getByRole('button',{name:'选择 '+name,exact:true}).click();await page.getByLabel('MCP 服务地址',{exact:true}).waitFor()}
async function checkBounds(page){
  const b=await page.locator('.mcp-connect-dialog').evaluate(e=>{const r=e.getBoundingClientRect();return{x:r.x,y:r.y,right:r.right,bottom:r.bottom,w:innerWidth,h:innerHeight,overflow:document.documentElement.scrollWidth>innerWidth,inert:document.getElementById('app').inert}})
  assert.ok(b.x>=0&&b.y>=0&&b.right<=b.w+.5&&b.bottom<=b.h+.5,JSON.stringify(b));assert.equal(b.overflow,false);assert.equal(b.inert,true)
  const horizontal=await page.locator('.mcp-connect-dialog').evaluate(root=>[...root.querySelectorAll('button,a,input,pre,textarea')].filter(e=>e.getClientRects().length).filter(e=>{const r=e.getBoundingClientRect();return r.x<0||r.right>innerWidth+.5}).map(e=>e.textContent?.slice(0,30)))
  assert.deepEqual(horizontal,[])
}
const protocolCalls=async page=>(await(await page.request.get(base+'/__mcp-fixture')).json()).calls
try{
  for(const theme of ['light','dark'])for(const[width,height]of[[1440,900],[1024,768],[768,1024],[390,844],[320,640],[844,390]]){
    const{context,page}=await fixture({theme,width,height})
    assert.equal(await page.locator('html').evaluate(e=>e.classList.contains('dark')),theme==='dark')
    assert.equal(await page.locator('.advanced-diagnostics').evaluate(e=>e.open),false)
    await page.screenshot({path:join(output,`${theme}-${width}x${height}-page.png`)})
    await open(page);await checkBounds(page)
    const actualSurface=await page.locator('.mcp-connect-dialog').evaluate(e=>getComputedStyle(e).backgroundColor)
    assert.equal(actualSurface,theme==='dark'?'rgb(28, 28, 28)':'rgb(255, 255, 255)')
    assert.equal(await page.locator('html').evaluate(e=>getComputedStyle(e).filter),'none')
    assert.equal(await page.locator('.cursor-logo').evaluate(e=>getComputedStyle(e).filter),theme==='dark'?'invert(1)':'none')
    assert.equal(await page.getByRole('button',{name:'选择 Codex',exact:true}).evaluate(e=>e===document.activeElement),true)
    assert.equal(await page.locator('.client-picker img').evaluateAll(images=>images.every(i=>i.complete&&i.naturalWidth>0)),true)
    await page.screenshot({path:join(output,`${theme}-${width}x${height}-picker.png`)})
    for(const name of ['Codex','Claude Code','Cursor']){
      await choose(page,name);await checkBounds(page)
      const selector=name==='Codex'?'复制接入命令':name==='Claude Code'?'复制 Claude Code 配置':'在 Cursor 中添加'
      const action=name==='Cursor'?page.getByRole('link',{name:selector,exact:true}):page.getByRole('button',{name:selector,exact:true})
      await action.scrollIntoViewIfNeeded();const r=await action.boundingBox();assert.ok(r.y>=0&&r.y+r.height<=height+.5,JSON.stringify({name,width,height,r}))
      await page.screenshot({path:join(output,`${theme}-${width}x${height}-${name.replaceAll(' ','-')}.png`)})
    }
    await page.keyboard.press('Escape');await page.getByRole('dialog').waitFor({state:'detached'})
    assert.equal(await page.getByRole('button',{name:'接入 AI 助手',exact:true}).evaluate(e=>e===document.activeElement),true)
    assert.equal(await page.evaluate(()=>document.getElementById('app').inert),false)
    assert.equal((await protocolCalls(page)).length,0)
    records.push({case:'layout-picker-and-three-clients',theme,width,height,status:'PASS'});await context.close()
  }
  {
    const{context,page}=await fixture();await open(page)
    const close=page.getByRole('button',{name:'关闭接入引导',exact:true});await close.focus();await page.keyboard.press('Shift+Tab')
    assert.equal(await page.locator('.dialog-footer button').evaluate(e=>e===document.activeElement),true)
    await page.keyboard.press('Tab');assert.equal(await close.evaluate(e=>e===document.activeElement),true)
    await page.getByRole('button',{name:'选择 Codex',exact:true}).focus();await page.keyboard.press('Space')
    await page.getByRole('button',{name:'复制接入命令',exact:true}).click()
    assert.match(await page.locator('.handoff-feedback').innerText(),/配置已准备，待客户端/)
    assert.match(await page.evaluate(()=>window.copied),/--bearer-token-env-var RAG_MCP_ACCESS_TOKEN/)
    assert.doesNotMatch(await page.evaluate(()=>window.copied),/synthetic-access/)
    await choose(page,'Claude Code');assert.equal(await page.locator('.handoff-feedback').innerText(),'')
    await page.getByRole('button',{name:'复制 Claude Code 配置',exact:true}).click()
    assert.equal(JSON.parse(await page.evaluate(()=>window.copied)).mcpServers['enterprise-rag'].headers.Authorization,'Bearer ${RAG_MCP_ACCESS_TOKEN}')
    await choose(page,'Cursor');const link=page.getByRole('link',{name:'在 Cursor 中添加',exact:true}),url=new URL(await link.getAttribute('href'))
    assert.equal(JSON.parse(Buffer.from(url.searchParams.get('config'),'base64').toString()).headers.Authorization,'Bearer ${env:RAG_MCP_ACCESS_TOKEN}')
    await link.click();assert.equal(await page.evaluate(()=>window.cursorIntents.length),1);assert.match(await page.locator('.handoff-feedback').innerText(),/仍待验证/)
    assert.equal((await protocolCalls(page)).length,0)
    records.push({case:'keyboard-trap-three-payloads-and-intercepted-cursor-handoff',status:'PASS'});await context.close()
  }
  {
    const{context,page}=await fixture();await open(page);await choose(page,'Codex')
    await page.evaluate(()=>{window.copyFault=true});await page.getByRole('button',{name:'复制接入命令',exact:true}).click()
    const manual=page.getByRole('textbox',{name:'手动复制接入内容',exact:true});await manual.waitFor();assert.equal(await manual.evaluate(e=>e===document.activeElement),true)
    await choose(page,'Cursor');assert.equal(await manual.count(),0)
    await page.getByLabel('MCP 服务地址',{exact:true}).fill('https://evil.test/mcp?token=x')
    assert.equal(await page.getByLabel('MCP 服务地址',{exact:true}).getAttribute('aria-invalid'),'true');assert.equal(await page.getByRole('link',{name:'在 Cursor 中添加',exact:true}).count(),0)
    await page.getByLabel('MCP 服务地址',{exact:true}).fill('https://kb.example.test/rag/mcp')
    const url=new URL(await page.getByRole('link',{name:'在 Cursor 中添加',exact:true}).getAttribute('href'))
    assert.equal(JSON.parse(Buffer.from(url.searchParams.get('config'),'base64').toString()).url,'https://kb.example.test/rag/mcp')
    await page.keyboard.press('Escape');await open(page);assert.equal(await page.getByLabel('MCP 服务地址',{exact:true}).count(),0)
    await choose(page,'Codex');assert.equal(await page.getByLabel('MCP 服务地址',{exact:true}).inputValue(),'http://127.0.0.1:8080/mcp')
    records.push({case:'copy-failure-invalid-address-config-only-and-reopen-reset',status:'PASS'});await context.close()
  }
  {
    const{context,page}=await fixture();await open(page);await choose(page,'Codex');await page.evaluate(()=>{window.copyHold=true})
    await page.getByRole('button',{name:'复制接入命令',exact:true}).click();await choose(page,'Claude Code');await page.evaluate(()=>window.releaseCopy())
    assert.equal(await page.locator('.handoff-feedback').innerText(),'')
    await page.evaluate(async()=>{const{useAuthStore}=await import('/src/stores/auth.ts');useAuthStore().setAuthTokens('rotated-fixture','refresh',true)})
    await page.getByRole('dialog').waitFor({state:'detached'});assert.equal(await page.evaluate(()=>document.getElementById('app').inert),false)
    await open(page);await choose(page,'Cursor');await page.evaluate(async()=>{const{useAuthStore}=await import('/src/stores/auth.ts');useAuthStore().clearAuth()})
    await page.getByRole('dialog').waitFor({state:'detached'});assert.equal(await page.evaluate(()=>document.getElementById('app').inert),false)
    records.push({case:'late-copy-suppressed-token-rotation-and-logout-cleanup',status:'PASS'});await context.close()
  }
  for(const mode of ['disabled','legacy']){
    const{context,page}=await fixture({mode});await open(page);await choose(page,'Codex');await page.locator('.service-note').waitFor()
    assert.match(await page.locator('.service-note').innerText(),mode==='disabled'?/未启用/:/暂未取得/)
    assert.equal((await protocolCalls(page)).length,0);records.push({case:'honest-'+mode,status:'PASS'});await context.close()
  }
  {
    const{context,page}=await fixture();await page.locator('.advanced-diagnostics > summary').click();await page.getByRole('button',{name:'连接检查',exact:true}).click()
    await page.getByRole('button',{name:'检查连接',exact:true}).click();await page.getByText('本浏览器路径本次协议检查通过。',{exact:true}).waitFor()
    assert.equal((await protocolCalls(page)).length,5);records.push({case:'advanced-diagnostics-keeps-original-five-request-path',status:'PASS'});await context.close()
  }
  assert.deepEqual(errors,[]);assert.deepEqual(outbound,[])
  await writeFile(join(output,'evidence.json'),JSON.stringify({records,errors,outbound,realProviderCalls:0,realClientInstallations:0},null,2))
  console.log(JSON.stringify({checks:records.length,screenshots:60,errors:errors.length,outbound:outbound.length}))
} finally {await browser.close()}
