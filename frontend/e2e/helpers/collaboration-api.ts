import { execFileSync } from 'node:child_process'
import type { APIRequestContext } from '@playwright/test'
import {
  DEMO_GROUP_CODE,
  DEMO_MASTER_NAME,
  E2E_ADMIN,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  FOL_GROUP_CODE,
  FOL_MASTER_NAME,
  E2E_CORP_TEMPLATE_AUTHOR,
} from './auth'
import { E2E_API_BASE_URL, findMasterByName } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

interface TemplateSummary {
  id: string
  externalId: string
  lifecycleStatus: string
  groupCode: string
  name: string
}

export interface TestingTemplateFixture {
  templateId: string
  externalId: string
  name: string
  groupCode: string
}

export interface CollaborationWorkItemFixture {
  workItemId: string
  templateId: string
  templateExternalId: string
  templateName: string
  groupCode: string
  queue: 'TEST' | 'APPROVAL' | 'ESCALATION'
  submitterUserId: string
}

export interface CollaborationTimeoutConfigFixture {
  scopeType: 'GLOBAL' | 'GROUP'
  groupCode: string | null
  testThresholdHours: number
  approvalThresholdHours: number
  pendingReleaseThresholdHours: number
  remediationThresholdHours: number
  updatedAt: string
}

const POSTGRES_CONTAINER = process.env.E2E_POSTGRES_CONTAINER ?? 'docgen-postgres'
const POSTGRES_USER = process.env.POSTGRES_USER ?? 'docgen'
const POSTGRES_DB = process.env.POSTGRES_DB ?? 'docgen'

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

function randomUuid(): string {
  return crypto.randomUUID()
}

function execPsql(sql: string): void {
  execFileSync(
    'docker',
    ['exec', POSTGRES_CONTAINER, 'psql', '-U', POSTGRES_USER, '-d', POSTGRES_DB, '-v', 'ON_ERROR_STOP=1', '-c', sql],
    { stdio: 'pipe', encoding: 'utf8' },
  )
}

async function configurePublishableTemplate(
  request: APIRequestContext,
  templateId: string,
  authorToken?: string,
): Promise<void> {
  const token = authorToken ?? (await apiLogin(request, E2E_TEMPLATE_AUTHOR))

  await authorizedPut(request, token, `/templates/${templateId}/variables/customerName`, {
    variableKey: 'customerName',
    variableType: 'TEXT',
    required: true,
    defaultValue: 'Customer',
    description: 'Customer name',
  })

  await authorizedPut(request, token, `/templates/${templateId}/bindings/HEADER`, {
    anchorId: 'HEADER',
    declaredContentType: 'TEXT',
    structuredContentJson:
      '{"nodes":[{"type":"paragraph","children":[{"type":"variable","key":"customerName"}]}]}',
  })

  await authorizedPost(request, token, `/templates/${templateId}/bindings/validate`, {})
}

export async function prepareTemplateInTesting(
  request: APIRequestContext,
  options?: {
    externalId?: string
    name?: string
    masterName?: string
    groupCode?: string
  },
): Promise<TestingTemplateFixture> {
  const masterCandidates = [
    {
      masterName: options?.masterName ?? DEMO_MASTER_NAME,
      groupCode: options?.groupCode ?? DEMO_GROUP_CODE,
    },
  ]
  if (!options?.masterName) {
    masterCandidates.push({ masterName: FOL_MASTER_NAME, groupCode: FOL_GROUP_CODE })
  }

  let lastError: Error | undefined
  for (const candidate of masterCandidates) {
    try {
      return await prepareTemplateInTestingWithMaster(request, candidate.masterName, candidate.groupCode, options)
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error))
    }
  }
  throw lastError ?? new Error('Unable to prepare TESTING template fixture')
}

/** RETAIL demo master only — required when template tester (RETAIL scope) must open the dev editor. */
export async function prepareRetailTemplateInTesting(
  request: APIRequestContext,
  options?: {
    externalId?: string
    name?: string
  },
): Promise<TestingTemplateFixture> {
  return prepareTemplateInTesting(request, {
    ...options,
    masterName: DEMO_MASTER_NAME,
    groupCode: DEMO_GROUP_CODE,
  })
}

