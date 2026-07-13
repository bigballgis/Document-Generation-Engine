import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  LifecycleCommentPayload,
  LifecycleDecisionPayload,
  LifecycleGovernancePayload,
  LifecycleImpactPreview,
  LifecycleImpactPreviewRequest,
  PublishTemplatePayload,
  TemplateDetail,
} from '@/types/template'

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
