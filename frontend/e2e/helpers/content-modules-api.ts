import type { APIRequestContext } from '@playwright/test'
import {
  DEMO_GROUP_CODE,
  DEMO_MASTER_NAME,
  DEMO_TEMPLATE_EXTERNAL_ID,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
} from './auth'
import { E2E_API_BASE_URL, findMasterByName } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

interface ContentModuleDetail {
  moduleId: string
  moduleCode: string
  groupCode: string
  name: string
  versions: Array<{ semanticVersion: string; reviewState: string; lifecycleState?: string }>
}

interface TemplateSummary {
  id: string
  externalId: string
  lifecycleStatus: string
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
  groupCode: string
  updatedAt?: string
}

interface TemplateContentModuleReference {
  referenceKey: string
  moduleId: string
  semanticVersion: string
  locked: boolean
}

export interface ApprovedContentModuleFixture {
  moduleId: string
  moduleCode: string
  name: string
  semanticVersion: string
}

export interface PublishedTemplateWithReferenceFixture {
  templateId: string
  externalId: string
  moduleCode: string
  referenceKey: string
  semanticVersion: string
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

function uniqueModuleCode(prefix: string): string {
  return `${prefix}-${Date.now().toString(36).toUpperCase()}`.replace(/[^A-Z0-9_-]/g, '-')
}

export async function findTemplateByExternalId(
  request: APIRequestContext,
  externalId: string,
): Promise<TemplateSummary | undefined> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const templates = await authorizedGet<TemplateSummary[]>(request, token, '/templates')
  return templates.find((template) => template.externalId === externalId)
}

export async function demoTemplateDetailPath(request: APIRequestContext): Promise<string> {
  const demoTemplate = await findTemplateByExternalId(request, DEMO_TEMPLATE_EXTERNAL_ID)
  if (!demoTemplate) {
    throw new Error(`Demo template "${DEMO_TEMPLATE_EXTERNAL_ID}" was not found`)
  }
  return `/templates/${demoTemplate.id}`
}

async function ensureDemoTemplateSubmittedForTest(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
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

  const testDataSet = await authorizedPost<{ testDataSetId: string }>(
    request,
    authorToken,
    `/templates/${templateId}/test-data-sets`,
    {
      name: 'E2E tester journey sample',
      required: true,
      variables: { customerName: 'Alice' },
    },
    201,
  )

  await authorizedPost(request, authorToken, `/templates/${templateId}/previews/test-generate`, {
    variables: { customerName: 'Alice' },
  })

  await authorizedPost(request, authorToken, `/templates/${templateId}/previews/batch-test`, {
    testDataSetIds: [testDataSet.testDataSetId],
  })

  await authorizedPost(request, authorToken, `/templates/${templateId}/lifecycle/submit-test`, {
    commentSummary: 'E2E ready for tester journey',
  })
}

export async function demoTestingTemplateDetailPath(request: APIRequestContext): Promise<string> {
  const testerToken = await apiLogin(request, E2E_TEMPLATE_TESTER)
  const templates = await authorizedGet<TemplateSummary[]>(request, testerToken, '/templates')
  const testingTemplates = templates
    .filter((template) => template.lifecycleStatus === 'TESTING')
    .sort((left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt))

  if (testingTemplates.length > 0) {
    return `/templates/${testingTemplates[0].id}`
  }

  const demoTemplate = await findTemplateByExternalId(request, DEMO_TEMPLATE_EXTERNAL_ID)
  if (!demoTemplate) {
    throw new Error(`Demo template "${DEMO_TEMPLATE_EXTERNAL_ID}" was not found`)
  }

  if (demoTemplate.lifecycleStatus !== 'TESTING') {
    await ensureDemoTemplateSubmittedForTest(request, demoTemplate.id)
  }

  const refreshed = await findTemplateByExternalId(request, DEMO_TEMPLATE_EXTERNAL_ID)
  if (!refreshed || refreshed.lifecycleStatus !== 'TESTING') {
    throw new Error(`Failed to prepare TESTING template for E2E (status=${refreshed?.lifecycleStatus})`)
  }

  return `/templates/${refreshed.id}`
}

