import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import en from '@/i18n/locales/en'
import LifecycleStepper from '@/components/templates/LifecycleStepper.vue'

function mountStepper(props: {
  lifecycleStatus: 'DRAFT' | 'TESTING' | 'APPROVAL' | 'PENDING_RELEASE' | 'PUBLISHED' | 'STOPPED' | 'DEPRECATED'
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
}) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(LifecycleStepper, {
    props,
    global: { plugins: [i18n, ElementPlus] },
  })
}

describe('LifecycleStepper', () => {
  it('BDD-CE-U15-LSS-001: renders stepper with Draft current and no workflow CTAs', () => {
    const wrapper = mountStepper({ lifecycleStatus: 'DRAFT' })

    expect(wrapper.find('[data-testid="lifecycle-stepper"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="lifecycle-stepper-step-draft"]').attributes('aria-current')).toBe(
      'step',
    )
    expect(wrapper.text()).toContain('Draft')
    expect(wrapper.text()).toContain('Testing')
    expect(wrapper.text()).toContain('Ready for approval')
    expect(wrapper.text()).toContain('Pending approval')
    expect(wrapper.text()).toContain('Pending release')
    expect(wrapper.text()).toContain('Published')
    expect(wrapper.text()).not.toMatch(/Submit for testing|Approve|Confirm go-live/i)
  })

  it('BDD-CE-U15-LSS-002: highlights Pending approval for APPROVAL + PENDING_DECISION', () => {
    const wrapper = mountStepper({
      lifecycleStatus: 'APPROVAL',
      approvalSubState: 'PENDING_DECISION',
    })
    expect(
      wrapper.get('[data-testid="lifecycle-stepper-step-pendingApproval"]').attributes('aria-current'),
    ).toBe('step')
  })

  it('BDD-CE-U15-LSS-003: terminal statuses show note without a current linear step', () => {
    const wrapper = mountStepper({ lifecycleStatus: 'STOPPED' })
    expect(wrapper.get('[data-testid="lifecycle-stepper"]').attributes('data-terminal')).toBe('true')
    expect(wrapper.get('[data-testid="lifecycle-stepper-terminal"]').text()).toMatch(/Paused/i)
    expect(wrapper.find('[aria-current="step"]').exists()).toBe(false)
  })

  it('BDD-CE-U15-LSS-007: optional step click emits orientation navigate only', async () => {
    const wrapper = mountStepper({ lifecycleStatus: 'PENDING_RELEASE' })
    await wrapper.get('[data-testid="lifecycle-stepper-step-testing"]').trigger('click')
    expect(wrapper.emitted('navigate')?.[0]).toEqual([{ workspaceTab: 'testing' }])
  })

  it('does not emit navigate for Published step', async () => {
    const onNavigate = vi.fn()
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(LifecycleStepper, {
      props: { lifecycleStatus: 'PUBLISHED' },
      attrs: { onNavigate },
      global: { plugins: [i18n, ElementPlus] },
    })
    await wrapper.get('[data-testid="lifecycle-stepper-step-published"]').trigger('click')
    expect(wrapper.emitted('navigate')).toBeUndefined()
  })
})
