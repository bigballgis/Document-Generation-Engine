import type {
  BatchTestRunSummary,
  ChangeDiffSummary,
  CoverageSummary,
  PreviewRecord,
  PreviewRunSummary,
  SubmitTestEligibility,
  TemplateContentModuleReference,
  TemplateDetail,
  TemplateReleaseVersion,
  TemplateVersionLineSummary,
  TestDataSet,
} from '@/types/template'

export interface VersionLinesCache {
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

export function createEmptyTemplatePanelEntry(): TemplatePanelEntry {
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

export function releaseVersionDetailKey(templateId: string, releaseVersion: string): string {
  return `${templateId}:${releaseVersion}`
}
