import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as templatesApi from '@/api/templates'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

describe('templates API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
    vi.mocked(http.post).mockReset()
    vi.mocked(http.put).mockReset()
  })

  it('lists templates from the management endpoint', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: [
          {
            id: 'tpl-1',
            externalId: 'TPL-RETAIL-LETTER',
            groupCode: 'RETAIL',
            name: 'Retail letter',
            lifecycleStatus: 'DRAFT',
            releaseVersion: null,
            masterId: 'master-1',
            updatedAt: '2026-06-23T10:00:00Z',
          },
        ],
      },
    })

    const templates = await templatesApi.listTemplates()

    expect(http.get).toHaveBeenCalledWith('/templates')
    expect(templates[0]?.externalId).toBe('TPL-RETAIL-LETTER')
  })

  it('submits template for test', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          id: 'tpl-1',
          externalId: 'TPL-RETAIL-LETTER',
          groupCode: 'RETAIL',
          name: 'Retail letter',
          description: null,
          masterId: 'master-1',
          lifecycleStatus: 'TESTING',
          releaseVersion: null,
          devVersionId: 'ver-1',
          devVersionNumber: 1,
          variables: [],
          bindings: [],
          rules: [],
          createdAt: '2026-06-23T10:00:00Z',
          updatedAt: '2026-06-23T10:05:00Z',
        },
      },
    })

    const updated = await templatesApi.submitForTest('tpl-1', { commentSummary: 'Ready for test' })

    expect(http.post).toHaveBeenCalledWith('/templates/tpl-1/lifecycle/submit-test', {
      commentSummary: 'Ready for test',
    })
    expect(updated.lifecycleStatus).toBe('TESTING')
  })

  it('starts test generation preview', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          previewId: 'preview-1',
          templateId: 'tpl-1',
          templateVersionId: 'ver-1',
          status: 'PENDING',
          outputFormat: 'PDF',
          artifactStorageKey: null,
          fidelityWarnings: [],
          previewComparison: null,
          testDataSetId: null,
          createdAt: '2026-06-23T10:00:00Z',
        },
      },
    })

    const preview = await templatesApi.testGenerate('tpl-1')

    expect(http.post).toHaveBeenCalledWith('/templates/tpl-1/previews/test-generate', {})
    expect(preview.previewId).toBe('preview-1')
  })

  it('fetches live publish gate checklist', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          templateId: 'tpl-1',
          ready: false,
          blockerCount: 1,
          items: [
            {
              checkCode: 'ANCHOR_INTEGRITY',
              ready: false,
              blocker: true,
              messageKey: 'api.publishGate.anchorIntegrity.blocked',
              summary: 'Anchor binding validation has blocking issues.',
            },
          ],
        },
      },
    })

    const checklist = await templatesApi.fetchPublishGate('tpl-1')

    expect(http.get).toHaveBeenCalledWith('/templates/tpl-1/publish-gate', undefined)
    expect(checklist.blockerCount).toBe(1)
  })

  it('fetches submit-for-approval gate checklist with phase param', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          templateId: 'tpl-1',
          ready: true,
          blockerCount: 0,
          items: [],
        },
      },
    })

    const checklist = await templatesApi.fetchPublishGate('tpl-1', 'SUBMIT_FOR_APPROVAL')

    expect(http.get).toHaveBeenCalledWith('/templates/tpl-1/publish-gate', {
      params: { phase: 'SUBMIT_FOR_APPROVAL' },
    })
    expect(checklist.ready).toBe(true)
  })

  it('lists template content module references', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: [
          {
            referenceKey: 'LOAN_DISCLOSURE',
            moduleId: 'MOD-LOAN-DISCLOSURE',
            semanticVersion: '1.0.0',
            locked: false,
          },
        ],
      },
    })

    const references = await templatesApi.listTemplateContentModuleReferences('tpl-1')

    expect(http.get).toHaveBeenCalledWith('/templates/tpl-1/content-module-references')
    expect(references[0]?.referenceKey).toBe('LOAN_DISCLOSURE')
  })

  it('upserts a template content module reference', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          referenceKey: 'LOAN_DISCLOSURE',
          moduleId: 'MOD-LOAN-DISCLOSURE',
          semanticVersion: '1.1.0',
          locked: false,
        },
      },
    })

    const saved = await templatesApi.upsertTemplateContentModuleReference('tpl-1', 'LOAN_DISCLOSURE', {
      referenceKey: 'LOAN_DISCLOSURE',
      moduleId: 'MOD-LOAN-DISCLOSURE',
      semanticVersion: '1.1.0',
    })

    expect(http.put).toHaveBeenCalledWith(
      '/templates/tpl-1/content-module-references/LOAN_DISCLOSURE',
      {
        referenceKey: 'LOAN_DISCLOSURE',
        moduleId: 'MOD-LOAN-DISCLOSURE',
        semanticVersion: '1.1.0',
      },
    )
    expect(saved.semanticVersion).toBe('1.1.0')
  })
})
