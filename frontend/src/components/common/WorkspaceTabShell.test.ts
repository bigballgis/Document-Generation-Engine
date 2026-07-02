import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import en from '@/i18n/locales/en'

describe('WorkspaceTabShell', () => {
  const tabs = [
    { name: 'design', labelKey: 'templates.devWorkspace.tabs.design' },
    { name: 'testing', labelKey: 'templates.devWorkspace.tabs.testing' },
  ]

  function mountShell(modelValue = 'design') {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(WorkspaceTabShell, {
      props: { modelValue, tabs, 'onUpdate:modelValue': () => undefined },
      slots: {
        design: '<p class="design-pane">Design content</p>',
        testing: '<p class="testing-pane">Testing content</p>',
        actions: '<button class="action-btn">Run</button>',
      },
      global: { plugins: [i18n, ElementPlus] },
    })
  }

  it('renders tab labels and active pane content', () => {
    const wrapper = mountShell('design')
    expect(wrapper.text()).toContain('Template design')
    expect(wrapper.find('.design-pane').exists()).toBe(true)
    expect(wrapper.find('.testing-pane').exists()).toBe(false)
  })

  it('renders right-aligned actions slot', () => {
    const wrapper = mountShell()
    expect(wrapper.find('.workspace-tab-shell__actions .action-btn').exists()).toBe(true)
  })
})
