import { describe, expect, it } from 'vitest'
import {
  matchTestDataSetBySampleId,
  normalizeSampleResults,
} from '@/utils/batchTestSampleResults'
import type { TestDataSet } from '@/types/template'

describe('normalizeSampleResults (BDD-CE-U18-BTH-009)', () => {
  it('normalizes async sample shape', () => {
    const result = normalizeSampleResults([
      {
        dataSetExternalId: 'ds-ext-1',
        success: true,
        docxKey: 'k.docx',
        pdfKey: 'k.pdf',
      },
      {
        dataSetExternalId: 'ds-ext-2',
        success: false,
        errorDetail: 'boom',
      },
    ])

    expect(result).toEqual([
      {
        dataSetExternalId: 'ds-ext-1',
        success: true,
        errorDetail: null,
        docxKey: 'k.docx',
        pdfKey: 'k.pdf',
        previewId: null,
      },
      {
        dataSetExternalId: 'ds-ext-2',
        success: false,
        errorDetail: 'boom',
        docxKey: null,
        pdfKey: null,
        previewId: null,
      },
    ])
  })

  it('normalizes legacy sync shape (testDataSetId / previewId / status)', () => {
    const result = normalizeSampleResults([
      {
        testDataSetId: 'uuid-1',
        previewId: 'prev-1',
        status: 'SUCCEEDED',
      },
      {
        testDataSetId: 'uuid-2',
        previewId: 'prev-2',
        status: 'FAILED',
      },
    ])

    expect(result).toEqual([
      {
        dataSetExternalId: 'uuid-1',
        success: true,
        errorDetail: null,
        docxKey: null,
        pdfKey: null,
        previewId: 'prev-1',
      },
      {
        dataSetExternalId: 'uuid-2',
        success: false,
        errorDetail: null,
        docxKey: null,
        pdfKey: null,
        previewId: 'prev-2',
      },
    ])
  })

  it('returns empty array for null, non-array, or empty input', () => {
    expect(normalizeSampleResults(null)).toEqual([])
    expect(normalizeSampleResults(undefined)).toEqual([])
    expect(normalizeSampleResults({})).toEqual([])
    expect(normalizeSampleResults([])).toEqual([])
  })

  it('skips unusable entries without an identifier', () => {
    expect(
      normalizeSampleResults([{ success: true }, { dataSetExternalId: 'ok', success: true }]),
    ).toEqual([
      {
        dataSetExternalId: 'ok',
        success: true,
        errorDetail: null,
        docxKey: null,
        pdfKey: null,
        previewId: null,
      },
    ])
  })
})

describe('matchTestDataSetBySampleId (BDD-CE-U18-BTH-002/003)', () => {
  const sets: TestDataSet[] = [
    {
      testDataSetId: 'id-1',
      externalId: 'EXT-A',
      templateId: 'tpl-1',
      name: 'Alpha set',
      description: null,
      variables: {},
      required: false,
      scenarioName: null,
      coverageTags: [],
      datasetVersion: 1,
      locked: false,
      derivedFromId: null,
      createdAt: '2026-07-01T00:00:00Z',
      updatedAt: '2026-07-01T00:00:00Z',
    },
    {
      testDataSetId: 'id-2',
      externalId: 'EXT-B',
      templateId: 'tpl-1',
      name: 'Beta set',
      description: null,
      variables: {},
      required: false,
      scenarioName: null,
      coverageTags: [],
      datasetVersion: 1,
      locked: false,
      derivedFromId: null,
      createdAt: '2026-07-01T00:00:00Z',
      updatedAt: '2026-07-01T00:00:00Z',
    },
  ]

  it('matches by externalId', () => {
    expect(matchTestDataSetBySampleId(sets, 'EXT-A')?.testDataSetId).toBe('id-1')
  })

  it('matches by name when externalId misses', () => {
    expect(matchTestDataSetBySampleId(sets, 'Beta set')?.testDataSetId).toBe('id-2')
  })

  it('matches by testDataSetId for legacy samples', () => {
    expect(matchTestDataSetBySampleId(sets, 'id-2')?.testDataSetId).toBe('id-2')
  })

  it('returns null when no match', () => {
    expect(matchTestDataSetBySampleId(sets, 'DELETED')).toBeNull()
  })
})
