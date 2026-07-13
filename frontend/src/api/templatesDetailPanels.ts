import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  AsyncPreviewStarted,
  BatchTestGeneratePayload,
  BatchTestRunSummary,
  BatchTestStarted,
  BatchTestSummary,
  CoverageSummary,
  PreviewRecord,
  PreviewRunSummary,
  SubmitTestEligibility,
  TemplateContentModuleReference,
  TestDataSet,
  TestGeneratePayload,
  UpsertContentModuleReferencePayload,
  UpsertTestDataSetPayload,
} from '@/types/template'

/** Detail-panel APIs: previews, coverage, test data, batch tests, content-module refs. */

export async function testGenerate(
  templateId: string,
  payload: TestGeneratePayload = {},
): Promise<PreviewRecord> {
  const response = await http.post<ApiEnvelope<PreviewRecord>>(
    `/templates/${templateId}/previews/test-generate`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function batchTestGenerate(
  templateId: string,
  payload: BatchTestGeneratePayload,
): Promise<BatchTestSummary> {
  const response = await http.post<ApiEnvelope<BatchTestSummary>>(
    `/templates/${templateId}/previews/batch-test`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function getTemplateCoverage(templateId: string): Promise<CoverageSummary> {
  const response = await http.get<ApiEnvelope<CoverageSummary>>(`/templates/${templateId}/coverage`)
  return unwrapEnvelope(response.data)
}

export async function getPreview(templateId: string, previewId: string): Promise<PreviewRecord> {
  const response = await http.get<ApiEnvelope<PreviewRecord>>(
    `/templates/${templateId}/previews/${previewId}`,
  )
  return unwrapEnvelope(response.data)
}

export async function listPreviewRuns(templateId: string): Promise<PreviewRunSummary[]> {
  const response = await http.get<ApiEnvelope<PreviewRunSummary[]>>(
    `/templates/${templateId}/previews`,
  )
  return unwrapEnvelope(response.data)
}

export type PreviewArtifactFormat = 'docx' | 'pdf'

export async function downloadPreviewArtifact(
  templateId: string,
  previewId: string,
  format: PreviewArtifactFormat,
): Promise<{ blob: Blob; filename: string }> {
  const response = await http.get<Blob>(
    `/templates/${templateId}/previews/${previewId}/artifacts/${format}`,
    { responseType: 'blob' },
  )
  const disposition = response.headers['content-disposition'] ?? ''
  const filenameMatch = /filename="([^"]+)"/i.exec(disposition)
  const fallback = format === 'pdf' ? 'preview.pdf' : 'preview.docx'
  return { blob: response.data, filename: filenameMatch?.[1] ?? fallback }
}

export async function listTestDataSets(templateId: string): Promise<TestDataSet[]> {
  const response = await http.get<ApiEnvelope<TestDataSet[]>>(
    `/templates/${templateId}/test-data-sets`,
  )
  return unwrapEnvelope(response.data)
}

export async function createTestDataSet(
  templateId: string,
  payload: UpsertTestDataSetPayload,
): Promise<TestDataSet> {
  const response = await http.post<ApiEnvelope<TestDataSet>>(
    `/templates/${templateId}/test-data-sets`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function updateTestDataSet(
  templateId: string,
  testDataSetId: string,
  payload: UpsertTestDataSetPayload,
): Promise<TestDataSet> {
  const response = await http.put<ApiEnvelope<TestDataSet>>(
    `/templates/${templateId}/test-data-sets/${testDataSetId}`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function deleteTestDataSet(templateId: string, testDataSetId: string): Promise<void> {
  await http.delete(`/templates/${templateId}/test-data-sets/${testDataSetId}`)
}

export async function deriveTestDataSet(
  templateId: string,
  testDataSetId: string,
): Promise<TestDataSet> {
  const response = await http.post<ApiEnvelope<TestDataSet>>(
    `/templates/${templateId}/test-data-sets/${testDataSetId}/derive`,
  )
  return unwrapEnvelope(response.data)
}

export async function listTemplateContentModuleReferences(
  templateId: string,
): Promise<TemplateContentModuleReference[]> {
  const response = await http.get<ApiEnvelope<TemplateContentModuleReference[]>>(
    `/templates/${templateId}/content-module-references`,
  )
  return unwrapEnvelope(response.data)
}

export async function startAsyncPreview(
  templateId: string,
  payload: { testDataSetId: string },
): Promise<AsyncPreviewStarted> {
  const response = await http.post<ApiEnvelope<AsyncPreviewStarted>>(
    `/templates/${templateId}/previews/async-preview`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function runBatchTest(templateId: string): Promise<BatchTestStarted> {
  const response = await http.post<ApiEnvelope<BatchTestStarted>>(
    `/templates/${templateId}/batch-tests/run`,
  )
  return unwrapEnvelope(response.data)
}

export async function getBatchTestHistory(templateId: string): Promise<BatchTestRunSummary[]> {
  const response = await http.get<ApiEnvelope<BatchTestRunSummary[]>>(
    `/templates/${templateId}/batch-tests`,
    { params: { limit: 5 } },
  )
  return unwrapEnvelope(response.data)
}

export async function getSubmitTestEligibility(
  templateId: string,
): Promise<SubmitTestEligibility> {
  const response = await http.get<
    ApiEnvelope<{
      eligible: boolean
      conditions: {
        hasValidTestResult: boolean
        allSamplesSucceeded: boolean
        coverageGatePassed: boolean
      }
      blockingDetails: {
        uncoveredAnchors: string[]
        uncoveredVariables: string[]
        failedDataSetNames: string[]
      }
    }>
  >(`/templates/${templateId}/batch-tests/submit-eligibility`)
  const result = unwrapEnvelope(response.data)
  return {
    eligible: result.eligible,
    hasValidTestResult: result.conditions.hasValidTestResult,
    allSamplesSucceeded: result.conditions.allSamplesSucceeded,
    coverageGatePassed: result.conditions.coverageGatePassed,
    failedDataSetNames: result.blockingDetails.failedDataSetNames,
    uncoveredAnchors: result.blockingDetails.uncoveredAnchors,
    uncoveredVariables: result.blockingDetails.uncoveredVariables,
  }
}

export async function upsertTemplateContentModuleReference(
  templateId: string,
  referenceKey: string,
  payload: UpsertContentModuleReferencePayload,
): Promise<TemplateContentModuleReference> {
  const response = await http.put<ApiEnvelope<TemplateContentModuleReference>>(
    `/templates/${templateId}/content-module-references/${encodeURIComponent(referenceKey)}`,
    payload,
  )
  return unwrapEnvelope(response.data)
}
