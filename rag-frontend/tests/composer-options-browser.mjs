// Real pointer regression against the local synthetic preview. No business calls.
// node tests/composer-options-browser.mjs <playwright/index.js> <output-dir> [chrome.exe]
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve, join } from 'node:path'

const { chromium } = createRequire(import.meta.url)(resolve(process.argv[2]))
const output = resolve(process.argv[3])
await mkdir(output, { recursive: true })
const browser = await chromium.launch({ headless: true, ...(process.argv[4]
  ? { executablePath: resolve(process.argv[4]) } : { channel: 'chrome' }) })
const base = 'http://127.0.0.1:5188'
const records = [], outbound = [], errors = [], requests = [], demoComparisons = []

async function insideViewport(locator, width, height) {
  await locator.page().waitForFunction(({ element, width, height }) => {
    const box = element.getBoundingClientRect()
    return box.x >= 0 && box.y >= 0 && box.right <= width && box.bottom <= height
  }, { element: await locator.elementHandle(), width, height }, { timeout: 2000 })
  const box = await locator.boundingBox()
  assert.ok(box && box.x >= 0 && box.y >= 0 && box.x + box.width <= width && box.y + box.height <= height,
    JSON.stringify({ box, viewport: [width, height], placement: await locator.getAttribute('data-placement') }))
}

// Click what a person sees, rather than calling radio.check(), which bypasses
// the old label -> focusout -> premature unmount bug.
async function clickAt(locator, edge = false) {
  const box = await locator.boundingBox()
  assert.ok(box)
  await locator.page().mouse.click(edge ? box.x + box.width - 5 : box.x + box.width / 2,
    box.y + box.height / 2)
}

async function selected(page, value) {
  await page.waitForFunction(value => document.querySelector('input[name="response-mode"]:checked')?.value === value, value, { timeout: 2000 })
  assert.equal(await page.getByRole('dialog', { name: '高级选项' }).count(), 1)
  const row = page.locator('.response-option.selected')
  assert.equal(await row.locator('input').inputValue(), value)
  assert.notEqual(await row.evaluate(e => getComputedStyle(e).boxShadow), 'none', 'selection must stay outlined without focus')
  await row.evaluate(async e => { await Promise.all(e.getAnimations().map(animation => animation.finished.catch(() => {}))) })
}

