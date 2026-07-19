import type { APIRequestContext } from '@playwright/test'
import {
  DEMO_GROUP_CODE,
  DEMO_MASTER_NAME,
  E2E_ADMIN,
  E2E_CORP_TEMPLATE_AUTHOR,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  FOL_GROUP_CODE,
  FOL_MASTER_NAME,
} from './auth'
import { E2E_API_BASE_URL, findMasterByName } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

export interface StructuredAuthoringFixture {
  templateId: string
  externalId: string
  name: string
}

export const CLEAN_STRUCTURED_CONTENT_JSON = JSON.stringify({
  schemaVersion: '1.0',
  nodes: [
    {
      type: 'paragraph',
      children: [{ type: 'textRun', value: 'Clean binding content' }],
    },
  ],
})

export const IMAGE_SCALING_STRUCTURED_CONTENT_JSON = JSON.stringify({
  schemaVersion: '1.0',
  nodes: [{ type: 'imageRef', imageRef: 'IMG-1', applyScaling: true }],
})

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

async function createDraftTemplate(
  request: APIRequestContext,
  options?: { externalId?: string; name?: string },
): Promise<StructuredAuthoringFixture> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const master = await findMasterByName(request, groupAdminToken, DEMO_MASTER_NAME)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found`)
  }

  const externalId = options?.externalId ?? uniqueExternalId('E2E-P18-T10')
  const name = options?.name ?? `E2E P18-T10 Structured Authoring ${externalId}`

  const created = await authorizedPost<{ id: string; externalId: string; lifecycleStatus: string }>(
    request,
    authorToken,
    '/templates',
    {
      externalId,
      groupCode: DEMO_GROUP_CODE,
      name,
      description: 'P18-T10 controlled structured authoring Playwright fixture',
      masterId: master.id,
      locale: 'en-US',
    },
    201,
  )

  if (created.lifecycleStatus !== 'DRAFT') {
    throw new Error(`Expected DRAFT template, got ${created.lifecycleStatus}`)
  }

  return {
    templateId: created.id,
    externalId,
    name,
  }
}

async function configureCustomerVariable(request: APIRequestContext, templateId: string): Promise<void> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  await authorizedPut(request, authorToken, `/templates/${templateId}/variables/customerName`, {
    variableKey: 'customerName',
    variableType: 'TEXT',
    required: true,
    defaultValue: 'Customer',
    description: 'Customer name',
  })
}

export interface PasteCleaningEvidencePayload {
  transformedCount: number
  removedCount: number
  warningCount: number
  blockedCount: number
  unresolvedPasteBlockers: boolean
  items: Array<{
    category: 'BLOCKED' | 'TRANSFORMED' | 'REMOVED' | 'WARNING'
    messageKey: string
    detectionSummary?: string | null
  }>
}

export interface AnchorBindingResult {
  anchorId: string
  declaredContentType: string
  structuredContentJson: string
  validationStatus: string
  /** CE-U21 concurrency token (ISO-8601 Instant). */
  updatedAt?: string
  pasteCleaningEvidence?: PasteCleaningEvidencePayload | null
}

export interface PublishGateChecklistResult {
  ready: boolean
  items: Array<{
    checkCode: string
    ready: boolean
    blocker: boolean
    messageKey: string
    summary: string
  }>
}

/** Defense-in-depth fixture: unresolved paste residue (BDD S3/S4 inject). */
export const UNRESOLVED_PASTE_CLEANING_EVIDENCE: PasteCleaningEvidencePayload = {
  transformedCount: 0,
  removedCount: 0,
  warningCount: 0,
  blockedCount: 1,
  unresolvedPasteBlockers: true,
  items: [
    {
      category: 'BLOCKED',
      messageKey: 'paste.summary.blocked',
      detectionSummary: 'Blocked embedded object in pasted HTML.',
    },
  ],
}

export const CLEAN_PASTE_CLEANING_EVIDENCE: PasteCleaningEvidencePayload = {
  transformedCount: 1,
  removedCount: 0,
  warningCount: 0,
  blockedCount: 0,
  unresolvedPasteBlockers: false,
  items: [
    {
      category: 'TRANSFORMED',
      messageKey: 'paste.summary.transformed',
      detectionSummary: 'Transformed paragraph element into controlled structured node.',
    },
  ],
}

export async function upsertBindingViaApi(
  request: APIRequestContext,
  templateId: string,
  anchorId: string,
  structuredContentJson: string,
  options?: {
    pasteCleaningEvidence?: PasteCleaningEvidencePayload | null
    clearPasteCleaningEvidence?: boolean
    /** CE-U21 — required when updating an existing binding row. */
    expectedUpdatedAt?: string | null
    credentials?: { username: string; password: string }
  },
): Promise<AnchorBindingResult> {
  const authorToken = await apiLogin(request, options?.credentials ?? E2E_TEMPLATE_AUTHOR)
  const payload: Record<string, unknown> = {
    anchorId,
    declaredContentType: 'TEXT',
    structuredContentJson,
  }
  if (options?.pasteCleaningEvidence) {
    payload.pasteCleaningEvidence = options.pasteCleaningEvidence
  }
  if (options?.clearPasteCleaningEvidence) {
    payload.clearPasteCleaningEvidence = true
  }
  if (options?.expectedUpdatedAt) {
    payload.expectedUpdatedAt = options.expectedUpdatedAt
  }
  return authorizedPut<AnchorBindingResult>(
    request,
    authorToken,
    `/templates/${templateId}/bindings/${anchorId}`,
    payload,
  )
}

export async function getBindingUpdatedAtViaApi(
  request: APIRequestContext,
  templateId: string,
  anchorId: string,
  credentials: { username: string; password: string } = E2E_ADMIN,
): Promise<string> {
  const token = await apiLogin(request, credentials)
  const response = await request.get(`${E2E_API_BASE_URL}/templates/${templateId}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok()) {
    throw new Error(`GET /templates/${templateId} failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<{
    bindings: Array<{ anchorId: string; updatedAt?: string }>
  }>
  const binding = body.result.bindings.find((row) => row.anchorId === anchorId)
  if (!binding?.updatedAt) {
    throw new Error(`Binding ${anchorId} missing updatedAt on template ${templateId}`)
  }
  return binding.updatedAt
}

/**
 * CORP FOL draft with two bound anchors — for CE-U21 per-anchor localDraft isolation (DAC-001/002/005).
 */
export async function prepareDualAnchorFolDraftTemplate(
  request: APIRequestContext,
): Promise<
  StructuredAuthoringFixture & {
    anchorA: string
    anchorB: string
  }
> {
  const corpToken = await apiLogin(request, E2E_CORP_TEMPLATE_AUTHOR)
  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const master = await findMasterByName(request, groupAdminToken, FOL_MASTER_NAME)
  if (!master) {
    throw new Error(
      `FOL master "${FOL_MASTER_NAME}" was not found. Deploy with DOCGEN_IMPORT_FOL_DEMO=true.`,
    )
  }

  const externalId = uniqueExternalId('E2E-CE-U21')
  const name = `E2E CE-U21 Dual Anchor ${externalId}`
  const created = await authorizedPost<{ id: string; externalId: string; lifecycleStatus: string }>(
    request,
    corpToken,
    '/templates',
    {
      externalId,
      groupCode: FOL_GROUP_CODE,
      name,
      description: 'CE-U21 per-anchor draft isolation Playwright fixture',
      masterId: master.id,
      locale: 'en-US',
    },
    201,
  )

  const anchorA = 'FOL_HEADER'
  const anchorB = 'FOL_FACILITY_SUMMARY'
  await upsertBindingViaApi(request, created.id, anchorA, CLEAN_STRUCTURED_CONTENT_JSON, {
    credentials: E2E_CORP_TEMPLATE_AUTHOR,
  })
  await upsertBindingViaApi(request, created.id, anchorB, CLEAN_STRUCTURED_CONTENT_JSON, {
    credentials: E2E_CORP_TEMPLATE_AUTHOR,
  })

  return {
    templateId: created.id,
    externalId: created.externalId,
    name,
    anchorA,
    anchorB,
  }
}

export async function fetchPublishGateViaApi(
  request: APIRequestContext,
  templateId: string,
): Promise<PublishGateChecklistResult> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const response = await request.get(`${E2E_API_BASE_URL}/templates/${templateId}/publish-gate`, {
    headers: { Authorization: `Bearer ${authorToken}` },
  })
  if (!response.ok()) {
    throw new Error(`GET publish-gate failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<PublishGateChecklistResult>
  return body.result
}

export async function validateBindingsViaApi(
  request: APIRequestContext,
  templateId: string,
): Promise<{
  summary: { validCount: number; totalBindings: number; blocking: boolean }
  bindings: Array<{ anchorId: string; validationStatus: string }>
}> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  return authorizedPost(request, authorToken, `/templates/${templateId}/bindings/validate`, {})
}

