import { computed, defineComponent, ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as collaborationApi from '@/api/collaboration'
import { useDashboardDataLoader } from '@/composables/useDashboardDataLoader'
import { useCollaborationStore } from '@/stores/collaboration'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import type { ManagementCapabilities } from '@/types/session'

const routeQuery = ref<Record<string, string>>({})
const routeHash = ref('')

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: routeQuery.value,
    hash: routeHash.value,
  }),
}))

vi.mock('@/api/collaboration', () => ({
  getCollaborationTimeoutConfig: vi.fn().mockResolvedValue({
    scopeType: 'GLOBAL',
    groupCode: null,
    testThresholdHours: 24,
    approvalThresholdHours: 48,
    pendingReleaseThresholdHours: 12,
    remediationThresholdHours: 72,
    updatedAt: '2026-06-01T00:00:00Z',
  }),
}))

const capabilityRefs = {
  context: ref({ roles: ['TEMPLATE_TESTER'] }),
  reviewMasters: ref(false),
  manageMasters: ref(false),
}

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => capabilityRefs,
}))

const isOverviewTab = ref(true)
const isWorkflowTab = ref(false)
const isTaskTab = ref(false)
const primaryClusterOneRole = ref<'MASTER_DESIGNER' | 'TEMPLATE_AUTHOR' | 'TEMPLATE_TESTER' | null>(
  'TEMPLATE_TESTER',
)
const showTimeoutConfig = ref(false)

function mountDataLoaderHarness() {
  let dataLoader!: ReturnType<typeof useDashboardDataLoader>
  const Harness = defineComponent({
    setup() {
      dataLoader = useDashboardDataLoader({
        isOverviewTab: computed(() => isOverviewTab.value),
        isWorkflowTab: computed(() => isWorkflowTab.value),
        isTaskTab: computed(() => isTaskTab.value),
        primaryClusterOneRole: computed(() => primaryClusterOneRole.value),
        showTimeoutConfig: computed(() => showTimeoutConfig.value),
      })
      return { dataLoader }
    },
    template: '<div />',
  })
  mount(Harness)
  return dataLoader
}

const BASE_CAPABILITIES: ManagementCapabilities = {
  manageMasters: false,
  reviewMasters: false,
  authorTemplates: false,
  decideTests: true,
  decideApprovals: false,
  publishTemplates: false,
  stopTemplates: false,
  restoreOrDeprecateTemplates: false,
  deleteTemplates: false,
  exportTemplates: false,
  viewCollaborationWorkItems: true,
  maintainCollaborationTimeoutConfig: false,
  authorContentModules: false,
  decideContentModuleReviews: false,
  manageContentModuleLifecycle: false,
  manageApiPolicy: false,
  readAudit: false,
}

