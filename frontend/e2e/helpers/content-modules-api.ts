import type { APIRequestContext } from '@playwright/test'
import {
  DEMO_FULL_FLOW_EXTERNAL_ID,
  DEMO_FULL_FLOW_NAME,
  DEMO_GROUP_CODE,
  DEMO_MASTER_NAME,
  DEMO_TEMPLATE_EXTERNAL_ID,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
} from './auth'
import {
  E2E_CATALOG_PAGE_SIZE,
  buildCatalogQuery,
  collectCatalogPages,
  findInCatalogPages,
  type CatalogPageView,
} from './catalog-query'
import { E2E_API_BASE_URL, ensureDemoRetailMasterApproved, findMasterByName } from './masters-api'
import { fetchSubmitGateChecklist } from './submit-approval-gate-api'
import { getBatchTestHistoryViaApi, runBatchTestViaApi } from './template-testing-api'
import { cloneReleaseVersion, listTemplateVersionLines } from './template-version-lines-api'

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

async function listTemplates(
  request: APIRequestContext,
  token: string,
  filters: {
    search?: string
    lifecycleStatus?: string
    approvalSubState?: string
    groupCode?: string
  } = {},
): Promise<TemplateSummary[]> {
  return collectCatalogPages<TemplateSummary>(
    (page, size) =>
      authorizedGet<CatalogPageView<TemplateSummary> | TemplateSummary[]>(
        request,
        token,
        `/templates${buildCatalogQuery({ ...filters, page, size })}`,
      ),
    { pageSize: E2E_CATALOG_PAGE_SIZE },
  )
}

export async function findTemplateByExternalId(
  request: APIRequestContext,
  externalId: string,
): Promise<TemplateSummary | undefined> {
  const token = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  return findInCatalogPages<TemplateSummary>(
    (page, size) =>
      authorizedGet<CatalogPageView<TemplateSummary> | TemplateSummary[]>(
        request,
        token,
        `/templates${buildCatalogQuery({ search: externalId, page, size })}`,
      ),
    (template) => template.externalId === externalId,
    { pageSize: E2E_CATALOG_PAGE_SIZE },
  )
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
  const templates = await listTemplates(request, testerToken, { lifecycleStatus: 'TESTING' })
  const testingTemplates = templates
    .filter((template) => template.lifecycleStatus === 'TESTING')
    .sort((left, right) => Date.parse(right.updatedAt ?? '') - Date.parse(left.updatedAt ?? ''))

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
  const templates = await listTemplates(request, approverToken, {
    lifecycleStatus: 'APPROVAL',
    approvalSubState: 'PENDING_DECISION',
  })
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
      fidelityViewedConfirmed: true,
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
  const templates = await listTemplates(request, groupAdminToken, {
    lifecycleStatus: 'PENDING_RELEASE',
  })
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

export async function createAndApproveAdditionalContentModuleVersion(
  request: APIRequestContext,
  moduleId: string,
  semanticVersion: string,
): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const approverToken = await apiLogin(request, E2E_TEMPLATE_APPROVER)

  await authorizedPost(
    request,
    authorToken,
    `/content-modules/${moduleId}/versions`,
    {
      semanticVersion,
      contentStructureJson: '{"blocks":[{"type":"paragraph","text":"E2E clause v2"}]}',
      changeDescription: `E2E version ${semanticVersion}`,
    },
  )

  await authorizedPost(
    request,
    authorToken,
    `/content-modules/${moduleId}/review/transition`,
    {
      operation: 'SUBMIT_FOR_REVIEW',
      actorRole: 'TEMPLATE_AUTHOR',
      actorId: E2E_TEMPLATE_AUTHOR.username,
      changeDescription: `Ready for E2E approval ${semanticVersion}`,
    },
  )

  await authorizedPost(
    request,
    approverToken,
    `/content-modules/${moduleId}/review/transition`,
    {
      operation: 'APPROVE_REVIEW',
      actorRole: 'APPROVER',
      actorId: E2E_TEMPLATE_APPROVER.username,
    },
  )
}

export interface OutdatedClauseReferenceFixture {
  templateId: string
  externalId: string
  inFlightDevVersionId: string
  referenceKey: string
  moduleId: string
  pinnedVersion: string
  latestVersion: string
}

