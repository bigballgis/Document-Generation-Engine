import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as contentModulesApi from '@/api/contentModules'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

describe('contentModules API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
    vi.mocked(http.post).mockReset()
    vi.mocked(http.put).mockReset()
  })

  it('lists content modules as PageView with page/size and groupCode', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          content: [
            {
              moduleId: 'MOD-LOAN-DISCLOSURE',
              moduleCode: 'MOD-LOAN-DISCLOSURE',
              groupCode: 'RETAIL',
              name: 'Loan disclosure',
              createdAt: '2026-06-26T10:00:00Z',
              updatedAt: '2026-06-26T10:00:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
      },
    })

    const pageView = await contentModulesApi.listContentModules(0, 20, { groupCode: 'RETAIL' })

    expect(http.get).toHaveBeenCalledWith('/content-modules', {
      params: { page: 0, size: 20, groupCode: 'RETAIL' },
      signal: undefined,
    })
    expect(pageView.content).toHaveLength(1)
    expect(pageView.content[0]?.name).toBe('Loan disclosure')
  })

  it('forwards search and sort query params', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        },
      },
    })

    await contentModulesApi.listContentModules(0, 20, {
      search: 'loan',
      sort: 'moduleCodeAsc',
    })

    expect(http.get).toHaveBeenCalledWith('/content-modules', {
      params: { page: 0, size: 20, search: 'loan', sort: 'moduleCodeAsc' },
      signal: undefined,
    })
  })

  it('creates a content module with initial draft version', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          moduleId: 'MOD-LOAN-DISCLOSURE',
          moduleCode: 'MOD-LOAN-DISCLOSURE',
          groupCode: 'RETAIL',
          name: 'Loan disclosure',
          versions: [
            {
              versionId: 'v1',
              semanticVersion: '1.0.0',
              reviewState: 'DRAFT',
              createdAt: '2026-06-26T10:00:00Z',
              updatedAt: '2026-06-26T10:00:00Z',
            },
          ],
        },
      },
    })

    const created = await contentModulesApi.createContentModule({
      moduleCode: 'MOD-LOAN-DISCLOSURE',
      groupCode: 'RETAIL',
      name: 'Loan disclosure',
      semanticVersion: '1.0.0',
      contentStructureJson: '{"blocks":[]}',
    })

    expect(http.post).toHaveBeenCalledWith('/content-modules', expect.objectContaining({
      moduleCode: 'MOD-LOAN-DISCLOSURE',
    }))
    expect(created.versions[0]?.reviewState).toBe('DRAFT')
  })

  it('updates shared group codes via PUT shared-group-codes', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          moduleId: 'MOD-LOAN-DISCLOSURE',
          moduleCode: 'MOD-LOAN-DISCLOSURE',
          groupCode: 'HQ',
          name: 'Loan disclosure',
          sharedGroupCodes: ['RETAIL', 'WEALTH'],
          versions: [],
          reviewHistory: [],
        },
      },
    })

    const updated = await contentModulesApi.updateContentModuleSharedGroupCodes(
      'MOD-LOAN-DISCLOSURE',
      { sharedGroupCodes: ['RETAIL', 'WEALTH'] },
    )

    expect(http.put).toHaveBeenCalledWith(
      '/content-modules/MOD-LOAN-DISCLOSURE/shared-group-codes',
      { sharedGroupCodes: ['RETAIL', 'WEALTH'] },
    )
    expect(updated.sharedGroupCodes).toEqual(['RETAIL', 'WEALTH'])
  })

  it('fetches lifecycle impact preview', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          referenceTemplateCount: 2,
          referenceTemplateListHint: 'TPL-001, TPL-002',
          impactedReleaseVersionsHint: '2026.06.1',
          defaultRouteAffected: true,
          recentCallSummary: '12 calls in 7 days',
          remediationHint: 'Review dependent templates before stopping.',
        },
      },
    })

    const preview = await contentModulesApi.previewContentModuleLifecycleImpact('MOD-LOAN-DISCLOSURE')

    expect(http.get).toHaveBeenCalledWith(
      '/content-modules/MOD-LOAN-DISCLOSURE/lifecycle/impact/preview',
    )
    expect(preview.referenceTemplateCount).toBe(2)
  })

  it('applies lifecycle operation with impact confirmation flags', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          applied: true,
          snapshot: {
            moduleId: 'MOD-LOAN-DISCLOSURE',
            state: 'STOPPED',
            updatedAt: '2026-06-26T11:00:00Z',
            updatedBy: '10000002',
          },
        },
      },
    })

    const result = await contentModulesApi.applyContentModuleLifecycleOperation('MOD-LOAN-DISCLOSURE', {
      operationType: 'STOP_USE',
      actorRole: 'GROUP_ADMIN',
      actorId: '10000002',
      impactSummaryViewed: true,
      secondConfirmation: true,
      impactSummary: {
        referenceTemplateCount: 1,
        referenceTemplateListHint: 'TPL-001',
        impactedReleaseVersionsHint: '2026.06.1',
        defaultRouteAffected: false,
        recentCallSummary: 'none',
        remediationHint: 'none',
        templateStopRequired: false,
        releaseStopRequired: false,
      },
    })

    expect(http.post).toHaveBeenCalledWith(
      '/content-modules/MOD-LOAN-DISCLOSURE/lifecycle/operation/apply',
      expect.objectContaining({ operationType: 'STOP_USE', secondConfirmation: true }),
    )
    expect(result.snapshot?.state).toBe('STOPPED')
  })
})
