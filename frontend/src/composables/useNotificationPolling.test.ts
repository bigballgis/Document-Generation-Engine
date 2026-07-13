import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import { canViewCollaborationWorkItems, sessionContext } from '@/auth/roles'
import {
  NOTIFICATION_POLL_INTERVAL_MS,
  useNotificationPolling,
} from '@/composables/useNotificationPolling'
import { useCollaborationNotificationsStore } from '@/stores/collaborationNotifications'
import { useSessionStore } from '@/stores/session'
import type { ManagementCapabilities } from '@/types/session'
import { ROUTE_KEYS } from '@/routing/routeKeys'

vi.mock('@/api/collaboration', () => ({
  getCollaborationNotificationUnreadCount: vi.fn(),
  listCollaborationNotifications: vi.fn(),
  markCollaborationNotificationRead: vi.fn(),
  markAllCollaborationNotificationsRead: vi.fn(),
}))

const capabilities: ManagementCapabilities = {
  manageMasters: false,
  reviewMasters: false,
  authorTemplates: false,
  decideTests: true,
  decideApprovals: false,
  publishTemplates: false,
  stopTemplates: false,
  restoreOrDeprecateTemplates: false,
  deleteTemplates: false,
  exportTemplates: false,
  viewCollaborationWorkItems: true,
  maintainCollaborationTimeoutConfig: false,
  authorContentModules: false,
  decideContentModuleReviews: false,
  manageContentModuleLifecycle: false,
  manageApiPolicy: false,
  readAudit: false,
}

function patchSession(viewCollaborationWorkItems: boolean) {
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000003',
      displayName: 'Tester',
      email: 'tester@example.com',
      authSource: 'LOCAL',
      roles: ['TEMPLATE_TESTER'],
      authorizedGroupCodes: ['RETAIL'],
      defaultRoute: ROUTE_KEYS.dashboardHome,
      visibleRoutes: [ROUTE_KEYS.dashboardHome],
      capabilities: { ...capabilities, viewCollaborationWorkItems },
      expiresAt: new Date(Date.now() + 30 * 60_000).toISOString(),
    },
  })
}

