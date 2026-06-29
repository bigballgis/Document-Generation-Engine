import { describe, expect, it } from 'vitest'
import {
  buildUpsertPayloadForDomain,
  createDomainFormFromPolicy,
} from '@/types/apiPolicyDomain'
import type { ApiPolicy } from '@/types/template'

const samplePolicy: ApiPolicy = {
  templateId: 'tpl-1',
  policyVersion: 3,
  allowedAdGroups: ['APP-DOCGEN-RETAIL'],
  defaultRouteReleaseVersion: '1.0.0',
  outputFormats: ['PDF'],
  outputModes: ['INLINE'],
  batchEnabled: true,
  maxBatchSize: 25,
  batchSyncMaxItems: 25,
  batchAsyncMaxItems: 5000,
  docxEncryptionEnabled: false,
  pdfEncryptionEnabled: true,
  updatedAt: '2026-06-25T10:00:00Z',
}

describe('apiPolicyDomain helpers', () => {
  it('builds upsert payload for output domain only', () => {
    const payload = buildUpsertPayloadForDomain(samplePolicy, 'OUTPUT_POLICY', {
      outputFormats: ['DOCX', 'PDF'],
      outputModes: ['SYNC_STREAM'],
    })

    expect(payload.outputFormats).toEqual(['DOCX', 'PDF'])
    expect(payload.outputModes).toEqual(['SYNC_STREAM'])
    expect(payload.allowedAdGroups).toEqual(samplePolicy.allowedAdGroups)
  })

  it('builds upsert payload for batch limits domain', () => {
    const payload = buildUpsertPayloadForDomain(samplePolicy, 'BATCH_LIMIT', {
      batchEnabled: false,
      syncMaxItems: 10,
      asyncMaxItems: 100,
    })

    expect(payload.batchEnabled).toBe(false)
    expect(payload.maxBatchSize).toBe(10)
    expect(payload.defaultRouteReleaseVersion).toBe('1.0.0')
  })

  it('creates domain forms from current policy', () => {
    const adGroups = createDomainFormFromPolicy(samplePolicy, 'AD_GROUP_AUTHORIZATION')
    const batch = createDomainFormFromPolicy(samplePolicy, 'BATCH_LIMIT')

    expect(adGroups.allowedAdGroups).toEqual(['APP-DOCGEN-RETAIL'])
    expect(batch.syncMaxItems).toBe(25)
    expect(batch.asyncMaxItems).toBe(5000)
  })
})
