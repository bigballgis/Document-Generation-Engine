import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateReleaseDetailView from '@/views/templates/TemplateReleaseDetailView.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { TemplateDetail } from '@/types/template'

const routerReplace = vi.fn()
const routerPush = vi.fn()
const routeState = {
  params: { templateId: 'tpl-1', releaseVersion: '1.0.0' },
  query: {} as Record<string, string>,
}

const fetchReleaseVersionDetail = vi.fn()
const cloneReleaseVersion = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({ push: routerPush, replace: routerReplace }),
}))

vi.mock('@/stores/templatePanelData', () => ({
  useTemplatePanelDataStore: () => ({
    fetchReleaseVersionDetail,
    cloneReleaseVersion,
  }),
}))

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => ({
    authorTemplates: true,
  }),
}))

vi.mock('@/api/audit', () => ({
  listLifecycleEvents: vi.fn().mockResolvedValue({ events: [], page: 0, size: 50, totalElements: 0, totalPages: 0 }),
}))

vi.mock('@/api/masters', () => ({
  getMaster: vi.fn().mockResolvedValue({ name: 'Corporate letterhead' }),
}))

vi.mock('@/api/templates', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/templates')>()
  return {
    ...actual,
    fetchPublishGate: vi.fn(),
    fetchReleasePublishGate: vi.fn(),
  }
})

const releaseDetail: TemplateDetail = {
  id: 'tpl-1',
  externalId: 'RETAIL-ACCOUNT-OPEN',
  groupCode: 'RETAIL',
  name: 'Retail account open',
  description: 'Published retail package',
  masterId: 'master-1',
  devVersionId: 'dev-1',
  devVersionNumber: 2,
  releaseVersion: '1.0.0',
  lifecycleStatus: 'PUBLISHED',
  approvalSubState: null,
  variables: [{ variableKey: 'customerName', variableType: 'TEXT', required: true }],
  bindings: [
    {
      anchorId: 'body',
      declaredContentType: 'TEXT',
      structuredContentJson: '{}',
      updatedAt: '2026-06-23T10:00:00Z',
    },
  ],
  rules: [{ ruleId: 'rule-1', targetAnchorId: 'body', conditionExpression: 'true' }],
  createdAt: '2026-06-01T10:00:00Z',
  updatedAt: '2026-06-23T10:00:00Z',
  updatedBy: '10000003',
  updatedByDisplayName: 'Template Author',
  readOnly: true,
}

function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(TemplateReleaseDetailView, {
    global: {
      plugins: [pinia, i18n, ElementPlus],
    },
  })
}

describe('TemplateReleaseDetailView', () => {
  beforeEach(() => {
    routeState.query = {}
    routerPush.mockReset()
    routerReplace.mockReset()
    fetchReleaseVersionDetail.mockReset()
    cloneReleaseVersion.mockReset()
    fetchReleaseVersionDetail.mockResolvedValue(releaseDetail)
    vi.mocked(templatesApi.fetchPublishGate).mockReset()
    vi.mocked(templatesApi.fetchReleasePublishGate).mockReset()
    vi.mocked(templatesApi.fetchReleasePublishGate).mockResolvedValue({
      templateId: 'tpl-1',
      ready: true,
      blockerCount: 0,
      items: [
        {
          checkCode: 'APPROVAL_SUMMARY',
          ready: true,
          blocker: true,
          messageKey: 'templates.publishGate.checkCodes.APPROVAL_SUMMARY',
          summary: 'Approval summary',
        },
      ],
    })
  })

  it('renders read-only basics, testing, and approval workspace tabs', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Basics')
    expect(wrapper.text()).toContain('Testing')
    expect(wrapper.text()).toContain('Approval')
    expect(wrapper.text()).toContain('Variables')
    expect(wrapper.text()).toContain('Published release snapshot — read-only.')
    expect(wrapper.text()).toContain('Release 1.0.0')
    expect(wrapper.text()).toContain('Live')
    expect(wrapper.text()).toContain('Retail account open')
    expect(wrapper.text()).toContain('Bindings')
    expect(wrapper.text()).toContain('Rules')
  })

  it('P1-3: Approval tab shows release-scoped publish-gate read-only checklist', async () => {
    routeState.query = { workspaceTab: 'approval' }
    const wrapper = mountView()
    await flushPromises()

    expect(templatesApi.fetchReleasePublishGate).toHaveBeenCalledWith('tpl-1', '1.0.0')
    expect(templatesApi.fetchPublishGate).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Current pre-release checks evaluation')
    expect(wrapper.text()).toContain('Live evaluation of this published release version')
    expect(wrapper.text()).toContain('Approval summary')
    const panel = wrapper.findComponent({ name: 'PublishGateReadOnlyPanel' })
    expect(panel.exists()).toBe(true)
    expect(panel.props('releaseVersion')).toBe('1.0.0')
  })

  it('shows LoadErrorPanel when release detail fetch fails', async () => {
    fetchReleaseVersionDetail.mockRejectedValue(new Error('load failed'))
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.findComponent({ name: 'LoadErrorPanel' }).exists()).toBe(true)
    expect(wrapper.text()).not.toContain('customerName')
  })

  it('uses contained panel page layout', async () => {
    const wrapper = mountView()
    await flushPromises()

    const layout = wrapper.find('.app-page-layout')
    expect(layout.classes()).toContain('app-page-layout--contained')
    expect(layout.classes()).toContain('app-page-layout--panel')
  })
})
