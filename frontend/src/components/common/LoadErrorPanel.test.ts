import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ElementPlus from 'element-plus'

import LoadErrorPanel from './LoadErrorPanel.vue'
import { i18n } from '@/i18n'

describe('LoadErrorPanel', () => {
  it('renders retry action with message key', () => {
    const wrapper = mount(LoadErrorPanel, {
      props: {
        messageKey: 'common.loadError',
      },
      global: {
        plugins: [ElementPlus, i18n],
      },
    })

    expect(wrapper.text()).toContain('Unable to load this page.')
    expect(wrapper.find('button').text()).toMatch(/retry/i)
  })

  it('shows retryable hint when error is retryable', () => {
    const wrapper = mount(LoadErrorPanel, {
      props: {
        messageKey: 'common.loadError',
        retryable: true,
      },
      global: {
        plugins: [ElementPlus, i18n],
      },
    })

    expect(wrapper.text()).toContain('This error is retryable')
  })
})
