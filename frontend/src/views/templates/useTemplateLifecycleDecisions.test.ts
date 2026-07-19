import { computed, defineComponent, ref, type Ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { TemplateDetail } from '@/types/template'
import type { TemplateDetailTab } from '@/views/templates/templateDetailTabs'
import { useTemplateLifecycleGates } from '@/views/templates/useTemplateLifecycleGates'
import { useTemplateLifecycleDecisions } from '@/views/templates/useTemplateLifecycleDecisions'

const routerPush = vi.fn()

vi.mock('@/api/templates', () => ({
  submitForTest: vi.fn(),
  recordTestDecision: vi.fn(),
  submitForApproval: vi.fn(),
  recordApprovalDecision: vi.fn(),
  publishTemplate: vi.fn(),
  fetchPublishGate: vi.fn(),
  getTemplateCoverage: vi.fn(),
  fetchChangeDiff: vi.fn(),
  fetchReleaseVersions: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => ({
    authorTemplates: ref(true),
    decideTests: ref(true),
    decideApprovals: ref(true),
    decideLegalApprovals: ref(false),
    publishTemplates: ref(true),
    stopTemplates: ref(true),
    restoreOrDeprecateTemplates: ref(true),
    deleteTemplates: ref(true),
  }),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
    ElMessageBox: { prompt: vi.fn(), confirm: vi.fn() },
  }
})

function makeReadyChecklist(ready = true) {
  return { templateId: 'tpl-1', ready, blockerCount: ready ? 0 : 1, items: [] }
}

function stubGateApis() {
  vi.mocked(templatesApi.fetchPublishGate).mockResolvedValue(makeReadyChecklist())
  vi.mocked(templatesApi.getTemplateCoverage).mockResolvedValue({
    templateId: 'tpl-1',
    aggregatePercentage: 100,
    belowThreshold: false,
    blockerCodes: [],
    dimensions: [],
    appliedThreshold: { percentage: 80, scope: 'TEMPLATE' },
  } as never)
  vi.mocked(templatesApi.fetchChangeDiff).mockResolvedValue({
    templateId: 'tpl-1',
    baselineReleaseVersion: null,
    candidateVersionId: 'dev-1',
    hasChanges: false,
    totalChangeCount: 0,
    dimensions: [],
  } as never)
  vi.mocked(templatesApi.fetchReleaseVersions).mockResolvedValue([])
}

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
    bindings: [],
    rules: [],
    createdAt: '2026-06-23T10:00:00Z',
    updatedAt: '2026-06-23T10:00:00Z',
    ...overrides,
  }
}

function mountDecisions(
  templateRef: Ref<TemplateDetail | null>,
  pinia: ReturnType<typeof createPinia>,
  activeDetailTab = ref<TemplateDetailTab>('overview'),
) {
  const templateId = computed(() => templateRef.value?.id ?? 'tpl-1')
  const loadTemplate = vi.fn(async () => {})
  const Comp = defineComponent({
    setup() {
      const gates = useTemplateLifecycleGates({ templateId, template: computed(() => templateRef.value) })
      const decisions = useTemplateLifecycleDecisions({
        templateId,
        template: computed(() => templateRef.value),
        isDevEditor: computed(() => false),
        errorMessage: computed(() => ''),
        loadTemplate,
        activeDetailTab,
        gates,
      })
      return { decisions, gates, loadTemplate }
    },
    template: '<div></div>',
  })
  const wrapper = mount(Comp, {
    global: { plugins: [pinia, createI18n({ legacy: false, locale: 'en', messages: { en } }), ElementPlus] },
  })
  return {
    wrapper,
    decisions: (wrapper.vm as { decisions: ReturnType<typeof useTemplateLifecycleDecisions> }).decisions,
    gates: (wrapper.vm as { gates: ReturnType<typeof useTemplateLifecycleGates> }).gates,
    loadTemplate: (wrapper.vm as { loadTemplate: ReturnType<typeof vi.fn> }).loadTemplate,
    activeDetailTab,
  }
}

