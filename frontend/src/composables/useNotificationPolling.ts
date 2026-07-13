import { getCurrentScope, onScopeDispose, watch, type Ref } from 'vue'
import { canViewCollaborationWorkItems, sessionContext } from '@/auth/roles'
import { useCollaborationNotificationsStore } from '@/stores/collaborationNotifications'
import { useSessionStore } from '@/stores/session'
import { NOTIFICATION_POLL_INTERVAL_MS } from '@/utils/notificationCenter'

export { NOTIFICATION_POLL_INTERVAL_MS }

export interface UseNotificationPollingOptions {
  /** Override poll interval (ms). Default 30_000. */
  intervalMs?: number
  /** Optional external gate; defaults to session collaboration capability. */
  enabled?: Ref<boolean> | (() => boolean)
}

export interface NotificationPollingController {
  stop: () => void
  refreshNow: () => Promise<void>
}

function isDocumentVisible(): boolean {
  return typeof document === 'undefined' || document.visibilityState !== 'hidden'
}

function resolveEnabled(
  sessionStore: ReturnType<typeof useSessionStore>,
  enabled?: Ref<boolean> | (() => boolean),
): boolean {
  if (typeof enabled === 'function') {
    return enabled()
  }
  if (enabled) {
    return enabled.value
  }
  return canViewCollaborationWorkItems(sessionContext(sessionStore.session))
}

/**
 * Unread-count polling for the shell notification bell (LR-C7 / C7-C7–C9).
 * Pauses when `document.visibilityState === 'hidden'`; refetches immediately on visible.
 * Starts / refreshes only after session token + collaboration gate are ready (avoids
 * shell-mount race where the first poll runs before auth is attached).
 */
export function useNotificationPolling(
  options: UseNotificationPollingOptions = {},
): NotificationPollingController {
  const sessionStore = useSessionStore()
  const notificationsStore = useCollaborationNotificationsStore()
  const intervalMs = options.intervalMs ?? NOTIFICATION_POLL_INTERVAL_MS

  let intervalId: ReturnType<typeof setInterval> | null = null
  let stopped = false

  function isReadyToPoll(): boolean {
    return Boolean(sessionStore.authenticated) && resolveEnabled(sessionStore, options.enabled)
  }

  async function refreshNow(): Promise<void> {
    if (stopped || !isReadyToPoll()) {
      return
    }
    try {
      await notificationsStore.fetchUnreadCount()
    } catch {
      // Errors stay on the store (last successful unread retained).
    }
  }

  function clearTimer() {
    if (intervalId !== null) {
      clearInterval(intervalId)
      intervalId = null
    }
  }

  function startTimer() {
    clearTimer()
    if (stopped || !isReadyToPoll() || !isDocumentVisible()) {
      return
    }
    intervalId = setInterval(() => {
      void refreshNow()
    }, intervalMs)
  }

  function onVisibilityChange() {
    if (stopped) {
      return
    }
    if (isDocumentVisible()) {
      void refreshNow()
      startTimer()
      return
    }
    clearTimer()
  }

  function stop() {
    stopped = true
    clearTimer()
    stopSessionWatch()
    if (typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', onVisibilityChange)
    }
  }

  if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', onVisibilityChange)
  }

  // Immediate + reactive: poll as soon as auth+capability are ready, and again
  // when they flip from not-ready → ready (shell mount / restoreSession race).
  const stopSessionWatch = watch(
    () =>
      Boolean(sessionStore.authenticated) && resolveEnabled(sessionStore, options.enabled),
    (ready) => {
      if (stopped) {
        return
      }
      if (ready) {
        void refreshNow()
        startTimer()
        return
      }
      // Logout or collaboration capability lost: drop in-memory unread so the
      // next SPA user / role never inherits a stale badge (arch Major #2).
      clearTimer()
      notificationsStore.clear()
    },
    { immediate: true },
  )

  if (getCurrentScope()) {
    onScopeDispose(stop)
  }

  return { stop, refreshNow }
}
