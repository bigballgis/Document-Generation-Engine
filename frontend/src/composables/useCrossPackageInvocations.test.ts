import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as apiPolicyApi from '@/api/apiPolicy'
import * as templatesApi from '@/api/templates'
import { CROSS_PACKAGE_SAMPLE_CAP } from '@/composables/externalServicesOpsCompose'
import { useCrossPackageInvocations } from '@/composables/useCrossPackageInvocations'

vi.mock('@/api/apiPolicy', () => ({
  listInvocations: vi.fn(),
}))

vi.mock('@/api/templates', () => ({
  listAllTemplates: vi.fn(),
  listTemplates: vi.fn(),
}))

describe('useCrossPackageInvocations (BDD-SYS-NORM-W3-005)', () => {
  beforeEach(() => {
    vi.mocked(apiPolicyApi.listInvocations).mockReset()
    vi.mocked(templatesApi.listAllTemplates).mockReset()
    vi.mocked(templatesApi.listTemplates).mockReset()
  })

  it('composes invocations across authorized packages', async () => {
    vi.mocked(templatesApi.listAllTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'P1',
          groupCode: 'RETAIL',
          name: 'Package One',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'm1',
          updatedBy: 'admin',
          updatedAt: '2026-07-20T00:00:00Z',
        },
        {
          id: 'tpl-2',
          externalId: 'P2',
          groupCode: 'RETAIL',
          name: 'Package Two',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'm2',
          updatedBy: 'admin',
          updatedAt: '2026-07-20T00:00:00Z',
        },
      ],
      totalElements: 2,
      truncated: false,
    })
    vi.mocked(apiPolicyApi.listInvocations).mockImplementation(async (templateId) => ({
      content: [
        {
          invocationId: `inv-${templateId}`,
          invocationKind: 'SINGLE',
          status: templateId === 'tpl-2' ? 'FAILED' : 'SUCCEEDED',
          requestId: `req-${templateId}`,
          resolvedReleaseVersion: '1.0.0',
          routeType: 'DEFAULT',
          createdAt:
            templateId === 'tpl-2' ? '2026-07-21T00:00:00Z' : '2026-07-20T00:00:00Z',
          accessAccountSummary: 'acct',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }))

    const api = useCrossPackageInvocations({ autoLoad: false })
    await api.loadInvocations()
    await flushPromises()

    expect(api.rows.value).toHaveLength(2)
    expect(api.rows.value[0]?.templateId).toBe('tpl-2')
    expect(api.opsSummary.value.failedCount).toBe(1)
    expect(api.opsSummary.value.sampledInvocationCount).toBe(2)
    expect(api.compositionLimited.value).toBe(false)
  })

  it('marks composition as limited when package sample is capped', async () => {
    const packages = Array.from({ length: CROSS_PACKAGE_SAMPLE_CAP + 2 }, (_, i) => ({
      id: `tpl-${i}`,
      externalId: `P${i}`,
      groupCode: 'RETAIL',
      name: `Package ${i}`,
      lifecycleStatus: 'PUBLISHED' as const,
      releaseVersion: '1.0.0',
      releaseVersionCount: 1,
      masterId: `m${i}`,
      updatedBy: 'admin',
      updatedAt: '2026-07-20T00:00:00Z',
    }))
    vi.mocked(templatesApi.listAllTemplates).mockResolvedValue({
      content: packages,
      totalElements: packages.length,
      truncated: false,
    })
    vi.mocked(apiPolicyApi.listInvocations).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    const api = useCrossPackageInvocations({ autoLoad: false })
    await api.loadInvocations()
    await flushPromises()

    expect(api.compositionLimited.value).toBe(true)
    expect(apiPolicyApi.listInvocations).toHaveBeenCalledTimes(CROSS_PACKAGE_SAMPLE_CAP)
  })

  it('uses server pagination when a package filter is selected', async () => {
    vi.mocked(templatesApi.listAllTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'P1',
          groupCode: 'RETAIL',
          name: 'Package One',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'm1',
          updatedBy: 'admin',
          updatedAt: '2026-07-20T00:00:00Z',
        },
      ],
      totalElements: 1,
      truncated: false,
    })
    vi.mocked(apiPolicyApi.listInvocations).mockResolvedValue({
      content: [
        {
          invocationId: 'inv-1',
          invocationKind: 'SINGLE',
          status: 'SUCCEEDED',
          requestId: 'req-1',
          resolvedReleaseVersion: '1.0.0',
          routeType: 'DEFAULT',
          createdAt: '2026-07-20T00:00:00Z',
          accessAccountSummary: 'acct',
        },
      ],
      page: 1,
      size: 20,
      totalElements: 45,
      totalPages: 3,
    })

    const api = useCrossPackageInvocations({ autoLoad: false })
    api.seedTemplateFilter('tpl-1')
    api.currentPage.value = 2
    await api.loadInvocations()
    await flushPromises()

    expect(apiPolicyApi.listInvocations).toHaveBeenCalledWith(
      'tpl-1',
      1,
      20,
      expect.any(Object),
    )
    expect(api.totalElements.value).toBe(45)
    expect(api.compositionLimited.value).toBe(false)
  })

  it('surfaces load failure for retry (BDD-SYS-NORM-W3-007)', async () => {
    vi.mocked(templatesApi.listAllTemplates).mockRejectedValue(new Error('boom'))
    const api = useCrossPackageInvocations({ autoLoad: false })
    await api.loadInvocations()
    await flushPromises()
    expect(api.loadFailed.value).toBe(true)
    expect(api.rows.value).toHaveLength(0)
  })

  it('sets loadFailed when catalog succeeds but every listInvocations fails (BDD-SYS-NORM-W3-007)', async () => {
    vi.mocked(templatesApi.listAllTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'P1',
          groupCode: 'RETAIL',
          name: 'Package One',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'm1',
          updatedBy: 'admin',
          updatedAt: '2026-07-20T00:00:00Z',
        },
        {
          id: 'tpl-2',
          externalId: 'P2',
          groupCode: 'RETAIL',
          name: 'Package Two',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'm2',
          updatedBy: 'admin',
          updatedAt: '2026-07-20T00:00:00Z',
        },
      ],
      totalElements: 2,
      truncated: false,
    })
    vi.mocked(apiPolicyApi.listInvocations).mockRejectedValue(new Error('inv boom'))

    const api = useCrossPackageInvocations({ autoLoad: false })
    await api.loadInvocations()
    await flushPromises()

    expect(api.loadFailed.value).toBe(true)
    expect(api.rows.value).toHaveLength(0)
    expect(api.opsSummary.value.sampledInvocationCount).toBe(0)
  })

  it('keeps partial composition results when some listInvocations fail', async () => {
    vi.mocked(templatesApi.listAllTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'P1',
          groupCode: 'RETAIL',
          name: 'Package One',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'm1',
          updatedBy: 'admin',
          updatedAt: '2026-07-20T00:00:00Z',
        },
        {
          id: 'tpl-2',
          externalId: 'P2',
          groupCode: 'RETAIL',
          name: 'Package Two',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'm2',
          updatedBy: 'admin',
          updatedAt: '2026-07-20T00:00:00Z',
        },
      ],
      totalElements: 2,
      truncated: false,
    })
    vi.mocked(apiPolicyApi.listInvocations).mockImplementation(async (templateId) => {
      if (templateId === 'tpl-2') {
        throw new Error('inv boom')
      }
      return {
        content: [
          {
            invocationId: 'inv-tpl-1',
            invocationKind: 'SINGLE',
            status: 'SUCCEEDED',
            requestId: 'req-tpl-1',
            resolvedReleaseVersion: '1.0.0',
            routeType: 'DEFAULT',
            createdAt: '2026-07-20T00:00:00Z',
            accessAccountSummary: 'acct',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }
    })

    const api = useCrossPackageInvocations({ autoLoad: false })
    await api.loadInvocations()
    await flushPromises()

    expect(api.loadFailed.value).toBe(false)
    expect(api.rows.value).toHaveLength(1)
    expect(api.rows.value[0]?.templateId).toBe('tpl-1')
  })

  it('sets loadFailed on ops summary when every listInvocations fails (BDD-SYS-NORM-W3-003)', async () => {
    vi.mocked(templatesApi.listAllTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'P1',
          groupCode: 'RETAIL',
          name: 'Package One',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'm1',
          updatedBy: 'admin',
          updatedAt: '2026-07-20T00:00:00Z',
        },
      ],
      totalElements: 1,
      truncated: false,
    })
    vi.mocked(apiPolicyApi.listInvocations).mockRejectedValue(new Error('inv boom'))

    const api = useCrossPackageInvocations({ autoLoad: false })
    await api.loadOpsSummaryOnly()
    await flushPromises()

    expect(api.loadFailed.value).toBe(true)
    expect(api.opsSummary.value.sampledInvocationCount).toBe(0)
  })
})
