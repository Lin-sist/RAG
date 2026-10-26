// Local, synthetic browser QA. Supply an existing Playwright module and an
// output directory; no packages, browser binaries or business services are fetched.
import assert from 'node:assert/strict'
import { mkdir, writeFile } from 'node:fs/promises'
import { pathToFileURL } from 'node:url'
import { resolve, join } from 'node:path'

const { chromium } = await import(pathToFileURL(resolve(process.argv[2])).href)
const output = resolve(process.argv[3])
await mkdir(output, { recursive: true })
const browser = await chromium.launch({ headless: true, channel: 'chrome' })
const records = [], checks = [], errors = [], outbound = []
const labels = ['品牌标志', '知识文档', '检索关联', '引用回答']
const titles = ['让每个回答，都有出处', '把分散资料，变成知识', '沿着问题，找到线索', '答案有依据，来源可核对']
const base = 'http://127.0.0.1:5188'
async function context(options = {}) {
  const ctx = await browser.newContext(options)
  await ctx.route('**/*', route => {
    const url = new URL(route.request().url())
    if (url.origin === base) return route.continue()
    outbound.push(url.origin); return route.abort()
  })
  return ctx
}
async function ready(page) {
  await page.goto(base + '/login')
  await page.locator('canvas[data-renderer="webgl"]').waitFor()
}
async function settle(page, target) {
  await page.waitForFunction(target => Math.abs(Number(document.querySelector('canvas').dataset.progress) - target) < .0002, target)
}
async function select(page, index) {
  await page.getByRole('button', { name: labels[index], exact: true }).click()
  await settle(page, index)
  assert.equal(await page.locator('h1').innerText(), titles[index])
}
async function capture(page, name) {
  const state = await page.evaluate(() => {
    const canvas = document.querySelector('canvas'), story = document.querySelector('.auth-story')
    const box = element => { const b = element.getBoundingClientRect(); return { x: b.x, y: b.y, width: b.width, height: b.height } }
    return { viewport: [innerWidth, innerHeight], overflow: document.documentElement.scrollWidth > innerWidth,
      theme: document.documentElement.classList.contains('dark') ? 'dark' : 'light',
      canvas: { ...canvas.dataset }, story: { ...story.dataset }, title: document.querySelector('h1').textContent,
      form: box(document.querySelector('.login-form')), stage: box(canvas),
      buttons: [...document.querySelectorAll('.particle-controls button')].map(b => ({ label: b.textContent.trim(), height: b.getBoundingClientRect().height })) }
  })
  assert.equal(state.overflow, false, name)
  assert.equal(Number(state.story.progress), state.canvas.renderer === 'webgl' ? Number(state.canvas.progress) : 0, name)
  assert.ok(state.buttons.every(button => button.height >= 44), name)
  const file = name + '.png'
  await page.screenshot({ path: join(output, file), fullPage: true })
  records.push({ name, file, ...state })
}
async function fraction(page, scene, phase) {
  await select(page, scene)
  // Controlled browser DOM events give reproducible fractional snapshots.
  await page.evaluate(phase => {
    let amount = phase
    while (amount > .000001) {
      const step = Math.min(.18, amount)
      document.querySelector('canvas').dispatchEvent(new WheelEvent('wheel', { deltaY: step * 1100, cancelable: true }))
      amount -= step
    }
  }, phase)
  await settle(page, scene + phase)
}
try {
  for (const colorScheme of ['dark', 'light']) {
    const ctx = await context({ viewport: { width: 1440, height: 900 }, colorScheme })
    const page = await ctx.newPage(); page.on('pageerror', error => errors.push(error.message))
    await ready(page)
    const logo = await page.evaluate(async () => {
      const { makeModels } = await import('/src/utils/loginParticleGeometry.ts')
      const svg = new DOMParser().parseFromString(await (await fetch('/src/assets/login-logo.svg')).text(), 'image/svg+xml')
      const paths = [...svg.querySelectorAll('path')].map(path => new Path2D(path.getAttribute('d')))
      const context = document.createElement('canvas').getContext('2d'), points = makeModels(4800)[0]
      let outside = 0, accent = 0
      for (let i = 0; i < points.length; i += 4) {
        const x = points[i] * 175 + 303, y = 256.5 - points[i + 1] * 175
        if (!paths.some(path => context.isPointInPath(path, x, y) || context.isPointInStroke(path, x, y))) outside++
        if (points[i + 3] > .9) accent++
      }
      return { outside, accent, count: points.length / 4 }
    })
    assert.ok(logo.outside / logo.count < .005, JSON.stringify(logo)); assert.ok(logo.accent > 200)
    checks.push({ name: colorScheme + '-logo-matches-existing-svg', ...logo })
    await page.getByLabel('用户名', { exact: true }).fill('particle-fixture')
    await page.getByLabel('密码', { exact: true }).fill('fixture-only')
    const input = await page.locator('#login-username').elementHandle()
    const before = await page.locator('.login-form').boundingBox()
    for (let index = 0; index < labels.length; index++) {
      await select(page, index)
      const after = await page.locator('.login-form').boundingBox()
      assert.ok(Math.abs(before.y - after.y) < .1)
      assert.equal(await input.evaluate(node => node === document.querySelector('#login-username')), true)
      assert.equal(await page.locator('#login-username').inputValue(), 'particle-fixture')
      assert.equal(await page.locator('#login-password').inputValue(), 'fixture-only')
      await capture(page, `${colorScheme}-1440-scene-${index}`)
    }
    for (let scene = 0; scene < 3; scene++) for (const phase of [.25, .5, .75]) {
      await fraction(page, scene, phase)
      assert.ok(Math.abs((await page.locator('.login-form').boundingBox()).y - before.y) < .1)
      await capture(page, `${colorScheme}-1440-transition-${scene}-${phase}`)
    }
    await fraction(page, 1, .5)
    await page.locator('#login-username').focus()
    const box = await page.locator('canvas').boundingBox()
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
    await page.mouse.wheel(0, -110); await settle(page, 1.4)
    assert.equal(await page.locator('#login-username').evaluate(node => node === document.activeElement), true)
    checks.push({ name: colorScheme + '-native-wheel-reverse-preserves-input-focus', progress: await page.locator('canvas').getAttribute('data-progress') })
    await page.mouse.move(box.x + box.width * .55, box.y + box.height * .5, { steps: 8 })
    await page.waitForFunction(() => Number(document.querySelector('canvas').dataset.offset) > .001)
    const peak = await page.locator('canvas').getAttribute('data-offset')
    await page.mouse.move(10, 10)
    await page.waitForFunction(() => Number(document.querySelector('canvas').dataset.offset) < .0001)
    checks.push({ name: colorScheme + '-pointer-recovers', observedOffset: peak })
    await page.getByRole('button', { name: '暂停粒子动画', exact: true }).click()
    await select(page, 3)
    const frame = await page.locator('canvas').getAttribute('data-frame')
    await page.waitForTimeout(120)
    assert.equal(await page.locator('canvas').getAttribute('data-frame'), frame)
    await capture(page, colorScheme + '-paused-answer')
    await page.emulateMedia({ reducedMotion: 'reduce' }); await select(page, 1)
    const reducedFrame = await page.locator('canvas').getAttribute('data-frame')
    await page.waitForTimeout(120)
    assert.equal(await page.locator('canvas').getAttribute('data-frame'), reducedFrame)
    await capture(page, colorScheme + '-reduced-document')
    await page.evaluate(() => {
      window.__particleContext = document.querySelector('canvas').getContext('webgl').getExtension('WEBGL_lose_context')
      window.__particleContext.loseContext()
    })
    await page.waitForFunction(() => document.querySelector('.particle-scene').dataset.renderer === 'static')
    assert.equal(await page.locator('h1').innerText(), titles[0])
    assert.equal(await page.getByRole('button', { name: labels[1], exact: true }).isDisabled(), true)
    await capture(page, colorScheme + '-fallback-brand')
    await page.evaluate(() => window.__particleContext.restoreContext())
    await page.waitForFunction(() => document.querySelector('.particle-scene').dataset.renderer === 'webgl')
    assert.equal(await page.locator('h1').innerText(), titles[1])
    checks.push({ name: colorScheme + '-pause-reduce-loss-restore-copy-synchronized', inputNodeRetained: await input.evaluate(node => node === document.querySelector('#login-username')) })
    await ctx.close()
    for (const viewport of [{ width: 1024, height: 768 }, { width: 390, height: 844 }]) {
      const ctx = await context({ viewport, colorScheme, reducedMotion: 'reduce' }), page = await ctx.newPage()
      await ready(page)
      for (let i = 0; i < labels.length; i++) {
        await page.getByRole('button', { name: labels[i], exact: true }).press('Space')
        await settle(page, i); await capture(page, `${colorScheme}-${viewport.width}-scene-${i}`)
      }
      await ctx.close()
    }
  }
  // Existing closed fixtures verify that the unchanged form still submits via Enter.
  for (const scenario of ['auth-motion-error', 'auth-motion-loading']) {
    const ctx = await context({ viewport: { width: 1440, height: 900 } }), page = await ctx.newPage()
    await page.goto(`${base}/login?fixture=${scenario}`)
    await page.getByLabel('用户名', { exact: true }).fill('particle-fixture')
    await page.getByLabel('密码', { exact: true }).fill('fixture-only')
    await page.getByLabel('密码', { exact: true }).press('Enter')
    if (scenario.endsWith('error')) {
      await page.getByText('合成登录失败，未连接真实服务', { exact: true }).waitFor()
      checks.push({ name: scenario, passed: true })
    } else {
      await page.getByRole('button', { name: '登录中...', exact: true }).waitFor()
      await page.waitForURL('**/chat'); assert.equal(await page.locator('canvas').count(), 0)
      checks.push({ name: scenario, passed: true, canvasAfterNavigation: 0 })
    }
    await ctx.close()
  }
  assert.deepEqual(errors, []); assert.deepEqual(outbound, [])
  console.log(`BROWSER_PASS: ${records.length} screenshots; ${checks.length} checks; no page errors or external traffic`)
} finally {
  await writeFile(join(output, 'browser-records.json'), JSON.stringify({ records, checks, errors, outbound, browserVersion: browser.version(), note: 'Headless system Chrome; software WebGL and emulated preferences are not physical GPU/mobile evidence.' }, null, 2))
  await browser.close()
}
