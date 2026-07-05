/**
 * LR-C8: role-aware onboarding tour state. Tracks whether the current role's tour has been
 * completed (persisted to localStorage) and exposes a `markComplete` / `reset` API.
 *
 * The actual tour UI is rendered by `el-tour` in the shell; this composable owns only the
 * completion state so the tour auto-runs once per role per browser.
 */
import { computed, ref } from 'vue'

const STORAGE_PREFIX = 'docgen.onboarding.'

export function useOnboardingTour(role: string) {
  const storageKey = `${STORAGE_PREFIX}${role}`
  const completed = ref<boolean>(loadCompleted())

  function loadCompleted(): boolean {
    try {
      return window.localStorage.getItem(storageKey) === 'true'
    } catch {
      return false
    }
  }

  function markComplete() {
    try {
      window.localStorage.setItem(storageKey, 'true')
    } catch {
      // best-effort
    }
    completed.value = true
  }

  function reset() {
    try {
      window.localStorage.removeItem(storageKey)
    } catch {
      // best-effort
    }
    completed.value = false
  }

  const shouldAutoRun = computed(() => !completed.value)

  return {
    completed,
    shouldAutoRun,
    markComplete,
    reset,
  }
}
