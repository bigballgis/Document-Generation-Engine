import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import SessionLimitReminder from '@/components/session/SessionLimitReminder.vue'
import en from '@/i18n/locales/en'
import zhCN from '@/i18n/locales/zh-CN'

function mountReminder(locale: 'en' | 'zh-CN' = 'en') {
  const i18n = createI18n({
    legacy: false,
    locale,
    fallbackLocale: 'en',
    messages: { en, 'zh-CN': zhCN },
  })
  return mount(SessionLimitReminder, {
    global: {
      plugins: [i18n, ElementPlus],
    },
  })
}

describe('SessionLimitReminder', () => {
  it('renders an accessible non-blocking alert with the reminder copy', () => {
    const wrapper = mountReminder()

    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Session ending soon')
    expect(wrapper.text()).toContain(
      'Your sign-in session is about to reach its time limit. Please save your work, then sign in again to continue.',
    )
    // Copy must never surface technical terms (spec §12: no token / JWT wording).
    expect(wrapper.text()).not.toMatch(/token|JWT/i)
  })

  it('emits action when the sign-in-again button is pressed', async () => {
    const wrapper = mountReminder()

    const actionButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Sign in again'))
    expect(actionButton).toBeDefined()

    await actionButton!.trigger('click')
    expect(wrapper.emitted('action')).toHaveLength(1)
  })

  it('binds the warning-scoped styling hooks on the action button', () => {
    const wrapper = mountReminder()

    // The scoped style overrides Element Plus button variables via the
    // `.el-button.session-limit-reminder__action` compound selector, keeping
    // the action brand-neutral with AA contrast on the warning banner. Both
    // class hooks must stay on the rendered button for that rule to bind.
    const actionButton = wrapper.find('button.session-limit-reminder__action')
    expect(actionButton.exists()).toBe(true)
    expect(actionButton.classes()).toContain('el-button')
    expect(wrapper.find('[role="alert"] button.session-limit-reminder__action').exists()).toBe(
      true,
    )
  })

  it('renders zh-CN copy from the locale bundle', () => {
    const wrapper = mountReminder('zh-CN')

    expect(wrapper.text()).toContain('会话即将结束')
    expect(wrapper.text()).toContain('您的登录会话即将到达时长上限。请先保存当前工作，然后重新登录以继续使用。')
    expect(wrapper.text()).toContain('重新登录')
  })
})