export async function prepareDraftTemplateWithOutdatedClauseReference(
  request: APIRequestContext,
): Promise<OutdatedClauseReferenceFixture> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const master = await ensureDemoRetailMasterApproved(request)
  const module = await createApprovedContentModule(request, { semanticVersion: '1.0.0' })
  const externalId = uniqueModuleCode('E2E-COB')
  const referenceKey = 'E2E_OUTDATED_REF'

  const createdTemplate = await authorizedPost<{ id: string; externalId: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name: `E2E outdated clause ${externalId}`,
      description: 'CE-U07 outdated clause Playwright fixture',
      masterId: master.id,
    },
    201,
  )

  await upsertTemplateContentModuleReference(
    request,
    createdTemplate.id,
    referenceKey,
    module.moduleId,
    '1.0.0',
  )

  await createAndApproveAdditionalContentModuleVersion(request, module.moduleId, '1.1.0')

  const lines = await listTemplateVersionLines(request, createdTemplate.id)
  const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
  if (!inFlight) {
    throw new Error(`No in-flight dev version for template ${createdTemplate.id}`)
  }

  return {
    templateId: createdTemplate.id,
    externalId,
    inFlightDevVersionId: inFlight.devVersionId,
    referenceKey,
    moduleId: module.moduleId,
    pinnedVersion: '1.0.0',
    latestVersion: '1.1.0',
  }
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

export const DEMO_FULL_FLOW_RELEASE_VERSION = '1.0.0'

export interface DemoFullFlowFixture {
  templateId: string
  externalId: string
  name: string
  releaseVersion: string
}

export interface DemoFullFlowTemplateDetail {
  lifecycleStatus: string
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
}

export interface DemoFullFlowApiPolicy {
  policyVersion: number
  defaultRouteReleaseVersion: string
  allowedAdGroups: string[]
  outputFormats: string[]
  outputModes: string[]
}

export interface ManagementInvocationSummary {
  invocationId: string
  invocationKind: string
  status: string
  requestId: string
  resolvedReleaseVersion: string
  routeType: string
  createdAt: string
  accessAccountSummary: string
}

export type DemoFullFlowLifecycleStage =
  | 'DRAFT'
  | 'TESTING'
  | 'APPROVAL_PENDING_DECISION'
  | 'PENDING_RELEASE'
  | 'PUBLISHED'

async function fetchDemoFullFlowTemplateDetail(
  request: APIRequestContext,
  templateId: string,
): Promise<DemoFullFlowTemplateDetail> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  return authorizedGet<DemoFullFlowTemplateDetail>(request, authorToken, `/templates/${templateId}`)
}

export async function fetchDemoFullFlowApiPolicy(
  request: APIRequestContext,
  templateId: string,
): Promise<DemoFullFlowApiPolicy> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  return authorizedGet<DemoFullFlowApiPolicy>(request, groupAdminToken, `/templates/${templateId}/api/policy`)
}

export async function fetchRecentManagementInvocations(
  request: APIRequestContext,
  templateId: string,
  limit = 10,
): Promise<ManagementInvocationSummary[]> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  return authorizedGet<ManagementInvocationSummary[]>(
    request,
    groupAdminToken,
    `/templates/${templateId}/api/invocations/recent?limit=${limit}`,
  )
}

export async function createDemoFullFlowDraftTemplate(
  request: APIRequestContext,
): Promise<DemoFullFlowFixture> {
  const existing = await findTemplateByExternalId(request, DEMO_FULL_FLOW_EXTERNAL_ID)
  if (existing) {
    if (existing.lifecycleStatus === 'DRAFT') {
      await configurePublishableTemplate(request, existing.id)
    }
    return {
      templateId: existing.id,
      externalId: DEMO_FULL_FLOW_EXTERNAL_ID,
      name: DEMO_FULL_FLOW_NAME,
      releaseVersion: DEMO_FULL_FLOW_RELEASE_VERSION,
    }
  }

  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const master = await ensureDemoRetailMasterApproved(request)

  const createdTemplate = await authorizedPost<{ id: string; externalId: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId: DEMO_FULL_FLOW_EXTERNAL_ID,
      groupCode: DEMO_GROUP_CODE,
      name: DEMO_FULL_FLOW_NAME,
      description: 'Full lifecycle demo template for E2E and manual walkthrough',
      masterId: master.id,
    },
    201,
  )

  await configurePublishableTemplate(request, createdTemplate.id)

  return {
    templateId: createdTemplate.id,
    externalId: DEMO_FULL_FLOW_EXTERNAL_ID,
    name: DEMO_FULL_FLOW_NAME,
    releaseVersion: DEMO_FULL_FLOW_RELEASE_VERSION,
  }
}

