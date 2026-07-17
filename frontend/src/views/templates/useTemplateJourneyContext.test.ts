import { computed, defineComponent, ref, type Ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import type { TemplateDetail } from '@/types/template'
import { useCollaborationStore } from '@/stores/collaboration'
import { useTemplateJourneyContext } from '@/views/templates/useTemplateJourneyContext'

const capabilityRefs = {
  authorTemplates: ref(true),
  decideTests: ref(true),
  decideApprovals: ref(true),
  publishTemplates: ref(true),
  reviewMasters: ref(true),
}

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => capabilityRefs,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

const sessionRoles = ref<string[]>(['TEMPLATE_AUTHOR'])

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

function mountJourney(templateRef: Ref<TemplateDetail | null>, pinia: ReturnType<typeof createPinia>) {
  const lastPreview = ref(null)
  const lifecycle = {
    publishGateReady: computed(() => true),
    submitForTestDialogOpen: ref(false),
    handleSubmitForApproval: vi.fn(async () => {}),
    loadPublishGateData: vi.fn(async () => {}),
    handlePublish: vi.fn(async () => {}),
  }
  const openDevWorkspaceTab = vi.fn()
  const openLifecyclePanel = vi.fn()
  const handleTestGenerate = vi.fn(async () => {})
  const Comp = defineComponent({
    setup() {
      const journey = useTemplateJourneyContext({
        isDevEditor: computed(() => false),
        template: computed(() => templateRef.value),
        lastPreview,
        lifecycle,
        openDevWorkspaceTab,
        openLifecyclePanel,
        handleTestGenerate,
      })
      return { journey }
    },
    template: '<div></div>',
  })
  const wrapper = mount(Comp, {
    global: { plugins: [pinia, createI18n({ legacy: false, locale: 'en', messages: { en } }), ElementPlus] },
  })
  return {
    wrapper,
    journey: (wrapper.vm as { journey: ReturnType<typeof useTemplateJourneyContext> }).journey,
    collaborationStore: useCollaborationStore(),
  }
}

describe('useTemplateJourneyContext', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    sessionRoles.value = ['TEMPLATE_AUTHOR']
  })

  it('BDD-F6-A3-002: showAuthorJourney is true for DRAFT template', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DRAFT' }))
    const { journey, wrapper } = mountJourney(templateRef, pinia)
    expect(journey.showAuthorJourney.value).toBe(true)
    wrapper.unmount()
  })

  it('BDD-F6-A3-002: showAuthorJourney is false when DEPRECATED', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DEPRECATED' }))
    const { journey, wrapper } = mountJourney(templateRef, pinia)
    expect(journey.showAuthorJourney.value).toBe(false)
    wrapper.unmount()
  })

  it('BDD-F6-A3-002: showTesterJourney is true when TESTING', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    const { journey, wrapper } = mountJourney(templateRef, pinia)
    expect(journey.showTesterJourney.value).toBe(true)
    wrapper.unmount()
  })

  it('BDD-F6-A3-002: authorJourneyContext reflects remediation work items', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'DRAFT' }))
    const { journey, collaborationStore, wrapper } = mountJourney(templateRef, pinia)
    collaborationStore.workItems = [
      {
        workItemId: 'wi-1',
        templateId: 'tpl-1',
        templateName: 'Test',
        groupCode: 'RETAIL',
        queue: 'REMEDIATION',
        triggerType: 'SUBMIT_FOR_TEST',
        submitterUserId: '10000001',
        summaryText: 'Fix',
        createdAt: '2026-06-23T10:00:00Z',
        ageSeconds: 60,
      },
    ]
    expect(journey.authorJourneyContext.value?.isRemediation).toBe(true)
    wrapper.unmount()
  })

  it('BDD-F6-A3-002: handleJourneyCheckEvidence marks fidelity and coverage viewed', () => {
    const templateRef = ref(makeTemplate({ lifecycleStatus: 'TESTING' }))
    const { journey, wrapper } = mountJourney(templateRef, pinia)
    journey.handleJourneyCheckEvidence()
    expect(journey.testerEvidenceViewed.value.fidelityViewedConfirmed).toBe(true)
    expect(journey.testerEvidenceViewed.value.coverageViewedConfirmed).toBe(true)
    wrapper.unmount()
  })
})
