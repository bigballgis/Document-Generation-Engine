import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import DashboardView from '@/views/dashboard/DashboardView.vue'
import { useTemplatesStore } from '@/stores/templates'
import { useSessionStore } from '@/stores/session'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

describe('DashboardView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
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

  it('shows recoverable error panel when template fetch fails', async () => {
    const templatesStore = useTemplatesStore()
    vi.spyOn(templatesStore, 'fetchTemplates').mockRejectedValue(new Error('network'))

    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          DashboardStatCards: true,
          LoadErrorPanel: {
            template: '<div class="load-error-stub"><button @click="$emit(\'retry\')">retry</button></div>',
          },
          ElCard: { template: '<div><slot /></div>' },
          ElSkeleton: true,
          ElEmpty: true,
          ElTable: true,
          ElTableColumn: true,
          ElButton: true,
        },
      },
    })

    await flushPromises()

    expect(wrapper.find('.load-error-stub').exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'DashboardStatCards' }).exists()).toBe(false)
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
          ElCard: { template: '<div><slot /></div>' },
          ElSkeleton: true,
          ElEmpty: true,
          ElTable: true,
          ElTableColumn: true,
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
})
