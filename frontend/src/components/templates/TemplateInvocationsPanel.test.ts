import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateInvocationsPanel from '@/components/templates/TemplateInvocationsPanel.vue'
import en from '@/i18n/locales/en'
import * as apiPolicyApi from '@/api/apiPolicy'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import { ElMessage } from 'element-plus'

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
  }
})

vi.mock('@/api/apiPolicy', () => ({
  listInvocations: vi.fn(),
  getInvocationDetail: vi.fn(),
  exportInvocationsCsv: vi.fn(),
}))

vi.mock('@/utils/downloadExport', () => ({
  downloadBlobExport: vi.fn(),
}))

const sampleRow = {
  invocationId: 'inv-1',
  invocationKind: 'SINGLE',
  status: 'SUCCEEDED',
  requestId: 'req-abc',
  resolvedReleaseVersion: '1.0.0',
  routeType: 'DEFAULT',
  createdAt: '2026-06-23T10:00:00Z',
  accessAccountSummary: 'svc***',
}

const sampleDetail = {
  invocationId: 'inv-1',
  requestId: 'req-abc',
  routeType: 'DEFAULT',
  resolvedReleaseVersion: '1.0.0',
  outcome: 'SUCCESS',
  durationMs: 120,
  accessAccountSummary: 'svc***',
  credentialId: 'cred-1',
  batchId: null,
  parentInvocationId: null,
  createdAt: '2026-06-23T10:00:00Z',
  documentPresent: true,
  auditLinkHint: {
    requestId: 'req-abc',
    auditId: 'audit-1',
  },
}

const failedDetail = {
  ...sampleDetail,
  invocationId: 'inv-fail',
  outcome: 'FAILURE',
  errorCode: 'REQUEST_BODY_INVALID',
  errorCategory: 'RUNTIME',
  errorMessageKey: 'api.error.validation.requestBodyInvalid',
  errorRetryable: false,
  errorMessage: 'Request body is invalid.',
}

function mountPanel() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: 'admin',
      displayName: 'Admin',
      email: 'admin@example.com',
      authSource: 'LOCAL',
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      defaultRoute: ROUTE_KEYS.templateManagement,
      visibleRoutes: [ROUTE_KEYS.templateManagement, ROUTE_KEYS.auditConsole],
      expiresAt: new Date().toISOString(),
    },
  })

  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(TemplateInvocationsPanel, {
    props: { templateId: 'tpl-1' },
    global: {
      plugins: [pinia, i18n, ElementPlus],
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a class="router-link-stub" :data-to="JSON.stringify(to)"><slot /></a>',
        },
      },
    },
    attachTo: document.body,
  })
}

