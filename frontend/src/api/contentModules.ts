import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  ContentModuleDetail,
  ContentModuleLifecycleImpactSummary,
  ContentModuleLifecycleOperationPayload,
  ContentModuleLifecycleOperationResult,
  ContentModuleReviewTransitionPayload,
  ContentModuleReviewTransitionResult,
  ContentModuleSummary,
  CreateContentModulePayload,
  CreateContentModuleVersionPayload,
  UpdateContentModuleVersionPayload,
} from '@/types/contentModule'

function unwrap<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.result) {
    throw new Error('API response missing result')
  }
  return envelope.result
}

export async function listContentModules(groupCode: string): Promise<ContentModuleSummary[]> {
  const response = await http.get<ApiEnvelope<ContentModuleSummary[]>>('/content-modules', {
    params: { groupCode },
  })
  return unwrap(response.data)
}

export async function getContentModule(moduleId: string): Promise<ContentModuleDetail> {
  const response = await http.get<ApiEnvelope<ContentModuleDetail>>(`/content-modules/${moduleId}`)
  return unwrap(response.data)
}

export async function createContentModule(
  payload: CreateContentModulePayload,
): Promise<ContentModuleDetail> {
  const response = await http.post<ApiEnvelope<ContentModuleDetail>>('/content-modules', payload)
  return unwrap(response.data)
}

export async function createContentModuleVersion(
  moduleId: string,
  payload: CreateContentModuleVersionPayload,
): Promise<ContentModuleDetail> {
  const response = await http.post<ApiEnvelope<ContentModuleDetail>>(
    `/content-modules/${moduleId}/versions`,
    payload,
  )
  return unwrap(response.data)
}

export async function updateContentModuleDraftVersion(
  moduleId: string,
  semanticVersion: string,
  payload: UpdateContentModuleVersionPayload,
): Promise<ContentModuleDetail> {
  const response = await http.put<ApiEnvelope<ContentModuleDetail>>(
    `/content-modules/${moduleId}/versions/${encodeURIComponent(semanticVersion)}`,
    payload,
  )
  return unwrap(response.data)
}

export async function transitionContentModuleReview(
  moduleId: string,
  payload: ContentModuleReviewTransitionPayload,
): Promise<ContentModuleReviewTransitionResult> {
  const response = await http.post<ApiEnvelope<ContentModuleReviewTransitionResult>>(
    `/content-modules/${moduleId}/review/transition`,
    payload,
  )
  return unwrap(response.data)
}

export async function previewContentModuleLifecycleImpact(
  moduleId: string,
): Promise<ContentModuleLifecycleImpactSummary> {
  const response = await http.get<ApiEnvelope<ContentModuleLifecycleImpactSummary>>(
    `/content-modules/${moduleId}/lifecycle/impact/preview`,
  )
  return unwrap(response.data)
}

export async function applyContentModuleLifecycleOperation(
  moduleId: string,
  payload: ContentModuleLifecycleOperationPayload,
): Promise<ContentModuleLifecycleOperationResult> {
  const response = await http.post<ApiEnvelope<ContentModuleLifecycleOperationResult>>(
    `/content-modules/${moduleId}/lifecycle/operation/apply`,
    payload,
  )
  return unwrap(response.data)
}
