import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import { applyBrandTheme } from './theme/tokens'
import './styles/global.scss'

applyBrandTheme('REDBC')

createApp(App).use(createPinia()).use(router).use(i18n).use(ElementPlus).mount('#app')