async function advanceDemoFullFlowToTesting(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const detail = await fetchDemoFullFlowTemplateDetail(request, templateId)
  if (detail.lifecycleStatus === 'TESTING') {
    return
  }
  if (detail.lifecycleStatus !== 'DRAFT') {
    return
  }
  await ensureDemoTemplateSubmittedForTest(request, templateId)
}

async function advanceDemoFullFlowToApprovalPendingDecision(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const detail = await fetchDemoFullFlowTemplateDetail(request, templateId)
  if (
    detail.lifecycleStatus === 'APPROVAL' &&
    detail.approvalSubState === 'PENDING_DECISION'
  ) {
    return
  }
  if (detail.lifecycleStatus === 'PENDING_RELEASE' || detail.lifecycleStatus === 'PUBLISHED') {
    return
  }

  await advanceDemoFullFlowToTesting(request, templateId)
  await ensureDemoTemplatePendingApprovalDecision(request, templateId)
}

async function advanceDemoFullFlowToPendingRelease(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const detail = await fetchDemoFullFlowTemplateDetail(request, templateId)
  if (detail.lifecycleStatus === 'PENDING_RELEASE' || detail.lifecycleStatus === 'PUBLISHED') {
    return
  }

  await advanceDemoFullFlowToApprovalPendingDecision(request, templateId)
  await ensureDemoTemplatePendingRelease(request, templateId)
}

async function advanceDemoFullFlowToPublished(
  request: APIRequestContext,
  templateId: string,
  releaseVersion: string,
): Promise<void> {
  const detail = await fetchDemoFullFlowTemplateDetail(request, templateId)
  if (detail.lifecycleStatus === 'PUBLISHED') {
    return
  }

  if (detail.lifecycleStatus !== 'PENDING_RELEASE') {
    await advanceDemoFullFlowToPendingRelease(request, templateId)
  }

  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
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

  const published = await fetchDemoFullFlowTemplateDetail(request, templateId)
  if (published.lifecycleStatus !== 'PUBLISHED') {
    throw new Error(
      `Failed to publish full-flow demo template (status=${published.lifecycleStatus}, templateId=${templateId})`,
    )
  }
}

export async function ensureDemoFullFlowAtStage(
  request: APIRequestContext,
  stage: DemoFullFlowLifecycleStage,
): Promise<DemoFullFlowFixture> {
  const fixture = await createDemoFullFlowDraftTemplate(request)

  switch (stage) {
    case 'DRAFT':
      break
    case 'TESTING':
      await advanceDemoFullFlowToTesting(request, fixture.templateId)
      break
    case 'APPROVAL_PENDING_DECISION':
      await advanceDemoFullFlowToApprovalPendingDecision(request, fixture.templateId)
      break
    case 'PENDING_RELEASE':
      await advanceDemoFullFlowToPendingRelease(request, fixture.templateId)
      break
    case 'PUBLISHED':
      await advanceDemoFullFlowToPublished(
        request,
        fixture.templateId,
        fixture.releaseVersion,
      )
      break
    default: {
      const exhaustiveStage: never = stage
      throw new Error(`Unsupported full-flow lifecycle stage: ${exhaustiveStage}`)
    }
  }

  return fixture
}

export async function ensureDemoFullFlowPublished(
  request: APIRequestContext,
): Promise<DemoFullFlowFixture> {
  return ensureDemoFullFlowAtStage(request, 'PUBLISHED')
}

export async function demoFullFlowPublishedDetailPath(
  request: APIRequestContext,
): Promise<string> {
  const fixture = await ensureDemoFullFlowPublished(request)
  return `/templates/${fixture.templateId}`
}

export const RUNTIME_API_BASE_URL =
  process.env.E2E_RUNTIME_API_BASE_URL ?? 'http://127.0.0.1:8080/api/dev/v1'