async function ensureDemoTemplatePendingApprovalDecision(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const testerToken = await apiLogin(request, E2E_TEMPLATE_TESTER)

  const detail = await authorizedGet<{ lifecycleStatus: string; approvalSubState?: string }>(
    request,
    authorToken,
    `/templates/${templateId}`,
  )

  if (
    detail.lifecycleStatus === 'APPROVAL' &&
    detail.approvalSubState === 'PENDING_DECISION'
  ) {
    return
  }

  if (detail.lifecycleStatus !== 'TESTING' && detail.lifecycleStatus !== 'APPROVAL') {
    await ensureDemoTemplateSubmittedForTest(request, templateId)
  }

  const afterSubmit = await authorizedGet<{ lifecycleStatus: string; approvalSubState?: string }>(
    request,
    authorToken,
    `/templates/${templateId}`,
  )

  if (afterSubmit.lifecycleStatus === 'TESTING') {
    await authorizedPost(request, testerToken, `/templates/${templateId}/lifecycle/test-decision`, {
      decision: 'PASSED',
      commentSummary: 'E2E test passed for approver journey',
      fidelityViewedConfirmed: true,
      coverageViewedConfirmed: true,
      previewViewedConfirmed: true,
    })
  }

  const afterTest = await authorizedGet<{ lifecycleStatus: string; approvalSubState?: string }>(
    request,
    authorToken,
    `/templates/${templateId}`,
  )

  if (
    afterTest.lifecycleStatus === 'APPROVAL' &&
    afterTest.approvalSubState === 'PENDING_SUBMIT'
  ) {
    await authorizedPost(request, authorToken, `/templates/${templateId}/lifecycle/submit-approval`, {
      commentSummary: 'E2E ready for approver journey',
    })
  }
}

export async function demoApprovalTemplateDetailPath(request: APIRequestContext): Promise<string> {
  const approverToken = await apiLogin(request, E2E_TEMPLATE_APPROVER)
  const templates = await authorizedGet<TemplateSummary[]>(request, approverToken, '/templates')
  const pendingDecisionTemplates = templates
    .filter(
      (template) =>
        template.lifecycleStatus === 'APPROVAL' &&
        template.approvalSubState === 'PENDING_DECISION',
    )
    .sort((left, right) => Date.parse(right.updatedAt ?? '') - Date.parse(left.updatedAt ?? ''))

  if (pendingDecisionTemplates.length > 0) {
    return `/templates/${pendingDecisionTemplates[0].id}`
  }

  const demoTemplate = await findTemplateByExternalId(request, DEMO_TEMPLATE_EXTERNAL_ID)
  if (!demoTemplate) {
    throw new Error(`Demo template "${DEMO_TEMPLATE_EXTERNAL_ID}" was not found`)
  }

  await ensureDemoTemplatePendingApprovalDecision(request, demoTemplate.id)

  const refreshed = await authorizedGet<{ lifecycleStatus: string; approvalSubState?: string }>(
    request,
    approverToken,
    `/templates/${demoTemplate.id}`,
  )
  if (
    refreshed.lifecycleStatus !== 'APPROVAL' ||
    refreshed.approvalSubState !== 'PENDING_DECISION'
  ) {
    throw new Error(
      `Failed to prepare APPROVAL PENDING_DECISION template for E2E (status=${refreshed.lifecycleStatus}, subState=${refreshed.approvalSubState})`,
    )
  }

  return `/templates/${demoTemplate.id}`
}

async function ensureDemoTemplatePendingRelease(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const approverToken = await apiLogin(request, E2E_TEMPLATE_APPROVER)

  const detail = await authorizedGet<{ lifecycleStatus: string; approvalSubState?: string }>(
    request,
    authorToken,
    `/templates/${templateId}`,
  )

  if (detail.lifecycleStatus === 'PENDING_RELEASE') {
    return
  }

  if (
    detail.lifecycleStatus !== 'APPROVAL' ||
    detail.approvalSubState !== 'PENDING_DECISION'
  ) {
    await ensureDemoTemplatePendingApprovalDecision(request, templateId)
  }

  const beforeApproval = await authorizedGet<{ lifecycleStatus: string; approvalSubState?: string }>(
    request,
    approverToken,
    `/templates/${templateId}`,
  )

  if (
    beforeApproval.lifecycleStatus === 'APPROVAL' &&
    beforeApproval.approvalSubState === 'PENDING_DECISION'
  ) {
    await authorizedPost(request, approverToken, `/templates/${templateId}/lifecycle/approval-decision`, {
      decision: 'APPROVED',
      commentSummary: 'E2E approved for team-lead go-live journey',
      keyEvidenceConfirmed: true,
    })
  }

  const afterApproval = await authorizedGet<{ lifecycleStatus: string }>(
    request,
    approverToken,
    `/templates/${templateId}`,
  )

  if (afterApproval.lifecycleStatus !== 'PENDING_RELEASE') {
    throw new Error(
      `Failed to prepare PENDING_RELEASE template for E2E (status=${afterApproval.lifecycleStatus})`,
    )
  }
}

