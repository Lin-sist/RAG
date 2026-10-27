import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const PREFERENCES_KEY = 'rag.ui.preferences.v1'
export interface Preferences {
  answerTextSize: 'small' | 'standard' | 'large'
  motion: 'system' | 'reduce'
  sendKey: 'enter' | 'modifier-enter'
  sources: 'collapsed' | 'expanded'
}
export const DEFAULT_PREFERENCES: Readonly<Preferences> = Object.freeze({
  answerTextSize: 'standard', motion: 'system', sendKey: 'enter', sources: 'collapsed',
})
const choices = {
  answerTextSize: ['small', 'standard', 'large'], motion: ['system', 'reduce'],
  sendKey: ['enter', 'modifier-enter'], sources: ['collapsed', 'expanded'],
} as const

export function parsePreferences(raw: string | null): Preferences {
  const defaults = { ...DEFAULT_PREFERENCES }
  if (!raw) return defaults
  try {
    const value = JSON.parse(raw)
    if (!value || typeof value !== 'object' || value.version !== 1) return defaults
    for (const key of Object.keys(choices) as (keyof Preferences)[]) {
      if ((choices[key] as readonly unknown[]).includes(value[key])) Object.assign(defaults, { [key]: value[key] })
    }
  } catch { /* Invalid local preferences fall back without revealing their contents. */ }
  return defaults
}

export interface PreferencesEnvironment {
  root: HTMLElement
  media: MediaQueryList
  read: () => string | null
  write: (value: string) => void
}

export const usePreferencesStore = defineStore('preferences', () => {
  const values = ref<Preferences>({ ...DEFAULT_PREFERENCES })
  const systemReduced = ref(false)
  const persistenceAvailable = ref(true)
  const reducedMotion = computed(() => systemReduced.value || values.value.motion === 'reduce')
  let environment: PreferencesEnvironment | undefined
  function apply() {
    if (!environment) return
    const sizes = { small: '.875rem', standard: '.9375rem', large: '1.0625rem' }
    environment.root.style.setProperty('--rag-answer-font-size', sizes[values.value.answerTextSize])
    environment.root.dataset.motion = reducedMotion.value ? 'reduce' : 'no-preference'
  }
  function onSystemChange(event: MediaQueryListEvent) { systemReduced.value = event.matches; apply() }
  function initialize(env?: PreferencesEnvironment) {
    if (environment) return
    environment = env ?? {
      root: document.documentElement, media: window.matchMedia('(prefers-reduced-motion: reduce)'),
      read: () => window.localStorage.getItem(PREFERENCES_KEY),
      write: value => window.localStorage.setItem(PREFERENCES_KEY, value),
    }
    try { values.value = parsePreferences(environment.read()) }
    catch { values.value = { ...DEFAULT_PREFERENCES }; persistenceAvailable.value = false }
    systemReduced.value = environment.media.matches
    environment.media.addEventListener('change', onSystemChange)
    apply()
  }
  function persist() {
    if (!environment) return
    try {
      const { answerTextSize, motion, sendKey, sources } = values.value
      environment.write(JSON.stringify({ version: 1, answerTextSize, motion, sendKey, sources }))
      persistenceAvailable.value = true
    } catch { persistenceAvailable.value = false }
  }
  function setPreference<K extends keyof Preferences>(key: K, value: Preferences[K]) {
    if (!(choices[key] as readonly unknown[]).includes(value)) return
    values.value = { ...values.value, [key]: value }; apply(); persist()
  }
  function reset() { values.value = { ...DEFAULT_PREFERENCES }; apply(); persist() }
  function dispose() { environment?.media.removeEventListener('change', onSystemChange); environment = undefined }
  return { values: computed(() => values.value), reducedMotion, persistenceAvailable, initialize, setPreference, reset, dispose }
})
