import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as auditApi from '@/api/audit'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
  },
}))

describe('audit API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
  })

  it('lists management audit events with query params', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          events: [
            {
              eventAt: '2026-06-23T10:00:00Z',
              eventType: 'API_POLICY_UPDATED',
              changedAreas: ['POLICY'],
              rollback: false,
              warningCodes: [],
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
      },
    })

    const page = await auditApi.listManagementEvents({
      actorRole: 'GLOBAL_ADMIN',
      eventType: 'API_POLICY_UPDATED',
    })

    expect(http.get).toHaveBeenCalledWith('/admin/audit/management-events', {
      params: {
        actorRole: 'GLOBAL_ADMIN',
        eventType: 'API_POLICY_UPDATED',
        page: 0,
        size: 20,
      },
    })
    expect(page.events).toHaveLength(1)
    expect(page.totalElements).toBe(1)
  })

  it('exports management audit events', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          format: 'management-audit-export-v1-json',
          events: [],
        },
      },
    })

    const result = await auditApi.exportManagementEvents({ actorRole: 'AUDIT_ADMIN' })

    expect(http.get).toHaveBeenCalledWith('/admin/audit/management-events/export', {
      params: { actorRole: 'AUDIT_ADMIN' },
    })
    expect(result.format).toBe('management-audit-export-v1-json')
  })

  it('lists lifecycle audit events', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          events: [
            {
              eventAt: '2026-06-23T11:00:00Z',
              eventType: 'LIFECYCLE_TRANSITION',
              warningCodes: [],
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
      },
    })

    const page = await auditApi.listLifecycleEvents({
      actorRole: 'GROUP_ADMIN',
      groupScope: 'RETAIL',
      templateId: 'tpl-1',
    })

    expect(http.get).toHaveBeenCalledWith('/admin/audit/lifecycle-events', {
      params: {
        actorRole: 'GROUP_ADMIN',
        groupScope: 'RETAIL',
        templateId: 'tpl-1',
        page: 0,
        size: 20,
      },
    })
    expect(page.events).toHaveLength(1)
  })
})
