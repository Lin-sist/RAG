// Modern settings menu verification against isolated fixtures.
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve, join } from 'node:path'
import { openSetting, selectSetting, settingValue } from './settings-browser-helpers.mjs'
const { chromium } = createRequire(import.meta.url)(resolve(process.argv[2]))
const output = resolve(process.argv[3]); await mkdir(output, { recursive: true })
const browser = await chromium.launch({ headless: true, executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe' })
const base = 'http://127.0.0.1:5188', records = [], errors = [], outbound = [], business = []
async function inside(page, menu) {
  const r = await menu.boundingBox(), dialog = await page.getByRole('dialog', { name: '设置', exact: true }).boundingBox()
  assert.ok(r && r.x >= dialog.x - .1 && r.y >= dialog.y - .1 && r.x + r.width <= dialog.x + dialog.width + .1 && r.y + r.height <= dialog.y + dialog.height + .1, JSON.stringify({ r, dialog }))
  assert.equal(await menu.evaluate(e => !!e.closest('.settings-modal') && !e.closest('[inert]')), true)
  assert.equal(await menu.evaluate(e => getComputedStyle(e).borderRadius), '14px')
}
async function category(page, id, label) {
  const mobile = page.locator('#settings-category')
  if (await mobile.isVisible()) await selectSetting(mobile, id)
  else await page.getByRole('button', { name: label, exact: true }).click()
}
try {
  for (const theme of ['dark', 'light']) for (const [width, height] of [[1440, 900], [390, 844], [320, 640], [844, 390]]) {
    const context = await browser.newContext({ viewport: { width, height } })
    await context.route('**/*', route => {
      const url = new URL(route.request().url())
      if (url.origin !== base) { outbound.push(url.origin); return route.abort() }
      if (url.pathname.startsWith('/api/qa/')) business.push(url.pathname)
      return route.continue()
    })
    await context.addInitScript(theme => { localStorage.setItem('rag_accessToken', 'fixture-access'); localStorage.setItem('theme', theme) }, theme)
    const page = await context.newPage(); page.on('pageerror', error => errors.push(error.message))
    await page.goto(base + '/chat?fixture=workspace-ready')
    if (width <= 768) { await page.getByRole('button', { name: '打开侧栏', exact: true }).waitFor(); await page.getByRole('button', { name: '打开侧栏', exact: true }).click() }
    await page.locator('.user-trigger').click(); await page.getByRole('button', { name: '设置', exact: true }).click()
    assert.equal(await page.locator('.settings-modal select').count(), 0)
    const fields = [['appearance','外观','回答字号','large'], ['appearance','外观','减少动态效果','reduce'], ['conversation','问答偏好','发送快捷键','modifier-enter'], ['conversation','问答偏好','来源片段','expanded']]
    for (const [id, label, field, value] of fields) {
      await category(page, id, label)
      const combo = page.getByRole('combobox', { name: field, exact: true }), menu = await openSetting(combo)
      await inside(page, menu)
      assert.equal(await combo.evaluate(element => getComputedStyle(element.closest('.settings-select').querySelector('.el-select__wrapper')).outlineStyle), 'none')
      assert.equal(await menu.locator('[role="option"][aria-selected="true"] .settings-option-check').count(), 1)
      await page.screenshot({ path: join(output, `${theme}-${width}-${height}-${field}.png`) })
      await combo.press('Escape'); await menu.waitFor({ state: 'detached' })
      assert.equal(await page.getByRole('dialog', { name: '设置', exact: true }).count(), 1)
      await page.mouse.move(0, 0); await combo.focus(); await combo.press('ArrowDown'); await page.locator('.settings-select-popper.el-popper:visible').waitFor(); await page.locator('.settings-select-popper.el-popper:visible').evaluate(async element => { await Promise.all(element.getAnimations({subtree:true}).map(animation => animation.finished.catch(() => {}))); await new Promise(requestAnimationFrame) })
      assert.equal(await combo.evaluate(element => getComputedStyle(element.closest('.settings-select').querySelector('.el-select__wrapper')).outlineWidth), '2px')
      await combo.press('End'); await page.waitForFunction(({id,value}) => document.getElementById(document.getElementById(id).getAttribute('aria-activedescendant'))?.dataset.value === value, {id:await combo.getAttribute('id'),value}); await combo.press('Enter')
      await page.locator('.settings-select-popper.el-popper').waitFor({ state: 'detached' })
      assert.equal(await settingValue(combo), value)
      const reopened = await openSetting(combo); await combo.press('Tab'); await reopened.waitFor({ state: 'detached' })
      assert.equal(await page.evaluate(() => !!document.activeElement?.closest('.settings-modal')), true)
      records.push({ theme, width, height, field, pointerKeyboardEscapeTab: 'PASS' })
    }
    if (width <= 768) {
      const menu = await openSetting(page.locator('#settings-category')); await inside(page, menu)
      await page.screenshot({ path: join(output, `${theme}-${width}-${height}-category.png`) })
      await page.locator('#settings-category').press('Escape'); await menu.waitFor({ state: 'detached' })
    }
    const combo = page.getByRole('combobox', { name: '来源片段', exact: true })
    if (width === 1440) {
      const menu = await openSetting(combo)
      await page.setViewportSize({ width: 390, height: 640 })
      const settled = () => {
        const menu = document.querySelector('.settings-select-popper.el-popper'), dialog = document.querySelector('.settings-modal')
        if (!menu || !dialog) return false
        const a = menu.getBoundingClientRect(), b = dialog.getBoundingClientRect()
        return a.x >= b.x - .1 && a.y >= b.y - .1 && a.right <= b.right + .1 && a.bottom <= b.bottom + .1
      }
      await page.waitForFunction(settled); await inside(page, menu)
      await page.locator('.settings-content').evaluate(element => element.scrollBy(0, 40))
      await page.waitForFunction(settled); await inside(page, menu)
      await combo.press('Escape'); await menu.waitFor({ state: 'detached' })
      records.push({ theme, case: 'resize-and-scroll-open-menu', status: 'PASS' })
    }
    await openSetting(combo); await page.getByRole('heading', { name: '问答偏好', exact: true }).click()
    await page.locator('.settings-select-popper.el-popper').waitFor({ state: 'detached' })
    await combo.focus(); await combo.press('Escape')
    assert.equal(await page.getByRole('dialog', { name: '设置', exact: true }).count(), 0)
    assert.equal(await page.locator('.settings-select-popper.el-popper').count(), 0)
    await context.close()
  }
  assert.deepEqual(errors, []); assert.deepEqual(outbound, []); assert.deepEqual(business, [])
  await writeFile(join(output, 'dropdown-evidence.json'), JSON.stringify({ records, errors, outbound, business }, null, 2))
  console.log(JSON.stringify({ checks: records.length, themes: 2, viewports: 4, status: 'PASS' }))
} catch (error) { const activePage = browser.contexts().at(-1)?.pages().at(-1); if (activePage) { console.error(await activePage.getByRole('dialog',{name:'设置',exact:true}).ariaSnapshot()); await activePage.screenshot({path:join(output,'failure-state.png')}) } await writeFile(join(output, 'dropdown-failure.txt'), String(error.stack)); console.error(error); process.exitCode = 1 }
finally { await browser.close() }



