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
import {
  useTemplateLifecycleActions,
  type UseTemplateLifecycleActionsOptions,
} from '@/views/templates/useTemplateLifecycleActions'

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

const capabilityRefs = {
  authorTemplates: ref(true),
  decideTests: ref(true),
  decideApprovals: ref(true),
  publishTemplates: ref(true),
  stopTemplates: ref(true),
  restoreOrDeprecateTemplates: ref(true),
  deleteTemplates: ref(true),
}

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => capabilityRefs,
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
    ElMessageBox: {
      prompt: vi.fn(),
      confirm: vi.fn(),
    },
  }
})

function makeReadyChecklist(ready = true) {
  return {
    templateId: 'tpl-1',
    ready,
    blockerCount: ready ? 0 : 1,
    items: [],
  }
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

function mountLifecycle(
  templateRef: Ref<TemplateDetail | null>,
  pinia: ReturnType<typeof createPinia>,
  overrides: Partial<Omit<UseTemplateLifecycleActionsOptions, 'templateId' | 'template'>> = {},
  activeDetailTab = ref<TemplateDetailTab>('overview'),
) {
  const templateId = computed(() => templateRef.value?.id ?? 'tpl-1')
  const isDevEditor = ref(false)
  const errorMessage = computed(() => '')
  const loadTemplate = vi.fn(async () => {})

  const Comp = defineComponent({
    setup() {
      const lifecycle = useTemplateLifecycleActions({
        templateId,
        template: computed(() => templateRef.value),
        isDevEditor: computed(() => isDevEditor.value),
        errorMessage,
        loadTemplate,
        activeDetailTab,
        ...overrides,
      })
      return { lifecycle, loadTemplate }
    },
    template: '<div></div>',
  })

  const wrapper = mount(Comp, {
    global: { plugins: [pinia, createI18n({ legacy: false, locale: 'en', messages: { en } }), ElementPlus] },
  })

  return {
    wrapper,
    lifecycle: (wrapper.vm as { lifecycle: ReturnType<typeof useTemplateLifecycleActions> }).lifecycle,
    loadTemplate: (wrapper.vm as { loadTemplate: ReturnType<typeof vi.fn> }).loadTemplate,
    activeDetailTab,
  }
}

describe('useTemplateLifecycleActions', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    routerPush.mockReset()
    capabilityRefs.authorTemplates.value = true
    capabilityRefs.decideTests.value = true
    capabilityRefs.decideApprovals.value = true
    capabilityRefs.publishTemplates.value = true
    capabilityRefs.stopTemplates.value = true
    capabilityRefs.restoreOrDeprecateTemplates.value = true
    capabilityRefs.deleteTemplates.value = true
    vi.mocked(templatesApi.submitForTest).mockReset()
    vi.mocked(templatesApi.recordTestDecision).mockReset()
    vi.mocked(templatesApi.submitForApproval).mockReset()
    vi.mocked(templatesApi.publishTemplate).mockReset()
    vi.mocked(templatesApi.fetchPublishGate).mockReset()
    vi.mocked(templatesApi.getTemplateCoverage).mockReset()
    vi.mocked(templatesApi.fetchChangeDiff).mockReset()
    vi.mocked(templatesApi.fetchReleaseVersions).mockReset()
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
  })

  it('showDraftActions is true when template is DRAFT and author can act', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DRAFT' }))
    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)
    expect(lifecycle.showDraftActions.value).toBe(true)
    wrapper.unmount()
  })

  it('showTestingDecisionActions is true when template is TESTING and tester can decide', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)
    expect(lifecycle.showTestingDecisionActions.value).toBe(true)
    wrapper.unmount()
  })

  it('showDeleteTemplateAction is false when template is DELETED', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DELETED' }))
    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)
    expect(lifecycle.showDeleteTemplateAction.value).toBe(false)
    wrapper.unmount()
  })

  it('handleSubmitForTest calls store and clears lifecycle comment', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DRAFT' }))
    vi.mocked(templatesApi.submitForTest).mockResolvedValue(templateRef.value)

    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)
    lifecycle.lifecycleComment.value = 'Ready for QA'
    await lifecycle.handleSubmitForTest('Ship it')
    await flushPromises()

    expect(templatesApi.submitForTest).toHaveBeenCalledWith('tpl-1', { commentSummary: 'Ship it' })
    expect(lifecycle.lifecycleComment.value).toBe('')
    wrapper.unmount()
  })

  it('handleTestDecision opens fail dialog for FAILED decision', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)

    await lifecycle.handleTestDecision('FAILED')

    expect(lifecycle.decisionDialogOpen.value).toBe(true)
    expect(lifecycle.decisionDialogMode.value).toBe('test-fail')
    wrapper.unmount()
  })

  it('handleTestDecision opens pass dialog for PASSED decision', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)

    await lifecycle.handleTestDecision('PASSED')

    expect(lifecycle.decisionDialogOpen.value).toBe(true)
    expect(lifecycle.decisionDialogMode.value).toBe('test-pass')
    wrapper.unmount()
  })

  it('submitLifecycleDecision records test pass and closes dialog', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    vi.mocked(templatesApi.recordTestDecision).mockResolvedValue(templateRef.value)

    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)
    lifecycle.decisionDialogMode.value = 'test-pass'
    lifecycle.decisionDialogOpen.value = true

    await lifecycle.submitLifecycleDecision({
      commentSummary: 'Looks good',
      fidelityViewedConfirmed: true,
      coverageViewedConfirmed: true,
      previewViewedConfirmed: true,
    })
    await flushPromises()

    expect(templatesApi.recordTestDecision).toHaveBeenCalledWith('tpl-1', {
      decision: 'PASSED',
      commentSummary: 'Looks good',
      fidelityViewedConfirmed: true,
      coverageViewedConfirmed: true,
      previewViewedConfirmed: true,
      exceptionIntervention: undefined,
      exceptionReason: undefined,
      secondaryConfirmed: undefined,
    })
    expect(lifecycle.decisionDialogOpen.value).toBe(false)
    wrapper.unmount()
  })

  it('handleSubmitForApproval opens summary when submit gate is ready', async () => {
    const templateRef = ref(
      makeTemplate({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_SUBMIT',
      }),
    )
    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)
    await flushPromises()
    lifecycle.submitGateChecklist.value = makeReadyChecklist()

    await lifecycle.handleSubmitForApproval()

    expect(lifecycle.submitSummaryOpen.value).toBe(true)
    wrapper.unmount()
  })

  it('handleSubmitForApproval does not open summary when submit gate is not ready', async () => {
    const templateRef = ref(
      makeTemplate({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_SUBMIT',
      }),
    )
    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)
    await flushPromises()
    lifecycle.submitGateChecklist.value = makeReadyChecklist(false)

    await lifecycle.handleSubmitForApproval()

    expect(lifecycle.submitSummaryOpen.value).toBe(false)
    wrapper.unmount()
  })

  it('confirmSubmitFromSummary submits approval with lifecycle comment', async () => {
    const templateRef = ref(
      makeTemplate({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_SUBMIT',
      }),
    )
    vi.mocked(templatesApi.submitForApproval).mockResolvedValue(templateRef.value)

    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)
    lifecycle.lifecycleComment.value = 'Please review'
    lifecycle.submitSummaryOpen.value = true

    await lifecycle.confirmSubmitFromSummary()
    await flushPromises()

    expect(templatesApi.submitForApproval).toHaveBeenCalledWith('tpl-1', { commentSummary: 'Please review' })
    expect(lifecycle.lifecycleComment.value).toBe('')
    expect(lifecycle.submitSummaryOpen.value).toBe(false)
    wrapper.unmount()
  })

  it('handleApprovalDecision opens reject dialog for REJECTED', async () => {
    const templateRef = ref(
      makeTemplate({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_DECISION',
      }),
    )
    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)

    await lifecycle.handleApprovalDecision('REJECTED')

    expect(lifecycle.decisionDialogOpen.value).toBe(true)
    expect(lifecycle.decisionDialogMode.value).toBe('approval-reject')
    wrapper.unmount()
  })

  it('handlePublish opens summary when publish gate is ready', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'PENDING_RELEASE' }))
    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)
    await flushPromises()
    lifecycle.publishGateChecklist.value = makeReadyChecklist()
    lifecycle.publishVersion.value = '1.0.1'

    await lifecycle.handlePublish()

    expect(lifecycle.publishSummaryOpen.value).toBe(true)
    wrapper.unmount()
  })

  it('publishGateReady requires checklist, version, and no conflict', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'PENDING_RELEASE' }))
    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)

    lifecycle.publishGateChecklist.value = makeReadyChecklist()
    lifecycle.publishVersion.value = '1.0.1'
    expect(lifecycle.publishGateReady.value).toBe(true)

    lifecycle.publishVersion.value = '   '
    expect(lifecycle.publishGateReady.value).toBe(false)
    wrapper.unmount()
  })

  it('resetLifecycleTransientState clears lifecycle gate and comment state', () => {
    const templateRef = ref(makeTemplate())
    const { lifecycle, wrapper } = mountLifecycle(templateRef, pinia)

    lifecycle.lifecycleComment.value = 'note'
    lifecycle.publishGateChecklist.value = makeReadyChecklist()
    lifecycle.submitGateChecklist.value = makeReadyChecklist()
    lifecycle.publishGateLoadError.value = 'templates.error.loadPublishGate'

    lifecycle.resetLifecycleTransientState()

    expect(lifecycle.lifecycleComment.value).toBe('')
    expect(lifecycle.publishGateChecklist.value).toBeNull()
    expect(lifecycle.submitGateChecklist.value).toBeNull()
    expect(lifecycle.publishGateLoadError.value).toBeNull()
    wrapper.unmount()
  })

  it('confirmPublishFromSummary publishes and navigates to release versions tab', async () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'PENDING_RELEASE' }))
    vi.mocked(templatesApi.publishTemplate).mockResolvedValue(templateRef.value)

    const { lifecycle, activeDetailTab, loadTemplate, wrapper } = mountLifecycle(templateRef, pinia)
    lifecycle.publishVersion.value = '2.0.0'
    lifecycle.publishSummaryOpen.value = true

    await lifecycle.confirmPublishFromSummary({ fidelityViewedConfirmed: true })
    await flushPromises()

    expect(templatesApi.publishTemplate).toHaveBeenCalledWith('tpl-1', {
      releaseVersion: '2.0.0',
      fidelityViewedConfirmed: true,
    })
    expect(loadTemplate).toHaveBeenCalled()
    expect(activeDetailTab.value).toBe('releaseVersions')
    expect(lifecycle.publishSummaryOpen.value).toBe(false)
    wrapper.unmount()
  })
})