describe('TemplateInvocationsPanel', () => {
  beforeEach(() => {
    vi.mocked(apiPolicyApi.listInvocations).mockResolvedValue({
      content: [sampleRow],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    vi.mocked(apiPolicyApi.getInvocationDetail).mockResolvedValue(sampleDetail)
    vi.mocked(apiPolicyApi.exportInvocationsCsv).mockResolvedValue({
      blob: new Blob(['csv'], { type: 'text/csv' }),
      filename: 'invocations-tpl-1.csv',
      truncated: false,
    })
  })

  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(apiPolicyApi.listInvocations).mockReset()
    vi.mocked(apiPolicyApi.getInvocationDetail).mockReset()
    vi.mocked(apiPolicyApi.exportInvocationsCsv).mockReset()
  })

  it('loads paginated invocation history on mount', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    expect(apiPolicyApi.listInvocations).toHaveBeenCalledWith('tpl-1', 0, 20, {
      status: undefined,
      invocationKind: undefined,
      requestId: undefined,
      resolvedReleaseVersion: undefined,
    })
    expect(wrapper.text()).toContain('Invocation history')
    expect(wrapper.text()).toContain('inv-1')
    expect(wrapper.text()).toContain('req-abc')
    expect(wrapper.text()).toContain('1.0.0')
    expect(wrapper.findAll('[data-testid="copy-invocation-id"]').length).toBeGreaterThan(0)
    expect(wrapper.findAll('[data-testid="copy-request-id"]').length).toBeGreaterThan(0)
  })

  it('copies invocation id to clipboard from table action', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText },
    })

    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.find('[data-testid="copy-invocation-id"]').trigger('click')
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith('inv-1')
    expect(ElMessage.success).toHaveBeenCalled()
  })

  it('applies status, requestId, and releaseVersion filters', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    const statusSelect = wrapper.findAll('.el-select').at(0)
    const requestInput = wrapper.find('[data-testid="invocation-request-id-filter"] input')
    const releaseInput = wrapper.find('[data-testid="invocation-release-version-filter"] input')

    await statusSelect?.find('.el-select__wrapper').trigger('click')
    await flushPromises()
    const failedOption = Array.from(document.querySelectorAll('.el-select-dropdown__item')).find(
      (item) => item.textContent?.includes('Failed'),
    )
    await failedOption?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    expect(requestInput.exists()).toBe(true)
    await requestInput.setValue('req-abc')
    await releaseInput.setValue('1.2.0')
    await wrapper.find('[data-testid="invocation-apply-filters"]').trigger('click')
    await flushPromises()

    expect(apiPolicyApi.listInvocations).toHaveBeenLastCalledWith('tpl-1', 0, 20, {
      status: 'FAILED',
      invocationKind: undefined,
      requestId: 'req-abc',
      resolvedReleaseVersion: '1.2.0',
    })
  })

  it('exports CSV using applied filters', async () => {
    const { downloadBlobExport } = await import('@/utils/downloadExport')
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.find('[data-testid="invocation-release-version-filter"] input').setValue('1.2.0')
    await wrapper.find('[data-testid="invocation-apply-filters"]').trigger('click')
    await flushPromises()
    await wrapper.find('[data-testid="invocation-export-csv"]').trigger('click')
    await flushPromises()

    expect(apiPolicyApi.exportInvocationsCsv).toHaveBeenCalledWith('tpl-1', {
      status: undefined,
      invocationKind: undefined,
      requestId: undefined,
      resolvedReleaseVersion: '1.2.0',
    })
    expect(downloadBlobExport).toHaveBeenCalled()
    expect(ElMessage.success).toHaveBeenCalled()
  })

  it('exports CSV with applied filters only (ignores draft-only release version)', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.find('[data-testid="invocation-release-version-filter"] input').setValue('1.2.0')
    await wrapper.find('[data-testid="invocation-export-csv"]').trigger('click')
    await flushPromises()

    expect(apiPolicyApi.exportInvocationsCsv).toHaveBeenCalledWith('tpl-1', {
      status: undefined,
      invocationKind: undefined,
      requestId: undefined,
      resolvedReleaseVersion: undefined,
    })
  })

  it('opens summary drawer on row click without parameter payload', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    const row = wrapper.find('.el-table__row')
    await row.trigger('click')
    await flushPromises()

    expect(apiPolicyApi.getInvocationDetail).toHaveBeenCalledWith('tpl-1', 'inv-1')
    expect(document.body.textContent).toContain('Invocation summary')
    expect(document.body.textContent).toContain('SUCCESS')
    expect(document.body.textContent).not.toContain('variables')
    expect(document.body.textContent).not.toContain('parametersStorage')
    expect(document.body.querySelector('[data-testid="invocation-error-envelope"]')).toBeNull()
  })

  it('shows error envelope for failed invocations', async () => {
    vi.mocked(apiPolicyApi.getInvocationDetail).mockResolvedValue(failedDetail)
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.find('.el-table__row').trigger('click')
    await flushPromises()

    expect(document.body.querySelector('[data-testid="invocation-error-envelope"]')).toBeTruthy()
    expect(document.body.textContent).toContain('REQUEST_BODY_INVALID')
    expect(document.body.textContent).toContain('RUNTIME')
    expect(document.body.textContent).toContain('api.error.validation.requestBodyInvalid')
    expect(document.body.textContent).not.toContain('parametersStorage')
    expect(document.body.textContent).not.toContain('"variables"')
  })

  it('shows error envelope placeholders for legacy failed rows without envelope fields', async () => {
    vi.mocked(apiPolicyApi.getInvocationDetail).mockResolvedValue({
      ...sampleDetail,
      outcome: 'FAILURE',
      errorCode: null,
      errorCategory: null,
      errorMessageKey: null,
      errorRetryable: null,
      errorMessage: null,
    })
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.find('.el-table__row').trigger('click')
    await flushPromises()

    const envelope = document.body.querySelector('[data-testid="invocation-error-envelope"]')
    expect(envelope).toBeTruthy()
    expect(envelope?.textContent).toContain('Error details')
    expect(document.body.querySelector('[data-testid="invocation-error-code"]')?.textContent).toBe(
      '—',
    )
    expect(document.body.textContent).not.toContain('REQUEST_BODY_INVALID')
  })

  it('shows audit console deep link with requestId query', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.find('.el-table__row').trigger('click')
    await flushPromises()

    const auditLink = Array.from(document.body.querySelectorAll('a')).find((anchor) =>
      anchor.textContent?.includes('View in activity log'),
    )
    expect(auditLink).toBeTruthy()
    expect(auditLink?.getAttribute('data-to')).toContain('req-abc')
  })
})
