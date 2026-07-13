import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as collaborationApi from '@/api/collaboration'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
  },
}))

describe('collaboration API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
    vi.mocked(http.put).mockReset()
    vi.mocked(http.post).mockReset()
  })

  it('lists collaboration work items with optional filters', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: [
          {
            workItemId: 'wi-1',
            templateId: 'tpl-1',
            templateName: 'Loan Notice',
            groupCode: 'RETAIL',
            queue: 'TEST',
            triggerType: 'SUBMIT_FOR_TEST',
            submitterUserId: '10000003',
            summaryText: 'Template submitted for testing',
            createdAt: '2026-06-26T10:00:00Z',
            ageSeconds: 3600,
          },
        ],
      },
    })

    const items = await collaborationApi.listCollaborationWorkItems({
      groupCode: 'RETAIL',
      queue: 'TEST',
    })

    expect(http.get).toHaveBeenCalledWith('/collaboration-work-items', {
      params: { groupCode: 'RETAIL', queue: 'TEST' },
    })
    expect(items).toHaveLength(1)
    expect(items[0]?.templateName).toBe('Loan Notice')
  })

  it('loads global timeout configuration', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          scopeType: 'GLOBAL',
          groupCode: null,
          testThresholdHours: 72,
          approvalThresholdHours: 72,
          pendingReleaseThresholdHours: 48,
          remediationThresholdHours: 168,
          updatedAt: '2026-06-26T10:00:00Z',
        },
      },
    })

    const config = await collaborationApi.getCollaborationTimeoutConfig()

    expect(http.get).toHaveBeenCalledWith('/collaboration-timeout-config', { params: undefined })
    expect(config.scopeType).toBe('GLOBAL')
    expect(config.testThresholdHours).toBe(72)
  })

  it('upserts group timeout override', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          scopeType: 'GROUP',
          groupCode: 'RETAIL',
          testThresholdHours: 24,
          approvalThresholdHours: 24,
          pendingReleaseThresholdHours: 12,
          remediationThresholdHours: 96,
          updatedAt: '2026-06-26T11:00:00Z',
        },
      },
    })

    const saved = await collaborationApi.upsertCollaborationTimeoutConfig({
      scopeType: 'GROUP',
      groupCode: 'RETAIL',
      testThresholdHours: 24,
      approvalThresholdHours: 24,
      pendingReleaseThresholdHours: 12,
      remediationThresholdHours: 96,
    })

    expect(http.put).toHaveBeenCalledWith('/collaboration-timeout-config', expect.objectContaining({
      scopeType: 'GROUP',
      groupCode: 'RETAIL',
    }))
    expect(saved.groupCode).toBe('RETAIL')
    expect(saved.testThresholdHours).toBe(24)
  })

  it('fetches collaboration notification unread count', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: { unreadCount: 3 },
      },
    })

    const result = await collaborationApi.getCollaborationNotificationUnreadCount()

    expect(http.get).toHaveBeenCalledWith('/collaboration-notifications/unread-count')
    expect(result.unreadCount).toBe(3)
  })

  it('lists collaboration notifications', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: [
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
        ],
      },
    })

    const items = await collaborationApi.listCollaborationNotifications()

    expect(http.get).toHaveBeenCalledWith('/collaboration-notifications')
    expect(items).toHaveLength(1)
    expect(items[0]?.workItemId).toBe('wi-1')
    expect(items[0]?.read).toBe(false)
  })

  it('marks one collaboration notification read', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: { unreadCount: 1 },
      },
    })

    const result = await collaborationApi.markCollaborationNotificationRead('wi-1')

    expect(http.post).toHaveBeenCalledWith('/collaboration-notifications/wi-1/read')
    expect(result.unreadCount).toBe(1)
  })

  it('marks all collaboration notifications read', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: { unreadCount: 0 },
      },
    })

    const result = await collaborationApi.markAllCollaborationNotificationsRead()

    expect(http.post).toHaveBeenCalledWith('/collaboration-notifications/read-all')
    expect(result.unreadCount).toBe(0)
  })
})
