// Shared interactions for the real Element Plus settings controls.
export async function openSetting(locator) {
  await locator.scrollIntoViewIfNeeded()
  const rect = await locator.evaluate(element => element.closest('.settings-select').getBoundingClientRect().toJSON())
  await locator.page().mouse.click(rect.x + rect.width / 2, rect.y + rect.height / 2)
  const menu = locator.page().locator('.settings-select-popper.el-popper:visible')
  await menu.waitFor({ state: 'visible' })
  await menu.evaluate(async element => { await Promise.all(element.getAnimations({ subtree: true }).map(animation => animation.finished.catch(() => {}))) })
  return menu
}
export async function selectSetting(locator, value) {
  const menu = await openSetting(locator)
  await menu.locator(`[role="option"][data-value="${value}"]`).click()
  await menu.waitFor({ state: 'detached' })
}
export async function settingValue(locator) {
  return locator.evaluate(element => element.closest('.settings-select').dataset.value)
}

