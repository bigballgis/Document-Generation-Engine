import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as templatesApi from '@/api/templates'
import { useTemplatesStore } from '@/stores/templates'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

vi.mock('@/api/templates', () => ({
  listTemplates: vi.fn(),
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
  })

  it('filters published templates for API policy home', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue([
      {
        id: 'tpl-1',
        externalId: 'TPL-A',
        groupCode: 'RETAIL',
        name: 'Draft template',
        lifecycleStatus: 'DRAFT',
        releaseVersion: null,
        masterId: 'master-1',
        updatedAt: '2026-06-23T10:00:00Z',
      },
      {
        id: 'tpl-2',
        externalId: 'TPL-B',
        groupCode: 'RETAIL',
        name: 'Published template',
        lifecycleStatus: 'PUBLISHED',
        releaseVersion: '1.0.0',
        masterId: 'master-2',
        updatedAt: '2026-06-23T11:00:00Z',
      },
    ])

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
})