describe('useNotificationPolling', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
    Object.defineProperty(document, 'visibilityState', {
      configurable: true,
      get: () => 'visible',
    })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('exports the 30s default poll interval', () => {
    expect(NOTIFICATION_POLL_INTERVAL_MS).toBe(30_000)
  })

  it('fetches unread immediately when started with collaboration capability', async () => {
    patchSession(true)
    const store = useCollaborationNotificationsStore()
    const fetchUnreadCount = vi.spyOn(store, 'fetchUnreadCount').mockResolvedValue()

    const polling = useNotificationPolling({ intervalMs: 5_000 })
    await nextTick()

    expect(canViewCollaborationWorkItems(sessionContext(useSessionStore().session))).toBe(true)
    expect(fetchUnreadCount).toHaveBeenCalledTimes(1)

    polling.stop()
  })

  it('does not poll when collaboration capability is absent', async () => {
    patchSession(false)
    const store = useCollaborationNotificationsStore()
    const fetchUnreadCount = vi.spyOn(store, 'fetchUnreadCount').mockResolvedValue()

    const polling = useNotificationPolling({ intervalMs: 5_000 })
    await nextTick()
    await vi.advanceTimersByTimeAsync(15_000)

    expect(fetchUnreadCount).not.toHaveBeenCalled()
    polling.stop()
  })

  it('polls on interval while document is visible', async () => {
    patchSession(true)
    const store = useCollaborationNotificationsStore()
    const fetchUnreadCount = vi.spyOn(store, 'fetchUnreadCount').mockResolvedValue()

    const polling = useNotificationPolling({ intervalMs: 5_000 })
    await nextTick()
    expect(fetchUnreadCount).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(5_000)
    expect(fetchUnreadCount).toHaveBeenCalledTimes(2)

    await vi.advanceTimersByTimeAsync(5_000)
    expect(fetchUnreadCount).toHaveBeenCalledTimes(3)

    polling.stop()
  })

  it('pauses polling while document is hidden and refetches on visible', async () => {
    let visibility: DocumentVisibilityState = 'visible'
    Object.defineProperty(document, 'visibilityState', {
      configurable: true,
      get: () => visibility,
    })

    patchSession(true)
    const store = useCollaborationNotificationsStore()
    const fetchUnreadCount = vi.spyOn(store, 'fetchUnreadCount').mockResolvedValue()

    const polling = useNotificationPolling({ intervalMs: 5_000 })
    await nextTick()
    expect(fetchUnreadCount).toHaveBeenCalledTimes(1)

    visibility = 'hidden'
    document.dispatchEvent(new Event('visibilitychange'))
    await nextTick()

    await vi.advanceTimersByTimeAsync(20_000)
    expect(fetchUnreadCount).toHaveBeenCalledTimes(1)

    visibility = 'visible'
    document.dispatchEvent(new Event('visibilitychange'))
    await nextTick()
    expect(fetchUnreadCount).toHaveBeenCalledTimes(2)

    polling.stop()
  })

  it('refreshes when session becomes authenticated after start', async () => {
    const store = useCollaborationNotificationsStore()
    const fetchUnreadCount = vi.spyOn(store, 'fetchUnreadCount').mockResolvedValue()

    // Shell may mount before restoreSession / login applies token+session.
    const polling = useNotificationPolling({ intervalMs: 5_000 })
    await nextTick()
    expect(fetchUnreadCount).not.toHaveBeenCalled()

    patchSession(true)
    await nextTick()
    expect(fetchUnreadCount).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(5_000)
    expect(fetchUnreadCount).toHaveBeenCalledTimes(2)

    polling.stop()
  })

  it('refreshes when collaboration capability becomes available on an authenticated session', async () => {
    patchSession(false)
    const store = useCollaborationNotificationsStore()
    const fetchUnreadCount = vi.spyOn(store, 'fetchUnreadCount').mockResolvedValue()

    const polling = useNotificationPolling({ intervalMs: 5_000 })
    await nextTick()
    expect(fetchUnreadCount).not.toHaveBeenCalled()

    patchSession(true)
    await nextTick()
    expect(fetchUnreadCount).toHaveBeenCalledTimes(1)

    polling.stop()
  })

  it('clears unread state when collaboration capability becomes false', async () => {
    patchSession(true)
    const store = useCollaborationNotificationsStore()
    vi.spyOn(store, 'fetchUnreadCount').mockResolvedValue()
    store.unreadCount = 3
    store.items = [
      {
        workItemId: 'wi-1',
        templateId: 'tpl-1',
        templateName: 'Loan Notice',
        groupCode: 'RETAIL',
        queue: 'TEST',
        triggerType: 'SUBMIT_FOR_TEST',
        summaryText: 'Template submitted for testing',
        createdAt: '2026-06-26T10:00:00Z',
        ageSeconds: 120,
        read: false,
      },
    ]

    const polling = useNotificationPolling({ intervalMs: 5_000 })
    await nextTick()

    patchSession(false)
    await nextTick()

    expect(store.unreadCount).toBe(0)
    expect(store.items).toEqual([])
    polling.stop()
  })

  it('clears unread state on logout so the next SPA user does not inherit a badge', async () => {
    patchSession(true)
    const store = useCollaborationNotificationsStore()
    const sessionStore = useSessionStore()
    vi.spyOn(store, 'fetchUnreadCount').mockResolvedValue()
    store.unreadCount = 7

    const polling = useNotificationPolling({ intervalMs: 5_000 })
    await nextTick()

    sessionStore.clearSession()
    await nextTick()

    expect(store.unreadCount).toBe(0)
    polling.stop()
  })
})