export interface RuntimeCredentialBundle {
  externalId: string
  secret: string
}

export interface CallerInvocationListResult {
  view: string
  items: CallerInvocationSummary[]
  page: number
  size: number
  totalElements: number
}

export interface CallerInvocationSummary {
  invocationId: string
  invocationKind: string
  status: string
  requestId: string
  routeType: string
  batchId?: string | null
  parentInvocationId?: string | null
  itemId?: string | null
}

export interface CallerInvocationDetail {
  summary: CallerInvocationSummary
  parameters: Record<string, unknown>
  childItems: CallerInvocationSummary[]
}

interface CallerInvocationDetailResult {
  invocation: CallerInvocationSummary & {
    parameters?: Record<string, unknown>
    childItems?: CallerInvocationSummary[]
  }
}

function runtimeCredentialHeaders(credential: RuntimeCredentialBundle): Record<string, string> {
  return {
    'X-Api-Credential-Id': credential.externalId,
    'X-Api-Credential-Secret': credential.secret,
    'X-Access-Account': 'e2e-runtime-caller',
  }
}

export async function createTemplateApiCredential(
  request: APIRequestContext,
  templateId: string,
): Promise<RuntimeCredentialBundle> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const created = await authorizedPost<{ externalId: string; secret: string }>(
    request,
    groupAdminToken,
    `/templates/${templateId}/api/credentials`,
    {},
    201,
  )
  return { externalId: created.externalId, secret: created.secret }
}

export async function runtimeGenerateDefault(
  request: APIRequestContext,
  templateExternalId: string,
  credential: RuntimeCredentialBundle,
  idempotencyKey: string,
): Promise<{ status: number; documentId: string | null }> {
  const response = await request.post(
    `${RUNTIME_API_BASE_URL}/templates/${templateExternalId}/default/generate`,
    {
      headers: {
        ...runtimeCredentialHeaders(credential),
        'Content-Type': 'application/json',
      },
      data: {
        output: { format: 'DOCX', mode: 'SYNC_STREAM' },
        variables: { customerName: 'Bob' },
        requestId: `req-${idempotencyKey}`,
        idempotencyKey,
      },
    },
  )
  const documentId =
    response.headers()['documentid'] ??
    response.headers()['documentId'] ??
    response.headers()['Document-Id'] ??
    null
  return { status: response.status(), documentId }
}

export async function fetchCallerInvocations(
  request: APIRequestContext,
  templateExternalId: string,
  credential: RuntimeCredentialBundle,
  view: 'logical' | 'flat' = 'logical',
): Promise<CallerInvocationListResult> {
  const response = await request.get(
    `${RUNTIME_API_BASE_URL}/templates/${templateExternalId}/invocations?view=${view}`,
    { headers: runtimeCredentialHeaders(credential) },
  )
  if (!response.ok()) {
    throw new Error(
      `GET runtime invocations failed (${response.status()}): ${await response.text()}`,
    )
  }
  const body = (await response.json()) as ApiEnvelope<CallerInvocationListResult>
  return body.result
}

export async function fetchCallerInvocationDetail(
  request: APIRequestContext,
  templateExternalId: string,
  credential: RuntimeCredentialBundle,
  invocationId: string,
): Promise<CallerInvocationDetail> {
  const response = await request.get(
    `${RUNTIME_API_BASE_URL}/templates/${templateExternalId}/invocations/${invocationId}`,
    { headers: runtimeCredentialHeaders(credential) },
  )
  if (!response.ok()) {
    throw new Error(
      `GET runtime invocation detail failed (${response.status()}): ${await response.text()}`,
    )
  }
  const body = (await response.json()) as ApiEnvelope<CallerInvocationDetailResult>
  const invocation = body.result.invocation
  const { parameters, childItems, ...summary } = invocation
  return {
    summary: summary as CallerInvocationSummary,
    parameters: parameters ?? {},
    childItems: childItems ?? [],
  }
}

export interface ManagementRoutesSummary {
  templateExternalId: string
  defaultRouteReleaseVersion: string
  defaultGeneratePath: string
  explicitPaths: Array<{ releaseVersion: string; generatePath: string }>
}

