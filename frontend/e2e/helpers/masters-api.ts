import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import type { APIRequestContext } from '@playwright/test'
import {
  DEMO_GROUP_CODE,
  DEMO_MASTER_NAME,
  E2E_ADMIN,
  E2E_GROUP_ADMIN,
} from './auth'
import {
  E2E_CATALOG_PAGE_SIZE,
  buildCatalogQuery,
  findInCatalogPages,
  type CatalogPageView,
} from './catalog-query'

export const E2E_API_BASE_URL =
  process.env.E2E_API_BASE_URL ?? 'http://127.0.0.1:8080/api/management/v1'

const FIXTURES_DIR = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', 'fixtures')
export const DEMO_SEED_DOCX_PATH = path.join(FIXTURES_DIR, 'demo-retail-letterhead-seed.docx')
export const REPLACEMENT_DOCX_PATH = path.join(FIXTURES_DIR, 'retail-letterhead-replacement.docx')
export const REPLACEMENT_DOCX_FILENAME = 'retail-letterhead-replacement.docx'
export const DEMO_SEED_DOCX_FILENAME = 'demo-retail-letterhead.docx'

interface MasterSummary {
  id: string
  name: string
  status: string
  originalFilename: string
}

interface MasterDetail extends MasterSummary {
  anchors: Array<{ anchorId: string; displayLabel: string }>
}

interface MasterRevisionLineSummary {
  id: string
  lineLabel: string
  current: boolean
  status?: string
  originalFilename?: string
  updatedAt?: string
}

interface PagedRevisionLines {
  content: MasterRevisionLineSummary[]
  totalElements: number
}

interface ApiEnvelope<T> {
  result: T
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
): Promise<T> {
  const response = await request.post(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
  })
  if (!response.ok()) {
    throw new Error(`POST ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<T>
  return body.result
}

export async function findMasterByName(
  request: APIRequestContext,
  token: string,
  name: string,
): Promise<MasterSummary | undefined> {
  // Server-side search over name ∪ groupCode; paginate so ≥500 catalog seed cannot hide a hit.
  return findInCatalogPages<MasterSummary>(
    (page, size) =>
      authorizedGet<CatalogPageView<MasterSummary> | MasterSummary[]>(
        request,
        token,
        `/masters${buildCatalogQuery({ search: name, page, size })}`,
      ),
    (master) => master.name === name,
    { pageSize: E2E_CATALOG_PAGE_SIZE },
  )
}

async function replaceMasterFile(
  request: APIRequestContext,
  token: string,
  masterId: string,
  filePath: string,
  uploadFilename: string,
): Promise<MasterDetail> {
  const response = await request.put(`${E2E_API_BASE_URL}/masters/${masterId}/file`, {
    headers: { Authorization: `Bearer ${token}` },
    multipart: {
      file: {
        name: uploadFilename,
        mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        buffer: fs.readFileSync(filePath),
      },
    },
  })
  if (!response.ok()) {
    throw new Error(
      `PUT /masters/${masterId}/file failed (${response.status()}): ${await response.text()}`,
    )
  }
  const body = (await response.json()) as ApiEnvelope<MasterDetail>
  return body.result
}

export async function demoMasterDetailPath(request: APIRequestContext): Promise<string> {
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  const master = await findMasterByName(request, token, DEMO_MASTER_NAME)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found`)
  }
  return `/masters/${master.id}`
}

export async function assertDemoCatalogSeeded(request: APIRequestContext): Promise<void> {
  await ensureDemoRetailMasterApproved(request)
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  const master = await findMasterByName(request, token, DEMO_MASTER_NAME)
  if (!master) {
    throw new Error(
      `Demo master "${DEMO_MASTER_NAME}" was not found after ensure step.`,
    )
  }
}

export async function ensureDemoRetailMasterApproved(
  request: APIRequestContext,
): Promise<MasterSummary> {
  if (!fs.existsSync(DEMO_SEED_DOCX_PATH)) {
    throw new Error(
      `Missing seed fixture ${DEMO_SEED_DOCX_PATH}. Run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test`,
    )
  }

  const groupAdminToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const existing = await findMasterByName(request, groupAdminToken, DEMO_MASTER_NAME)
  if (existing?.status === 'APPROVED') {
    return existing
  }

  if (existing) {
    let detail = await authorizedGet<MasterDetail>(request, groupAdminToken, `/masters/${existing.id}`)
    if (detail.status === 'DRAFT' || detail.status === 'REJECTED') {
      detail = await authorizedPost<MasterDetail>(
        request,
        groupAdminToken,
        `/masters/${existing.id}/submit-review`,
        { changeSummary: 'E2E demo retail master submit' },
      )
    }
    if (detail.status === 'PENDING_REVIEW') {
      const adminToken = await apiLogin(request, E2E_ADMIN)
      detail = await authorizedPost<MasterDetail>(
        request,
        adminToken,
        `/masters/${existing.id}/review`,
        { decision: 'APPROVED', commentSummary: 'E2E demo retail master approval' },
      )
    }
    if (detail.status !== 'APPROVED') {
      throw new Error(`Failed to approve demo master (status=${detail.status})`)
    }
    return detail
  }

  const createResponse = await request.post(`${E2E_API_BASE_URL}/masters`, {
    headers: { Authorization: `Bearer ${groupAdminToken}` },
    multipart: {
      file: {
        name: DEMO_SEED_DOCX_FILENAME,
        mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        buffer: fs.readFileSync(DEMO_SEED_DOCX_PATH),
      },
      groupCode: DEMO_GROUP_CODE,
      name: DEMO_MASTER_NAME,
      description: 'Demo retail letterhead for full-lifecycle E2E',
    },
  })
  if (!createResponse.ok()) {
    throw new Error(
      `POST /masters failed (${createResponse.status()}): ${await createResponse.text()}`,
    )
  }
  const created = ((await createResponse.json()) as ApiEnvelope<MasterDetail>).result

  await authorizedPost<MasterDetail>(
    request,
    groupAdminToken,
    `/masters/${created.id}/submit-review`,
    { changeSummary: 'E2E demo retail master initial submit' },
  )

  const adminToken = await apiLogin(request, E2E_ADMIN)
  const approved = await authorizedPost<MasterDetail>(
    request,
    adminToken,
    `/masters/${created.id}/review`,
    { decision: 'APPROVED', commentSummary: 'E2E demo retail master initial approval' },
  )

  if (approved.status !== 'APPROVED') {
    throw new Error(`Failed to create approved demo master (status=${approved.status})`)
  }

  return approved
}