async function prepareTemplateInTestingWithMaster(
  request: APIRequestContext,
  masterName: string,
  groupCode: string,
  options?: { externalId?: string; name?: string },
): Promise<TestingTemplateFixture> {
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const authorCredentials =
    groupCode === FOL_GROUP_CODE ? E2E_CORP_TEMPLATE_AUTHOR : E2E_TEMPLATE_AUTHOR
  const authorToken = await apiLogin(request, authorCredentials)
  const master = await findMasterByName(request, groupAdminToken, masterName)
  if (!master) {
    throw new Error(`Master "${masterName}" was not found`)
  }

  const externalId = options?.externalId ?? uniqueExternalId('E2E-COLLAB')
  const name = options?.name ?? `E2E Collaboration ${externalId}`

  const createdTemplate = await authorizedPost<{ id: string; externalId: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId,
      groupCode,
      name,
      description: 'P14-T02 collaboration Playwright fixture',
      masterId: master.id,
      locale: 'en-US',
    },
    201,
  )

  await configurePublishableTemplate(request, createdTemplate.id, authorToken)

  const testDataSet = await authorizedPost<{ testDataSetId: string }>(
    request,
    authorToken,
    `/templates/${createdTemplate.id}/test-data-sets`,
    {
      name: 'E2E collaboration sample',
      required: true,
      variables: { customerName: 'Alice' },
    },
    201,
  )

  await authorizedPost(request, authorToken, `/templates/${createdTemplate.id}/previews/test-generate`, {
    variables: { customerName: 'Alice' },
  })

  await authorizedPost(request, authorToken, `/templates/${createdTemplate.id}/previews/batch-test`, {
    testDataSetIds: [testDataSet.testDataSetId],
  })

  await authorizedPost(request, authorToken, `/templates/${createdTemplate.id}/lifecycle/submit-test`, {
    commentSummary: 'E2E ready for test queue',
  })

  return {
    templateId: createdTemplate.id,
    externalId,
    name,
    groupCode,
  }
}

export async function getTemplateLifecycleStatus(
  request: APIRequestContext,
  templateId: string,
): Promise<string> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const detail = await authorizedGet<TemplateSummary>(request, authorToken, `/templates/${templateId}`)
  return detail.lifecycleStatus
}

function queryOpenTestWorkItemIdsForTemplate(templateId: string): string[] {
  const output = execFileSync(
    'docker',
    [
      'exec',
      POSTGRES_CONTAINER,
      'psql',
      '-U',
      POSTGRES_USER,
      '-d',
      POSTGRES_DB,
      '-v',
      'ON_ERROR_STOP=1',
      '-t',
      '-A',
      '-c',
      `SELECT id::text FROM collaboration_work_item WHERE template_id = '${templateId}'::uuid AND queue_type = 'TEST' AND status = 'OPEN' ORDER BY created_at ASC;`,
    ],
    { stdio: 'pipe', encoding: 'utf8' },
  ).trim()
  return output ? output.split('\n').filter(Boolean) : []
}

function hasOpenEscalationWorkItemForTemplate(templateId: string): boolean {
  const output = execFileSync(
    'docker',
    [
      'exec',
      POSTGRES_CONTAINER,
      'psql',
      '-U',
      POSTGRES_USER,
      '-d',
      POSTGRES_DB,
      '-v',
      'ON_ERROR_STOP=1',
      '-t',
      '-A',
      '-c',
      `SELECT 1 FROM collaboration_work_item WHERE template_id = '${templateId}'::uuid AND queue_type = 'ESCALATION' AND status = 'OPEN' LIMIT 1;`,
    ],
    { stdio: 'pipe', encoding: 'utf8' },
  ).trim()
  return output === '1'
}

