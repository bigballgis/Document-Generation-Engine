import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ApproverWorkbenchView from '@/views/workbench/ApproverWorkbenchView.vue'
import { useCollaborationStore } from '@/stores/collaboration'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

describe('ApproverWorkbenchView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders approver header and APPROVAL queue panel', async () => {
    const store = useCollaborationStore()
    vi.spyOn(store, 'fetchWorkItems').mockResolvedValue(undefined)

    const wrapper = mount(ApproverWorkbenchView, {
      global: {
        stubs: {
          AppPageLayout: { template: '<div><slot /></div>' },
          CollaborationWorkItemPanel: {
            props: ['queue', 'emptyMessageKey'],
            template: '<div class="panel-stub">{{ queue }}</div>',
          },
        },
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('workbench.approver.title')
    expect(wrapper.find('.panel-stub').text()).toBe('APPROVAL')
  })
})
