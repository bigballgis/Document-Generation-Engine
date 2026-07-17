/**
 * CE-G05 API fixtures — annual-review due/complete + content-module FULL_TEXT / where-used.
 */
import type { APIRequestContext } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  E2E_AUDIT_ADMIN,
  E2E_TEMPLATE_AUTHOR,
} from './auth'
import {
  buildCatalogQuery,
  type CatalogPageView,
  E2E_CATALOG_PAGE_SIZE,
} from './catalog-query'
import {
  createAndApproveAdditionalContentModuleVersion,
  createApprovedContentModule,
  preparePublishedTemplateWithLockedReference,
  publishSecondReleaseFromClone,
  type ApprovedContentModuleFixture,
  type PublishedTemplateWithReferenceFixture,
} from './content-modules-api'
import { E2E_API_BASE_URL, ensureDemoRetailMasterApproved } from './masters-api'

interface ApiEnvelope<T> {
  result?: T
  error?: {
    code?: string
    category?: string
    messageKey?: string
    message?: string
  }
}

export interface AnnualReviewDueTask {
  templateId: string
  externalId: string
  groupCode: string
  name: string
  nextReviewDue: string
  lifecycleStatus: string
  updatedAt: string
}

export interface TemplateDetailWithReview {
  id: string
  externalId: string
  name: string
  lifecycleStatus: string
  nextReviewDue?: string | null
  releaseVersion?: string | null
}

export interface ContentModuleCatalogRow {
  moduleId: string
  moduleCode: string
  name: string
  groupCode: string
}

export interface WhereUsedRow {
  id: string
  externalId: string
  name: string
  groupCode: string
  lifecycleStatus: string
  pinnedSemanticVersion?: string | null
}

