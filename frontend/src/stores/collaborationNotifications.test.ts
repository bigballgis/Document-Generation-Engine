import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as collaborationApi from '@/api/collaboration'
import { useCollaborationNotificationsStore } from '@/stores/collaborationNotifications'

vi.mock('@/api/collaboration', () => ({
  getCollaborationNotificationUnreadCount: vi.fn(),
  listCollaborationNotifications: vi.fn(),
  markCollaborationNotificationRead: vi.fn(),
  markAllCollaborationNotificationsRead: vi.fn(),
}))

const sampleItem = {
  workItemId: 'wi-1',
  templateId: 'tpl-1',
  templateName: 'Loan Notice',
  groupCode: 'RETAIL',
  queue: 'TEST' as const,
  triggerType: 'SUBMIT_FOR_TEST' as const,
  summaryText: 'Template submitted for testing',
  createdAt: '2026-06-26T10:00:00Z',
  ageSeconds: 120,
  read: false,
}

describe('collaborationNotifications store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(collaborationApi.getCollaborationNotificationUnreadCount).mockReset()
    vi.mocked(collaborationApi.listCollaborationNotifications).mockReset()
    vi.mocked(collaborationApi.markCollaborationNotificationRead).mockReset()
    vi.mocked(collaborationApi.markAllCollaborationNotificationsRead).mockReset()
  })

  it('loads unread count into state', async () => {
    vi.mocked(collaborationApi.getCollaborationNotificationUnreadCount).mockResolvedValue({
      unreadCount: 2,
    })

    const store = useCollaborationNotificationsStore()
    await store.fetchUnreadCount()

    expect(store.unreadCount).toBe(2)
    expect(store.unreadErrorMessageKey).toBeNull()
  })

  it('keeps last successful unread count when refresh fails', async () => {
    vi.mocked(collaborationApi.getCollaborationNotificationUnreadCount)
      .mockResolvedValueOnce({ unreadCount: 4 })
      .mockRejectedValueOnce(new Error('network'))

    const store = useCollaborationNotificationsStore()
    await store.fetchUnreadCount()
    await expect(store.fetchUnreadCount()).rejects.toThrow('network')

    expect(store.unreadCount).toBe(4)
    expect(store.unreadErrorMessageKey).toBe('collaboration.notifications.error.loadUnread')
  })

  it('loads notification list', async () => {
    vi.mocked(collaborationApi.listCollaborationNotifications).mockResolvedValue([sampleItem])

    const store = useCollaborationNotificationsStore()
    await store.fetchList()

    expect(store.items).toHaveLength(1)
    expect(store.listErrorMessageKey).toBeNull()
  })

  it('records list error without clearing previous items into empty-success', async () => {
    vi.mocked(collaborationApi.listCollaborationNotifications)
      .mockResolvedValueOnce([sampleItem])
      .mockRejectedValueOnce(new Error('network'))

    const store = useCollaborationNotificationsStore()
    await store.fetchList()
    await expect(store.fetchList()).rejects.toThrow('network')

    expect(store.items).toHaveLength(1)
    expect(store.listErrorMessageKey).toBe('collaboration.notifications.error.loadList')
  })

  it('marks one notification read and updates unread count', async () => {
    vi.mocked(collaborationApi.listCollaborationNotifications).mockResolvedValue([sampleItem])
    vi.mocked(collaborationApi.markCollaborationNotificationRead).mockResolvedValue({
      unreadCount: 0,
    })

    const store = useCollaborationNotificationsStore()
    await store.fetchList()
    await store.markRead('wi-1')

    expect(collaborationApi.markCollaborationNotificationRead).toHaveBeenCalledWith('wi-1')
    expect(store.unreadCount).toBe(0)
    expect(store.items[0]?.read).toBe(true)
    expect(store.actionErrorMessageKey).toBeNull()
  })

  it('does not update state when mark-read fails', async () => {
    vi.mocked(collaborationApi.listCollaborationNotifications).mockResolvedValue([sampleItem])
    vi.mocked(collaborationApi.markCollaborationNotificationRead).mockRejectedValue(
      new Error('denied'),
    )

    const store = useCollaborationNotificationsStore()
    store.unreadCount = 1
    await store.fetchList()
    await expect(store.markRead('wi-1')).rejects.toThrow('denied')

    expect(store.unreadCount).toBe(1)
    expect(store.items[0]?.read).toBe(false)
    expect(store.actionErrorMessageKey).toBe('collaboration.notifications.error.markRead')
  })

  it('marks all notifications read', async () => {
    vi.mocked(collaborationApi.listCollaborationNotifications).mockResolvedValue([
      sampleItem,
      { ...sampleItem, workItemId: 'wi-2', read: false },
    ])
    vi.mocked(collaborationApi.markAllCollaborationNotificationsRead).mockResolvedValue({
      unreadCount: 0,
    })

    const store = useCollaborationNotificationsStore()
    await store.fetchList()
    await store.markAllRead()

    expect(collaborationApi.markAllCollaborationNotificationsRead).toHaveBeenCalled()
    expect(store.unreadCount).toBe(0)
    expect(store.items.every((item) => item.read)).toBe(true)
  })

  it('clear resets unread count, items, and error keys', () => {
    const store = useCollaborationNotificationsStore()
    store.unreadCount = 5
    store.items = [sampleItem]
    store.unreadErrorMessageKey = 'collaboration.notifications.error.loadUnread'
    store.listErrorMessageKey = 'collaboration.notifications.error.loadList'
    store.actionErrorMessageKey = 'collaboration.notifications.error.markRead'

    store.clear()

    expect(store.unreadCount).toBe(0)
    expect(store.items).toEqual([])
    expect(store.unreadErrorMessageKey).toBeNull()
    expect(store.listErrorMessageKey).toBeNull()
    expect(store.actionErrorMessageKey).toBeNull()
  })
})
