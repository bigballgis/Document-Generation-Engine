import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'

describe('AppPageLayout', () => {
  it('defaults to fluid layout for management pages (BDD-SYS-NORM-W1-001)', () => {
    const wrapper = mount(AppPageLayout, {
      slots: {
        default: '<div class="content">Page</div>',
      },
    })

    const root = wrapper.find('.app-page-layout')
    expect(root.classes()).toContain('app-page-layout--fluid')
    expect(wrapper.find('.app-page-layout__inner').exists()).toBe(false)
    expect(root.attributes('style') ?? '').not.toContain('max-width')
  })

  it('applies max width for contained layout variant', () => {
    const wrapper = mount(AppPageLayout, {
      props: {
        layoutVariant: 'contained',
      },
      slots: {
        default: '<div class="content">Page</div>',
      },
    })

    const inner = wrapper.find('.app-page-layout__inner')
    expect(inner.exists()).toBe(true)
    expect(inner.attributes('style')).toContain('max-width: 1440px')
    expect(wrapper.find('.app-page-layout').classes()).toContain('app-page-layout--contained')
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

  it('applies panel surface by default for full-width white workspace', () => {
    const wrapper = mount(AppPageLayout, {
      slots: {
        default: '<div class="content">Page</div>',
      },
    })

    expect(wrapper.find('.app-page-layout').classes()).toContain('app-page-layout--panel')
  })
})
