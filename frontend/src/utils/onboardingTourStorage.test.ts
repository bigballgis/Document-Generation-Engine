import { beforeEach, describe, expect, it } from 'vitest'
import {
  ONBOARDING_TOUR_DISMISS_VALUE,
  isOnboardingTourDismissed,
  markOnboardingTourDismissed,
  onboardingTourDismissStorageKey,
} from '@/utils/onboardingTourStorage'

describe('onboardingTourStorage', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('builds per-username dismiss key (BDD C8-C6)', () => {
    expect(onboardingTourDismissStorageKey('author.user')).toBe(
      'docgen.onboardingTour.dismissed.v1:author.user',
    )
  })

  it('reports not dismissed when key is absent', () => {
    expect(isOnboardingTourDismissed(localStorage, 'author.user')).toBe(false)
  })

  it('persists dismiss marker and reads it back (BDD-LRP-C8-002)', () => {
    expect(markOnboardingTourDismissed(localStorage, 'author.user')).toBe(true)
    expect(localStorage.getItem(onboardingTourDismissStorageKey('author.user'))).toBe(
      ONBOARDING_TOUR_DISMISS_VALUE,
    )
    expect(isOnboardingTourDismissed(localStorage, 'author.user')).toBe(true)
  })

  it('scopes dismiss per username', () => {
    markOnboardingTourDismissed(localStorage, 'user-a')
    expect(isOnboardingTourDismissed(localStorage, 'user-a')).toBe(true)
    expect(isOnboardingTourDismissed(localStorage, 'user-b')).toBe(false)
  })

  it('returns false from mark when storage throws (best-effort)', () => {
    const broken: Storage = {
      get length() {
        return 0
      },
      clear() {},
      getItem() {
        return null
      },
      key() {
        return null
      },
      removeItem() {},
      setItem() {
        throw new Error('quota')
      },
    }
    expect(markOnboardingTourDismissed(broken, 'author.user')).toBe(false)
  })
})
