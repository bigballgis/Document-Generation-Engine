import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'
import DashboardView from '@/views/dashboard/DashboardView.vue'
import { useTemplatesStore } from '@/stores/templates'
import { useCollaborationStore } from '@/stores/collaboration'
import { useSessionStore } from '@/stores/session'
import { useMastersStore } from '@/stores/masters'
import {
  masterDesignerJourneySteps,
  templateAuthorJourneySteps,
  templateApproverJourneySteps,
  templateTeamLeadJourneySteps,
  templateTesterJourneySteps,
} from '@/constants/roleJourneyDefinitions'

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

const journeyTimelineStub = {
  name: 'RoleJourneyTimeline',
  props: ['steps', 'currentStepIndex', 'guidanceKey', 'titleKey'],
  template:
    '<div class="journey-timeline-stub" :data-step-count="steps.length" :data-current-index="currentStepIndex" :data-guidance-key="guidanceKey">{{ titleKey }}</div>',
}

function mountDashboard(extraStubs: Record<string, unknown> = {}) {
  return mount(DashboardView, {
    global: {
      stubs: {
        DashboardStatCards: true,
        LoadErrorPanel: true,
        CollaborationTimeoutConfigPanel: true,
        TaskHubPartitionSection: true,
        RoleJourneyTimeline: journeyTimelineStub,
        ElCard: { template: '<div><slot /></div>' },
        ElSkeleton: true,
        ElEmpty: true,
        ElButton: true,
        ...extraStubs,
      },
    },
  })
}

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

  it('shows journey section above tasks for TEMPLATE_AUTHOR with six-step stepper', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Author',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_AUTHOR'],
      capabilities: { authorTemplates: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockImplementation(async () => {
      templatesStore.templates = []
    })

    const wrapper = mountDashboard({
      TaskHubPartitionSection: {
        props: ['partition'],
        template: '<div class="partition-stub">{{ partition.id }}</div>',
      },
    })
    await flushPromises()

    const journeySection = wrapper.find('#journey-section')
    const tasksSection = wrapper.find('#tasks-section')
    expect(journeySection.exists()).toBe(true)
    expect(tasksSection.exists()).toBe(true)
    expect(
      journeySection.element.compareDocumentPosition(tasksSection.element) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy()

    const stub = wrapper.find('.journey-timeline-stub')
    expect(Number(stub.attributes('data-step-count'))).toBe(templateAuthorJourneySteps.length)
    expect(stub.attributes('data-current-index')).toBe('0')
    expect(stub.text()).toBe('journey.roles.TEMPLATE_AUTHOR.title')
  })

  it('sets template author journey index 3 for draft ready to submit', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Author',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_AUTHOR'],
      capabilities: { authorTemplates: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockImplementation(async () => {
      templatesStore.templates = [
        {
          id: 'tpl-1',
          externalId: 'TPL-001',
          groupCode: 'RETAIL',
          name: 'Retail letter',
          lifecycleStatus: 'DRAFT',
          releaseVersion: null,
          releaseVersionCount: 0,
          masterId: 'master-1',
          updatedBy: '10000005',
          updatedAt: '2026-06-26T10:00:00Z',
          bindingsCount: 2,
          hasSuccessfulTrialOutput: true,
        },
      ] as never
    })

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('.journey-timeline-stub').attributes('data-current-index')).toBe('3')
  })

  it('sets template author journey index 5 with team-lead guidance for pending release', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Author',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_AUTHOR'],
      capabilities: { authorTemplates: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockImplementation(async () => {
      templatesStore.templates = [
        {
          id: 'tpl-pending',
          externalId: 'TPL-PENDING',
          groupCode: 'RETAIL',
          name: 'Pending template',
          lifecycleStatus: 'PENDING_RELEASE',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'master-1',
          updatedBy: '10000005',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ]
    })

    const wrapper = mountDashboard()
    await flushPromises()

    const stub = wrapper.find('.journey-timeline-stub')
    expect(stub.attributes('data-current-index')).toBe('5')
    expect(stub.attributes('data-guidance-key')).toBe(
      'journey.roles.TEMPLATE_AUTHOR.awaitGoLive.teamLeadGuidance',
    )
  })

  it('sets template author journey waiting guidance when only testing templates exist', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Author',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_AUTHOR'],
      capabilities: { authorTemplates: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockImplementation(async () => {
      templatesStore.templates = [
        {
          id: 'tpl-testing',
          externalId: 'TPL-TEST',
          groupCode: 'RETAIL',
          name: 'Testing template',
          lifecycleStatus: 'TESTING',
          releaseVersion: null,
          releaseVersionCount: 0,
          masterId: 'master-1',
          updatedBy: '10000005',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ]
    })

    const wrapper = mountDashboard()
    await flushPromises()

    const stub = wrapper.find('.journey-timeline-stub')
    expect(stub.attributes('data-current-index')).toBeUndefined()
    expect(stub.attributes('data-guidance-key')).toBe(
      'journey.roles.TEMPLATE_AUTHOR.waitingTesting.guidance',
    )
  })

  it('shows master designer journey with 4 steps on unfiltered dashboard', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Designer',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.master-management', 'route.template-management'],
      roles: ['MASTER_DESIGNER'],
      capabilities: { manageMasters: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(() => true)

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('#journey-section').exists()).toBe(true)
    expect(Number(wrapper.find('.journey-timeline-stub').attributes('data-step-count'))).toBe(
      masterDesignerJourneySteps.length,
    )
    expect(wrapper.find('.journey-timeline-stub').text()).toBe(
      'journey.roles.MASTER_DESIGNER.title',
    )
  })

  it('sets master designer journey index 0 when catalog is empty', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Designer',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.master-management'],
      roles: ['MASTER_DESIGNER'],
      capabilities: { manageMasters: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(() => true)

    const mastersStore = useMastersStore()
    vi.spyOn(mastersStore, 'fetchMasters').mockImplementation(async () => {
      mastersStore.masters = []
    })
    vi.spyOn(mastersStore, 'enrichDraftMasterReviewHistory').mockResolvedValue(undefined)

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('.journey-timeline-stub').attributes('data-current-index')).toBe('0')
  })

  it('sets master designer journey index 2 for ready draft master', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Designer',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.master-management'],
      roles: ['MASTER_DESIGNER'],
      capabilities: { manageMasters: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(() => true)

    const mastersStore = useMastersStore()
    vi.spyOn(mastersStore, 'fetchMasters').mockImplementation(async () => {
      mastersStore.masters = [
        {
          id: 'm1',
          groupCode: 'RETAIL',
          name: 'Letterhead',
          status: 'DRAFT',
          originalFilename: 'letterhead.docx',
          anchorCount: 2,
          updatedBy: '10000005',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ]
    })
    vi.spyOn(mastersStore, 'enrichDraftMasterReviewHistory').mockResolvedValue(undefined)

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('.journey-timeline-stub').attributes('data-current-index')).toBe('2')
  })

  it('sets master designer journey index 3 when draft has rejected review history', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Designer',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.master-management'],
      roles: ['MASTER_DESIGNER'],
      capabilities: { manageMasters: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(() => true)

    const mastersStore = useMastersStore()
    vi.spyOn(mastersStore, 'fetchMasters').mockImplementation(async () => {
      mastersStore.masters = [
        {
          id: 'm1',
          groupCode: 'RETAIL',
          name: 'Letterhead',
          status: 'DRAFT',
          originalFilename: 'letterhead.docx',
          anchorCount: 2,
          updatedBy: '10000005',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ]
    })
    vi.spyOn(mastersStore, 'getDraftReviewHistory').mockImplementation((masterId) =>
      masterId === 'm1'
        ? [
            {
              action: 'REJECTED',
              decision: 'REJECTED',
              changeSummary: null,
              commentSummary: 'Fix header',
              actorUsername: '10000001',
              createdAt: '2026-06-25T10:00:00Z',
            },
          ]
        : undefined,
    )
    vi.spyOn(mastersStore, 'enrichDraftMasterReviewHistory').mockResolvedValue(undefined)

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('.journey-timeline-stub').attributes('data-current-index')).toBe('3')
  })

  it('sets master designer journey waiting guidance when only pending review masters exist', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Designer',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.master-management'],
      roles: ['MASTER_DESIGNER'],
      capabilities: { manageMasters: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(() => true)

    const mastersStore = useMastersStore()
    vi.spyOn(mastersStore, 'fetchMasters').mockImplementation(async () => {
      mastersStore.masters = [
        {
          id: 'm1',
          groupCode: 'RETAIL',
          name: 'Letterhead',
          status: 'PENDING_REVIEW',
          originalFilename: 'letterhead.docx',
          anchorCount: 2,
          updatedBy: '10000005',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ]
    })
    vi.spyOn(mastersStore, 'enrichDraftMasterReviewHistory').mockResolvedValue(undefined)

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('.journey-timeline-stub').attributes('data-current-index')).toBeUndefined()
    expect(wrapper.find('.journey-timeline-stub').attributes('data-guidance-key')).toBe(
      'journey.roles.MASTER_DESIGNER.waitingReview.guidance',
    )
  })

  it('shows tester journey with 3 steps when TEMPLATE_TESTER only', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Tester',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_TESTER'],
      capabilities: { decideTests: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const wrapper = mountDashboard()
    await flushPromises()

    expect(Number(wrapper.find('.journey-timeline-stub').attributes('data-step-count'))).toBe(
      templateTesterJourneySteps.length,
    )
  })

  it('sets template tester journey index 0 for newest OPEN TEST work item', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Tester',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_TESTER'],
      capabilities: { decideTests: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockImplementation(async () => {
      templatesStore.templates = [
        {
          id: 'tpl-testing',
          externalId: 'TPL-TEST',
          groupCode: 'RETAIL',
          name: 'Testing template',
          lifecycleStatus: 'TESTING',
          releaseVersion: null,
          releaseVersionCount: 0,
          masterId: 'master-1',
          updatedBy: '10000003',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ] as never
    })

    const collaborationStore = useCollaborationStore()
    collaborationStore.workItems = [
      {
        workItemId: 'wi-1',
        templateId: 'tpl-testing',
        templateName: 'Testing template',
        queue: 'TEST',
        triggerType: 'SUBMIT_FOR_TEST',
        groupCode: 'RETAIL',
        submitterUserId: '10000003',
        summaryText: 'Ready for test',
        ageSeconds: 3600,
        createdAt: '2026-06-26T10:00:00Z',
      },
    ]

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('.journey-timeline-stub').attributes('data-current-index')).toBe('0')
  })

  it('sets template tester journey index 1 for TESTING template without work items', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Tester',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_TESTER'],
      capabilities: { decideTests: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockImplementation(async () => {
      templatesStore.templates = [
        {
          id: 'tpl-testing',
          externalId: 'TPL-TEST',
          groupCode: 'RETAIL',
          name: 'Testing template',
          lifecycleStatus: 'TESTING',
          releaseVersion: null,
          releaseVersionCount: 0,
          masterId: 'master-1',
          updatedBy: '10000003',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ] as never
    })

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('.journey-timeline-stub').attributes('data-current-index')).toBe('1')
  })

  it('shows approver journey with 3 steps for TEMPLATE_APPROVER-only sessions', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Approver',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_APPROVER'],
      capabilities: { decideApprovals: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('#journey-section').exists()).toBe(true)
    expect(Number(wrapper.find('.journey-timeline-stub').attributes('data-step-count'))).toBe(
      templateApproverJourneySteps.length,
    )
    expect(wrapper.find('.journey-timeline-stub').text()).toBe(
      'journey.roles.TEMPLATE_APPROVER.title',
    )
  })

  it('sets template approver journey index 0 for newest OPEN APPROVAL work item', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Approver',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_APPROVER'],
      capabilities: { decideApprovals: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const collaborationStore = useCollaborationStore()
    collaborationStore.workItems = [
      {
        workItemId: 'wi-approval',
        templateId: 'tpl-approval',
        templateName: 'Approval template',
        queue: 'APPROVAL',
        triggerType: 'SUBMIT_FOR_APPROVAL',
        groupCode: 'RETAIL',
        submitterUserId: '10000003',
        summaryText: 'Ready for approval',
        ageSeconds: 3600,
        createdAt: '2026-06-26T10:00:00Z',
      },
    ]

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('.journey-timeline-stub').attributes('data-current-index')).toBe('0')
  })

  it('still shows journey section on filtered queue deep links', async () => {
    routeRef.value.query = { queue: 'TEST' }
    routeRef.value.hash = '#tasks-section'

    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Tester',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['TEMPLATE_TESTER'],
      capabilities: { decideTests: true },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const collaborationStore = useCollaborationStore()
    vi.spyOn(collaborationStore, 'fetchWorkItems').mockResolvedValue(undefined)

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('#journey-section').exists()).toBe(true)
    expect(wrapper.find('h1').text()).toBe('collaboration.workItem.queue.TEST.title')
  })

  it('shows team-lead journey with 4 steps for GROUP_ADMIN-only sessions', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Group Admin',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management', 'route.master-management'],
      roles: ['GROUP_ADMIN'],
      capabilities: { publishTemplates: true, reviewMasters: true, decideApprovals: false },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(() => true)

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)
    const mastersStore = useMastersStore()
    vi.spyOn(mastersStore, 'fetchMasters').mockResolvedValue(undefined)

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('#journey-section').exists()).toBe(true)
    expect(Number(wrapper.find('.journey-timeline-stub').attributes('data-step-count'))).toBe(
      templateTeamLeadJourneySteps.length,
    )
    expect(wrapper.find('.journey-timeline-stub').text()).toBe('journey.roles.GROUP_ADMIN.title')
  })

  it('sets team-lead journey index 0 when PENDING_REVIEW masters exist', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Group Admin',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.master-management', 'route.template-management'],
      roles: ['GROUP_ADMIN'],
      capabilities: { publishTemplates: true, reviewMasters: true, decideApprovals: false },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(() => true)

    const mastersStore = useMastersStore()
    vi.spyOn(mastersStore, 'fetchMasters').mockImplementation(async () => {
      mastersStore.masters = [
        {
          id: 'm-pending',
          groupCode: 'RETAIL',
          name: 'Letterhead',
          status: 'PENDING_REVIEW',
          originalFilename: 'letterhead.docx',
          anchorCount: 2,
          updatedBy: '10000005',
          updatedAt: '2026-06-26T10:00:00Z',
        },
      ]
    })

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('.journey-timeline-stub').attributes('data-current-index')).toBe('0')
  })

  it('sets team-lead journey index 1 for newest OPEN PENDING_RELEASE work item', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      displayName: 'Group Admin',
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      roles: ['GROUP_ADMIN'],
      capabilities: { publishTemplates: true, reviewMasters: true, decideApprovals: false },
    } as never
    vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation(
      (routeKey: string) => routeKey === 'route.template-management',
    )

    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockResolvedValue(undefined)

    const collaborationStore = useCollaborationStore()
    collaborationStore.workItems = [
      {
        workItemId: 'wi-pending-release',
        templateId: 'tpl-pending-release',
        templateName: 'Pending release template',
        queue: 'PENDING_RELEASE',
        triggerType: 'APPROVAL_PENDING_RELEASE',
        groupCode: 'RETAIL',
        submitterUserId: '10000003',
        summaryText: 'Ready for go-live',
        ageSeconds: 3600,
        createdAt: '2026-06-26T10:00:00Z',
      },
    ]

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('.journey-timeline-stub').attributes('data-current-index')).toBe('1')
  })
})
