import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus, { ElMessage } from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateCallerContractPanel from '@/components/templates/TemplateCallerContractPanel.vue'
import en from '@/i18n/locales/en'
import * as contractApi from '@/api/contract'
import * as templatesApi from '@/api/templates'
import type { CallerContract } from '@/types/contract'
import type { TestDataSet } from '@/types/templatePreview'
import {
  API_CREDENTIAL_ID_PLACEHOLDER,
  IDEMPOTENCY_KEY_PLACEHOLDER,
} from '@/utils/contractCopyableExample'

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
    },
  }
})

vi.mock('@/api/contract', () => ({
  getCallerContract: vi.fn(),
}))

vi.mock('@/api/templates', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/templates')>()
  return {
    ...actual,
    listTestDataSets: vi.fn(),
  }
})

function sampleContract(environment: 'dev' | 'uat' | 'prod' = 'uat'): CallerContract {
  return {
    templateId: 'TPL-1',
    paths: [`/api/${environment}/v1/templates/TPL-1/default/generate`],
    defaultRoute: {
      url: `/api/${environment}/v1/templates/TPL-1/default/generate`,
      currentTargetReleaseVersion: '1.0.0',
      currentTargetStatus: 'PUBLISHED',
      updatedAt: '2026-06-23T00:00:00Z',
      updatedBy: '10000007',
      explicitVersionUrl: `/api/${environment}/v1/templates/TPL-1/versions/1.0.0/generate`,
    },
    apiPolicy: {
      policyVersion: 1,
      updatedAt: '2026-06-23T00:00:00Z',
      updatedBy: '10000007',
      allowedOutputFormats: ['DOCX'],
      allowedOutputModes: ['SYNC_STREAM'],
      batchLimits: { syncMaxItems: 10, asyncMaxItems: 10 },
      encryptionCapabilities: { docxEnabled: false, pdfEnabled: false, permissions: [] },
      adGroupAuthorizationSummary: {
        authorized: true,
        cacheTtlSeconds: 300,
        authorizationScopeSummary: 'ok',
        effectivePolicyDescription: 'ok',
      },
      credentialSummary: null,
    },
    callableVersions: [
      {
        releaseVersion: '1.0.0',
        explicitVersionUrl: `/api/${environment}/v1/templates/TPL-1/versions/1.0.0/generate`,
      },
    ],
    schemas: ['GenerateRequest'],
    errorCodes: [],
    examples: [],
  }
}

function sampleDataSet(id: string, name: string, variables: Record<string, unknown>): TestDataSet {
  return {
    testDataSetId: id,
    templateId: 'tpl-1',
    name,
    description: null,
    variables,
    required: false,
    scenarioName: null,
    coverageTags: [],
    datasetVersion: 1,
    locked: false,
    derivedFromId: null,
    createdAt: '2026-06-23T10:00:00Z',
    updatedAt: '2026-06-23T10:00:00Z',
  }
}

async function mountPanel(environment: 'dev' | 'uat' | 'prod' = 'uat') {
  const pinia = createPinia()
  setActivePinia(pinia)
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  const wrapper = mount(TemplateCallerContractPanel, {
    props: { templateId: 'tpl-1', environment },
    global: {
      plugins: [pinia, i18n, ElementPlus],
    },
    attachTo: document.body,
  })
  await flushPromises()
  return wrapper
}