async function apiLogin(
  request: APIRequestContext,
  credentials: { username: string; password: string },
): Promise<string> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: credentials,
  })
  if (!response.ok()) {
    throw new Error(`API login failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<{ accessToken: string }>
  return body.result!.accessToken
}

export function utcToday(): string {
  return new Date().toISOString().slice(0, 10)
}

export function utcPlusDays(days: number): string {
  const d = new Date()
  d.setUTCDate(d.getUTCDate() + days)
  return d.toISOString().slice(0, 10)
}

export function clauseBodyJson(phrase: string): string {
  return JSON.stringify({
    blocks: [{ type: 'paragraph', text: phrase }],
  })
}

export async function fetchTemplateDetailViaApi(
  request: APIRequestContext,
  templateId: string,
  credentials: { username: string; password: string } = E2E_TEMPLATE_AUTHOR,
): Promise<TemplateDetailWithReview> {
  const token = await apiLogin(request, credentials)
  const response = await request.get(`${E2E_API_BASE_URL}/templates/${templateId}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok()) {
    throw new Error(`GET template failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<TemplateDetailWithReview>
  return body.result!
}

export async function listAnnualReviewDueTasksViaApi(
  request: APIRequestContext,
  credentials: { username: string; password: string } = E2E_TEMPLATE_AUTHOR,
): Promise<{ status: number; tasks: AnnualReviewDueTask[]; error?: ApiEnvelope<unknown>['error'] }> {
  const token = await apiLogin(request, credentials)
  const response = await request.get(
    `${E2E_API_BASE_URL}/author-workflow/annual-review-due-tasks`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  const raw = (await response.json()) as ApiEnvelope<AnnualReviewDueTask[]>
  if (!response.ok()) {
    return { status: response.status(), tasks: [], error: raw.error }
  }
  return { status: response.status(), tasks: raw.result ?? [] }
}

export async function completeAnnualReviewViaApi(
  request: APIRequestContext,
  templateId: string,
  nextReviewDue?: string | null,
  credentials: { username: string; password: string } = E2E_TEMPLATE_AUTHOR,
): Promise<{
  status: number
  summary?: { id: string; nextReviewDue?: string | null }
  error?: ApiEnvelope<unknown>['error']
}> {
  const token = await apiLogin(request, credentials)
  const data =
    nextReviewDue === undefined
      ? {}
      : nextReviewDue === null
        ? { nextReviewDue: null }
        : { nextReviewDue }
  const response = await request.post(
    `${E2E_API_BASE_URL}/templates/${templateId}/annual-review/complete`,
    {
      headers: { Authorization: `Bearer ${token}` },
      data,
    },
  )
  const raw = (await response.json()) as ApiEnvelope<{ id: string; nextReviewDue?: string | null }>
  if (!response.ok()) {
    return { status: response.status(), error: raw.error }
  }
  return { status: response.status(), summary: raw.result }
}

export async function completeAnnualReviewRawBody(
  request: APIRequestContext,
  templateId: string,
  body: unknown,
  credentials: { username: string; password: string } = E2E_TEMPLATE_AUTHOR,
): Promise<{ status: number; error?: ApiEnvelope<unknown>['error'] }> {
  const token = await apiLogin(request, credentials)
  const response = await request.post(
    `${E2E_API_BASE_URL}/templates/${templateId}/annual-review/complete`,
    {
      headers: { Authorization: `Bearer ${token}` },
      data: body,
    },
  )
  const raw = (await response.json()) as ApiEnvelope<unknown>
  return { status: response.status(), error: raw.error }
}

export async function preparePublishedAnnualReviewTemplate(
  request: APIRequestContext,
): Promise<
  PublishedTemplateWithReferenceFixture & {
    name: string
    nextReviewDue: string
  }
> {
  const fixture = await preparePublishedTemplateWithLockedReference(request)
  const detail = await fetchTemplateDetailViaApi(request, fixture.templateId)
  if (!detail.nextReviewDue) {
    throw new Error(`Published template ${fixture.templateId} missing nextReviewDue seed`)
  }
  return {
    ...fixture,
    name: detail.name,
    nextReviewDue: detail.nextReviewDue,
  }
}

export async function markTemplateAnnualReviewDueToday(
  request: APIRequestContext,
  templateId: string,
): Promise<string> {
  const today = utcToday()
  const result = await completeAnnualReviewViaApi(request, templateId, today)
  if (result.status !== 200) {
    throw new Error(
      `Failed to mark annual review due (${result.status}): ${JSON.stringify(result.error)}`,
    )
  }
  return today
}

export async function republishTemplatePreservingReviewDue(
  request: APIRequestContext,
  templateId: string,
  sourceReleaseVersion: string,
  nextReleaseVersion: string,
): Promise<TemplateDetailWithReview> {
  await publishSecondReleaseFromClone(
    request,
    templateId,
    sourceReleaseVersion,
    nextReleaseVersion,
  )
  return fetchTemplateDetailViaApi(request, templateId)
}

export async function createApprovedModuleWithBodyPhrase(
  request: APIRequestContext,
  options: { phrase: string; name?: string; moduleCode?: string },
): Promise<ApprovedContentModuleFixture> {
  return createApprovedContentModule(request, {
    moduleCode: options.moduleCode,
    name: options.name,
    contentStructureJson: clauseBodyJson(options.phrase),
  })
}

export async function listContentModulesSearchViaApi(
  request: APIRequestContext,
  filters: {
    search?: string
    searchMode?: 'NAME' | 'FULL_TEXT'
    groupCode?: string
    page?: number
    size?: number
  } = {},
  credentials: { username: string; password: string } = E2E_TEMPLATE_AUTHOR,
): Promise<CatalogPageView<ContentModuleCatalogRow>> {
  const token = await apiLogin(request, credentials)
  const response = await request.get(
    `${E2E_API_BASE_URL}/content-modules${buildCatalogQuery({
      search: filters.search,
      searchMode: filters.searchMode,
      groupCode: filters.groupCode,
      page: filters.page ?? 0,
      size: filters.size ?? E2E_CATALOG_PAGE_SIZE,
    })}`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  if (!response.ok()) {
    throw new Error(`list content-modules failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<CatalogPageView<ContentModuleCatalogRow>>
  return body.result!
}

export async function listWhereUsedViaApi(
  request: APIRequestContext,
  moduleId: string,
  credentials: { username: string; password: string } = E2E_TEMPLATE_AUTHOR,
): Promise<CatalogPageView<WhereUsedRow>> {
  const token = await apiLogin(request, credentials)
  const response = await request.get(
    `${E2E_API_BASE_URL}/content-modules/${moduleId}/where-used${buildCatalogQuery({
      page: 0,
      size: E2E_CATALOG_PAGE_SIZE,
    })}`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  if (!response.ok()) {
    throw new Error(`where-used failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<CatalogPageView<WhereUsedRow>>
  return body.result!
}

export async function bumpModuleBodyPhrase(
  request: APIRequestContext,
  moduleId: string,
  semanticVersion: string,
  phrase: string,
): Promise<void> {
  await createAndApproveAdditionalContentModuleVersion(request, moduleId, semanticVersion, {
    contentStructureJson: clauseBodyJson(phrase),
  })
}

/**
 * Best-effort management audit lookup. Returns undefined when the audit console
 * query is unavailable (some stacks return 500 for filtered eventType queries).
 */
export async function findManagementAuditEvent(
  request: APIRequestContext,
  options: { eventType: string; templateExternalId?: string },
): Promise<{ eventType: string; details?: Record<string, unknown> } | undefined> {
  const token = await apiLogin(request, E2E_AUDIT_ADMIN)
  const response = await request.get(
    `${E2E_API_BASE_URL}/admin/audit/management-events${buildCatalogQuery({
      eventType: options.eventType,
      page: 0,
      size: 50,
    })}`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  if (!response.ok()) {
    return undefined
  }
  const body = (await response.json()) as ApiEnvelope<
    CatalogPageView<{
      eventType: string
      details?: Record<string, unknown>
      payload?: Record<string, unknown>
    }>
  >
  const rows = body.result?.content ?? []
  if (!options.templateExternalId) {
    return rows[0]
  }
  return rows.find((row) => {
    const blob = JSON.stringify(row.details ?? row.payload ?? row)
    return blob.includes(options.templateExternalId!)
  })
}

/** Never-published draft template — nextReviewDue remains null (BDD-CE-G05-005). */
export async function createDraftTemplateNeverPublished(
  request: APIRequestContext,
): Promise<{ templateId: string; externalId: string; name: string }> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const master = await ensureDemoRetailMasterApproved(request)
  const stamp = Date.now().toString(36).toUpperCase()
  const externalId = `E2E-G05-DRAFT-${stamp}`
  const name = `E2E G05 Draft ${stamp}`
  const response = await request.post(`${E2E_API_BASE_URL}/templates`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name,
      description: 'CE-G05 null nextReviewDue control',
      masterId: master.id,
    },
  })
  if (response.status() !== 201) {
    throw new Error(`Create draft template failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<{ id: string; externalId: string }>
  return { templateId: body.result!.id, externalId: body.result!.externalId, name }
}

