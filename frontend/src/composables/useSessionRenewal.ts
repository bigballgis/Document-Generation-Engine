import { getCurrentScope, onScopeDispose, ref, type Ref } from 'vue'
import { resolveApiError } from '@/api/http'
import { useSessionStore } from '@/stores/session'

/**
 * Silent sliding session renewal + absolute-limit reminder scheduler
 * (LR-B6, BDD-LRP-SESSION-001).
 *
 * Windows follow the confirmed spec defaults ([ASSUMED-THRESHOLDS] /
 * [ASSUMED-ACTIVITY-DEFINITION]): renew when the token has < 5 minutes left
 * and the user was active within the last 5 minutes; show the non-blocking
 * reminder and suspend renewal when the absolute deadline is < 10 minutes
 * away. All instants come from server-issued response fields — the JWT is
 * never decoded and the local clock is only used for relative scheduling
 * (spec B9). Cross-tab renewal races are accepted behavior handled by the
 * shared 401 flow (spec SCEN-CONCURRENT-01, COR-F03).
 */
export const SESSION_RENEWAL_TICK_MS = 60_000
export const SESSION_RENEWAL_WINDOW_MS = 5 * 60_000
export const SESSION_REMINDER_WINDOW_MS = 10 * 60_000
export const SESSION_ACTIVITY_WINDOW_MS = 5 * 60_000

const ACTIVITY_THROTTLE_MS = 5_000
const ACTIVITY_EVENTS = ['pointerdown', 'pointermove', 'keydown'] as const
const ABSOLUTE_LIMIT_ERROR_CODE = 'SESSION_ABSOLUTE_LIMIT_REACHED'

export interface SessionRenewalController {
  /** True while the near-absolute-limit reminder should be shown (SCEN-UX-02). */
  reminderVisible: Readonly<Ref<boolean>>
  /** Detaches the tick timer and activity listeners. */
  stop: () => void
}

function parseInstant(value: string | null): number | null {
  if (!value) {
    return null
  }
  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? null : parsed
}

export function useSessionRenewal(): SessionRenewalController {
  const sessionStore = useSessionStore()
  const reminderVisible = ref(false)

  // Mounting the scheduler follows a navigation, which counts as activity.
  let lastActivityAt = Date.now()
  let absoluteLimitReached = false
  let renewInFlight = false
  let intervalId: ReturnType<typeof setInterval> | null = null

  function recordActivity() {
    const now = Date.now()
    if (now - lastActivityAt >= ACTIVITY_THROTTLE_MS) {
      lastActivityAt = now
    }
  }

  async function evaluate(): Promise<void> {
    const now = Date.now()

    if (!sessionStore.authenticated) {
      reminderVisible.value = false
      absoluteLimitReached = false
      return
    }

    const deadline = parseInstant(sessionStore.sessionAbsoluteDeadline)
    if (deadline !== null && deadline - now < SESSION_REMINDER_WINDOW_MS) {
      // Near the absolute limit: surface the reminder and suspend silent
      // renewal; the shared 401 flow takes over once the deadline passes.
      reminderVisible.value = true
      return
    }
    reminderVisible.value = false

    if (absoluteLimitReached || renewInFlight) {
      return
    }

    const expiresAt = parseInstant(sessionStore.accessTokenExpiresAt)
    if (expiresAt === null || expiresAt - now >= SESSION_RENEWAL_WINDOW_MS) {
      return
    }
    if (now - lastActivityAt > SESSION_ACTIVITY_WINDOW_MS) {
      // Idle sessions are not renewed; the token lapses naturally (spec B2).
      return
    }

    renewInFlight = true
    try {
      await sessionStore.renewSession()
    } catch (error) {
      if (resolveApiError(error)?.error.code === ABSOLUTE_LIMIT_ERROR_CODE) {
        absoluteLimitReached = true
      }
      // Transient failures (e.g. network blips) retry on the next tick.
    } finally {
      renewInFlight = false
    }
  }

  function stop() {
    if (intervalId !== null) {
      clearInterval(intervalId)
      intervalId = null
    }
    for (const eventName of ACTIVITY_EVENTS) {
      window.removeEventListener(eventName, recordActivity)
    }
  }

  intervalId = setInterval(() => {
    void evaluate()
  }, SESSION_RENEWAL_TICK_MS)
  for (const eventName of ACTIVITY_EVENTS) {
    window.addEventListener(eventName, recordActivity, { passive: true })
  }
  void evaluate()

  if (getCurrentScope()) {
    onScopeDispose(stop)
  }

  return { reminderVisible, stop }
}
