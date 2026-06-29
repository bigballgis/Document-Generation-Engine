import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'
import RoleJourneyTimeline from '@/components/journey/RoleJourneyTimeline.vue'
import en from '@/i18n/locales/en'
import { masterDesignerJourneySteps } from '@/constants/roleJourneyDefinitions'

function mountTimeline(
  props: {
    steps?: typeof masterDesignerJourneySteps
    currentStepIndex: number | null
    guidanceKey?: string
    ariaLabelKey?: string
    titleKey?: string
  },
) {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages: { en },
  })

  return mount(RoleJourneyTimeline, {
    attachTo: document.body,
    props: {
      steps: masterDesignerJourneySteps,
      ...props,
    },
    global: {
      plugins: [i18n],
    },
  })
}

describe('RoleJourneyTimeline', () => {
  it('renders completed, current, and upcoming step states when currentStepIndex is 2', async () => {
    const wrapper = mountTimeline({ currentStepIndex: 2 })
    await flushPromises()

    const steps = wrapper.findAll('[data-journey-step]')
    expect(steps).toHaveLength(4)
    expect(steps[0]?.classes()).toContain('is-completed')
    expect(steps[1]?.classes()).toContain('is-completed')
    expect(steps[2]?.classes()).toContain('is-current')
    expect(steps[3]?.classes()).toContain('is-upcoming')

    expect(steps[2]?.attributes('aria-current')).toBe('step')
    expect(wrapper.find('[data-journey-guidance]').text()).toBe(
      en.journey.roles.MASTER_DESIGNER.steps.submitReview.guidance,
    )
  })

  it('shows onboarding guidance and no aria-current when currentStepIndex is null', async () => {
    const wrapper = mountTimeline({ currentStepIndex: null })
    await flushPromises()

    expect(wrapper.findAll('[aria-current="step"]')).toHaveLength(0)
    expect(wrapper.find('[data-journey-guidance]').text()).toBe(
      en.journey.roles.MASTER_DESIGNER.empty.guidance,
    )
    wrapper.findAll('[data-journey-step]').forEach((step) => {
      expect(step.classes()).toContain('is-upcoming')
    })
  })

  it('uses guidanceKey override regardless of currentStepIndex', async () => {
    const wrapper = mountTimeline({
      currentStepIndex: 2,
      guidanceKey: 'journey.custom.demo',
    })
    await flushPromises()

    expect(wrapper.find('[data-journey-guidance]').text()).toBe('Custom demo guidance')
  })

  it('renders nothing when steps is empty', () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(RoleJourneyTimeline, {
      props: { steps: [], currentStepIndex: null },
      global: { plugins: [i18n] },
    })

    expect(wrapper.find('[data-journey-timeline]').exists()).toBe(false)
  })

  it('clamps out-of-range currentStepIndex to the last step', async () => {
    const wrapper = mountTimeline({ currentStepIndex: 99 })
    await flushPromises()

    const steps = wrapper.findAll('[data-journey-step]')
    expect(steps[3]?.classes()).toContain('is-current')
    expect(steps[3]?.attributes('aria-current')).toBe('step')
  })

  it('exposes exactly one aria-current step when currentStepIndex is 1', async () => {
    const wrapper = mountTimeline({ currentStepIndex: 1 })
    await flushPromises()

    expect(wrapper.findAll('[aria-current="step"]')).toHaveLength(1)
  })

  it('wraps the stepper in a navigation landmark with aria label', async () => {
    const wrapper = mountTimeline({ currentStepIndex: null })
    await flushPromises()

    const nav = wrapper.find('[role="navigation"]')
    expect(nav.exists()).toBe(true)
    expect(nav.attributes('aria-label')).toBe(en.journey.timeline.ariaLabel)
  })

  it('moves focus to the next step on ArrowRight', async () => {
    const wrapper = mountTimeline({ currentStepIndex: null })
    await flushPromises()

    const steps = wrapper.findAll('[data-journey-step]')
    const first = steps[0]?.element as HTMLElement
    const second = steps[1]?.element as HTMLElement
    first.focus()

    await steps[0]?.trigger('keydown', { key: 'ArrowRight' })
    await flushPromises()

    expect(document.activeElement).toBe(second)
  })

  it('renders titleKey when provided', async () => {
    const wrapper = mountTimeline({
      currentStepIndex: null,
      titleKey: 'journey.roles.MASTER_DESIGNER.title',
    })
    await flushPromises()

    expect(wrapper.find('[data-journey-title]').text()).toBe(
      en.journey.roles.MASTER_DESIGNER.title,
    )
  })
})
