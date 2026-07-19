import type { APIRequestContext } from '@playwright/test'
import {
  DEMO_GROUP_CODE,
  DEMO_MASTER_NAME,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
} from './auth'
import { prepareTemplateInTesting, type TestingTemplateFixture } from './collaboration-api'
import { E2E_API_BASE_URL, findMasterByName } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

interface TemplateDetail {
  id: string
  externalId: string
  lifecycleStatus: string
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
  name: string
  groupCode: string
}

interface PublishGateChecklist {
  ready: boolean
  items: Array<{ checkCode: string; ready: boolean; blocker: boolean }>
}

export type PendingSubmitTemplateFixture = TestingTemplateFixture

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
  params?: Record<string, string>,
): Promise<T> {
  const response = await request.get(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    params,
  })
  if (!response.ok()) {
    throw new Error(`GET ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<T>
  return body.result
}

async function authorizedPost<T>(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
  data: unknown,
  expectedStatus = 200,
): Promise<T> {
  const response = await request.post(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
  })
  if (response.status() !== expectedStatus) {
    throw new Error(`POST ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
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

function uniqueExternalId(prefix: string): string {
  return `${prefix}-${Date.now().toString(36).toUpperCase()}`.replace(/[^A-Z0-9_-]/g, '-')
}

async function configurePublishableTemplate(request: APIRequestContext, templateId: string): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)

  await authorizedPut(request, authorToken, `/templates/${templateId}/variables/customerName`, {
    variableKey: 'customerName',
    variableType: 'TEXT',
    required: true,
    defaultValue: 'Customer',
    description: 'Customer name',
  })

  await authorizedPut(request, authorToken, `/templates/${templateId}/bindings/HEADER`, {
    anchorId: 'HEADER',
    declaredContentType: 'TEXT',
    structuredContentJson:
      '{"nodes":[{"type":"paragraph","children":[{"type":"variable","key":"customerName"}]}]}',
  })

  await authorizedPost(request, authorToken, `/templates/${templateId}/bindings/validate`, {})
}

async function passTestDecision(request: APIRequestContext, templateId: string): Promise<void> {
  const testerToken = await apiLogin(request, E2E_TEMPLATE_TESTER)
  await authorizedPost(request, testerToken, `/templates/${templateId}/lifecycle/test-decision`, {
    decision: 'PASSED',
    commentSummary: 'E2E submit-for-approval gate fixture',
    fidelityViewedConfirmed: true,
    coverageViewedConfirmed: true,
    previewViewedConfirmed: true,
  })
}

async function assertPendingSubmit(
  request: APIRequestContext,
  templateId: string,
): Promise<TemplateDetail> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const detail = await authorizedGet<TemplateDetail>(request, authorToken, `/templates/${templateId}`)
  if (detail.lifecycleStatus !== 'APPROVAL' || detail.approvalSubState !== 'PENDING_SUBMIT') {
    throw new Error(
      `Expected APPROVAL/PENDING_SUBMIT, got ${detail.lifecycleStatus}/${detail.approvalSubState ?? 'null'} (${templateId})`,
    )
  }
  return detail
}

export async function fetchSubmitGateChecklist(
  request: APIRequestContext,
  templateId: string,
): Promise<PublishGateChecklist> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  return authorizedGet<PublishGateChecklist>(request, authorToken, `/templates/${templateId}/publish-gate`, {
    phase: 'SUBMIT_FOR_APPROVAL',
  })
}

export async function fetchTemplateDetail(
  request: APIRequestContext,
  templateId: string,
): Promise<TemplateDetail> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  return authorizedGet<TemplateDetail>(request, authorToken, `/templates/${templateId}`)
}

export async function prepareTemplatePendingSubmitReady(
  request: APIRequestContext,
  options?: { externalId?: string; name?: string },
): Promise<PendingSubmitTemplateFixture> {
  const template = await prepareTemplateInTesting(request, {
    externalId: options?.externalId ?? uniqueExternalId('E2E-SUBMIT-GATE-OK'),
    name: options?.name ?? `E2E Submit Gate Pass ${Date.now().toString(36).toUpperCase()}`,
  })

  await passTestDecision(request, template.templateId)
  await assertPendingSubmit(request, template.templateId)

  const gate = await fetchSubmitGateChecklist(request, template.templateId)
  if (!gate.ready) {
    const pending = gate.items.filter((item) => item.blocker && !item.ready).map((item) => item.checkCode)
    throw new Error(
      `Expected green submit gate for ${template.templateId}, pending blockers: ${pending.join(', ') || 'unknown'}`,
    )
  }

  return template
}

/**
 * API setup only — advances a green PENDING_SUBMIT template to APPROVAL/PENDING_DECISION
 * so browser specs can exercise Approver Approve without UI setup clicks.
 */