export interface ManagementCallerContract {
  paths: string[]
  callableVersions: Array<{ releaseVersion: string; explicitVersionUrl: string }>
}

async function authorizedGetOptional<T>(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
): Promise<T | null> {
  const response = await request.get(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (response.status() === 404) {
    return null
  }
  if (!response.ok()) {
    throw new Error(`GET ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<T>
  return body.result
}

export async function fetchApiPolicyOrNull(
  request: APIRequestContext,
  templateId: string,
): Promise<DemoFullFlowApiPolicy | null> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  return authorizedGetOptional<DemoFullFlowApiPolicy>(
    request,
    groupAdminToken,
    `/templates/${templateId}/api/policy`,
  )
}

export async function fetchManagementRoutesSummary(
  request: APIRequestContext,
  templateId: string,
): Promise<ManagementRoutesSummary> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  return authorizedGet<ManagementRoutesSummary>(
    request,
    groupAdminToken,
    `/templates/${templateId}/api/routes-summary?environment=dev`,
  )
}

export async function fetchManagementCallerContract(
  request: APIRequestContext,
  templateId: string,
): Promise<ManagementCallerContract> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const contract = await authorizedGet<{
    paths: string[]
    callableVersions: Array<{ releaseVersion: string; explicitVersionUrl: string }>
  }>(request, groupAdminToken, `/templates/${templateId}/api/contract?environment=dev`)
  return {
    paths: contract.paths ?? [],
    callableVersions: contract.callableVersions ?? [],
  }
}

export async function createIsolatedTemplatePendingRelease(
  request: APIRequestContext,
  externalIdPrefix = 'E2E-T13',
): Promise<{ templateId: string; externalId: string }> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const master = await ensureDemoRetailMasterApproved(request)
  const externalId = uniqueModuleCode(externalIdPrefix)

  const createdTemplate = await authorizedPost<{ id: string; externalId: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name: `E2E API materialize ${externalId}`,
      description: 'CD-E2E-T13 publish materialize fixture',
      masterId: master.id,
    },
    201,
  )

  await configurePublishableTemplate(request, createdTemplate.id)
  await ensureDemoTemplatePendingRelease(request, createdTemplate.id)

  return { templateId: createdTemplate.id, externalId: createdTemplate.externalId }
}

export async function publishTemplateRelease(
  request: APIRequestContext,
  templateId: string,
  releaseVersion: string,
): Promise<void> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  await authorizedPost(request, groupAdminToken, `/templates/${templateId}/lifecycle/publish`, {
    releaseVersion,
    // CD-E2E-T10: publish is fail-closed without fidelity viewed confirmation
    fidelityViewedConfirmed: true,
  })

  const detail = await fetchDemoFullFlowTemplateDetail(request, templateId)
  if (detail.lifecycleStatus !== 'PUBLISHED') {
    throw new Error(
      `Publish failed for template ${templateId} (status=${detail.lifecycleStatus}, release=${releaseVersion})`,
    )
  }
}

export async function publishSecondReleaseFromClone(
  request: APIRequestContext,
  templateId: string,
  sourceReleaseVersion: string,
  nextReleaseVersion: string,
): Promise<void> {
  await cloneReleaseVersion(request, templateId, sourceReleaseVersion, 201)
  await advanceInFlightDevToPendingRelease(request, templateId)
  await publishTemplateRelease(request, templateId, nextReleaseVersion)
}

async function advanceInFlightDevToPendingRelease(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const testerToken = await apiLogin(request, E2E_TEMPLATE_TESTER)
  const approverToken = await apiLogin(request, E2E_TEMPLATE_APPROVER)

  await authorizedPost<{ testDataSetId: string }>(
    request,
    authorToken,
    `/templates/${templateId}/test-data-sets`,
    {
      name: `E2E second publish sample ${Date.now()}`,
      required: true,
      variables: { customerName: 'Alice' },
    },
    201,
  )

  await authorizedPost(request, authorToken, `/templates/${templateId}/previews/test-generate`, {
    variables: { customerName: 'Alice' },
  })

  const batchRun = await runBatchTestViaApi(request, templateId)
  await waitForBatchTestGatePassed(request, templateId, batchRun.runId)

  await authorizedPost(request, authorToken, `/templates/${templateId}/lifecycle/submit-test`, {
    commentSummary: 'E2E second publish ready for test',
  })

  await authorizedPost(request, testerToken, `/templates/${templateId}/lifecycle/test-decision`, {
    decision: 'PASSED',
    commentSummary: 'E2E second publish test passed',
    fidelityViewedConfirmed: true,
    coverageViewedConfirmed: true,
    previewViewedConfirmed: true,
  })

  const submitGate = await fetchSubmitGateChecklist(request, templateId)
  if (!submitGate.ready) {
    const blockers = submitGate.items
      .filter((item) => item.blocker && !item.ready)
      .map((item) => item.checkCode)
    throw new Error(`Submit-for-approval gate blocked: ${blockers.join(', ') || 'unknown'}`)
  }

  await authorizedPost(request, authorToken, `/templates/${templateId}/lifecycle/submit-approval`, {
    commentSummary: 'E2E second publish ready for approval',
  })

  await authorizedPost(request, approverToken, `/templates/${templateId}/lifecycle/approval-decision`, {
    decision: 'APPROVED',
    commentSummary: 'E2E second publish approved',
    fidelityViewedConfirmed: true,
    keyEvidenceConfirmed: true,
  })

  const detail = await fetchDemoFullFlowTemplateDetail(request, templateId)
  if (detail.lifecycleStatus !== 'PENDING_RELEASE') {
    throw new Error(
      `Failed to advance cloned dev to PENDING_RELEASE (status=${detail.lifecycleStatus})`,
    )
  }
}

async function waitForBatchTestGatePassed(
  request: APIRequestContext,
  templateId: string,
  runId: string,
): Promise<void> {
  const deadline = Date.now() + 300_000
  while (Date.now() < deadline) {
    const history = await getBatchTestHistoryViaApi(request, templateId, 1)
    const latest = history[0]
    if (latest?.runId === runId && latest.status === 'COMPLETED') {
      if (latest.gatePassed !== true) {
        throw new Error(`Batch test completed without gate pass: ${JSON.stringify(latest)}`)
      }
      return
    }
    await new Promise((resolve) => setTimeout(resolve, 2_000))
  }
  throw new Error(`Batch test ${runId} did not complete within timeout`)
}

export async function updateApiPolicyBatchSettings(
  request: APIRequestContext,
  templateId: string,
  batchEnabled: boolean,
  maxBatchSize = 10,
): Promise<DemoFullFlowApiPolicy> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const current = await fetchDemoFullFlowApiPolicy(request, templateId)
  return authorizedPut<DemoFullFlowApiPolicy>(request, groupAdminToken, `/templates/${templateId}/api/policy`, {
    allowedAdGroups: current.allowedAdGroups.length > 0 ? current.allowedAdGroups : ['RETAIL_API'],
    defaultRouteReleaseVersion: current.defaultRouteReleaseVersion,
    outputFormats: current.outputFormats,
    outputModes: current.outputModes.length > 0 ? current.outputModes : ['SYNC_STREAM'],
    batchEnabled,
    maxBatchSize,
    docxEncryptionEnabled: false,
    pdfEncryptionEnabled: false,
  })
}

export async function runtimeBatchGenerateDefault(
  request: APIRequestContext,
  templateExternalId: string,
  credential: RuntimeCredentialBundle,
  idempotencyKey: string,
  itemCount = 3,
): Promise<{ status: number; batchId: string | null }> {
  const items = Array.from({ length: itemCount }, (_, index) => ({
    itemId: `item-${index + 1}`,
    variables: { customerName: `Customer-${index + 1}` },
  }))

  const response = await request.post(
    `${RUNTIME_API_BASE_URL}/templates/${templateExternalId}/default/batch-generate`,
    {
      headers: {
        ...runtimeCredentialHeaders(credential),
        'Content-Type': 'application/json',
      },
      data: {
        output: { format: 'DOCX', mode: 'SYNC_STREAM' },
        items,
        requestId: `req-batch-${idempotencyKey}`,
        idempotencyKey,
      },
    },
  )

  let batchId: string | null = null
  if (response.ok()) {
    const body = (await response.json()) as ApiEnvelope<{
      batch?: { batchId?: string }
    }>
    batchId = body.result.batch?.batchId ?? null
  }

  return { status: response.status(), batchId }
}
