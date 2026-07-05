import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useNotificationStore } from '@/stores/notifications'

describe('useNotificationStore', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('starts with zero unread and a closed panel', () => {
    const store = useNotificationStore()
    expect(store.unreadCount).toBe(0)
    expect(store.hasUnread).toBe(false)
    expect(store.panelOpen).toBe(false)
  })

  it('sets the unread count and flips hasUnread', () => {
    const store = useNotificationStore()
    store.setUnreadCount(3)
    expect(store.unreadCount).toBe(3)
    expect(store.hasUnread).toBe(true)
    expect(store.lastFetchedAt).not.toBeNull()
  })

  it('clamps negative counts to zero', () => {
    const store = useNotificationStore()
    store.setUnreadCount(-5)
    expect(store.unreadCount).toBe(0)
  })

  it('toggles, opens, and closes the panel', () => {
    const store = useNotificationStore()
    store.togglePanel()
    expect(store.panelOpen).toBe(true)
    store.togglePanel()
    expect(store.panelOpen).toBe(false)
    store.openPanel()
    expect(store.panelOpen).toBe(true)
    store.closePanel()
    expect(store.panelOpen).toBe(false)
  })

  it('markAllRead zeroes the unread count', () => {
    const store = useNotificationStore()
    store.setUnreadCount(4)
    store.markAllRead()
    expect(store.unreadCount).toBe(0)
    expect(store.hasUnread).toBe(false)
  })
})
