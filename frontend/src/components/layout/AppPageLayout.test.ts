import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'

describe('AppPageLayout', () => {
  it('applies max width for contained layout variant', () => {
    const wrapper = mount(AppPageLayout, {
      slots: {
        default: '<div class="content">Page</div>',
      },
    })

    const inner = wrapper.find('.app-page-layout__inner')
    expect(inner.exists()).toBe(true)
    expect(inner.attributes('style')).toContain('max-width: 1440px')
  })

  it('omits max width for fluid layout variant', () => {
    const wrapper = mount(AppPageLayout, {
      props: {
        layoutVariant: 'fluid',
      },
      slots: {
        default: '<div class="content">Page</div>',
      },
    })

    const root = wrapper.find('.app-page-layout')
    expect(root.classes()).toContain('app-page-layout--fluid')
    expect(root.attributes('style') ?? '').not.toContain('max-width')
  })

  it('uses inner container for contained layout variant', () => {
    const wrapper = mount(AppPageLayout, {
      slots: {
        default: '<div class="content">Page</div>',
      },
    })

    expect(wrapper.find('.app-page-layout').classes()).toContain('app-page-layout--contained')
    expect(wrapper.find('.app-page-layout__inner').exists()).toBe(true)
  })

  it('applies panel surface by default for full-width white workspace', () => {
    const wrapper = mount(AppPageLayout, {
      slots: {
        default: '<div class="content">Page</div>',
      },
    })

    expect(wrapper.find('.app-page-layout').classes()).toContain('app-page-layout--panel')
  })
})
