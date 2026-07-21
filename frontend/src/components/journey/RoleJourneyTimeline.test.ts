import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'
import ElementPlus from 'element-plus'
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
    inlineHelp?: boolean
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
      plugins: [i18n, ElementPlus],
    },
  })
}

describe('RoleJourneyTimeline', () => {
  it('renders completed, current, and upcoming step states when currentStepIndex is 2', async () => {
    const wrapper = mountTimeline({ currentStepIndex: 2, inlineHelp: true })
    await flushPromises()

    const steps = wrapper.findAll('[data-journey-step]')
    expect(steps).toHaveLength(4)
    expect(steps[0]?.classes()).toContain('is-completed')
    expect(steps[1]?.classes()).toContain('is-completed')
    expect(steps[2]?.classes()).toContain('is-current')
    expect(steps[3]?.classes()).toContain('is-upcoming')

    expect(steps[2]?.attributes('aria-current')).toBe('step')
    expect(wrapper.find('[data-journey-guidance]').text()).toBe(
      en.journey.roles.DOCUMENT_AUTHOR.letterhead.steps.submitReview.guidance,
    )
  })

  it('shows onboarding guidance and no aria-current when currentStepIndex is null', async () => {
    const wrapper = mountTimeline({ currentStepIndex: null, inlineHelp: true })
    await flushPromises()

    expect(wrapper.findAll('[aria-current="step"]')).toHaveLength(0)
    expect(wrapper.find('[data-journey-guidance]').text()).toBe(
      en.journey.roles.DOCUMENT_AUTHOR.letterhead.empty.guidance,
    )
    wrapper.findAll('[data-journey-step]').forEach((step) => {
      expect(step.classes()).toContain('is-upcoming')
    })
  })

  it('uses guidanceKey override regardless of currentStepIndex', async () => {
    const wrapper = mountTimeline({
      currentStepIndex: 2,
      guidanceKey: 'journey.custom.demo',
      inlineHelp: true,
    })
    await flushPromises()

    expect(wrapper.find('[data-journey-guidance]').text()).toBe('Custom demo guidance')
  })

  it('BDD-SYS-NORM-W8-014 — shows honest empty guidance when steps array is empty', () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(RoleJourneyTimeline, {
      props: { steps: [], currentStepIndex: null },
      global: { plugins: [i18n, ElementPlus] },
    })

    expect(wrapper.find('[data-testid="journey-timeline-honest-empty"]').exists()).toBe(true)
    expect(wrapper.text()).toContain(en.journey.timeline.emptyTitle)
    expect(wrapper.text()).toContain(en.journey.timeline.empty.guidance)
    expect(wrapper.find('[data-journey-timeline]').exists()).toBe(false)
  })

  it('BDD-SYS-NORM-W8-013 — empty work set shows visible guidance even when inlineHelp is false', async () => {
    const wrapper = mountTimeline({
      currentStepIndex: null,
      titleKey: 'journey.roles.DOCUMENT_AUTHOR.title',
      inlineHelp: false,
    })
    await flushPromises()

    expect(wrapper.find('[data-journey-guidance]').exists()).toBe(true)
    expect(wrapper.find('[data-journey-guidance]').text()).toBe(
      en.journey.roles.DOCUMENT_AUTHOR.letterhead.empty.guidance,
    )
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
      titleKey: 'journey.roles.DOCUMENT_AUTHOR.title',
    })
    await flushPromises()

    expect(wrapper.find('[data-journey-title]').text()).toBe(
      en.journey.roles.DOCUMENT_AUTHOR.title,
    )
  })

  it('shows context help instead of inline guidance when inlineHelp is false', async () => {
    const wrapper = mountTimeline({
      currentStepIndex: 2,
      titleKey: 'journey.roles.DOCUMENT_AUTHOR.title',
      inlineHelp: false,
    })
    await flushPromises()

    expect(wrapper.find('[data-journey-guidance]').exists()).toBe(false)
    expect(wrapper.find('.context-help-trigger').exists()).toBe(true)
  })
})
