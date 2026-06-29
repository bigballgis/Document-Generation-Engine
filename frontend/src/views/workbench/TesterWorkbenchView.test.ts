import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import TesterWorkbenchView from '@/views/workbench/TesterWorkbenchView.vue'
import { useCollaborationStore } from '@/stores/collaboration'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

describe('TesterWorkbenchView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders workbench header and collaboration panel', async () => {
    const store = useCollaborationStore()
    vi.spyOn(store, 'fetchWorkItems').mockResolvedValue(undefined)

    const wrapper = mount(TesterWorkbenchView, {
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

    expect(wrapper.text()).toContain('workbench.tester.title')
    expect(wrapper.find('.panel-stub').text()).toBe('TEST')
  })
})
