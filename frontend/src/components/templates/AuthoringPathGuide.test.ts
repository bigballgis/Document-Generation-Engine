import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import en from '@/i18n/locales/en'
import AuthoringPathGuide from '@/components/templates/AuthoringPathGuide.vue'

function mountGuide(currentStep: 'master' | 'bindings' | 'variables' | 'preview' = 'master') {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(AuthoringPathGuide, {
    props: { currentStep },
    global: { plugins: [i18n, ElementPlus] },
  })
}

describe('AuthoringPathGuide', () => {
  it('BDD-CE-U16-APC-003/006: renders Authoring path (not lifecycle-stepper) without lifecycle CTAs', () => {
    const wrapper = mountGuide('master')

    expect(wrapper.find('[data-testid="authoring-path-guide"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="lifecycle-stepper"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="authoring-path-guide-step-master"]').attributes('aria-current')).toBe(
      'step',
    )
    expect(wrapper.text()).toContain('Authoring path')
    expect(wrapper.text()).toContain('Letterhead')
    expect(wrapper.text()).toContain('Bindings')
    expect(wrapper.text()).toContain('Variables')
    expect(wrapper.text()).toContain('Preview')
    expect(wrapper.text()).toContain('Skip guide')
    expect(wrapper.text()).not.toMatch(/Submit for testing|Approve|Reject|Confirm go-live|Publish/i)
  })

  it('BDD-CE-U16-APC-004: step click emits workspace navigate query', async () => {
    const wrapper = mountGuide('master')
    await wrapper.get('[data-testid="authoring-path-guide-step-bindings"]').trigger('click')
    expect(wrapper.emitted('navigate')?.[0]).toEqual([
      {
        workspaceTab: 'design',
        designTab: 'bindings',
        authoringGuide: '1',
        authoringGuideStep: 'bindings',
      },
    ])

    await wrapper.get('[data-testid="authoring-path-guide-step-preview"]').trigger('click')
    expect(wrapper.emitted('navigate')?.[1]).toEqual([
      {
        workspaceTab: 'testing',
        testingTab: 'previewRuns',
        authoringGuide: '1',
        authoringGuideStep: 'preview',
      },
    ])
  })

  it('BDD-CE-U16-APC-005: Skip / Dismiss emit dismiss', async () => {
    const wrapper = mountGuide('bindings')
    await wrapper.get('[data-testid="authoring-path-guide-skip"]').trigger('click')
    await wrapper.get('[data-testid="authoring-path-guide-dismiss"]').trigger('click')
    expect(wrapper.emitted('dismiss')).toHaveLength(2)
  })

  it('Next advances from Letterhead to Bindings', async () => {
    const wrapper = mountGuide('master')
    await wrapper.get('[data-testid="authoring-path-guide-next"]').trigger('click')
    expect(wrapper.emitted('navigate')?.[0]).toEqual([
      {
        workspaceTab: 'design',
        designTab: 'bindings',
        authoringGuide: '1',
        authoringGuideStep: 'bindings',
      },
    ])
  })
})
