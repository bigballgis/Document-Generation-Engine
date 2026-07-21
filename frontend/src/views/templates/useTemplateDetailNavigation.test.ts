import { computed, defineComponent, ref, type Ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import type { PreviewRecord, TemplateDetail } from '@/types/template'
import type { TemplateDetailTab } from '@/views/templates/templateDetailTabs'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'
import {
  useTemplateDetailNavigation,
  type TemplateDetailNavigationLifecycleDeps,
  type TemplateDetailNavigationPolicyDeps,
  type UseTemplateDetailNavigationOptions,
} from '@/views/templates/useTemplateDetailNavigation'
import { useCollaborationStore } from '@/stores/collaboration'
import { useTemplatesStore } from '@/stores/templates'

const routerPush = vi.fn()
const routerReplace = vi.fn()
const routeQuery = ref<Record<string, string | string[]>>({})
const routeParams = ref<{ templateId: string; devVersionId?: string }>({ templateId: 'tpl-1' })

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: routeParams.value,
    query: routeQuery.value,
  }),
  useRouter: () => ({ push: routerPush, replace: routerReplace }),
}))

vi.mock('@/api/templates', () => ({
  fetchDevVersionDetail: vi.fn(),
}))

const capabilityRefs = {
  authorTemplates: ref(true),
  decideTests: ref(true),
  decideApprovals: ref(true),
  publishTemplates: ref(true),
  reviewMasters: ref(true),
  context: ref({ roles: ['DOCUMENT_AUTHOR'] }),
}

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => capabilityRefs,
}))

vi.mock('@/auth/roles', () => ({
  canViewCollaborationWorkItems: () => true,
}))

const sessionRoles = ref<string[]>(['DOCUMENT_AUTHOR'])

vi.mock('@/stores/session', () => ({
  useSessionStore: () => ({
    session: computed(() => ({ roles: sessionRoles.value })),
  }),
}))

function makeTemplate(overrides: Partial<TemplateDetail> = {}): TemplateDetail {
  return {
    id: 'tpl-1',
    externalId: 'TPL-001',
    groupCode: 'RETAIL',
    name: 'Test template',
    description: null,
    masterId: 'master-1',
    lifecycleStatus: 'DRAFT',
    releaseVersion: null,
    devVersionId: 'dev-1',
    devVersionNumber: 1,
    variables: [],
    bindings: [
      {
        anchorId: 'a1',
        declaredContentType: 'TEXT',
        structuredContentJson: null,
        updatedAt: '2026-06-23T10:00:00Z',
      },
    ],
    rules: [],
    createdAt: '2026-06-23T10:00:00Z',
    updatedAt: '2026-06-23T10:00:00Z',
    ...overrides,
  }
}

function makeLifecycleDeps(
  overrides: Partial<TemplateDetailNavigationLifecycleDeps> = {},
): TemplateDetailNavigationLifecycleDeps {
  return {
    publishGateReady: computed(() => true),
    submitForTestDialogOpen: ref(false),
    handleSubmitForApproval: vi.fn(async () => {}),
    loadPublishGateData: vi.fn(async () => {}),
    handlePublish: vi.fn(async () => {}),
    resetLifecycleTransientState: vi.fn(),
    ...overrides,
  }
}

function makePolicyDeps(
  overrides: Partial<TemplateDetailNavigationPolicyDeps> = {},
): TemplateDetailNavigationPolicyDeps {
  return {
    showPolicyPanel: computed(() => false),
    loadPolicyData: vi.fn(async () => {}),
    resetPolicyCredentialsTransientState: vi.fn(),
    ...overrides,
  }
}

function mountNavigation(
  templateRef: Ref<TemplateDetail | null>,
  pinia: ReturnType<typeof createPinia>,
  overrides: Partial<Omit<UseTemplateDetailNavigationOptions, 'templateId' | 'template'>> = {},
  isDevEditor = false,
) {
  const templateId = computed(() => templateRef.value?.id ?? 'tpl-1')
  const devVersionId = computed(() => (isDevEditor ? 'dev-v1' : ''))
  const lastPreview = ref<PreviewRecord | null>(null)
  const showAuthoringSection = computed(() => true)
  const loadTemplateHolder = { fn: async () => {} }
  const lifecycle = makeLifecycleDeps(overrides.lifecycle)
  const policy = makePolicyDeps(overrides.policy)
  const handleTestGenerate = vi.fn(async () => {})
  const activeDetailTab = ref<TemplateDetailTab>('overview')
  const activeDevWorkspaceTab = ref<TemplateDevWorkspaceTab>('design')

  const Comp = defineComponent({
    setup() {
      const navigation = useTemplateDetailNavigation({
        isDevEditor: computed(() => isDevEditor),
        templateId,
        devVersionId,
        template: computed(() => templateRef.value),
        lastPreview,
        showAuthoringSection,
        activeDetailTab,
        activeDevWorkspaceTab,
        loadTemplateHolder,
        lifecycle,
        policy,
        handleTestGenerate,
        ...overrides,
      })
      return { navigation }
    },
    template: '<div></div>',
  })

  const wrapper = mount(Comp, {
    global: { plugins: [pinia, createI18n({ legacy: false, locale: 'en', messages: { en } }), ElementPlus] },
  })

  return {
    wrapper,
    navigation: (wrapper.vm as { navigation: ReturnType<typeof useTemplateDetailNavigation> }).navigation,
    lifecycle,
    policy,
    lastPreview,
    handleTestGenerate,
    activeDetailTab,
    store: useTemplatesStore(),
    collaborationStore: useCollaborationStore(),
  }
}

