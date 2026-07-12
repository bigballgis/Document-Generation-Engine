/** Per-user onboarding tour dismiss marker (LR-C8 / C8-C6). */

const ONBOARDING_TOUR_DISMISS_KEY_PREFIX = 'docgen.onboardingTour.dismissed.v1:'

export const ONBOARDING_TOUR_DISMISS_VALUE = '1'

export function onboardingTourDismissStorageKey(username: string): string {
  return `${ONBOARDING_TOUR_DISMISS_KEY_PREFIX}${username}`
}

export function isOnboardingTourDismissed(storage: Storage, username: string): boolean {
  try {
    return storage.getItem(onboardingTourDismissStorageKey(username)) === ONBOARDING_TOUR_DISMISS_VALUE
  } catch {
    return false
  }
}

/** Best-effort write; returns false when storage is unavailable (C8 shell must not block). */
export function markOnboardingTourDismissed(storage: Storage, username: string): boolean {
  try {
    storage.setItem(onboardingTourDismissStorageKey(username), ONBOARDING_TOUR_DISMISS_VALUE)
    return true
  } catch {
    return false
  }
}
