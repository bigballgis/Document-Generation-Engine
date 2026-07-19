import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as templatesApi from '@/api/templates'
import { useTemplatesStore } from '@/stores/templates'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

vi.mock('@/api/templates', () => ({
  listTemplates: vi.fn(),
  listAllTemplates: vi.fn(),
  listTemplateVersionLines: vi.fn(),
  getTemplate: vi.fn(),
  submitForTest: vi.fn(),
  recordTestDecision: vi.fn(),
  submitForApproval: vi.fn(),
  recordApprovalDecision: vi.fn(),
  publishTemplate: vi.fn(),
  testGenerate: vi.fn(),
  getPreview: vi.fn(),
  createTemplate: vi.fn(),
}))

describe('templates store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(templatesApi.listTemplates).mockReset()
    vi.mocked(templatesApi.listAllTemplates).mockReset()
    vi.mocked(templatesApi.listTemplateVersionLines).mockReset()
    vi.mocked(templatesApi.getTemplate).mockReset()
    vi.mocked(templatesApi.createTemplate).mockReset()
    vi.mocked(templatesApi.publishTemplate).mockReset()
  })

  it('loads templates from paginated API', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'TPL-A',
          groupCode: 'RETAIL',
          name: 'Draft template',
          lifecycleStatus: 'DRAFT',
          releaseVersion: null,
          releaseVersionCount: 0,
          masterId: 'master-1',
          updatedBy: '10000001',
          updatedAt: '2026-06-23T10:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    const store = useTemplatesStore()

    await store.fetchTemplates()

    expect(store.templates).toHaveLength(1)
    expect(store.templateListTotalElements).toBe(1)
  })

  it('fetchAllTemplates merges every page into the store list', async () => {
    vi.mocked(templatesApi.listAllTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'TPL-A',
          groupCode: 'RETAIL',
          name: 'Draft template',
          lifecycleStatus: 'DRAFT',
          releaseVersion: null,
          releaseVersionCount: 0,
          masterId: 'master-1',
          updatedBy: '10000001',
          updatedAt: '2026-06-23T10:00:00Z',
        },
        {
          id: 'tpl-2',
          externalId: 'TPL-B',
          groupCode: 'CORPORATE',
          name: 'Second template',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'master-2',
          updatedBy: '10000002',
          updatedAt: '2026-06-24T10:00:00Z',
        },
      ],
      totalElements: 125,
      truncated: false,
    })

    const store = useTemplatesStore()
    await store.fetchAllTemplates({ sort: 'groupCodeAsc' })

    expect(templatesApi.listAllTemplates).toHaveBeenCalledWith({ sort: 'groupCodeAsc' })
    expect(store.templates).toHaveLength(2)
    expect(store.templateListTotalElements).toBe(125)
  })

  it('records list retryable flag on template load failure', async () => {
    vi.mocked(templatesApi.listTemplates).mockRejectedValue(
      axiosEnvelopeError(503, 'api.error.generation.serviceUnavailable', {
        retryable: true,
      }),
    )
    const store = useTemplatesStore()

    await expect(store.fetchTemplates()).rejects.toBeTruthy()
    expect(store.lastListErrorRetryable).toBe(true)
  })

  it('filters published templates for API policy home', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [
      {
        id: 'tpl-1',
        externalId: 'TPL-A',
        groupCode: 'RETAIL',
        name: 'Draft template',
        lifecycleStatus: 'DRAFT',
        releaseVersion: null,
        releaseVersionCount: 0,
        masterId: 'master-1',
        updatedBy: '10000001',
        updatedAt: '2026-06-23T10:00:00Z',
      },
      {
        id: 'tpl-2',
        externalId: 'TPL-B',
        groupCode: 'RETAIL',
        name: 'Published template',
        lifecycleStatus: 'PUBLISHED',
        releaseVersion: '1.0.0',
        releaseVersionCount: 1,
        masterId: 'master-2',
        updatedBy: '10000001',
        updatedAt: '2026-06-23T11:00:00Z',
      },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    })

    const store = useTemplatesStore()
    await store.fetchTemplates()

    expect(store.publishedTemplates).toHaveLength(1)
    expect(store.publishedTemplates[0]?.name).toBe('Published template')
  })

  it('stores api error message key when create fails with envelope', async () => {
    vi.mocked(templatesApi.createTemplate).mockRejectedValue(
      axiosEnvelopeError(
        422,
        'api.error.template.externalIdExists',
        {
          code: 'TEMPLATE_VALIDATION_FAILED',
          category: 'TEMPLATE',
          message: 'External ID already exists.',
        },
      ),
    )

    const store = useTemplatesStore()
    await expect(
      store.createTemplate({
        groupCode: 'RETAIL',
        masterId: 'master-1',
        externalId: 'TPL-DUP',
        name: 'Duplicate',
        locale: 'zh-CN',
      }),
    ).rejects.toBeTruthy()

    expect(store.lastErrorMessageKey).toBe('api.error.template.externalIdExists')
  })

  it('falls back to templates.error.create when create fails without envelope', async () => {
    vi.mocked(templatesApi.createTemplate).mockRejectedValue(new Error('network'))

    const store = useTemplatesStore()
    await expect(
      store.createTemplate({
        groupCode: 'RETAIL',
        masterId: 'master-1',
        externalId: 'TPL-NEW',
        name: 'New template',
        locale: 'zh-CN',
      }),
    ).rejects.toBeTruthy()

    expect(store.lastErrorMessageKey).toBe('templates.error.create')
  })

  it('stores api error message key when publish fails with publish gate envelope', async () => {
    vi.mocked(templatesApi.publishTemplate).mockRejectedValue(
      axiosEnvelopeError(422, 'api.error.template.publishGateBlocked', {
        code: 'TEMPLATE_VALIDATION_FAILED',
        category: 'TEMPLATE',
        message: 'Publish gate blocked.',
      }),
    )

    const store = useTemplatesStore()
    await expect(store.publishTemplate('tpl-1', { releaseVersion: '1.0.0' })).rejects.toBeTruthy()

    expect(store.lastErrorMessageKey).toBe('api.error.template.publishGateBlocked')
  })

  it('falls back to templates.error.lifecycle when publish fails without envelope', async () => {
    vi.mocked(templatesApi.publishTemplate).mockRejectedValue(new Error('network'))

    const store = useTemplatesStore()
    await expect(store.publishTemplate('tpl-1', { releaseVersion: '1.0.0' })).rejects.toBeTruthy()

    expect(store.lastErrorMessageKey).toBe('templates.error.lifecycle')
  })

  it('BDD-LRP-C2-002 soft-refresh fetchTemplate does not flip loadingDetail for selected template', async () => {
    const detail = {
      id: 'tpl-1',
      externalId: 'TPL-A',
      groupCode: 'RETAIL',
      name: 'Draft template',
      description: null,
      lifecycleStatus: 'DRAFT',
      releaseVersion: null,
      masterId: 'master-1',
      masterRevisionId: null,
      variables: [],
      bindings: [],
      rules: [],
      createdBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedBy: '10000001',
      updatedAt: '2026-06-23T10:00:00Z',
      devVersionId: null,
      devVersionNumber: null,
      contentModuleReferences: [],
    } as unknown as Awaited<ReturnType<typeof templatesApi.getTemplate>>

    vi.mocked(templatesApi.getTemplate).mockImplementation(async () => {
      // Soft refresh must not expose loadingDetail mid-flight (workspace teardown).
      expect(useTemplatesStore().loadingDetail).toBe(false)
      return { ...detail, name: 'Draft template refreshed' }
    })

    const store = useTemplatesStore()
    store.$patch({ selectedTemplate: detail, loadingDetail: false })

    await store.fetchTemplate('tpl-1')

    expect(store.loadingDetail).toBe(false)
    expect(store.selectedTemplate?.name).toBe('Draft template refreshed')
    expect(templatesApi.getTemplate).toHaveBeenCalledWith('tpl-1')
  })

  it('fetchTemplate sets loadingDetail for cold load of a different template', async () => {
    let sawLoading = false
    vi.mocked(templatesApi.getTemplate).mockImplementation(async () => {
      sawLoading = useTemplatesStore().loadingDetail
      return {
        id: 'tpl-2',
        externalId: 'TPL-B',
        groupCode: 'RETAIL',
        name: 'Other',
        description: null,
        lifecycleStatus: 'DRAFT',
        releaseVersion: null,
        masterId: 'master-2',
        masterRevisionId: null,
        variables: [],
        bindings: [],
        rules: [],
        createdBy: '10000001',
        createdAt: '2026-06-23T10:00:00Z',
        updatedBy: '10000001',
        updatedAt: '2026-06-23T10:00:00Z',
        devVersionId: null,
        devVersionNumber: null,
        contentModuleReferences: [],
      } as unknown as Awaited<ReturnType<typeof templatesApi.getTemplate>>
    })

    const store = useTemplatesStore()
    store.$patch({ selectedTemplate: null, loadingDetail: false })

    await store.fetchTemplate('tpl-2')

    expect(sawLoading).toBe(true)
    expect(store.loadingDetail).toBe(false)
  })

  it('forwards approvalSubState when fetching templates', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
    const store = useTemplatesStore()

    await store.fetchTemplates(0, 20, {
      lifecycleStatus: 'APPROVAL',
      approvalSubState: 'PENDING_DECISION',
      sort: 'groupCodeAsc',
    })

    expect(templatesApi.listTemplates).toHaveBeenCalledWith(0, 20, {
      lifecycleStatus: 'APPROVAL',
      approvalSubState: 'PENDING_DECISION',
      sort: 'groupCodeAsc',
    })
  })

  it('enriches in-flight devVersionId for collaboration deep links (CE-U14)', async () => {
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [
        {
          devVersionId: 'dev-in-flight',
          devVersionNumber: 2,
          releaseVersion: null,
          lifecycleStatus: 'TESTING',
          lineKind: 'IN_FLIGHT',
          updatedAt: '2026-06-26T10:00:00Z',
          updatedBy: '10000003',
          defaultRouteTarget: null,
          cloneable: false,
        },
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    })

    const store = useTemplatesStore()
    await store.enrichDevVersionIdsForWorkflow(['tpl-1', 'tpl-1'])

    expect(templatesApi.listTemplateVersionLines).toHaveBeenCalledTimes(1)
    expect(store.devVersionIdByTemplateId['tpl-1']).toBe('dev-in-flight')
  })

  it('does not enrich when no IN_FLIGHT line (Hub + queue-aware path)', async () => {
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [
        {
          devVersionId: 'dev-published',
          devVersionNumber: 1,
          releaseVersion: '1.0.0',
          lifecycleStatus: 'PUBLISHED',
          lineKind: 'PUBLISHED',
          updatedAt: '2026-06-20T10:00:00Z',
          updatedBy: '10000003',
          defaultRouteTarget: null,
          cloneable: true,
        },
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    })

    const store = useTemplatesStore()
    await store.enrichDevVersionIdsForWorkflow(['tpl-published-only'])

    expect(templatesApi.listTemplateVersionLines).toHaveBeenCalledWith(
      'tpl-published-only',
      0,
      5,
    )
    expect(store.devVersionIdByTemplateId['tpl-published-only']).toBeUndefined()
    expect(store.devVersionIdByTemplateId).toEqual({})
  })
})