describe('useTemplateLifecycleDecisions', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    routerPush.mockReset()
    vi.mocked(templatesApi.submitForTest).mockReset()
    vi.mocked(templatesApi.recordTestDecision).mockReset()
    vi.mocked(templatesApi.submitForApproval).mockReset()
    vi.mocked(templatesApi.publishTemplate).mockReset()
    stubGateApis()
  })

  it('BDD-F6-A2-002: handleSubmitForTest calls store and clears comment', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DRAFT' }))
    vi.mocked(templatesApi.submitForTest).mockResolvedValue(templateRef.value)
    const { decisions, wrapper } = mountDecisions(templateRef, pinia)
    decisions.lifecycleComment.value = 'Ready'
    await decisions.handleSubmitForTest('Ship it')
    await flushPromises()
    expect(templatesApi.submitForTest).toHaveBeenCalledWith('tpl-1', { commentSummary: 'Ship it' })
    expect(decisions.lifecycleComment.value).toBe('')
    wrapper.unmount()
  })

  it('BDD-F6-A2-002: handleTestDecision opens fail dialog', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    const { decisions, wrapper } = mountDecisions(templateRef, pinia)
    await decisions.handleTestDecision('FAILED')
    expect(decisions.decisionDialogOpen.value).toBe(true)
    expect(decisions.decisionDialogMode.value).toBe('test-fail')
    wrapper.unmount()
  })

  it('BDD-F6-A2-002: submitLifecycleDecision records test pass and closes dialog', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    vi.mocked(templatesApi.recordTestDecision).mockResolvedValue(templateRef.value)
    const { decisions, wrapper } = mountDecisions(templateRef, pinia)
    decisions.decisionDialogMode.value = 'test-pass'
    decisions.decisionDialogOpen.value = true
    await decisions.submitLifecycleDecision({ commentSummary: 'OK', fidelityViewedConfirmed: true })
    await flushPromises()
    expect(templatesApi.recordTestDecision).toHaveBeenCalled()
    expect(decisions.decisionDialogOpen.value).toBe(false)
    wrapper.unmount()
  })

  it('BDD-CDP-FID-002: submitLifecycleDecision sends fidelityViewedConfirmed on approve', async () => {
    const templateRef = ref(
      makeTemplate({ lifecycleStatus: 'APPROVAL', approvalSubState: 'PENDING_DECISION' }),
    )
    vi.mocked(templatesApi.recordApprovalDecision).mockResolvedValue(templateRef.value)
    const { decisions, wrapper } = mountDecisions(templateRef, pinia)
    decisions.decisionDialogMode.value = 'approval-approve'
    decisions.decisionDialogOpen.value = true
    await decisions.submitLifecycleDecision({
      commentSummary: 'Approved',
      fidelityViewedConfirmed: true,
      keyEvidenceConfirmed: true,
    })
    await flushPromises()
    expect(templatesApi.recordApprovalDecision).toHaveBeenCalledWith('tpl-1', {
      decision: 'APPROVED',
      commentSummary: 'Approved',
      fidelityViewedConfirmed: true,
      keyEvidenceConfirmed: true,
      exceptionIntervention: undefined,
      exceptionReason: undefined,
      secondaryConfirmed: undefined,
    })
    expect(decisions.decisionDialogOpen.value).toBe(false)
    wrapper.unmount()
  })

  it('BDD-F6-A2-002: handleSubmitForApproval opens summary when gate ready', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'APPROVAL', approvalSubState: 'PENDING_SUBMIT' }))
    const { decisions, gates, wrapper } = mountDecisions(templateRef, pinia)
    await flushPromises()
    gates.submitGateChecklist.value = makeReadyChecklist()
    await decisions.handleSubmitForApproval()
    expect(decisions.submitSummaryOpen.value).toBe(true)
    wrapper.unmount()
  })

  it('BDD-F6-A2-002: confirmPublishFromSummary publishes and navigates', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'PENDING_RELEASE' }))
    vi.mocked(templatesApi.publishTemplate).mockResolvedValue(templateRef.value)
    const { decisions, gates, loadTemplate, activeDetailTab, wrapper } = mountDecisions(templateRef, pinia)
    gates.publishVersion.value = '2.0.0'
    decisions.publishSummaryOpen.value = true
    await decisions.confirmPublishFromSummary({ fidelityViewedConfirmed: true })
    await flushPromises()
    expect(templatesApi.publishTemplate).toHaveBeenCalledWith('tpl-1', {
      releaseVersion: '2.0.0',
      fidelityViewedConfirmed: true,
    })
    expect(loadTemplate).toHaveBeenCalled()
    expect(activeDetailTab.value).toBe('releaseVersions')
    wrapper.unmount()
  })
})
