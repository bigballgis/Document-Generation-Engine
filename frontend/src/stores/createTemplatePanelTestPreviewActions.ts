import * as templatesApi from '@/api/templates'
import type {
  AsyncPreviewStarted,
  BatchTestRunSummary,
  BatchTestStarted,
  CoverageSummary,
  PreviewRecord,
  PreviewRunSummary,
  SubmitTestEligibility,
  TestDataSet,
  UpsertTestDataSetPayload,
} from '@/types/template'
import type { TemplatePanelEntry } from '@/stores/templatePanelDataTypes'

export function createTemplatePanelTestPreviewActions(deps: {
  entryFor: (templateId: string) => TemplatePanelEntry
  invalidateTestDataSetDomains: (templateId: string) => void
  invalidatePreviewDomains: (templateId: string) => void
  invalidateBatchTestDomains: (templateId: string) => void
}) {
  const {
    entryFor,
    invalidateTestDataSetDomains,
    invalidatePreviewDomains,
    invalidateBatchTestDomains,
  } = deps

  async function fetchTestDataSets(templateId: string): Promise<TestDataSet[]> {
    const entry = entryFor(templateId)
    entry.loadingTestDataSets = true
    try {
      entry.testDataSets = await templatesApi.listTestDataSets(templateId)
      return entry.testDataSets
    } finally {
      entry.loadingTestDataSets = false
    }
  }

  async function createTestDataSet(
    templateId: string,
    payload: UpsertTestDataSetPayload,
  ): Promise<TestDataSet> {
    const created = await templatesApi.createTestDataSet(templateId, payload)
    invalidateTestDataSetDomains(templateId)
    await fetchTestDataSets(templateId)
    return created
  }

  async function updateTestDataSet(
    templateId: string,
    testDataSetId: string,
    payload: UpsertTestDataSetPayload,
  ): Promise<TestDataSet> {
    const updated = await templatesApi.updateTestDataSet(templateId, testDataSetId, payload)
    invalidateTestDataSetDomains(templateId)
    await fetchTestDataSets(templateId)
    return updated
  }

  async function deleteTestDataSet(templateId: string, testDataSetId: string): Promise<void> {
    await templatesApi.deleteTestDataSet(templateId, testDataSetId)
    invalidateTestDataSetDomains(templateId)
    await fetchTestDataSets(templateId)
  }

  async function deriveTestDataSet(templateId: string, testDataSetId: string): Promise<TestDataSet> {
    const derived = await templatesApi.deriveTestDataSet(templateId, testDataSetId)
    invalidateTestDataSetDomains(templateId)
    await fetchTestDataSets(templateId)
    return derived
  }

  async function startAsyncPreview(
    templateId: string,
    payload: { testDataSetId: string },
  ): Promise<AsyncPreviewStarted> {
    const result = await templatesApi.startAsyncPreview(templateId, payload)
    invalidatePreviewDomains(templateId)
    return result
  }

  async function fetchCoverage(templateId: string): Promise<{
    coverage: CoverageSummary | null
    eligibility: SubmitTestEligibility | null
  }> {
    const entry = entryFor(templateId)
    entry.loadingCoverage = true
    try {
      const [coverageResult, eligibilityResult] = await Promise.allSettled([
        templatesApi.getTemplateCoverage(templateId),
        templatesApi.getSubmitTestEligibility(templateId),
      ])
      entry.coverage = coverageResult.status === 'fulfilled' ? coverageResult.value : null
      entry.submitTestEligibility =
        eligibilityResult.status === 'fulfilled' ? eligibilityResult.value : null
      return { coverage: entry.coverage, eligibility: entry.submitTestEligibility }
    } finally {
      entry.loadingCoverage = false
    }
  }

  async function fetchPreviewRuns(templateId: string): Promise<PreviewRunSummary[]> {
    const entry = entryFor(templateId)
    entry.loadingPreviewRuns = true
    try {
      entry.previewRuns = await templatesApi.listPreviewRuns(templateId)
      return entry.previewRuns
    } finally {
      entry.loadingPreviewRuns = false
    }
  }

  async function fetchPreview(templateId: string, previewId: string): Promise<PreviewRecord> {
    const entry = entryFor(templateId)
    entry.loadingPreviewById = { ...entry.loadingPreviewById, [previewId]: true }
    try {
      const preview = await templatesApi.getPreview(templateId, previewId)
      entry.previewsById = { ...entry.previewsById, [previewId]: preview }
      return preview
    } finally {
      entry.loadingPreviewById = { ...entry.loadingPreviewById, [previewId]: false }
    }
  }

  async function downloadPreviewArtifact(
    templateId: string,
    previewId: string,
    format: 'docx' | 'pdf',
  ): Promise<{ blob: Blob; filename: string }> {
    return templatesApi.downloadPreviewArtifact(templateId, previewId, format)
  }

  async function fetchBatchTestHistory(templateId: string): Promise<BatchTestRunSummary[]> {
    const entry = entryFor(templateId)
    entry.loadingBatchTestHistory = true
    try {
      entry.batchTestHistory = await templatesApi.getBatchTestHistory(templateId)
      return entry.batchTestHistory
    } finally {
      entry.loadingBatchTestHistory = false
    }
  }

  async function runBatchTest(templateId: string): Promise<BatchTestStarted> {
    const result = await templatesApi.runBatchTest(templateId)
    invalidateBatchTestDomains(templateId)
    return result
  }

  return {
    fetchTestDataSets,
    createTestDataSet,
    updateTestDataSet,
    deleteTestDataSet,
    deriveTestDataSet,
    startAsyncPreview,
    fetchCoverage,
    fetchPreviewRuns,
    fetchPreview,
    downloadPreviewArtifact,
    fetchBatchTestHistory,
    runBatchTest,
  }
}
