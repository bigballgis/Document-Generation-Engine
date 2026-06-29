import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import CollaborationWorkItemPanel from '@/components/collaboration/CollaborationWorkItemPanel.vue'
import { useCollaborationStore } from '@/stores/collaboration'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

describe('CollaborationWorkItemPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('loads work items on mount and renders table rows', async () => {
    const store = useCollaborationStore()
    vi.spyOn(store, 'fetchWorkItems').mockImplementation(async () => {
      store.workItems = [
        {
          workItemId: 'wi-1',
          templateId: 'tpl-1',
          templateName: 'Loan Notice',
          groupCode: 'RETAIL',
          queue: 'TEST',
          triggerType: 'SUBMIT_FOR_TEST',
          submitterUserId: '10000003',
          summaryText: 'Template submitted for testing',
          createdAt: '2026-06-26T10:00:00Z',
          ageSeconds: 3600,
        },
      ]
    })

    const wrapper = mount(CollaborationWorkItemPanel, {
      props: { queue: 'TEST' },
      global: {
        stubs: {
          LoadErrorPanel: true,
          AppDataTable: {
            props: ['data'],
            template: '<div class="work-items-table-stub">{{ data.length }}</div>',
          },
          AppTablePagination: true,
          TableColumnHeader: true,
          ElSkeleton: true,
          ElEmpty: true,
          ElTableColumn: true,
        },
      },
    })

    await flushPromises()

    expect(store.fetchWorkItems).toHaveBeenCalledWith({ queue: 'TEST', groupCode: undefined })
    expect(wrapper.find('.work-items-table-stub').text()).toBe('1')
  })

  it('shows empty state when no items are returned', async () => {
    const store = useCollaborationStore()
    vi.spyOn(store, 'fetchWorkItems').mockResolvedValue(undefined)

    const wrapper = mount(CollaborationWorkItemPanel, {
      props: { queue: 'APPROVAL', emptyMessageKey: 'workbench.approver.empty' },
      global: {
        stubs: {
          LoadErrorPanel: true,
          AppDataTable: true,
          AppTablePagination: true,
          TableColumnHeader: true,
          ElSkeleton: true,
          ElEmpty: {
            props: ['description'],
            template: '<div class="empty-stub">{{ description }}</div>',
          },
          ElTableColumn: true,
        },
      },
    })

    await flushPromises()

    expect(wrapper.find('.empty-stub').text()).toBe('workbench.approver.empty')
  })
})
