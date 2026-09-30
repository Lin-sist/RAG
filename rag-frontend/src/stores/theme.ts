import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export type ThemePreference = 'system' | 'light' | 'dark'
export interface ThemeEnvironment {
  root: HTMLElement
  media: MediaQueryList
  read: () => string | null
  write: (value: string) => void
}

export const useThemeStore = defineStore('theme', () => {
  const preference = ref<ThemePreference>('system')
  const systemDark = ref(false)
  const persistenceAvailable = ref(true)
  const effectiveTheme = computed(() => preference.value === 'system'
    ? systemDark.value ? 'dark' : 'light' : preference.value)
  let environment: ThemeEnvironment | undefined

  function apply() {
    if (!environment) return
    const dark = effectiveTheme.value === 'dark'
    environment.root.classList.toggle('dark', dark)
    environment.root.classList.toggle('light', !dark)
    environment.root.style.colorScheme = effectiveTheme.value
  }

  function onSystemChange(event: MediaQueryListEvent) {
    systemDark.value = event.matches
    apply()
  }

  function initialize(env?: ThemeEnvironment) {
    if (environment) return
    environment = env ?? {
      root: document.documentElement,
      media: window.matchMedia('(prefers-color-scheme: dark)'),
      read: () => window.localStorage.getItem('theme'),
      write: value => window.localStorage.setItem('theme', value),
    }
    try {
      const saved = environment.read()
      preference.value = saved === 'dark' || saved === 'light' ? saved : 'system'
    } catch { persistenceAvailable.value = false }
    systemDark.value = environment.media.matches
    environment.media.addEventListener('change', onSystemChange)
    apply()
  }

  function setPreference(value: ThemePreference) {
    preference.value = value
    apply()
    if (!environment) return
    try {
      environment.write(value)
      persistenceAvailable.value = true
    } catch { persistenceAvailable.value = false }
  }

  function dispose() {
    environment?.media.removeEventListener('change', onSystemChange)
    environment = undefined
  }

  return { preference, effectiveTheme, persistenceAvailable, initialize, setPreference, dispose }
})