export async function demoPendingReleaseTemplateDetailPath(
  request: APIRequestContext,
): Promise<string> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const templates = await authorizedGet<TemplateSummary[]>(request, groupAdminToken, '/templates')
  const pendingReleaseTemplates = templates
    .filter((template) => template.lifecycleStatus === 'PENDING_RELEASE')
    .sort((left, right) => Date.parse(right.updatedAt ?? '') - Date.parse(left.updatedAt ?? ''))

  if (pendingReleaseTemplates.length > 0) {
    return `/templates/${pendingReleaseTemplates[0].id}`
  }

  const demoTemplate = await findTemplateByExternalId(request, DEMO_TEMPLATE_EXTERNAL_ID)
  if (!demoTemplate) {
    throw new Error(`Demo template "${DEMO_TEMPLATE_EXTERNAL_ID}" was not found`)
  }

  await ensureDemoTemplatePendingRelease(request, demoTemplate.id)

  const refreshed = await authorizedGet<{ lifecycleStatus: string }>(
    request,
    groupAdminToken,
    `/templates/${demoTemplate.id}`,
  )
  if (refreshed.lifecycleStatus !== 'PENDING_RELEASE') {
    throw new Error(
      `Failed to prepare PENDING_RELEASE template for E2E (status=${refreshed.lifecycleStatus})`,
    )
  }

  return `/templates/${demoTemplate.id}`
}

export async function createApprovedContentModule(
  request: APIRequestContext,
  options?: { moduleCode?: string; name?: string; semanticVersion?: string },
): Promise<ApprovedContentModuleFixture> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const approverToken = await apiLogin(request, E2E_TEMPLATE_APPROVER)
  const moduleCode = options?.moduleCode ?? uniqueModuleCode('E2E-MOD')
  const name = options?.name ?? `E2E Content Module ${moduleCode}`
  const semanticVersion = options?.semanticVersion ?? '1.0.0'

  const created = await authorizedPost<ContentModuleDetail>(
    request,
    authorToken,
    '/content-modules',
    {
      moduleCode,
      groupCode: DEMO_GROUP_CODE,
      name,
      description: 'Playwright P14-T01 fixture',
      sharedGroupCodes: [],
      semanticVersion,
      contentStructureJson: '{"blocks":[{"type":"paragraph","text":"E2E clause"}]}',
      changeDescription: 'Initial E2E draft',
    },
  )

  await authorizedPost(
    request,
    authorToken,
    `/content-modules/${created.moduleId}/review/transition`,
    {
      operation: 'SUBMIT_FOR_REVIEW',
      actorRole: 'TEMPLATE_AUTHOR',
      actorId: E2E_TEMPLATE_AUTHOR.username,
      changeDescription: 'Ready for E2E approval',
    },
  )

  await authorizedPost(
    request,
    approverToken,
    `/content-modules/${created.moduleId}/review/transition`,
    {
      operation: 'APPROVE_REVIEW',
      actorRole: 'APPROVER',
      actorId: E2E_TEMPLATE_APPROVER.username,
    },
  )

  return {
    moduleId: created.moduleId,
    moduleCode,
    name,
    semanticVersion,
  }
}

export async function createDraftContentModule(
  request: APIRequestContext,
  options?: { moduleCode?: string; name?: string; semanticVersion?: string },
): Promise<ApprovedContentModuleFixture> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const moduleCode = options?.moduleCode ?? uniqueModuleCode('E2E-DRAFT')
  const name = options?.name ?? `E2E Draft Module ${moduleCode}`
  const semanticVersion = options?.semanticVersion ?? '1.0.0'

  const created = await authorizedPost<ContentModuleDetail>(
    request,
    authorToken,
    '/content-modules',
    {
      moduleCode,
      groupCode: DEMO_GROUP_CODE,
      name,
      description: 'Playwright P14-T01 UIUX draft fixture',
      sharedGroupCodes: [],
      semanticVersion,
      contentStructureJson: '{"blocks":[{"type":"paragraph","text":"E2E draft clause"}]}',
      changeDescription: 'Initial E2E draft for UIUX evidence',
    },
  )

  return {
    moduleId: created.moduleId,
    moduleCode,
    name,
    semanticVersion,
  }
}

