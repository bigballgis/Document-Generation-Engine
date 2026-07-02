import type { APIRequestContext } from '@playwright/test'

import { E2E_CORP_TEMPLATE_AUTHOR, E2E_TEMPLATE_AUTHOR } from './auth'
import { prepareRetailTemplateInTesting, type TestingTemplateFixture } from './collaboration-api'
import { E2E_API_BASE_URL } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

interface TemplateVersionLineSummary {
  devVersionId: string
  lineKind: 'IN_FLIGHT' | 'PUBLISHED'
}

interface PageView<T> {
  content: T[]
}

export interface TemplateRiskPromptConfig {
  useDefault: boolean
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
  updatedAt?: string | null
}

export interface DecisionFormConfig {
  reasonCategories: string[]
  riskPromptCopy: Record<string, string>
}

export const ALL_REASON_CATEGORIES = [
  'BINDING_ISSUE',
  'VARIABLE_SCHEMA_ISSUE',
  'RULE_VALIDATION_ISSUE',
  'FIDELITY_WARNING',
  'COVERAGE_BELOW_THRESHOLD',
  'PREVIEW_COMPARISON_DIFF',
  'CONTRACT_SCOPE_CHANGE',
  'OTHER',
] as const

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

async function authorizedPut<T>(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
  data: unknown,
): Promise<T> {
  const response = await request.put(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
  })
  if (!response.ok()) {
    throw new Error(`PUT ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<T>
  return body.result
}

function authorForGroup(groupCode: string) {
  return groupCode === 'CORP' ? E2E_CORP_TEMPLATE_AUTHOR : E2E_TEMPLATE_AUTHOR
}

export async function getTemplateRiskPromptConfig(
  request: APIRequestContext,
  templateId: string,
  groupCode = 'RETAIL',
): Promise<TemplateRiskPromptConfig> {
  const token = await apiLogin(request, authorForGroup(groupCode))
  return authorizedGet<TemplateRiskPromptConfig>(
    request,
    token,
    `/templates/${templateId}/risk-prompt-config`,
  )
}

export async function upsertTemplateRiskPromptConfig(
  request: APIRequestContext,
  templateId: string,
  payload: {
    useDefault: boolean
    reasonCategories: string[]
    riskPromptCopy?: Record<string, string>
  },
  groupCode = 'RETAIL',
): Promise<TemplateRiskPromptConfig> {
  const token = await apiLogin(request, authorForGroup(groupCode))
  return authorizedPut<TemplateRiskPromptConfig>(
    request,
    token,
    `/templates/${templateId}/risk-prompt-config`,
    payload,
  )
}

export async function getDecisionFormConfig(
  request: APIRequestContext,
  templateId: string,
  groupCode = 'RETAIL',
): Promise<DecisionFormConfig> {
  const token = await apiLogin(request, authorForGroup(groupCode))
  return authorizedGet<DecisionFormConfig>(
    request,
    token,
    `/templates/${templateId}/lifecycle/decision-form-config`,
  )
}

export async function prepareTestingTemplateWithRiskPromptOverride(
  request: APIRequestContext,
  reasonCategories: string[],
  options?: { riskPromptCopy?: Record<string, string> },
): Promise<TestingTemplateFixture> {
  const fixture = await prepareRetailTemplateInTesting(request)
  await upsertTemplateRiskPromptConfig(
    request,
    fixture.templateId,
    {
      useDefault: false,
      reasonCategories,
      riskPromptCopy: options?.riskPromptCopy ?? {},
    },
    fixture.groupCode,
  )
  return fixture
}

export async function resolveDevEditorTestPreviewPath(
  request: APIRequestContext,
  fixture: TestingTemplateFixture,
): Promise<string> {
  const token = await apiLogin(request, authorForGroup(fixture.groupCode))
  const lines = await authorizedGet<PageView<TemplateVersionLineSummary>>(
    request,
    token,
    `/templates/${fixture.templateId}/version-lines?page=0&size=10`,
  )
  const inFlight = lines.content.find((line) => line.lineKind === 'IN_FLIGHT')
  if (!inFlight) {
    throw new Error(`No in-flight dev version for template ${fixture.templateId}`)
  }
  return `/templates/${fixture.templateId}/dev/${inFlight.devVersionId}?tab=authoring&authoringTab=testPreview`
}

/** @deprecated Use resolveDevEditorTestPreviewPath for workflow actions in dev editor. */
export async function resolveDevEditorLifecyclePath(
  request: APIRequestContext,
  fixture: TestingTemplateFixture,
): Promise<string> {
  return resolveDevEditorTestPreviewPath(request, fixture)
}
