import { computed, defineComponent, ref, type Ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { TemplateDetail } from '@/types/template'
import { useTemplateLifecycleGates } from '@/views/templates/useTemplateLifecycleGates'

vi.mock('@/api/templates', () => ({
  fetchPublishGate: vi.fn(),
  getTemplateCoverage: vi.fn(),
  fetchChangeDiff: vi.fn(),
  fetchReleaseVersions: vi.fn(),
}))

const capabilityRefs = {
  authorTemplates: ref(true),
  decideTests: ref(true),
  decideApprovals: ref(true),
  decideLegalApprovals: ref(false),
  publishTemplates: ref(true),
  stopTemplates: ref(true),
  restoreOrDeprecateTemplates: ref(true),
  deleteTemplates: ref(true),
}

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => capabilityRefs,
}))

function makeReadyChecklist(ready = true) {
  return { templateId: 'tpl-1', ready, blockerCount: ready ? 0 : 1, items: [] }
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

function mountGates(templateRef: Ref<TemplateDetail | null>, pinia: ReturnType<typeof createPinia>) {
  const templateId = computed(() => templateRef.value?.id ?? 'tpl-1')
  const Comp = defineComponent({
    setup() {
      const gates = useTemplateLifecycleGates({ templateId, template: computed(() => templateRef.value) })
      return { gates }
    },
    template: '<div></div>',
  })
  const wrapper = mount(Comp, {
    global: { plugins: [pinia, createI18n({ legacy: false, locale: 'en', messages: { en } }), ElementPlus] },
  })
  return { wrapper, gates: (wrapper.vm as { gates: ReturnType<typeof useTemplateLifecycleGates> }).gates }
}

describe('useTemplateLifecycleGates', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
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

  it('BDD-F6-A2-001: showDraftActions is true when template is DRAFT', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DRAFT' }))
    const { gates, wrapper } = mountGates(templateRef, pinia)
    expect(gates.showDraftActions.value).toBe(true)
    wrapper.unmount()
  })

  it('BDD-F6-A2-001: showTestingDecisionActions is true when template is TESTING', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    const { gates, wrapper } = mountGates(templateRef, pinia)
    expect(gates.showTestingDecisionActions.value).toBe(true)
    wrapper.unmount()
  })

  it('BDD-F6-A2-001: publishGateReady requires checklist, version, and no conflict', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'PENDING_RELEASE' }))
    const { gates, wrapper } = mountGates(templateRef, pinia)
    gates.publishGateChecklist.value = makeReadyChecklist()
    gates.publishVersion.value = '1.0.1'
    expect(gates.publishGateReady.value).toBe(true)
    gates.publishVersion.value = '   '
    expect(gates.publishGateReady.value).toBe(false)
    wrapper.unmount()
  })

  it('BDD-F6-A2-001: resetGateState clears gate state', () => {
    const templateRef = ref(makeTemplate())
    const { gates, wrapper } = mountGates(templateRef, pinia)
    gates.publishGateChecklist.value = makeReadyChecklist()
    gates.submitGateChecklist.value = makeReadyChecklist()
    gates.publishGateLoadError.value = 'templates.error.loadPublishGate'
    gates.resetGateState()
    expect(gates.publishGateChecklist.value).toBeNull()
    expect(gates.submitGateChecklist.value).toBeNull()
    expect(gates.publishGateLoadError.value).toBeNull()
    wrapper.unmount()
  })

  it('showDeleteTemplateAction is false when template is DELETED', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DELETED' }))
    const { gates, wrapper } = mountGates(templateRef, pinia)
    expect(gates.showDeleteTemplateAction.value).toBe(false)
    wrapper.unmount()
  })
})
