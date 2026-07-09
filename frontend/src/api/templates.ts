import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type { PageView } from '@/types/identity'
import type {
  AnchorBinding,
  AsyncPreviewStarted,
  BatchTestRunSummary,
  BatchTestStarted,
  BindingValidationResult,
  BatchTestGeneratePayload,
  BatchTestSummary,
  ChangeDiffSummary,
  CompositionRuleInput,
  CoverageSummary,
  CreateTemplatePayload,
  DeleteTemplatePayload,
  ImportTemplatePayload,
  LifecycleCommentPayload,
  LifecycleDecisionPayload,
  LifecycleGovernancePayload,
  LifecycleImpactPreview,
  LifecycleImpactPreviewRequest,
  PreviewRecord,
  PreviewRunSummary,
  PublishGateChecklist,
  PublishTemplatePayload,
  RuleValidationResult,
  SubmitTestEligibility,
  TemplateContentModuleReference,
  TemplateDetail,
  TemplateDevVersionCreated,
  TemplateExportResult,
  TemplateImportResult,
  TemplateReleaseVersion,
  TemplateSummary,
  TemplateVersionLineSummary,
  UpsertContentModuleReferencePayload,
  TestDataSet,
  TestGeneratePayload,
  UpdateTemplateMetadataPayload,
  UpsertBindingPayload,
  UpsertTestDataSetPayload,
  UpsertVariablePayload,
  VariableSchema,
  MasterStyleCatalog,
  PasteCleanResult,
} from '@/types/template'

export interface TemplateListQuery {
  page?: number
  size?: number
  search?: string
  groupCode?: string
  lifecycleStatus?: string
  sort?: string
}

export async function listTemplates(
  page = 0,
  size = 20,
  options: { signal?: AbortSignal; search?: string; groupCode?: string; lifecycleStatus?: string; sort?: string } = {},
): Promise<PageView<TemplateSummary>> {
  const params: Record<string, string | number> = { page, size }
  if (options.search) {
    params.search = options.search
  }
  if (options.groupCode) {
    params.groupCode = options.groupCode
  }
  if (options.lifecycleStatus) {
    params.lifecycleStatus = options.lifecycleStatus
  }
  if (options.sort) {
    params.sort = options.sort
  }
  const response = await http.get<ApiEnvelope<PageView<TemplateSummary>>>('/templates', {
    params,
    signal: options.signal,
  })
  return unwrapEnvelope(response.data)
}

export async function getTemplate(templateId: string): Promise<TemplateDetail> {
  const response = await http.get<ApiEnvelope<TemplateDetail>>(`/templates/${templateId}`)
  return unwrapEnvelope(response.data)
}

export async function listTemplateVersionLines(
  templateId: string,
  page: number,
  size: number,
): Promise<PageView<TemplateVersionLineSummary>> {
  const response = await http.get<ApiEnvelope<PageView<TemplateVersionLineSummary>>>(
    `/templates/${templateId}/version-lines`,
    { params: { page, size } },
  )
  return unwrapEnvelope(response.data)
}

export async function fetchDevVersionDetail(
  templateId: string,
  devVersionId: string,
): Promise<TemplateDetail> {
  const response = await http.get<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/dev/${devVersionId}`,
  )
  return unwrapEnvelope(response.data)
}

export async function fetchReleaseVersionDetail(
  templateId: string,
  releaseVersion: string,
): Promise<TemplateDetail> {
  const response = await http.get<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/releases/${encodeURIComponent(releaseVersion)}`,
  )
  return unwrapEnvelope(response.data)
}

export async function cloneReleaseVersion(
  templateId: string,
  releaseVersion: string,
): Promise<TemplateDevVersionCreated> {
  const response = await http.post<ApiEnvelope<TemplateVersionLineSummary>>(
    `/templates/${templateId}/release-versions/${encodeURIComponent(releaseVersion)}/clone`,
  )
  const result = unwrapEnvelope(response.data)
  return {
    devVersionId: result.devVersionId,
    devVersionNumber: result.devVersionNumber,
    lifecycleStatus: result.lifecycleStatus,
  }
}

export async function abandonDevVersion(templateId: string, devVersionId: string): Promise<void> {
  const response = await http.post<ApiEnvelope<unknown> | undefined>(
    `/templates/${templateId}/dev/${devVersionId}/abandon`,
  )
  if (response.status === 204 || !response.data) {
    return
  }
  unwrapEnvelope(response.data)
}

export async function createTemplate(payload: CreateTemplatePayload): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>('/templates', payload)
  return unwrapEnvelope(response.data)
}

export async function deleteTemplate(
  templateId: string,
  payload: DeleteTemplatePayload,
): Promise<void> {
  await http.delete(`/templates/${templateId}`, { data: payload })
}

