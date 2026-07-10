import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import en from '@/i18n/locales/en'

function mountPanel(slots?: { actions?: string }) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(EmptyStatePanel, {
    props: {
      titleKey: 'templates.list.empty',
      descriptionKey: 'contentModules.list.emptyDescription',
    },
    slots,
    global: { plugins: [i18n, ElementPlus] },
  })
}

describe('EmptyStatePanel', () => {
  it('renders title and description from i18n keys', () => {
    const wrapper = mountPanel()
    expect(wrapper.text()).toContain('No template packages yet.')
    expect(wrapper.text()).toContain('Create a module to start authoring reusable clause content.')
    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(false)
  })

  it('renders actions slot content when provided', () => {
    const wrapper = mountPanel({
      actions: '<button type="button">Create now</button>',
    })
    expect(wrapper.find('[data-testid="empty-state-actions"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Create now')
  })
})