try {
  for (const theme of ['dark', 'light']) for (const [width, height] of [[1536, 720], [1440, 900], [768, 1024], [390, 844], [320, 568], [844, 390]]) {
    const context = await browser.newContext({ viewport: { width, height } })
    await context.route('**/*', route => {
      if (new URL(route.request().url()).origin === base) return route.continue()
      outbound.push(new URL(route.request().url()).origin)
      return route.abort()
    })
    await context.addInitScript(theme => {
      localStorage.setItem('rag_accessToken', 'fixture-access')
      localStorage.setItem('theme', theme)
    }, theme)
    await context.route('**/api/knowledge-bases', route => route.fulfill({ json: {
      code: 200, data: Array.from({ length: 24 }, (_, i) => ({ id: 901 + i,
        name: i === 23 ? '合成长名称知识库用于验证完整名称提示与单行布局'.repeat(5) : `合成知识库 ${i + 1}`,
        documentCount: 2, isPublic: false,
      })),
    } }))
    const page = await context.newPage()
    page.on('pageerror', error => errors.push(error.message))
    page.on('request', request => { if (request.url().includes('/api/qa/ask')) requests.push(new URL(request.url()).pathname) })
    await page.goto(base + '/chat?fixture=chat-stream')
    const trigger = page.getByRole('button', { name: '高级选项', exact: true })
    const composerBox = await page.locator('.input-container').boundingBox()
    assert.equal(composerBox.height, 56, 'single-line input must match the Demo height')
    if (width >= 1440) assert.equal(composerBox.width, 820)
    assert.equal((await trigger.boundingBox()).width, 34)
    if (width === 1536) {
      const demo = await context.newPage()
      await demo.goto(base + '/__demo/index.html')
      await demo.locator('.composer-box').waitFor()
      await demo.evaluate(theme => { document.documentElement.dataset.theme = theme }, theme)
      await demo.locator('.composer-box').evaluate(async e => { await Promise.all(e.getAnimations().map(animation => animation.finished.catch(() => {}))) })
      const readMetrics = e => {
        const s = getComputedStyle(e)
        return { width: e.offsetWidth, height: e.offsetHeight, radius: s.borderRadius,
          padding: s.padding, background: s.backgroundColor, border: s.borderColor, shadow: s.boxShadow }
      }
      const formal = await page.locator('.input-container').evaluate(readMetrics)
      const reference = await demo.locator('.composer-box').evaluate(readMetrics)
      assert.deepEqual(formal, reference)
      demoComparisons.push({ theme, formal, reference })
      await demo.close()
    }
    await page.getByRole('button', { name: '选择知识库', exact: true }).click()
    const kbBox = await page.locator('.kb-dropdown').boundingBox()
    assert.ok(kbBox.y >= 0 && kbBox.y + kbBox.height <= height, 'knowledge-base menu must stay inside the viewport')
    assert.equal(await page.locator('.kb-menu-item').count(), 24)
    await insideViewport(page.locator('.kb-dropdown'), width, height)
    await page.screenshot({ path: join(output, `composer-kb-${theme}-${width}-${height}.png`) })
    const longKb = page.locator('.kb-menu-item').last()
    await longKb.scrollIntoViewIfNeeded()
    await longKb.click()
    assert.equal(await page.locator('.kb-dropdown').count(), 0)
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false)
    assert.ok((await page.locator('.kb-scope-chip span').getAttribute('title')).length > 60)
    await page.getByRole('textbox').fill('点击模式时保留的合成草稿')
    const textarea = await page.getByRole('textbox').elementHandle()
    await trigger.click()
    // This failed before the fix: clicking the visible label closed the panel
    // before the native radio could receive its activation click.
    await clickAt(page.getByText('流式问答', { exact: true }))
    assert.equal(await page.getByRole('dialog', { name: '高级选项' }).count(), 1, 'visible label click must not dismiss the panel')
    await selected(page, 'stream')
    await insideViewport(page.getByRole('dialog', { name: '高级选项' }), width, height)
    await clickAt(page.getByText('完整返回回答与来源', { exact: true }))
    await selected(page, 'sync')
    await clickAt(page.locator('.response-option').filter({ hasText: '流式问答' }), true)
    await selected(page, 'stream')
    await page.getByRole('radio', { name: '流式问答 逐步输出，完整回答后展示来源', exact: true }).press('ArrowUp')
    await selected(page, 'sync')
    await clickAt(page.getByText('流式问答', { exact: true }))
    await selected(page, 'stream')
    assert.equal(await textarea.evaluate(e => e.isConnected), true)
    assert.equal(await page.getByRole('textbox').inputValue(), '点击模式时保留的合成草稿')
    await page.screenshot({ path: join(output, `composer-pointer-${theme}-${width}.png`) })
    await page.getByRole('radio', { name: '流式问答 逐步输出，完整回答后展示来源', exact: true }).press('Escape')
    assert.equal(await trigger.evaluate(e => e === document.activeElement), true)
    await trigger.click()
    await selected(page, 'stream')
    await page.getByRole('textbox').click()
    assert.equal(await page.getByRole('dialog', { name: '高级选项' }).count(), 0)
    assert.equal(requests.length, 0, 'mode selection must not send requests')
    await page.getByRole('textbox').fill('多行草稿保留与菜单限高检查\n'.repeat(30))
    assert.ok((await page.getByRole('textbox').boundingBox()).height <= 200)
    await page.getByRole('button', { name: '选择知识库', exact: true }).click()
    await insideViewport(page.locator('.kb-dropdown'), width, height)
    const shorterHeight = Math.max(320, height - 160)
    await page.setViewportSize({ width, height: shorterHeight })
    await insideViewport(page.locator('.kb-dropdown'), width, shorterHeight)
    await page.locator('.kb-menu-item').first().press('Escape')
    assert.equal(await page.getByRole('button', { name: '选择知识库', exact: true }).evaluate(e => e === document.activeElement), true)
    await page.getByRole('textbox').fill('点击模式时保留的合成草稿')
    await page.setViewportSize({ width, height })
    records.push({ theme, width, height, label: true, hint: true, edge: true, keyboard: true, persistentSelection: true, draft: true, longList: 24, resize: true, multiline: true })
    if (theme === 'light' && width === 844) {
      await page.getByRole('button', { name: '选择知识库', exact: true }).click()
      await page.getByRole('button', { name: '合成知识库 1', exact: true }).click()
      await page.getByRole('button', { name: '发送问题', exact: true }).click()
      await page.waitForFunction(() => !document.querySelector('.options-btn').disabled)
      assert.deepEqual(requests, ['/api/qa/ask/stream'])
      assert.match(await page.locator('.stream-state').innerText(), /完整回答/)
      await trigger.click()
      await clickAt(page.getByText('同步问答', { exact: true }))
      await page.getByRole('radio', { name: '同步问答 完整返回回答与来源', exact: true }).press('Escape')
      await page.getByRole('textbox').fill('合成同步请求')
      await page.getByRole('button', { name: '发送问题', exact: true }).click()
      await page.waitForFunction(() => !document.querySelector('.options-btn').disabled)
      assert.deepEqual(requests, ['/api/qa/ask/stream', '/api/qa/ask'])
      await page.goto(base + '/chat?fixture=chat-stop')
      await page.setViewportSize({ width: 320, height: 568 })
      await page.getByRole('button', { name: '选择知识库', exact: true }).click()
      await page.getByRole('button', { name: '合成知识库 1', exact: true }).click()
      await trigger.click()
      await clickAt(page.getByText('流式问答', { exact: true }))
      await page.getByRole('radio', { name: '流式问答 逐步输出，完整回答后展示来源', exact: true }).press('Escape')
      await page.getByRole('textbox').fill('合成停止接收检查')
      await page.getByRole('button', { name: '发送问题', exact: true }).click()
      const stop = page.getByRole('button', { name: '停止接收', exact: true })
      await stop.waitFor()
      assert.equal((await stop.boundingBox()).width, 34)
      await insideViewport(stop, 320, 568)
      await stop.click()
      await page.waitForFunction(() => !document.querySelector('.options-btn').disabled)
      assert.match(await page.locator('.stream-state').innerText(), /已停止接收/)
      assert.deepEqual(requests, ['/api/qa/ask/stream', '/api/qa/ask', '/api/qa/ask/stream'])
    }
    await context.close()
  }
  assert.deepEqual(outbound, [])
  assert.deepEqual(errors, [])
  await writeFile(join(output, 'composer-pointer-qa.json'), JSON.stringify({ records, demoComparisons, outbound, errors, requests }, null, 2))
  console.log(JSON.stringify({ status: 'PASS', cases: records.length, realProviderCalls: 0, outbound, errors, requests }))
} finally {
  await browser.close()
}
