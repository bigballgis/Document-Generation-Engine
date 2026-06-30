import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateDetailView from '@/views/templates/TemplateDetailView.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'

vi.mock('@/api/templates', () => ({
  getTemplate: vi.fn(),
  fetchPublishGate: vi.fn(),
  getTemplateCoverage: vi.fn(),
  fetchChangeDiff: vi.fn(),
  fetchReleaseVersions: vi.fn(),
}))

vi.mock('@/stores/collaboration', () => ({
  useCollaborationStore: () => ({
    workItems: [],
    fetchWorkItems: vi.fn().mockResolvedValue(undefined),
  }),
}))

const { routeState, routerReplace } = vi.hoisted(() => ({
  routeState: {
    params: { templateId: 'tpl-b' },
    query: {} as Record<string, string | string[] | undefined>,
  },
  routerReplace: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({ push: vi.fn(), replace: routerReplace }),
}))

function makeTemplate(id: string, name: string) {
  return {
    id,
    externalId: `EXT-${id}`,
    name,
    description: null,
    groupCode: 'RETAIL',
    lifecycleStatus: 'DRAFT',
    releaseVersion: null,
    approvalSubState: null,
    bindings: [],
    variables: [],
    rules: [],
    createdBy: '10000003',
    updatedBy: '10000003',
    createdAt: '2026-06-23T10:00:00Z',
    updatedAt: '2026-06-23T10:00:00Z',
  }
}

function patchSession() {
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000003',
      displayName: 'Author',
      email: 'author@example.com',
      authSource: 'LOCAL',
      roles: ['TEMPLATE_AUTHOR'],
      authorizedGroupCodes: ['RETAIL'],
      defaultRoute: 'route.dashboard-home',
      visibleRoutes: ['route.dashboard-home', 'route.template-management'],
      expiresAt: '2099-01-01T00:00:00Z',
    },
  })
}

describe('TemplateDetailView', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    patchSession()
    routeState.params.templateId = 'tpl-b'
    routeState.query = {}
    routerReplace.mockReset()
    vi.mocked(templatesApi.getTemplate).mockReset()
  })

  function mountView() {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(TemplateDetailView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: {
          TemplateAuthorJourneyBlock: true,
          TemplateTesterJourneyBlock: true,
          TemplateWorkflowBanner: true,
          TemplateDetailOverviewTab: true,
          TemplateDetailLifecycleTab: true,
          TemplateDetailAuthoringTab: true,
          TemplateDetailReleaseVersionsTab: true,
          TemplateDetailApiAccessTab: true,
          TemplateMetadataEditDialog: true,
          TemplatePublishSummaryDialog: true,
          TemplateLifecycleDecisionDialog: true,
          TemplateExportActions: true,
        },
      },
    })
  }

  it('does not render stale template data while route template id changes', async () => {
    const store = useTemplatesStore()
    store.selectedTemplate = makeTemplate('tpl-a', 'Template A') as never

    vi.mocked(templatesApi.getTemplate).mockResolvedValue(makeTemplate('tpl-b', 'Template B') as never)

    const wrapper = mountView()
    expect(wrapper.text()).not.toContain('Template A')
    expect(wrapper.find('.el-skeleton').exists()).toBe(true)

    await flushPromises()

    expect(wrapper.text()).toContain('Template B')
    expect(wrapper.text()).not.toContain('Template A')
  })

  it('normalizes focus=lifecycle deep-link to tab query without focus', async () => {
    routeState.query = { focus: 'lifecycle', queue: 'REMEDIATION' }
    vi.mocked(templatesApi.getTemplate).mockResolvedValue(makeTemplate('tpl-b', 'Template B') as never)

    mountView()
    await flushPromises()

    expect(routerReplace).toHaveBeenCalledWith({
      query: { queue: 'REMEDIATION', tab: 'lifecycle' },
    })
  })
})
