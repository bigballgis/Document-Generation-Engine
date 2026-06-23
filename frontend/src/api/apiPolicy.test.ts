import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as apiPolicyApi from '@/api/apiPolicy'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
  },
}))

describe('apiPolicy API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
    vi.mocked(http.put).mockReset()
    vi.mocked(http.post).mockReset()
  })

  it('loads API policy for a template', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          templateId: 'tpl-1',
          policyVersion: 1,
          allowedAdGroups: ['APP-DOCGEN-RETAIL'],
          defaultRouteReleaseVersion: '1.0.0',
          outputFormats: ['PDF'],
          outputModes: ['INLINE'],
          batchEnabled: false,
          maxBatchSize: 10,
          docxEncryptionEnabled: false,
          pdfEncryptionEnabled: false,
          updatedAt: '2026-06-23T10:00:00Z',
        },
      },
    })

    const policy = await apiPolicyApi.getApiPolicy('tpl-1')

    expect(http.get).toHaveBeenCalledWith('/templates/tpl-1/api/policy')
    expect(policy.policyVersion).toBe(1)
  })

  it('upserts API policy', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          templateId: 'tpl-1',
          policyVersion: 2,
          allowedAdGroups: ['APP-DOCGEN-RETAIL'],
          defaultRouteReleaseVersion: '1.0.0',
          outputFormats: ['PDF'],
          outputModes: ['INLINE'],
          batchEnabled: true,
          maxBatchSize: 25,
          docxEncryptionEnabled: true,
          pdfEncryptionEnabled: false,
          updatedAt: '2026-06-23T11:00:00Z',
        },
      },
    })

    const payload = {
      allowedAdGroups: ['APP-DOCGEN-RETAIL'],
      defaultRouteReleaseVersion: '1.0.0',
      outputFormats: ['PDF'],
      outputModes: ['INLINE'],
      batchEnabled: true,
      maxBatchSize: 25,
      docxEncryptionEnabled: true,
      pdfEncryptionEnabled: false,
    }

    const policy = await apiPolicyApi.upsertApiPolicy('tpl-1', payload)

    expect(http.put).toHaveBeenCalledWith('/templates/tpl-1/api/policy', payload)
    expect(policy.policyVersion).toBe(2)
  })

  it('creates API credential', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          credentialId: 'cred-1',
          externalId: 'EXT-001',
          secret: 'secret-value',
          status: 'ACTIVE',
          createdAt: '2026-06-23T10:00:00Z',
        },
      },
    })

    const credential = await apiPolicyApi.createCredential('tpl-1')

    expect(http.post).toHaveBeenCalledWith('/templates/tpl-1/api/credentials')
    expect(credential.externalId).toBe('EXT-001')
  })
})
