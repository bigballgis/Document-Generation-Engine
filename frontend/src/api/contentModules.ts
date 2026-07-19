import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import {
  collectAllPageContent,
  type CollectedCatalogPage,
} from '@/api/catalogPageCollect'
import type { ApiEnvelope } from '@/types/session'
import type { PageView } from '@/types/identity'
import type {
  ContentModuleDetail,
  ContentModuleLifecycleImpactSummary,
  ContentModuleLifecycleOperationPayload,
  ContentModuleLifecycleOperationResult,
  ContentModuleReviewTransitionPayload,
  ContentModuleReviewTransitionResult,
  ContentModuleSearchMode,
  ContentModuleSummary,
  ContentModuleWhereUsedTemplate,
  ContentModuleWorkflowTask,
  CreateContentModulePayload,
  CreateContentModuleVersionPayload,
  UpdateContentModuleSharedGroupCodesPayload,
  UpdateContentModuleVersionPayload,
} from '@/types/contentModule'

export type ContentModuleListQueryOptions = {
  signal?: AbortSignal
  search?: string
  /** CE-G05 — default NAME (ILIKE); FULL_TEXT for body tsvector. */
  searchMode?: ContentModuleSearchMode
  groupCode?: string
  /** CE-U20 — head display status filter (badge-aligned). */
  status?: string
  /** IBL-E1 — optional exact BCP-47 locale filter (AND with other filters). */
  locale?: string
  sort?: string
}

function normalizeGroupCode(groupCode: string | undefined): string | undefined {
  const trimmed = groupCode?.trim()
  return trimmed ? trimmed.toUpperCase() : undefined
}

export async function listContentModules(
  page = 0,
  size = 20,
  options: ContentModuleListQueryOptions = {},
): Promise<PageView<ContentModuleSummary>> {
  const params: Record<string, string | number> = { page, size }
  const groupCode = normalizeGroupCode(options.groupCode)
  if (groupCode) {
    params.groupCode = groupCode
  }
  if (options.search) {
    params.search = options.search
  }
  if (options.searchMode && options.searchMode !== 'NAME') {
    params.searchMode = options.searchMode
  }
  if (options.status?.trim()) {
    params.status = options.status.trim()
  }
  const locale = options.locale?.trim()
  if (locale) {
    params.locale = locale
  }
  if (options.sort) {
    params.sort = options.sort
  }
  const response = await http.get<ApiEnvelope<PageView<ContentModuleSummary>>>('/content-modules', {
    params,
    signal: options.signal,
  })
  return unwrapEnvelope(response.data)
}

/** Multi-page merge for authoring pickers (LR-C5 PageView; avoids silent size=100 truncate). */
export async function listAllContentModules(
  options: ContentModuleListQueryOptions = {},
): Promise<CollectedCatalogPage<ContentModuleSummary>> {
  return collectAllPageContent((page, size) => listContentModules(page, size, options))
}

export async function listContentModuleWorkflowTasks(): Promise<ContentModuleWorkflowTask[]> {
  const response = await http.get<ApiEnvelope<ContentModuleWorkflowTask[]>>(
    '/content-modules/workflow-tasks',
  )
  return response.data.result ?? []
}

export async function getContentModule(moduleId: string): Promise<ContentModuleDetail> {
  const response = await http.get<ApiEnvelope<ContentModuleDetail>>(`/content-modules/${moduleId}`)
  return unwrapEnvelope(response.data)
}

export async function createContentModule(
  payload: CreateContentModulePayload,
): Promise<ContentModuleDetail> {
  const response = await http.post<ApiEnvelope<ContentModuleDetail>>('/content-modules', payload)
  return unwrapEnvelope(response.data)
}

export async function updateContentModuleSharedGroupCodes(
  moduleId: string,
  payload: UpdateContentModuleSharedGroupCodesPayload,
): Promise<ContentModuleDetail> {
  const response = await http.put<ApiEnvelope<ContentModuleDetail>>(
    `/content-modules/${moduleId}/shared-group-codes`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function createContentModuleVersion(
  moduleId: string,
  payload: CreateContentModuleVersionPayload,
): Promise<ContentModuleDetail> {
  const response = await http.post<ApiEnvelope<ContentModuleDetail>>(
    `/content-modules/${moduleId}/versions`,
    payload,
  )
  return unwrapEnvelope(response.data)
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
  return unwrapEnvelope(response.data)
}

export async function transitionContentModuleReview(
  moduleId: string,
  payload: ContentModuleReviewTransitionPayload,
): Promise<ContentModuleReviewTransitionResult> {
  const response = await http.post<ApiEnvelope<ContentModuleReviewTransitionResult>>(
    `/content-modules/${moduleId}/review/transition`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function previewContentModuleLifecycleImpact(
  moduleId: string,
): Promise<ContentModuleLifecycleImpactSummary> {
  const response = await http.get<ApiEnvelope<ContentModuleLifecycleImpactSummary>>(
    `/content-modules/${moduleId}/lifecycle/impact/preview`,
  )
  return unwrapEnvelope(response.data)
}

export async function applyContentModuleLifecycleOperation(
  moduleId: string,
  payload: ContentModuleLifecycleOperationPayload,
): Promise<ContentModuleLifecycleOperationResult> {
  const response = await http.post<ApiEnvelope<ContentModuleLifecycleOperationResult>>(
    `/content-modules/${moduleId}/lifecycle/operation/apply`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

/** CE-G05 — paginated templates that reference the module (authorized scope). */
export async function listContentModuleWhereUsed(
  moduleId: string,
  page = 0,
  size = 20,
  options: { signal?: AbortSignal } = {},
): Promise<PageView<ContentModuleWhereUsedTemplate>> {
  const response = await http.get<ApiEnvelope<PageView<ContentModuleWhereUsedTemplate>>>(
    `/content-modules/${moduleId}/where-used`,
    {
      params: { page, size },
      signal: options.signal,
    },
  )
  return unwrapEnvelope(response.data)
}
