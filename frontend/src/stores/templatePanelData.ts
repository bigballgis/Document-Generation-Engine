import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as templatesApi from '@/api/templates'
import type { PageView } from '@/types/identity'
import type {
  AsyncPreviewStarted,
  BatchTestRunSummary,
  BatchTestStarted,
  ChangeDiffSummary,
  CoverageSummary,
  PreviewRecord,
  PreviewRunSummary,
  SubmitTestEligibility,
  TemplateContentModuleReference,
  TemplateDetail,
  TemplateDevVersionCreated,
  TemplateExportResult,
  TemplateReleaseVersion,
  TemplateVersionLineSummary,
  TestDataSet,
  UpsertContentModuleReferencePayload,
  UpsertTestDataSetPayload,
} from '@/types/template'

/**
 * Invalidation rules (SOR-F04):
 * - Test data set mutations → testDataSets, coverage, submitTestEligibility
 * - Preview start / preview refresh → previewRuns (+ coverage/eligibility when preview completes elsewhere)
 * - Version line mutations → versionLines, releaseVersions, changeDiff
 * - Content module reference mutations → contentModuleReferences, coverage, changeDiff
 * - Batch test run → batchTestHistory, coverage, submitTestEligibility
 */
interface VersionLinesCache {
  page: number
  size: number
  content: TemplateVersionLineSummary[]
  totalElements: number
  totalPages: number
}

export interface TemplatePanelEntry {
  testDataSets: TestDataSet[]
  loadingTestDataSets: boolean
  coverage: CoverageSummary | null
  submitTestEligibility: SubmitTestEligibility | null
  loadingCoverage: boolean
  versionLines: VersionLinesCache | null
  loadingVersionLines: boolean
  previewRuns: PreviewRunSummary[]
  loadingPreviewRuns: boolean
  previewsById: Record<string, PreviewRecord>
  loadingPreviewById: Record<string, boolean>
  changeDiff: ChangeDiffSummary | null
  loadingChangeDiff: boolean
  releaseVersions: TemplateReleaseVersion[]
  loadingReleaseVersions: boolean
  contentModuleReferences: TemplateContentModuleReference[]
  loadingContentModuleReferences: boolean
  batchTestHistory: BatchTestRunSummary[]
  loadingBatchTestHistory: boolean
  exporting: boolean
  releaseVersionDetails: Record<string, TemplateDetail>
  loadingReleaseVersionDetail: Record<string, boolean>
}

function createEmptyEntry(): TemplatePanelEntry {
  return {
    testDataSets: [],
    loadingTestDataSets: false,
    coverage: null,
    submitTestEligibility: null,
    loadingCoverage: false,
    versionLines: null,
    loadingVersionLines: false,
    previewRuns: [],
    loadingPreviewRuns: false,
    previewsById: {},
    loadingPreviewById: {},
    changeDiff: null,
    loadingChangeDiff: false,
    releaseVersions: [],
    loadingReleaseVersions: false,
    contentModuleReferences: [],
    loadingContentModuleReferences: false,
    batchTestHistory: [],
    loadingBatchTestHistory: false,
    exporting: false,
    releaseVersionDetails: {},
    loadingReleaseVersionDetail: {},
  }
}

function releaseVersionDetailKey(templateId: string, releaseVersion: string): string {
  return `${templateId}:${releaseVersion}`
}

