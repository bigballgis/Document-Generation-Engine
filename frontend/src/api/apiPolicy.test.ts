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

const samplePolicy = {
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
  saveGeneratedDocuments: true,
  invocationRecordRetentionDays: 90,
  documentRetentionDays: 30,
  updatedAt: '2026-06-23T11:00:00Z',
}

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
          ...samplePolicy,
          policyVersion: 1,
          batchEnabled: false,
          maxBatchSize: 10,
          docxEncryptionEnabled: false,
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
        result: samplePolicy,
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

  it('saves AD group authorization domain', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: { metadata: {}, result: samplePolicy },
    })

    await apiPolicyApi.saveAdGroupsDomain('tpl-1', {
      allowedAdGroups: ['APP-DOCGEN-WHOLESALE'],
    })

    expect(http.put).toHaveBeenCalledWith('/templates/tpl-1/api/policy/ad-groups', {
      allowedAdGroups: ['APP-DOCGEN-WHOLESALE'],
      confirmed: true,
    })
  })

  it('saves output policy domain', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: { metadata: {}, result: samplePolicy },
    })

    await apiPolicyApi.saveOutputDomain('tpl-1', {
      outputFormats: ['DOCX', 'PDF'],
      outputModes: ['SYNC_STREAM'],
    })

    expect(http.put).toHaveBeenCalledWith('/templates/tpl-1/api/policy/output', {
      outputFormats: ['DOCX', 'PDF'],
      outputModes: ['SYNC_STREAM'],
      confirmed: true,
    })
  })

  it('saves batch limits domain', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: { metadata: {}, result: samplePolicy },
    })

    await apiPolicyApi.saveBatchLimitsDomain('tpl-1', {
      batchEnabled: true,
      syncMaxItems: 50,
      asyncMaxItems: 8000,
    })

    expect(http.put).toHaveBeenCalledWith('/templates/tpl-1/api/policy/batch-limits', {
      batchEnabled: true,
      syncMaxItems: 50,
      asyncMaxItems: 8000,
      confirmed: true,
    })
  })

  it('saves encryption capability domain', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: { metadata: {}, result: samplePolicy },
    })

    await apiPolicyApi.saveEncryptionDomain('tpl-1', {
      docxEncryptionEnabled: true,
      pdfEncryptionEnabled: true,
    })

    expect(http.put).toHaveBeenCalledWith('/templates/tpl-1/api/policy/encryption', {
      docxEncryptionEnabled: true,
      pdfEncryptionEnabled: true,
      confirmed: true,
    })
  })

  it('saves default route domain', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: { metadata: {}, result: samplePolicy },
    })

    await apiPolicyApi.saveDefaultRouteDomain('tpl-1', {
      defaultRouteReleaseVersion: '2.0.0',
    })

    expect(http.put).toHaveBeenCalledWith('/templates/tpl-1/api/policy/default-route', {
      defaultRouteReleaseVersion: '2.0.0',
      confirmed: true,
    })
  })

  it('saves invocation retention domain', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: { metadata: {}, result: samplePolicy },
    })

    await apiPolicyApi.saveInvocationRetentionDomain('tpl-1', {
      saveGeneratedDocuments: true,
      invocationRecordRetentionDays: 365,
      documentRetentionDays: 180,
    })

    expect(http.put).toHaveBeenCalledWith('/templates/tpl-1/api/policy/invocation-retention', {
      saveGeneratedDocuments: true,
      invocationRecordRetentionDays: 365,
      documentRetentionDays: 180,
      confirmed: true,
    })
  })

  it('routes saveApiPolicyDomain to the matching endpoint', async () => {
    vi.mocked(http.put).mockResolvedValue({
      data: { metadata: {}, result: samplePolicy },
    })

    await apiPolicyApi.saveApiPolicyDomain('tpl-1', 'ENCRYPTION_CAPABILITY', {
      docxEncryptionEnabled: false,
      pdfEncryptionEnabled: true,
    })

    expect(http.put).toHaveBeenCalledWith('/templates/tpl-1/api/policy/encryption', {
      docxEncryptionEnabled: false,
      pdfEncryptionEnabled: true,
      confirmed: true,
    })
  })

  it('loads API policy impact preview', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          changedAreas: ['OUTPUT_POLICY'],
          blocking: false,
          warnings: ['api.apimgmt.policyImpact.defaultRouteChanged'],
          defaultRouteImpacted: false,
          currentPolicyVersion: 2,
          nextPolicyVersion: 3,
          summaryMessageKey: 'api.apimgmt.policyImpact.warning',
          contractDiffSummary: null,
          idempotencyImpactSummary: null,
        },
      },
    })

    const payload = {
      allowedAdGroups: ['APP-DOCGEN-RETAIL'],
      defaultRouteReleaseVersion: '1.0.0',
      outputFormats: ['DOCX', 'PDF'],
      outputModes: ['SYNC_STREAM'],
      batchEnabled: true,
      maxBatchSize: 25,
      docxEncryptionEnabled: true,
      pdfEncryptionEnabled: true,
    }
    const preview = await apiPolicyApi.fetchApiPolicyImpactPreview('tpl-1', payload)

    expect(http.post).toHaveBeenCalledWith('/templates/tpl-1/api/policy/impact-preview', payload)
    expect(preview.nextPolicyVersion).toBe(3)
    expect(preview.blocking).toBe(false)
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

  it('rotates API credential', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          credentialId: 'cred-1',
          externalId: 'EXT-001',
          secret: 'new-secret',
          rotatedAt: '2026-06-23T12:00:00Z',
        },
      },
    })

    const rotated = await apiPolicyApi.rotateCredential('tpl-1', 'cred-1')

    expect(http.post).toHaveBeenCalledWith('/templates/tpl-1/api/credentials/cred-1/rotate')
    expect(rotated.secret).toBe('new-secret')
  })

  it('revokes API credential', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          credentialId: 'cred-1',
          externalId: 'EXT-001',
          status: 'REVOKED',
          createdAt: '2026-06-23T10:00:00Z',
          revokedAt: '2026-06-23T12:00:00Z',
        },
      },
    })

    const revoked = await apiPolicyApi.revokeCredential('tpl-1', 'cred-1')

    expect(http.post).toHaveBeenCalledWith('/templates/tpl-1/api/credentials/cred-1/revoke')
    expect(revoked.status).toBe('REVOKED')
  })

  it('loads paginated invocations with filters', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          content: [
            {
              invocationId: 'inv-1',
              invocationKind: 'SINGLE',
              status: 'SUCCEEDED',
              requestId: 'req-abc',
              resolvedReleaseVersion: '1.0.0',
              routeType: 'DEFAULT',
              createdAt: '2026-06-23T10:00:00Z',
              accessAccountSummary: 'svc***',
            },
          ],
          page: 1,
          size: 20,
          totalElements: 25,
          totalPages: 2,
        },
      },
    })

    const page = await apiPolicyApi.listInvocations('tpl-1', 1, 20, {
      status: 'FAILED',
      invocationKind: 'SINGLE',
      requestId: 'req-abc',
    })

    expect(http.get).toHaveBeenCalledWith('/templates/tpl-1/api/invocations', {
      params: {
        page: 1,
        size: 20,
        status: 'FAILED',
        invocationKind: 'SINGLE',
        requestId: 'req-abc',
      },
    })
    expect(page.content).toHaveLength(1)
    expect(page.totalElements).toBe(25)
  })

  it('loads invocation detail summary', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          invocationId: 'inv-1',
          requestId: 'req-abc',
          routeType: 'DEFAULT',
          resolvedReleaseVersion: '1.0.0',
          outcome: 'SUCCEEDED',
          durationMs: 120,
          accessAccountSummary: 'svc***',
          credentialId: 'cred-1',
          batchId: null,
          parentInvocationId: null,
          createdAt: '2026-06-23T10:00:00Z',
          documentPresent: true,
          auditLinkHint: {
            requestId: 'req-abc',
            auditId: 'audit-1',
          },
        },
      },
    })

    const detail = await apiPolicyApi.getInvocationDetail('tpl-1', 'inv-1')

    expect(http.get).toHaveBeenCalledWith('/templates/tpl-1/api/invocations/inv-1')
    expect(detail.outcome).toBe('SUCCEEDED')
    expect(detail.auditLinkHint.requestId).toBe('req-abc')
  })

  it('loads routes summary for a template', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          templateExternalId: 'RETAIL-ACCOUNT-OPEN',
          defaultRouteReleaseVersion: '2.1.0',
          defaultGeneratePath: '/api/dev/v1/templates/RETAIL-ACCOUNT-OPEN/default/generate',
          explicitPaths: [
            {
              releaseVersion: '2.1.0',
              generatePath: '/api/dev/v1/templates/RETAIL-ACCOUNT-OPEN/versions/2.1.0/generate',
            },
          ],
        },
      },
    })

    const summary = await apiPolicyApi.fetchRoutesSummary('tpl-1')

    expect(http.get).toHaveBeenCalledWith('/templates/tpl-1/api/routes-summary')
    expect(summary.externalId).toBe('RETAIL-ACCOUNT-OPEN')
    expect(summary.defaultPath).toContain('/default/generate')
  })

  it('loads cross-package API access alerts', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: [
          {
            alertType: 'NO_CREDENTIALS',
            templateId: 'tpl-2',
            templateName: 'Mortgage approval',
            templateExternalId: 'MORTGAGE-APPROVAL',
          },
        ],
      },
    })

    const alerts = await apiPolicyApi.fetchAlerts()

    expect(http.get).toHaveBeenCalledWith('/api-access/alerts')
    expect(alerts).toHaveLength(1)
    expect(alerts[0]?.alertKind).toBe('NO_CREDENTIALS')
  })
})
