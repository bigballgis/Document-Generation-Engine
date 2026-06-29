import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import type { APIRequestContext } from '@playwright/test'
import {
  DEMO_MASTER_NAME,
  E2E_ADMIN,
  E2E_GROUP_ADMIN,
} from './auth'

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
  const masters = await authorizedGet<MasterSummary[]>(request, token, '/masters')
  return masters.find((master) => master.name === name)
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

export async function restoreDemoMasterToApproved(request: APIRequestContext): Promise<void> {
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