export async function upsertTemplateContentModuleReference(
  request: APIRequestContext,
  templateId: string,
  referenceKey: string,
  moduleId: string,
  semanticVersion: string,
): Promise<TemplateContentModuleReference> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  return authorizedPut<TemplateContentModuleReference>(
    request,
    token,
    `/templates/${templateId}/content-module-references/${encodeURIComponent(referenceKey)}`,
    {
      referenceKey,
      moduleId,
      semanticVersion,
    },
  )
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

async function publishTemplateThroughLifecycle(
  request: APIRequestContext,
  templateId: string,
  releaseVersion: string,
): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const testerToken = await apiLogin(request, E2E_TEMPLATE_TESTER)
  const approverToken = await apiLogin(request, E2E_TEMPLATE_APPROVER)
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)

  const testDataSet = await authorizedPost<{ testDataSetId: string }>(
    request,
    authorToken,
    `/templates/${templateId}/test-data-sets`,
    {
      name: 'E2E required sample',
      required: true,
      variables: { customerName: 'Alice' },
    },
    201,
  )

  await authorizedPost(request, authorToken, `/templates/${templateId}/previews/test-generate`, {
    variables: { customerName: 'Alice' },
  })

  await authorizedPost(request, authorToken, `/templates/${templateId}/previews/batch-test`, {
    testDataSetIds: [testDataSet.testDataSetId],
  })

  await authorizedPost(request, authorToken, `/templates/${templateId}/lifecycle/submit-test`, {
    commentSummary: 'E2E ready for test',
  })

  await authorizedPost(request, testerToken, `/templates/${templateId}/lifecycle/test-decision`, {
    decision: 'PASSED',
    commentSummary: 'E2E test passed',
    fidelityViewedConfirmed: true,
    coverageViewedConfirmed: true,
    previewViewedConfirmed: true,
  })

  await authorizedPost(request, approverToken, `/templates/${templateId}/lifecycle/approval-decision`, {
    decision: 'APPROVED',
    commentSummary: 'E2E approved',
    keyEvidenceConfirmed: true,
  })

  await authorizedPut(request, groupAdminToken, `/templates/${templateId}/api/policy`, {
    allowedAdGroups: ['RETAIL_API'],
    defaultRouteReleaseVersion: releaseVersion,
    outputFormats: ['DOCX'],
    outputModes: ['SYNC_STREAM'],
    batchEnabled: false,
    maxBatchSize: 10,
    docxEncryptionEnabled: false,
    pdfEncryptionEnabled: false,
  })

  await authorizedPost(request, groupAdminToken, `/templates/${templateId}/lifecycle/publish`, {
    releaseVersion,
  })
}

export async function preparePublishedTemplateWithLockedReference(
  request: APIRequestContext,
): Promise<PublishedTemplateWithReferenceFixture> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const master = await findMasterByName(request, groupAdminToken, DEMO_MASTER_NAME)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found`)
  }

  const module = await createApprovedContentModule(request)
  const externalId = uniqueModuleCode('E2E-CM-PUB')
  const referenceKey = 'E2E_LOCKED_REF'
  const releaseVersion = '1.0.0'

  const createdTemplate = await authorizedPost<{ id: string; externalId: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name: `E2E publish lock ${externalId}`,
      description: 'P14-T01 publish lock Playwright fixture',
      masterId: master.id,
    },
    201,
  )

  await configurePublishableTemplate(request, createdTemplate.id)
  await upsertTemplateContentModuleReference(
    request,
    createdTemplate.id,
    referenceKey,
    module.moduleId,
    module.semanticVersion,
  )
  await publishTemplateThroughLifecycle(request, createdTemplate.id, releaseVersion)

  return {
    templateId: createdTemplate.id,
    externalId,
    moduleCode: module.moduleCode,
    referenceKey,
    semanticVersion: module.semanticVersion,
  }
}

export async function attachReferenceToDemoTemplate(
  request: APIRequestContext,
  module: ApprovedContentModuleFixture,
  referenceKey: string,
): Promise<void> {
  const demoTemplate = await findTemplateByExternalId(request, DEMO_TEMPLATE_EXTERNAL_ID)
  if (!demoTemplate) {
    throw new Error(`Demo template "${DEMO_TEMPLATE_EXTERNAL_ID}" was not found`)
  }
  await upsertTemplateContentModuleReference(
    request,
    demoTemplate.id,
    referenceKey,
    module.moduleId,
    module.semanticVersion,
  )
}
