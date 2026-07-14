import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type { PageView } from '@/types/identity'
import type {
  ChangeDiffSummary,
  PublishGateChecklist,
  TemplateDetail,
  TemplateDevVersionCreated,
  TemplateReleaseVersion,
  TemplateVersionLineSummary,
  UpdateTemplateMetadataPayload,
} from '@/types/template'

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

export async function fetchReleaseVersions(templateId: string): Promise<TemplateReleaseVersion[]> {
  const response = await http.get<ApiEnvelope<TemplateReleaseVersion[]>>(
    `/templates/${templateId}/release-versions`,
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

export async function fetchReleasePublishGate(
  templateId: string,
  releaseVersion: string,
): Promise<PublishGateChecklist> {
  const response = await http.get<ApiEnvelope<PublishGateChecklist>>(
    `/templates/${templateId}/releases/${encodeURIComponent(releaseVersion)}/publish-gate`,
  )
  return unwrapEnvelope(response.data)
}

export async function fetchChangeDiff(templateId: string): Promise<ChangeDiffSummary> {
  const response = await http.get<ApiEnvelope<ChangeDiffSummary>>(
    `/templates/${templateId}/change-diff`,
  )
  return unwrapEnvelope(response.data)
}

export async function fetchReleaseChangeDiff(
  templateId: string,
  releaseVersionA: string,
  releaseVersionB: string,
): Promise<ChangeDiffSummary> {
  const response = await http.get<ApiEnvelope<ChangeDiffSummary>>(
    `/templates/${templateId}/change-diff/releases`,
    {
      params: { releaseVersionA, releaseVersionB },
    },
  )
  return unwrapEnvelope(response.data)
}

/** Re-export panel APIs so the detail domain remains one import surface. */
export * from '@/api/templatesDetailPanels'
