import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import NotificationBell from '@/components/layout/NotificationBell.vue'
import en from '@/i18n/locales/en'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useCollaborationNotificationsStore } from '@/stores/collaborationNotifications'
import { useSessionStore } from '@/stores/session'
import type { ManagementCapabilities } from '@/types/session'

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/api/collaboration', () => ({
  getCollaborationNotificationUnreadCount: vi.fn().mockResolvedValue({ unreadCount: 0 }),
  listCollaborationNotifications: vi.fn().mockResolvedValue([]),
  markCollaborationNotificationRead: vi.fn().mockResolvedValue({ unreadCount: 0 }),
  markAllCollaborationNotificationsRead: vi.fn().mockResolvedValue({ unreadCount: 0 }),
}))

vi.mock('@/composables/useNotificationPolling', () => ({
  NOTIFICATION_POLL_INTERVAL_MS: 30_000,
  useNotificationPolling: vi.fn(() => ({
    stop: vi.fn(),
    refreshNow: vi.fn().mockResolvedValue(undefined),
  })),
}))

const withCollab: ManagementCapabilities = {
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
  manageAssetLibrary: false,
}

const withoutCollab: ManagementCapabilities = {
  ...withCollab,
  viewCollaborationWorkItems: false,
  decideTests: false,
}

function mountBell(capabilities: ManagementCapabilities) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000003',
      displayName: 'Tester',
      email: 'tester@example.com',
      authSource: 'LOCAL',
      roles: capabilities.viewCollaborationWorkItems ? ['TEMPLATE_TESTER'] : ['AUDIT_ADMIN'],
      authorizedGroupCodes: ['RETAIL'],
      defaultRoute: ROUTE_KEYS.dashboardHome,
      visibleRoutes: [ROUTE_KEYS.dashboardHome],
      capabilities,
      expiresAt: new Date(Date.now() + 30 * 60_000).toISOString(),
    },
  })

  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(NotificationBell, {
    attachTo: document.body,
    global: { plugins: [pinia, i18n, ElementPlus] },
  })
}

describe('NotificationBell', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('hides the bell when collaboration capability is absent (fail-closed)', () => {
    const wrapper = mountBell(withoutCollab)
    expect(wrapper.find('[data-testid="notification-bell"]').exists()).toBe(false)
  })

  it('shows the bell and unread badge when unreadCount > 0', async () => {
    const wrapper = mountBell(withCollab)
    const store = useCollaborationNotificationsStore()
    store.unreadCount = 2
    await flushPromises()

    expect(wrapper.find('[data-testid="notification-bell"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="notification-badge"]').text()).toContain('2')
  })

  it('hides badge when unreadCount is 0 but keeps the bell', async () => {
    const wrapper = mountBell(withCollab)
    const store = useCollaborationNotificationsStore()
    store.unreadCount = 0
    await flushPromises()

    expect(wrapper.find('[data-testid="notification-bell"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="notification-badge"]').exists()).toBe(false)
  })

  it('caps badge at 99+', async () => {
    const wrapper = mountBell(withCollab)
    const store = useCollaborationNotificationsStore()
    store.unreadCount = 120
    await flushPromises()

    expect(wrapper.find('[data-testid="notification-badge"]').text()).toContain('99+')
  })

  it('loads list on open without marking read', async () => {
    const wrapper = mountBell(withCollab)
    const store = useCollaborationNotificationsStore()
    store.unreadCount = 1
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
    const fetchList = vi.spyOn(store, 'fetchList').mockResolvedValue()
    const markRead = vi.spyOn(store, 'markRead').mockResolvedValue()

    await wrapper.find('[data-testid="notification-bell"]').trigger('click')
    await flushPromises()

    expect(fetchList).toHaveBeenCalled()
    expect(markRead).not.toHaveBeenCalled()
    expect(document.body.querySelector('[data-testid="notification-dropdown"]')).toBeTruthy()
  })

  it('hides mark-all when unreadCount is 0', async () => {
    const wrapper = mountBell(withCollab)
    const store = useCollaborationNotificationsStore()
    store.unreadCount = 0
    store.items = []
    vi.spyOn(store, 'fetchList').mockResolvedValue()

    await wrapper.find('[data-testid="notification-bell"]').trigger('click')
    await flushPromises()

    expect(document.body.querySelector('[data-testid="notification-mark-all"]')).toBeNull()
  })

  it('marks read and deep-links on item click', async () => {
    const wrapper = mountBell(withCollab)
    const store = useCollaborationNotificationsStore()
    store.unreadCount = 1
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
    vi.spyOn(store, 'fetchList').mockResolvedValue()
    const markRead = vi.spyOn(store, 'markRead').mockResolvedValue()

    await wrapper.find('[data-testid="notification-bell"]').trigger('click')
    await flushPromises()

    const item = document.body.querySelector('[data-testid="notification-item"]') as HTMLElement
    expect(item).toBeTruthy()
    item.click()
    await flushPromises()

    expect(markRead).toHaveBeenCalledWith('wi-1')
    expect(routerPush).toHaveBeenCalledWith({
      path: '/dashboard',
      query: { queue: 'TEST' },
      hash: '#tasks-section',
    })
  })

  it('does not navigate when mark-read fails', async () => {
    const wrapper = mountBell(withCollab)
    const store = useCollaborationNotificationsStore()
    store.unreadCount = 1
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
    vi.spyOn(store, 'fetchList').mockResolvedValue()
    vi.spyOn(store, 'markRead').mockRejectedValue(new Error('denied'))

    await wrapper.find('[data-testid="notification-bell"]').trigger('click')
    await flushPromises()

    const item = document.body.querySelector('[data-testid="notification-item"]') as HTMLElement
    item.click()
    await flushPromises()

    expect(routerPush).not.toHaveBeenCalled()
  })

  it('shows empty state when list is empty and no list error', async () => {
    const wrapper = mountBell(withCollab)
    const store = useCollaborationNotificationsStore()
    store.unreadCount = 0
    store.items = []
    store.listErrorMessageKey = null
    vi.spyOn(store, 'fetchList').mockResolvedValue()

    await wrapper.find('[data-testid="notification-bell"]').trigger('click')
    await flushPromises()

    const dropdown = document.body.querySelector('[data-testid="notification-dropdown"]')
    expect(dropdown?.textContent).toContain('No notifications')
  })

  it('shows list error instead of empty success on failure', async () => {
    const wrapper = mountBell(withCollab)
    const store = useCollaborationNotificationsStore()
    store.unreadCount = 1
    store.items = []
    store.listErrorMessageKey = 'collaboration.notifications.error.loadList'
    vi.spyOn(store, 'fetchList').mockResolvedValue()

    await wrapper.find('[data-testid="notification-bell"]').trigger('click')
    await flushPromises()

    const dropdown = document.body.querySelector('[data-testid="notification-dropdown"]')
    expect(dropdown?.textContent).toContain('Unable to load notifications')
    expect(dropdown?.textContent).not.toContain('No notifications')
  })
})
