import type { APIRequestContext } from '@playwright/test'

import folDemoTestVariables from '../fixtures/fol-demo-test-variables.json' with { type: 'json' }
import {
  E2E_TEMPLATE_AUTHOR,
  FOL_CLAUSE_CODES,
  FOL_EXPECTED_ANCHOR_COUNT,
  FOL_GROUP_CODE,
  FOL_TEMPLATE_EXTERNAL_ID,
} from './auth'
import { E2E_API_BASE_URL } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

interface TemplateDetail {
  id: string
  externalId: string
  groupCode: string
  lifecycleStatus: string
  description?: string
}

interface AnchorBindingSummary {
  anchorId: string
}

interface ContentModuleSummary {
  moduleCode: string
  groupCode: string
  name: string
}

export interface FolCatalogFixture {
  templateId: string
  externalId: string
  groupCode: string
}

export const FOL_TEST_VARIABLES = folDemoTestVariables.variables

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
  return body.result.accessToken
}

async function authorizedGet<T>(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
): Promise<T> {
  const response = await request.get(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok()) {
    throw new Error(`GET ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<T>
  return body.result
}

interface TemplateListPage {
  content: TemplateDetail[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

async function listTemplates(
  request: APIRequestContext,
  token: string,
): Promise<TemplateDetail[]> {
  const page = await authorizedGet<TemplateListPage | TemplateDetail[]>(
    request,
    token,
    '/templates?size=200',
  )
  if (Array.isArray(page)) {
    return page
  }
  return page.content ?? []
}

export async function findFolTemplate(
  request: APIRequestContext,
): Promise<TemplateDetail | undefined> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const templates = await listTemplates(request, token)
  return templates.find((template) => template.externalId === FOL_TEMPLATE_EXTERNAL_ID)
}

export async function requireFolTemplate(request: APIRequestContext): Promise<FolCatalogFixture> {
  const template = await findFolTemplate(request)
  if (!template) {
    throw new Error(
      `FOL template "${FOL_TEMPLATE_EXTERNAL_ID}" was not found. ` +
        'Ensure DOCGEN_SEED_DEMO_CATALOG=true and restart docgen-backend.',
    )
  }
  return {
    templateId: template.id,
    externalId: template.externalId,
    groupCode: template.groupCode,
  }
}

export async function folTemplateDetailPath(request: APIRequestContext): Promise<string> {
  const fixture = await requireFolTemplate(request)
  return `/templates/${fixture.templateId}`
}

export async function listFolClauses(request: APIRequestContext): Promise<ContentModuleSummary[]> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const modules = await authorizedGet<ContentModuleSummary[]>(
    request,
    token,
    `/content-modules?groupCode=${FOL_GROUP_CODE}`,
  )
  return modules.filter((module) => FOL_CLAUSE_CODES.includes(module.moduleCode))
}

export async function assertFolCatalogSeeded(request: APIRequestContext): Promise<FolCatalogFixture> {
  const fixture = await requireFolTemplate(request)
  if (fixture.groupCode !== FOL_GROUP_CODE) {
    throw new Error(`FOL template group mismatch: expected ${FOL_GROUP_CODE}, got ${fixture.groupCode}`)
  }

  const clauses = await listFolClauses(request)
  if (clauses.length < FOL_CLAUSE_CODES.length) {
    const found = clauses.map((clause) => clause.moduleCode).join(', ')
    throw new Error(
      `Expected ${FOL_CLAUSE_CODES.length} FOL clauses, found ${clauses.length}: [${found}]`,
    )
  }

  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const detail = await authorizedGet<{ bindings: AnchorBindingSummary[] }>(
    request,
    token,
    `/templates/${fixture.templateId}`,
  )
  const bindings = detail.bindings ?? []
  if (bindings.length < FOL_EXPECTED_ANCHOR_COUNT) {
    throw new Error(
      `Expected at least ${FOL_EXPECTED_ANCHOR_COUNT} FOL anchor bindings, found ${bindings.length}`,
    )
  }

  return fixture
}
