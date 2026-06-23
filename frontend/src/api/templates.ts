import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  BindingValidationResult,
  CompositionRuleInput,
  CreateTemplatePayload,
  LifecycleCommentPayload,
  LifecycleDecisionPayload,
  PreviewRecord,
  PublishTemplatePayload,
  RuleValidationResult,
  TemplateDetail,
  TemplateSummary,
  TestDataSet,
  TestGeneratePayload,
  UpsertTestDataSetPayload,
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