export async function prepareTemplatePendingApprovalDecision(
  request: APIRequestContext,
  options?: { externalId?: string; name?: string },
): Promise<PendingSubmitTemplateFixture> {
  const template = await prepareTemplatePendingSubmitReady(request, {
    externalId: options?.externalId ?? uniqueExternalId('E2E-CDP-APPR'),
    name: options?.name ?? `E2E CDP Approver ${Date.now().toString(36).toUpperCase()}`,
  })

  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  await authorizedPost(request, authorToken, `/templates/${template.templateId}/lifecycle/submit-approval`, {
    commentSummary: 'E2E CDP approver decision fixture — ready for PENDING_DECISION',
  })

  const detail = await fetchTemplateDetail(request, template.templateId)
  if (detail.lifecycleStatus !== 'APPROVAL' || detail.approvalSubState !== 'PENDING_DECISION') {
    throw new Error(
      `Expected APPROVAL/PENDING_DECISION, got ${detail.lifecycleStatus}/${detail.approvalSubState ?? 'null'} (${template.templateId})`,
    )
  }

  return template
}

/**
 * API setup only — advances a green PENDING_DECISION template to PENDING_RELEASE
 * so browser specs can exercise team-lead Confirm go-live without UI setup clicks.
 */
export async function prepareTemplatePendingRelease(
  request: APIRequestContext,
  options?: { externalId?: string; name?: string },
): Promise<PendingSubmitTemplateFixture> {
  const template = await prepareTemplatePendingApprovalDecision(request, {
    externalId: options?.externalId ?? uniqueExternalId('E2E-CDP-PUB'),
    name: options?.name ?? `E2E CDP Publish ${Date.now().toString(36).toUpperCase()}`,
  })

  const approverToken = await apiLogin(request, E2E_TEMPLATE_APPROVER)
  await authorizedPost(request, approverToken, `/templates/${template.templateId}/lifecycle/approval-decision`, {
    decision: 'APPROVED',
    commentSummary: 'E2E CDP team-lead publish fixture — ready for PENDING_RELEASE',
    fidelityViewedConfirmed: true,
    keyEvidenceConfirmed: true,
  })

  const detail = await fetchTemplateDetail(request, template.templateId)
  if (detail.lifecycleStatus !== 'PENDING_RELEASE') {
    throw new Error(
      `Expected PENDING_RELEASE, got ${detail.lifecycleStatus} (${template.templateId})`,
    )
  }

  return template
}

export async function prepareTemplatePendingSubmitBlocked(
  request: APIRequestContext,
  options?: { externalId?: string; name?: string },
): Promise<PendingSubmitTemplateFixture> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const master = await findMasterByName(request, groupAdminToken, DEMO_MASTER_NAME)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found`)
  }

  const externalId = options?.externalId ?? uniqueExternalId('E2E-SUBMIT-GATE-BLOCK')
  const name = options?.name ?? `E2E Submit Gate Blocked ${Date.now().toString(36).toUpperCase()}`

  const createdTemplate = await authorizedPost<{ id: string; externalId: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name,
      description: 'P12-AUD-B10 submit gate blocker Playwright fixture',
      masterId: master.id,
      locale: 'en-US',
    },
    201,
  )

  await configurePublishableTemplate(request, createdTemplate.id)

  const testDataSet = await authorizedPost<{ testDataSetId: string }>(
    request,
    authorToken,
    `/templates/${createdTemplate.id}/test-data-sets`,
    {
      name: 'E2E submit gate blocker sample',
      required: true,
      variables: { customerName: 'Alice' },
    },
    201,
  )

  await authorizedPost(request, authorToken, `/templates/${createdTemplate.id}/previews/test-generate`, {
    variables: { customerName: 'Alice' },
  })

  // Intentionally skip batch-test so TEST_RESULTS remains a hard blocker at submit gate.
  void testDataSet

  await authorizedPost(request, authorToken, `/templates/${createdTemplate.id}/lifecycle/submit-test`, {
    commentSummary: 'E2E submit gate blocker — no batch test run',
  })

  await passTestDecision(request, createdTemplate.id)
  await assertPendingSubmit(request, createdTemplate.id)

  const gate = await fetchSubmitGateChecklist(request, createdTemplate.id)
  if (gate.ready) {
    throw new Error(`Expected blocked submit gate for ${createdTemplate.id}, but checklist is ready`)
  }

  const hasTestResultsBlocker = gate.items.some(
    (item) => item.checkCode === 'TEST_RESULTS' && item.blocker && !item.ready,
  )
  if (!hasTestResultsBlocker) {
    throw new Error(
      `Expected TEST_RESULTS blocker for ${createdTemplate.id}, got ${JSON.stringify(gate.items)}`,
    )
  }

  return {
    templateId: createdTemplate.id,
    externalId,
    name,
    groupCode: DEMO_GROUP_CODE,
  }
}
