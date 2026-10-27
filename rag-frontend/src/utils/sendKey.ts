import type { Preferences } from '../stores/preferences'

interface SendKeyEvent {
  key: string; shiftKey: boolean; ctrlKey?: boolean; metaKey?: boolean; altKey?: boolean
  repeat?: boolean; isComposing?: boolean; keyCode?: number
}
export function isSendKey(event: SendKeyEvent, mode: Preferences['sendKey'], compositionBoundary = false): boolean {
  if (event.key !== 'Enter' || event.shiftKey || event.altKey || event.repeat
    || event.isComposing || event.keyCode === 229 || compositionBoundary) return false
  return mode === 'modifier-enter' ? !!(event.ctrlKey || event.metaKey) : !(event.ctrlKey || event.metaKey)
}
