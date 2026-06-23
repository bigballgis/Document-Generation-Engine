import { describe, expect, it, vi } from 'vitest'
import { getCallerContract } from '@/api/contract'
import { http } from '@/api/http'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
  },
}))

describe('contract api', () => {
  it('loads caller contract for a template', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: { auditId: 'AUD-1', traceId: 'trace-1' },
        result: {
          templateId: 'TPL-1',
          paths: ['/api/dev/v1/templates/TPL-1/contract'],
          defaultRoute: {
            url: '/api/dev/v1/templates/TPL-1/default/generate',
            currentTargetReleaseVersion: '1.0.0',
            currentTargetStatus: 'PUBLISHED',
            updatedAt: '2026-06-23T00:00:00Z',
            updatedBy: '10000007',
            explicitVersionUrl: '/api/dev/v1/templates/TPL-1/versions/1.0.0/generate',
          },
          apiPolicy: {
            policyVersion: 1,
            updatedAt: '2026-06-23T00:00:00Z',
            updatedBy: '10000007',
            allowedOutputFormats: ['DOCX'],
            allowedOutputModes: ['SYNC_STREAM'],
            batchLimits: { syncMaxItems: 10, asyncMaxItems: 10 },
            encryptionCapabilities: { docxEnabled: false, pdfEnabled: false, permissions: [] },
            adGroupAuthorizationSummary: {
              authorized: true,
              cacheTtlSeconds: 300,
              authorizationScopeSummary: '1 authorized AD groups configured',
              effectivePolicyDescription: 'Fail-closed AD Group authorization is enforced for runtime calls',
            },
            credentialSummary: null,
          },
          callableVersions: [
            {
              releaseVersion: '1.0.0',
              explicitVersionUrl: '/api/dev/v1/templates/TPL-1/versions/1.0.0/generate',
            },
          ],
          schemas: ['GenerateRequest'],
          errorCodes: [{ category: 'RUNTIME', code: 'REQUEST_BODY_INVALID', messageKey: 'x', retryable: false, message: 'Invalid body.' }],
          examples: ['generate-sync-docx'],
        },
      },
    })

    const contract = await getCallerContract('template-uuid', 'dev')

    expect(http.get).toHaveBeenCalledWith('/templates/template-uuid/api/contract', {
      params: { environment: 'dev' },
    })
    expect(contract.templateId).toBe('TPL-1')
    expect(contract.errorCodes[0]?.code).toBe('REQUEST_BODY_INVALID')
  })
})
