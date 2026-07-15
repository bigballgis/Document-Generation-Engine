import { describe, expect, it } from 'vitest'
import type { CallerContract } from '@/types/contract'
import type { TestDataSet } from '@/types/templatePreview'
import {
  ACCESS_TOKEN_PLACEHOLDER,
  IDEMPOTENCY_KEY_PLACEHOLDER,
  absolutizeGenerateUrl,
  buildContractCopyableExample,
  buildGeneratePayload,
  pickDefaultTestDataSet,
  resolveGenerateUrl,
} from '@/utils/contractCopyableExample'

function sampleContract(overrides?: Partial<CallerContract>): CallerContract {
  return {
    templateId: 'TPL-1',
    paths: ['/api/uat/v1/templates/TPL-1/default/generate'],
    defaultRoute: {
      url: '/api/uat/v1/templates/TPL-1/default/generate',
      currentTargetReleaseVersion: '1.0.0',
      currentTargetStatus: 'PUBLISHED',
      updatedAt: '2026-06-23T00:00:00Z',
      updatedBy: '10000007',
      explicitVersionUrl: '/api/uat/v1/templates/TPL-1/versions/1.0.0/generate',
    },
    apiPolicy: {
      policyVersion: 1,
      updatedAt: '2026-06-23T00:00:00Z',
      updatedBy: '10000007',
      allowedOutputFormats: ['DOCX', 'PDF'],
      allowedOutputModes: ['SYNC_STREAM', 'ASYNC_TASK'],
      batchLimits: { syncMaxItems: 10, asyncMaxItems: 10 },
      encryptionCapabilities: { docxEnabled: false, pdfEnabled: false, permissions: [] },
      adGroupAuthorizationSummary: {
        authorized: true,
        cacheTtlSeconds: 300,
        authorizationScopeSummary: '1 authorized AD groups configured',
        effectivePolicyDescription: 'Fail-closed AD Group authorization',
      },
      credentialSummary: null,
    },
    callableVersions: [
      {
        releaseVersion: '1.0.0',
        explicitVersionUrl: '/api/uat/v1/templates/TPL-1/versions/1.0.0/generate',
      },
    ],
    schemas: ['GenerateRequest'],
    errorCodes: [],
    examples: ['generate-sync-docx'],
    ...overrides,
  }
}

function sampleDataSet(
  id: string,
  variables: Record<string, unknown>,
  locked = false,
): TestDataSet {
  return {
    testDataSetId: id,
    templateId: 'tpl-uuid',
    name: id,
    description: null,
    variables,
    required: false,
    scenarioName: null,
    coverageTags: [],
    datasetVersion: 1,
    locked,
    derivedFromId: null,
    createdAt: '2026-06-23T10:00:00Z',
    updatedAt: '2026-06-23T10:00:00Z',
  }
}

