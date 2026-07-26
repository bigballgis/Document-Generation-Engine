import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import { createRouter, createMemoryHistory } from 'vue-router'
import App from '@/App.vue'
import en from '@/i18n/locales/en'

let appLocale = 'zh-CN'

vi.mock('@/stores/app', () => ({
  useAppStore: () => ({
    get locale() {
      return appLocale
    },
  }),
}))

vi.mock('@/components/layout/ManagementShell.vue', () => ({
  default: { template: '<div class="shell-stub"><slot /></div>' },
}))

describe('App ElConfigProvider (FOS-W5-3)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    appLocale = 'zh-CN'
  })

  it('binds Element Plus zh-cn locale when app locale is zh-CN', async () => {
    const i18n = createI18n({ legacy: false, locale: 'zh-CN', messages: { en } })
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div />' }, meta: { public: true } }],
    })
    await router.push('/')
    const wrapper = mount(App, {
      global: { plugins: [i18n, router, createPinia()] },
    })
    const provider = wrapper.findComponent({ name: 'ElConfigProvider' })
    expect(provider.exists()).toBe(true)
    expect(String(provider.props('locale')?.name ?? '')).toMatch(/zh/i)
  })

  it('binds Element Plus en locale when app locale is en', async () => {
    appLocale = 'en'
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div />' }, meta: { public: true } }],
    })
    await router.push('/')
    const wrapper = mount(App, {
      global: { plugins: [i18n, router, createPinia()] },
    })
    const provider = wrapper.findComponent({ name: 'ElConfigProvider' })
    expect(String(provider.props('locale')?.name ?? '')).toMatch(/en/i)
  })
})
