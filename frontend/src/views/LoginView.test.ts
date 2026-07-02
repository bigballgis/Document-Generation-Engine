import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginView from '@/views/LoginView.vue'
import en from '@/i18n/locales/en'
import { useAppStore } from '@/stores/app'

const routerReplace = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ replace: routerReplace }),
}))

vi.mock('@/stores/session', () => ({
  useSessionStore: () => ({
    login: vi.fn(),
    loginErrorMessageKey: vi.fn(),
    defaultHomePath: () => '/dashboard',
  }),
}))

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerReplace.mockReset()
  })

  it('renders split brand panel and form panel layout', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useAppStore().setBrand('REDBC')

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(LoginView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    expect(wrapper.find('.login-brand-panel').exists()).toBe(true)
    expect(wrapper.find('.login-form-panel').exists()).toBe(true)
    expect(wrapper.text()).toContain('Sign in')
    expect(wrapper.text()).toContain('Sign in to continue')
  })
})
