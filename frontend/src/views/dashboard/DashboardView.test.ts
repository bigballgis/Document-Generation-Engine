import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'
import DashboardView from '@/views/dashboard/DashboardView.vue'
import { useTemplatesStore } from '@/stores/templates'
import { useCollaborationStore } from '@/stores/collaboration'
import { useSessionStore } from '@/stores/session'

const routeRef = ref({
  hash: '',
  path: '/dashboard',
  query: {} as Record<string, string>,
})

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => routeRef.value,
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

describe('DashboardView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routeRef.value = { hash: '', path: '/dashboard', query: {} }
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Admin',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      capabilities: {},
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )
  })

  it('shows recoverable error panel when template fetch fails without hiding tasks section', async () => {
    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockRejectedValue(new Error('network'))

    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          DashboardStatCards: true,
          LoadErrorPanel: {
            template: '<div class="load-error-stub"><button @click="$emit(\'retry\')">retry</button></div>',
          },
          TaskHubPartitionSection: true,
          ElCard: { template: '<div><slot /></div>' },
          ElSkeleton: true,
          ElEmpty: true,
          ElButton: true,
        },
      },
    })

    await flushPromises()

    expect(wrapper.find('.load-error-stub').exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'DashboardStatCards' }).exists()).toBe(false)
    expect(wrapper.find('#tasks-section').exists()).toBe(true)
  })

  it('retries loading after error panel retry', async () => {
    const templatesStore = useTemplatesStore()
    const fetchSpy = vi
      .spyOn(templatesStore, 'fetchTemplates')
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce(undefined)

    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          DashboardStatCards: true,
          LoadErrorPanel: {
            props: ['messageKey'],
            template: '<button class="retry-btn" @click="$emit(\'retry\')">retry</button>',
          },
          TaskHubPartitionSection: true,
          ElCard: { template: '<div><slot /></div>' },
          ElSkeleton: true,
          ElEmpty: true,
          ElButton: true,
        },
      },
    })

    await flushPromises()
    await wrapper.find('.retry-btn').trigger('click')
    await flushPromises()

    expect(fetchSpy).toHaveBeenCalledTimes(2)
    expect(wrapper.find('.retry-btn').exists()).toBe(false)
  })

  it('renders queue-filtered page title and calls fetchWorkItems with queue param', async () => {
    routeRef.value.query = { queue: 'TEST' }

    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Tester',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_TESTER'],
      capabilities: {
        decideTests: true,
      },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const collaborationStore = useCollaborationStore()
    const fetchSpy = vi.spyOn(collaborationStore, 'fetchWorkItems').mockImplementation(async () => {
      collaborationStore.workItems = [
        {
          workItemId: 'wi-1',
          templateId: 'tpl-1',
          templateName: 'Retail letter',
          groupCode: 'RETAIL',
          queue: 'TEST',
          triggerType: 'SUBMIT_FOR_TEST',
          submitterUserId: '10000003',
          summaryText: 'Template submitted for testing',
          createdAt: '2026-06-23T10:00:00Z',
          ageSeconds: 120,
        },
      ]
    })

    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          DashboardStatCards: true,
          LoadErrorPanel: true,
          CollaborationTimeoutConfigPanel: true,
          TaskHubPartitionSection: {
            props: ['partition'],
            template: '<div class="partition-stub">{{ partition.id }}</div>',
          },
          ElCard: { template: '<div><slot /></div>' },
          ElSkeleton: true,
          ElEmpty: true,
          ElButton: true,
        },
      },
    })

    await flushPromises()

    expect(fetchSpy).toHaveBeenCalledWith({ queue: 'TEST' })
    expect(wrapper.find('h1').text()).toBe('collaboration.workItem.queue.TEST.title')
    expect(wrapper.find('.partition-stub').text()).toBe('queue-TEST')
  })

  it('skips collaboration fetch for master-review filter', async () => {
    routeRef.value.query = { filter: 'master-review' }

    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Admin',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management', 'route.master-management'],
      roles: ['GLOBAL_ADMIN'],
      capabilities: {
        reviewMasters: true,
        manageMasters: true,
      },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(() => true)

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const collaborationStore = useCollaborationStore()
    const fetchSpy = vi.spyOn(collaborationStore, 'fetchWorkItems')

    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          DashboardStatCards: true,
          LoadErrorPanel: true,
          CollaborationTimeoutConfigPanel: true,
          TaskHubPartitionSection: true,
          ElCard: { template: '<div><slot /></div>' },
          ElSkeleton: true,
          ElEmpty: true,
          ElButton: true,
        },
      },
    })

    await flushPromises()

    expect(fetchSpy).not.toHaveBeenCalled()
    expect(wrapper.find('h1').text()).toBe('nav.behaviorItems.masterReview')
  })

  it('shows collaboration load error inside tasks section while stats remain visible', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Tester',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_TESTER'],
      capabilities: {
        decideTests: true,
      },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const collaborationStore = useCollaborationStore()
    vi.spyOn(collaborationStore, 'fetchWorkItems').mockImplementation(async () => {
      collaborationStore.workItemsErrorMessageKey = 'collaboration.workItems.error.load'
      throw new Error('network')
    })

    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          DashboardStatCards: {
            template: '<div class="stats-stub" />',
          },
          LoadErrorPanel: {
            props: ['messageKey'],
            template: '<div class="load-error-stub">{{ messageKey }}</div>',
          },
          TaskHubPartitionSection: true,
          CollaborationTimeoutConfigPanel: true,
          ElCard: { template: '<div><slot /></div>' },
          ElSkeleton: true,
          ElEmpty: true,
          ElButton: true,
        },
      },
    })

    await flushPromises()

    expect(wrapper.find('.stats-stub').exists()).toBe(true)
    expect(wrapper.find('#tasks-section .load-error-stub').text()).toBe(
      'collaboration.workItems.error.load',
    )
  })
})
