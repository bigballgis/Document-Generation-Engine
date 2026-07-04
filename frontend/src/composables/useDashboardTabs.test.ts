import { computed, defineComponent, ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useDashboardTabs } from '@/composables/useDashboardTabs'
import { useSessionStore } from '@/stores/session'

const routerReplace = vi.fn()
const routeQuery = ref<Record<string, string>>({})
const routeHash = ref('')

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: routeQuery.value,
    hash: routeHash.value,
  }),
  useRouter: () => ({ replace: routerReplace }),
}))

const capabilityRefs = {
  context: ref({ roles: ['TEMPLATE_TESTER'] }),
  reviewMasters: ref(false),
  manageMasters: ref(false),
}

vi.mock('@/composables/useWorkflowTasks', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/composables/useWorkflowTasks')>()
  return {
    ...actual,
    getVisibleCollaborationQueues: () => ['TEST'] as const,
  }
})

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => capabilityRefs,
}))

const showJourneySection = ref(true)
const journeyTitleKey = ref<string | undefined>('journey.roles.TEMPLATE_TESTER.title')

function mountTabsHarness() {
  let tabs!: ReturnType<typeof useDashboardTabs>
  const Harness = defineComponent({
    setup() {
      tabs = useDashboardTabs({
        showJourneySection: computed(() => showJourneySection.value),
        journeyTitleKey: computed(() => journeyTitleKey.value),
      })
      return { tabs }
    },
    template: '<div />',
  })
  mount(Harness)
  return tabs
}

describe('useDashboardTabs', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerReplace.mockClear()
    routeQuery.value = {}
    routeHash.value = ''
    showJourneySection.value = true
    journeyTitleKey.value = 'journey.roles.TEMPLATE_TESTER.title'
    capabilityRefs.context.value = { roles: ['TEMPLATE_TESTER'] }
    capabilityRefs.reviewMasters.value = false
    capabilityRefs.manageMasters.value = false

    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['TEMPLATE_TESTER'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
    } as never
  })

  it('defaults active tab to overview when no query params', () => {
    const tabs = mountTabsHarness()

    expect(tabs.activeTab.value).toBe('overview')
    expect(tabs.isOverviewTab.value).toBe(true)
    expect(tabs.isWorkflowTab.value).toBe(false)
    expect(tabs.isTaskTab.value).toBe(false)
  })

  it('resolves workflow tab from query when journey section is visible', () => {
    routeQuery.value = { tab: 'workflow' }

    const tabs = mountTabsHarness()

    expect(tabs.activeTab.value).toBe('workflow')
    expect(tabs.isWorkflowTab.value).toBe(true)
  })

  it('ignores workflow query when journey section is hidden', () => {
    routeQuery.value = { tab: 'workflow' }
    showJourneySection.value = false

    const tabs = mountTabsHarness()

    expect(tabs.activeTab.value).toBe('overview')
  })

  it('resolves queue tab from query', () => {
    routeQuery.value = { queue: 'TEST' }

    const tabs = mountTabsHarness()

    expect(tabs.activeTab.value).toBe('TEST')
    expect(tabs.isTaskTab.value).toBe(true)
  })

  it('resolves master-review tab from filter query', () => {
    routeQuery.value = { filter: 'master-review' }
    capabilityRefs.reviewMasters.value = true

    const tabs = mountTabsHarness()

    expect(tabs.activeTab.value).toBe('master-review')
    expect(tabs.isTaskTab.value).toBe(true)
  })

  it('includes workflow tab when journey section is visible', () => {
    const tabs = mountTabsHarness()

    expect(tabs.visibleTabs.value.some((tab) => tab.key === 'workflow')).toBe(true)
  })

  it('uses journey title for workflow tab label', () => {
    const tabs = mountTabsHarness()

    const workflowTab = tabs.visibleTabs.value.find((tab) => tab.key === 'workflow')
    expect(workflowTab).toBeDefined()
    expect(tabs.tabLabel(workflowTab!)).toBe('journey.roles.TEMPLATE_TESTER.title')
  })

  it('navigates to overview on tab change', () => {
    const tabs = mountTabsHarness()

    tabs.handleTabChange('overview')
    expect(routerReplace).toHaveBeenCalledWith({ path: '/dashboard' })
  })

  it('navigates to workflow tab on tab change', () => {
    const tabs = mountTabsHarness()

    tabs.handleTabChange('workflow')
    expect(routerReplace).toHaveBeenCalledWith({ path: '/dashboard', query: { tab: 'workflow' } })
  })

  it('navigates to queue tab on tab change', () => {
    const tabs = mountTabsHarness()

    tabs.handleTabChange('TEST')
    expect(routerReplace).toHaveBeenCalledWith({ path: '/dashboard', query: { queue: 'TEST' } })
  })
})