describe('useDashboardDataLoader', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routeQuery.value = {}
    routeHash.value = ''
    isOverviewTab.value = true
    isWorkflowTab.value = false
    isTaskTab.value = false
    primaryClusterOneRole.value = 'TEMPLATE_TESTER'
    showTimeoutConfig.value = false
    capabilityRefs.context.value = { roles: ['TEMPLATE_TESTER'] }
    capabilityRefs.reviewMasters.value = false
    capabilityRefs.manageMasters.value = false
    vi.mocked(collaborationApi.getCollaborationTimeoutConfig).mockClear()
  })

  function setupSession(overrides: Partial<ManagementCapabilities> = {}) {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['TEMPLATE_TESTER'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      capabilities: { ...BASE_CAPABILITIES, ...overrides },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )
    vi.spyOn(sessionStore, 'hasRole').mockReturnValue(false)
  }

  it('loads templates and collaboration work items on mount for overview tab', async () => {
    setupSession()

    const templatesStore = useTemplatesStore()
    const fetchTemplatesSpy = vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const collaborationStore = useCollaborationStore()
    const fetchWorkItemsSpy = vi.spyOn(collaborationStore, 'fetchWorkItems').mockResolvedValue(undefined)

    mountDataLoaderHarness()
    await flushPromises()

    expect(fetchTemplatesSpy).toHaveBeenCalled()
    expect(fetchWorkItemsSpy).toHaveBeenCalledWith(undefined)
  })

  it('marks templates load error without throwing', async () => {
    setupSession()

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockRejectedValue(new Error('network'))

    const dataLoader = mountDataLoaderHarness()
    await flushPromises()

    expect(dataLoader.templatesLoadError.value).toBe(true)
    expect(dataLoader.showStatsSection.value).toBe(false)
  })

  it('fetches collaboration with queue param on task tab', async () => {
    setupSession()
    routeQuery.value = { queue: 'TEST' }
    isOverviewTab.value = false
    isTaskTab.value = true

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const collaborationStore = useCollaborationStore()
    const fetchWorkItemsSpy = vi.spyOn(collaborationStore, 'fetchWorkItems').mockResolvedValue(undefined)

    mountDataLoaderHarness()
    await flushPromises()

    expect(fetchWorkItemsSpy).toHaveBeenCalledWith({ queue: 'TEST' })
  })

  it('skips collaboration fetch for master-review filter', async () => {
    setupSession({ reviewMasters: true, manageMasters: true })
    routeQuery.value = { filter: 'master-review' }
    isOverviewTab.value = false
    isTaskTab.value = true
    capabilityRefs.reviewMasters.value = true
    capabilityRefs.manageMasters.value = true

    const sessionStore = useSessionStore()
    vi.spyOn(sessionStore, 'canAccessRoute').mockReturnValue(true)

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const collaborationStore = useCollaborationStore()
    const fetchWorkItemsSpy = vi.spyOn(collaborationStore, 'fetchWorkItems')

    mountDataLoaderHarness()
    await flushPromises()

    expect(fetchWorkItemsSpy).not.toHaveBeenCalled()
  })

  it('enriches master review history for master designer after load', async () => {
    setupSession({ manageMasters: true })
    primaryClusterOneRole.value = 'MASTER_DESIGNER'
    isOverviewTab.value = true

    const sessionStore = useSessionStore()
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.master-management',
    )

    const mastersStore = useMastersStore()
    vi.spyOn(mastersStore, 'fetchMasters').mockResolvedValue(undefined)
    const enrichSpy = vi.spyOn(mastersStore, 'enrichDraftMasterReviewHistory').mockResolvedValue(undefined)

    mountDataLoaderHarness()
    await flushPromises()

    expect(enrichSpy).toHaveBeenCalled()
  })

  it('reports collaboration fetch failure from store', async () => {
    setupSession()
    routeQuery.value = { queue: 'TEST' }
    isOverviewTab.value = false
    isTaskTab.value = true

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const collaborationStore = useCollaborationStore()
    vi.spyOn(collaborationStore, 'fetchWorkItems').mockImplementation(async () => {
      collaborationStore.workItemsErrorMessageKey = 'collaboration.workItems.error.load'
    })

    const dataLoader = mountDataLoaderHarness()
    await flushPromises()

    expect(dataLoader.collaborationFetchFailed.value).toBe(true)
    expect(dataLoader.collaborationLoadErrorKey.value).toBe('collaboration.workItems.error.load')
  })

  it('retries collaboration load via retryCollaborationLoad', async () => {
    setupSession()
    routeQuery.value = { queue: 'TEST' }
    isOverviewTab.value = false
    isTaskTab.value = true

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const collaborationStore = useCollaborationStore()
    const fetchWorkItemsSpy = vi
      .spyOn(collaborationStore, 'fetchWorkItems')
      .mockResolvedValue(undefined)

    const dataLoader = mountDataLoaderHarness()
    await flushPromises()
    fetchWorkItemsSpy.mockClear()

    await dataLoader.retryCollaborationLoad()
    await flushPromises()

    expect(fetchWorkItemsSpy).toHaveBeenCalledWith({ queue: 'TEST' })
  })
})