describe('TemplateCallerContractPanel copyable examples (CE-U12)', () => {
  beforeEach(() => {
    vi.mocked(contractApi.getCallerContract).mockReset()
    vi.mocked(templatesApi.listTestDataSets).mockReset()
    vi.mocked(ElMessage.success).mockReset()
    vi.mocked(ElMessage.error).mockReset()
    vi.mocked(contractApi.getCallerContract).mockImplementation(async (_id, environment) =>
      sampleContract(environment as 'dev' | 'uat' | 'prod'),
    )
    vi.mocked(templatesApi.listTestDataSets).mockResolvedValue([
      sampleDataSet('ds-1', 'Sample customer A', { customerName: 'Acme' }),
      sampleDataSet('ds-2', 'Sample customer B', { customerName: 'Beta Corp' }),
    ])
  })

  it('CCE-001 / FOS-W9-1: renders curl with credential headers, POST, and generate URL', async () => {
    const wrapper = await mountPanel('uat')
    const curl = wrapper.get('[data-testid="contract-example-curl"]').text()

    expect(curl).toContain('curl -X POST')
    expect(curl).toContain(`X-Api-Credential-Id: ${API_CREDENTIAL_ID_PLACEHOLDER}`)
    expect(curl).not.toContain('Authorization: Bearer')
    expect(curl).not.toContain('Idempotency-Key:')
    expect(curl).toContain('/api/uat/v1/templates/TPL-1/default/generate')
    expect(wrapper.text()).not.toContain('generate-sync-docx')
  })

  it('CCE-002: payload reflects selected test data set variables without path fields', async () => {
    const wrapper = await mountPanel()
    const payload = wrapper.get('[data-testid="contract-example-payload"]').text()

    expect(payload).toContain('"customerName": "Acme"')
    expect(payload).not.toContain('templateId')
    expect(payload).not.toContain('releaseVersion')
  })

  it('CCE-003: switching data set updates payload', async () => {
    const wrapper = await mountPanel()
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    const dataset = selects.find((s) => s.attributes('data-testid') === 'contract-example-dataset')
    expect(dataset).toBeTruthy()
    await dataset!.vm.$emit('update:modelValue', 'ds-2')
    await flushPromises()

    const payload = wrapper.get('[data-testid="contract-example-payload"]').text()
    expect(payload).toContain('Beta Corp')
    expect(payload).not.toContain('Acme')
  })

  it('CCE-004: Copy curl writes full curl and shows success feedback', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText },
    })

    const wrapper = await mountPanel()
    const curl = wrapper.get('[data-testid="contract-example-curl"]').text()
    await wrapper.get('[data-testid="contract-copy-curl"]').trigger('click')
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith(curl)
    expect(ElMessage.success).toHaveBeenCalledWith('Copied to clipboard.')
  })

  it('CCE-005: Copy payload writes current JSON without real secrets', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText },
    })

    const wrapper = await mountPanel()
    const payload = wrapper.get('[data-testid="contract-example-payload"]').text().trimEnd()
    await wrapper.get('[data-testid="contract-copy-payload"]').trigger('click')
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith(payload)
    expect(writeText.mock.calls[0]?.[0]).toContain(IDEMPOTENCY_KEY_PLACEHOLDER)
    expect(writeText.mock.calls[0]?.[0]).not.toMatch(/eyJ[A-Za-z0-9_-]+\./)
    expect(ElMessage.success).toHaveBeenCalledWith('Copied to clipboard.')
  })

  it('CCE-006: no test data sets shows empty guidance and keeps curl copyable', async () => {
    vi.mocked(templatesApi.listTestDataSets).mockResolvedValue([])
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText },
    })

    const wrapper = await mountPanel()
    expect(wrapper.get('[data-testid="contract-example-empty-dataset"]').text()).toContain(
      'No test data set selected',
    )
    const payload = wrapper.get('[data-testid="contract-example-payload"]').text()
    expect(payload).toContain('"variables": {}')
    await wrapper.get('[data-testid="contract-copy-curl"]').trigger('click')
    await flushPromises()
    expect(writeText).toHaveBeenCalled()
    expect(writeText.mock.calls[0]?.[0]).toContain(`X-Api-Credential-Id: ${API_CREDENTIAL_ID_PLACEHOLDER}`)
  })

  it('CCE-007: environment switch regenerates curl URL', async () => {
    const wrapper = await mountPanel('uat')
    expect(wrapper.get('[data-testid="contract-example-curl"]').text()).toContain('/api/uat/')

    await wrapper.setProps({ environment: 'prod' })
    await flushPromises()

    expect(vi.mocked(contractApi.getCallerContract)).toHaveBeenCalledWith('tpl-1', 'prod')
    const curl = wrapper.get('[data-testid="contract-example-curl"]').text()
    expect(curl).toContain('/api/prod/v1/templates/TPL-1/default/generate')
    expect(curl).toContain(`X-Api-Credential-Id: ${API_CREDENTIAL_ID_PLACEHOLDER}`)
    expect(curl).not.toContain('Idempotency-Key:')
  })
})
