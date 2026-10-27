import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'
import { usePreferencesStore } from './stores/preferences'
import './styles/global.css'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)
useThemeStore(pinia).initialize()
usePreferencesStore(pinia).initialize()
app.use(router)
app.use(ElementPlus)

app.onUnmount(() => {
  usePreferencesStore(pinia).dispose()
  useThemeStore(pinia).dispose()
})

app.mount('#app')