describe('useTemplateDetailNavigation', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    routerPush.mockReset()
    routerReplace.mockReset()
    routeQuery.value = {}
    routeParams.value = { templateId: 'tpl-1' }
    capabilityRefs.authorTemplates.value = true
    capabilityRefs.decideTests.value = true
    capabilityRefs.decideApprovals.value = true
    capabilityRefs.publishTemplates.value = true
    capabilityRefs.reviewMasters.value = true
    sessionRoles.value = ['DOCUMENT_AUTHOR']
    vi.mocked(templatesApi.fetchDevVersionDetail).mockReset()
  })

  it('showAuthorJourney is true for DRAFT template when author capability is granted', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DRAFT' }))
    const { navigation, wrapper } = mountNavigation(templateRef, pinia)
    expect(navigation.showAuthorJourney.value).toBe(true)
    wrapper.unmount()
  })

  it('showAuthorJourney is false when template is DEPRECATED', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DEPRECATED' }))
    const { navigation, wrapper } = mountNavigation(templateRef, pinia)
    expect(navigation.showAuthorJourney.value).toBe(false)
    wrapper.unmount()
  })

  it('showTesterJourney is true when template is TESTING and user can decide tests', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    const { navigation, wrapper } = mountNavigation(templateRef, pinia)
    expect(navigation.showTesterJourney.value).toBe(true)
    wrapper.unmount()
  })

  it('showApproverJourney is true when template is APPROVAL with PENDING_DECISION', () => {
    const templateRef = ref(
      makeTemplate({ lifecycleStatus: 'APPROVAL', approvalSubState: 'PENDING_DECISION' }),
    )
    const { navigation, wrapper } = mountNavigation(templateRef, pinia)
    expect(navigation.showApproverJourney.value).toBe(true)
    wrapper.unmount()
  })

  it('showTeamLeadJourney is true when template is PENDING_RELEASE and user can publish', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'PENDING_RELEASE' }))
    const { navigation, wrapper } = mountNavigation(templateRef, pinia)
    expect(navigation.showTeamLeadJourney.value).toBe(true)
    wrapper.unmount()
  })

  it('authorJourneyContext reflects remediation work items', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DRAFT' }))
    const { navigation, collaborationStore, wrapper } = mountNavigation(templateRef, pinia)
    collaborationStore.workItems = [
      {
        workItemId: 'wi-1',
        templateId: 'tpl-1',
        templateName: 'Test template',
        groupCode: 'RETAIL',
        queue: 'REMEDIATION',
        triggerType: 'SUBMIT_FOR_TEST',
        submitterUserId: '10000001',
        summaryText: 'Fix binding',
        createdAt: '2026-06-23T10:00:00Z',
        ageSeconds: 60,
      },
    ]
    expect(navigation.authorJourneyContext.value?.isRemediation).toBe(true)
    wrapper.unmount()
  })

  it('handleJourneyCheckEvidence marks fidelity and coverage viewed in legacy mode', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    const { navigation, activeDetailTab, wrapper } = mountNavigation(templateRef, pinia)
    navigation.handleJourneyCheckEvidence()
    expect(navigation.testerEvidenceViewed.value.fidelityViewedConfirmed).toBe(true)
    expect(navigation.testerEvidenceViewed.value.coverageViewedConfirmed).toBe(true)
    expect(navigation.testerEvidenceViewed.value.previewViewedConfirmed).toBe(false)
    expect(activeDetailTab.value).toBe('lifecycle')
    wrapper.unmount()
  })

  it('handleJourneyRecordResult marks all tester evidence viewed', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    const { navigation, wrapper } = mountNavigation(templateRef, pinia)
    navigation.handleJourneyRecordResult()
    expect(navigation.testerEvidenceViewed.value.previewViewedConfirmed).toBe(true)
    wrapper.unmount()
  })

  it('handleJourneyApproverRecordDecision marks approver evidence viewed', () => {
    const templateRef = ref(
      makeTemplate({ lifecycleStatus: 'APPROVAL', approvalSubState: 'PENDING_DECISION' }),
    )
    const { navigation, wrapper } = mountNavigation(templateRef, pinia)
    navigation.handleJourneyApproverRecordDecision()
    expect(navigation.approverEvidenceViewed.value.submissionReviewedConfirmed).toBe(true)
    expect(navigation.approverEvidenceViewed.value.keyEvidenceViewedConfirmed).toBe(true)
    wrapper.unmount()
  })

  it('handleJourneySubmitForTest opens submit dialog and testing tab in dev editor', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DRAFT' }))
    const lifecycle = makeLifecycleDeps()
    const { navigation, wrapper } = mountNavigation(
      templateRef,
      pinia,
      { lifecycle },
      true,
    )
    navigation.handleJourneySubmitForTest()
    expect(lifecycle.submitForTestDialogOpen.value).toBe(true)
    expect(routerReplace).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('handleJourneyTrialGenerate opens lifecycle panel and invokes test generate', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DRAFT' }))
    const { navigation, handleTestGenerate, activeDetailTab, wrapper } = mountNavigation(templateRef, pinia)
    await navigation.handleJourneyTrialGenerate()
    expect(handleTestGenerate).toHaveBeenCalled()
    expect(activeDetailTab.value).toBe('lifecycle')
    wrapper.unmount()
  })

  it('syncTabFromRoute sets lifecycle tab when focus=lifecycle query is normalized', () => {
    routeQuery.value = { focus: 'lifecycle', tab: 'overview' }
    const templateRef = ref(makeTemplate())
    const { navigation, activeDetailTab, wrapper } = mountNavigation(templateRef, pinia)
    navigation.syncTabFromRoute()
    expect(activeDetailTab.value).toBe('lifecycle')
    expect(routerReplace).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('backToList navigates to template management in legacy workspace', () => {
    const templateRef = ref(makeTemplate())
    const { navigation, wrapper } = mountNavigation(templateRef, pinia)
    navigation.backToList()
    expect(routerPush).toHaveBeenCalledWith(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
    wrapper.unmount()
  })

  it('loadTemplate sets loadFailed when fetchTemplate throws', async () => {
    const templateRef = ref(makeTemplate())
    const store = useTemplatesStore()
    vi.spyOn(store, 'fetchTemplate').mockRejectedValue(new Error('network'))
    const { navigation, wrapper } = mountNavigation(templateRef, pinia)
    await navigation.loadTemplate()
    expect(navigation.loadFailed.value).toBe(true)
    wrapper.unmount()
  })

  it('resetTransientDetailState clears evidence refs and delegates to lifecycle and policy', () => {
    const templateRef = ref(makeTemplate())
    const lifecycle = makeLifecycleDeps()
    const policy = makePolicyDeps()
    const { navigation, lastPreview, wrapper } = mountNavigation(templateRef, pinia, { lifecycle, policy })
    navigation.testerEvidenceViewed.value = {
      fidelityViewedConfirmed: true,
      coverageViewedConfirmed: true,
      previewViewedConfirmed: true,
    }
    lastPreview.value = {
      previewId: 'p1',
      templateId: 'tpl-1',
      templateVersionId: 'ver-1',
      status: 'SUCCEEDED',
      outputFormat: 'DOCX',
      artifactStorageKey: null,
      pdfArtifactStorageKey: null,
      fidelityWarnings: [],
      previewComparison: null,
      testDataSetId: null,
      createdAt: '2026-06-23T10:00:00Z',
    }
    navigation.resetTransientDetailState()
    expect(navigation.testerEvidenceViewed.value.previewViewedConfirmed).toBe(false)
    expect(lastPreview.value).toBeNull()
    expect(lifecycle.resetLifecycleTransientState).toHaveBeenCalled()
    expect(policy.resetPolicyCredentialsTransientState).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('loadAuthorRemediationWorkItems fetches REMEDIATION queue on mount', async () => {
    const templateRef = ref(makeTemplate())
    const collaborationStore = useCollaborationStore()
    const fetchSpy = vi.spyOn(collaborationStore, 'fetchWorkItems').mockResolvedValue(undefined)
    const { wrapper } = mountNavigation(templateRef, pinia)
    await flushPromises()
    expect(fetchSpy).toHaveBeenCalledWith({ queue: 'REMEDIATION' })
    wrapper.unmount()
  })
})
