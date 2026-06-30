import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AppBreadcrumb from '@/components/layout/AppBreadcrumb.vue'
import en from '@/i18n/locales/en'

const routerPush = vi.fn()
const routeState = { path: '/templates/tmpl-1', query: {} as Record<string, string> }

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({ push: routerPush }),
}))

describe('AppBreadcrumb', () => {
  beforeEach(() => {
    routerPush.mockReset()
    routeState.path = '/templates/tmpl-1'
  })

  function mountBreadcrumb() {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    return mount(AppBreadcrumb, {
      global: { plugins: [i18n, ElementPlus] },
    })
  }

  it('renders navigable breadcrumb links for ancestor segments', async () => {
    const wrapper = mountBreadcrumb()
    await flushPromises()

    const links = wrapper.findAll('button.breadcrumb-link')
    expect(links.length).toBeGreaterThan(0)
    expect(links[0]!.classes()).toContain('breadcrumb-link')
  })

  it('navigates when a breadcrumb link is clicked', async () => {
    const wrapper = mountBreadcrumb()
    await flushPromises()

    const link = wrapper.find('button.breadcrumb-link')
    await link.trigger('click')

    expect(routerPush).toHaveBeenCalled()
  })
})
