/**
 * IBL-E1 (#128) — API fixtures for locale-variant template journeys.
 * Prefix externalIds with E2E- so global-teardown cleans them up.
 */
import { randomUUID } from 'node:crypto'
import type { APIRequestContext } from '@playwright/test'

import { DEMO_GROUP_CODE, DEMO_MASTER_NAME, E2E_TEMPLATE_AUTHOR } from './auth'
import { E2E_API_BASE_URL, findMasterByName } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

export interface LocaleVariantTemplateFixture {
  templateId: string
  externalId: string
  name: string
  locale: string
  localeVariantFamilyId: string
  groupCode: string
}

export interface LocaleVariantSiblingPair {
  familyId: string
  stamp: string
  en: LocaleVariantTemplateFixture
  zh: LocaleVariantTemplateFixture
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

async function authorizedPost<T>(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
  data: unknown,
  expectedStatus = 201,
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

export async function createLocaleVariantTemplate(
  request: APIRequestContext,
  options: {
    locale: string
    localeVariantFamilyId: string
    externalId?: string
    name?: string
    groupCode?: string
  },
): Promise<LocaleVariantTemplateFixture> {
  const authorToken = await apiLogin(request, E2E_TEMPLATE_AUTHOR)
  const master = await findMasterByName(request, authorToken, DEMO_MASTER_NAME)
  if (!master) {
    throw new Error(`Demo master "${DEMO_MASTER_NAME}" was not found`)
  }

  const stamp = Date.now().toString(36).toUpperCase()
  const localeTag = options.locale.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
  const externalId = options.externalId ?? `E2E-IBL-E1-${localeTag}-${stamp}`
  const name = options.name ?? `E2E IBL-E1 ${options.locale} ${stamp}`
  const groupCode = options.groupCode ?? DEMO_GROUP_CODE

  const created = await authorizedPost<{
    id: string
    externalId: string
    name: string
    locale: string
    localeVariantFamilyId?: string | null
    groupCode: string
  }>(request, authorToken, '/templates', {
    externalId,
    groupCode,
    name,
    description: 'IBL-E1 Playwright locale-variant fixture',
    masterId: master.id,
    locale: options.locale,
    localeVariantFamilyId: options.localeVariantFamilyId,
  })

  return {
    templateId: created.id,
    externalId: created.externalId,
    name: created.name,
    locale: created.locale,
    localeVariantFamilyId: created.localeVariantFamilyId ?? options.localeVariantFamilyId,
    groupCode: created.groupCode,
  }
}

/** Seeds an en-US + zh-CN sibling pair in one family for filter + hub nav. */
export async function prepareLocaleVariantSiblingPair(
  request: APIRequestContext,
): Promise<LocaleVariantSiblingPair> {
  const familyId = randomUUID()
  const stamp = Date.now().toString(36).toUpperCase()
  const en = await createLocaleVariantTemplate(request, {
    locale: 'en-US',
    localeVariantFamilyId: familyId,
    externalId: `E2E-IBL-E1-EN-${stamp}`,
    name: `E2E IBL-E1 EN ${stamp}`,
  })
  const zh = await createLocaleVariantTemplate(request, {
    locale: 'zh-CN',
    localeVariantFamilyId: familyId,
    externalId: `E2E-IBL-E1-ZH-${stamp}`,
    name: `E2E IBL-E1 ZH ${stamp}`,
  })
  return { familyId, stamp, en, zh }
}