/** Draft with no bindings — for CE-U19 empty Anchors partition (DRV-005). */
export async function prepareEmptyDraftTemplate(
  request: APIRequestContext,
): Promise<StructuredAuthoringFixture> {
  return createDraftTemplate(request, {
    externalId: uniqueExternalId('E2E-CE-U19'),
    name: `E2E CE-U19 Empty Bindings ${Date.now()}`,
  })
}

export async function prepareDraftTemplateWithCleanBinding(
  request: APIRequestContext,
): Promise<StructuredAuthoringFixture> {
  const fixture = await createDraftTemplate(request, {
    name: `E2E P18-T10 Clean ${Date.now()}`,
  })
  await configureCustomerVariable(request, fixture.templateId)
  await upsertBindingViaApi(request, fixture.templateId, 'HEADER', CLEAN_STRUCTURED_CONTENT_JSON)
  await authorizedPost(
    request,
    await apiLogin(request, E2E_TEMPLATE_AUTHOR),
    `/templates/${fixture.templateId}/bindings/validate`,
    {},
  )
  return fixture
}

export async function prepareDraftTemplateWithImageScalingBinding(
  request: APIRequestContext,
): Promise<StructuredAuthoringFixture> {
  const fixture = await createDraftTemplate(request, {
    name: `E2E P18-T10 Image Scaling ${Date.now()}`,
  })
  await configureCustomerVariable(request, fixture.templateId)
  await upsertBindingViaApi(
    request,
    fixture.templateId,
    'HEADER',
    IMAGE_SCALING_STRUCTURED_CONTENT_JSON,
  )
  await authorizedPost(
    request,
    await apiLogin(request, E2E_TEMPLATE_AUTHOR),
    `/templates/${fixture.templateId}/bindings/validate`,
    {},
  )
  return fixture
}

/**
 * Draft template whose HEADER binding carries unresolved paste-cleaning residue
 * (BDD-OPS-PASTE-BINDING-001 / S3–S5 inject path).
 */
export async function prepareDraftTemplateWithUnresolvedPasteResidue(
  request: APIRequestContext,
): Promise<StructuredAuthoringFixture & { binding: AnchorBindingResult }> {
  const fixture = await createDraftTemplate(request, {
    externalId: uniqueExternalId('E2E-OPS-PASTE'),
    name: `E2E OPS Paste Residue ${Date.now()}`,
  })
  await configureCustomerVariable(request, fixture.templateId)
  const binding = await upsertBindingViaApi(
    request,
    fixture.templateId,
    'HEADER',
    CLEAN_STRUCTURED_CONTENT_JSON,
    { pasteCleaningEvidence: UNRESOLVED_PASTE_CLEANING_EVIDENCE },
  )
  await validateBindingsViaApi(request, fixture.templateId)
  return { ...fixture, binding }
}
