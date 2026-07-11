import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import OnboardingTour from '@/components/layout/OnboardingTour.vue'
import { templateAuthorJourneySteps } from '@/constants/roleJourneyDefinitions'
import en from '@/i18n/locales/en'

function mountTour(props: {
  open?: boolean
  current?: number
}) {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages: { en },
  })

  return mount(OnboardingTour, {
    props: {
      open: props.open ?? true,
      current: props.current ?? 0,
      steps: templateAuthorJourneySteps,
      targetFor: () => () => null,
    },
    attachTo: document.body,
    global: {
      plugins: [i18n, ElementPlus],
    },
  })
}

describe('OnboardingTour', () => {
  it('renders tour host and skip control when open (BDD-LRP-C8-010)', async () => {
    const wrapper = mountTour({ open: true })
    expect(wrapper.find('[data-testid="onboarding-tour"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="onboarding-tour-skip"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="onboarding-tour-next"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('emits dismiss on skip', async () => {
    const wrapper = mountTour({ open: true })
    await wrapper.get('[data-testid="onboarding-tour-skip"]').trigger('click')
    expect(wrapper.emitted('dismiss')?.length).toBe(1)
    wrapper.unmount()
  })

  it('shows finish on last step and emits dismiss', async () => {
    const wrapper = mountTour({
      open: true,
      current: templateAuthorJourneySteps.length - 1,
    })
    expect(wrapper.find('[data-testid="onboarding-tour-finish"]').exists()).toBe(true)
    await wrapper.get('[data-testid="onboarding-tour-finish"]').trigger('click')
    expect(wrapper.emitted('dismiss')?.length).toBe(1)
    wrapper.unmount()
  })

  it('uses journey i18n keys for step title copy (BDD-LRP-C8-012)', () => {
    const wrapper = mountTour({ open: true })
    expect(wrapper.text()).toContain('Create template')
    wrapper.unmount()
  })
})