export function ageCollaborationWorkItem(workItemId: string, createdAgeInterval: string): void {
  execPsql(`
UPDATE collaboration_work_item
SET created_at = (NOW() AT TIME ZONE 'UTC') - ${createdAgeInterval},
    updated_at = (NOW() AT TIME ZONE 'UTC') - ${createdAgeInterval}
WHERE id = '${workItemId}'::uuid;
`)
}

export function cleanupDuplicateOpenTestWorkItems(templateId: string, keepWorkItemId: string): void {
  execPsql(`
DELETE FROM collaboration_work_item
WHERE template_id = '${templateId}'::uuid
  AND queue_type = 'TEST'
  AND status = 'OPEN'
  AND id <> '${keepWorkItemId}'::uuid;
`)
}

export async function requireOpenTestWorkItemForTemplate(
  request: APIRequestContext,
  template: TestingTemplateFixture,
): Promise<CollaborationWorkItemFixture> {
  const deadline = Date.now() + 15_000
  let workItemIds: string[] = []
  while (Date.now() < deadline) {
    workItemIds = queryOpenTestWorkItemIdsForTemplate(template.templateId)
    if (workItemIds.length > 0) {
      break
    }
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  if (workItemIds.length === 0) {
    throw new Error(`No OPEN TEST work item found for template ${template.templateId}`)
  }
  const primaryId = workItemIds[0]
  if (workItemIds.length > 1) {
    cleanupDuplicateOpenTestWorkItems(template.templateId, primaryId)
  }

  const apiItems = await listCollaborationWorkItems(request, E2E_TEMPLATE_TESTER, { queue: 'TEST' })
  const apiItem = apiItems.find(
    (item) => item.workItemId === primaryId || item.templateName === template.name,
  )

  return {
    workItemId: primaryId,
    templateId: template.templateId,
    templateExternalId: template.externalId,
    templateName: template.name,
    groupCode: template.groupCode,
    queue: 'TEST',
    submitterUserId: apiItem?.submitterUserId ?? E2E_TEMPLATE_AUTHOR.username,
  }
}

export function seedCollaborationWorkItem(options: {
  workItemId?: string
  templateId: string
  templateExternalId: string
  templateName: string
  groupCode?: string
  queue: 'TEST' | 'APPROVAL' | 'ESCALATION'
  triggerType?: string
  submitterUserId?: string
  summaryText?: string
  createdAgeInterval?: string
  sourceWorkItemId?: string | null
}): CollaborationWorkItemFixture {
  const workItemId = options.workItemId ?? randomUuid()
  const groupCode = options.groupCode ?? DEMO_GROUP_CODE
  const submitterUserId = options.submitterUserId ?? E2E_TEMPLATE_AUTHOR.username
  const createdAgeInterval = options.createdAgeInterval ?? "INTERVAL '2 hours'"
  const triggerType =
    options.triggerType ??
    (options.queue === 'ESCALATION'
      ? 'TIMEOUT_ESCALATION'
      : options.queue === 'APPROVAL'
        ? 'SUBMIT_FOR_APPROVAL'
        : 'SUBMIT_FOR_TEST')
  const summaryText =
    options.summaryText ??
    (options.queue === 'ESCALATION'
      ? 'TEST queue to-do exceeded 1 hour threshold'
      : options.queue === 'APPROVAL'
        ? 'Template awaiting approval decision'
        : 'Template submitted for testing')

  const sourceWorkItemIdSql =
    options.sourceWorkItemId === undefined ? 'NULL' : `'${options.sourceWorkItemId}'::uuid`

  execPsql(`
INSERT INTO collaboration_work_item (
  id,
  template_id,
  template_external_id,
  template_name,
  group_code,
  queue_type,
  trigger_type,
  status,
  submitter_user_id,
  summary_text,
  created_at,
  updated_at,
  source_work_item_id
) VALUES (
  '${workItemId}'::uuid,
  '${options.templateId}'::uuid,
  '${options.templateExternalId.replace(/'/g, "''")}',
  '${options.templateName.replace(/'/g, "''")}',
  '${groupCode}',
  '${options.queue}',
  '${triggerType}',
  'OPEN',
  '${submitterUserId}',
  '${summaryText.replace(/'/g, "''")}',
  (NOW() AT TIME ZONE 'UTC') - ${createdAgeInterval},
  (NOW() AT TIME ZONE 'UTC') - ${createdAgeInterval},
  ${sourceWorkItemIdSql}
);
`)

  return {
    workItemId,
    templateId: options.templateId,
    templateExternalId: options.templateExternalId,
    templateName: options.templateName,
    groupCode,
    queue: options.queue,
    submitterUserId,
  }
}

export async function getCollaborationTimeoutConfig(
  request: APIRequestContext,
  actor: { username: string; password: string } = E2E_ADMIN,
  groupCode?: string,
): Promise<CollaborationTimeoutConfigFixture> {
  const token = await apiLogin(request, actor)
  return authorizedGet<CollaborationTimeoutConfigFixture>(
    request,
    token,
    '/collaboration-timeout-config',
    groupCode ? { groupCode } : undefined,
  )
}

export async function upsertCollaborationTimeoutConfig(
  request: APIRequestContext,
  payload: {
    scopeType: 'GLOBAL' | 'GROUP'
    groupCode?: string | null
    testThresholdHours: number
    approvalThresholdHours: number
    pendingReleaseThresholdHours: number
    remediationThresholdHours: number
  },
  actor: { username: string; password: string } = E2E_ADMIN,
): Promise<CollaborationTimeoutConfigFixture> {
  const token = await apiLogin(request, actor)
  return authorizedPut<CollaborationTimeoutConfigFixture>(
    request,
    token,
    '/collaboration-timeout-config',
    payload,
  )
}

export async function listCollaborationWorkItems(
  request: APIRequestContext,
  actor: { username: string; password: string },
  params?: { queue?: string; groupCode?: string },
): Promise<
  Array<{
    workItemId: string
    templateId: string
    templateName: string
    groupCode: string
    queue: string
    submitterUserId: string
    ageSeconds: number
  }>
> {
  const token = await apiLogin(request, actor)
  return authorizedGet(request, token, '/collaboration-work-items', params)
}

export async function waitForEscalationWorkItem(
  request: APIRequestContext,
  templateId: string,
  timeoutMs = 330_000,
): Promise<void> {
  const started = Date.now()
  while (Date.now() - started < timeoutMs) {
    if (hasOpenEscalationWorkItemForTemplate(templateId)) {
      return
    }
    await new Promise((resolve) => setTimeout(resolve, 5_000))
  }
  throw new Error(`Timed out waiting for ESCALATION work item for template ${templateId}`)
}

export async function prepareOverdueTestWorkItem(
  request: APIRequestContext,
): Promise<{ template: TestingTemplateFixture; sourceWorkItem: CollaborationWorkItemFixture }> {
  const template = await prepareTemplateInTesting(request)
  await upsertCollaborationTimeoutConfig(request, {
    scopeType: 'GROUP',
    groupCode: DEMO_GROUP_CODE,
    testThresholdHours: 1,
    approvalThresholdHours: 72,
    pendingReleaseThresholdHours: 48,
    remediationThresholdHours: 168,
  })

  const sourceWorkItem = await requireOpenTestWorkItemForTemplate(request, template)
  ageCollaborationWorkItem(sourceWorkItem.workItemId, "INTERVAL '73 hours'")

  return { template, sourceWorkItem }
}

export async function seedEscalationFromOverdueSource(
  sourceWorkItem: CollaborationWorkItemFixture,
  template: TestingTemplateFixture,
): Promise<CollaborationWorkItemFixture> {
  return seedCollaborationWorkItem({
    templateId: template.templateId,
    templateExternalId: template.externalId,
    templateName: template.name,
    queue: 'ESCALATION',
    triggerType: 'TIMEOUT_ESCALATION',
    sourceWorkItemId: sourceWorkItem.workItemId,
    summaryText: 'TEST queue to-do exceeded 1 hour threshold',
    createdAgeInterval: "INTERVAL '0 minutes'",
  })
}

export { E2E_TEMPLATE_TESTER }
