import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { useOnboardingTour } from '@/composables/useOnboardingTour'

describe('useOnboardingTour', () => {
  beforeEach(() => window.localStorage.clear())
  afterEach(() => window.localStorage.clear())

  it('auto-runs when the role tour has not been completed', () => {
    const tour = useOnboardingTour('TEMPLATE_AUTHOR')
    expect(tour.completed.value).toBe(false)
    expect(tour.shouldAutoRun.value).toBe(true)
  })

  it('does not auto-run after the tour is marked complete', () => {
    const tour = useOnboardingTour('TEMPLATE_AUTHOR')
    tour.markComplete()
    expect(tour.completed.value).toBe(true)
    expect(tour.shouldAutoRun.value).toBe(false)
    expect(window.localStorage.getItem('docgen.onboarding.TEMPLATE_AUTHOR')).toBe('true')
  })

  it('reset re-enables auto-run', () => {
    const tour = useOnboardingTour('TEMPLATE_AUTHOR')
    tour.markComplete()
    tour.reset()
    expect(tour.completed.value).toBe(false)
    expect(tour.shouldAutoRun.value).toBe(true)
  })

  it('isolates completion by role', () => {
    const author = useOnboardingTour('TEMPLATE_AUTHOR')
    const approver = useOnboardingTour('TEMPLATE_APPROVER')
    author.markComplete()
    expect(author.shouldAutoRun.value).toBe(false)
    expect(approver.shouldAutoRun.value).toBe(true)
  })

  it('reads existing completion from storage on init', () => {
    window.localStorage.setItem('docgen.onboarding.GLOBAL_ADMIN', 'true')
    const tour = useOnboardingTour('GLOBAL_ADMIN')
    expect(tour.completed.value).toBe(true)
    expect(tour.shouldAutoRun.value).toBe(false)
  })
})
