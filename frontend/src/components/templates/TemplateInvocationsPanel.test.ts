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
    },
  }
})

vi.mock('@/api/apiPolicy', () => ({
  listInvocations: vi.fn(),
  getInvocationDetail: vi.fn(),
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
  outcome: 'SUCCEEDED',
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
  })

  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(apiPolicyApi.listInvocations).mockReset()
    vi.mocked(apiPolicyApi.getInvocationDetail).mockReset()
  })

  it('loads paginated invocation history on mount', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    expect(apiPolicyApi.listInvocations).toHaveBeenCalledWith('tpl-1', 0, 20, {
      status: undefined,
      invocationKind: undefined,
      requestId: undefined,
    })
    expect(wrapper.text()).toContain('Invocation history')
    expect(wrapper.text()).toContain('inv-1')
    expect(wrapper.text()).toContain('req-abc')
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

  it('applies status and requestId filters', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    const statusSelect = wrapper.findAll('.el-select').at(0)
    const requestInput = wrapper.find('[data-testid="invocation-request-id-filter"] input')

    await statusSelect?.find('.el-select__wrapper').trigger('click')
    await flushPromises()
    const failedOption = Array.from(document.querySelectorAll('.el-select-dropdown__item')).find(
      (item) => item.textContent?.includes('FAILED'),
    )
    await failedOption?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    expect(requestInput.exists()).toBe(true)
    await requestInput.setValue('req-abc')
    await wrapper.find('button.el-button--primary').trigger('click')
    await flushPromises()

    expect(apiPolicyApi.listInvocations).toHaveBeenLastCalledWith('tpl-1', 0, 20, {
      status: 'FAILED',
      invocationKind: undefined,
      requestId: 'req-abc',
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
    expect(document.body.textContent).toContain('SUCCEEDED')
    expect(document.body.textContent).not.toContain('variables')
    expect(document.body.textContent).not.toContain('parametersStorage')
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
