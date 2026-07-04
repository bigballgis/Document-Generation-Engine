import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/theme-chalk/base.css'

import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import { useAppStore } from './stores/app'
import './styles/global.scss'

async function bootstrap() {
  const app = createApp(App)
  const pinia = createPinia()
  app.use(pinia).use(router).use(i18n)

  const appStore = useAppStore(pinia)
  await appStore.initializePreferences()

  app.mount('#app')
}

void bootstrap()