export async function demoMasterRevisionDetailPath(request: APIRequestContext): Promise<string> {
  const hubPath = await demoMasterDetailPath(request)
  const masterId = hubPath.replace('/masters/', '')
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  const page = await authorizedGet<PagedRevisionLines>(
    request,
    token,
    `/masters/${masterId}/revision-lines?page=0&size=1`,
  )
  const currentLine = page.content.find((line) => line.current) ?? page.content[0]
  if (!currentLine) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" has no revision lines`)
  }
  return `/masters/${masterId}/revisions/${currentLine.id}`
}

export async function replaceDemoMasterFileViaApi(
  request: APIRequestContext,
  filePath: string = REPLACEMENT_DOCX_PATH,
  uploadFilename: string = REPLACEMENT_DOCX_FILENAME,
): Promise<MasterDetail> {
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  const master = await findMasterByName(request, token, DEMO_MASTER_NAME)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found`)
  }
  return replaceMasterFile(request, token, master.id, filePath, uploadFilename)
}

export async function listDemoMasterRevisionLines(
  request: APIRequestContext,
): Promise<PagedRevisionLines> {
  const hubPath = await demoMasterDetailPath(request)
  const masterId = hubPath.replace('/masters/', '')
  const token = await apiLogin(request, E2E_GROUP_ADMIN)
  return authorizedGet<PagedRevisionLines>(
    request,
    token,
    `/masters/${masterId}/revision-lines?page=0&size=20`,
  )
}

export async function prepareDemoMasterWithReplaceHistory(
  request: APIRequestContext,
): Promise<{
  masterId: string
  hubPath: string
  currentRevisionPath: string
  historicalRevisionPath: string
}> {
  await restoreDemoMasterToApproved(request, { force: true })
  await replaceDemoMasterFileViaApi(request)

  const hubPath = await demoMasterDetailPath(request)
  const masterId = hubPath.replace('/masters/', '')
  const page = await listDemoMasterRevisionLines(request)

  if (page.totalElements < 2) {
    throw new Error(
      `Expected at least 2 revision lines after replace, got ${page.totalElements}`,
    )
  }

  const currentLine = page.content.find((line) => line.current)
  const historicalCandidates = page.content.filter(
    (line) =>
      !line.current &&
      line.originalFilename === DEMO_SEED_DOCX_FILENAME &&
      line.status === 'APPROVED',
  )
  const historicalLine = historicalCandidates.sort(
    (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt),
  )[0]
  if (!currentLine || !historicalLine) {
    throw new Error('Expected current replacement line and historical seed line after replace')
  }

  return {
    masterId,
    hubPath,
    currentRevisionPath: `/masters/${masterId}/revisions/${currentLine.id}`,
    historicalRevisionPath: `/masters/${masterId}/revisions/${historicalLine.id}`,
  }
}

export async function restoreDemoMasterToApproved(
  request: APIRequestContext,
  options?: { force?: boolean },
): Promise<void> {
  if (!fs.existsSync(DEMO_SEED_DOCX_PATH)) {
    throw new Error(
      `Missing seed fixture ${DEMO_SEED_DOCX_PATH}. Run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test`,
    )
  }

  const operatorToken = await apiLogin(request, E2E_GROUP_ADMIN)
  const master = await findMasterByName(request, operatorToken, DEMO_MASTER_NAME)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found`)
  }

  if (
    !options?.force &&
    master.status === 'APPROVED' &&
    master.originalFilename === DEMO_SEED_DOCX_FILENAME
  ) {
    return
  }

  let detail = await replaceMasterFile(
    request,
    operatorToken,
    master.id,
    DEMO_SEED_DOCX_PATH,
    DEMO_SEED_DOCX_FILENAME,
  )

  if (detail.status === 'DRAFT' || detail.status === 'REJECTED') {
    detail = await authorizedPost<MasterDetail>(
      request,
      operatorToken,
      `/masters/${master.id}/submit-review`,
      { changeSummary: 'E2E restore after master replace test' },
    )
  }

  if (detail.status === 'PENDING_REVIEW') {
    const adminToken = await apiLogin(request, E2E_ADMIN)
    await authorizedPost<MasterDetail>(
      request,
      adminToken,
      `/masters/${master.id}/review`,
      { decision: 'APPROVED', commentSummary: 'E2E restore approval' },
    )
  }
}
