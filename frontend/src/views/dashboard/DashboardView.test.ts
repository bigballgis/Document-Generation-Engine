import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import DashboardView from '@/views/dashboard/DashboardView.vue'
import { useCollaborationStore } from '@/stores/collaboration'
import { useDashboardStore } from '@/stores/dashboard'
import {
  globalAdminJourneySteps,
  masterDesignerJourneySteps,
  templateAuthorJourneySteps,
  templateApproverJourneySteps,
  templateTeamLeadJourneySteps,
  templateTesterJourneySteps,
} from '@/constants/roleJourneyDefinitions'
import {
  caps,
  expectJourney,
  masterFixture,
  mountDashboard,
  mountDashboardQueueTab,
  mountWorkflowAndFlush,
  resetDashboardRoute,
  routeRef,
  setWorkItems,
  stubDashboardSummary,
  stubMasters,
  stubSession,
  stubTemplates,
  templateFixture,
  workItemFixture,
} from './dashboardViewTestSupport'

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
    resetDashboardRoute()
    stubSession('admin')
    stubDashboardSummary()
  })

  it('shows recoverable error panel when dashboard summary fails without hiding tasks section', async () => {
    stubDashboardSummary('reject')

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
    expect(wrapper.find('#tasks-section').exists()).toBe(false)
  })

  it('retries loading after error panel retry', async () => {
    const dashboardStore = useDashboardStore()
    const fetchSpy = vi
      .spyOn(dashboardStore, 'fetchSummary')
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
    stubSession('tester')
    stubTemplates()

    const collaborationStore = useCollaborationStore()
    const fetchSpy = vi.spyOn(collaborationStore, 'fetchWorkItems').mockImplementation(async () => {
      collaborationStore.workItems = [
        workItemFixture({
          summaryText: 'Template submitted for testing',
          createdAt: '2026-06-23T10:00:00Z',
          ageSeconds: 120,
        }),
      ] as never
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
    expect(wrapper.find('h1').text()).toBe('dashboard.title')
    expect(wrapper.find('.partition-stub').text()).toBe('queue-TEST')
  })

  it('skips collaboration fetch for master-review filter', async () => {
    stubSession('masterDesigner', {
      visibleRoutes: ['route.master-management', 'route.template-management'],
      allowAllRoutes: true,
    })
    stubTemplates()
    stubMasters()

    const collaborationStore = useCollaborationStore()
    const fetchSpy = vi.spyOn(collaborationStore, 'fetchWorkItems').mockResolvedValue(undefined)

    const wrapper = mountDashboardQueueTab('MASTER_REVIEW')
    await flushPromises()

    expect(fetchSpy).not.toHaveBeenCalled()
    expect(wrapper.find('h1').text()).toBe('dashboard.title')
  })

  it('shows collaboration load error inside tasks section on queue tab', async () => {
    stubSession('tester')
    stubTemplates()

    const collaborationStore = useCollaborationStore()
    vi.spyOn(collaborationStore, 'fetchWorkItems').mockImplementation(async () => {
      collaborationStore.workItemsErrorMessageKey = 'collaboration.workItems.error.load'
      throw new Error('network')
    })

    const wrapper = mountDashboardQueueTab('TEST', {
      LoadErrorPanel: {
        props: ['messageKey'],
        template: '<div class="load-error-stub">{{ messageKey }}</div>',
      },
      TaskHubPartitionSection: true,
    })
    await flushPromises()

    expect(wrapper.find('#tasks-section .load-error-stub').text()).toBe(
      'collaboration.workItems.error.load',
    )
  })

  it('shows journey on workflow tab for TEMPLATE_AUTHOR with six-step stepper', async () => {
    stubSession('templateAuthor')
    stubTemplates('empty')

    const wrapper = await mountWorkflowAndFlush({
      TaskHubPartitionSection: {
        props: ['partition'],
        template: '<div class="partition-stub">{{ partition.id }}</div>',
      },
    })

    expect(wrapper.find('#journey-section').exists()).toBe(true)
    expect(wrapper.find('#tasks-section').exists()).toBe(false)
    expectJourney(wrapper, {
      stepCount: templateAuthorJourneySteps.length,
      index: '0',
      titleKey: 'journey.roles.TEMPLATE_AUTHOR.title',
    })
  })

  it('sets template author journey index 3 for draft ready to submit', async () => {
    stubSession('templateAuthor')
    stubTemplates([
      templateFixture({
        bindingsCount: 2,
        hasSuccessfulTrialOutput: true,
      }),
    ])

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { index: '3' })
  })

  it('sets template author journey index 5 with team-lead guidance for pending release', async () => {
    stubSession('templateAuthor')
    stubTemplates([
      templateFixture({
        id: 'tpl-pending',
        externalId: 'TPL-PENDING',
        name: 'Pending template',
        lifecycleStatus: 'PENDING_RELEASE',
        releaseVersion: '1.0.0',
        releaseVersionCount: 1,
      }),
    ])

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, {
      index: '5',
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.awaitGoLive.teamLeadGuidance',
    })
  })

  it('sets template author journey waiting guidance when only testing templates exist', async () => {
    stubSession('templateAuthor')
    stubTemplates([
      templateFixture({
        id: 'tpl-testing',
        externalId: 'TPL-TEST',
        name: 'Testing template',
        lifecycleStatus: 'TESTING',
      }),
    ])

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, {
      index: undefined,
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.waitingTesting.guidance',
    })
  })

  it('shows master designer journey with 4 steps on unfiltered dashboard', async () => {
    stubSession('masterDesigner')
    stubTemplates()

    const wrapper = await mountWorkflowAndFlush()
    expect(wrapper.find('#journey-section').exists()).toBe(true)
    expectJourney(wrapper, {
      stepCount: masterDesignerJourneySteps.length,
      titleKey: 'journey.roles.MASTER_DESIGNER.title',
    })
  })

  it('sets master designer journey index 0 when catalog is empty', async () => {
    stubSession('masterDesigner', { visibleRoutes: ['route.master-management'] })
    stubMasters([])

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { index: '0' })
  })

  it('sets master designer journey index 2 for ready draft master', async () => {
    stubSession('masterDesigner', { visibleRoutes: ['route.master-management'] })
    stubMasters([masterFixture()])

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { index: '2' })
  })

  it('sets master designer journey index 3 when draft has rejected review history', async () => {
    stubSession('masterDesigner', { visibleRoutes: ['route.master-management'] })
    stubMasters([masterFixture()], {
      reviewHistory: (masterId) =>
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
    })

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { index: '3' })
  })

  it('sets master designer journey waiting guidance when only pending review masters exist', async () => {
    stubSession('masterDesigner', { visibleRoutes: ['route.master-management'] })
    stubMasters([masterFixture({ status: 'PENDING_REVIEW' })])

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, {
      index: undefined,
      guidanceKey: 'journey.roles.MASTER_DESIGNER.waitingReview.guidance',
    })
  })

  it('shows tester journey with 3 steps when TEMPLATE_TESTER only', async () => {
    stubSession('tester')
    stubTemplates()

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { stepCount: templateTesterJourneySteps.length })
  })

  it('sets template tester journey index 0 for newest OPEN TEST work item', async () => {
    stubSession('tester')
    stubTemplates([
      templateFixture({
        id: 'tpl-testing',
        externalId: 'TPL-TEST',
        name: 'Testing template',
        lifecycleStatus: 'TESTING',
        updatedBy: '10000003',
      }),
    ])
    setWorkItems([
      workItemFixture({
        templateId: 'tpl-testing',
        templateName: 'Testing template',
        queue: 'TEST',
        triggerType: 'SUBMIT_FOR_TEST',
      }),
    ])

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { index: '0' })
  })

  it('sets template tester journey index 1 for TESTING template without work items', async () => {
    stubSession('tester')
    stubTemplates([
      templateFixture({
        id: 'tpl-testing',
        externalId: 'TPL-TEST',
        name: 'Testing template',
        lifecycleStatus: 'TESTING',
        updatedBy: '10000003',
      }),
    ])

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { index: '1' })
  })

  it('shows approver journey with 3 steps for TEMPLATE_APPROVER-only sessions', async () => {
    stubSession('approver')
    stubTemplates()

    const wrapper = await mountWorkflowAndFlush()
    expect(wrapper.find('#journey-section').exists()).toBe(true)
    expectJourney(wrapper, {
      stepCount: templateApproverJourneySteps.length,
      titleKey: 'journey.roles.TEMPLATE_APPROVER.title',
    })
  })

  it('sets template approver journey index 0 for newest OPEN APPROVAL work item', async () => {
    stubSession('approver')
    stubTemplates()
    setWorkItems([
      workItemFixture({
        workItemId: 'wi-approval',
        templateId: 'tpl-approval',
        templateName: 'Approval template',
        queue: 'APPROVAL',
        triggerType: 'SUBMIT_FOR_APPROVAL',
        summaryText: 'Ready for approval',
      }),
    ])

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { index: '0' })
  })

  it('hides journey section on filtered queue deep links', async () => {
    routeRef.value.query = { queue: 'TEST' }
    routeRef.value.hash = '#tasks-section'
    stubSession('tester')
    stubTemplates()

    const collaborationStore = useCollaborationStore()
    vi.spyOn(collaborationStore, 'fetchWorkItems').mockResolvedValue(undefined)

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('#journey-section').exists()).toBe(false)
    expect(wrapper.find('#tasks-section').exists()).toBe(true)
    expect(wrapper.find('h1').text()).toBe('dashboard.title')
  })

  it('shows team-lead journey with 4 steps for GROUP_ADMIN-only sessions', async () => {
    stubSession('teamLead')
    stubTemplates()
    stubMasters()

    const wrapper = await mountWorkflowAndFlush()
    expect(wrapper.find('#journey-section').exists()).toBe(true)
    expectJourney(wrapper, {
      stepCount: templateTeamLeadJourneySteps.length,
      titleKey: 'journey.roles.GROUP_ADMIN.title',
    })
  })

  it('sets team-lead journey index 0 when PENDING_REVIEW masters exist', async () => {
    stubSession('teamLead')
    stubMasters([
      masterFixture({
        id: 'm-pending',
        status: 'PENDING_REVIEW',
      }),
    ])
    stubTemplates()

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { index: '0' })
  })

  it('sets team-lead journey index 1 for newest OPEN PENDING_RELEASE work item', async () => {
    stubSession('teamLead', { visibleRoutes: ['route.template-management'], allowAllRoutes: false })
    stubTemplates()
    setWorkItems([
      workItemFixture({
        workItemId: 'wi-pending-release',
        templateId: 'tpl-pending-release',
        templateName: 'Pending release template',
        queue: 'PENDING_RELEASE',
        triggerType: 'APPROVAL_PENDING_RELEASE',
        summaryText: 'Ready for go-live',
      }),
    ])

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { index: '1' })
  })

  it('shows global admin journey with 5 steps for GLOBAL_ADMIN sessions', async () => {
    stubSession('globalAdmin')
    stubTemplates()
    stubMasters()

    const wrapper = await mountWorkflowAndFlush()
    expect(wrapper.find('#journey-section').exists()).toBe(true)
    expectJourney(wrapper, {
      stepCount: globalAdminJourneySteps.length,
      titleKey: 'journey.roles.GLOBAL_ADMIN.title',
    })
  })

  it('does not show team-lead journey for GLOBAL_ADMIN with team-lead capabilities', async () => {
    stubSession('globalAdmin', {
      visibleRoutes: ['route.template-management', 'route.master-management'],
      capabilities: caps({
        publishTemplates: true,
        reviewMasters: true,
        decideApprovals: false,
        viewCollaborationWorkItems: true,
        maintainCollaborationTimeoutConfig: true,
      }),
    })
    stubTemplates()
    stubMasters()

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { titleKey: 'journey.roles.GLOBAL_ADMIN.title' })
    expect(wrapper.find('.journey-timeline-stub').text()).not.toBe('journey.roles.GROUP_ADMIN.title')
  })

  it('sets global admin journey index 4 when ESCALATION work items exist', async () => {
    stubSession('globalAdmin', {
      visibleRoutes: ['route.template-management'],
      allowAllRoutes: false,
      capabilities: caps({
        publishTemplates: true,
        reviewMasters: true,
        deleteTemplates: true,
        viewCollaborationWorkItems: true,
        maintainCollaborationTimeoutConfig: true,
      }),
    })
    stubTemplates()

    const collaborationStore = useCollaborationStore()
    vi.spyOn(collaborationStore, 'fetchWorkItems').mockImplementation(async () => {
      collaborationStore.workItems = [
        workItemFixture({
          workItemId: 'wi-escalation',
          templateId: 'tpl-escalation',
          templateName: 'Overdue template',
          queue: 'ESCALATION',
          triggerType: 'TIMEOUT_ESCALATION',
          summaryText: 'Overdue reminder',
          ageSeconds: 7200,
        }),
      ] as never
    })

    const wrapper = await mountWorkflowAndFlush()
    expectJourney(wrapper, { index: '4' })
  })

  it('fetches collaboration work items on overview tab for accurate pending actions count', async () => {
    stubSession('tester')
    stubTemplates()

    const collaborationStore = useCollaborationStore()
    const fetchSpy = vi.spyOn(collaborationStore, 'fetchWorkItems').mockResolvedValue(undefined)

    routeRef.value.query = {}

    const wrapper = mountDashboard()
    await flushPromises()

    expect(fetchSpy).toHaveBeenCalledWith(undefined)
    expect(wrapper.find('#tasks-section').exists()).toBe(false)
  })
})
