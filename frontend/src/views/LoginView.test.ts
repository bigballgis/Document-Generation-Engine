import { mount, flushPromises } from '@vue/test-utils'
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

const loginMock = vi.fn()

vi.mock('@/stores/session', () => ({
  useSessionStore: () => ({
    login: loginMock,
    loginErrorMessageKey: vi.fn(),
    defaultHomePath: () => '/dashboard',
  }),
}))

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerReplace.mockReset()
    loginMock.mockReset()
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

  it('does not show username required error when employee id is filled', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

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

    const usernameInput = wrapper.find('input[autocomplete="username"]')
    await usernameInput.setValue('10000001')
    await usernameInput.trigger('blur')
    await flushPromises()

    expect(wrapper.text()).not.toContain('Username is required.')

    const passwordInput = wrapper.find('input[type="password"]')
    await passwordInput.setValue('secret')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(loginMock).toHaveBeenCalledWith('10000001', 'secret')
    expect(wrapper.text()).not.toContain('Username is required.')
  })
})
