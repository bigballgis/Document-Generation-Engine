import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import PageHeader from '@/components/layout/PageHeader.vue'
import en from '@/i18n/locales/en'

describe('PageHeader', () => {
  function mountHeader(props: Record<string, unknown> = {}, slots: Record<string, string> = {}) {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    return mount(PageHeader, {
      props: {
        title: 'Page title',
        ...props,
      },
      slots,
      global: { plugins: [i18n, ElementPlus] },
    })
  }

  it('renders title and optional description', () => {
    const wrapper = mountHeader({
      description: 'Page description',
    })

    expect(wrapper.find('h1').text()).toBe('Page title')
    expect(wrapper.text()).toContain('Page description')
  })

  it('emits back when the back button is clicked', async () => {
    const wrapper = mountHeader({
      showBack: true,
      backLabel: 'Back to list',
    })

    await wrapper.find('.el-button--primary.is-link').trigger('click')

    expect(wrapper.emitted('back')).toHaveLength(1)
  })

  it('renders meta and actions slots', () => {
    const wrapper = mountHeader(
      {},
      {
        meta: '<span class="meta-slot">Status</span>',
        actions: '<button type="button" class="action-slot">Create</button>',
      },
    )

    expect(wrapper.find('.meta-slot').exists()).toBe(true)
    expect(wrapper.find('.action-slot').exists()).toBe(true)
  })
})
