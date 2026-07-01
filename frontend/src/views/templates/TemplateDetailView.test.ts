import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateDetailView from '@/views/templates/TemplateDetailView.vue'
import TemplateDetailLifecycleTab from '@/views/templates/detail/TemplateDetailLifecycleTab.vue'
import TemplateSubmitForApprovalSummaryDialog from '@/components/templates/TemplateSubmitForApprovalSummaryDialog.vue'
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
  submitForApproval: vi.fn(),
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
    masterId: 'master-1',
    lifecycleStatus: 'DRAFT',
    releaseVersion: null,
    approvalSubState: null,
    devVersionId: 'dev-1',
    devVersionNumber: 1,
    bindings: [],
    variables: [],
    rules: [],
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
    vi.mocked(templatesApi.fetchPublishGate).mockReset()
    vi.mocked(templatesApi.getTemplateCoverage).mockReset()
    vi.mocked(templatesApi.fetchChangeDiff).mockReset()
    vi.mocked(templatesApi.submitForApproval).mockReset()
  })

  function mountView(stubLifecycleTab = true) {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(TemplateDetailView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: {
          TemplateAuthorJourneyBlock: true,
          TemplateTesterJourneyBlock: true,
          TemplateApproverJourneyBlock: true,
          TemplateTeamLeadJourneyBlock: true,
          TemplateWorkflowBanner: true,
          TemplateDetailOverviewTab: true,
          TemplateDetailLifecycleTab: stubLifecycleTab,
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

  it('does not show metadata edit for TEMPLATE_AUTHOR (AUD-B04)', async () => {
    vi.mocked(templatesApi.getTemplate).mockResolvedValue(makeTemplate('tpl-b', 'Draft Template') as never)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).not.toContain('Edit metadata')
  })

  it('shows metadata edit for GROUP_ADMIN on editable lifecycle status (AUD-B04)', async () => {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      session: {
        username: '10000002',
        displayName: 'Group Admin',
        email: 'admin@example.com',
        authSource: 'LOCAL',
        roles: ['GROUP_ADMIN'],
        authorizedGroupCodes: ['RETAIL'],
        defaultRoute: 'route.dashboard-home',
        visibleRoutes: ['route.dashboard-home', 'route.template-management'],
        expiresAt: '2099-01-01T00:00:00Z',
        capabilities: {
          publishTemplates: true,
          authorTemplates: true,
        },
      },
    })

    vi.mocked(templatesApi.getTemplate).mockResolvedValue(makeTemplate('tpl-b', 'Draft Template') as never)

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Edit metadata')
  })

  it('shows team-lead journey block for GROUP_ADMIN on PENDING_RELEASE template', async () => {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      session: {
        username: '10000002',
        displayName: 'Group Admin',
        email: 'admin@example.com',
        authSource: 'LOCAL',
        roles: ['GROUP_ADMIN'],
        authorizedGroupCodes: ['RETAIL'],
        defaultRoute: 'route.dashboard-home',
        visibleRoutes: ['route.dashboard-home', 'route.template-management'],
        expiresAt: '2099-01-01T00:00:00Z',
        capabilities: {
          publishTemplates: true,
          reviewMasters: true,
        },
      },
    })

    vi.mocked(templatesApi.getTemplate).mockResolvedValue({
      ...makeTemplate('tpl-b', 'Pending Release Template'),
      lifecycleStatus: 'PENDING_RELEASE',
      releaseVersion: '1.0.0',
    } as never)
    vi.mocked(templatesApi.fetchPublishGate).mockResolvedValue({
      ready: true,
      items: [],
    } as never)
    vi.mocked(templatesApi.getTemplateCoverage).mockResolvedValue({} as never)
    vi.mocked(templatesApi.fetchChangeDiff).mockResolvedValue({} as never)
    vi.mocked(templatesApi.fetchReleaseVersions).mockResolvedValue([])

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.findComponent({ name: 'TemplateTeamLeadJourneyBlock' }).exists()).toBe(true)
  })

  it('loads submit gate with SUBMIT_FOR_APPROVAL phase for pending submit templates', async () => {
    vi.mocked(templatesApi.getTemplate).mockResolvedValue({
      ...makeTemplate('tpl-b', 'Pending Submit Template'),
      lifecycleStatus: 'APPROVAL',
      approvalSubState: 'PENDING_SUBMIT',
    } as never)
    vi.mocked(templatesApi.fetchPublishGate).mockResolvedValue({
      ready: true,
      items: [{ checkCode: 'ANCHOR_INTEGRITY', ready: true, blocker: true, messageKey: '', summary: '' }],
    } as never)
    vi.mocked(templatesApi.getTemplateCoverage).mockResolvedValue({} as never)
    vi.mocked(templatesApi.fetchChangeDiff).mockResolvedValue({} as never)

    mountView()
    await flushPromises()

    expect(templatesApi.fetchPublishGate).toHaveBeenCalledWith('tpl-b', 'SUBMIT_FOR_APPROVAL')
  })

  it('opens submit summary dialog before calling submitForApproval API', async () => {
    routeState.query = { tab: 'lifecycle' }
    vi.mocked(templatesApi.getTemplate).mockResolvedValue({
      ...makeTemplate('tpl-b', 'Pending Submit Template'),
      lifecycleStatus: 'APPROVAL',
      approvalSubState: 'PENDING_SUBMIT',
    } as never)
    vi.mocked(templatesApi.fetchPublishGate).mockResolvedValue({
      ready: true,
      items: [{ checkCode: 'ANCHOR_INTEGRITY', ready: true, blocker: true, messageKey: '', summary: '' }],
    } as never)
    vi.mocked(templatesApi.getTemplateCoverage).mockResolvedValue({} as never)
    vi.mocked(templatesApi.fetchChangeDiff).mockResolvedValue({} as never)

    const wrapper = mountView(false)
    await flushPromises()

    const lifecycleTab = wrapper.findComponent(TemplateDetailLifecycleTab)
    const submitButton = lifecycleTab
      .findAll('button')
      .find((button) => button.text().includes('Submit for approval'))
    await submitButton!.trigger('click')
    await flushPromises()

    const summaryDialog = wrapper.findComponent(TemplateSubmitForApprovalSummaryDialog)
    expect(summaryDialog.props('modelValue')).toBe(true)
    expect(templatesApi.submitForApproval).not.toHaveBeenCalled()
  })

  it('calls submitForApproval after summary dialog confirm', async () => {
    routeState.query = { tab: 'lifecycle' }
    vi.mocked(templatesApi.getTemplate).mockResolvedValue({
      ...makeTemplate('tpl-b', 'Pending Submit Template'),
      lifecycleStatus: 'APPROVAL',
      approvalSubState: 'PENDING_SUBMIT',
    } as never)
    vi.mocked(templatesApi.fetchPublishGate).mockResolvedValue({
      ready: true,
      items: [{ checkCode: 'ANCHOR_INTEGRITY', ready: true, blocker: true, messageKey: '', summary: '' }],
    } as never)
    vi.mocked(templatesApi.getTemplateCoverage).mockResolvedValue({} as never)
    vi.mocked(templatesApi.fetchChangeDiff).mockResolvedValue({} as never)
    vi.mocked(templatesApi.submitForApproval).mockResolvedValue({
      ...makeTemplate('tpl-b', 'Pending Submit Template'),
      lifecycleStatus: 'APPROVAL',
      approvalSubState: 'PENDING_DECISION',
    } as never)

    const wrapper = mountView(false)
    await flushPromises()

    const lifecycleTab = wrapper.findComponent(TemplateDetailLifecycleTab)
    const submitButton = lifecycleTab
      .findAll('button')
      .find((button) => button.text().includes('Submit for approval'))
    await submitButton!.trigger('click')
    await flushPromises()

    const summaryDialog = wrapper.findComponent(TemplateSubmitForApprovalSummaryDialog)
    await summaryDialog.vm.$emit('confirm')
    await flushPromises()

    expect(templatesApi.submitForApproval).toHaveBeenCalledWith('tpl-b', { commentSummary: '' })
  })
})
