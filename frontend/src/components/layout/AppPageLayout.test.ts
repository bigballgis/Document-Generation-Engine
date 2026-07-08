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

    expect(wrapper.find('.app-page-layout').attributes('style')).toContain('max-width: 1440px')
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

    const style = wrapper.find('.app-page-layout').attributes('style')
    expect(style ?? '').not.toContain('max-width')
  })
})