export const useTemplatePanelDataStore = defineStore('templatePanelData', () => {
  const entries = ref<Record<string, TemplatePanelEntry>>({})

  function entryFor(templateId: string): TemplatePanelEntry {
    if (!entries.value[templateId]) {
      entries.value = { ...entries.value, [templateId]: createEmptyEntry() }
    }
    return entries.value[templateId]!
  }

  function clearTemplate(templateId: string) {
    const next = { ...entries.value }
    delete next[templateId]
    entries.value = next
  }

  function invalidateTestDataSetDomains(templateId: string) {
    const entry = entryFor(templateId)
    entry.testDataSets = []
    entry.coverage = null
    entry.submitTestEligibility = null
  }

  function invalidatePreviewDomains(templateId: string) {
    const entry = entryFor(templateId)
    entry.previewRuns = []
    entry.previewsById = {}
  }

  function invalidateVersionLineDomains(templateId: string) {
    const entry = entryFor(templateId)
    entry.versionLines = null
    entry.releaseVersions = []
    entry.changeDiff = null
  }

  function invalidateContentModuleReferenceDomains(templateId: string) {
    const entry = entryFor(templateId)
    entry.contentModuleReferences = []
    entry.coverage = null
    entry.submitTestEligibility = null
    entry.changeDiff = null
  }

  function invalidateBatchTestDomains(templateId: string) {
    const entry = entryFor(templateId)
    entry.batchTestHistory = []
    entry.coverage = null
    entry.submitTestEligibility = null
  }

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
      if (coverageResult.status === 'fulfilled') {
        entry.coverage = coverageResult.value
      } else {
        entry.coverage = null
      }
      if (eligibilityResult.status === 'fulfilled') {
        entry.submitTestEligibility = eligibilityResult.value
      } else {
        entry.submitTestEligibility = null
      }
      return { coverage: entry.coverage, eligibility: entry.submitTestEligibility }
    } finally {
      entry.loadingCoverage = false
    }
  }

  async function fetchVersionLines(
    templateId: string,
    page: number,
    size: number,
  ): Promise<PageView<TemplateVersionLineSummary>> {
    const entry = entryFor(templateId)
    entry.loadingVersionLines = true
    try {
      const pageView = await templatesApi.listTemplateVersionLines(templateId, page, size)
      entry.versionLines = {
        page: pageView.page,
        size: pageView.size,
        content: pageView.content,
        totalElements: pageView.totalElements,
        totalPages: pageView.totalPages,
      }
      return pageView
    } finally {
      entry.loadingVersionLines = false
    }
  }

  async function cloneReleaseVersion(
    templateId: string,
    releaseVersion: string,
  ): Promise<TemplateDevVersionCreated> {
    const created = await templatesApi.cloneReleaseVersion(templateId, releaseVersion)
    invalidateVersionLineDomains(templateId)
    return created
  }

  async function abandonDevVersion(templateId: string, devVersionId: string): Promise<void> {
    await templatesApi.abandonDevVersion(templateId, devVersionId)
    invalidateVersionLineDomains(templateId)
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

  async function fetchChangeDiff(templateId: string): Promise<ChangeDiffSummary | null> {
    const entry = entryFor(templateId)
    entry.loadingChangeDiff = true
    try {
      entry.changeDiff = await templatesApi.fetchChangeDiff(templateId)
      return entry.changeDiff
    } finally {
      entry.loadingChangeDiff = false
    }
  }

  async function fetchReleaseVersions(templateId: string): Promise<TemplateReleaseVersion[]> {
    const entry = entryFor(templateId)
    entry.loadingReleaseVersions = true
    try {
      entry.releaseVersions = await templatesApi.fetchReleaseVersions(templateId)
      return entry.releaseVersions
    } finally {
      entry.loadingReleaseVersions = false
    }
  }

  async function fetchContentModuleReferences(
    templateId: string,
  ): Promise<TemplateContentModuleReference[]> {
    const entry = entryFor(templateId)
    entry.loadingContentModuleReferences = true
    try {
      entry.contentModuleReferences = await templatesApi.listTemplateContentModuleReferences(templateId)
      return entry.contentModuleReferences
    } finally {
      entry.loadingContentModuleReferences = false
    }
  }

  async function upsertContentModuleReference(
    templateId: string,
    referenceKey: string,
    payload: UpsertContentModuleReferencePayload,
  ): Promise<TemplateContentModuleReference> {
    const reference = await templatesApi.upsertTemplateContentModuleReference(
      templateId,
      referenceKey,
      payload,
    )
    invalidateContentModuleReferenceDomains(templateId)
    await fetchContentModuleReferences(templateId)
    return reference
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

  async function fetchReleaseVersionDetail(
    templateId: string,
    releaseVersion: string,
  ): Promise<TemplateDetail> {
    const entry = entryFor(templateId)
    const key = releaseVersionDetailKey(templateId, releaseVersion)
    entry.loadingReleaseVersionDetail = { ...entry.loadingReleaseVersionDetail, [key]: true }
    try {
      const detail = await templatesApi.fetchReleaseVersionDetail(templateId, releaseVersion)
      entry.releaseVersionDetails = { ...entry.releaseVersionDetails, [key]: detail }
      return detail
    } finally {
      entry.loadingReleaseVersionDetail = { ...entry.loadingReleaseVersionDetail, [key]: false }
    }
  }

  async function exportTemplateJson(templateId: string): Promise<TemplateExportResult> {
    const entry = entryFor(templateId)
    entry.exporting = true
    try {
      return await templatesApi.exportTemplateJson(templateId)
    } finally {
      entry.exporting = false
    }
  }

  async function exportTemplateZip(templateId: string): Promise<{ blob: Blob; filename: string }> {
    const entry = entryFor(templateId)
    entry.exporting = true
    try {
      return await templatesApi.exportTemplateZip(templateId)
    } finally {
      entry.exporting = false
    }
  }

  function getEntry(templateId: string): TemplatePanelEntry {
    return entryFor(templateId)
  }

  return {
    entries,
    clearTemplate,
    invalidateTestDataSetDomains,
    invalidatePreviewDomains,
    invalidateVersionLineDomains,
    invalidateContentModuleReferenceDomains,
    invalidateBatchTestDomains,
    fetchTestDataSets,
    createTestDataSet,
    updateTestDataSet,
    deleteTestDataSet,
    deriveTestDataSet,
    startAsyncPreview,
    fetchCoverage,
    fetchVersionLines,
    cloneReleaseVersion,
    abandonDevVersion,
    fetchPreviewRuns,
    fetchPreview,
    downloadPreviewArtifact,
    fetchChangeDiff,
    fetchReleaseVersions,
    fetchContentModuleReferences,
    upsertContentModuleReference,
    fetchBatchTestHistory,
    runBatchTest,
    fetchReleaseVersionDetail,
    exportTemplateJson,
    exportTemplateZip,
    getEntry,
  }
})
