import type { TestDataSet } from '@/types/template'

/** Display model after FE normalize (async + legacy sync shapes). */
export interface NormalizedBatchTestSampleResult {
  dataSetExternalId: string
  success: boolean
  errorDetail: string | null
  docxKey: string | null
  pdfKey: string | null
  previewId: string | null
}

function asNonEmptyString(value: unknown): string | null {
  return typeof value === 'string' && value.trim().length > 0 ? value : null
}

function resolveSuccess(raw: Record<string, unknown>): boolean {
  if (typeof raw.success === 'boolean') {
    return raw.success
  }
  const status = asNonEmptyString(raw.status)
  if (status == null) {
    return false
  }
  const normalized = status.toUpperCase()
  return normalized === 'SUCCEEDED' || normalized === 'SUCCESS' || normalized === 'PASSED'
}

/**
 * Normalize batch-test sampleResults from management history API.
 * Supports async shape (dataSetExternalId + success) and legacy sync
 * (testDataSetId / previewId / status).
 */
export function normalizeSampleResults(raw: unknown): NormalizedBatchTestSampleResult[] {
  if (!Array.isArray(raw)) {
    return []
  }

  const results: NormalizedBatchTestSampleResult[] = []
  for (const item of raw) {
    if (item == null || typeof item !== 'object') {
      continue
    }
    const row = item as Record<string, unknown>
    const dataSetExternalId =
      asNonEmptyString(row.dataSetExternalId) ?? asNonEmptyString(row.testDataSetId)
    if (dataSetExternalId == null) {
      continue
    }
    results.push({
      dataSetExternalId,
      success: resolveSuccess(row),
      errorDetail: asNonEmptyString(row.errorDetail),
      docxKey: asNonEmptyString(row.docxKey),
      pdfKey: asNonEmptyString(row.pdfKey),
      previewId: asNonEmptyString(row.previewId),
    })
  }
  return results
}

/** Match a sample identifier to a current test data set (externalId → name → id). */
export function matchTestDataSetBySampleId(
  dataSets: TestDataSet[],
  sampleId: string,
): TestDataSet | null {
  const needle = sampleId.trim()
  if (!needle) {
    return null
  }
  return (
    dataSets.find((row) => row.externalId === needle) ??
    dataSets.find((row) => row.name === needle) ??
    dataSets.find((row) => row.testDataSetId === needle) ??
    null
  )
}
