import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import ForbiddenView from '@/views/ForbiddenView.vue'

const routerPush = vi.fn()
let routeQuery: Record<string, string> = {}

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: routeQuery }),
  useRouter: () => ({ push: routerPush }),
}))

function mountForbidden(query: Record<string, string> = {}) {
  routeQuery = query
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    fallbackLocale: 'en',
    messages: { en },
  })

  return mount(ForbiddenView, {
    global: {
      plugins: [i18n, createPinia()],
      stubs: {
        'el-result': {
          props: ['title', 'subTitle'],
          template: '<div><h1>{{ title }}</h1><p>{{ subTitle }}</p><slot name="extra" /></div>',
        },
        'el-button': {
          template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  })
}

describe('ForbiddenView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    routeQuery = {}
  })

  it('shows trace reference from route query', () => {
    const wrapper = mountForbidden({ traceId: 'TRC-FORBIDDEN-1' })

    expect(wrapper.text()).toContain('TRC-FORBIDDEN-1')
    expect(wrapper.text()).toContain('Reference')
  })

  it('renders localized access denied copy', () => {
    const wrapper = mountForbidden()

    expect(wrapper.text()).toContain('Access denied')
    expect(wrapper.text()).toContain('You do not have permission to view this page.')
  })
})