export async function submitForTest(
  templateId: string,
  payload: LifecycleCommentPayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/submit-test`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function recordTestDecision(
  templateId: string,
  payload: LifecycleDecisionPayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/test-decision`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function submitForApproval(
  templateId: string,
  payload: LifecycleCommentPayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/submit-approval`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function recordApprovalDecision(
  templateId: string,
  payload: LifecycleDecisionPayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/approval-decision`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function publishTemplate(
  templateId: string,
  payload: PublishTemplatePayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/publish`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function stopTemplate(
  templateId: string,
  payload: LifecycleGovernancePayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/stop`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function restoreTemplate(
  templateId: string,
  payload: LifecycleGovernancePayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/restore`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function deprecateTemplate(
  templateId: string,
  payload: LifecycleGovernancePayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/deprecate`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function fetchLifecycleImpactPreview(
  templateId: string,
  payload: LifecycleImpactPreviewRequest,
): Promise<LifecycleImpactPreview> {
  const response = await http.post<ApiEnvelope<LifecycleImpactPreview>>(
    `/templates/${templateId}/lifecycle/impact-preview`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function fetchReleaseVersions(templateId: string): Promise<TemplateReleaseVersion[]> {
  const response = await http.get<ApiEnvelope<TemplateReleaseVersion[]>>(
    `/templates/${templateId}/release-versions`,
  )
  return unwrapEnvelope(response.data)
}

export async function deactivateTemplateVersion(
  templateId: string,
  releaseVersion: string,
  payload: LifecycleGovernancePayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/versions/${encodeURIComponent(releaseVersion)}/deactivate`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function restoreTemplateVersion(
  templateId: string,
  releaseVersion: string,
  payload: LifecycleGovernancePayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/versions/${encodeURIComponent(releaseVersion)}/restore`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function updateTemplateMetadata(
  templateId: string,
  payload: UpdateTemplateMetadataPayload,
): Promise<TemplateDetail> {
  const response = await http.patch<ApiEnvelope<TemplateDetail>>(`/templates/${templateId}`, payload)
  return unwrapEnvelope(response.data)
}

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

export type PublishGatePhase = 'PUBLISH' | 'SUBMIT_FOR_APPROVAL'

export async function fetchPublishGate(
  templateId: string,
  phase?: PublishGatePhase,
): Promise<PublishGateChecklist> {
  const response = await http.get<ApiEnvelope<PublishGateChecklist>>(
    `/templates/${templateId}/publish-gate`,
    phase ? { params: { phase } } : undefined,
  )
  return unwrapEnvelope(response.data)
}

export async function fetchChangeDiff(templateId: string): Promise<ChangeDiffSummary> {
  const response = await http.get<ApiEnvelope<ChangeDiffSummary>>(
    `/templates/${templateId}/change-diff`,
  )
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

export async function validateBindings(templateId: string): Promise<BindingValidationResult> {
  const response = await http.post<ApiEnvelope<BindingValidationResult>>(
    `/templates/${templateId}/bindings/validate`,
    {},
  )
  return unwrapEnvelope(response.data)
}

export async function getMasterStyleCatalog(templateId: string): Promise<MasterStyleCatalog> {
  const response = await http.get<ApiEnvelope<MasterStyleCatalog>>(
    `/templates/${templateId}/master-style-catalog`,
  )
  return unwrapEnvelope(response.data)
}

export async function pasteClean(
  templateId: string,
  payload: { sourceHtml: string; prePasteStructuredContentJson: string },
): Promise<PasteCleanResult> {
  const response = await http.post<ApiEnvelope<PasteCleanResult>>(
    `/templates/${templateId}/paste-clean`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function validateRules(
  templateId: string,
  rules: CompositionRuleInput[],
): Promise<RuleValidationResult> {
  const response = await http.post<ApiEnvelope<RuleValidationResult>>(
    `/templates/${templateId}/rules/validate`,
    { rules },
  )
  return unwrapEnvelope(response.data)
}

export async function saveRules(
  templateId: string,
  rules: CompositionRuleInput[],
): Promise<CompositionRuleInput[]> {
  const response = await http.put<ApiEnvelope<CompositionRuleInput[]>>(
    `/templates/${templateId}/rules`,
    { rules },
  )
  return unwrapEnvelope(response.data)
}

export async function upsertVariable(
  templateId: string,
  variableKey: string,
  payload: UpsertVariablePayload,
): Promise<VariableSchema> {
  const response = await http.put<ApiEnvelope<VariableSchema>>(
    `/templates/${templateId}/variables/${encodeURIComponent(variableKey)}`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function deleteVariable(templateId: string, variableKey: string): Promise<void> {
  await http.delete(`/templates/${templateId}/variables/${encodeURIComponent(variableKey)}`)
}

export async function upsertBinding(
  templateId: string,
  anchorId: string,
  payload: UpsertBindingPayload,
): Promise<AnchorBinding> {
  const response = await http.put<ApiEnvelope<AnchorBinding>>(
    `/templates/${templateId}/bindings/${encodeURIComponent(anchorId)}`,
    payload,
  )
  return unwrapEnvelope(response.data)
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

export async function exportTemplateJson(templateId: string): Promise<TemplateExportResult> {
  const response = await http.get<ApiEnvelope<TemplateExportResult>>(`/templates/${templateId}/export`)
  return unwrapEnvelope(response.data)
}

export async function exportTemplateZip(templateId: string): Promise<{ blob: Blob; filename: string }> {
  const response = await http.get<Blob>(`/templates/${templateId}/export`, {
    params: { format: 'zip' },
    responseType: 'blob',
  })
  const disposition = response.headers['content-disposition'] ?? ''
  const filenameMatch = /filename="([^"]+)"/i.exec(disposition)
  const filename = filenameMatch?.[1] ?? 'template-export.zip'
  return { blob: response.data, filename }
}

export async function importTemplate(payload: ImportTemplatePayload): Promise<TemplateImportResult> {
  const response = await http.post<ApiEnvelope<TemplateImportResult>>('/templates/import', payload)
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