describe('contractCopyableExample', () => {
  it('CCE-001: curl includes Auth, Idempotency-Key, POST, and generate URL', () => {
    const example = buildContractCopyableExample(
      sampleContract(),
      sampleDataSet('ds-1', { customerName: 'Acme' }),
      { origin: 'https://api.example.com' },
    )

    expect(example.curl).toContain('curl -X POST')
    expect(example.curl).toContain(`Authorization: Bearer ${ACCESS_TOKEN_PLACEHOLDER}`)
    expect(example.curl).toContain(`Idempotency-Key: ${IDEMPOTENCY_KEY_PLACEHOLDER}`)
    expect(example.curl).toContain('Content-Type: application/json')
    expect(example.curl).toContain('https://api.example.com/api/uat/v1/templates/TPL-1/default/generate')
    expect(example.curl).not.toMatch(/^generate-sync-docx$/)
    expect(example.curl.length).toBeGreaterThan('generate-sync-docx'.length)
  })

  it('CCE-002: payload reflects selected dataset variables without path fields', () => {
    const payload = buildGeneratePayload(
      sampleContract(),
      sampleDataSet('ds-1', { customerName: 'Acme', noticeType: 'LOAN' }).variables,
    )

    expect(payload.variables).toEqual({ customerName: 'Acme', noticeType: 'LOAN' })
    expect(payload.output).toEqual({ format: 'DOCX', mode: 'SYNC_STREAM' })
    expect(payload).not.toHaveProperty('templateId')
    expect(payload).not.toHaveProperty('releaseVersion')
    expect(JSON.stringify(payload)).not.toContain('templateId')
    expect(JSON.stringify(payload)).not.toContain('releaseVersion')
  })

  it('CCE-003: switching dataset updates payload and curl body', () => {
    const contract = sampleContract()
    const d1 = sampleDataSet('d1', { customerName: 'Acme' })
    const d2 = sampleDataSet('d2', { customerName: 'Beta Corp' })

    const first = buildContractCopyableExample(contract, d1, { origin: 'https://x.test' })
    const second = buildContractCopyableExample(contract, d2, { origin: 'https://x.test' })

    expect(first.payload.variables.customerName).toBe('Acme')
    expect(second.payload.variables.customerName).toBe('Beta Corp')
    expect(first.payloadJson).toContain('Acme')
    expect(second.payloadJson).toContain('Beta Corp')
    expect(second.curl).toContain('Beta Corp')
    expect(second.curl).not.toContain('Acme')
  })

  it('CCE-005/CCE-003: copy payload text is current JSON without secrets', () => {
    const example = buildContractCopyableExample(
      sampleContract(),
      sampleDataSet('ds-1', { customerName: 'Acme' }),
    )

    expect(example.payloadJson).toContain('"customerName": "Acme"')
    expect(example.payloadJson).toContain(IDEMPOTENCY_KEY_PLACEHOLDER)
    expect(example.payloadJson).not.toMatch(/eyJ[A-Za-z0-9_-]+\./)
    expect(example.curl).toContain(ACCESS_TOKEN_PLACEHOLDER)
    expect(example.curl).not.toMatch(/Bearer\s+[A-Za-z0-9_-]{20,}/)
  })

  it('CCE-006: empty dataset yields skeleton variables and still builds curl', () => {
    const example = buildContractCopyableExample(sampleContract(), null, {
      origin: 'https://api.example.com',
    })

    expect(example.hasTestDataSet).toBe(false)
    expect(example.payload.variables).toEqual({})
    expect(example.curl).toContain(`Authorization: Bearer ${ACCESS_TOKEN_PLACEHOLDER}`)
    expect(example.curl).toContain(`Idempotency-Key: ${IDEMPOTENCY_KEY_PLACEHOLDER}`)
    expect(example.curl).toContain('-d ')
  })

  it('CCE-007: environment URL change regenerates curl host/path', () => {
    const uat = sampleContract()
    const prod = sampleContract({
      defaultRoute: {
        ...uat.defaultRoute,
        url: '/api/prod/v1/templates/TPL-1/default/generate',
      },
      callableVersions: [
        {
          releaseVersion: '1.0.0',
          explicitVersionUrl: '/api/prod/v1/templates/TPL-1/versions/1.0.0/generate',
        },
      ],
    })

    const uatExample = buildContractCopyableExample(uat, null, { origin: 'https://api.example.com' })
    const prodExample = buildContractCopyableExample(prod, null, {
      origin: 'https://api.example.com',
    })

    expect(uatExample.generateUrl).toContain('/api/uat/')
    expect(prodExample.generateUrl).toContain('/api/prod/')
    expect(prodExample.curl).toContain('/api/prod/v1/templates/TPL-1/default/generate')
    expect(prodExample.curl).toContain(`Authorization: Bearer ${ACCESS_TOKEN_PLACEHOLDER}`)
    expect(prodExample.curl).toContain(`Idempotency-Key: ${IDEMPOTENCY_KEY_PLACEHOLDER}`)
  })

  it('resolves default route URL and absolutizes root-relative paths', () => {
    expect(resolveGenerateUrl(sampleContract())).toBe(
      '/api/uat/v1/templates/TPL-1/default/generate',
    )
    expect(absolutizeGenerateUrl('/api/dev/v1/x', 'https://host')).toBe('https://host/api/dev/v1/x')
    expect(absolutizeGenerateUrl('https://other/api/x', 'https://host')).toBe('https://other/api/x')
  })

  it('picks first unlocked test data set by default', () => {
    const locked = sampleDataSet('locked', { a: 1 }, true)
    const open = sampleDataSet('open', { a: 2 }, false)
    expect(pickDefaultTestDataSet([locked, open])?.testDataSetId).toBe('open')
    expect(pickDefaultTestDataSet([locked])?.testDataSetId).toBe('locked')
    expect(pickDefaultTestDataSet([])).toBeNull()
  })
})
