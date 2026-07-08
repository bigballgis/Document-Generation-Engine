import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as apiPolicyApi from '@/api/apiPolicy'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

vi.mock('@/api/apiPolicy', () => ({
  getApiPolicy: vi.fn(),
  listCredentials: vi.fn(),
  saveApiPolicyDomain: vi.fn(),
  saveInvocationRetentionDomain: vi.fn(),
  fetchApiPolicyImpactPreview: vi.fn(),
  fetchAlerts: vi.fn(),
  createCredential: vi.fn(),
  rotateCredential: vi.fn(),
  revokeCredential: vi.fn(),
  upsertApiPolicy: vi.fn(),
}))

const samplePolicy = {
  templateId: 'tpl-1',
  policyVersion: 2,
  allowedAdGroups: ['APP-DOCGEN'],
  defaultRouteReleaseVersion: '1.0.0',
  outputFormats: ['PDF'],
  outputModes: ['INLINE'],
  batchEnabled: false,
  maxBatchSize: 10,
  docxEncryptionEnabled: false,
  pdfEncryptionEnabled: false,
  saveGeneratedDocuments: true,
  invocationRecordRetentionDays: 90,
  documentRetentionDays: 30,
  updatedAt: '2026-06-25T10:00:00Z',
}

describe('apiPolicy store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(apiPolicyApi.getApiPolicy).mockReset()
    vi.mocked(apiPolicyApi.listCredentials).mockReset()
    vi.mocked(apiPolicyApi.saveApiPolicyDomain).mockReset()
    vi.mocked(apiPolicyApi.createCredential).mockReset()
    vi.mocked(apiPolicyApi.rotateCredential).mockReset()
    vi.mocked(apiPolicyApi.revokeCredential).mockReset()
    vi.mocked(apiPolicyApi.fetchApiPolicyImpactPreview).mockReset()
  })

  it('fetchPolicy stores policy keyed by templateId', async () => {
    vi.mocked(apiPolicyApi.getApiPolicy).mockResolvedValue(samplePolicy)
    const store = useApiPolicyStore()

    await store.fetchPolicy('tpl-1')

    expect(store.entryFor('tpl-1').policy).toEqual(samplePolicy)
    expect(store.entryFor('tpl-1').loadingPolicy).toBe(false)
  })

  it('setActiveTemplate exposes active template policy through computed getters', async () => {
    vi.mocked(apiPolicyApi.getApiPolicy).mockResolvedValue(samplePolicy)
    const store = useApiPolicyStore()

    store.setActiveTemplate('tpl-1')
    await store.fetchPolicy('tpl-1')

    expect(store.apiPolicy).toEqual(samplePolicy)
    expect(store.loadingPolicy).toBe(false)
  })

  it('fetchPolicy records error message key on failure', async () => {
    vi.mocked(apiPolicyApi.getApiPolicy).mockRejectedValue(
      axiosEnvelopeError(503, 'api.error.generation.serviceUnavailable'),
    )
    const store = useApiPolicyStore()

    await expect(store.fetchPolicy('tpl-1')).rejects.toBeTruthy()
    expect(store.entryFor('tpl-1').lastErrorMessageKey).toBe('api.error.generation.serviceUnavailable')
  })

  it('fetchCredentials stores credentials for templateId', async () => {
    vi.mocked(apiPolicyApi.listCredentials).mockResolvedValue([
      {
        credentialId: 'cred-1',
        externalId: 'ext-1',
        status: 'ACTIVE',
        createdAt: '2026-06-25T10:00:00Z',
        revokedAt: null,
      },
    ])
    const store = useApiPolicyStore()

    store.setActiveTemplate('tpl-1')
    await store.fetchCredentials('tpl-1')

    expect(store.credentials).toHaveLength(1)
    expect(store.credentials[0]?.externalId).toBe('ext-1')
  })

  it('savePolicyDomain updates policy for templateId', async () => {
    vi.mocked(apiPolicyApi.saveApiPolicyDomain).mockResolvedValue({
      ...samplePolicy,
      policyVersion: 3,
    })
    const store = useApiPolicyStore()

    await store.savePolicyDomain('tpl-1', 'OUTPUT_POLICY', {
      outputFormats: ['PDF'],
      outputModes: ['INLINE'],
    })

    expect(store.entryFor('tpl-1').policy?.policyVersion).toBe(3)
    expect(apiPolicyApi.saveApiPolicyDomain).toHaveBeenCalledWith(
      'tpl-1',
      'OUTPUT_POLICY',
      { outputFormats: ['PDF'], outputModes: ['INLINE'] },
      true,
    )
  })

  it('previewImpact delegates to API and clears submitting flag', async () => {
    vi.mocked(apiPolicyApi.fetchApiPolicyImpactPreview).mockResolvedValue({
      changedAreas: ['OUTPUT_POLICY'],
      blocking: false,
      warnings: [],
      defaultRouteImpacted: false,
      currentPolicyVersion: 2,
      nextPolicyVersion: 3,
      summaryMessageKey: 'api.apimgmt.policyImpact.safe',
      contractDiffSummary: null,
      idempotencyImpactSummary: null,
    })
    const store = useApiPolicyStore()

    const preview = await store.previewImpact('tpl-1', {
      allowedAdGroups: [],
      defaultRouteReleaseVersion: '1.0.0',
      outputFormats: ['PDF'],
      outputModes: ['INLINE'],
      batchEnabled: false,
      maxBatchSize: 10,
      docxEncryptionEnabled: false,
      pdfEncryptionEnabled: false,
    })

    expect(preview.blocking).toBe(false)
    expect(store.entryFor('tpl-1').submitting).toBe(false)
  })

  it('createCredential refreshes credentials list', async () => {
    vi.mocked(apiPolicyApi.createCredential).mockResolvedValue({
      credentialId: 'cred-new',
      externalId: 'ext-new',
      secret: 'secret',
      status: 'ACTIVE',
      createdAt: '2026-06-25T10:00:00Z',
    })
    vi.mocked(apiPolicyApi.listCredentials).mockResolvedValue([
      {
        credentialId: 'cred-new',
        externalId: 'ext-new',
        status: 'ACTIVE',
        createdAt: '2026-06-25T10:00:00Z',
        revokedAt: null,
      },
    ])
    const store = useApiPolicyStore()

    store.setActiveTemplate('tpl-1')
    const created = await store.createCredential('tpl-1')

    expect(created.externalId).toBe('ext-new')
    expect(store.lastCreatedCredential?.secret).toBe('secret')
    expect(store.credentials).toHaveLength(1)
  })

  it('clearTemplate removes entry and resets active template', async () => {
    vi.mocked(apiPolicyApi.getApiPolicy).mockResolvedValue(samplePolicy)
    const store = useApiPolicyStore()

    store.setActiveTemplate('tpl-1')
    await store.fetchPolicy('tpl-1')
    store.clearTemplate('tpl-1')

    expect(store.activeTemplateId).toBeNull()
    expect(store.apiPolicy).toBeNull()
  })

  it('fetchAlerts stores cross-package alerts', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue([
      {
        alertKind: 'MISSING_AD_GROUP',
        templateId: 'tpl-1',
        templateName: 'Retail account open',
        templateExternalId: 'RETAIL-ACCOUNT-OPEN',
      },
    ])
    const store = useApiPolicyStore()

    await store.fetchAlerts()

    expect(store.alerts).toHaveLength(1)
    expect(store.loadingAlerts).toBe(false)
  })
})
