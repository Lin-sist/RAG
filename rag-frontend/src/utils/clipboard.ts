export type ClipboardResult = 'copied' | 'unavailable' | 'failed'

export async function copyText(
  text: string,
  clipboard: Pick<Clipboard, 'writeText'> | undefined = globalThis.navigator?.clipboard,
): Promise<ClipboardResult> {
  if (!clipboard?.writeText) return 'unavailable'
  try {
    await clipboard.writeText(text)
    return 'copied'
  } catch {
    return 'failed'
  }
}

export function clipboardMessage(result: ClipboardResult): string {
  if (result === 'copied') return '已复制'
  if (result === 'unavailable') return '剪贴板不可用，请手动选择并复制文本。'
  return '复制失败，请手动选择并复制文本。'
}
