import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as templatesApi from '@/api/templates'
import { http } from '@/api/http'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('templates export/import API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
    vi.mocked(http.post).mockReset()
  })

  it('exports template JSON through the management envelope', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: { traceId: 'trace-1' },
        result: {
          format: 'template-export-bundle-v1-json',
          bundle: {
            format: 'template-export-bundle-v1-json',
            metadata: { externalId: 'TPL-1' },
          },
        },
      },
    })

    const result = await templatesApi.exportTemplateJson('tpl-1')
    expect(http.get).toHaveBeenCalledWith('/templates/tpl-1/export')
    expect(result.format).toBe('template-export-bundle-v1-json')
  })

  it('exports template ZIP as a blob download', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: new Blob(['zip']),
      headers: { 'content-disposition': 'attachment; filename="TPL-1-export.zip"' },
    })

    const result = await templatesApi.exportTemplateZip('tpl-1')
    expect(http.get).toHaveBeenCalledWith('/templates/tpl-1/export', {
      params: { format: 'zip' },
      responseType: 'blob',
    })
    expect(result.filename).toBe('TPL-1-export.zip')
  })

  it('imports a template bundle', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: { traceId: 'trace-2' },
        result: {
          importSummary: {
            resolvedTemplateId: 'tpl-2',
            newDevelopmentVersion: 2,
            importBatchId: 'batch-1',
          },
          template: { id: 'tpl-2', lifecycleStatus: 'DRAFT' },
        },
      },
    })

    const payload = {
      masterId: 'master-1',
      bundle: {
        format: 'template-export-bundle-v1-json' as const,
        metadata: {
          templateId: 'tpl-2',
          externalId: 'TPL-2',
          groupCode: 'RETAIL',
          name: 'Imported',
          description: null,
          masterId: 'master-1',
          lifecycleStatus: 'PUBLISHED' as const,
          releaseVersion: '1.0.0',
          devVersionId: 'dev-1',
          devVersionNumber: 1,
          exportedAt: '2026-06-26T00:00:00Z',
        },
        variables: [],
        bindings: [],
        rules: [],
        contentModuleReferences: [],
        policySnapshot: undefined,
      },
      importConflictPolicy: 'REJECT_IMPORT' as const,
      dryRun: false as const,
    }

    const result = await templatesApi.importTemplate(payload)
    expect(http.post).toHaveBeenCalledWith('/templates/import', payload)
    expect(result.template.id).toBe('tpl-2')
  })

  it('dry-runs import and returns dependency report', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: { traceId: 'trace-3' },
        result: {
          imported: false,
          dependencyReport: {
            items: [
              {
                dependencyType: 'ASSET_BINARY',
                severity: 'WILL_MATERIALIZE',
                code: 'ASSET_WILL_MATERIALIZE',
                messageKey: 'api.error.template.dep.assetWillMaterialize',
              },
            ],
            blockingCount: 0,
            warningCount: 1,
            infoCount: 0,
            readyToCommit: true,
          },
        },
      },
    })

    const payload = {
      masterId: 'master-1',
      bundle: {
        format: 'template-export-bundle-v2-json' as const,
        metadata: {
          templateId: 'tpl-3',
          externalId: 'TPL-3',
          groupCode: 'RETAIL',
          name: 'Promotion',
          description: null,
          masterId: 'master-1',
          lifecycleStatus: 'PUBLISHED' as const,
          releaseVersion: '1.0.0',
          devVersionId: 'dev-3',
          devVersionNumber: 1,
          exportedAt: '2026-07-21T00:00:00Z',
        },
        variables: [],
        bindings: [],
        rules: [],
        contentModuleReferences: [],
        policySnapshot: undefined,
      },
      importConflictPolicy: 'REJECT_IMPORT' as const,
      dryRun: true as const,
    }

    const result = await templatesApi.importTemplate(payload)
    expect(http.post).toHaveBeenCalledWith('/templates/import', payload)
    expect(result.imported).toBe(false)
    expect(result.dependencyReport.readyToCommit).toBe(true)
  })

  it('imports a ZIP bundle via multipart form data', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: { traceId: 'trace-4' },
        result: {
          importSummary: {
            resolvedTemplateId: 'tpl-4',
            newDevelopmentVersion: 1,
            importBatchId: 'batch-4',
          },
          template: { id: 'tpl-4', lifecycleStatus: 'DRAFT' },
        },
      },
    })

    const file = new File(['zip'], 'promotion.zip', { type: 'application/zip' })
    const result = await templatesApi.importTemplate({
      masterId: 'master-1',
      file,
      importConflictPolicy: 'REJECT_IMPORT',
      dryRun: false,
    })

    expect(http.post).toHaveBeenCalledWith(
      '/templates/import',
      expect.any(FormData),
      expect.objectContaining({ headers: { 'Content-Type': 'multipart/form-data' } }),
    )
    const formData = vi.mocked(http.post).mock.calls[0]?.[1] as FormData
    expect(formData.get('masterId')).toBe('master-1')
    expect(formData.get('dryRun')).toBe('false')
    expect(formData.get('file')).toBe(file)
    expect(result.template.id).toBe('tpl-4')
  })
})
