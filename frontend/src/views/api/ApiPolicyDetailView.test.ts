import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus, { ElMessageBox } from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ApiPolicyDetailView from '@/views/api/ApiPolicyDetailView.vue'
import en from '@/i18n/locales/en'
import * as apiPolicyApi from '@/api/apiPolicy'
import * as templatesApi from '@/api/templates'

vi.mock('@/api/apiPolicy', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/apiPolicy')>()
  return {
    ...actual,
    getApiPolicy: vi.fn(),
    fetchApiPolicyImpactPreview: vi.fn(),
    saveApiPolicyDomain: vi.fn(),
    listCredentials: vi.fn(),
  }
})

vi.mock('@/api/templates', () => ({
  getTemplate: vi.fn(),
}))

const routerReplace = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { templateId: 'tpl-1' },
    query: { domain: 'OUTPUT_POLICY' },
  }),
  useRouter: () => ({ push: vi.fn(), replace: routerReplace }),
}))

const samplePolicy = {
  templateId: 'tpl-1',
  policyVersion: 2,
  allowedAdGroups: ['APP-DOCGEN-RETAIL'],
  defaultRouteReleaseVersion: '1.0.0',
  outputFormats: ['PDF'],
  outputModes: ['INLINE'],
  batchEnabled: false,
  maxBatchSize: 10,
  docxEncryptionEnabled: false,
  pdfEncryptionEnabled: false,
  updatedAt: '2026-06-25T10:00:00Z',
}

function mockTemplateLoad() {
  vi.mocked(templatesApi.getTemplate).mockResolvedValue({
    id: 'tpl-1',
    externalId: 'EXT-001',
    name: 'Retail statement',
    description: null,
    groupCode: 'RETAIL',
    lifecycleStatus: 'PUBLISHED',
    releaseVersion: '1.0.0',
    approvalSubState: null,
    bindings: [],
    variables: [],
    rules: [],
    createdBy: '10000001',
    updatedBy: '10000001',
    createdAt: '2026-06-25T10:00:00Z',
    updatedAt: '2026-06-25T10:00:00Z',
  } as never)
  vi.mocked(apiPolicyApi.getApiPolicy).mockResolvedValue(samplePolicy)
  vi.mocked(apiPolicyApi.listCredentials).mockResolvedValue([])
}

describe('ApiPolicyDetailView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(apiPolicyApi.getApiPolicy).mockReset()
    vi.mocked(apiPolicyApi.fetchApiPolicyImpactPreview).mockReset()
    vi.mocked(apiPolicyApi.saveApiPolicyDomain).mockReset()
    vi.mocked(apiPolicyApi.listCredentials).mockReset()
    vi.mocked(templatesApi.getTemplate).mockReset()
    routerReplace.mockReset()
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue(undefined as never)
  })

  function mountView() {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    return mount(ApiPolicyDetailView, {
      global: {
        plugins: [createPinia(), i18n, ElementPlus],
      },
    })
  }

  function findButton(wrapper: ReturnType<typeof mount>, label: string) {
    return wrapper.findAll('.action-row .el-button').find((button) => button.text().includes(label))
  }

  it('renders domain navigation and policy version', async () => {
    mockTemplateLoad()

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Output policy')
    expect(wrapper.text()).toContain('v2')
    expect(wrapper.text()).toContain('AD group authorization')
  })

  it('disables save when impact preview reports a hard block', async () => {
    mockTemplateLoad()
    vi.mocked(apiPolicyApi.fetchApiPolicyImpactPreview).mockResolvedValue({
      changedAreas: ['DEFAULT_ROUTE_TARGET'],
      blocking: true,
      warnings: ['api.apimgmt.policyImpact.defaultRouteNotCallable'],
      defaultRouteImpacted: true,
      currentPolicyVersion: 2,
      nextPolicyVersion: 3,
      summaryMessageKey: 'api.apimgmt.policyImpact.blocking',
      contractDiffSummary: 'currentTarget=1.0.0,candidateTarget=9.9.9',
      idempotencyImpactSummary: 'api.apimgmt.policyImpact.idempotencyDefaultRouteGuard',
    })

    const wrapper = mountView()
    await flushPromises()

    await findButton(wrapper, 'Run impact preview')?.trigger('click')
    await flushPromises()

    const saveButton = findButton(wrapper, 'Save domain')
    expect(saveButton?.attributes('disabled')).toBeDefined()
    expect(apiPolicyApi.saveApiPolicyDomain).not.toHaveBeenCalled()
  })

  it('runs preview and saves output domain through the domain endpoint', async () => {
    mockTemplateLoad()
    vi.mocked(apiPolicyApi.fetchApiPolicyImpactPreview).mockResolvedValue({
      changedAreas: ['OUTPUT_POLICY'],
      blocking: false,
      warnings: [],
      defaultRouteImpacted: false,
      currentPolicyVersion: 2,
      nextPolicyVersion: 3,
      summaryMessageKey: 'api.apimgmt.policyImpact.safe',
      contractDiffSummary: null,
      idempotencyImpactSummary: null,
    })
    vi.mocked(apiPolicyApi.saveApiPolicyDomain).mockResolvedValue({
      ...samplePolicy,
      policyVersion: 3,
    })

    const wrapper = mountView()
    await flushPromises()

    await findButton(wrapper, 'Save domain')?.trigger('click')
    await flushPromises()

    expect(apiPolicyApi.fetchApiPolicyImpactPreview).toHaveBeenCalled()
    expect(ElMessageBox.confirm).toHaveBeenCalled()
    expect(apiPolicyApi.saveApiPolicyDomain).toHaveBeenCalledWith('tpl-1', 'OUTPUT_POLICY', {
      outputFormats: ['PDF'],
      outputModes: ['INLINE'],
    }, true)
  })

  it('requires confirmation when impact preview returns warnings', async () => {
    mockTemplateLoad()
    vi.mocked(apiPolicyApi.fetchApiPolicyImpactPreview).mockResolvedValue({
      changedAreas: ['DEFAULT_ROUTE_TARGET'],
      blocking: false,
      warnings: ['api.apimgmt.policyImpact.defaultRouteChanged'],
      defaultRouteImpacted: true,
      currentPolicyVersion: 2,
      nextPolicyVersion: 3,
      summaryMessageKey: 'api.apimgmt.policyImpact.warning',
      contractDiffSummary: 'currentTarget=1.0.0,candidateTarget=2.0.0',
      idempotencyImpactSummary: 'api.apimgmt.policyImpact.idempotencyDefaultRouteGuard',
    })

    const wrapper = mountView()
    await flushPromises()

    await findButton(wrapper, 'Save domain')?.trigger('click')
    await flushPromises()

    expect(ElMessageBox.confirm).toHaveBeenCalled()
  })
})
