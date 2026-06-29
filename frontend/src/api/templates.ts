import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  AnchorBinding,
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
  PublishGateChecklist,
  PublishTemplatePayload,
  RuleValidationResult,
  TemplateContentModuleReference,
  TemplateDetail,
  TemplateExportResult,
  TemplateImportResult,
  TemplateReleaseVersion,
  TemplateSummary,
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

function unwrap<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.result) {
    throw new Error('API response missing result')
  }
  return envelope.result
}

export async function listTemplates(): Promise<TemplateSummary[]> {
  const response = await http.get<ApiEnvelope<TemplateSummary[]>>('/templates')
  return unwrap(response.data)
}

export async function getTemplate(templateId: string): Promise<TemplateDetail> {
  const response = await http.get<ApiEnvelope<TemplateDetail>>(`/templates/${templateId}`)
  return unwrap(response.data)
}

export async function createTemplate(payload: CreateTemplatePayload): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>('/templates', payload)
  return unwrap(response.data)
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
  return unwrap(response.data)
}

export async function recordTestDecision(
  templateId: string,
  payload: LifecycleDecisionPayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/test-decision`,
    payload,
  )
  return unwrap(response.data)
}

export async function submitForApproval(
  templateId: string,
  payload: LifecycleCommentPayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/submit-approval`,
    payload,
  )
  return unwrap(response.data)
}

export async function recordApprovalDecision(
  templateId: string,
  payload: LifecycleDecisionPayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/approval-decision`,
    payload,
  )
  return unwrap(response.data)
}

export async function publishTemplate(
  templateId: string,
  payload: PublishTemplatePayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/publish`,
    payload,
  )
  return unwrap(response.data)
}

export async function stopTemplate(
  templateId: string,
  payload: LifecycleGovernancePayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/stop`,
    payload,
  )
  return unwrap(response.data)
}

export async function restoreTemplate(
  templateId: string,
  payload: LifecycleGovernancePayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/restore`,
    payload,
  )
  return unwrap(response.data)
}

export async function deprecateTemplate(
  templateId: string,
  payload: LifecycleGovernancePayload,
): Promise<TemplateDetail> {
  const response = await http.post<ApiEnvelope<TemplateDetail>>(
    `/templates/${templateId}/lifecycle/deprecate`,
    payload,
  )
  return unwrap(response.data)
}

export async function fetchLifecycleImpactPreview(
  templateId: string,
  payload: LifecycleImpactPreviewRequest,
): Promise<LifecycleImpactPreview> {
  const response = await http.post<ApiEnvelope<LifecycleImpactPreview>>(
    `/templates/${templateId}/lifecycle/impact-preview`,
    payload,
  )
  return unwrap(response.data)
}

export async function fetchReleaseVersions(templateId: string): Promise<TemplateReleaseVersion[]> {
  const response = await http.get<ApiEnvelope<TemplateReleaseVersion[]>>(
    `/templates/${templateId}/release-versions`,
  )
  return unwrap(response.data)
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
  return unwrap(response.data)
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
  return unwrap(response.data)
}

export async function updateTemplateMetadata(
  templateId: string,
  payload: UpdateTemplateMetadataPayload,
): Promise<TemplateDetail> {
  const response = await http.patch<ApiEnvelope<TemplateDetail>>(`/templates/${templateId}`, payload)
  return unwrap(response.data)
}

export async function testGenerate(
  templateId: string,
  payload: TestGeneratePayload = {},
): Promise<PreviewRecord> {
  const response = await http.post<ApiEnvelope<PreviewRecord>>(
    `/templates/${templateId}/previews/test-generate`,
    payload,
  )
  return unwrap(response.data)
}

export async function batchTestGenerate(
  templateId: string,
  payload: BatchTestGeneratePayload,
): Promise<BatchTestSummary> {
  const response = await http.post<ApiEnvelope<BatchTestSummary>>(
    `/templates/${templateId}/previews/batch-test`,
    payload,
  )
  return unwrap(response.data)
}

export async function getTemplateCoverage(templateId: string): Promise<CoverageSummary> {
  const response = await http.get<ApiEnvelope<CoverageSummary>>(`/templates/${templateId}/coverage`)
  return unwrap(response.data)
}

export async function fetchPublishGate(templateId: string): Promise<PublishGateChecklist> {
  const response = await http.get<ApiEnvelope<PublishGateChecklist>>(
    `/templates/${templateId}/publish-gate`,
  )
  return unwrap(response.data)
}

export async function fetchChangeDiff(templateId: string): Promise<ChangeDiffSummary> {
  const response = await http.get<ApiEnvelope<ChangeDiffSummary>>(
    `/templates/${templateId}/change-diff`,
  )
  return unwrap(response.data)
}

export async function getPreview(templateId: string, previewId: string): Promise<PreviewRecord> {
  const response = await http.get<ApiEnvelope<PreviewRecord>>(
    `/templates/${templateId}/previews/${previewId}`,
  )
  return unwrap(response.data)
}

export async function validateBindings(templateId: string): Promise<BindingValidationResult> {
  const response = await http.post<ApiEnvelope<BindingValidationResult>>(
    `/templates/${templateId}/bindings/validate`,
  )
  return unwrap(response.data)
}

export async function getMasterStyleCatalog(templateId: string): Promise<MasterStyleCatalog> {
  const response = await http.get<ApiEnvelope<MasterStyleCatalog>>(
    `/templates/${templateId}/master-style-catalog`,
  )
  return unwrap(response.data)
}

export async function pasteClean(
  templateId: string,
  payload: { sourceHtml: string; prePasteStructuredContentJson: string },
): Promise<PasteCleanResult> {
  const response = await http.post<ApiEnvelope<PasteCleanResult>>(
    `/templates/${templateId}/paste-clean`,
    payload,
  )
  return unwrap(response.data)
}

export async function validateRules(
  templateId: string,
  rules: CompositionRuleInput[],
): Promise<RuleValidationResult> {
  const response = await http.post<ApiEnvelope<RuleValidationResult>>(
    `/templates/${templateId}/rules/validate`,
    { rules },
  )
  return unwrap(response.data)
}

export async function saveRules(
  templateId: string,
  rules: CompositionRuleInput[],
): Promise<CompositionRuleInput[]> {
  const response = await http.put<ApiEnvelope<CompositionRuleInput[]>>(
    `/templates/${templateId}/rules`,
    { rules },
  )
  return unwrap(response.data)
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
  return unwrap(response.data)
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
  return unwrap(response.data)
}

export async function listTestDataSets(templateId: string): Promise<TestDataSet[]> {
  const response = await http.get<ApiEnvelope<TestDataSet[]>>(
    `/templates/${templateId}/test-data-sets`,
  )
  return unwrap(response.data)
}

export async function createTestDataSet(
  templateId: string,
  payload: UpsertTestDataSetPayload,
): Promise<TestDataSet> {
  const response = await http.post<ApiEnvelope<TestDataSet>>(
    `/templates/${templateId}/test-data-sets`,
    payload,
  )
  return unwrap(response.data)
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
  return unwrap(response.data)
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
  return unwrap(response.data)
}

export async function exportTemplateJson(templateId: string): Promise<TemplateExportResult> {
  const response = await http.get<ApiEnvelope<TemplateExportResult>>(`/templates/${templateId}/export`)
  return unwrap(response.data)
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
  return unwrap(response.data)
}

export async function listTemplateContentModuleReferences(
  templateId: string,
): Promise<TemplateContentModuleReference[]> {
  const response = await http.get<ApiEnvelope<TemplateContentModuleReference[]>>(
    `/templates/${templateId}/content-module-references`,
  )
  return unwrap(response.data)
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
  return unwrap(response.data)
}
